if [ "${ENV}" = "DEV" ]; then
    java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005 -jar /app/application.jar
else
    java -jar /app/application.jar
fi