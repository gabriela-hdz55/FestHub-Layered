FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY proto/ proto/
COPY backend/ backend/
COPY frontend/pom.xml frontend/pom.xml
RUN mvn -q -B -pl backend -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd -r app
USER app
COPY --from=build /src/backend/target/app.jar /app.jar
EXPOSE 50051
ENTRYPOINT ["java", "-jar", "/app.jar"]