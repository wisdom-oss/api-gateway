<h1 align="center">API Gateway</h1>
<h3 align="center">api-gateway</h3>
<div align="center">
<p>
🛡️ A <a href="">Kong API Gateway</a> extended with functionality for the WISdoM
platform
</p>
</div>

# About
The [WISdoM project](https://github.com/wisdom-oss) utilizes the Kong API
Gateway to route requests to their services. Since some needed functionality of
the gateway is locked behind paid plugins, this repository contains some custom
plugins which implement the following features:

- [x] OpenID Connect Authentication/Authorization with JWT Validation

The plugins are implemented in Golang using IPC for communicating with the
API Gateway.

## How to use
Since the API gateway in included in every deployment of the WISdoM platform,
there are no extra steps you need to take.