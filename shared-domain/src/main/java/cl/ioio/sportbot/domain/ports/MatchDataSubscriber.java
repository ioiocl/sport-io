package cl.ioio.sportbot.domain.ports;

import cl.ioio.sportbot.domain.model.MatchEvent;

import java.util.function.Consumer;

/**
 * Port for subscribing to match events
 * Hexagonal architecture - input port
 */
public interface MatchDataSubscriber {
    
    /**
     * Subscribe to match events for specific matches
     * 
     * @param matchIds the match IDs to subscribe to
     * @param handler the event handler
     */
    void subscribe(String[] matchIds, Consumer<MatchEvent> handler);
    
    /**
     * Unsubscribe from all matches
     */
    void unsubscribe();
}
