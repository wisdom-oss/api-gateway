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
