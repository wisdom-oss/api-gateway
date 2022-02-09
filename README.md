# WISdoM OSS - API Gateway
Maintainer: [Jan Eike Suchard](mailto:jan.eike.suchard@uni-oldenburg.de)
<hr>

## Information
This API Gateway is based on the Spring Cloud Gateway.  
This Gateway was extended with a filter which will validate the Authorization Tokens directly 
before passing the request to a microservice. The token will also be passed to the microservice 
in the case the microservice will need the token or wants to validate it again by itself.  
Another filter included in this gateway is a filter which will create a request uuid and will 
set the HTTP header `X-Request-ID` to the  created uuid. The UUID will be present in both 
communication directions (to the microservice/to the user).

## External Configuration

Currently, the configuration is hardcoded into the `*.jar`  files during the build
process. However, this may change in the future. Information about this will be 
presented here

## Allowing CORS (Dangerous!!)

With this development release you are able to allow CORS for special clients and environments. 
This is done by setting the following parameters:

- Environment Variable  
Please set the following environment variable to a value of your choice: `CORS_BYPASS_TOKEN`

- HTTP Request Header  
Set the Header `X-CORS-BYPASS` to the value of the previously set environment variable

After setting those two headers you should be able to work with CORS Requests

> **WARNING**  
> Sharing the values of any parameter may cause the leaking of Authorization Keys, API secrets and 
> other sensitive information. If you are not sure if you really need this feature, please do not 
> configure it!
