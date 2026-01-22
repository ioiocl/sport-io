package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a live event from the match (goal, card, substitution, etc.)
 * From API-Football /fixtures/events endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveMatchEvent {
    
    @JsonProperty("minute")
    private Integer minute;
    
    @JsonProperty("extraTime")
    private Integer extraTime;
    
    @JsonProperty("teamId")
    private String teamId;
    
    @JsonProperty("teamName")
    private String teamName;
    
    @JsonProperty("playerId")
    private String playerId;
    
    @JsonProperty("playerName")
    private String playerName;
    
    @JsonProperty("assistPlayerId")
    private String assistPlayerId;
    
    @JsonProperty("assistPlayerName")
    private String assistPlayerName;
    
    @JsonProperty("eventType")
    private EventType eventType;
    
    @JsonProperty("detail")
    private String detail;
    
    @JsonProperty("comments")
    private String comments;
    
    public enum EventType {
        GOAL,
        OWN_GOAL,
        PENALTY_GOAL,
        MISSED_PENALTY,
        YELLOW_CARD,
        SECOND_YELLOW,
        RED_CARD,
        SUBSTITUTION,
        VAR,
        OTHER
    }
    
    /**
     * Parse event type from API-Football response
     */
    public static EventType parseEventType(String type, String detail) {
        if (type == null) return EventType.OTHER;
        
        switch (type.toLowerCase()) {
            case "goal":
                if (detail != null) {
                    if (detail.toLowerCase().contains("own")) return EventType.OWN_GOAL;
                    if (detail.toLowerCase().contains("penalty")) return EventType.PENALTY_GOAL;
                }
                return EventType.GOAL;
            case "card":
                if (detail != null) {
                    if (detail.toLowerCase().contains("red")) return EventType.RED_CARD;
                    if (detail.toLowerCase().contains("second yellow")) return EventType.SECOND_YELLOW;
                }
                return EventType.YELLOW_CARD;
            case "subst":
                return EventType.SUBSTITUTION;
            case "var":
                return EventType.VAR;
            default:
                return EventType.OTHER;
        }
    }
}
