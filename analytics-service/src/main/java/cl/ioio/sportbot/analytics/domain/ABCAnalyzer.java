package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.List;

/**
 * ABC Analyzer: Integrated ARIMA-Bayes-Carlo Analysis
 * 
 * Flow:
 * 1. ARIMA detects trends and structural breaks in match data
 * 2. Bayes updates momentum probabilities using ARIMA-informed priors
 * 3. Monte Carlo simulates outcomes with dynamic Bayesian probabilities
 * 4. Feedback loop: structural breaks trigger recalibration
 */
@ApplicationScoped
@Slf4j
public class ABCAnalyzer {
    
    @Inject
    GoalForecaster goalForecaster;
    
    @Inject
    MomentumAnalyzer momentumAnalyzer;
    
    @Inject
    MatchSimulator matchSimulator;
    
    /**
     * Perform integrated ABC analysis
     * 
     * @param possessionHistory possession percentage history
     * @param goalHistory cumulative goals history
     * @param currentHomeScore current home score
     * @param currentAwayScore current away score
     * @param minutesRemaining minutes remaining in match
     * @return Complete ABC analysis result
     */
    public ABCAnalysisResult analyze(
            List<BigDecimal> possessionHistory,
            List<BigDecimal> goalHistory,
            int currentHomeScore,
            int currentAwayScore,
            int minutesRemaining) {
        
        log.debug("Starting ABC analysis: possession={}, goals={}, minutes remaining={}", 
                possessionHistory.size(), goalHistory.size(), minutesRemaining);
        
        // Step 1: ARIMA Analysis - Detect trends and structural breaks
        GoalForecast goalForecast = goalForecaster.forecast(goalHistory);
        ARIMASignal arimaSignal = goalForecaster.extractTrendSignal(goalHistory);
        
        log.info("ARIMA Signal: trend={}, change detected={}, confidence={}", 
                arimaSignal.getTrend(), arimaSignal.isStructuralBreakDetected(), 
                arimaSignal.getConfidence());
        
        // Step 2: Bayesian Analysis - Update momentum with ARIMA-informed prior
        MomentumMetrics momentum = momentumAnalyzer.analyzeWithARIMAPrior(
                possessionHistory, arimaSignal);
        
        log.info("Bayesian Momentum: drift={}, volatility={}, ARIMA-adjusted={}", 
                momentum.getDrift(), momentum.getVolatility(), arimaSignal.getTrend());
        
        // Step 3: Monte Carlo Simulation - Use dynamic Bayesian probabilities
        MatchPrediction prediction = matchSimulator.simulate(
                currentHomeScore,
                currentAwayScore,
                momentum.getDrift().doubleValue(),
                momentum.getVolatility().doubleValue(),
                10000,
                minutesRemaining
        );
        
        log.info("Monte Carlo: P(Home)={}, P(Draw)={}, P(Away)={}", 
                prediction.getProbabilityHomeWin(), 
                prediction.getProbabilityDraw(), 
                prediction.getProbabilityAwayWin());
        
        // Step 4: Feedback Loop - Check if recalibration needed
        boolean needsRecalibration = arimaSignal.isStructuralBreakDetected() || 
                                     momentum.getVolatility().doubleValue() > 0.15;
        
        if (needsRecalibration) {
            log.warn("Structural break detected! Recalibration recommended. " +
                    "ARIMA break={}, High volatility={}", 
                    arimaSignal.isStructuralBreakDetected(),
                    momentum.getVolatility().doubleValue() > 0.15);
        }
        
        return ABCAnalysisResult.builder()
                .arimaSignal(arimaSignal)
                .momentumMetrics(momentum)
                .matchPrediction(prediction)
                .goalForecast(goalForecast)
                .needsRecalibration(needsRecalibration)
                .integrationConfidence(calculateIntegrationConfidence(arimaSignal, momentum))
                .build();
    }
    
    /**
     * Calculate overall confidence in the integrated analysis
     * Considers ARIMA confidence, Bayesian confidence, and structural stability
     */
    private BigDecimal calculateIntegrationConfidence(ARIMASignal arimaSignal, MomentumMetrics momentum) {
        double arimaConf = arimaSignal.getConfidence().doubleValue();
        double bayesConf = momentum.getConfidence().doubleValue();
        
        // Penalize if structural break detected (less confidence during transitions)
        double stabilityFactor = arimaSignal.isStructuralBreakDetected() ? 0.7 : 1.0;
        
        // Combined confidence (geometric mean with stability adjustment)
        double combined = Math.sqrt(arimaConf * bayesConf) * stabilityFactor;
        
        return BigDecimal.valueOf(combined);
    }
}
