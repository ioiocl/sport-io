FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Copy Maven settings with mirror configuration
COPY maven-settings.xml /root/.m2/settings.xml

# Copy POMs for dependency resolution
COPY pom.xml .
COPY shared-domain shared-domain/
COPY analytics-service/pom.xml analytics-service/

# Install parent POM and build shared-domain first with retry logic
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -N install -s /root/.m2/settings.xml && \
    mvn -f shared-domain/pom.xml clean install -DskipTests -s /root/.m2/settings.xml

# Download dependencies with caching
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -f analytics-service/pom.xml dependency:go-offline -s /root/.m2/settings.xml

# Copy source and build
COPY analytics-service/src analytics-service/src/
RUN --mount=type=cache,target=/root/.m2/repository \
    mvn -f analytics-service/pom.xml clean package -DskipTests -s /root/.m2/settings.xml

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/analytics-service/target/quarkus-app/ /app/

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
