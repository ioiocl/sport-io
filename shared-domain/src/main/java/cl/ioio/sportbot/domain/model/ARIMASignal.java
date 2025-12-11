package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * ARIMA trend signal for feeding into Bayesian analysis
 * Represents the "attack trend" signal that informs momentum updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ARIMASignal {
    
    @JsonProperty("trend")
    private BigDecimal trend;
    
    @JsonProperty("trendPercentage")
    private BigDecimal trendPercentage;
    
    @JsonProperty("structuralBreakDetected")
    private boolean structuralBreakDetected;
    
    @JsonProperty("breakMagnitude")
    private BigDecimal breakMagnitude;
    
    @JsonProperty("confidence")
    private BigDecimal confidence;
    
    @JsonProperty("volatility")
    private BigDecimal volatility;
    
    @JsonProperty("description")
    private String description;
}
