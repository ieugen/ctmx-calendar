FROM openjdk:8-alpine

COPY target/uberjar/jlp.jar /jlp/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/jlp/app.jar"]
