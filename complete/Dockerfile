FROM openjdk:11-jre-slim
#FROM public.ecr.aws/gw2/jdk-11:latest
ARG JAR_FILE

ADD target/${JAR_FILE} /app.jar
ENTRYPOINT ["java","-jar","app.jar"]
