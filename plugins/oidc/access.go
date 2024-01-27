package main

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"regexp"
	"strconv"
	"strings"

	"github.com/Kong/go-pdk"
	"github.com/lestrrat-go/jwx/v2/jwk"
	"github.com/lestrrat-go/jwx/v2/jwt"
)

var headerRegex = regexp.MustCompile(`^(\w+) (\S+)$`)

// Access is executed every time the kong gateway receives a request. Since the
// plugin may be restarted at any moment
func (c *Configuration) Access(kong *pdk.PDK) {
	// create a shortcut for the logger
	logger := kong.Log

	// get the configuration of the openid connect server
	res, err := http.Get(c.DiscoveryUri)
	if err != nil {
		logger.Crit("unable to load openid connect configuration", err.Error())
		response := GatewayError{
			ErrorCode:        "gateway.OPENID_CONNECT_DISCOVERY_FAILURE",
			ErrorTitle:       "OpenID Connect Discovery Failure",
			ErrorDescription: "The configured OpenID Connect discovery endpoint could not be accessed",
			HttpStatusCode:   500,
			HttpStatusText:   "Internal Server Error",
		}
		response.SendError(kong, nil)
		return
	}
	var oidcConfiguration map[string]interface{}
	err = json.NewDecoder(res.Body).Decode(&oidcConfiguration)
	if err != nil {
		logger.Crit("unable to parse open id connect configuration", err.Error())
		response := GatewayError{
			ErrorCode:        "gateway.OPENID_CONNECT_DISCOVERY_FAILURE",
			ErrorTitle:       "OpenID Connect Discovery Failure",
			ErrorDescription: "The response returned by the configured OpenID Connect discovery endpoint could not be read: " + err.Error(),
			HttpStatusCode:   500,
			HttpStatusText:   "Internal Server Error",
		}
		response.SendError(kong, nil)
	}

	// now retrieve the jwks url and the introspection url
	UserinfoEndpoint := oidcConfiguration["userinfo_endpoint"].(string)
	JWKSUrl := oidcConfiguration["jwks_uri"].(string)
	// now download the jwks used to sign the access tokens
	jwks, err := jwk.Fetch(context.Background(), JWKSUrl)
	if err != nil {
		logger.Crit("unable to load JWKS", err.Error())
		response := GatewayError{
			ErrorCode:        "gateway.OPENID_CONNECT_JWKS_DOWNLOAD_ERROR",
			ErrorTitle:       "JWKS Download Error",
			ErrorDescription: "Unable to download the JWKS needed for validating the access token: " + err.Error(),
			HttpStatusCode:   500,
			HttpStatusText:   "Internal Server Error",
		}
		response.SendError(kong, nil)
	}

	// access the request and get the authorization header
	request := kong.Request
	authorizationHeader, _ := request.GetHeader("Authorization")
	// now trim away any blank values at the start or end of the string
	authorizationHeader = strings.TrimSpace(authorizationHeader)
	if authorizationHeader == "" {
		response := GatewayError{
			ErrorCode:        "gateway.MISSING_AUTHORIZATION_INFORMATION",
			ErrorTitle:       "Authorization Header Missing",
			ErrorDescription: "The 'Authorization' header is not set. Please check your request",
			HttpStatusCode:   401,
			HttpStatusText:   "Unauthorized",
		}
		headers := map[string][]string{
			"WWW-Authenticate": {`Bearer scope="openid profile email", error="invalid_request", error_description="Authorization header malformed'"`},
		}
		response.SendError(kong, headers)
		return
	}
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
	authorizationMethod := matches[1]
	token := matches[2]
	logger.Debug("extracted authorization method and token")
	logger.Debug("validating authorization method")
	// now check if the authorization method is Bearer
	if authorizationMethod != "Bearer" {
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
	logger.Debug("validated authorization method")

	// now take the extracted token and parse it using the jwks and validate it
	logger.Debug("parsing and validating jwt from headers")
	accessToken, err := jwt.ParseString(token, jwt.WithKeySet(jwks), jwt.WithAudience(c.ClientID), jwt.WithValidate(true))
	if err != nil {
		// prepare an error response
		response := GatewayError{}
		switch {
		case errors.Is(err, jwt.ErrTokenExpired()):
			response.ErrorCode = "gateway.TOKEN_EXPIRED"
			response.ErrorTitle = "Access Token Expired"
			response.ErrorDescription = "The access token in the 'Authorization' header has expired"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case errors.Is(err, jwt.ErrInvalidIssuedAt()):
			response.ErrorCode = "gateway.TOKEN_ISSUED_IN_FUTURE"
			response.ErrorTitle = "Access Token Issued In Future"
			response.ErrorDescription = "The access token in the 'Authorization' header has been issued in the future"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case errors.Is(err, jwt.ErrTokenNotYetValid()):
			response.ErrorCode = "gateway.TOKEN_USED_TOO_EARLY"
			response.ErrorTitle = "Access Token Used Too Early"
			response.ErrorDescription = "The access token in the 'Authorization' header is not valid yet. please try again later"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		case errors.Is(err, jwt.ErrInvalidAudience()):
			response.ErrorCode = "gateway.TOKEN_INVALID_AUDIENCE"
			response.ErrorTitle = "Access Token Invalid Audience"
			response.ErrorDescription = "The access token in the 'Authorization' header has not been issued for this platform"
			response.HttpStatusCode = http.StatusUnauthorized
			response.HttpStatusText = http.StatusText(response.HttpStatusCode)
			break
		default:
			logger.Err("unknown error occurred while checking jwt", err)
			response.WrapError(err, kong)
			return
		}
		response.SendError(kong, nil)
	}
	logger.Debug("validated jwt")
	// since the access token now has been validated to be active, request the
	// userinfo endpoint using the access token as Authorization
	logger.Debug("requesting userinfo")
	userinfoRequest, err := http.NewRequest("GET", UserinfoEndpoint, nil)
	if err != nil {
		logger.Err("unable to create new request for userinfo", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	userinfoRequest.Header.Set("Authorization", authorizationHeader)
	httpClient := http.Client{}
	userinfoResponse, err := httpClient.Do(userinfoRequest)
	if err != nil {
		logger.Err("error while getting userinfo", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	// now parse the userinfo response
	logger.Debug("parsing userinfo response")
	var userinfo map[string]interface{}
	err = json.NewDecoder(userinfoResponse.Body).Decode(&userinfo)
	if err != nil {
		logger.Err("error while parsing userinfo", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now check if the userinfo response contains a subject
	subject, isSet := userinfo["sub"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'subject'")
		logger.Err("invalid userinfo response", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now check if the userinfo really is for the access token
	if accessToken.Subject() != subject {
		err := errors.New("userinfo subject mismatch")
		logger.Err("invalid userinfo response", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	// now extract the username and the groups the token is valid for
	username, isSet := userinfo["preferred_username"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'preferred_username'")
		logger.Err("invalid userinfo response", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}

	groupsInterface, isSet := userinfo["groups"].([]interface{})
	if !isSet {
		err := errors.New("userinfo missing 'groups'")
		logger.Err("invalid userinfo response", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	// now convert them into a string array
	groups := []string{}
	for _, group := range groupsInterface {
		groupString, ok := group.(string)
		if !ok {
			err := errors.New("group not convertible to string")
			logger.Err("unable to convert items of group", err, group)
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}
		groups = append(groups, groupString)
	}

	// now join the groups together
	groupString := strings.Join(groups, ",")

	// now check if the userinfo response contains the staff marker
	staffString, isSet := userinfo["staff"].(string)
	if !isSet {
		err := kong.ServiceRequest.SetHeader("X-Is-Staff", "false")
		if err != nil {
			logger.Err("unable to set staff header", err)
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}
		err = kong.ServiceRequest.SetHeader("X-Superuser", "false")
		if err != nil {
			logger.Err("unable to set superuser header", err)
			response := GatewayError{}
			response.WrapError(err, kong)
			return
		}
	} else {
		// try to parse the staff string into a boolean
		isStaff, err := strconv.ParseBool(staffString)
		if err != nil || !isStaff {
			err := kong.ServiceRequest.SetHeader("X-Is-Staff", "false")
			if err != nil {
				logger.Err("unable to set staff header", err)
				response := GatewayError{}
				response.WrapError(err, kong)
				return
			}
			err = kong.ServiceRequest.SetHeader("X-Superuser", "false")
			if err != nil {
				logger.Err("unable to set superuser header", err)
				response := GatewayError{}
				response.WrapError(err, kong)
				return
			}
		} else {
			err := kong.ServiceRequest.SetHeader("X-Is-Staff", "true")
			if err != nil {
				logger.Err("unable to set staff header", err)
				response := GatewayError{}
				response.WrapError(err, kong)
				return
			}
			err = kong.ServiceRequest.SetHeader("X-Superuser", "true")
			if err != nil {
				logger.Err("unable to set superuser header", err)
				response := GatewayError{}
				response.WrapError(err, kong)
				return
			}
		}
	}

	// now add the headers to the downstream request, which set the groups
	err = kong.ServiceRequest.SetHeader("X-WISdoM-User", username)
	if err != nil {
		logger.Err("unable to set username header", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	err = kong.ServiceRequest.SetHeader("X-Authenticated-User", username)
	if err != nil {
		logger.Err("unable to set authenticated user header", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	err = kong.ServiceRequest.SetHeader("X-WISdoM-Groups", groupString)
	if err != nil {
		logger.Err("unable to set groups header", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
	err = kong.ServiceRequest.SetHeader("X-Authenticated-Groups", groupString)
	if err != nil {
		logger.Err("unable to set authenticated groups header", err)
		response := GatewayError{}
		response.WrapError(err, kong)
		return
	}
}
