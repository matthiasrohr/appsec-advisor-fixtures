FROM eclipse-temurin:11-jre

WORKDIR /app
COPY target/spring-boot-threat-fixture-0.1.0.jar /app/app.jar

ENV PARTNER_API_TOKEN=demo-partner-token-baked-into-image
ENV APP_ADMIN_BOOTSTRAP_TOKEN=adm1n-demo-token-please-change

USER root

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
