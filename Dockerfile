FROM maven:3.8.4-openjdk-17 AS maven-build
COPY src /usr/src/app/src
COPY pom.xml /usr/src/app
RUN mvn -B -f  /usr/src/app/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
COPY --from=maven-build /usr/src/app/target/api-gateway-*.jar /opt/api-gateway/api-gateway.jar
WORKDIR /opt/api-gateway/
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "/opt/api-gateway/api-gateway.jar"]
ENV SPRING_PROFILES_ACTIVE=production
ENV SERVICE_REGISTRY_HOSTNAME=service-registry
LABEL de.uol.wisdom.oss.version="1.0-RELEASE"
LABEL de.uol.wisdom.oss.maintainer="Jan Eike Suchard<wisdom@uol.de>"
