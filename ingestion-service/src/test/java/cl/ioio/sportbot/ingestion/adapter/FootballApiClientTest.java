package cl.ioio.sportbot.ingestion.adapter;

import cl.ioio.sportbot.domain.model.LiveMatchEvent;
import cl.ioio.sportbot.domain.model.TeamStatistics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FootballApiClientTest {

    @Test
    void parseEventType_goal_returnsGoal() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Goal", "Normal Goal");
        assertEquals(LiveMatchEvent.EventType.GOAL, type);
    }

    @Test
    void parseEventType_ownGoal_returnsOwnGoal() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Goal", "Own Goal");
        assertEquals(LiveMatchEvent.EventType.OWN_GOAL, type);
    }

    @Test
    void parseEventType_penaltyGoal_returnsPenaltyGoal() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Goal", "Penalty");
        assertEquals(LiveMatchEvent.EventType.PENALTY_GOAL, type);
    }

    @Test
    void parseEventType_yellowCard_returnsYellowCard() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Card", "Yellow Card");
        assertEquals(LiveMatchEvent.EventType.YELLOW_CARD, type);
    }

    @Test
    void parseEventType_redCard_returnsRedCard() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Card", "Red Card");
        assertEquals(LiveMatchEvent.EventType.RED_CARD, type);
    }

    @Test
    void parseEventType_secondYellow_returnsSecondYellow() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Card", "Second Yellow card");
        assertEquals(LiveMatchEvent.EventType.SECOND_YELLOW, type);
    }

    @Test
    void parseEventType_substitution_returnsSubstitution() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("subst", "Substitution 1");
        assertEquals(LiveMatchEvent.EventType.SUBSTITUTION, type);
    }

    @Test
    void parseEventType_var_returnsVar() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("Var", "Goal cancelled");
        assertEquals(LiveMatchEvent.EventType.VAR, type);
    }

    @Test
    void parseEventType_null_returnsOther() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType(null, null);
        assertEquals(LiveMatchEvent.EventType.OTHER, type);
    }

    @Test
    void parseEventType_unknown_returnsOther() {
        LiveMatchEvent.EventType type = LiveMatchEvent.parseEventType("unknown", "something");
        assertEquals(LiveMatchEvent.EventType.OTHER, type);
    }

    @Test
    void teamStatistics_builder_createsValidObject() {
        TeamStatistics stats = TeamStatistics.builder()
                .teamId("123")
                .teamName("Test FC")
                .totalShots(10)
                .shotsOnGoal(5)
                .shotsInsideBox(7)
                .shotsOutsideBox(3)
                .blockedShots(2)
                .possession(new java.math.BigDecimal("55.5"))
                .cornerKicks(4)
                .offsides(2)
                .fouls(12)
                .yellowCards(2)
                .redCards(0)
                .goalkeeperSaves(3)
                .totalPasses(450)
                .passesAccurate(380)
                .passAccuracy(new java.math.BigDecimal("84.4"))
                .build();

        assertNotNull(stats);
        assertEquals("123", stats.getTeamId());
        assertEquals("Test FC", stats.getTeamName());
        assertEquals(10, stats.getTotalShots());
        assertEquals(5, stats.getShotsOnGoal());
        assertEquals(new java.math.BigDecimal("55.5"), stats.getPossession());
    }

    @Test
    void halfStatistics_builder_createsValidObject() {
        TeamStatistics.HalfStatistics halfStats = TeamStatistics.HalfStatistics.builder()
                .totalShots(5)
                .shotsOnGoal(2)
                .shotsInsideBox(3)
                .possession(new java.math.BigDecimal("52"))
                .fouls(6)
                .yellowCards(1)
                .build();

        assertNotNull(halfStats);
        assertEquals(5, halfStats.getTotalShots());
        assertEquals(2, halfStats.getShotsOnGoal());
        assertEquals(new java.math.BigDecimal("52"), halfStats.getPossession());
    }

    @Test
    void liveMatchEvent_builder_createsValidObject() {
        LiveMatchEvent event = LiveMatchEvent.builder()
                .minute(45)
                .extraTime(2)
                .teamId("123")
                .teamName("Test FC")
                .playerId("456")
                .playerName("John Doe")
                .eventType(LiveMatchEvent.EventType.GOAL)
                .detail("Normal Goal")
                .build();

        assertNotNull(event);
        assertEquals(45, event.getMinute());
        assertEquals(2, event.getExtraTime());
        assertEquals("Test FC", event.getTeamName());
        assertEquals(LiveMatchEvent.EventType.GOAL, event.getEventType());
    }
}
