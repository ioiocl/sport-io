package cl.ioio.sportbot.ingestion.application;

import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.ports.MatchDataPublisher;
import cl.ioio.sportbot.ingestion.adapter.FootballApiClient;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Application service for match data ingestion
 * Polls API-Football at regular intervals and publishes events
 */
@ApplicationScoped
@Slf4j
public class MatchIngestionService {
    
    @ConfigProperty(name = "football.matches", defaultValue = "")
    String matchesConfig;
    
    @ConfigProperty(name = "auto.discover.live", defaultValue = "true")
    boolean autoDiscoverLive;
    
    @Inject
    FootballApiClient footballApiClient;
    
    @Inject
    MatchDataPublisher matchDataPublisher;
    
    @Inject
    RedisDataSource redisDataSource;
    
    private ValueCommands<String, String> valueCommands;
    private List<String> matchIds = new ArrayList<>();
    
    void onStart(@Observes StartupEvent ev) {
        this.valueCommands = redisDataSource.value(String.class);
        
        if (autoDiscoverLive) {
            log.info("Ingestion service started in AUTO-DISCOVER mode. Will fetch live matches automatically.");
            // Trigger immediate discovery
            discoverLiveMatches();
        } else {
            log.info("Ingestion service started. Waiting for match selection...");
        }
    }
    
    /**
     * Poll matches at configured interval
     * Default: every 15 seconds
     */
    @Scheduled(every = "${poll.interval:15s}")
    void pollMatches() {
        // If auto-discover is enabled and no matches, skip
        if (matchIds.isEmpty()) {
            log.debug("No matches to poll yet");
            return;
        }
        
        log.debug("Polling {} matches", matchIds.size());
        
        for (String matchId : matchIds) {
            try {
                MatchEvent event = footballApiClient.fetchMatchData(matchId.trim());
                
                if (event != null) {
                    log.info("Match {}: {} {} - {} {} ({}' - {})", 
                            matchId,
                            event.getHomeTeam(),
                            event.getHomeScore(),
                            event.getAwayScore(),
                            event.getAwayTeam(),
                            event.getMinute(),
                            event.getStatus());
                    
                    matchDataPublisher.publish(event);
                } else {
                    log.warn("No data received for match {}", matchId);
                }
                
                // Small delay between requests to avoid rate limiting
                Thread.sleep(500);
                
            } catch (Exception e) {
                log.error("Error polling match {}", matchId, e);
            }
        }
    }
    
    /**
     * Discover live matches and auto-update the list
     * Runs every 5 minutes
     */
    @Scheduled(every = "5m")
    void discoverLiveMatches() {
        if (!autoDiscoverLive) {
            return;
        }
        
        try {
            List<String> liveMatches = footballApiClient.getLiveMatches();
            log.info("Discovered {} live matches", liveMatches.size());
            
            if (!liveMatches.isEmpty()) {
                // Update the match list
                matchIds = new ArrayList<>(liveMatches);
                
                // Store in Redis for other services
                String matchIdsStr = String.join(",", liveMatches);
                valueCommands.set("active_match_ids", matchIdsStr);
                
                log.info("Updated active matches: {}", matchIdsStr);
            } else {
                log.warn("No live matches found");
            }
            
        } catch (Exception e) {
            log.error("Error discovering live matches", e);
        }
    }
}
