FROM golang:alpine AS plugin-build
COPY plugins /tmp/plugins
WORKDIR /tmp/plugins
RUN mkdir /plugins
RUN for d in */ ; do echo "Bulding plugin in $d"; cd $d; go mod download -x -json; go build -x -v -o /plugins/ -x . ; echo "Built plugin in $d"; cd ../; done
RUN ls /plugins

FROM kong:alpine
USER root
COPY --from=plugin-build /plugins /usr/local/bin/
RUN chown kong:kong /usr/local/bin/oidc
ENV KONG_PLUGINSERVER_NAMES=oidc
ENV KONG_PLUGINSERVER_OIDC_START_CMD="/usr/local/bin/oidc"
ENV KONG_PLUGINSERVER_OIDC_SOCKET="/usr/local/kong/oidc.socket"
ENV KONG_PLUGINSERVER_OIDC_QUERY_CMD="/usr/local/bin/oidc -dump"
ENV KONG_PROXY_LISTEN="0.0.0.0:8000 reuseport backlog=16384"
ENV KONG_ADMIN_LISTEN="0.0.0.0:8001 reuseport backlog=16384"
ENV KONG_GUI_LISTEN="0.0.0.0:8002"
ENV KONG_PLUGINS=bundled,oidc
ENV KONG_PROXY_ACCESS_LOG=/dev/stdout
ENV KONG_ADMIN_ACCESS_LOG=/dev/stdout
ENV KONG_PROXY_ERROR_LOG=/dev/stderr
ENV KONG_ADMIN_ERROR_LOG=/dev/stderr
