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
        
        // Don't start with any matches - wait for user selection
        log.info("Ingestion service started. Waiting for match selection...");
    }
    
    /**
     * Poll matches at configured interval
     * Default: every 15 seconds
     */
    @Scheduled(every = "${poll.interval:15s}")
    void pollMatches() {
        // Check Redis for active match IDs
        String activeMatchIds = valueCommands.get("active_match_ids");
        
        if (activeMatchIds == null || activeMatchIds.isEmpty()) {
            // No matches selected yet
            return;
        }
        
        // Update match IDs list
        matchIds = Arrays.asList(activeMatchIds.split(","));
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
     * Discover live matches (optional - can be used to auto-update match list)
     */
    @Scheduled(every = "5m")
    void discoverLiveMatches() {
        try {
            List<String> liveMatches = footballApiClient.getLiveMatches();
            log.info("Currently {} live matches available", liveMatches.size());
            
            // Optionally update matchIds list here
            // matchIds = liveMatches;
            
        } catch (Exception e) {
            log.error("Error discovering live matches", e);
        }
    }
}
