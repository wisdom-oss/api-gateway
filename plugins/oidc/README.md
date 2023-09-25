<div align="center">
<img height="150px" src="https://raw.githubusercontent.com/wisdom-oss/brand/main/svg/standalone_color.svg">
<h1>Open ID Connect Plugin</h1>
<h3>oidc</h3>
<p>🧩 A plugin for authenticating and authorizing users using JWTs and OpenID Connect</p>
<img src="https://img.shields.io/github/go-mod/go-version/wisdom-oss/api-gateway?style=for-the-badge&filename=/plugins/oidc/go.mod" alt="Go Lang Version"/>
</div>

## About
This plugin intercepts every call to the API and checks the `Authorization`
header and the JWT (JSON Web Token) contained in it.
The JWT is validated using the JWKS (JSON Web Key Set) available on the endpoint
specified in the plugin configuration.
The JWKS is downloaded once at the first request the plugin intercepts and will
be stored on disk, to limit the needed network requests to external servers and
therefore speeding up the validation of the tokens.
After the JWT has been validated, it will be used to access the information
about a user that is available under the configured user info endpoint.
These responses can optionally be cached using a redis database to further
minimize the number of network requests executed for each intercepted request.

## Configuration
### Plugin
The configuration of the plugin is done using the plugin configuration through
the Admin API of the Gateway.
This means that the following parameters are to be set via the configuration of 
the plugin in the gateway:
* `jwksUri` &ndash; The URI pointing to the JSON Web Key Set of the Identity 
  Provider
* `userinfoUri` &ndash; The URI pointing to the Userinfo Endpoint of the
  identity provider

## Cache
Even though the cache is a part of the plugin, it will be configured using
environment variables on the container the gateway lives in.
This is done, to allow other plugins to also create a connection to the redis
cache, if needed.
To allow the redis connection to be created, the following environment variables
need to be present in the environment.
* `REDIS_ENABLED` &ndash; Indicates to plugins, if redis may be used if needed
* `REDIS_URI` &ndash; Indicates the URI for connecting to the redis database