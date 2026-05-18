FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY market-app/pom.xml market-app/pom.xml
RUN chmod +x mvnw && ./mvnw -pl market-app dependency:go-offline -B

COPY market-app/src market-app/src
RUN ./mvnw -pl market-app clean package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=build /workspace/market-app/target/market-app-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
