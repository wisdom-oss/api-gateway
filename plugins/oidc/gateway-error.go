package main

import (
	"encoding/json"

	"github.com/Kong/go-pdk"
	wisdomTypes "github.com/wisdom-oss/commonTypes/v2"
)

type GatewayError wisdomTypes.WISdoMError

func (e *GatewayError) SendKong(kong *pdk.PDK, headers ...map[string][]string) {
	// set the instance to the gateway
	e.Instance = "gateway"

	// now convert the object into a byte array
	response, err := json.Marshal(e)
	if err != nil {
		kong.Log.Crit("unable to convert gateway error")
		kong.Response.Exit(500, []byte("unable to create error"), nil)
	}

	kong.Response.SetHeader("Content-Type", "application/problem+json; charset=utf-8")
	for _, header := range headers {
		_ = kong.Response.SetHeaders(header)
	}
	kong.Response.Exit(e.Status, response, nil)
}

func (e *GatewayError) WrapNativeError(err error) {
	wisdomError := wisdomTypes.WISdoMError{}
	wisdomError.WrapNativeError(err)
	*e = GatewayError(wisdomError)
	e.Instance = "gateway"
}
