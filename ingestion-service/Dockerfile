FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy Maven settings with mirror configuration
COPY maven-settings.xml /root/.m2/settings.xml

# Copy parent pom and shared-domain first
COPY pom.xml .
COPY shared-domain shared-domain/
COPY ingestion-service/pom.xml ingestion-service/

# Install parent POM and build shared-domain first
RUN mvn -N install -s /root/.m2/settings.xml && \
    mvn -f shared-domain/pom.xml clean install -DskipTests -s /root/.m2/settings.xml

# Copy source and build (dependencies will be downloaded during build)
COPY ingestion-service/src ingestion-service/src/
RUN mvn -f ingestion-service/pom.xml clean package -DskipTests -s /root/.m2/settings.xml

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/ingestion-service/target/quarkus-app/ /app/

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
