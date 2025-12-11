package cl.ioio.sportbot.ingestion.adapter;

import cl.ioio.sportbot.domain.model.MatchEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Adapter for API-Football REST API
 * Polls match data at regular intervals
 */
@ApplicationScoped
@Slf4j
public class FootballApiClient {
    
    @ConfigProperty(name = "football.api.key")
    String apiKey;
    
    @ConfigProperty(name = "football.api.host")
    String apiHost;
    
    @ConfigProperty(name = "football.base.url")
    String baseUrl;
    
    @Inject
    ObjectMapper objectMapper;
    
    private HttpClient httpClient;
    
    @jakarta.annotation.PostConstruct
    void init() {
        try {
            // Create a trust manager that accepts all certificates (for development)
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
            
            this.httpClient = HttpClient.newBuilder()
                    .sslContext(sslContext)
                    .build();
                    
            log.info("HTTP client initialized with permissive SSL context");
        } catch (Exception e) {
            log.error("Failed to initialize HTTP client with custom SSL context, using default", e);
            this.httpClient = HttpClient.newHttpClient();
        }
    }
    
    /**
     * Fetch live match data for a specific match
     */
    public MatchEvent fetchMatchData(String matchId) {
        try {
            // Get match statistics
            String statsUrl = baseUrl + "/fixtures/statistics?fixture=" + matchId;
            JsonNode statsResponse = makeRequest(statsUrl);
            
            if (statsResponse == null || !statsResponse.has("response") || 
                statsResponse.get("response").isEmpty()) {
                log.warn("No statistics data for match {}", matchId);
                return null;
            }
            
            JsonNode matchData = statsResponse.get("response").get(0);
            
            // Validate required fields
            if (!matchData.has("team") || !matchData.has("statistics")) {
                log.warn("Invalid match data structure for match {}", matchId);
                return null;
            }
            
            JsonNode team = matchData.get("team");
            JsonNode statistics = matchData.get("statistics");
            
            // Get fixture data separately
            String fixtureUrl = baseUrl + "/fixtures?id=" + matchId;
            JsonNode fixtureResponse = makeRequest(fixtureUrl);
            
            if (fixtureResponse == null || !fixtureResponse.has("response") || 
                fixtureResponse.get("response").isEmpty()) {
                log.warn("No fixture data for match {}", matchId);
                return null;
            }
            
            JsonNode fixtureData = fixtureResponse.get("response").get(0);
            JsonNode fixture = fixtureData.get("fixture");
            JsonNode teams = fixtureData.get("teams");
            JsonNode goals = fixtureData.get("goals");
            
            // Extract basic info
            String homeTeam = teams.get("home").get("name").asText();
            String awayTeam = teams.get("away").get("name").asText();
            String status = fixture.get("status").get("short").asText();
            Integer minute = fixture.get("status").has("elapsed") ? 
                fixture.get("status").get("elapsed").asInt() : 0;
            
            // Get goals
            Integer homeScore = goals.has("home") && !goals.get("home").isNull() ? 
                goals.get("home").asInt() : 0;
            Integer awayScore = goals.has("away") && !goals.get("away").isNull() ? 
                goals.get("away").asInt() : 0;
            
            // Extract statistics (only for home team from statistics endpoint)
            JsonNode homeStats = statistics;
            
            BigDecimal possession = extractStat(homeStats, "Ball Possession");
            Integer shots = extractStatInt(homeStats, "Total Shots");
            Integer shotsOnTarget = extractStatInt(homeStats, "Shots on Goal");
            Integer corners = extractStatInt(homeStats, "Corner Kicks");
            Integer fouls = extractStatInt(homeStats, "Fouls");
            Integer yellowCards = extractStatInt(homeStats, "Yellow Cards");
            Integer redCards = extractStatInt(homeStats, "Red Cards");
            
            // Build event
            return MatchEvent.builder()
                    .matchId(matchId)
                    .timestamp(Instant.now())
                    .minute(minute)
                    .status(status)
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .homeScore(homeScore)
                    .awayScore(awayScore)
                    .possession(possession)
                    .shots(shots)
                    .shotsOnTarget(shotsOnTarget)
                    .corners(corners)
                    .fouls(fouls)
                    .yellowCards(yellowCards)
                    .redCards(redCards)
                    .dangerousAttacks(0) // Not available in API
                    .expectedGoals(BigDecimal.ZERO) // Not available in free tier
                    .eventType(MatchEvent.EventType.NONE)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching match data for {}", matchId, e);
            return null;
        }
    }
    
    /**
     * Get list of live match IDs
     */
    public List<String> getLiveMatches() {
        try {
            String url = baseUrl + "/fixtures?live=all";
            JsonNode response = makeRequest(url);
            
            if (response == null || !response.has("response")) {
                return new ArrayList<>();
            }
            
            List<String> matchIds = new ArrayList<>();
            JsonNode fixtures = response.get("response");
            
            for (JsonNode fixture : fixtures) {
                String matchId = fixture.get("fixture").get("id").asText();
                matchIds.add(matchId);
            }
            
            log.info("Found {} live matches", matchIds.size());
            return matchIds;
            
        } catch (Exception e) {
            log.error("Error fetching live matches", e);
            return new ArrayList<>();
        }
    }
    
    private JsonNode makeRequest(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("x-apisports-key", apiKey)
                .header("x-apisports-host", apiHost)
                .GET()
                .build();
        
        HttpResponse<String> response = httpClient.send(request, 
                HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            log.error("API request failed with status {}: {}", 
                    response.statusCode(), response.body());
            return null;
        }
        
        return objectMapper.readTree(response.body());
    }
    
    private BigDecimal extractStat(JsonNode stats, String type) {
        for (JsonNode stat : stats) {
            if (stat.get("type").asText().equals(type)) {
                String value = stat.get("value").asText();
                if (value != null && !value.isEmpty() && !value.equals("null")) {
                    // Remove % sign if present
                    value = value.replace("%", "");
                    try {
                        return new BigDecimal(value);
                    } catch (NumberFormatException e) {
                        return BigDecimal.ZERO;
                    }
                }
            }
        }
        return BigDecimal.ZERO;
    }
    
    private Integer extractStatInt(JsonNode stats, String type) {
        for (JsonNode stat : stats) {
            if (stat.get("type").asText().equals(type)) {
                JsonNode value = stat.get("value");
                if (value != null && !value.isNull()) {
                    return value.asInt();
                }
            }
        }
        return 0;
    }
}
