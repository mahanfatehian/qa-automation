FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN mkdir -p /app/fixtures && printf 'sample\\n' > /app/fixtures/sample.txt
COPY --from=build /src/target/runner-engine.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]
