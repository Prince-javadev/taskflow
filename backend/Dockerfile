# ---- build stage ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 taskflow
COPY --from=build /app/target/backend-*.jar app.jar
USER taskflow
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
