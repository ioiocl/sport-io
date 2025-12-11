package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Complete ABC (ARIMA-Bayes-Carlo) analysis result
 * Represents the integrated output of the three-stage analysis pipeline
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ABCAnalysisResult {
    
    @JsonProperty("arimaSignal")
    private ARIMASignal arimaSignal;
    
    @JsonProperty("momentumMetrics")
    private MomentumMetrics momentumMetrics;
    
    @JsonProperty("matchPrediction")
    private MatchPrediction matchPrediction;
    
    @JsonProperty("goalForecast")
    private GoalForecast goalForecast;
    
    @JsonProperty("needsRecalibration")
    private boolean needsRecalibration;
    
    @JsonProperty("integrationConfidence")
    private BigDecimal integrationConfidence;
}
