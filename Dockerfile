FROM openjdk:8-alpine

COPY target/uberjar/cmtx-calendar.jar /cmtx-calendar/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/cmtx-calendar/app.jar"]
