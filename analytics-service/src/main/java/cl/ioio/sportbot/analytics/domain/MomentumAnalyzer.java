package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.ARIMASignal;
import cl.ioio.sportbot.domain.model.MomentumMetrics;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Bayesian analysis for match momentum estimation
 * Analyzes possession percentage to determine momentum and variability
 * Domain service implementing Bayesian inference
 */
@ApplicationScoped
@Slf4j
public class MomentumAnalyzer {
    
    private static final int SCALE = 8;
    
    /**
     * Calculate momentum metrics with ARIMA-informed prior
     * ARIMA trend signal updates the Bayesian prior mean
     * 
     * @param possessionHistory historical possession percentages (home team)
     * @param arimaSignal ARIMA trend signal from goal forecasting
     * @return Momentum metrics with ARIMA-adjusted drift and volatility
     */
    public MomentumMetrics analyzeWithARIMAPrior(List<BigDecimal> possessionHistory, ARIMASignal arimaSignal) {
        if (possessionHistory == null || possessionHistory.size() < 2) {
            return createDefaultMetrics();
        }
        
        try {
            // Calculate possession changes
            double[] changes = calculatePossessionChanges(possessionHistory);
            
            if (changes.length == 0) {
                return createDefaultMetrics();
            }
            
            DescriptiveStatistics stats = new DescriptiveStatistics(changes);
            
            // ARIMA-informed prior parameters
            // Prior mean comes from ARIMA trend signal
            double arimaTrend = arimaSignal.getTrend().doubleValue();
            double priorMean = arimaTrend * 10.0; // Scale ARIMA trend to possession change scale
            
            // Adjust prior variance based on ARIMA confidence
            double arimaConfidence = arimaSignal.getConfidence().doubleValue();
            double priorVariance = 0.01 * (2.0 - arimaConfidence); // Higher confidence = tighter prior
            double priorN = 1.0 + arimaConfidence; // More weight to prior if ARIMA is confident
            
            // Sample statistics
            double sampleMean = stats.getMean();
            double sampleVariance = stats.getVariance();
            int sampleSize = changes.length;
            
            // Posterior parameters (Bayesian update)
            double posteriorN = priorN + sampleSize;
            double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
            
            // Posterior variance
            double posteriorVariance = ((priorN * priorVariance + 
                    sampleSize * sampleVariance + 
                    (priorN * sampleSize / posteriorN) * 
                    Math.pow(sampleMean - priorMean, 2)) / posteriorN);
            
            // Adjust confidence based on structural breaks
            double confidence = 1.0 - (1.0 / Math.sqrt(sampleSize + 1));
            if (arimaSignal.isStructuralBreakDetected()) {
                confidence *= 0.7; // Reduce confidence during structural breaks
            }
            
            double drift = posteriorMean;
            double volatility = Math.sqrt(posteriorVariance);
            
            log.info("Bayesian update: ARIMA prior mean={}, posterior mean={}, confidence={}", 
                    priorMean, posteriorMean, confidence);
            
            return MomentumMetrics.builder()
                    .drift(BigDecimal.valueOf(drift).setScale(SCALE, RoundingMode.HALF_UP))
                    .volatility(BigDecimal.valueOf(volatility).setScale(SCALE, RoundingMode.HALF_UP))
                    .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                    .sampleSize(sampleSize)
                    .priorMean(BigDecimal.valueOf(priorMean).setScale(SCALE, RoundingMode.HALF_UP))
                    .priorVariance(BigDecimal.valueOf(priorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                    .posteriorMean(BigDecimal.valueOf(posteriorMean).setScale(SCALE, RoundingMode.HALF_UP))
                    .posteriorVariance(BigDecimal.valueOf(posteriorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in momentum analysis with ARIMA prior", e);
            return createDefaultMetrics();
        }
    }
    
    /**
     * Calculate momentum metrics from possession series
     * Uses conjugate prior for normal distribution
     * 
     * @param possessionHistory historical possession percentages (home team)
     * @return Momentum metrics with drift and volatility
     */
    public MomentumMetrics analyze(List<BigDecimal> possessionHistory) {
        if (possessionHistory == null || possessionHistory.size() < 2) {
            return createDefaultMetrics();
        }
        
        try {
            // Calculate possession changes (similar to returns in finance)
            double[] changes = calculatePossessionChanges(possessionHistory);
            
            if (changes.length == 0) {
                return createDefaultMetrics();
            }
            
            DescriptiveStatistics stats = new DescriptiveStatistics(changes);
            
            // Prior parameters (weakly informative)
            // Prior mean = 0 (neutral momentum)
            double priorMean = 0.0;
            double priorVariance = 0.01;
            double priorN = 1.0;
            
            // Sample statistics
            double sampleMean = stats.getMean();
            double sampleVariance = stats.getVariance();
            int sampleSize = changes.length;
            
            // Posterior parameters (Bayesian update)
            double posteriorN = priorN + sampleSize;
            double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
            
            // Posterior variance (accounting for uncertainty)
            double posteriorVariance = ((priorN * priorVariance + 
                    sampleSize * sampleVariance + 
                    (priorN * sampleSize / posteriorN) * 
                    Math.pow(sampleMean - priorMean, 2)) / posteriorN);
            
            // Calculate confidence (based on sample size and variance)
            double confidence = 1.0 - (1.0 / Math.sqrt(sampleSize + 1));
            
            // Drift = momentum direction (positive = home advantage)
            // Volatility = match variability
            double drift = posteriorMean;
            double volatility = Math.sqrt(posteriorVariance);
            
            return MomentumMetrics.builder()
                    .drift(BigDecimal.valueOf(drift).setScale(SCALE, RoundingMode.HALF_UP))
                    .volatility(BigDecimal.valueOf(volatility).setScale(SCALE, RoundingMode.HALF_UP))
                    .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                    .sampleSize(sampleSize)
                    .priorMean(BigDecimal.valueOf(priorMean).setScale(SCALE, RoundingMode.HALF_UP))
                    .priorVariance(BigDecimal.valueOf(priorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                    .posteriorMean(BigDecimal.valueOf(posteriorMean).setScale(SCALE, RoundingMode.HALF_UP))
                    .posteriorVariance(BigDecimal.valueOf(posteriorVariance).setScale(SCALE, RoundingMode.HALF_UP))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in momentum analysis", e);
            return createDefaultMetrics();
        }
    }
    
    /**
     * Calculate possession changes from possession history
     * Similar to log returns in finance
     */
    private double[] calculatePossessionChanges(List<BigDecimal> possessionHistory) {
        double[] changes = new double[possessionHistory.size() - 1];
        
        for (int i = 1; i < possessionHistory.size(); i++) {
            double currentPossession = possessionHistory.get(i).doubleValue();
            double previousPossession = possessionHistory.get(i - 1).doubleValue();
            
            // Calculate change in possession
            // Normalize to [-1, 1] range
            changes[i - 1] = (currentPossession - previousPossession) / 100.0;
        }
        
        return changes;
    }
    
    private MomentumMetrics createDefaultMetrics() {
        return MomentumMetrics.builder()
                .drift(BigDecimal.ZERO)
                .volatility(BigDecimal.ZERO)
                .confidence(BigDecimal.ZERO)
                .sampleSize(0)
                .priorMean(BigDecimal.ZERO)
                .priorVariance(BigDecimal.valueOf(0.01))
                .posteriorMean(BigDecimal.ZERO)
                .posteriorVariance(BigDecimal.ZERO)
                .build();
    }
}
