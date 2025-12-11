package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Monte Carlo simulation results for match outcome prediction
 * Equivalent to MonteCarloResults in Finbot
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchPrediction {
    
    @JsonProperty("simulations")
    private Integer simulations;
    
    // Outcome probabilities
    @JsonProperty("probabilityHomeWin")
    private BigDecimal probabilityHomeWin;
    
    @JsonProperty("probabilityDraw")
    private BigDecimal probabilityDraw;
    
    @JsonProperty("probabilityAwayWin")
    private BigDecimal probabilityAwayWin;
    
    // Score predictions
    @JsonProperty("expectedFinalScore")
    private String expectedFinalScore; // e.g., "2-1"
    
    @JsonProperty("mostLikelyScores")
    private List<ScoreProbability> mostLikelyScores;
    
    // Goal predictions
    @JsonProperty("probabilityMoreGoals")
    private BigDecimal probabilityMoreGoals;
    
    @JsonProperty("expectedTotalGoals")
    private BigDecimal expectedTotalGoals;
    
    // Risk metrics (for betting)
    @JsonProperty("probabilityComeback")
    private BigDecimal probabilityComeback; // If losing
    
    @JsonProperty("probabilityHoldLead")
    private BigDecimal probabilityHoldLead; // If winning
    
    // Percentiles for goal distribution
    @JsonProperty("percentiles")
    private List<Percentile> percentiles;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScoreProbability {
        @JsonProperty("score")
        private String score;
        
        @JsonProperty("probability")
        private BigDecimal probability;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Percentile {
        @JsonProperty("level")
        private Integer level;
        
        @JsonProperty("totalGoals")
        private BigDecimal totalGoals;
    }
}
