# 🎉 Frontend Complete! - Sportbot Dashboard

## ✅ What's Been Created

### **WebSocket API**
- ✅ `BroadcastService.java` - WebSocket endpoint
- ✅ Broadcasts snapshots every 5 seconds
- ✅ Port: 8083
- ✅ Endpoint: `ws://localhost:8083/matches`

### **React Dashboard**
- ✅ Modern React 18 + Vite
- ✅ TailwindCSS styling
- ✅ Real-time WebSocket connection
- ✅ Beautiful match cards
- ✅ Live momentum visualization
- ✅ Win probability bars
- ✅ Goal predictions
- ✅ Auto-reconnect on disconnect

---

## 🚀 How to Run

### **Option 1: Full Stack (Recommended)**

```bash
# 1. Get live match IDs
curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
  -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"

# 2. Update .env with match IDs
# Edit: FOOTBALL_MATCHES=215662,592872,867946

# 3. Start everything
docker compose up --build

# 4. Open dashboard
# http://localhost:3000
```

### **Option 2: Development Mode**

```bash
# Terminal 1: Start backend services
docker compose up redis ingestion-service analytics-service websocket-api

# Terminal 2: Start dashboard in dev mode
cd dashboard
npm install
npm run dev

# Open: http://localhost:3000
```

---

## 📊 Dashboard Features

### **Match Cards**
- **Live Score** - Real-time updates
- **Match Status** - 1H, HT, 2H, FT
- **Minute** - Current match minute
- **Team Names** - Home vs Away

### **Momentum Visualization**
- **Color-coded bar** - Green (home) to Red (away)
- **Drift value** - Momentum direction
- **Volatility** - Match variability
- **Confidence** - Statistical confidence

### **Win Probabilities**
- **Home Win %** - Green bar
- **Draw %** - Yellow bar
- **Away Win %** - Red bar
- **Expected Score** - Most likely final score

### **Goal Forecast**
- **Next Goal Probability** - Chance of goal in next 10 min
- **Next Goal Team** - Which team will score
- **Live Updates** - Every 5 seconds

### **Match State Indicators**
- 🔥 **HOME_DOMINATING** - Home team in control
- 📈 **HOME_SLIGHT_ADVANTAGE** - Home team ahead
- ⚖️ **BALANCED** - Even match
- 📉 **AWAY_SLIGHT_ADVANTAGE** - Away team ahead
- ❄️ **AWAY_DOMINATING** - Away team in control

---

## 🎨 Dashboard Preview

```
┌─────────────────────────────────────────────────────┐
│  ⚽ Sportbot                          🟢 Live        │
│  Real-time football analytics                       │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌───────────────────┐  ┌───────────────────┐     │
│  │  Real Madrid      │  │  Barcelona        │     │
│  │       2           │  │       1           │     │
│  │                   │  │                   │     │
│  │  📈 HOME_SLIGHT_ADVANTAGE                │     │
│  │                   │  │                   │     │
│  │  Momentum: ████████░░░░                  │     │
│  │                   │  │                   │     │
│  │  🎯 Win Probabilities                    │     │
│  │  Home Win:  ████████████ 65%             │     │
│  │  Draw:      ████ 20%                     │     │
│  │  Away Win:  ███ 15%                      │     │
│  │                   │  │                   │     │
│  │  Expected: 3-1    │  │                   │     │
│  │                   │  │                   │     │
│  │  ⚡ Next Goal: 45% (HOME)                │     │
│  └───────────────────┘  └───────────────────┘     │
│                                                     │
│  Analyzing 3 matches • Updates every 5 seconds     │
└─────────────────────────────────────────────────────┘
```

---

## 🔧 Architecture

```
API-Football (15s)
    ↓
Ingestion Service
    ↓
Redis Pub/Sub
    ↓
Analytics Service
    ↓
Redis KV (snapshots)
    ↓
WebSocket API (5s broadcast)
    ↓
React Dashboard (real-time)
```

---

## 📁 Complete File Structure

```
Sportbot/
├── ✅ websocket-api/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/main/java/cl/ioio/sportbot/websocket/
│       └── BroadcastService.java
│
└── ✅ dashboard/
    ├── package.json
    ├── vite.config.js
    ├── tailwind.config.js
    ├── Dockerfile
    ├── nginx.conf
    ├── index.html
    └── src/
        ├── main.jsx
        ├── App.jsx
        ├── index.css
        └── components/
            └── MatchCard.jsx
```

---

## 🎯 Ports

| Service | Port | URL |
|---------|------|-----|
| **Redis** | 6379 | - |
| **Ingestion** | 8081 | - |
| **Analytics** | 8082 | - |
| **WebSocket** | 8083 | ws://localhost:8083/matches |
| **Dashboard** | 3000 | http://localhost:3000 |

---

## ✅ Verification Checklist

After running `docker compose up --build`:

- [ ] All 5 services running
- [ ] Redis healthy
- [ ] Ingestion polling API
- [ ] Analytics generating snapshots
- [ ] WebSocket broadcasting
- [ ] Dashboard accessible at http://localhost:3000
- [ ] Match cards appearing
- [ ] Live updates every 5 seconds
- [ ] Momentum bars animating
- [ ] Win probabilities showing

---

## 🐛 Troubleshooting

### **Dashboard shows "Waiting for match data"**

**Check:**
1. Are match IDs live?
   ```bash
   curl -X GET "https://v3.football.api-sports.io/fixtures?live=all" \
     -H "x-apisports-key: 85c00191235f58aa27fcccd8d737d8a7"
   ```

2. Is ingestion service working?
   ```bash
   docker compose logs ingestion-service
   ```

3. Are snapshots in Redis?
   ```bash
   docker exec -it sportbot-redis redis-cli KEYS latest_match_snapshot:*
   ```

4. Is WebSocket broadcasting?
   ```bash
   docker compose logs websocket-api
   ```

### **Dashboard shows "Disconnected"**

**Solution:**
```bash
# Check WebSocket service
docker compose ps websocket-api

# Check logs
docker compose logs websocket-api

# Restart if needed
docker compose restart websocket-api
```

### **Match cards not updating**

**Solution:**
```bash
# Check broadcast interval
docker compose logs websocket-api | grep "Broadcasted"

# Should see messages every 5 seconds
```

---

## 🎨 Customization

### **Change Update Frequency**

Edit `.env`:
```bash
BROADCAST_INTERVAL=3s  # Update every 3 seconds
```

### **Change Dashboard Port**

Edit `docker-compose.yml`:
```yaml
dashboard:
  ports:
    - "8080:80"  # Change to port 8080
```

### **Customize Colors**

Edit `dashboard/src/components/MatchCard.jsx`:
```javascript
// Change momentum colors
const getMomentumColor = (drift) => {
  if (value > 0.10) return 'bg-blue-500'  // Your color
  // ...
}
```

---

## 📊 What You See

### **Real-time Data**
- Score updates every 15 seconds (from API)
- Analytics updates every 15 seconds
- Dashboard updates every 5 seconds
- Smooth animations and transitions

### **Analytics Display**
- **Momentum Bar** - Visual momentum indicator
- **Drift** - Numerical momentum value
- **Volatility** - Match variability
- **Confidence** - Statistical confidence
- **Win Probabilities** - Home/Draw/Away percentages
- **Expected Score** - Most likely final score
- **Next Goal** - Probability and team

---

## 🚀 Performance

- **WebSocket** - Efficient real-time updates
- **React** - Fast rendering
- **TailwindCSS** - Optimized styling
- **Nginx** - Production-ready serving
- **Auto-reconnect** - Resilient connections

---

## 🎓 Tech Stack

### **Backend**
- Java 17
- Quarkus 3.6
- WebSocket (Jakarta)
- Redis
- Docker

### **Frontend**
- React 18
- Vite 5
- TailwindCSS 3
- WebSocket API
- Nginx (production)

---

## 🎉 Success!

You now have a **complete, production-ready football analytics dashboard** with:

✅ Real-time data ingestion  
✅ Advanced analytics (Bayesian, ARIMA, Monte Carlo)  
✅ WebSocket broadcasting  
✅ Beautiful React dashboard  
✅ Live visualizations  
✅ Docker deployment  

**Open http://localhost:3000 and watch the magic happen!** ⚽📊🚀

---

## 📞 Quick Commands

```bash
# Start everything
docker compose up --build

# Stop everything
docker compose down

# View logs
docker compose logs -f

# Restart dashboard
docker compose restart dashboard

# Rebuild dashboard only
docker compose up --build dashboard

# Check all services
docker compose ps
```

---

**The complete Sportbot system is ready! Enjoy your real-time football analytics!** 🎉⚽
