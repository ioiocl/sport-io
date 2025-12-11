# ⚽ Sportbot - Quick Start Guide

## ✅ What's Complete (80%)

### **Core System - READY TO RUN!**

1. ✅ **Shared Domain** - All football models
2. ✅ **Ingestion Service** - API-Football polling
3. ✅ **Analytics Service** - Bayesian, ARIMA, Monte Carlo
4. ✅ **Docker Configuration** - docker-compose.yml
5. ✅ **Documentation** - Complete README

### **Optional (Not Required to Run)**

- ⚠️ WebSocket API (for dashboard)
- ⚠️ React Dashboard (for visualization)

**The core analytics system is fully functional without these!**

---

## 🚀 Quick Start (3 Steps)

### **Step 1: Get Live Match IDs**

```bash
curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
```

Look for `fixture.id` in the response. Example: `215662`

### **Step 2: Update .env**

Edit `.env` file:
```bash
FOOTBALL_MATCHES=215662,592872,867946  # Replace with live match IDs
```

### **Step 3: Run!**

```bash
docker compose up --build
```

That's it! 🎉

---

## 📊 What You'll See

### **Ingestion Service Logs:**
```
Polling 3 matches
Match 215662: Real Madrid 2 - 1 Barcelona (67' - 2H)
Published event for match 215662
```

### **Analytics Service Logs:**
```
Analyzing 3 matches
Analyzing match 215662: Real Madrid 2 - 1 Barcelona
Snapshot saved for match 215662: HOME_SLIGHT_ADVANTAGE (momentum: 0.08)
```

### **Redis Data:**
```bash
# Check snapshots
docker exec -it sportbot-redis redis-cli
KEYS latest_match_snapshot:*
GET latest_match_snapshot:215662
```

You'll see JSON with:
- **momentumMetrics**: drift, volatility, confidence
- **matchPrediction**: win probabilities, expected score
- **goalForecast**: next goal prediction

---

## 📈 Example Output

```json
{
  "matchId": "215662",
  "homeTeam": "Real Madrid",
  "awayTeam": "Barcelona",
  "minute": 67,
  "homeScore": 2,
  "awayScore": 1,
  "momentumMetrics": {
    "drift": 0.08,
    "volatility": 0.12,
    "confidence": 0.95
  },
  "matchPrediction": {
    "probabilityHomeWin": 0.65,
    "probabilityDraw": 0.20,
    "probabilityAwayWin": 0.15,
    "expectedFinalScore": "3-1",
    "probabilityMoreGoals": 0.72
  },
  "goalForecast": {
    "nextGoalProbability": 0.45,
    "nextGoalTeam": "HOME",
    "nextGoalMinute": 72
  },
  "matchState": "HOME_SLIGHT_ADVANTAGE"
}
```

---

## 🔍 Verify It's Working

### **1. Check Services are Running**
```bash
docker compose ps
```

Should show:
- ✅ sportbot-redis (healthy)
- ✅ sportbot-ingestion (running)
- ✅ sportbot-analytics (running)

### **2. Check Logs**
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f ingestion-service
docker compose logs -f analytics-service
```

### **3. Check Redis**
```bash
# Connect to Redis
docker exec -it sportbot-redis redis-cli

# List all keys
KEYS *

# Get a snapshot
GET latest_match_snapshot:215662

# Subscribe to events
SUBSCRIBE match-events:*
```

---

## 🎯 What Each Service Does

### **Ingestion Service**
- Polls API-Football every 15 seconds
- Fetches match data (score, possession, shots, etc.)
- Publishes to Redis channel: `match-events:{matchId}`

### **Analytics Service**
- Subscribes to match events
- Analyzes momentum (Bayesian)
- Forecasts goals (ARIMA)
- Simulates outcomes (Monte Carlo)
- Saves snapshots to Redis: `latest_match_snapshot:{matchId}`

### **Redis**
- Message broker (Pub/Sub)
- Data store (snapshots)

---

## ⚙️ Configuration

### **Change Polling Interval**

Edit `.env`:
```bash
POLL_INTERVAL=30s  # Poll every 30 seconds (saves API calls)
```

### **Change Forecast Horizon**

Edit `.env`:
```bash
MONTE_CARLO_HORIZON_MINUTES=90  # Full match simulation
ARIMA_HORIZON_MINUTES=15        # Predict next 15 minutes
```

### **Add More Matches**

Edit `.env`:
```bash
FOOTBALL_MATCHES=215662,592872,867946,123456,789012
```

---

## 🐛 Troubleshooting

### **Problem: No data appearing**

**Solution 1:** Check match IDs are live
```bash
curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
```

**Solution 2:** Check logs
```bash
docker compose logs ingestion-service | grep "No data"
```

**Solution 3:** Verify API key
```bash
curl -X GET "https://v3.football.api-sports.io/status" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
```

### **Problem: Services not starting**

**Solution:** Rebuild
```bash
docker compose down
docker compose up --build
```

### **Problem: Redis connection failed**

**Solution:** Check Redis is healthy
```bash
docker compose ps redis
docker exec -it sportbot-redis redis-cli PING
```

---

## 📊 Understanding the Analytics

### **Momentum (Bayesian Analysis)**

**Drift:**
- `> 0.10`: Home team dominating
- `0.03 to 0.10`: Home slight advantage
- `-0.03 to 0.03`: Balanced
- `-0.10 to -0.03`: Away slight advantage
- `< -0.10`: Away team dominating

**Volatility:**
- `< 0.05`: Controlled match
- `0.05 to 0.15`: Normal variability
- `> 0.15`: Chaotic match

### **Match Prediction (Monte Carlo)**

**Probabilities:**
- Sum always equals 1.0 (100%)
- Based on 10,000 simulations
- Accounts for current score and momentum

**Expected Score:**
- Most likely final score
- Based on simulation frequency

### **Goal Forecast (ARIMA)**

**Next Goal Probability:**
- Probability of goal in next N minutes
- Based on goal rate trend

**Next Goal Team:**
- Which team more likely to score
- Based on momentum and xG

---

## 🎓 Next Steps

### **Option 1: Use the Data**

Access snapshots programmatically:
```python
import redis
import json

r = redis.Redis(host='localhost', port=6379)
snapshot = json.loads(r.get('latest_match_snapshot:215662'))

print(f"Home Win: {snapshot['matchPrediction']['probabilityHomeWin']}")
```

### **Option 2: Build a Dashboard**

Create a simple web page that:
1. Connects to Redis
2. Reads snapshots
3. Displays predictions

### **Option 3: Betting Bot**

Use predictions to:
1. Identify value bets
2. Calculate expected value
3. Automate betting decisions

### **Option 4: Extend the System**

Add:
- WebSocket API for real-time updates
- React dashboard for visualization
- Historical data analysis
- Player-level statistics

---

## 📞 Support

### **Check Logs First**
```bash
docker compose logs -f
```

### **Verify Configuration**
```bash
cat .env
```

### **Test API Connection**
```bash
curl -X GET "https://v3.football.api-sports.io/status" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
```

### **Restart Everything**
```bash
docker compose down
docker compose up --build
```

---

## ✅ Success Checklist

- [ ] Got live match IDs from API
- [ ] Updated `.env` with match IDs
- [ ] Ran `docker compose up --build`
- [ ] Saw ingestion service polling
- [ ] Saw analytics service processing
- [ ] Found snapshots in Redis
- [ ] Verified JSON structure

**If all checked, you're good to go!** 🎉⚽

---

## 🎯 What You Have

A **fully functional football analytics system** that:
- ✅ Ingests live match data
- ✅ Analyzes momentum (Bayesian)
- ✅ Forecasts goals (ARIMA)
- ✅ Predicts outcomes (Monte Carlo)
- ✅ Stores results in Redis
- ✅ Updates every 15 seconds

**No dashboard needed - the data is ready to use!**

---

**Built with the same architecture as Finbot, adapted for football.** ⚽📊
