package cl.ioio.sportbot.websocket;

import cl.ioio.sportbot.domain.model.MatchSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for broadcasting match snapshots to clients
 */
@ServerEndpoint("/matches")
@ApplicationScoped
@Slf4j
public class BroadcastService {
    
    @ConfigProperty(name = "broadcast.matches", defaultValue = "")
    String matchesConfig;
    
    @Inject
    ObjectMapper objectMapper;
    
    @Inject
    RedisDataSource redisDataSource;
    
    private ValueCommands<String, String> valueCommands;
    private KeyCommands<String> keyCommands;
    
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    @jakarta.annotation.PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
        this.keyCommands = redisDataSource.key();
        log.info("WebSocket broadcast service initialized");
    }
    
    @OnOpen
    public void onOpen(Session session) {
        sessions.put(session.getId(), session);
        log.info("Client connected: {} (total: {})", session.getId(), sessions.size());
    }
    
    @OnClose
    public void onClose(Session session) {
        sessions.remove(session.getId());
        log.info("Client disconnected: {} (total: {})", session.getId(), sessions.size());
    }
    
    @Scheduled(every = "${broadcast.interval:5s}")
    void broadcast() {
        if (sessions.isEmpty()) {
            return;
        }
        
        try {
            // Read active match IDs from Redis (set by user selection)
            String activeMatchIds = valueCommands.get("active_match_ids");
            
            if (activeMatchIds == null || activeMatchIds.isEmpty()) {
                // No matches selected yet
                return;
            }
            
            String[] matchIds = activeMatchIds.split(",");
            log.debug("Broadcasting {} matches to {} clients", matchIds.length, sessions.size());
            
            for (String matchId : matchIds) {
                String key = "latest_match_snapshot:" + matchId.trim();
                String json = valueCommands.get(key);
                
                if (json != null) {
                    MatchSnapshot snapshot = objectMapper.readValue(json, MatchSnapshot.class);
                    broadcastSnapshot(snapshot);
                } else {
                    log.debug("No snapshot found for match {}", matchId.trim());
                }
            }
            
        } catch (Exception e) {
            log.error("Error broadcasting snapshots", e);
        }
    }
    
    private void broadcastSnapshot(MatchSnapshot snapshot) {
        try {
            String message = objectMapper.writeValueAsString(snapshot);
            
            sessions.values().forEach(session -> {
                session.getAsyncRemote().sendText(message, result -> {
                    if (result.getException() != null) {
                        log.error("Error sending to client", result.getException());
                    }
                });
            });
            
            log.debug("Broadcasted snapshot for match {} to {} clients", 
                    snapshot.getMatchId(), sessions.size());
                    
        } catch (Exception e) {
            log.error("Error broadcasting snapshot", e);
        }
    }
}
