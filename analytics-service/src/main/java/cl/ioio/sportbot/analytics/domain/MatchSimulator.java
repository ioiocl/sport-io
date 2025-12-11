package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.MatchPrediction;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.PoissonDistribution;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Monte Carlo simulator for match outcome prediction
 * Simulates final scores using Poisson distribution for goals
 */
@ApplicationScoped
@Slf4j
public class MatchSimulator {
    
    private static final int SCALE = 8;
    
    @ConfigProperty(name = "monte.carlo.simulations", defaultValue = "10000")
    int defaultSimulations;
    
    @ConfigProperty(name = "monte.carlo.horizon.minutes", defaultValue = "45")
    int defaultHorizon;
    
    private final Random random = new Random();
    
    /**
     * Run Monte Carlo simulation for match outcome
     * 
     * @param currentHomeScore current home score
     * @param currentAwayScore current away score
     * @param momentum match momentum (positive = home advantage)
     * @param variability match variability
     * @param simulations number of simulations
     * @param minutesRemaining minutes remaining in match
     * @return Match prediction with outcome probabilities
     */
    public MatchPrediction simulate(
            int currentHomeScore,
            int currentAwayScore,
            double momentum,
            double variability,
            int simulations,
            int minutesRemaining) {
        
        try {
            // Calculate expected goals per minute based on momentum
            // Base rate: ~0.03 goals per minute (2.7 goals per 90 minutes)
            double baseGoalRate = 0.03;
            
            // Adjust for momentum (positive = home advantage)
            double homeGoalRate = baseGoalRate * (1.0 + momentum);
            double awayGoalRate = baseGoalRate * (1.0 - momentum);
            
            // Expected goals in remaining time
            double homeExpectedGoals = homeGoalRate * minutesRemaining;
            double awayExpectedGoals = awayGoalRate * minutesRemaining;
            
            // Poisson distributions for goal scoring
            PoissonDistribution homeDist = new PoissonDistribution(homeExpectedGoals);
            PoissonDistribution awayDist = new PoissonDistribution(awayExpectedGoals);
            
            // Track outcomes
            int homeWins = 0;
            int draws = 0;
            int awayWins = 0;
            int moreGoals = 0;
            
            Map<String, Integer> scoreFrequency = new HashMap<>();
            List<Integer> totalGoalsList = new ArrayList<>();
            
            // Run simulations
            for (int sim = 0; sim < simulations; sim++) {
                int homeGoals = homeDist.sample();
                int awayGoals = awayDist.sample();
                
                int finalHomeScore = currentHomeScore + homeGoals;
                int finalAwayScore = currentAwayScore + awayGoals;
                
                // Track outcomes
                if (finalHomeScore > finalAwayScore) {
                    homeWins++;
                } else if (finalHomeScore < finalAwayScore) {
                    awayWins++;
                } else {
                    draws++;
                }
                
                // Track if more goals will be scored
                if (homeGoals + awayGoals > 0) {
                    moreGoals++;
                }
                
                // Track score frequency
                String score = finalHomeScore + "-" + finalAwayScore;
                scoreFrequency.put(score, scoreFrequency.getOrDefault(score, 0) + 1);
                
                // Track total goals
                totalGoalsList.add(finalHomeScore + finalAwayScore);
            }
            
            // Calculate probabilities
            double probabilityHomeWin = (double) homeWins / simulations;
            double probabilityDraw = (double) draws / simulations;
            double probabilityAwayWin = (double) awayWins / simulations;
            double probabilityMoreGoals = (double) moreGoals / simulations;
            
            // Find most likely scores
            List<MatchPrediction.ScoreProbability> mostLikelyScores = scoreFrequency.entrySet().stream()
                    .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                    .limit(5)
                    .map(e -> MatchPrediction.ScoreProbability.builder()
                            .score(e.getKey())
                            .probability(BigDecimal.valueOf((double) e.getValue() / simulations)
                                    .setScale(SCALE, RoundingMode.HALF_UP))
                            .build())
                    .collect(Collectors.toList());
            
            // Expected final score (most likely)
            String expectedFinalScore = mostLikelyScores.isEmpty() ? 
                    currentHomeScore + "-" + currentAwayScore : 
                    mostLikelyScores.get(0).getScore();
            
            // Calculate expected total goals
            double expectedTotalGoals = totalGoalsList.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
            
            // Calculate comeback/hold lead probabilities
            double probabilityComeback = 0.0;
            double probabilityHoldLead = 0.0;
            
            if (currentHomeScore < currentAwayScore) {
                // Home team is losing
                probabilityComeback = probabilityHomeWin;
            } else if (currentHomeScore > currentAwayScore) {
                // Home team is winning
                probabilityHoldLead = probabilityHomeWin;
            }
            
            // Calculate percentiles for goal distribution
            totalGoalsList.sort(Integer::compareTo);
            List<MatchPrediction.Percentile> percentiles = List.of(
                    createPercentile(5, totalGoalsList.get((int)(simulations * 0.05))),
                    createPercentile(25, totalGoalsList.get((int)(simulations * 0.25))),
                    createPercentile(50, totalGoalsList.get((int)(simulations * 0.50))),
                    createPercentile(75, totalGoalsList.get((int)(simulations * 0.75))),
                    createPercentile(95, totalGoalsList.get((int)(simulations * 0.95)))
            );
            
            return MatchPrediction.builder()
                    .simulations(simulations)
                    .probabilityHomeWin(BigDecimal.valueOf(probabilityHomeWin).setScale(SCALE, RoundingMode.HALF_UP))
                    .probabilityDraw(BigDecimal.valueOf(probabilityDraw).setScale(SCALE, RoundingMode.HALF_UP))
                    .probabilityAwayWin(BigDecimal.valueOf(probabilityAwayWin).setScale(SCALE, RoundingMode.HALF_UP))
                    .expectedFinalScore(expectedFinalScore)
                    .mostLikelyScores(mostLikelyScores)
                    .probabilityMoreGoals(BigDecimal.valueOf(probabilityMoreGoals).setScale(SCALE, RoundingMode.HALF_UP))
                    .expectedTotalGoals(BigDecimal.valueOf(expectedTotalGoals).setScale(SCALE, RoundingMode.HALF_UP))
                    .probabilityComeback(BigDecimal.valueOf(probabilityComeback).setScale(SCALE, RoundingMode.HALF_UP))
                    .probabilityHoldLead(BigDecimal.valueOf(probabilityHoldLead).setScale(SCALE, RoundingMode.HALF_UP))
                    .percentiles(percentiles)
                    .build();
                    
        } catch (Exception e) {
            log.error("Error in match simulation", e);
            return createDefaultResults(simulations);
        }
    }
    
    /**
     * Simulate with default parameters
     */
    public MatchPrediction simulate(int currentHomeScore, int currentAwayScore, 
                                    double momentum, double variability) {
        return simulate(currentHomeScore, currentAwayScore, momentum, variability, 
                       defaultSimulations, defaultHorizon);
    }
    
    private MatchPrediction.Percentile createPercentile(int level, double totalGoals) {
        return MatchPrediction.Percentile.builder()
                .level(level)
                .totalGoals(BigDecimal.valueOf(totalGoals).setScale(SCALE, RoundingMode.HALF_UP))
                .build();
    }
    
    private MatchPrediction createDefaultResults(int simulations) {
        log.info("Using Monte Carlo configuration: simulations={}, horizon={} minutes", 
                defaultSimulations, defaultHorizon);
        
        return MatchPrediction.builder()
                .simulations(simulations)
                .probabilityHomeWin(BigDecimal.valueOf(0.33))
                .probabilityDraw(BigDecimal.valueOf(0.34))
                .probabilityAwayWin(BigDecimal.valueOf(0.33))
                .expectedFinalScore("0-0")
                .mostLikelyScores(new ArrayList<>())
                .probabilityMoreGoals(BigDecimal.valueOf(0.5))
                .expectedTotalGoals(BigDecimal.ZERO)
                .probabilityComeback(BigDecimal.ZERO)
                .probabilityHoldLead(BigDecimal.ZERO)
                .percentiles(new ArrayList<>())
                .build();
    }
}
