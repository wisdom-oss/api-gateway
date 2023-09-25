package main

type Configuration struct {
	JWKSEndpoint     string `json:"jwksUri"`
	UserinfoEndpoint string `json:"userinfoUri"`
	ClientID         string `json:"clientID"`
}
