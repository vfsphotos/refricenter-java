FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app
COPY build.gradle settings.gradle ./
COPY src ./src
COPY static ./static
COPY templates ./templates
RUN gradle bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
COPY --from=builder /app/static ./static
COPY --from=builder /app/templates ./templates
RUN mkdir -p media backups
EXPOSE 8000
ENTRYPOINT ["java", "-jar", "app.jar"]
