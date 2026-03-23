FROM gradle:8.11-jdk17 AS build
WORKDIR /app
COPY . .
RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/build/libs/spotik-server.jar ./spotik-server.jar
EXPOSE 8080
CMD ["java", "-jar", "spotik-server.jar"]

