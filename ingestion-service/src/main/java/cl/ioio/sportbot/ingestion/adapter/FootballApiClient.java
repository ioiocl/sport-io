package cl.ioio.sportbot.ingestion.adapter;

import cl.ioio.sportbot.domain.model.LiveMatchEvent;
import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.model.TeamStatistics;
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
     * Fetch live match data for a specific match with enriched statistics
     */
    public MatchEvent fetchMatchData(String matchId) {
        try {
            // Get fixture data first
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
            String homeTeamId = teams.get("home").get("id").asText();
            String awayTeamId = teams.get("away").get("id").asText();
            String status = fixture.get("status").get("short").asText();
            Integer minute = fixture.get("status").has("elapsed") && !fixture.get("status").get("elapsed").isNull() ? 
                fixture.get("status").get("elapsed").asInt() : 0;
            
            // Get goals
            Integer homeScore = goals.has("home") && !goals.get("home").isNull() ? 
                goals.get("home").asInt() : 0;
            Integer awayScore = goals.has("away") && !goals.get("away").isNull() ? 
                goals.get("away").asInt() : 0;
            
            // Get match statistics for both teams (with half breakdown)
            String statsUrl = baseUrl + "/fixtures/statistics?fixture=" + matchId + "&half=true";
            JsonNode statsResponse = makeRequest(statsUrl);
            
            TeamStatistics homeStats = null;
            TeamStatistics awayStats = null;
            BigDecimal possession = BigDecimal.ZERO;
            Integer shots = 0, shotsOnTarget = 0, corners = 0, fouls = 0, yellowCards = 0, redCards = 0;
            
            if (statsResponse != null && statsResponse.has("response") && 
                !statsResponse.get("response").isEmpty()) {
                
                JsonNode statsArray = statsResponse.get("response");
                
                for (JsonNode teamStats : statsArray) {
                    String teamId = teamStats.get("team").get("id").asText();
                    String teamName = teamStats.get("team").get("name").asText();
                    JsonNode statistics = teamStats.get("statistics");
                    JsonNode stats1h = teamStats.has("statistics_1h") ? teamStats.get("statistics_1h") : null;
                    JsonNode stats2h = teamStats.has("statistics_2h") ? teamStats.get("statistics_2h") : null;
                    
                    TeamStatistics teamStatistics = parseTeamStatistics(teamId, teamName, statistics, stats1h, stats2h);
                    
                    if (teamId.equals(homeTeamId)) {
                        homeStats = teamStatistics;
                        // Legacy fields from home team
                        possession = teamStatistics.getPossession();
                        shots = teamStatistics.getTotalShots();
                        shotsOnTarget = teamStatistics.getShotsOnGoal();
                        corners = teamStatistics.getCornerKicks();
                        fouls = teamStatistics.getFouls();
                        yellowCards = teamStatistics.getYellowCards();
                        redCards = teamStatistics.getRedCards();
                    } else {
                        awayStats = teamStatistics;
                    }
                }
            }
            
            // Get live events (goals, cards, substitutions)
            List<LiveMatchEvent> liveEvents = fetchLiveEvents(matchId);
            
            // Build enriched event
            return MatchEvent.builder()
                    .matchId(matchId)
                    .timestamp(Instant.now())
                    .minute(minute)
                    .status(status)
                    .homeTeam(homeTeam)
                    .awayTeam(awayTeam)
                    .homeTeamId(homeTeamId)
                    .awayTeamId(awayTeamId)
                    .homeScore(homeScore)
                    .awayScore(awayScore)
                    // Legacy fields for backward compatibility
                    .possession(possession)
                    .shots(shots)
                    .shotsOnTarget(shotsOnTarget)
                    .corners(corners)
                    .fouls(fouls)
                    .yellowCards(yellowCards)
                    .redCards(redCards)
                    .dangerousAttacks(0)
                    .expectedGoals(homeStats != null ? homeStats.getExpectedGoals() : BigDecimal.ZERO)
                    .eventType(MatchEvent.EventType.NONE)
                    // New enriched fields
                    .homeStats(homeStats)
                    .awayStats(awayStats)
                    .liveEvents(liveEvents)
                    .apiVersion("v2")
                    .build();
                    
        } catch (Exception e) {
            log.error("Error fetching match data for {}", matchId, e);
            return null;
        }
    }
    
    /**
     * Parse team statistics from API response
     */
    private TeamStatistics parseTeamStatistics(String teamId, String teamName, 
            JsonNode stats, JsonNode stats1h, JsonNode stats2h) {
        
        return TeamStatistics.builder()
                .teamId(teamId)
                .teamName(teamName)
                // Shot breakdown
                .shotsOnGoal(extractStatInt(stats, "Shots on Goal"))
                .shotsOffGoal(extractStatInt(stats, "Shots off Goal"))
                .totalShots(extractStatInt(stats, "Total Shots"))
                .blockedShots(extractStatInt(stats, "Blocked Shots"))
                .shotsInsideBox(extractStatInt(stats, "Shots insidebox"))
                .shotsOutsideBox(extractStatInt(stats, "Shots outsidebox"))
                // Passing
                .totalPasses(extractStatInt(stats, "Total passes"))
                .passesAccurate(extractStatInt(stats, "Passes accurate"))
                .passAccuracy(extractStat(stats, "Passes %"))
                // Possession & territory
                .possession(extractStat(stats, "Ball Possession"))
                .cornerKicks(extractStatInt(stats, "Corner Kicks"))
                .offsides(extractStatInt(stats, "Offsides"))
                // Discipline
                .fouls(extractStatInt(stats, "Fouls"))
                .yellowCards(extractStatInt(stats, "Yellow Cards"))
                .redCards(extractStatInt(stats, "Red Cards"))
                // Goalkeeper
                .goalkeeperSaves(extractStatInt(stats, "Goalkeeper Saves"))
                // Advanced
                .expectedGoals(extractStat(stats, "expected_goals"))
                .goalsPrevented(extractStatInt(stats, "goals_prevented"))
                // Half breakdowns
                .firstHalfStats(stats1h != null ? parseHalfStatistics(stats1h) : null)
                .secondHalfStats(stats2h != null ? parseHalfStatistics(stats2h) : null)
                .build();
    }
    
    /**
     * Parse half-specific statistics
     */
    private TeamStatistics.HalfStatistics parseHalfStatistics(JsonNode stats) {
        return TeamStatistics.HalfStatistics.builder()
                .shotsOnGoal(extractStatInt(stats, "Shots on Goal"))
                .shotsOffGoal(extractStatInt(stats, "Shots off Goal"))
                .totalShots(extractStatInt(stats, "Total Shots"))
                .blockedShots(extractStatInt(stats, "Blocked Shots"))
                .shotsInsideBox(extractStatInt(stats, "Shots insidebox"))
                .shotsOutsideBox(extractStatInt(stats, "Shots outsidebox"))
                .totalPasses(extractStatInt(stats, "Total passes"))
                .passesAccurate(extractStatInt(stats, "Passes accurate"))
                .passAccuracy(extractStat(stats, "Passes %"))
                .possession(extractStat(stats, "Ball Possession"))
                .cornerKicks(extractStatInt(stats, "Corner Kicks"))
                .offsides(extractStatInt(stats, "Offsides"))
                .fouls(extractStatInt(stats, "Fouls"))
                .yellowCards(extractStatInt(stats, "Yellow Cards"))
                .redCards(extractStatInt(stats, "Red Cards"))
                .goalkeeperSaves(extractStatInt(stats, "Goalkeeper Saves"))
                .expectedGoals(extractStat(stats, "expected_goals"))
                .build();
    }
    
    /**
     * Fetch live match events (goals, cards, substitutions)
     */
    public List<LiveMatchEvent> fetchLiveEvents(String matchId) {
        List<LiveMatchEvent> events = new ArrayList<>();
        
        try {
            String eventsUrl = baseUrl + "/fixtures/events?fixture=" + matchId;
            JsonNode response = makeRequest(eventsUrl);
            
            if (response == null || !response.has("response")) {
                return events;
            }
            
            JsonNode eventsArray = response.get("response");
            
            for (JsonNode event : eventsArray) {
                JsonNode time = event.get("time");
                JsonNode team = event.get("team");
                JsonNode player = event.get("player");
                JsonNode assist = event.get("assist");
                
                String type = event.has("type") ? event.get("type").asText() : null;
                String detail = event.has("detail") ? event.get("detail").asText() : null;
                
                LiveMatchEvent liveEvent = LiveMatchEvent.builder()
                        .minute(time.has("elapsed") && !time.get("elapsed").isNull() ? 
                                time.get("elapsed").asInt() : null)
                        .extraTime(time.has("extra") && !time.get("extra").isNull() ? 
                                time.get("extra").asInt() : null)
                        .teamId(team.has("id") && !team.get("id").isNull() ? 
                                team.get("id").asText() : null)
                        .teamName(team.has("name") && !team.get("name").isNull() ? 
                                team.get("name").asText() : null)
                        .playerId(player.has("id") && !player.get("id").isNull() ? 
                                player.get("id").asText() : null)
                        .playerName(player.has("name") && !player.get("name").isNull() ? 
                                player.get("name").asText() : null)
                        .assistPlayerId(assist.has("id") && !assist.get("id").isNull() ? 
                                assist.get("id").asText() : null)
                        .assistPlayerName(assist.has("name") && !assist.get("name").isNull() ? 
                                assist.get("name").asText() : null)
                        .eventType(LiveMatchEvent.parseEventType(type, detail))
                        .detail(detail)
                        .comments(event.has("comments") && !event.get("comments").isNull() ? 
                                event.get("comments").asText() : null)
                        .build();
                
                events.add(liveEvent);
            }
            
            log.debug("Fetched {} live events for match {}", events.size(), matchId);
            
        } catch (Exception e) {
            log.warn("Error fetching live events for match {}: {}", matchId, e.getMessage());
        }
        
        return events;
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
