package cl.ioio.sportbot.websocket;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * REST endpoint to control analysis start/stop
 */
@Path("/api/start-analysis")
@Slf4j
public class AnalysisControlResource {

    @Inject
    RedisDataSource redisDataSource;

    private ValueCommands<String, String> valueCommands;

    @jakarta.annotation.PostConstruct
    void init() {
        this.valueCommands = redisDataSource.value(String.class);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startAnalysis(MatchSelectionRequest request) {
        try {
            List<String> matchIds = request.getMatchIds();
            
            if (matchIds == null || matchIds.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"error\": \"No match IDs provided\"}")
                        .build();
            }

            // Store selected match IDs in Redis for ingestion service to pick up
            String matchIdsStr = String.join(",", matchIds);
            valueCommands.set("active_match_ids", matchIdsStr);
            
            log.info("Started analysis for {} matches: {}", matchIds.size(), matchIdsStr);
            
            // Don't add CORS headers manually - Quarkus handles it via application.properties
            return Response.ok()
                    .entity("{\"status\": \"started\", \"matchCount\": " + matchIds.size() + "}")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error starting analysis", e);
            return Response.serverError()
                    .entity("{\"error\": \"" + e.getMessage() + "\"}")
                    .build();
        }
    }

    public static class MatchSelectionRequest {
        private List<String> matchIds;

        public List<String> getMatchIds() {
            return matchIds;
        }

        public void setMatchIds(List<String> matchIds) {
            this.matchIds = matchIds;
        }
    }
}
