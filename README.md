# ⚽ Sportbot - Real-Time Football Analytics System

Advanced football match analytics using **Bayesian Analysis**, **ARIMA Forecasting**, and **Monte Carlo Simulation** - powered by **API-Football**.

## 🎯 What It Does

Sportbot analyzes live football matches in real-time and provides:

- **Match Momentum** (Bayesian Analysis) - Which team is dominating?
- **Goal Predictions** (ARIMA Forecast) - When will the next goal happen?
- **Match Outcome Probabilities** (Monte Carlo) - Who will win?
- **Live Statistics** - Possession, shots, xG, and more

## 🏗️ Architecture

Based on **Hexagonal Architecture** (Ports & Adapters):

```
┌─────────────────┐
│  API-Football   │ (Data Source)
│  REST API       │
└────────┬────────┘
         │ HTTP Polling (15s)
         ↓
┌─────────────────────────┐
│  Ingestion Service      │
│  • FootballApiClient    │
│  • Event Normalizer     │
│  • RedisPublisher       │
└────────┬────────────────┘
         │ Redis Pub/Sub (match-events)
         ↓
┌─────────────────────────┐
│  Analytics Service      │
│  • MomentumAnalyzer     │ → Bayesian (drift/volatility)
│  • GoalForecaster       │ → ARIMA (goal predictions)
│  • MatchSimulator       │ → Monte Carlo (probabilities)
└────────┬────────────────┘
         │ Redis KV (latest_match_snapshot:MATCH_ID)
         ↓
┌─────────────────────────┐
│  WebSocket API          │
│  • Broadcast Service    │
└────────┬────────────────┘
         │ WebSocket
         ↓
┌─────────────────────────┐
│  React Dashboard        │
│  • Live Match View      │
│  • Predictions          │
│  • Statistics           │
└─────────────────────────┘
```

## 📊 Analytics Explained

### 1. Momentum Analysis (Bayesian)

Analyzes possession percentage over time to determine match momentum:

- **Drift (μ)**: Momentum direction
  - `μ > 0`: Home team has momentum
  - `μ < 0`: Away team has momentum
  - `μ ≈ 0`: Balanced match

- **Volatility (σ)**: Match variability
  - High: Open, unpredictable match
  - Low: Controlled, predictable match

**Example:**
```json
{
  "drift": 0.15,           // Home team dominating
  "volatility": 0.08,      // Controlled match
  "confidence": 0.95       // 95% confidence
}
```

### 2. Goal Forecast (ARIMA)

Predicts when and how many goals will be scored:

- Uses ARIMA(1,1,1) model
- Analyzes goal rate over time
- Predicts next 10 minutes

**Example:**
```json
{
  "nextGoalProbability": 0.45,  // 45% chance in next 10 min
  "nextGoalTeam": "HOME",       // Home team more likely
  "nextGoalMinute": 72,         // Expected at minute 72
  "expectedTotalGoals": 3.2     // 3-4 goals total expected
}
```

### 3. Match Prediction (Monte Carlo)

Simulates 10,000 possible match endings:

**Example:**
```json
{
  "probabilityHomeWin": 0.65,   // 65% home win
  "probabilityDraw": 0.20,      // 20% draw
  "probabilityAwayWin": 0.15,   // 15% away win
  "expectedFinalScore": "3-1",
  "mostLikelyScores": [
    {"score": "3-1", "probability": 0.18},
    {"score": "2-1", "probability": 0.15},
    {"score": "3-2", "probability": 0.12}
  ]
}
```

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- API-Football API Key (included: `85c00191235f58aa27fcccd8d737d8a7`)

### 1. Get Live Match IDs

Visit: https://v3.football.api-sports.io/fixtures?live=all

Or use curl:
```bash
curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
```

### 2. Update Match IDs

Edit `.env`:
```bash
FOOTBALL_MATCHES=215662,592872,867946  # Replace with live match IDs
```

### 3. Start System

```bash
docker compose up --build
```

### 4. Open Dashboard

Navigate to: http://localhost:3000

## 📁 Project Structure

```
sportbot/
├── shared-domain/          # Domain models & ports
│   └── src/main/java/cl/ioio/sportbot/domain/
│       ├── model/
│       │   ├── MatchEvent.java
│       │   ├── MatchSnapshot.java
│       │   ├── MomentumMetrics.java
│       │   ├── MatchPrediction.java
│       │   └── GoalForecast.java
│       └── ports/
│           ├── MatchDataPublisher.java
│           ├── MatchDataSubscriber.java
│           └── SnapshotRepository.java
│
├── ingestion-service/      # Data ingestion from API-Football
│   └── src/main/java/cl/ioio/sportbot/ingestion/
│       ├── adapter/
│       │   ├── FootballApiClient.java
│       │   └── RedisMatchDataPublisher.java
│       └── application/
│           └── MatchIngestionService.java
│
├── analytics-service/      # Bayesian, ARIMA, Monte Carlo
│   └── src/main/java/cl/ioio/sportbot/analytics/
│       ├── domain/
│       │   ├── MomentumAnalyzer.java
│       │   ├── GoalForecaster.java
│       │   └── MatchSimulator.java
│       ├── adapter/
│       │   ├── RedisMatchDataSubscriber.java
│       │   └── RedisSnapshotRepository.java
│       └── application/
│           └── MatchAnalysisService.java
│
├── websocket-api/          # WebSocket broadcasting
│   └── src/main/java/cl/ioio/sportbot/websocket/
│       └── BroadcastService.java
│
├── dashboard/              # React frontend
│   └── src/
│       ├── App.jsx
│       └── components/
│           └── MatchCard.jsx
│
├── docker-compose.yml
├── .env
└── README.md
```

## ⚙️ Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `FOOTBALL_API_KEY` | (provided) | API-Football API key |
| `FOOTBALL_MATCHES` | - | Comma-separated match IDs |
| `POLL_INTERVAL` | 15s | How often to poll API |
| `MONTE_CARLO_HORIZON_MINUTES` | 45 | Minutes to simulate |
| `MONTE_CARLO_SIMULATIONS` | 10000 | Number of simulations |
| `ARIMA_HORIZON_MINUTES` | 10 | Minutes to forecast |

### Changing Forecast Horizon

**For half-time analysis (45 min):**
```bash
MONTE_CARLO_HORIZON_MINUTES=45
```

**For full match (90 min):**
```bash
MONTE_CARLO_HORIZON_MINUTES=90
```

**For extra time (30 min):**
```bash
MONTE_CARLO_HORIZON_MINUTES=30
```

## 📡 API-Football Integration

### Endpoints Used

1. **Live Fixtures**
   ```
   GET /fixtures?live=all
   ```

2. **Match Statistics**
   ```
   GET /fixtures/statistics?fixture={matchId}
   ```

3. **Match Events**
   ```
   GET /fixtures/events?fixture={matchId}
   ```

### Rate Limits

- **Free Plan**: 100 requests/day
- **Basic Plan**: 1,000 requests/day ($15/month)
- **Pro Plan**: Unlimited ($50/month)

**Recommendation**: Use 15-30 second polling interval to stay within limits.

## 🎨 Dashboard Features

### Match Card Display

- **Live Score** - Real-time score updates
- **Match Status** - 1H, HT, 2H, FT
- **Momentum Bar** - Visual momentum indicator
- **Win Probabilities** - Home/Draw/Away percentages
- **Next Goal Prediction** - Team and probability
- **Statistics** - Possession, shots, xG

### Color Coding

- 🟢 **Green**: Home team advantage
- 🟡 **Yellow**: Balanced
- 🔴 **Red**: Away team advantage

## 🧪 Testing

### Test with Mock Data

```bash
# Start only Redis
docker compose up redis

# Run analytics service locally
cd analytics-service
mvn quarkus:dev
```

### Verify Redis Data

```bash
docker exec -it sportbot-redis redis-cli

# Check published events
SUBSCRIBE match-events:*

# Check snapshots
KEYS latest_match_snapshot:*
GET latest_match_snapshot:215662
```

## 📈 Use Cases

### 1. Live Betting Analysis

```
Minute 67: Real Madrid 2-1 Barcelona

Analysis:
- Momentum: +0.15 (Real Madrid dominating)
- Volatility: 0.08 (controlled match)
- Probability Real wins: 65%
- Probability more goals: 72%

Recommendation: Bet on "Over 3.5 total goals"
```

### 2. Tactical Analysis

```
Change detected at minute 60:
- Possession dropped from 58% to 52%
- xG increased from 1.8 to 2.1
- Momentum changed from +0.20 to +0.05

Interpretation: Barcelona pressing after substitution
```

### 3. Automated Alerts

```
ALERT: High goal probability in next 5 minutes!
Team: Real Madrid (85%)
Reason: High momentum + increasing xG + corner kicks
```

## 🔧 Development

### Build All Services

```bash
mvn clean install
```

### Build Specific Service

```bash
cd analytics-service
mvn clean package
```

### Run Tests

```bash
mvn test
```

### Hot Reload (Quarkus Dev Mode)

```bash
cd analytics-service
mvn quarkus:dev
```

## 🐛 Troubleshooting

### No Data Appearing

1. **Check match IDs are live:**
   ```bash
   curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
     -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
   ```

2. **Check ingestion service logs:**
   ```bash
   docker compose logs -f ingestion-service
   ```

3. **Verify Redis connection:**
   ```bash
   docker exec -it sportbot-redis redis-cli PING
   ```

### High API Usage

- Increase `POLL_INTERVAL` to 30s or 60s
- Reduce number of matches in `FOOTBALL_MATCHES`
- Upgrade to paid API plan

### Inaccurate Predictions

- Wait for more data (need 30+ events)
- Check `confidence` field in response
- Reduce forecast horizon for better accuracy

## 📚 Learn More

### Analytics Methods

- **Bayesian Analysis**: [Wikipedia](https://en.wikipedia.org/wiki/Bayesian_inference)
- **ARIMA**: [Wikipedia](https://en.wikipedia.org/wiki/Autoregressive_integrated_moving_average)
- **Monte Carlo**: [Wikipedia](https://en.wikipedia.org/wiki/Monte_Carlo_method)

### API Documentation

- **API-Football**: https://www.api-football.com/documentation-v3

### Architecture

- **Hexagonal Architecture**: https://alistair.cockburn.us/hexagonal-architecture/

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## 📄 License

MIT License - feel free to use for personal or commercial projects

## 🙏 Credits

- **API-Football** for match data
- **Finbot** architecture as inspiration
- **Quarkus** framework
- **Apache Commons Math** for statistical functions

## 📞 Support

For issues or questions:
- Check logs: `docker compose logs -f`
- Verify configuration in `.env`
- Review `FORECAST_CONFIGURATION.md`

---

**Built with ❤️ for football analytics enthusiasts** ⚽📊
