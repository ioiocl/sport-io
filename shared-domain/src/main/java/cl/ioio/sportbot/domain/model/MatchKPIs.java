package cl.ioio.sportbot.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Comprehensive KPI bundle for match analysis
 * Contains all calculated metrics for dashboard display
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchKPIs {
    
    // ============ POSSESSION & TERRITORY ============
    /**
     * Possession momentum: Rate of change in possession over time
     * Positive = home gaining control, Negative = away gaining control
     */
    @JsonProperty("possessionMomentum")
    private BigDecimal possessionMomentum;
    
    /**
     * Attacking territory: Weighted combination of shots inside box, corners, offsides
     * Higher = more attacking presence in opponent's third
     */
    @JsonProperty("homeAttackingTerritory")
    private BigDecimal homeAttackingTerritory;
    
    @JsonProperty("awayAttackingTerritory")
    private BigDecimal awayAttackingTerritory;
    
    // ============ SHOT QUALITY & PRESSURE ============
    /**
     * Shot-pressure index: Frequency and quality of shots
     * Based on shots inside box, shots on target, blocked shots
     */
    @JsonProperty("homeShotPressure")
    private BigDecimal homeShotPressure;
    
    @JsonProperty("awayShotPressure")
    private BigDecimal awayShotPressure;
    
    /**
     * Shot-quality mix: Ratio of quality shots (inside box, on target) to total shots
     */
    @JsonProperty("homeShotQuality")
    private BigDecimal homeShotQuality;
    
    @JsonProperty("awayShotQuality")
    private BigDecimal awayShotQuality;
    
    // ============ PLAYING STYLE INDICATORS ============
    /**
     * Direct-speed: Pass accuracy combined with forward progression
     * Higher = more direct, efficient passing
     */
    @JsonProperty("homeDirectSpeed")
    private BigDecimal homeDirectSpeed;
    
    @JsonProperty("awayDirectSpeed")
    private BigDecimal awayDirectSpeed;
    
    /**
     * High-press indicator: Fouls + tackles in opponent's half
     * Derived from fouls, offsides caught, possession changes
     */
    @JsonProperty("homeHighPress")
    private BigDecimal homeHighPress;
    
    @JsonProperty("awayHighPress")
    private BigDecimal awayHighPress;
    
    /**
     * Counter-threat proxy: Offsides + fast breaks potential
     * Based on offsides (indicating forward runs) and shot speed
     */
    @JsonProperty("homeCounterThreat")
    private BigDecimal homeCounterThreat;
    
    @JsonProperty("awayCounterThreat")
    private BigDecimal awayCounterThreat;
    
    // ============ DISCIPLINE METRICS ============
    /**
     * Disciplinary balance: Cards relative to fouls
     * Higher = more cards per foul (referee strictness or reckless play)
     */
    @JsonProperty("homeDisciplinaryBalance")
    private BigDecimal homeDisciplinaryBalance;
    
    @JsonProperty("awayDisciplinaryBalance")
    private BigDecimal awayDisciplinaryBalance;
    
    /**
     * Foul intensity: Fouls per minute of play
     */
    @JsonProperty("homeFoulIntensity")
    private BigDecimal homeFoulIntensity;
    
    @JsonProperty("awayFoulIntensity")
    private BigDecimal awayFoulIntensity;
    
    // ============ GOALKEEPER METRICS ============
    /**
     * Keeper pressure: Shots faced / saves ratio
     * Higher = keeper under more pressure
     */
    @JsonProperty("homeKeeperPressure")
    private BigDecimal homeKeeperPressure;
    
    @JsonProperty("awayKeeperPressure")
    private BigDecimal awayKeeperPressure;
    
    /**
     * Save efficiency: Saves / shots on target faced
     */
    @JsonProperty("homeKeeperEfficiency")
    private BigDecimal homeKeeperEfficiency;
    
    @JsonProperty("awayKeeperEfficiency")
    private BigDecimal awayKeeperEfficiency;
    
    // ============ ALERT FLAGS ============
    /**
     * High press alert: Team is pressing aggressively
     */
    @JsonProperty("homeHighPressAlert")
    private Boolean homeHighPressAlert;
    
    @JsonProperty("awayHighPressAlert")
    private Boolean awayHighPressAlert;
    
    /**
     * Card risk alert: High foul intensity suggests card incoming
     */
    @JsonProperty("homeCardRiskAlert")
    private Boolean homeCardRiskAlert;
    
    @JsonProperty("awayCardRiskAlert")
    private Boolean awayCardRiskAlert;
    
    /**
     * Goal imminent alert: High shot pressure + quality suggests goal likely
     */
    @JsonProperty("homeGoalImminentAlert")
    private Boolean homeGoalImminentAlert;
    
    @JsonProperty("awayGoalImminentAlert")
    private Boolean awayGoalImminentAlert;
    
    // ============ HALF COMPARISON ============
    /**
     * First half vs second half momentum shift
     */
    @JsonProperty("homeMomentumShift")
    private BigDecimal homeMomentumShift;
    
    @JsonProperty("awayMomentumShift")
    private BigDecimal awayMomentumShift;
    
    // ============ TIMELINE DATA ============
    /**
     * Possession trend over time (for timeline chart)
     * Array of possession values at different minutes
     */
    @JsonProperty("possessionTimeline")
    private java.util.List<TimelinePoint> possessionTimeline;
    
    /**
     * Shot pressure trend over time
     */
    @JsonProperty("shotPressureTimeline")
    private java.util.List<TimelinePoint> shotPressureTimeline;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelinePoint {
        private Integer minute;
        private BigDecimal homeValue;
        private BigDecimal awayValue;
    }
}
