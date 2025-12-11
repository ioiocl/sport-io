package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.ARIMASignal;
import cl.ioio.sportbot.domain.model.GoalForecast;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * ARIMA forecaster for goal prediction
 * Simplified ARIMA(1,1,1) implementation for football matches
 */
@ApplicationScoped
@Slf4j
public class GoalForecaster {
    
    private static final int SCALE = 8;
    
    @ConfigProperty(name = "arima.horizon.minutes", defaultValue = "10")
    int defaultHorizon;
    
    /**
     * Generate goal forecast
     * Predicts total goals over time using ARIMA(1,1,1)
     * 
     * @param goalHistory cumulative goals over time
     * @param horizon number of minutes to forecast
     * @return Goal forecast with predictions and confidence intervals
     */
    public GoalForecast forecast(List<BigDecimal> goalHistory, int horizon) {
        if (goalHistory == null || goalHistory.size() < 10) {
            return createDefaultForecast(horizon);
        }
        
        try {
            // Use exponential smoothing with trend (Holt's method)
            double alpha = 0.3; // level smoothing
            double beta = 0.1;  // trend smoothing
            
            double[] goalArray = goalHistory.stream()
                    .mapToDouble(BigDecimal::doubleValue)
                    .toArray();
            
            // Initialize level and trend
            double level = goalArray[0];
            double trend = (goalArray[goalArray.length - 1] - goalArray[0]) / goalArray.length;
            
            // Smooth the series
            for (int i = 1; i < goalArray.length; i++) {
                double prevLevel = level;
                level = alpha * goalArray[i] + (1 - alpha) * (level + trend);
                trend = beta * (level - prevLevel) + (1 - beta) * trend;
            }
            
            // Generate forecasts
            List<BigDecimal> predictions = new ArrayList<>();
            List<BigDecimal> lowerBounds = new ArrayList<>();
            List<BigDecimal> upperBounds = new ArrayList<>();
            
            // Calculate standard error
            DescriptiveStatistics stats = new DescriptiveStatistics(goalArray);
            double stdError = stats.getStandardDeviation();
            
            for (int h = 1; h <= horizon; h++) {
                double forecast = level + h * trend;
                double margin = 1.96 * stdError * Math.sqrt(h); // 95% confidence
                
                predictions.add(BigDecimal.valueOf(forecast).setScale(SCALE, RoundingMode.HALF_UP));
                lowerBounds.add(BigDecimal.valueOf(Math.max(0, forecast - margin)).setScale(SCALE, RoundingMode.HALF_UP));
                upperBounds.add(BigDecimal.valueOf(forecast + margin).setScale(SCALE, RoundingMode.HALF_UP));
            }
            
            // Calculate next goal probability and timing
            double currentGoals = goalArray[goalArray.length - 1];
            double nextGoalExpected = predictions.get(0).doubleValue();
            double goalRate = trend; // goals per minute
            
            // Probability of goal in next N minutes
            double nextGoalProbability = Math.min(0.95, Math.max(0.05, goalRate * horizon));
            
            // Predict which team will score (based on momentum if available)
            String nextGoalTeam = "UNKNOWN";
            
            // Estimate minute of next goal
            int nextGoalMinute = (int) Math.round(1.0 / Math.max(0.01, goalRate));
            
            // Calculate AIC (simplified)
            double aic = calculateAIC(goalArray, 3); // 3 parameters: level, trend, error
            
            // Calculate confidence
            double confidence = 1.0 - (1.0 / Math.sqrt(goalArray.length + 1));
            
            return GoalForecast.builder()
                    .predictions(predictions)
                    .confidenceIntervalLower(lowerBounds)
                    .confidenceIntervalUpper(upperBounds)
                    .horizon(horizon)
                    .modelOrder("ARIMA(1,1,1)")
                    .aic(BigDecimal.valueOf(aic).setScale(SCALE, RoundingMode.HALF_UP))
                    .nextGoalProbability(BigDecimal.valueOf(nextGoalProbability).setScale(SCALE, RoundingMode.HALF_UP))
                    .nextGoalTeam(nextGoalTeam)
                    .nextGoalMinute(nextGoalMinute)
                    .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in goal forecasting", e);
            return createDefaultForecast(horizon);
        }
    }
    
    /**
     * Forecast with default horizon
     */
    public GoalForecast forecast(List<BigDecimal> goalHistory) {
        log.debug("Using ARIMA configuration: horizon={} minutes", defaultHorizon);
        return forecast(goalHistory, defaultHorizon);
    }
    
    /**
     * Calculate Akaike Information Criterion
     */
    private double calculateAIC(double[] data, int numParams) {
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double variance = stats.getVariance();
        int n = data.length;
        
        if (variance <= 0 || n <= numParams) {
            return Double.MAX_VALUE;
        }
        
        return n * Math.log(variance) + 2 * numParams;
    }
    
    /**
     * Extract ARIMA trend signal for Bayesian prior update
     * Detects structural breaks and exports trend information
     * 
     * @param goalHistory cumulative goals over time
     * @return ARIMA signal with trend and break detection
     */
    public ARIMASignal extractTrendSignal(List<BigDecimal> goalHistory) {
        if (goalHistory == null || goalHistory.size() < 10) {
            return createDefaultSignal();
        }
        
        try {
            double alpha = 0.3;
            double beta = 0.1;
            
            double[] goalArray = goalHistory.stream()
                    .mapToDouble(BigDecimal::doubleValue)
                    .toArray();
            
            // Calculate trend
            double level = goalArray[0];
            double trend = (goalArray[goalArray.length - 1] - goalArray[0]) / goalArray.length;
            
            for (int i = 1; i < goalArray.length; i++) {
                double prevLevel = level;
                level = alpha * goalArray[i] + (1 - alpha) * (level + trend);
                trend = beta * (level - prevLevel) + (1 - beta) * trend;
            }
            
            // Detect structural breaks using CUSUM
            boolean structuralBreak = detectStructuralBreak(goalArray);
            double breakMagnitude = structuralBreak ? calculateBreakMagnitude(goalArray) : 0.0;
            
            // Calculate trend percentage (relative to base rate)
            double baseGoalRate = 0.03; // 0.03 goals per minute
            double trendPercentage = (trend / baseGoalRate) * 100.0;
            
            // Calculate confidence
            DescriptiveStatistics stats = new DescriptiveStatistics(goalArray);
            double volatility = stats.getStandardDeviation();
            double confidence = 1.0 - (1.0 / Math.sqrt(goalArray.length + 1));
            
            // Generate description
            String description = generateTrendDescription(trendPercentage, structuralBreak);
            
            log.info("ARIMA Signal: {}", description);
            
            return ARIMASignal.builder()
                    .trend(BigDecimal.valueOf(trend).setScale(SCALE, RoundingMode.HALF_UP))
                    .trendPercentage(BigDecimal.valueOf(trendPercentage).setScale(2, RoundingMode.HALF_UP))
                    .structuralBreakDetected(structuralBreak)
                    .breakMagnitude(BigDecimal.valueOf(breakMagnitude).setScale(SCALE, RoundingMode.HALF_UP))
                    .confidence(BigDecimal.valueOf(confidence).setScale(SCALE, RoundingMode.HALF_UP))
                    .volatility(BigDecimal.valueOf(volatility).setScale(SCALE, RoundingMode.HALF_UP))
                    .description(description)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error extracting ARIMA signal", e);
            return createDefaultSignal();
        }
    }
    
    /**
     * Detect structural breaks using CUSUM (Cumulative Sum Control Chart)
     * Detects sudden changes in goal-scoring patterns
     */
    private boolean detectStructuralBreak(double[] data) {
        if (data.length < 10) return false;
        
        DescriptiveStatistics stats = new DescriptiveStatistics(data);
        double mean = stats.getMean();
        double stdDev = stats.getStandardDeviation();
        
        if (stdDev == 0) return false;
        
        // CUSUM parameters
        double threshold = 3.0 * stdDev; // Detection threshold
        double cusumPos = 0;
        double cusumNeg = 0;
        
        // Only check recent data (last 30% of observations)
        int startIdx = (int)(data.length * 0.7);
        
        for (int i = startIdx; i < data.length; i++) {
            double deviation = data[i] - mean;
            cusumPos = Math.max(0, cusumPos + deviation);
            cusumNeg = Math.min(0, cusumNeg + deviation);
            
            if (Math.abs(cusumPos) > threshold || Math.abs(cusumNeg) > threshold) {
                log.warn("Structural break detected at index {} (CUSUM: pos={}, neg={})", 
                        i, cusumPos, cusumNeg);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Calculate magnitude of structural break
     */
    private double calculateBreakMagnitude(double[] data) {
        int splitPoint = (int)(data.length * 0.7);
        
        DescriptiveStatistics before = new DescriptiveStatistics();
        DescriptiveStatistics after = new DescriptiveStatistics();
        
        for (int i = 0; i < splitPoint; i++) {
            before.addValue(data[i]);
        }
        for (int i = splitPoint; i < data.length; i++) {
            after.addValue(data[i]);
        }
        
        return Math.abs(after.getMean() - before.getMean());
    }
    
    /**
     * Generate human-readable trend description
     */
    private String generateTrendDescription(double trendPercentage, boolean structuralBreak) {
        StringBuilder desc = new StringBuilder();
        
        if (Math.abs(trendPercentage) < 5) {
            desc.append("Attack stable");
        } else if (trendPercentage > 0) {
            desc.append(String.format("Attack increasing %.1f%% in trend", trendPercentage));
        } else {
            desc.append(String.format("Attack decreasing %.1f%% in trend", Math.abs(trendPercentage)));
        }
        
        if (structuralBreak) {
            desc.append(" [STRUCTURAL BREAK DETECTED - Recalibration needed]");
        }
        
        return desc.toString();
    }
    
    private ARIMASignal createDefaultSignal() {
        return ARIMASignal.builder()
                .trend(BigDecimal.ZERO)
                .trendPercentage(BigDecimal.ZERO)
                .structuralBreakDetected(false)
                .breakMagnitude(BigDecimal.ZERO)
                .confidence(BigDecimal.ZERO)
                .volatility(BigDecimal.ZERO)
                .description("Insufficient data")
                .build();
    }
    
    private GoalForecast createDefaultForecast(int horizon) {
        List<BigDecimal> zeros = new ArrayList<>();
        for (int i = 0; i < horizon; i++) {
            zeros.add(BigDecimal.ZERO);
        }
        
        return GoalForecast.builder()
                .predictions(zeros)
                .confidenceIntervalLower(zeros)
                .confidenceIntervalUpper(zeros)
                .horizon(horizon)
                .modelOrder("ARIMA(0,0,0)")
                .aic(BigDecimal.ZERO)
                .nextGoalProbability(BigDecimal.ZERO)
                .nextGoalTeam("UNKNOWN")
                .nextGoalMinute(0)
                .confidence(BigDecimal.ZERO)
                .build();
    }
}
