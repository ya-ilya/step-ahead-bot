FROM gradle:jdk21-alpine AS build

WORKDIR /home/gradle/src

COPY gradlew gradle.properties build.gradle.kts settings.gradle.kts ./
COPY . .
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon build || return 0  # Build for cache dependencies
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon shadowJar

FROM openjdk:21-jdk-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/shadow.jar /app/application.jar

COPY --from=build /home/gradle/src/documents/ /app/documents/

ENTRYPOINT ["java", "-jar", "/app/application.jar"]