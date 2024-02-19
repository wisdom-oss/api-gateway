package main

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"net/http"
	"reflect"
	"regexp"
	"strings"
	"time"

	"github.com/Kong/go-pdk"
	"github.com/lestrrat-go/jwx/v2/jwk"
	"github.com/lestrrat-go/jwx/v2/jwt"
)

// bearerTokenPattern is a regular expression pattern used to validate and parse
// bearer tokens.
// It matches the following format: "Bearer <token>". The token is a non-empty
// string of characters that do not contain whitespace.
// The pattern is case-insensitive.
// Example usage:
//
//	if matched, _ := regexp.MatchString(bearerTokenPattern, token); matched {
//	    // Token is valid
//	} else {
//	    // Token is invalid
//	}
const bearerTokenPattern = `(?i)^(Bearer) (\S+)$`

// bearerTokenRegEx is used to store the regular expression after building it
// once
var bearerTokenRegEx *regexp.Regexp

var errEmptyEndpoint = errors.New("empty discovery endpoint")
var errDiscoveryRequestFailure = errors.New("discovery request failed")
var errDiscoveryUnmarshalFailure = errors.New("unable to unmarshal openid connect configuration")
var errDiscoveryInvalidType = errors.New("discovery response contained invalid type")

// fieldTypeErrorTemplate is used to keep the same repeating string as a
// constant before creating error messages with it
const fieldTypeErrorTemplate = `field '%s' is expected to be 'string' got: %s`

// discoverEndpoints retrieves the userinfo and jwks endpoints from the given
// discovery endpoint.
// The function sends a GET request to the discovery endpoint, which should
// respond with the openid connect configuration.
// This configuration is a JSON object that the function decodes into a map,
// extracting the userinfo_endpoint and jwks_uri fields as the userinfo and jwks
// endpoints respectively.
// In case of error, it will also join the error with suitable contextual error
// message and return.
// If the discovery endpoint is empty or leads to a failed request, the function
// returns an error.
// If the retrieval or decoding of the openid connect configuration fails, the
// function returns an error.
//
// Parameters:
//
//	discoveryEndpoint (string): address of the discovery endpoint.
//
// Returns:
//
//	userinfoEndpoint (string): the discovered userinfo endpoint.
//	jwksEndpoint (string): the discovered jwks endpoint.
//	err (error): the error, if any occurred during the process.
func discoverEndpoints(discoveryEndpoint string) (userinfoEndpoint string, jwksEndpoint string, gwErr *GatewayError) {
	// cleanup the discovery endpoint address and remove possible whitespaces
	discoveryEndpoint = strings.TrimSpace(discoveryEndpoint)
	if discoveryEndpoint == "" {
		return "", "", &ErrEmptyDiscoveryEndpoint
	}

	// now request the openid connect configuration endpoint
	res, err := http.Get(discoveryEndpoint)
	if err != nil {
		ErrDiscoveryRequestFail.Error = err.Error()
		return "", "", &ErrDiscoveryRequestFail
	}

	// now read the openid connect configuration
	var openIdConnectConfiguration map[string]interface{}
	err = json.NewDecoder(res.Body).Decode(&openIdConnectConfiguration)
	if err != nil {
		ErrDiscoveryRequestResultParseFail.Error = err.Error()
		return "", "", &ErrDiscoveryRequestResultParseFail
	}
	var isValidString bool
	userinfoEndpoint, isValidString = openIdConnectConfiguration["userinfo_endpoint"].(string)
	if !isValidString {
		actualType := reflect.TypeOf(openIdConnectConfiguration["userinfo_endpoint"])
		err = errors.Join(errDiscoveryInvalidType, fmt.Errorf(fieldTypeErrorTemplate, "userinfo_endpoint", actualType))
		ErrDiscoveryResultInvalidFieldType.Error = err.Error()
		return "", "", &ErrDiscoveryResultInvalidFieldType
	}

	jwksEndpoint, isValidString = openIdConnectConfiguration["jwks_uri"].(string)
	if !isValidString {
		actualType := reflect.TypeOf(openIdConnectConfiguration["jwks_uri"])
		err = errors.Join(errDiscoveryInvalidType, fmt.Errorf(fieldTypeErrorTemplate, "jwks_uri", actualType))
		return "", "", &ErrDiscoveryResultInvalidFieldType

	}

	return userinfoEndpoint, jwksEndpoint, nil
}

// extractBearerToken is a function that takes an authorization header value as
// input and extracts the bearer token from it.
// It uses a regular expression to match the header value against a predefined
// pattern.
// If a match is found and there are two capture groups in the match result, the
// function returns the second capture group, which represents the bearer token.
// If no match is found or there are not exactly two capture groups, an empty
// string is returned.
//
// Example usage:
//
//	token := extractBearerToken("Bearer abcxyz123")
//	// token = "abcxyz123"
//
// Parameters:
//   - authorizationHeaderValue: The value of the authorization header.
//
// Returns:
//
//	The extracted bearer token, or an empty string if no valid bearer token is found.
func extractBearerToken(authorizationHeaderValue string) string {
	// check if the bearer token regex has already been compiled
	if bearerTokenRegEx == nil {
		bearerTokenRegEx = regexp.MustCompile(bearerTokenPattern)
	}
	matches := bearerTokenRegEx.FindStringSubmatch(authorizationHeaderValue)
	if len(matches) == 3 {
		return matches[2]
	}
	return ""
}

// Access is executed every time the kong gateway receives a request. Since ther
// plugin may be restarted at any moment
func (c *Configuration) Access(kong *pdk.PDK) {
	// create a shortcut for the logger
	logger := kong.Log

	userinfoEndpoint, jwksEndpoint, gwErr := discoverEndpoints(c.DiscoveryUri)
	if gwErr != nil {
		logger.Crit("unable to extract required endpoints from openid connect discovery")
		gwErr.SendKong(kong)
		return
	}

	// use the extracted JWKS endpoint to download the JSON Web Key Set to be
	// able to verify the access token locally
	jwks, err := jwk.Fetch(context.Background(), jwksEndpoint)
	if err != nil {
		logger.Crit("unable to fetch the JWKS", err.Error())
		response := GatewayError{
			Type:   "gateway.OPENID_CONNECT_JWKS_DOWNLOAD_ERROR",
			Title:  "JWKS Download Error",
			Detail: "Unable to download the JWKS needed for validating the access token",
			Status: 500,
			Error:  err.Error(),
		}
		response.SendKong(kong)
		return
	}

	authorizationHeader, err := kong.Request.GetHeader("Authorization")
	if err != nil {
		logger.Crit("unable to extract headers from request", err.Error())
		response := GatewayError{
			Type:   "https://www.rfc-editor.org/rfc/rfc9110.html#section-15.6.1",
			Title:  "Authorization Header Retrieval Failed",
			Detail: "Unable to extract required request headers from the incoming request",
			Status: 500,
			Error:  err.Error(),
		}
		response.SendKong(kong)
		return
	}

	bearerToken := extractBearerToken(authorizationHeader)
	if strings.TrimSpace(bearerToken) == "" {
		logger.Warn("empty bearer token extracted from request")
		response := GatewayError{
			Type:   "https://www.rfc-editor.org/rfc/rfc9110.html#section-15.6.1",
			Title:  "Empty Bearer Token",
			Detail: "The Bearer Token extracted from the request is empty.",
			Status: 400,
			Error:  "Bad Request",
		}
		response.SendKong(kong, map[string][]string{
			"WWW-Authenticate": {`Bearer scope="openid profile email", error="invalid_request", error_description="Authorization header malformed'"`},
		})
	}

	// now parse the bearer token and automatically validate it
	accessToken, err := jwt.ParseString(bearerToken, jwt.WithKeySet(jwks), jwt.WithAudience(c.ClientID), jwt.WithValidate(true))
	if err != nil {

		response := GatewayError{
			Instance: "https://www.rfc-editor.org/rfc/rfc9110.html#section-15.5.2",
			Status:   401,
		}
		switch {
		case errors.Is(err, jwt.ErrTokenExpired()):
			response.Title = "Access Token Expired"
			response.Detail = "The access token in the 'Authorization' header has expired"
		case errors.Is(err, jwt.ErrInvalidIssuedAt()):
			response.Title = "Access Token Issued In Future"
			response.Detail = "The access token in the 'Authorization' header has been issued in the future"
		case errors.Is(err, jwt.ErrTokenNotYetValid()):
			response.Title = "Access Token Used Too Early"
			response.Detail = "The access token in the 'Authorization' header is not valid yet. please try again later"
		case errors.Is(err, jwt.ErrInvalidAudience()):
			response.Title = "Access Token Invalid Audience"
			response.Detail = "The access token in the 'Authorization' header has not been issued for this platform"
		default:
			logger.Crit("unknown error occurred while checking jwt", err.Error())
			response.WrapNativeError(err)
			return
		}
		response.SendKong(kong)
	}
	// since the access token now has been validated to be active, request the
	// userinfo endpoint using the access token as Authorization
	userinfoRequest, err := http.NewRequest("GET", userinfoEndpoint, nil)
	if err != nil {
		logger.Crit("unable to create new request for userinfo", err)
		response := GatewayError{}
		response.WrapNativeError(err)
		response.SendKong(kong)
		return
	}
	userinfoRequest.Header.Set("Authorization", authorizationHeader)
	httpClient := http.Client{}
	userinfoResponse, err := httpClient.Do(userinfoRequest)
	if err != nil {
		logger.Crit("error while getting userinfo", err)
		response := GatewayError{}
		response.WrapNativeError(err)
		response.SendKong(kong)
		return
	}
	// now parse the userinfo response
	var userinfo map[string]interface{}
	err = json.NewDecoder(userinfoResponse.Body).Decode(&userinfo)
	if err != nil {
		logger.Crit("error while parsing userinfo", err)
		ErrUserinfoParseFail.SendKong(kong)
		return
	}

	// now check if the userinfo response contains a subject
	subject, isSet := userinfo["sub"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'subject'")
		logger.Crit("invalid userinfo response", err)
		ErrUserinfoMissingSubject.SendKong(kong)
		return
	}

	// now check if the userinfo really is for the access token
	if accessToken.Subject() != subject {
		err := errors.New("userinfo subject mismatch")
		logger.Crit("invalid userinfo response", err)
		ErrSubjectMismatch.SendKong(kong)
		return
	}

	// now extract the username and the groups the token is valid for
	username, isSet := userinfo["preferred_username"].(string)
	if !isSet {
		err := errors.New("userinfo missing 'preferred_username'")
		logger.Crit("invalid userinfo response", err)
		ErrUserinfoMissingUsername.SendKong(kong)
		return
	}

	// now extract the username and the groups the token is valid for
	staff, isSet := userinfo["staff"].(string)
	if !isSet {
		staff = "false"
	}

	groupsInterface, isSet := userinfo["groups"].([]interface{})
	if !isSet {
		err := errors.New("userinfo missing 'groups'")
		logger.Err("invalid userinfo response", err)
		ErrUserinfoMissingGroups.SendKong(kong)
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
			response.WrapNativeError(err)
			response.SendKong(kong)
			return
		}
		groups = append(groups, groupString)
	}

	// now build the token sent to the microservice
	serviceTokenBuilder := jwt.NewBuilder()
	serviceTokenBuilder.Issuer("api-gateway")
	serviceTokenBuilder.IssuedAt(time.Now())
	serviceTokenBuilder.Expiration(time.Now().Add(1 * time.Minute))
	serviceTokenBuilder.Subject(username)
	serviceTokenBuilder.Claim("groups", groups)
	serviceTokenBuilder.Claim("staff", staff)
	// now retrieve the token
	serviceToken, err := serviceTokenBuilder.Build()
	serializer := jwt.NewSerializer()
	rawServiceToken, _ := serializer.Serialize(serviceToken)

	kong.ServiceRequest.SetHeader("Authorization", fmt.Sprintf("Bearer %s", rawServiceToken))
}
