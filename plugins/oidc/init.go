package main

import (
	"context"
	"encoding/json"
	"log"
	"net/http"
	"os"

	"github.com/lestrrat-go/jwx/v2/jwk"
)

func init() {
	// get the environment variable pointing to the openid connect configuration
	// on the authorization server
	oidcConnectUrl, isSet := os.LookupEnv("OIDC_AUTHORITY")
	if !isSet {
		log.Fatal("OIDC_AUTHORITY environment variable is not set")
	}

	// get the configuration of the openid connect server
	res, err := http.Get(oidcConnectUrl)
	if err != nil {
		log.Fatal("unable to load openid connect configuration")
	}
	var oidcConfiguration map[string]interface{}
	err = json.NewDecoder(res.Body).Decode(&oidcConfiguration)
	if err != nil {
		log.Fatal("unable to parse open id connect configuration")
	}

	// now retrieve the jwks url and the introspection url
	UserinfoEndpoint = oidcConfiguration["userinfo_endpoint"].(string)
	JWKSUrl = oidcConfiguration["jwks_uri"].(string)
	// now download the jwks used to sign the access tokens
	JWKSCache = jwk.NewCache(context.Background(), nil)
	err = JWKSCache.Register(JWKSUrl)
	if err != nil {
		log.Fatal("unable to register JWKS URL in JWKSCache")
	}

	// now get the Client ID
	OIDC_ClientID, isSet = os.LookupEnv("OIDC_CLIENT_ID")
	if !isSet {
		log.Fatal("OIDC_CLIENT_ID environment variable is not set")
	}
}
