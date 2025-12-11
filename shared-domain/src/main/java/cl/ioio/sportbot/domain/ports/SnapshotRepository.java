package cl.ioio.sportbot.domain.ports;

import cl.ioio.sportbot.domain.model.MatchSnapshot;

import java.util.Optional;

/**
 * Port for storing and retrieving match snapshots
 * Hexagonal architecture - output port
 */
public interface SnapshotRepository {
    
    /**
     * Save a match snapshot
     * 
     * @param snapshot the snapshot to save
     */
    void save(MatchSnapshot snapshot);
    
    /**
     * Get the latest snapshot for a match
     * 
     * @param matchId the match ID
     * @return the latest snapshot, if available
     */
    Optional<MatchSnapshot> getLatest(String matchId);
    
    /**
     * Delete a snapshot
     * 
     * @param matchId the match ID
     */
    void delete(String matchId);
}
