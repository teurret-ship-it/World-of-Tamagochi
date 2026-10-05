# Build: docker compose build server   (context = repository root)
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY . .
RUN ./gradlew :server:installDist -Pwot.jvmOnly=true --no-daemon

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /src/server/build/install/server /app
EXPOSE 8080
ENTRYPOINT ["/app/bin/server"]
