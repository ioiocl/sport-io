package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Comprehensive team statistics from API-Football
 * Contains all available per-team metrics for KPI calculation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeamStatistics {
    
    @JsonProperty("teamId")
    private String teamId;
    
    @JsonProperty("teamName")
    private String teamName;
    
    // Shot breakdown
    @JsonProperty("shotsOnGoal")
    private Integer shotsOnGoal;
    
    @JsonProperty("shotsOffGoal")
    private Integer shotsOffGoal;
    
    @JsonProperty("totalShots")
    private Integer totalShots;
    
    @JsonProperty("blockedShots")
    private Integer blockedShots;
    
    @JsonProperty("shotsInsideBox")
    private Integer shotsInsideBox;
    
    @JsonProperty("shotsOutsideBox")
    private Integer shotsOutsideBox;
    
    // Passing metrics
    @JsonProperty("totalPasses")
    private Integer totalPasses;
    
    @JsonProperty("passesAccurate")
    private Integer passesAccurate;
    
    @JsonProperty("passAccuracy")
    private BigDecimal passAccuracy;
    
    // Possession & territory
    @JsonProperty("possession")
    private BigDecimal possession;
    
    @JsonProperty("cornerKicks")
    private Integer cornerKicks;
    
    @JsonProperty("offsides")
    private Integer offsides;
    
    // Discipline
    @JsonProperty("fouls")
    private Integer fouls;
    
    @JsonProperty("yellowCards")
    private Integer yellowCards;
    
    @JsonProperty("redCards")
    private Integer redCards;
    
    // Goalkeeper
    @JsonProperty("goalkeeperSaves")
    private Integer goalkeeperSaves;
    
    // Advanced metrics (when available)
    @JsonProperty("expectedGoals")
    private BigDecimal expectedGoals;
    
    @JsonProperty("goalsPrevented")
    private Integer goalsPrevented;
    
    // Half-by-half breakdown (for momentum analysis)
    @JsonProperty("firstHalfStats")
    private HalfStatistics firstHalfStats;
    
    @JsonProperty("secondHalfStats")
    private HalfStatistics secondHalfStats;
    
    /**
     * Statistics for a single half
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HalfStatistics {
        private Integer shotsOnGoal;
        private Integer shotsOffGoal;
        private Integer totalShots;
        private Integer blockedShots;
        private Integer shotsInsideBox;
        private Integer shotsOutsideBox;
        private Integer totalPasses;
        private Integer passesAccurate;
        private BigDecimal passAccuracy;
        private BigDecimal possession;
        private Integer cornerKicks;
        private Integer offsides;
        private Integer fouls;
        private Integer yellowCards;
        private Integer redCards;
        private Integer goalkeeperSaves;
        private BigDecimal expectedGoals;
    }
}
