package cl.ioio.sportbot.analytics.application;

import cl.ioio.sportbot.analytics.domain.GoalForecaster;
import cl.ioio.sportbot.analytics.domain.KPICalculator;
import cl.ioio.sportbot.analytics.domain.MatchSimulator;
import cl.ioio.sportbot.analytics.domain.MomentumAnalyzer;
import cl.ioio.sportbot.domain.model.*;
import cl.ioio.sportbot.domain.ports.MatchDataSubscriber;
import cl.ioio.sportbot.domain.ports.SnapshotRepository;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Slf4j
public class MatchAnalysisService {
    
    @ConfigProperty(name = "analytics.matches", defaultValue = "")
    String matchesConfig;
    
    @Inject
    MatchDataSubscriber matchDataSubscriber;
    
    @Inject
    SnapshotRepository snapshotRepository;
    
    @Inject
    MomentumAnalyzer momentumAnalyzer;
    
    @Inject
    GoalForecaster goalForecaster;
    
    @Inject
    MatchSimulator matchSimulator;
    
    @Inject
    cl.ioio.sportbot.analytics.domain.ABCAnalyzer abcAnalyzer;
    
    @Inject
    KPICalculator kpiCalculator;
    
    @Inject
    RedisDataSource redisDataSource;
    
    private ValueCommands<String, String> valueCommands;
    private final Map<String, List<MatchEvent>> eventHistory = new ConcurrentHashMap<>();
    private String[] matchIds = new String[0];
    private String lastActiveMatchIds = "";
    
    void onStart(@Observes StartupEvent ev) {
        this.valueCommands = redisDataSource.value(String.class);
        log.info("Analytics service started. Waiting for match selection...");
    }
    
    @Scheduled(every = "5s")
    void checkForNewMatches() {
        try {
            // Check if match selection has changed
            String activeMatchIds = valueCommands.get("active_match_ids");
            
            if (activeMatchIds == null || activeMatchIds.isEmpty()) {
                return;
            }
            
            if (!activeMatchIds.equals(lastActiveMatchIds)) {
                // Unsubscribe from old matches
                if (matchIds.length > 0) {
                    matchDataSubscriber.unsubscribe();
                    eventHistory.clear();
                }
                
                // Subscribe to new matches
                matchIds = activeMatchIds.split(",");
                lastActiveMatchIds = activeMatchIds;
                log.info("Subscribing to {} matches", matchIds.length);
                matchDataSubscriber.subscribe(matchIds, this::handleMatchEvent);
            }
        } catch (Exception e) {
            log.warn("Error checking for new matches: {}", e.getMessage());
        }
    }
    
    private void handleMatchEvent(MatchEvent event) {
        String matchId = event.getMatchId();
        
        eventHistory.computeIfAbsent(matchId, k -> new ArrayList<>()).add(event);
        
        List<MatchEvent> history = eventHistory.get(matchId);
        if (history.size() < 10) {
            log.debug("Not enough data for match {} ({} events)", matchId, history.size());
            return;
        }
        
        log.info("Analyzing match {}: {} {} - {} {}", 
                matchId, event.getHomeTeam(), event.getHomeScore(),
                event.getAwayScore(), event.getAwayTeam());
    }
    
    @Scheduled(every = "{snapshot.interval:15s}")
    void generateSnapshots() {
        for (String matchId : matchIds) {
            try {
                generateSnapshot(matchId.trim());
            } catch (Exception e) {
                log.error("Error generating snapshot for {}", matchId, e);
            }
        }
    }
    
    private void generateSnapshot(String matchId) {
        List<MatchEvent> history = eventHistory.get(matchId);
        if (history == null || history.size() < 10) {
            return;
        }
        
        MatchEvent latest = history.get(history.size() - 1);
        
        // Extract possession history
        List<BigDecimal> possessionHistory = new ArrayList<>();
        for (MatchEvent event : history) {
            if (event.getPossession() != null) {
                possessionHistory.add(event.getPossession());
            }
        }
        
        // Extract goal history
        List<BigDecimal> goalHistory = new ArrayList<>();
        for (MatchEvent event : history) {
            goalHistory.add(BigDecimal.valueOf(event.getHomeScore() + event.getAwayScore()));
        }
        
        // ABC Integrated Analysis: ARIMA → Bayes → Monte Carlo
        int minutesRemaining = 90 - (latest.getMinute() != null ? latest.getMinute() : 0);
        cl.ioio.sportbot.domain.model.ABCAnalysisResult abcResult = abcAnalyzer.analyze(
                possessionHistory,
                goalHistory,
                latest.getHomeScore(),
                latest.getAwayScore(),
                minutesRemaining
        );
        
        // Calculate KPIs from enriched event history
        MatchKPIs kpis = kpiCalculator.calculate(history);
        
        // Determine match state from ABC-adjusted momentum
        MatchSnapshot.MatchState state = determineMatchState(
                abcResult.getMomentumMetrics().getDrift().doubleValue());
        
        // Create snapshot with ABC analysis and KPIs
        MatchSnapshot snapshot = MatchSnapshot.builder()
                .matchId(matchId)
                .homeTeam(latest.getHomeTeam())
                .awayTeam(latest.getAwayTeam())
                .homeTeamId(latest.getHomeTeamId())
                .awayTeamId(latest.getAwayTeamId())
                .timestamp(Instant.now())
                .minute(latest.getMinute())
                .status(latest.getStatus())
                .homeScore(latest.getHomeScore())
                .awayScore(latest.getAwayScore())
                .momentumMetrics(abcResult.getMomentumMetrics())
                .matchPrediction(abcResult.getMatchPrediction())
                .goalForecast(abcResult.getGoalForecast())
                .matchState(state)
                .sampleSize(history.size())
                .arimaSignal(abcResult.getArimaSignal())
                .abcIntegrationConfidence(abcResult.getIntegrationConfidence())
                .needsRecalibration(abcResult.isNeedsRecalibration())
                // New KPI bundle
                .kpis(kpis)
                .homeStats(latest.getHomeStats())
                .awayStats(latest.getAwayStats())
                .apiVersion("v2")
                .build();
        
        snapshotRepository.save(snapshot);
        
        // Log KPI alerts
        logKPIAlerts(matchId, kpis);
        
        if (abcResult.isNeedsRecalibration()) {
            log.warn("⚠️ Match {}: {} - RECALIBRATION NEEDED! ARIMA: {}", 
                    matchId, state, abcResult.getArimaSignal().getDescription());
        } else {
            log.info("✓ Match {}: {} - ABC confidence: {} - ARIMA: {}", 
                    matchId, state, abcResult.getIntegrationConfidence(), 
                    abcResult.getArimaSignal().getDescription());
        }
    }
    
    private void logKPIAlerts(String matchId, MatchKPIs kpis) {
        if (kpis == null) return;
        
        StringBuilder alerts = new StringBuilder();
        
        if (Boolean.TRUE.equals(kpis.getHomeHighPressAlert())) {
            alerts.append("🔥 HOME HIGH PRESS ");
        }
        if (Boolean.TRUE.equals(kpis.getAwayHighPressAlert())) {
            alerts.append("🔥 AWAY HIGH PRESS ");
        }
        if (Boolean.TRUE.equals(kpis.getHomeCardRiskAlert())) {
            alerts.append("🟨 HOME CARD RISK ");
        }
        if (Boolean.TRUE.equals(kpis.getAwayCardRiskAlert())) {
            alerts.append("🟨 AWAY CARD RISK ");
        }
        if (Boolean.TRUE.equals(kpis.getHomeGoalImminentAlert())) {
            alerts.append("⚽ HOME GOAL IMMINENT ");
        }
        if (Boolean.TRUE.equals(kpis.getAwayGoalImminentAlert())) {
            alerts.append("⚽ AWAY GOAL IMMINENT ");
        }
        
        if (alerts.length() > 0) {
            log.info("🚨 Match {} ALERTS: {}", matchId, alerts.toString().trim());
        }
    }
    
    private MatchSnapshot.MatchState determineMatchState(double drift) {
        if (drift > 0.10) return MatchSnapshot.MatchState.HOME_DOMINATING;
        if (drift > 0.03) return MatchSnapshot.MatchState.HOME_SLIGHT_ADVANTAGE;
        if (drift < -0.10) return MatchSnapshot.MatchState.AWAY_DOMINATING;
        if (drift < -0.03) return MatchSnapshot.MatchState.AWAY_SLIGHT_ADVANTAGE;
        return MatchSnapshot.MatchState.BALANCED;
    }
}
