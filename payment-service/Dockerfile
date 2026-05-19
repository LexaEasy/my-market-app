FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY api/payment-api.yaml api/payment-api.yaml
COPY market-app/pom.xml market-app/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
RUN chmod +x mvnw && ./mvnw -pl payment-service dependency:go-offline -B

COPY payment-service/src payment-service/src
RUN ./mvnw -pl payment-service clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/payment-service/target/payment-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
