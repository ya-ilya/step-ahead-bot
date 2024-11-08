FROM gradle:jdk21-alpine AS build

WORKDIR /home/gradle/src

COPY gradlew gradle.properties build.gradle.kts settings.gradle.kts ./
COPY gradle gradle/
COPY . .
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon -i clean build shadowJar || return 0  # Build for cache dependencies

FROM openjdk:21-jdk-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/shadow.jar /app/application.jar

ENTRYPOINT ["java", "-jar", "/app/application.jar"]