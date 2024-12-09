FROM gradle:jdk21-alpine AS build

WORKDIR /home/gradle/src

COPY . .
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon build || return 0  # Build for cache dependencies
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon shadowJar

FROM openjdk:21-jdk-slim
RUN mkdir /app
COPY --from=build /home/gradle/src/startup.bot.sh /app/startup.bot.sh
COPY --from=build /home/gradle/src/bot/build/libs/shadow.jar /app/application.jar

RUN chmod +x /app/startup.bot.sh

EXPOSE 5005:5005
ENTRYPOINT ["/bin/sh", "/app/startup.bot.sh"]