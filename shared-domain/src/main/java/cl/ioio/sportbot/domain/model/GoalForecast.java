package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * ARIMA forecast for goal predictions
 * Equivalent to ArimaForecast in Finbot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalForecast {
    
    @JsonProperty("modelOrder")
    private String modelOrder; // e.g., "ARIMA(1,1,1)"
    
    @JsonProperty("horizon")
    private Integer horizon; // Minutes ahead
    
    // Goal predictions by minute
    @JsonProperty("predictions")
    private List<BigDecimal> predictions; // Expected total goals at each minute
    
    @JsonProperty("confidenceIntervalLower")
    private List<BigDecimal> confidenceIntervalLower;
    
    @JsonProperty("confidenceIntervalUpper")
    private List<BigDecimal> confidenceIntervalUpper;
    
    // Next goal prediction
    @JsonProperty("nextGoalProbability")
    private BigDecimal nextGoalProbability; // Probability of goal in next N minutes
    
    @JsonProperty("nextGoalTeam")
    private String nextGoalTeam; // HOME, AWAY, or UNKNOWN
    
    @JsonProperty("nextGoalMinute")
    private Integer nextGoalMinute; // Predicted minute of next goal
    
    // Model quality
    @JsonProperty("aic")
    private BigDecimal aic; // Akaike Information Criterion
    
    @JsonProperty("confidence")
    private BigDecimal confidence; // Model confidence (0-1)
}
