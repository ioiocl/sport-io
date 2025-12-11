# ABC Analysis: ARIMA-Bayes-Carlo Integration

## Overview

The ABC (ARIMA-Bayes-Carlo) analysis is an integrated three-stage analytical pipeline that provides real-time match predictions with adaptive learning capabilities.

## Architecture

```
Match Events (possession, goals, shots)
    ↓
┌─────────────────────────────────────────────────────────┐
│ Stage 1: ARIMA Analysis                                 │
│ - Detects trends in goal-scoring patterns              │
│ - Identifies structural breaks (CUSUM algorithm)       │
│ - Exports trend signals                                 │
└─────────────────────────────────────────────────────────┘
    ↓ (trend signal feeds Bayesian prior)
┌─────────────────────────────────────────────────────────┐
│ Stage 2: Bayesian Momentum Analysis                     │
│ - Updates momentum with ARIMA-informed prior           │
│ - Calculates drift (momentum) and volatility           │
│ - Adjusts confidence based on structural breaks        │
└─────────────────────────────────────────────────────────┘
    ↓ (dynamic probabilities)
┌─────────────────────────────────────────────────────────┐
│ Stage 3: Monte Carlo Simulation                         │
│ - Simulates 10,000 match outcomes                      │
│ - Uses Bayesian momentum to adjust goal rates          │
│ - Generates probability distributions                   │
└─────────────────────────────────────────────────────────┘
    ↓ (feedback loop)
┌─────────────────────────────────────────────────────────┐
│ Recalibration Trigger                                   │
│ - Structural break detected → reset models             │
│ - High volatility → reduce confidence                   │
└─────────────────────────────────────────────────────────┘
```

## Stage 1: ARIMA Analysis

### Purpose
Detect trends and structural breaks in match data to inform Bayesian priors.

### Implementation
- **Algorithm**: Holt's exponential smoothing (simplified ARIMA)
- **Structural Break Detection**: CUSUM (Cumulative Sum Control Chart)
- **Output**: `ARIMASignal` with trend percentage and break detection

### Example Output
```
"Attack increasing 12.5% in trend"
"Attack stable"
"Attack decreasing 8.2% in trend [STRUCTURAL BREAK DETECTED]"
```

### Key Features
- Monitors last 30% of observations for sudden changes
- Threshold: 3 standard deviations
- Detects events like: player injuries, red cards, tactical changes

### Code Location
`analytics-service/src/main/java/cl/ioio/sportbot/analytics/domain/GoalForecaster.java`

## Stage 2: Bayesian Momentum Analysis

### Purpose
Update match momentum using ARIMA trend as an informed prior.

### How ARIMA Feeds Bayes
```java
// ARIMA says: "Attack increasing 12% in trend"
double arimaTrend = arimaSignal.getTrend().doubleValue();

// Bayesian prior mean is set from ARIMA trend
double priorMean = arimaTrend * 10.0;

// Prior variance adjusted by ARIMA confidence
double priorVariance = 0.01 * (2.0 - arimaConfidence);

// Bayesian update combines ARIMA prior with observed possession changes
double posteriorMean = (priorN * priorMean + sampleSize * sampleMean) / posteriorN;
```

### Benefits
- **Adaptive**: Prior adjusts based on what's actually happening
- **Confident**: High ARIMA confidence → tighter prior
- **Cautious**: Structural breaks → reduced confidence

### Code Location
`analytics-service/src/main/java/cl/ioio/sportbot/analytics/domain/MomentumAnalyzer.java`

## Stage 3: Monte Carlo Simulation

### Purpose
Simulate thousands of match outcomes using dynamic Bayesian probabilities.

### How Bayes Feeds Monte Carlo
```java
// Bayesian momentum (drift) adjusts goal rates
double homeGoalRate = baseGoalRate * (1.0 + momentum);
double awayGoalRate = baseGoalRate * (1.0 - momentum);

// Monte Carlo simulates 10,000 matches with adjusted rates
PoissonDistribution homeDist = new PoissonDistribution(homeExpectedGoals);
PoissonDistribution awayDist = new PoissonDistribution(awayExpectedGoals);
```

### Output
- Win/Draw/Loss probabilities
- Most likely final scores
- Goal distribution percentiles
- Comeback/hold lead probabilities

### Code Location
`analytics-service/src/main/java/cl/ioio/sportbot/analytics/domain/MatchSimulator.java`

## Feedback Loop: Recalibration

### Trigger Conditions
1. **ARIMA detects structural break** → Models need recalibration
2. **High volatility** (> 0.15) → Match is unpredictable

### Actions
- Reduce confidence scores
- Flag snapshot with `needsRecalibration: true`
- Log warning for monitoring

### Example Scenarios
- **Red card**: Sudden drop in attack → structural break detected
- **Injury to key player**: Change in possession patterns → recalibration triggered
- **Tactical shift**: Formation change → ARIMA detects trend change

## Integration Confidence

The ABC system calculates an overall confidence score:

```java
double arimaConf = arimaSignal.getConfidence();
double bayesConf = momentum.getConfidence();

// Penalize during structural breaks
double stabilityFactor = structuralBreak ? 0.7 : 1.0;

// Geometric mean with stability adjustment
double integrationConfidence = sqrt(arimaConf * bayesConf) * stabilityFactor;
```

## API Response

The `MatchSnapshot` now includes ABC analysis:

```json
{
  "matchId": "12345",
  "homeTeam": "Team A",
  "awayTeam": "Team B",
  "homeScore": 1,
  "awayScore": 0,
  "arimaSignal": {
    "trend": 0.0025,
    "trendPercentage": 12.5,
    "structuralBreakDetected": false,
    "confidence": 0.85,
    "description": "Attack increasing 12.5% in trend"
  },
  "momentumMetrics": {
    "drift": 0.045,
    "volatility": 0.08,
    "confidence": 0.82,
    "priorMean": 0.025,
    "posteriorMean": 0.045
  },
  "matchPrediction": {
    "probabilityHomeWin": 0.68,
    "probabilityDraw": 0.22,
    "probabilityAwayWin": 0.10,
    "expectedFinalScore": "2-0"
  },
  "abcIntegrationConfidence": 0.83,
  "needsRecalibration": false
}
```

## Benefits of ABC Integration

### 1. Live Adaptation
- Models adjust in real-time to match dynamics
- Not just "who's winning" but "how the match is evolving"

### 2. Structural Break Detection
- Identifies sudden changes (injuries, red cards, tactical shifts)
- Triggers recalibration automatically

### 3. Informed Priors
- Bayesian analysis uses ARIMA trends, not fixed assumptions
- More accurate momentum estimation

### 4. Complete Probability Distributions
- Not just point predictions
- Full distribution of possible outcomes
- Confidence intervals and percentiles

### 5. Feedback Loop
- Monte Carlo results can inform future ARIMA calibration
- Continuous improvement through adaptive learning

## Comparison: Before vs After ABC

| Aspect | Before | After ABC |
|--------|--------|-----------|
| **ARIMA** | Independent goal forecast | Feeds Bayesian prior |
| **Bayesian** | Fixed prior (mean=0) | ARIMA-informed prior |
| **Monte Carlo** | Uses Bayesian momentum | Uses ARIMA-adjusted momentum |
| **Structural Breaks** | Not detected | CUSUM detection + recalibration |
| **Integration** | Separate components | Orchestrated pipeline |
| **Confidence** | Per-component only | Integrated confidence score |

## Usage

The ABC analyzer is automatically used in the `MatchAnalysisService`:

```java
ABCAnalysisResult abcResult = abcAnalyzer.analyze(
    possessionHistory,
    goalHistory,
    currentHomeScore,
    currentAwayScore,
    minutesRemaining
);

// Check if recalibration needed
if (abcResult.isNeedsRecalibration()) {
    log.warn("Structural break detected! ARIMA: {}", 
        abcResult.getArimaSignal().getDescription());
}
```

## Monitoring

Watch for these log messages:

- ✅ `✓ Match X: BALANCED - ABC confidence: 0.85 - ARIMA: Attack stable`
- ⚠️ `⚠️ Match X: HOME_DOMINATING - RECALIBRATION NEEDED! ARIMA: Attack increasing 15.2% in trend [STRUCTURAL BREAK DETECTED]`

## Future Enhancements

1. **Adaptive Learning**: Use prediction errors to tune ARIMA parameters
2. **Multi-feature ARIMA**: Include shots, xG, not just goals
3. **Bayesian Change Point Detection**: More sophisticated break detection
4. **Dynamic Recalibration**: Automatically reset models on breaks
5. **Historical Validation**: Backtest ABC predictions vs actual outcomes
