FROM gradle:8-jdk21 AS build

WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .

RUN gradle buildFatJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]