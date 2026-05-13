FROM gradle:8.10-jdk21-alpine AS builder
WORKDIR /app
# Desabilita daemon e native file watcher (necessário para ARM64 / Apple Silicon)
ENV GRADLE_OPTS="-Dorg.gradle.daemon=false -Dorg.gradle.native=false"
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
# static e templates chegam via volume no docker-compose (dev)
RUN mkdir -p static templates media backups
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
