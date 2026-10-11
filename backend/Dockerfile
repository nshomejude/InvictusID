# ChildTrace backend — build the Ktor app and run its distribution.
# Build context is the repo root: docker build -f backend/Dockerfile .

FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY . .
# Build only the backend and its dependency (:shared); no Android SDK needed.
RUN ./gradlew --no-daemon :backend:installDist

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /src/backend/build/install/backend/ /app/
EXPOSE 8080
# DATABASE_URL (optional; in-memory if unset), DATABASE_USER/PASSWORD,
# CHILDTRACE_ALLOW_16_17 are read from the environment at startup.
ENTRYPOINT ["/app/bin/backend"]
