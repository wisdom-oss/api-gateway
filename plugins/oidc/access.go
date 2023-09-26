package main

import (
	"encoding/json"
	"errors"
	"fmt"
	"github.com/Kong/go-pdk"
	"github.com/lestrrat-go/jwx/v2/jwk"
	"github.com/lestrrat-go/jwx/v2/jwt"
	"io"
	"net/http"
	"os"
	"regexp"
	"strings"
)

var headerRegex = regexp.MustCompile(`^(\w+) (\S+)$`)

// Access is executed every time the kong gateway receives a request. Since the
// plugin may be restarted at any moment
func (c *Configuration) Access(kong *pdk.PDK) {
	// access the request and get the authorization header
	request := kong.Request
	authorizationHeader, _ := request.GetHeader("Authorization")
	// now trim away any blank values at the start or end of the string
	authorizationHeader = strings.TrimSpace(authorizationHeader)
	// now get the authorization method and token from the header
	matches := headerRegex.FindStringSubmatch(authorizationHeader)
	if len(matches) != 3 {
		// since the matches array needs to contain three entries, the value is
		// invalid and therefore the user is not authorized
		response := GatewayError{
			ErrorCode:        "gateway.MALFORMED_AUTHORIZATION_HEADER",
			ErrorTitle:       "Authorization Header Malformed",
			ErrorDescription: "The 'Authorization' header is not correctly formatted. please check your request",
			HttpStatusCode:   400,
			HttpStatusText:   "Bad Request",
		}
		headers := map[string][]string{
			"WWW-Authenticate": {`Bearer scope="openid profile email", error="invalid_request", error_description="Authorization header malformed'"`},
		}
		response.SendError(kong, headers)
		return
	}

	// now extract the authorization method and the token
	authorizationMetod := matches[1]
	token := matches[2]
	// now check if the authorization method is Bearer
	if authorizationMetod != "Bearer" {
		response := GatewayError{
			ErrorCode:        "gateway.INVALID_AUTH_SCHEME",
			ErrorTitle:       "Authorization Header Malformed",
			ErrorDescription: "The 'Authorization' does not use the 'Bearer' auth scheme",
			HttpStatusCode:   400,
			HttpStatusText:   "Bad Request",
		}
		headers := map[string][]string{
			"WWW-Authenticate": {`Bearer scope="openid profile email", error="invalid_request", error_description="Authorization header malformed'"`},
		}
		response.SendError(kong, headers)
		return
	}

	// now try to get the JWKS from the specified endpoint if it has not been
	// downloaded to the disk already
	var jwksFile *os.File
	jwksFile, err := os.Open("/tmp/jwks.json")
	if os.IsNotExist(err) {
		// since the file does not exist, download the jwks and store it in the
		// file
		jwksFile, err = os.Create("/tmp/jwks.json")
		if err != nil {
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}
		// now get the configuration and request the jwks from the idp
		idpResponse, err := http.Get(c.JWKSEndpoint)
		if err != nil {
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}

		// now write the idp response into the file
		_, err = io.Copy(jwksFile, idpResponse.Body)
		if err != nil {
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}
	}
	// now read the jwks file
	jwks, err := jwk.ParseReader(jwksFile)
	if err != nil {
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now take the extracted token and parse it using the jwks and validate it
	accessToken, err := jwt.ParseString(token, jwt.WithKeySet(jwks), jwt.WithAudience(c.ClientID))
	if err != nil {
		// prepare an error response
		response := GatewayError{}
		switch err {
		case jwt.ErrTokenExpired():
			response.ErrorCode = "gateway.TOKEN_EXPIRED"
			response.ErrorTitle = "Access Token Expired"
			response.ErrorDescription = "The access token in the 'Authorization' header has expired"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case jwt.ErrInvalidIssuedAt():
			response.ErrorCode = "gateway.TOKEN_ISSUED_IN_FUTURE"
			response.ErrorTitle = "Access Token Issued In Future"
			response.ErrorDescription = "The access token in the 'Authorization' header has been issued in the future"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case jwt.ErrTokenNotYetValid():
			response.ErrorCode = "gateway.TOKEN_USED_TOO_EARLY"
			response.ErrorTitle = "Access Token Used Too Early"
			response.ErrorDescription = "The access token in the 'Authorization' header is not valid yet. please try again later"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case jwt.ErrInvalidAudience():
			response.ErrorCode = "gateway.TOKEN_INVALID_AUDIENCE"
			response.ErrorTitle = "Access Token Invalid Audience"
			response.ErrorDescription = "The access token in the 'Authorization' header has not been issued for this platform"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		default:
			response.WrapError(err, kong)
			return
		}
		response.SendError(kong, nil)
	}

	// since the access token now has been validated to be active, request the
	// userinfo endpoint using the access token as Authorization
	userinfoRequest, err := http.NewRequest("GET", c.UserinfoEndpoint, nil)
	if err != nil {
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	userinfoRequest.Header.Set("Authorization", fmt.Sprintf("Bearer %s", token))
	httpClient := http.Client{}
	userinfoResponse, err := httpClient.Do(userinfoRequest)
	if err != nil {
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	// now parse the userinfo response
	var userinfo map[string]interface{}
	err = json.NewDecoder(userinfoResponse.Body).Decode(&userinfo)
	if err != nil {
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now check if the userinfo response contains a subject
	subject, isSet := userinfo["sub"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'subject'")
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now check if the userinfo really is for the access token
	if accessToken.Subject() != subject {
		err := errors.New("userinfo subject mismatch")
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now extract the username and the groups the token is valid for
	username, isSet := userinfo["preferred_username"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'preferred_username'")
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	groupsInterface, isSet := userinfo["groups"]
	if !isSet {
		err := errors.New("userinfo missing 'groups'")
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	// now convert them into a string array
	groups := groupsInterface.([]string)

	// now join the groups together
	groupString := strings.Join(groups, ",")

	// now add the headers to the downstream request, which set the groups
	kong.ServiceRequest.SetHeader("X-WISdoM-User", username)
	kong.ServiceRequest.SetHeader("X-WISdoM-Groups", groupString)
}
