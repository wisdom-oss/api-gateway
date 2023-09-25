package main

import (
	"encoding/json"
	"fmt"
	"github.com/Kong/go-pdk"
	wisdomType "github.com/wisdom-oss/commonTypes"
	"net/http"
)

type GatewayError wisdomType.WISdoMError

func (e *GatewayError) SendError(kong *pdk.PDK, headers map[string][]string) {
	// convert the error into json
	bytes, err := json.Marshal(e)
	if err != nil {
		kong.Response.Exit(500, []byte(err.Error()), nil)
		return
	}

	// now respond with the error to the request
	kong.Response.Exit(e.HttpStatusCode, bytes, nil)
}

func (e *GatewayError) WrapError(err error, kong *pdk.PDK) {
	e.ErrorCode = "gateway.INTERNAL_ERROR"
	e.ErrorTitle = "Internal Error"
	e.ErrorDescription = fmt.Sprintf("While handling the request, an internal error occurred in the gateway: %s", err)
	e.HttpStatusCode = http.StatusInternalServerError
	e.HttpStatusText = http.StatusText(e.HttpStatusCode)

	e.SendError(kong, nil)
}
