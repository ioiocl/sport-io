package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.model.MatchKPIs;
import cl.ioio.sportbot.domain.model.TeamStatistics;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * KPI Calculator that consumes enriched MatchEvent history
 * and produces comprehensive match analytics
 */
@ApplicationScoped
@Slf4j
public class KPICalculator {
    
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int SCALE = 4;
    
    /**
     * Calculate all KPIs from match event history
     */
    public MatchKPIs calculate(List<MatchEvent> history) {
        if (history == null || history.isEmpty()) {
            return MatchKPIs.builder().build();
        }
        
        MatchEvent latest = history.get(history.size() - 1);
        int currentMinute = latest.getMinute() != null ? latest.getMinute() : 0;
        
        // Get team statistics
        TeamStatistics homeStats = latest.getHomeStats();
        TeamStatistics awayStats = latest.getAwayStats();
        
        // Build KPIs
        MatchKPIs.MatchKPIsBuilder builder = MatchKPIs.builder();
        
        // Calculate possession momentum from history
        builder.possessionMomentum(calculatePossessionMomentum(history));
        
        // Calculate territory metrics
        if (homeStats != null && awayStats != null) {
            builder.homeAttackingTerritory(calculateAttackingTerritory(homeStats));
            builder.awayAttackingTerritory(calculateAttackingTerritory(awayStats));
            
            // Shot pressure and quality
            builder.homeShotPressure(calculateShotPressure(homeStats, currentMinute));
            builder.awayShotPressure(calculateShotPressure(awayStats, currentMinute));
            builder.homeShotQuality(calculateShotQuality(homeStats));
            builder.awayShotQuality(calculateShotQuality(awayStats));
            
            // Playing style indicators
            builder.homeDirectSpeed(calculateDirectSpeed(homeStats));
            builder.awayDirectSpeed(calculateDirectSpeed(awayStats));
            builder.homeHighPress(calculateHighPress(homeStats, currentMinute));
            builder.awayHighPress(calculateHighPress(awayStats, currentMinute));
            builder.homeCounterThreat(calculateCounterThreat(homeStats));
            builder.awayCounterThreat(calculateCounterThreat(awayStats));
            
            // Discipline metrics
            builder.homeDisciplinaryBalance(calculateDisciplinaryBalance(homeStats));
            builder.awayDisciplinaryBalance(calculateDisciplinaryBalance(awayStats));
            builder.homeFoulIntensity(calculateFoulIntensity(homeStats, currentMinute));
            builder.awayFoulIntensity(calculateFoulIntensity(awayStats, currentMinute));
            
            // Goalkeeper metrics (home keeper faces away shots and vice versa)
            builder.homeKeeperPressure(calculateKeeperPressure(awayStats, homeStats));
            builder.awayKeeperPressure(calculateKeeperPressure(homeStats, awayStats));
            builder.homeKeeperEfficiency(calculateKeeperEfficiency(homeStats, awayStats));
            builder.awayKeeperEfficiency(calculateKeeperEfficiency(awayStats, homeStats));
            
            // Alert flags
            BigDecimal homeHighPress = calculateHighPress(homeStats, currentMinute);
            BigDecimal awayHighPress = calculateHighPress(awayStats, currentMinute);
            builder.homeHighPressAlert(homeHighPress.compareTo(new BigDecimal("0.7")) > 0);
            builder.awayHighPressAlert(awayHighPress.compareTo(new BigDecimal("0.7")) > 0);
            
            BigDecimal homeFoulIntensity = calculateFoulIntensity(homeStats, currentMinute);
            BigDecimal awayFoulIntensity = calculateFoulIntensity(awayStats, currentMinute);
            builder.homeCardRiskAlert(homeFoulIntensity.compareTo(new BigDecimal("0.3")) > 0);
            builder.awayCardRiskAlert(awayFoulIntensity.compareTo(new BigDecimal("0.3")) > 0);
            
            BigDecimal homeShotPressure = calculateShotPressure(homeStats, currentMinute);
            BigDecimal awayShotPressure = calculateShotPressure(awayStats, currentMinute);
            BigDecimal homeShotQuality = calculateShotQuality(homeStats);
            BigDecimal awayShotQuality = calculateShotQuality(awayStats);
            builder.homeGoalImminentAlert(
                homeShotPressure.compareTo(new BigDecimal("0.6")) > 0 && 
                homeShotQuality.compareTo(new BigDecimal("0.5")) > 0);
            builder.awayGoalImminentAlert(
                awayShotPressure.compareTo(new BigDecimal("0.6")) > 0 && 
                awayShotQuality.compareTo(new BigDecimal("0.5")) > 0);
            
            // Half comparison (momentum shift)
            builder.homeMomentumShift(calculateMomentumShift(homeStats));
            builder.awayMomentumShift(calculateMomentumShift(awayStats));
        }
        
        // Build timelines from history
        builder.possessionTimeline(buildPossessionTimeline(history));
        builder.shotPressureTimeline(buildShotPressureTimeline(history));
        
        return builder.build();
    }
    
    /**
     * Calculate possession momentum from history
     * Uses linear regression on possession values
     */
    private BigDecimal calculatePossessionMomentum(List<MatchEvent> history) {
        if (history.size() < 3) {
            return BigDecimal.ZERO;
        }
        
        // Get last 10 possession values (or all if less)
        int start = Math.max(0, history.size() - 10);
        List<BigDecimal> possessionValues = new ArrayList<>();
        
        for (int i = start; i < history.size(); i++) {
            MatchEvent event = history.get(i);
            if (event.getPossession() != null) {
                possessionValues.add(event.getPossession());
            }
        }
        
        if (possessionValues.size() < 2) {
            return BigDecimal.ZERO;
        }
        
        // Simple momentum: (last - first) / count
        BigDecimal first = possessionValues.get(0);
        BigDecimal last = possessionValues.get(possessionValues.size() - 1);
        BigDecimal change = last.subtract(first);
        
        // Normalize to -1 to 1 range (assuming max change is 50%)
        return change.divide(new BigDecimal("50"), SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate attacking territory index
     * Weighted combination of shots inside box, corners, offsides
     */
    private BigDecimal calculateAttackingTerritory(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        int shotsInsideBox = stats.getShotsInsideBox() != null ? stats.getShotsInsideBox() : 0;
        int corners = stats.getCornerKicks() != null ? stats.getCornerKicks() : 0;
        int offsides = stats.getOffsides() != null ? stats.getOffsides() : 0;
        
        // Weighted score: shots inside box (3x) + corners (2x) + offsides (1x)
        double score = (shotsInsideBox * 3.0) + (corners * 2.0) + (offsides * 1.0);
        
        // Normalize to 0-1 range (assuming max score of 30)
        return new BigDecimal(Math.min(1.0, score / 30.0)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate shot pressure index
     * Shots per minute weighted by quality
     */
    private BigDecimal calculateShotPressure(TeamStatistics stats, int minute) {
        if (stats == null || minute <= 0) return BigDecimal.ZERO;
        
        int totalShots = stats.getTotalShots() != null ? stats.getTotalShots() : 0;
        int shotsOnTarget = stats.getShotsOnGoal() != null ? stats.getShotsOnGoal() : 0;
        int shotsInsideBox = stats.getShotsInsideBox() != null ? stats.getShotsInsideBox() : 0;
        
        // Weighted shots: on target (2x) + inside box (1.5x) + other (1x)
        double weightedShots = (shotsOnTarget * 2.0) + (shotsInsideBox * 1.5) + 
                              ((totalShots - shotsOnTarget - shotsInsideBox) * 1.0);
        
        // Per minute rate, normalized to 0-1 (assuming max 0.5 weighted shots/min)
        double rate = weightedShots / minute;
        return new BigDecimal(Math.min(1.0, rate / 0.5)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate shot quality mix
     * Ratio of quality shots to total shots
     */
    private BigDecimal calculateShotQuality(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        int totalShots = stats.getTotalShots() != null ? stats.getTotalShots() : 0;
        if (totalShots == 0) return BigDecimal.ZERO;
        
        int shotsOnTarget = stats.getShotsOnGoal() != null ? stats.getShotsOnGoal() : 0;
        int shotsInsideBox = stats.getShotsInsideBox() != null ? stats.getShotsInsideBox() : 0;
        
        // Quality = (on target + inside box) / (2 * total) - normalized to 0-1
        double quality = (shotsOnTarget + shotsInsideBox) / (2.0 * totalShots);
        return new BigDecimal(Math.min(1.0, quality)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate direct speed indicator
     * Pass accuracy combined with forward progression
     */
    private BigDecimal calculateDirectSpeed(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        BigDecimal passAccuracy = stats.getPassAccuracy() != null ? stats.getPassAccuracy() : BigDecimal.ZERO;
        int totalPasses = stats.getTotalPasses() != null ? stats.getTotalPasses() : 0;
        int shotsInsideBox = stats.getShotsInsideBox() != null ? stats.getShotsInsideBox() : 0;
        
        if (totalPasses == 0) return BigDecimal.ZERO;
        
        // Direct speed = pass accuracy * (shots inside box / passes) factor
        // Higher shots inside box per pass = more direct
        double directFactor = 1.0 + (shotsInsideBox * 10.0 / totalPasses);
        double speed = passAccuracy.doubleValue() / 100.0 * directFactor;
        
        return new BigDecimal(Math.min(1.0, speed)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate high press indicator
     * Based on fouls, tackles in opponent's half
     */
    private BigDecimal calculateHighPress(TeamStatistics stats, int minute) {
        if (stats == null || minute <= 0) return BigDecimal.ZERO;
        
        int fouls = stats.getFouls() != null ? stats.getFouls() : 0;
        int offsides = stats.getOffsides() != null ? stats.getOffsides() : 0;
        BigDecimal possession = stats.getPossession() != null ? stats.getPossession() : BigDecimal.ZERO;
        
        // High press = fouls/min * (100 - possession)/100 + offsides caught factor
        double foulsPerMin = (double) fouls / minute;
        double possessionFactor = (100 - possession.doubleValue()) / 100.0;
        double pressScore = (foulsPerMin * 5.0 * possessionFactor) + (offsides * 0.1);
        
        return new BigDecimal(Math.min(1.0, pressScore)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate counter threat proxy
     * Based on offsides and shot efficiency
     */
    private BigDecimal calculateCounterThreat(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        int offsides = stats.getOffsides() != null ? stats.getOffsides() : 0;
        int totalShots = stats.getTotalShots() != null ? stats.getTotalShots() : 0;
        int shotsOnTarget = stats.getShotsOnGoal() != null ? stats.getShotsOnGoal() : 0;
        BigDecimal possession = stats.getPossession() != null ? stats.getPossession() : BigDecimal.ZERO;
        
        // Counter threat = offsides (forward runs) + shot efficiency when possession is low
        double shotEfficiency = totalShots > 0 ? (double) shotsOnTarget / totalShots : 0;
        double lowPossessionFactor = possession.doubleValue() < 50 ? 1.5 : 1.0;
        double threat = (offsides * 0.15 + shotEfficiency * 0.5) * lowPossessionFactor;
        
        return new BigDecimal(Math.min(1.0, threat)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate disciplinary balance
     * Cards relative to fouls
     */
    private BigDecimal calculateDisciplinaryBalance(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        int fouls = stats.getFouls() != null ? stats.getFouls() : 0;
        int yellowCards = stats.getYellowCards() != null ? stats.getYellowCards() : 0;
        int redCards = stats.getRedCards() != null ? stats.getRedCards() : 0;
        
        if (fouls == 0) return BigDecimal.ZERO;
        
        // Cards per foul (yellow = 1, red = 3)
        double cardScore = (yellowCards + redCards * 3.0) / fouls;
        return new BigDecimal(Math.min(1.0, cardScore)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate foul intensity
     * Fouls per minute
     */
    private BigDecimal calculateFoulIntensity(TeamStatistics stats, int minute) {
        if (stats == null || minute <= 0) return BigDecimal.ZERO;
        
        int fouls = stats.getFouls() != null ? stats.getFouls() : 0;
        
        // Fouls per minute, normalized (0.5 fouls/min = 1.0)
        double intensity = (double) fouls / minute / 0.5;
        return new BigDecimal(Math.min(1.0, intensity)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate keeper pressure
     * Shots faced by the keeper
     */
    private BigDecimal calculateKeeperPressure(TeamStatistics opponentStats, TeamStatistics ownStats) {
        if (opponentStats == null) return BigDecimal.ZERO;
        
        int shotsOnTarget = opponentStats.getShotsOnGoal() != null ? opponentStats.getShotsOnGoal() : 0;
        int shotsInsideBox = opponentStats.getShotsInsideBox() != null ? opponentStats.getShotsInsideBox() : 0;
        
        // Pressure = shots on target + shots inside box weighted
        double pressure = (shotsOnTarget * 2.0 + shotsInsideBox) / 15.0;
        return new BigDecimal(Math.min(1.0, pressure)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate keeper efficiency
     * Saves / shots on target faced
     */
    private BigDecimal calculateKeeperEfficiency(TeamStatistics ownStats, TeamStatistics opponentStats) {
        if (ownStats == null || opponentStats == null) return BigDecimal.ZERO;
        
        int saves = ownStats.getGoalkeeperSaves() != null ? ownStats.getGoalkeeperSaves() : 0;
        int shotsOnTarget = opponentStats.getShotsOnGoal() != null ? opponentStats.getShotsOnGoal() : 0;
        
        if (shotsOnTarget == 0) return BigDecimal.ONE; // No shots = perfect efficiency
        
        double efficiency = (double) saves / shotsOnTarget;
        return new BigDecimal(Math.min(1.0, efficiency)).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Calculate momentum shift between halves
     */
    private BigDecimal calculateMomentumShift(TeamStatistics stats) {
        if (stats == null) return BigDecimal.ZERO;
        
        TeamStatistics.HalfStatistics first = stats.getFirstHalfStats();
        TeamStatistics.HalfStatistics second = stats.getSecondHalfStats();
        
        if (first == null || second == null) return BigDecimal.ZERO;
        
        // Compare possession and shots between halves
        BigDecimal firstPoss = first.getPossession() != null ? first.getPossession() : BigDecimal.ZERO;
        BigDecimal secondPoss = second.getPossession() != null ? second.getPossession() : BigDecimal.ZERO;
        
        int firstShots = first.getTotalShots() != null ? first.getTotalShots() : 0;
        int secondShots = second.getTotalShots() != null ? second.getTotalShots() : 0;
        
        // Momentum shift = possession change + shot change
        double possChange = (secondPoss.doubleValue() - firstPoss.doubleValue()) / 50.0;
        double shotChange = firstShots > 0 ? (secondShots - firstShots) / (double) firstShots : 0;
        
        double shift = (possChange + shotChange) / 2.0;
        return new BigDecimal(Math.max(-1.0, Math.min(1.0, shift))).setScale(SCALE, RoundingMode.HALF_UP);
    }
    
    /**
     * Build possession timeline from history
     */
    private List<MatchKPIs.TimelinePoint> buildPossessionTimeline(List<MatchEvent> history) {
        List<MatchKPIs.TimelinePoint> timeline = new ArrayList<>();
        
        for (MatchEvent event : history) {
            if (event.getMinute() != null && event.getPossession() != null) {
                BigDecimal homePoss = event.getPossession();
                BigDecimal awayPoss = HUNDRED.subtract(homePoss);
                
                timeline.add(MatchKPIs.TimelinePoint.builder()
                        .minute(event.getMinute())
                        .homeValue(homePoss)
                        .awayValue(awayPoss)
                        .build());
            }
        }
        
        return timeline;
    }
    
    /**
     * Build shot pressure timeline from history
     */
    private List<MatchKPIs.TimelinePoint> buildShotPressureTimeline(List<MatchEvent> history) {
        List<MatchKPIs.TimelinePoint> timeline = new ArrayList<>();
        
        for (MatchEvent event : history) {
            if (event.getMinute() != null && event.getMinute() > 0) {
                TeamStatistics homeStats = event.getHomeStats();
                TeamStatistics awayStats = event.getAwayStats();
                
                BigDecimal homePressure = calculateShotPressure(homeStats, event.getMinute());
                BigDecimal awayPressure = calculateShotPressure(awayStats, event.getMinute());
                
                timeline.add(MatchKPIs.TimelinePoint.builder()
                        .minute(event.getMinute())
                        .homeValue(homePressure)
                        .awayValue(awayPressure)
                        .build());
            }
        }
        
        return timeline;
    }
}
