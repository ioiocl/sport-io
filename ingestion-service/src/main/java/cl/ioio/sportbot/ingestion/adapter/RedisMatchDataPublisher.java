package cl.ioio.sportbot.ingestion.adapter;

import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.ports.MatchDataPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.pubsub.PubSubCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

/**
 * Redis adapter for publishing match events
 * Implements MatchDataPublisher port
 */
@ApplicationScoped
@Slf4j
public class RedisMatchDataPublisher implements MatchDataPublisher {
    
    private static final String CHANNEL_PREFIX = "match-events:";
    
    @Inject
    RedisDataSource redisDataSource;
    
    @Inject
    ObjectMapper objectMapper;
    
    private PubSubCommands<String> pubsub;
    
    @jakarta.annotation.PostConstruct
    void init() {
        this.pubsub = redisDataSource.pubsub(String.class);
    }
    
    @Override
    public void publish(MatchEvent event) {
        try {
            String channel = CHANNEL_PREFIX + event.getMatchId();
            String message = objectMapper.writeValueAsString(event);
            
            pubsub.publish(channel, message);
            
            log.debug("Published event for match {} to channel {}", 
                    event.getMatchId(), channel);
                    
        } catch (Exception e) {
            log.error("Error publishing match event", e);
        }
    }
}
