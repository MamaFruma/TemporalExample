FROM docker-hub.crpt.tech/openjdk:11-jre-slim
ARG VERSION
ARG JAR_FILE=/build/libs/*.jar
COPY $JAR_FILE app.jar

ENV SERVER_PORT=8080

EXPOSE $SERVER_PORT

ENTRYPOINT java -jar app.jar