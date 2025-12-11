package cl.ioio.sportbot.domain.ports;

import cl.ioio.sportbot.domain.model.MatchEvent;

/**
 * Port for publishing match events
 * Hexagonal architecture - output port
 */
public interface MatchDataPublisher {
    
    /**
     * Publish a match event to the messaging system
     * 
     * @param event the match event to publish
     */
    void publish(MatchEvent event);
}
