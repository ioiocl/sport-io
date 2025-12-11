# Sportbot Analytics Service Setup Script
# This script creates all remaining files for the analytics service

Write-Host "🚀 Setting up Sportbot Analytics Service..." -ForegroundColor Green
Write-Host ""

# 1. RedisMatchDataSubscriber.java
Write-Host "Creating RedisMatchDataSubscriber.java..." -ForegroundColor Yellow
$subscriberContent = @"
package cl.ioio.sportbot.analytics.adapter;

import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.ports.MatchDataSubscriber;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Consumer;

@ApplicationScoped
@Slf4j
public class RedisMatchDataSubscriber implements MatchDataSubscriber {
    
    private final PubSubCommands<String> pubSubCommands;
    private final ObjectMapper objectMapper;
    
    @Inject
    public RedisMatchDataSubscriber(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.pubSubCommands = redisDataSource.pubsub(String.class);
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void subscribe(String[] matchIds, Consumer<MatchEvent> handler) {
        for (String matchId : matchIds) {
            String channel = "match-events:" + matchId;
            pubSubCommands.subscribe(channel, message -> {
                try {
                    MatchEvent event = objectMapper.readValue(message, MatchEvent.class);
                    handler.accept(event);
                } catch (Exception e) {
                    log.error("Error handling message", e);
                }
            });
            log.info("Subscribed to {}", channel);
        }
    }
    
    @Override
    public void unsubscribe() {
        log.info("Unsubscribing from all matches");
    }
}
"@
$subscriberContent | Out-File -FilePath "analytics-service\src\main\java\cl\ioio\sportbot\analytics\adapter\RedisMatchDataSubscriber.java" -Encoding UTF8

# 2. RedisSnapshotRepository.java
Write-Host "Creating RedisSnapshotRepository.java..." -ForegroundColor Yellow
$repoContent = @"
package cl.ioio.sportbot.analytics.adapter;

import cl.ioio.sportbot.domain.model.MatchSnapshot;
import cl.ioio.sportbot.domain.ports.SnapshotRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@ApplicationScoped
@Slf4j
public class RedisSnapshotRepository implements SnapshotRepository {
    
    private static final String KEY_PREFIX = "latest_match_snapshot:";
    
    private final ValueCommands<String, String> valueCommands;
    private final KeyCommands<String> keyCommands;
    private final ObjectMapper objectMapper;
    
    @Inject
    public RedisSnapshotRepository(RedisDataSource redisDataSource, ObjectMapper objectMapper) {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        this.objectMapper = objectMapper;
    }
    
    @Override
    public void save(MatchSnapshot snapshot) {
        try {
            String key = KEY_PREFIX + snapshot.getMatchId();
            String json = objectMapper.writeValueAsString(snapshot);
            valueCommands.set(key, json);
            log.debug("Saved snapshot for match {}", snapshot.getMatchId());
        } catch (Exception e) {
            log.error("Error saving snapshot", e);
        }
    }
    
    @Override
    public Optional<MatchSnapshot> getLatest(String matchId) {
        try {
            String key = KEY_PREFIX + matchId;
            String json = valueCommands.get(key);
            if (json != null) {
                return Optional.of(objectMapper.readValue(json, MatchSnapshot.class));
            }
        } catch (Exception e) {
            log.error("Error getting snapshot", e);
        }
        return Optional.empty();
    }
    
    @Override
    public void delete(String matchId) {
        String key = KEY_PREFIX + matchId;
        keyCommands.del(key);
    }
}
"@
$repoContent | Out-File -FilePath "analytics-service\src\main\java\cl\ioio\sportbot\analytics\adapter\RedisSnapshotRepository.java" -Encoding UTF8

# 3. MatchAnalysisService.java
Write-Host "Creating MatchAnalysisService.java..." -ForegroundColor Yellow
$serviceContent = @"
package cl.ioio.sportbot.analytics.application;

import cl.ioio.sportbot.analytics.domain.GoalForecaster;
import cl.ioio.sportbot.analytics.domain.MatchSimulator;
import cl.ioio.sportbot.analytics.domain.MomentumAnalyzer;
import cl.ioio.sportbot.domain.model.*;
import cl.ioio.sportbot.domain.ports.MatchDataSubscriber;
import cl.ioio.sportbot.domain.ports.SnapshotRepository;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class MatchAnalysisService {
    
    @ConfigProperty(name = "analytics.matches")
    String matchesConfig;
    
    @Inject
    MatchDataSubscriber matchDataSubscriber;
    
    @Inject
    SnapshotRepository snapshotRepository;
    
    @Inject
    MomentumAnalyzer momentumAnalyzer;
    
    @Inject
    GoalForecaster goalForecaster;
    
    @Inject
    MatchSimulator matchSimulator;
    
    private final Map<String, List<MatchEvent>> eventHistory = new ConcurrentHashMap<>();
    private String[] matchIds;
    
    void onStart(@Observes StartupEvent ev) {
        matchIds = matchesConfig.split(",");
        log.info("Analyzing {} matches", matchIds.length);
        
        matchDataSubscriber.subscribe(matchIds, this::handleMatchEvent);
    }
    
    private void handleMatchEvent(MatchEvent event) {
        String matchId = event.getMatchId();
        
        eventHistory.computeIfAbsent(matchId, k -> new ArrayList<>()).add(event);
        
        List<MatchEvent> history = eventHistory.get(matchId);
        if (history.size() < 10) {
            log.debug("Not enough data for match {} ({} events)", matchId, history.size());
            return;
        }
        
        log.info("Analyzing match {}: {} {} - {} {}", 
                matchId, event.getHomeTeam(), event.getHomeScore(),
                event.getAwayScore(), event.getAwayTeam());
    }
    
    @Scheduled(every = "{snapshot.interval:15s}")
    void generateSnapshots() {
        for (String matchId : matchIds) {
            try {
                generateSnapshot(matchId.trim());
            } catch (Exception e) {
                log.error("Error generating snapshot for {}", matchId, e);
            }
        }
    }
    
    private void generateSnapshot(String matchId) {
        List<MatchEvent> history = eventHistory.get(matchId);
        if (history == null || history.size() < 10) {
            return;
        }
        
        MatchEvent latest = history.get(history.size() - 1);
        
        // Extract possession history
        List<BigDecimal> possessionHistory = new ArrayList<>();
        for (MatchEvent event : history) {
            if (event.getPossession() != null) {
                possessionHistory.add(event.getPossession());
            }
        }
        
        // Momentum analysis
        MomentumMetrics momentum = momentumAnalyzer.analyze(possessionHistory);
        
        // Goal forecast
        List<BigDecimal> goalHistory = new ArrayList<>();
        for (MatchEvent event : history) {
            goalHistory.add(BigDecimal.valueOf(event.getHomeScore() + event.getAwayScore()));
        }
        GoalForecast goalForecast = goalForecaster.forecast(goalHistory);
        
        // Match simulation
        int minutesRemaining = 90 - (latest.getMinute() != null ? latest.getMinute() : 0);
        MatchPrediction prediction = matchSimulator.simulate(
                latest.getHomeScore(),
                latest.getAwayScore(),
                momentum.getDrift().doubleValue(),
                momentum.getVolatility().doubleValue(),
                10000,
                minutesRemaining
        );
        
        // Determine match state
        MatchSnapshot.MatchState state = determineMatchState(momentum.getDrift().doubleValue());
        
        // Create snapshot
        MatchSnapshot snapshot = MatchSnapshot.builder()
                .matchId(matchId)
                .homeTeam(latest.getHomeTeam())
                .awayTeam(latest.getAwayTeam())
                .timestamp(Instant.now())
                .minute(latest.getMinute())
                .status(latest.getStatus())
                .homeScore(latest.getHomeScore())
                .awayScore(latest.getAwayScore())
                .momentumMetrics(momentum)
                .matchPrediction(prediction)
                .goalForecast(goalForecast)
                .matchState(state)
                .sampleSize(history.size())
                .build();
        
        snapshotRepository.save(snapshot);
        log.info("Snapshot saved for match {}: {} (momentum: {})", 
                matchId, state, momentum.getDrift());
    }
    
    private MatchSnapshot.MatchState determineMatchState(double drift) {
        if (drift > 0.10) return MatchSnapshot.MatchState.HOME_DOMINATING;
        if (drift > 0.03) return MatchSnapshot.MatchState.HOME_SLIGHT_ADVANTAGE;
        if (drift < -0.10) return MatchSnapshot.MatchState.AWAY_DOMINATING;
        if (drift < -0.03) return MatchSnapshot.MatchState.AWAY_SLIGHT_ADVANTAGE;
        return MatchSnapshot.MatchState.BALANCED;
    }
}
"@
$serviceContent | Out-File -FilePath "analytics-service\src\main\java\cl\ioio\sportbot\analytics\application\MatchAnalysisService.java" -Encoding UTF8

# 4. application.properties
Write-Host "Creating application.properties..." -ForegroundColor Yellow
$propsContent = @"
# Quarkus configuration
quarkus.application.name=analytics-service
quarkus.http.port=8082

# Analytics configuration
analytics.matches=`${ANALYTICS_MATCHES:215662,592872,867946}
snapshot.interval=`${SNAPSHOT_INTERVAL:15s}

# Monte Carlo configuration
monte.carlo.horizon.minutes=`${MONTE_CARLO_HORIZON_MINUTES:45}
monte.carlo.simulations=`${MONTE_CARLO_SIMULATIONS:10000}

# ARIMA configuration
arima.horizon.minutes=`${ARIMA_HORIZON_MINUTES:10}

# Redis configuration
redis.host=`${REDIS_HOST:localhost}
redis.port=`${REDIS_PORT:6379}
quarkus.redis.hosts=`${QUARKUS_REDIS_HOSTS:redis://localhost:6379}

# Logging
quarkus.log.level=INFO
quarkus.log.category."cl.ioio.sportbot".level=DEBUG
quarkus.log.console.format=%d{HH:mm:ss} %-5p [%c{2.}] (%t) %s%e%n
"@
$propsContent | Out-File -FilePath "analytics-service\src\main\resources\application.properties" -Encoding UTF8

# 5. pom.xml
Write-Host "Creating pom.xml..." -ForegroundColor Yellow
$pomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>cl.ioio.sportbot</groupId>
        <artifactId>sportbot-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>analytics-service</artifactId>
    <name>Sportbot - Analytics Service</name>

    <dependencies>
        <dependency>
            <groupId>cl.ioio.sportbot</groupId>
            <artifactId>shared-domain</artifactId>
            <version>`${project.version}</version>
        </dependency>

        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-arc</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-redis-client</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-scheduler</artifactId>
        </dependency>

        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-math3</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>io.quarkus.platform</groupId>
                <artifactId>quarkus-maven-plugin</artifactId>
                <extensions>true</extensions>
            </plugin>
        </plugins>
    </build>
</project>
"@
$pomContent | Out-File -FilePath "analytics-service\pom.xml" -Encoding UTF8

# 6. Dockerfile
Write-Host "Creating Dockerfile..." -ForegroundColor Yellow
$dockerContent = @"
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

COPY pom.xml .
COPY shared-domain shared-domain/
COPY analytics-service/pom.xml analytics-service/

RUN mvn -f analytics-service/pom.xml dependency:go-offline

COPY analytics-service/src analytics-service/src/
RUN mvn -f analytics-service/pom.xml clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=build /build/analytics-service/target/quarkus-app/lib/ /app/lib/
COPY --from=build /build/analytics-service/target/quarkus-app/*.jar /app/
COPY --from=build /build/analytics-service/target/quarkus-app/app/ /app/app/
COPY --from=build /build/analytics-service/target/quarkus-app/quarkus/ /app/quarkus/

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/quarkus-run.jar"]
"@
$dockerContent | Out-File -FilePath "analytics-service\Dockerfile" -Encoding UTF8

# 7. docker-compose.yml
Write-Host "Creating docker-compose.yml..." -ForegroundColor Yellow
$composeContent = @"
version: '3.8'

services:
  redis:
    image: redis:7-alpine
    container_name: sportbot-redis
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 3s
      retries: 5
    networks:
      - sportbot-network

  ingestion-service:
    build:
      context: .
      dockerfile: ingestion-service/Dockerfile
    container_name: sportbot-ingestion
    environment:
      - FOOTBALL_API_KEY=`${FOOTBALL_API_KEY}
      - FOOTBALL_API_HOST=`${FOOTBALL_API_HOST}
      - FOOTBALL_BASE_URL=`${FOOTBALL_BASE_URL}
      - FOOTBALL_MATCHES=`${FOOTBALL_MATCHES}
      - POLL_INTERVAL=`${POLL_INTERVAL}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - QUARKUS_REDIS_HOSTS=redis://redis:6379
    depends_on:
      redis:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - sportbot-network

  analytics-service:
    build:
      context: .
      dockerfile: analytics-service/Dockerfile
    container_name: sportbot-analytics
    environment:
      - ANALYTICS_MATCHES=`${ANALYTICS_MATCHES}
      - SNAPSHOT_INTERVAL=`${SNAPSHOT_INTERVAL}
      - MONTE_CARLO_HORIZON_MINUTES=`${MONTE_CARLO_HORIZON_MINUTES}
      - MONTE_CARLO_SIMULATIONS=`${MONTE_CARLO_SIMULATIONS}
      - ARIMA_HORIZON_MINUTES=`${ARIMA_HORIZON_MINUTES}
      - REDIS_HOST=redis
      - REDIS_PORT=6379
      - QUARKUS_REDIS_HOSTS=redis://redis:6379
    depends_on:
      redis:
        condition: service_healthy
    restart: unless-stopped
    networks:
      - sportbot-network

networks:
  sportbot-network:
    driver: bridge
"@
$composeContent | Out-File -FilePath "docker-compose.yml" -Encoding UTF8

Write-Host ""
Write-Host "✅ All files created successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "📋 Next steps:" -ForegroundColor Cyan
Write-Host "1. Get live match IDs from API-Football"
Write-Host "2. Update .env with live match IDs"
Write-Host "3. Run: mvn clean install"
Write-Host "4. Run: docker compose up --build"
Write-Host ""
Write-Host "🎯 Your API key is already configured: 85c00191235f58aa27fcccd8d737d8a7" -ForegroundColor Yellow
Write-Host ""
