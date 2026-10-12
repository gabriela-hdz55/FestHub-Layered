FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
COPY pom.xml .
COPY proto/ proto/
COPY frontend/ frontend/
COPY backend/pom.xml backend/pom.xml
RUN mvn -q -B -pl frontend -am package -DskipTests

FROM eclipse-temurin:21-jre
RUN useradd -r app
USER app
COPY --from=build /src/frontend/target/app.jar /app.jar
COPY --from=build /src/frontend/public /public
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]