FROM openjdk:21-slim as builder
WORKDIR /app
COPY /build/libs/tas.jar .

CMD ["java", "-jar", "tas.jar"]