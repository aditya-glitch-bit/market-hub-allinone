# MarketHub backend image: builds the fat jar with Maven, then runs it on a slim JRE.
#   docker build -t markethub-api backend
#   docker run -p 8080:8080 markethub-api

# ---- build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- run stage ----
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/markethub-backend.jar app.jar

# Embedded H2 database is written to /app/data (use MARKETHUB_DB_URL for MySQL instead).
ENV MARKETHUB_SEED=true
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
