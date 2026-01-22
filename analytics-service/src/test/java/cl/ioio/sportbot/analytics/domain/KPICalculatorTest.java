package cl.ioio.sportbot.analytics.domain;

import cl.ioio.sportbot.domain.model.MatchEvent;
import cl.ioio.sportbot.domain.model.MatchKPIs;
import cl.ioio.sportbot.domain.model.TeamStatistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KPICalculatorTest {

    private KPICalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new KPICalculator();
    }

    @Test
    void calculate_withEmptyHistory_returnsEmptyKPIs() {
        MatchKPIs kpis = calculator.calculate(new ArrayList<>());
        assertNotNull(kpis);
    }

    @Test
    void calculate_withNullHistory_returnsEmptyKPIs() {
        MatchKPIs kpis = calculator.calculate(null);
        assertNotNull(kpis);
    }

    @Test
    void calculate_withValidHistory_calculatesAllKPIs() {
        List<MatchEvent> history = createSampleHistory(15);
        
        MatchKPIs kpis = calculator.calculate(history);
        
        assertNotNull(kpis);
        assertNotNull(kpis.getPossessionMomentum());
        assertNotNull(kpis.getHomeShotPressure());
        assertNotNull(kpis.getAwayShotPressure());
        assertNotNull(kpis.getHomeShotQuality());
        assertNotNull(kpis.getAwayShotQuality());
    }

    @Test
    void calculate_possessionMomentum_positiveWhenHomeGainingControl() {
        List<MatchEvent> history = new ArrayList<>();
        
        // Home team gaining possession over time: 45% -> 55%
        for (int i = 0; i < 10; i++) {
            history.add(createEventWithPossession(i + 1, new BigDecimal(45 + i)));
        }
        
        MatchKPIs kpis = calculator.calculate(history);
        
        assertTrue(kpis.getPossessionMomentum().compareTo(BigDecimal.ZERO) > 0,
                "Possession momentum should be positive when home team is gaining control");
    }

    @Test
    void calculate_possessionMomentum_negativeWhenAwayGainingControl() {
        List<MatchEvent> history = new ArrayList<>();
        
        // Home team losing possession over time: 55% -> 45%
        for (int i = 0; i < 10; i++) {
            history.add(createEventWithPossession(i + 1, new BigDecimal(55 - i)));
        }
        
        MatchKPIs kpis = calculator.calculate(history);
        
        assertTrue(kpis.getPossessionMomentum().compareTo(BigDecimal.ZERO) < 0,
                "Possession momentum should be negative when away team is gaining control");
    }

    @Test
    void calculate_shotPressure_increasesWithMoreShots() {
        TeamStatistics lowShots = TeamStatistics.builder()
                .totalShots(2)
                .shotsOnGoal(1)
                .shotsInsideBox(1)
                .build();
        
        TeamStatistics highShots = TeamStatistics.builder()
                .totalShots(10)
                .shotsOnGoal(5)
                .shotsInsideBox(6)
                .build();
        
        List<MatchEvent> lowShotHistory = createHistoryWithStats(15, lowShots, lowShots);
        List<MatchEvent> highShotHistory = createHistoryWithStats(15, highShots, highShots);
        
        MatchKPIs lowKpis = calculator.calculate(lowShotHistory);
        MatchKPIs highKpis = calculator.calculate(highShotHistory);
        
        assertTrue(highKpis.getHomeShotPressure().compareTo(lowKpis.getHomeShotPressure()) > 0,
                "Shot pressure should be higher with more shots");
    }

    @Test
    void calculate_shotQuality_higherWithMoreOnTargetShots() {
        TeamStatistics lowQuality = TeamStatistics.builder()
                .totalShots(10)
                .shotsOnGoal(1)
                .shotsInsideBox(2)
                .build();
        
        TeamStatistics highQuality = TeamStatistics.builder()
                .totalShots(10)
                .shotsOnGoal(7)
                .shotsInsideBox(8)
                .build();
        
        List<MatchEvent> lowQualityHistory = createHistoryWithStats(15, lowQuality, lowQuality);
        List<MatchEvent> highQualityHistory = createHistoryWithStats(15, highQuality, highQuality);
        
        MatchKPIs lowKpis = calculator.calculate(lowQualityHistory);
        MatchKPIs highKpis = calculator.calculate(highQualityHistory);
        
        assertTrue(highKpis.getHomeShotQuality().compareTo(lowKpis.getHomeShotQuality()) > 0,
                "Shot quality should be higher with more on-target shots");
    }

    @Test
    void calculate_alerts_triggeredWhenThresholdsExceeded() {
        TeamStatistics highPressStats = TeamStatistics.builder()
                .totalShots(15)
                .shotsOnGoal(8)
                .shotsInsideBox(10)
                .fouls(20)
                .offsides(5)
                .possession(new BigDecimal("35"))
                .build();
        
        List<MatchEvent> history = createHistoryWithStats(30, highPressStats, highPressStats);
        
        MatchKPIs kpis = calculator.calculate(history);
        
        // At least one alert should be triggered with these aggressive stats
        boolean anyAlert = Boolean.TRUE.equals(kpis.getHomeHighPressAlert()) ||
                          Boolean.TRUE.equals(kpis.getHomeCardRiskAlert()) ||
                          Boolean.TRUE.equals(kpis.getHomeGoalImminentAlert());
        
        assertTrue(anyAlert, "At least one alert should be triggered with high-pressure stats");
    }

    @Test
    void calculate_timelines_populatedFromHistory() {
        List<MatchEvent> history = createSampleHistory(20);
        
        MatchKPIs kpis = calculator.calculate(history);
        
        assertNotNull(kpis.getPossessionTimeline());
        assertFalse(kpis.getPossessionTimeline().isEmpty());
        
        assertNotNull(kpis.getShotPressureTimeline());
        assertFalse(kpis.getShotPressureTimeline().isEmpty());
    }

    @Test
    void calculate_disciplinaryMetrics_calculatedCorrectly() {
        TeamStatistics disciplineStats = TeamStatistics.builder()
                .fouls(15)
                .yellowCards(3)
                .redCards(1)
                .build();
        
        List<MatchEvent> history = createHistoryWithStats(45, disciplineStats, disciplineStats);
        
        MatchKPIs kpis = calculator.calculate(history);
        
        assertNotNull(kpis.getHomeDisciplinaryBalance());
        assertNotNull(kpis.getHomeFoulIntensity());
        assertTrue(kpis.getHomeFoulIntensity().compareTo(BigDecimal.ZERO) > 0,
                "Foul intensity should be positive with fouls committed");
    }

    @Test
    void calculate_keeperMetrics_calculatedFromOpponentStats() {
        TeamStatistics homeStats = TeamStatistics.builder()
                .goalkeeperSaves(5)
                .shotsOnGoal(3)
                .shotsInsideBox(4)
                .build();
        
        TeamStatistics awayStats = TeamStatistics.builder()
                .goalkeeperSaves(2)
                .shotsOnGoal(8)
                .shotsInsideBox(10)
                .build();
        
        List<MatchEvent> history = createHistoryWithStats(30, homeStats, awayStats);
        
        MatchKPIs kpis = calculator.calculate(history);
        
        // Home keeper faces away shots, so home keeper pressure should be based on away shots
        assertNotNull(kpis.getHomeKeeperPressure());
        assertNotNull(kpis.getHomeKeeperEfficiency());
        
        // Away has more shots, so home keeper should be under more pressure
        assertTrue(kpis.getHomeKeeperPressure().compareTo(kpis.getAwayKeeperPressure()) > 0,
                "Home keeper should be under more pressure when facing more shots");
    }

    // Helper methods
    
    private List<MatchEvent> createSampleHistory(int count) {
        List<MatchEvent> history = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            history.add(createEventWithPossession(i + 1, new BigDecimal(50)));
        }
        return history;
    }

    private MatchEvent createEventWithPossession(int minute, BigDecimal possession) {
        TeamStatistics stats = TeamStatistics.builder()
                .totalShots(minute / 5)
                .shotsOnGoal(minute / 10)
                .shotsInsideBox(minute / 7)
                .fouls(minute / 8)
                .cornerKicks(minute / 15)
                .possession(possession)
                .build();
        
        return MatchEvent.builder()
                .matchId("test-match")
                .timestamp(Instant.now())
                .minute(minute)
                .status("1H")
                .homeTeam("Home FC")
                .awayTeam("Away FC")
                .homeScore(0)
                .awayScore(0)
                .possession(possession)
                .homeStats(stats)
                .awayStats(stats)
                .build();
    }

    private List<MatchEvent> createHistoryWithStats(int minute, TeamStatistics homeStats, TeamStatistics awayStats) {
        List<MatchEvent> history = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            int currentMinute = (minute * i) / 10;
            history.add(MatchEvent.builder()
                    .matchId("test-match")
                    .timestamp(Instant.now())
                    .minute(currentMinute)
                    .status(currentMinute <= 45 ? "1H" : "2H")
                    .homeTeam("Home FC")
                    .awayTeam("Away FC")
                    .homeScore(0)
                    .awayScore(0)
                    .possession(homeStats.getPossession() != null ? homeStats.getPossession() : new BigDecimal("50"))
                    .homeStats(homeStats)
                    .awayStats(awayStats)
                    .build());
        }
        return history;
    }
}
