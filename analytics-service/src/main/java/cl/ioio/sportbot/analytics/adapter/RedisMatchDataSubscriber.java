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
