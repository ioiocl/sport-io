package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Complete analytical snapshot of a match at a point in time
 * Equivalent to MarketSnapshot in Finbot
 * 
 * Enhanced with KPI bundle for comprehensive match analysis
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchSnapshot {
    
    @JsonProperty("matchId")
    private String matchId;
    
    @JsonProperty("homeTeam")
    private String homeTeam;
    
    @JsonProperty("awayTeam")
    private String awayTeam;
    
    @JsonProperty("homeTeamId")
    private String homeTeamId;
    
    @JsonProperty("awayTeamId")
    private String awayTeamId;
    
    @JsonProperty("timestamp")
    private Instant timestamp;
    
    @JsonProperty("minute")
    private Integer minute;
    
    @JsonProperty("status")
    private String status;
    
    // Current state
    @JsonProperty("homeScore")
    private Integer homeScore;
    
    @JsonProperty("awayScore")
    private Integer awayScore;
    
    // Bayesian analysis - momentum metrics
    @JsonProperty("momentumMetrics")
    private MomentumMetrics momentumMetrics;
    
    // Monte Carlo prediction
    @JsonProperty("matchPrediction")
    private MatchPrediction matchPrediction;
    
    @JsonProperty("goalForecast")
    private GoalForecast goalForecast;
    
    // Match state
    @JsonProperty("matchState")
    private MatchState matchState;
    
    // Sample size for confidence
    @JsonProperty("sampleSize")
    private Integer sampleSize;
    
    // ABC Analysis (ARIMA-Bayes-Carlo integrated analysis)
    @JsonProperty("arimaSignal")
    private ARIMASignal arimaSignal;
    
    @JsonProperty("abcIntegrationConfidence")
    private java.math.BigDecimal abcIntegrationConfidence;
    
    @JsonProperty("needsRecalibration")
    private Boolean needsRecalibration;
    
    // ============ NEW KPI BUNDLE ============
    /**
     * Comprehensive KPI metrics for dashboard display
     * Includes possession momentum, shot pressure, discipline metrics, etc.
     */
    @JsonProperty("kpis")
    private MatchKPIs kpis;
    
    /**
     * Structured home team statistics (from API)
     */
    @JsonProperty("homeStats")
    private TeamStatistics homeStats;
    
    /**
     * Structured away team statistics (from API)
     */
    @JsonProperty("awayStats")
    private TeamStatistics awayStats;
    
    /**
     * API version for backward compatibility
     * v1 = legacy, v2 = includes KPIs and structured stats
     */
    @JsonProperty("apiVersion")
    @Builder.Default
    private String apiVersion = "v2";
    
    public enum MatchState {
        HOME_DOMINATING,
        HOME_SLIGHT_ADVANTAGE,
        BALANCED,
        AWAY_SLIGHT_ADVANTAGE,
        AWAY_DOMINATING
    }
    
    /**
     * Check if this snapshot has KPI data
     */
    public boolean hasKPIs() {
        return kpis != null;
    }
}
