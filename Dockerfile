# Build stage
FROM gradle:8.10-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN ./gradlew shadowJar --no-daemon

# Run stage
FROM eclipse-temurin:17-jre
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/zeero_api-all.jar /app/zeero_api-all.jar
WORKDIR /app
CMD ["java", "-jar", "zeero_api-all.jar"]
