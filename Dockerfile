FROM golang:alpine AS plugin-build
COPY plugins /tmp/plugins
WORKDIR /tmp/plugins
RUN mkdir /plugins
RUN for d in */ ; do echo "Bulding plugin in $d"; cd $d; go mod download -x -json; go build -o /plugins/ -x . ; echo "Built plugin in $d"; cd ../; done
RUN ls /plugins

FROM kong:alpine
USER root
COPY --from=plugin-build /plugins /usr/local/kong/
USER kong
ENV KONG_PLUGINSERVER_NAMES=oidc
ENV KONG_PLUGINSERVER_OIDC_QUERY_CMD="/usr/local/kong/oidc -dump"
ENV KONG_PLUGINSERVER_OIDC_START_CMD="/usr/local/kong/oidc"
ENV KONG_PROXY_LISTEN="0.0.0.0:8000 http2 reuseport backlog=16384"
ENV KONG_ADMIN_LISTEN="0.0.0.0:8001 http2 reuseport backlog=16384"
ENV KONG_PLUGINS=oidc,bundled
ENV KONG_PROXY_ACCESS_LOG=/dev/stdout
ENV KONG_ADMIN_ACCESS_LOG=/dev/stdout
ENV KONG_PROXY_ERROR_LOG=/dev/stderr
ENV KONG_ADMIN_ERROR_LOG=/dev/stderr
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 8000 8001
STOPSIGNAL SIGQUIT
HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD kong health
CMD ["kong", "docker-start"]