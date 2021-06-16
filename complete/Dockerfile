FROM openjdk:11-jre-slim
ARG JAR_FILE

ADD target/${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","app.jar"]
