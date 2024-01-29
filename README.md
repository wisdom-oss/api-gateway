<div align="center">
<img height="150px" src="https://raw.githubusercontent.com/wisdom-oss/brand/main/svg/standalone_color.svg">
<h1>API Gateway</h1>
<h3>api-gateway</h3>
<p>🛡️ Extended Kong API Gateway for the WISdoM Platform</p>
<img alt="GitHub Actions Workflow Status" src="https://img.shields.io/github/actions/workflow/status/wisdom-oss/api-gateway/docker.yaml?style=for-the-badge&label=Docker%20Build">
<a href="https://github.com/wisdom-oss/api-gateway/pkgs/container/api-gateway">
<img alt="Static Badge" src="https://img.shields.io/badge/ghcr.io-wisdom--oss%2Fapi--gateway-2496ED?style=for-the-badge&logo=docker&logoColor=white&labelColor=555555">
</a>
</div>

> [!NOTE]
> This API Gateway is based on the Open Source Edition of the [Kong API Gateway](https://konghq.com/products/kong-gateway).
> Therefore, features available in the free (closed-source) and enterprise (also
> closed-source) edition are not available with this image.

The API Gateway is the central part of the WISdoM Plaform and manages the access
to the microservices implemented for the platform.
Furhtermore, it is extended by a plugin using the 
[`go-pdk`](https://github.com/Kong/go-pdk) to validate JWTs which are used in a
standard deployment to authenticate and secure requests.
> &mdash; [Read more](plugins/oidc/README.md)

The API Gateway is autommatically configured using the 
[gateway-service-watcher](https://github.com/wisdom-oss/gateway-service-watcher)
which acts as a watchdog to check the deployed containers on the host for their
association to the WISdoM plaform.
> &mdash; [Read more](https://github.com/wisdom-oss/gateway-service-watcher)