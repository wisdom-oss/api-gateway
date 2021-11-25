FROM maven:3.8.4-openjdk-17 AS build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -f /usr/src/app/pom.xml clean package

FROM amazoncorretto:17-alpine3.14
COPY --from=build /usr/src/app/target/api-gateway-*.jar /opt/api-gateway/api-gateway.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/opt/api-gateway/api-gateway.jar"]
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVICE_REGISTRY_HOSTNAME=service-registry
