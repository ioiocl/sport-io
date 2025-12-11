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
    
    public enum MatchState {
        HOME_DOMINATING,
        HOME_SLIGHT_ADVANTAGE,
        BALANCED,
        AWAY_SLIGHT_ADVANTAGE,
        AWAY_DOMINATING
    }
}
