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
