package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Bayesian analysis results for match momentum
 * Equivalent to BayesianMetrics in Finbot
 * 
 * Drift (μ): Momentum direction
 *   - Positive: Home team has momentum
 *   - Negative: Away team has momentum
 *   - Zero: Balanced match
 * 
 * Volatility (σ): Match variability
 *   - High: Open, unpredictable match
 *   - Low: Controlled, predictable match
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MomentumMetrics {
    
    @JsonProperty("drift")
    private BigDecimal drift; // Momentum direction (positive = home advantage)
    
    @JsonProperty("volatility")
    private BigDecimal volatility; // Match variability
    
    @JsonProperty("confidence")
    private BigDecimal confidence; // Statistical confidence (0-1)
    
    @JsonProperty("sampleSize")
    private Integer sampleSize; // Number of data points analyzed
    
    // Prior parameters (for transparency)
    @JsonProperty("priorMean")
    private BigDecimal priorMean;
    
    @JsonProperty("priorVariance")
    private BigDecimal priorVariance;
    
    // Posterior parameters
    @JsonProperty("posteriorMean")
    private BigDecimal posteriorMean;
    
    @JsonProperty("posteriorVariance")
    private BigDecimal posteriorVariance;
}
