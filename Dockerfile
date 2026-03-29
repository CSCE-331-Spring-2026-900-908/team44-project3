FROM ghcr.io/prefix-dev/pixi:latest AS build

WORKDIR /app
COPY pixi.toml pixi.lock README.md ./
RUN pixi install

RUN apt-get update && apt-get install -y curl && curl -fsSL https://deb.nodesource.com/setup_22.x | bash - && apt-get install -y nodejs && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
COPY src ./src

ENV NODE_OPTIONS="--max-old-space-size=1024"
RUN pixi run mvn package -DskipTests -Dquarkus.package.jar.type=uber-jar

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY --from=build /app/target/*-runner.jar app.jar

EXPOSE 8080

CMD ["java", "-Dquarkus.http.host=0.0.0.0", "-jar", "app.jar"]
