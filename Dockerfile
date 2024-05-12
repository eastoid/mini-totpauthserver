FROM openjdk:21-slim as builder

WORKDIR /app
COPY /build/libs/tas.jar .

ENV RUNNING_DOCKERIZED=true

CMD ["java", "-jar", "tas.jar"]