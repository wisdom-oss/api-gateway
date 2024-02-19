//go:generate gomarkdoc --output docs.md .
package main

// ErrEmptyDiscoveryEndpoint is returned to the client, if the plugin
// configuration is missing the OIDC discovery endpoint uri since the uri
// is required to automatically detect the required other uris for the
// validation flow used in the gateway
var ErrEmptyDiscoveryEndpoint = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Discovery Endpoint Missing",
	Detail: "The plugin used for authenticating requests is misconfigured. Please contact your administrator for further assistance.",
}

// ErrDiscoveryRequestFail is returned to the client, if the plugin is unable
// to request the discovery information for the following uris from the OIDC
// server.
//
//	jwks_uri:          The URI pointing to the JSON Web Key Set used to sign the JWT in the request headers
//	userinfo_endpoint: The URI pointing to the user information that can be requested for further information about the user
var ErrDiscoveryRequestFail = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "OIDC Discovery Failed",
	Detail: "The discovery of the required endpoints for the authorization validation failed. Please contact your administrator for further assistance.",
}

// ErrDiscoveryRequestResultParseFail is returned to the client, if the plugin
// received a response, but the body of the response could not be parsed.
var ErrDiscoveryRequestResultParseFail = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "OIDC Discovery Response Parsing Failed",
	Detail: "The discovery of the required endpoints was unsuccessful since the contents could not be read. Please contact your administrator for further assistance.",
}

// ErrDiscoveryResultInvalidFieldType is returned to the client, if the plugin
// received a response and has been able to decode it, but the field it needs
// for the authorization flow is not in the expected type.
var ErrDiscoveryResultInvalidFieldType = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "OIDC Discovery Response Invalid Field Type",
	Detail: "The discovery response contained an invalid value on at least one field. Please contact your administrator for further assistance.",
}

// ErrUserinfoParseFail is returned if the userinfo cannot be read from the
// response sent by the OIDC server
var ErrUserinfoParseFail = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Userinfo Parsing Failure",
	Detail: "The requested information about the identified user could not be parsed",
}

// ErrUserinfoMissingSubject is returned if the userinfo response lacks the
// 'subject' field and therefore no association between the requesting user and
// the information supplied by the OpenID Connect server can be found. Due to
// the potential security risks the request is rejected
var ErrUserinfoMissingSubject = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Userinfo Response Missing Subject",
	Detail: "The requested information about the identified user does not contain a subject and the gateway therefore is unable to ensure that the information is for the current user",
}

// ErrSubjectMismatch is returned if the userinfo shows another user
// than the user set the JWT. As the userinfo is not usable the request is
// rejected
var ErrSubjectMismatch = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Subject Mismatch",
	Detail: "The requested information about the identified user contained a different subject that the access token",
}

// ErrUserinfoMissingUsername is returned if the userinfo response lacks the
// 'username' field . Due to the potential security risks the request is
// rejected
var ErrUserinfoMissingUsername = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Userinfo Response Missing Username",
	Detail: "The requested information about the identified user does not contain a username",
}

// ErrUserinfoMissingGroups is returned if the userinfo response lacks the
// 'groups' field. Since the services in the backend require this value the
// request is rejected
var ErrUserinfoMissingGroups = GatewayError{
	Type:   "https://www.rfc-editor.org/rfc/rfc9110#section-15.6.1",
	Status: 500,
	Title:  "Userinfo Response Missing Groups",
	Detail: "The requested information about the identified user does not contain information about groups",
}
