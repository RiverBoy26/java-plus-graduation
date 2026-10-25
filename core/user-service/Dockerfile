
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app
COPY . .

RUN mvn -B -pl core/user-service -am -DskipTests package

RUN set -eu; \
    JAR=$(find core/user-service/target -maxdepth 1 -type f -name '*.jar' \
        ! -name '*-sources.jar' ! -name '*-javadoc.jar' \
        ! -name '*.original' ! -name '*.jar.original' \
        | head -n 1); \
    test -n "$JAR"; \
    cp "$JAR" /app/app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]