FROM eclipse-temurin:25-jre-alpine
WORKDIR /app/run
COPY /build/libs/ViaProxyPlus-*.jar /app/ViaProxyPlus.jar
ENTRYPOINT ["java", "-jar", "/app/ViaProxyPlus.jar", "config", "viaproxy.yml"]