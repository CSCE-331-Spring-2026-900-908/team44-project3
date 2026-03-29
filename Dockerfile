FROM ghcr.io/prefix-dev/pixi:latest AS build

WORKDIR /app
COPY pixi.toml pixi.lock README.md ./
RUN pixi install

COPY pom.xml .
COPY src ./src

RUN pixi run mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*-runner.jar app.jar

EXPOSE 8080

CMD ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "app.jar"]
