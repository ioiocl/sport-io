package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Represents a single event/tick from a live football match
 * Equivalent to MarketTick in Finbot
 * 
 * Enhanced with structured home/away statistics and live event feeds
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchEvent {
    
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("minute")
    private Integer minute;
    
    @JsonProperty("status")
    private String status; // 1H, HT, 2H, ET, P, FT, etc.
    
    // Score
    @JsonProperty("homeScore")
    private Integer homeScore;
    
    @JsonProperty("awayScore")
    private Integer awayScore;
    
    // Teams
    @JsonProperty("homeTeam")
    private String homeTeam;
    
    @JsonProperty("awayTeam")
    private String awayTeam;
    
    @JsonProperty("homeTeamId")
    private String homeTeamId;
    
    @JsonProperty("awayTeamId")
    private String awayTeamId;
    
    // ============ LEGACY FIELDS (kept for backward compatibility) ============
    // Statistics (converted to momentum metric)
    @JsonProperty("possession")
    private BigDecimal possession; // Home team possession %
    
    @JsonProperty("shots")
    private Integer shots;
    
    @JsonProperty("shotsOnTarget")
    private Integer shotsOnTarget;
    
    @JsonProperty("corners")
    private Integer corners;
    
    @JsonProperty("fouls")
    private Integer fouls;
    
    @JsonProperty("yellowCards")
    private Integer yellowCards;
    
    @JsonProperty("redCards")
    private Integer redCards;
    
    // Advanced metrics
    @JsonProperty("dangerousAttacks")
    private Integer dangerousAttacks;
    
    @JsonProperty("expectedGoals")
    private BigDecimal expectedGoals; // xG for home team
    
    // Event type (if specific event occurred)
    @JsonProperty("eventType")
    private EventType eventType;
    
    // ============ NEW STRUCTURED STATISTICS ============
    /**
     * Comprehensive home team statistics
     */
    @JsonProperty("homeStats")
    private TeamStatistics homeStats;
    
    /**
     * Comprehensive away team statistics
     */
    @JsonProperty("awayStats")
    private TeamStatistics awayStats;
    
    /**
     * Live match events (goals, cards, substitutions, etc.)
     * Ordered chronologically
     */
    @JsonProperty("liveEvents")
    private List<LiveMatchEvent> liveEvents;
    
    /**
     * API version indicator for backward compatibility
     * v1 = legacy fields only, v2 = includes structured stats
     */
    @JsonProperty("apiVersion")
    @Builder.Default
    private String apiVersion = "v2";
    
    public enum EventType {
        GOAL,
        YELLOW_CARD,
        RED_CARD,
        SUBSTITUTION,
        VAR,
        PENALTY,
        CORNER,
        OFFSIDE,
        NONE
    }
    
    /**
     * Check if this event has enriched statistics
     */
    public boolean hasEnrichedStats() {
        return homeStats != null && awayStats != null;
    }
}
