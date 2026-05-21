FROM maven:3.9.6-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

RUN apt-get update && apt-get install -y tesseract-ocr tesseract-ocr-eng && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar"]