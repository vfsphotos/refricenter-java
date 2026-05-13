FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
RUN gradle bootJar -x test --no-daemon -Dorg.gradle.native=false

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
# static e templates chegam via volume no docker-compose (dev)
# ou via COPY separado no deploy (prod com frontend junto)
RUN mkdir -p static templates media backups
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
