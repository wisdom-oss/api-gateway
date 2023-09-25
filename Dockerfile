FROM golang:alpine AS plugin-build
COPY plugins /tmp/plugins
WORKDIR /tmp/plugins
RUN mkdir /plugins
RUN for d in */ ; do echo "Bulding plugin in $d"; cd $d; go mod download -x -json; go build -o /plugins/ -x . ; echo "Built plugin in $d"; cd ../; done
RUN ls /plugins

FROM kong:alpine
USER root
COPY --from=plugin-build /plugins /usr/local/bin/
USER kong
ENV KONG_PLUGINSERVER_NAMES=oidc
ENV KONG_PLUGINSERVER_OIDC_QUERY_CMD=/usr/local/bin/oidc
ENTRYPOINT ["/docker-entrypoint.sh"]
EXPOSE 8000 8443 8001 8444
STOPSIGNAL SIGQUIT
HEALTHCHECK --interval=10s --timeout=10s --retries=10 CMD kong health
CMD ["kong", "docker-start"]