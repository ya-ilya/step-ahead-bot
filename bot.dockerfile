# Stage 1: Build the Gradle application
FROM gradle:jdk21-alpine AS build

# Set the working directory
WORKDIR /home/gradle/src

# Copy the entire project into the container
COPY --chown=gradle:gradle . .

# Build the application and cache dependencies
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon build || true
RUN --mount=type=cache,target=/root/.gradle gradle --no-daemon shadowJar

# Stage 2: Create the final image
FROM openjdk:21-jdk-slim

# Create app directory
RUN mkdir /app

# Copy the startup script and the built shadow JAR file into the final image
COPY --from=build /home/gradle/src/startup.bot.sh /app/startup.bot.sh
COPY --from=build /home/gradle/src/bot/build/libs/shadow.jar /app/application.jar

# Make the startup script executable
RUN chmod +x /app/startup.bot.sh

# Expose the application port
EXPOSE 5005

# Set the entrypoint
ENTRYPOINT ["/bin/sh", "/app/startup.bot.sh"]