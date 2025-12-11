# Sportbot Home Page - User Guide

## 🎯 Overview

The new home page provides a live match discovery interface with a visual semaphore indicator to show data availability status.

## 🚀 Access the Dashboard

Open your browser and navigate to:
```
http://localhost:3000
```

## 📊 Features

### 1. **Status Semaphore** (Top of page)
Visual indicator showing real-time API status:

- **🟢 Green** - Live matches available
- **🟡 Yellow** - No live matches (but API working)
- **⚪ Gray (pulsing)** - Checking for matches
- **🔴 Red** - API error

### 2. **Live Match Discovery**
- Automatically scans for live football matches
- Refreshes every 30 seconds
- Manual refresh button available
- Shows match details:
  - Team names and logos
  - Current score
  - Match minute
  - League information

### 3. **Match Selection**
- Click on any match card to select it
- Selected matches are highlighted in blue with a checkmark
- Select multiple matches for simultaneous analysis
- Selection count displayed on the "Start Analysis" button

### 4. **Start Analysis**
- Click the "Start Analysis" button when matches are selected
- System will:
  - Update `.env` with selected match IDs
  - Start ingestion service
  - Begin real-time analytics
  - Display live predictions and momentum analysis

### 5. **Analysis View**
Once analysis starts:
- View real-time match snapshots
- See Bayesian momentum metrics
- ARIMA goal forecasts
- Monte Carlo match simulations
- Click "Back to Home" to return and select different matches

## 🔧 How It Works

### Data Flow:
```
Home Page → API-Football → Live Matches → User Selection → Analysis View
                                                              ↓
                                                    WebSocket Connection
                                                              ↓
                                                    Real-time Analytics
```

### Components:
1. **HomePage.jsx** - Match discovery and selection
2. **App.jsx** - Analysis view with WebSocket connection
3. **MatchCard.jsx** - Individual match analytics display

## 📝 Current Status

### ⚠️ Important Note:
Currently showing **0 live matches** because:
- System date is November 16, 2025 (future date)
- No real football matches exist in the future
- API-Football returns real-time data only

### When Live Matches Are Available:
The home page will automatically display them with:
- Real-time scores
- Match status (1H, HT, 2H, etc.)
- Team information
- League details

## 🎮 Usage Instructions

### Step 1: Check for Live Matches
1. Open http://localhost:3000
2. Look at the semaphore indicator
3. If yellow, wait for match times (typically weekends/evenings)
4. Click "Refresh" to check manually

### Step 2: Select Matches
1. When matches appear, click on cards to select
2. Selected matches show blue highlight
3. Select 1-5 matches for optimal performance

### Step 3: Start Analysis
1. Click "Start Analysis" button
2. Wait for WebSocket connection (green indicator)
3. View real-time analytics as they update

### Step 4: Monitor Analysis
- Momentum metrics update every 15 seconds
- Goal forecasts refresh with new data
- Match simulations recalculate continuously
- Click "Back to Home" to change selection

## 🔍 Troubleshooting

### No Matches Showing
- **Check semaphore color**: Yellow = no matches (normal), Red = error
- **Verify API key**: Check `.env` file has valid `FOOTBALL_API_KEY`
- **Check match times**: Most matches on weekends 12:00-22:00 UTC
- **Manual refresh**: Click refresh button

### Can't Select Matches
- **Ensure matches are loaded**: Wait for loading animation to finish
- **Check browser console**: Look for JavaScript errors
- **Try different browser**: Test in Chrome/Firefox

### Analysis Not Starting
- **Check services**: Run `docker compose ps` - all should be "Up"
- **Verify selection**: At least one match must be selected
- **Check logs**: `docker compose logs websocket-api`

## 🛠️ Technical Details

### API Endpoint Used:
```
GET https://v3.football.api-sports.io/fixtures?live=all
Headers:
  x-apisports-key: YOUR_API_KEY
  x-apisports-host: v3.football.api-sports.io
```

### Response Structure:
```json
{
  "results": 10,
  "response": [
    {
      "fixture": {
        "id": 1234567,
        "status": { "short": "1H", "elapsed": 23 }
      },
      "teams": {
        "home": { "name": "Team A", "logo": "..." },
        "away": { "name": "Team B", "logo": "..." }
      },
      "goals": { "home": 1, "away": 0 },
      "league": { "name": "Premier League", "country": "England" }
    }
  ]
}
```

### State Management:
- **view**: 'home' | 'analysis'
- **selectedMatchIds**: Array of match IDs
- **liveMatches**: Array of match data from API
- **apiStatus**: 'checking' | 'available' | 'unavailable'

## 📚 Next Steps

### To Test with Real Data:
1. Wait for actual match times
2. Or modify code to use historical match IDs
3. Or create mock data generator

### To Customize:
- Edit `HomePage.jsx` for UI changes
- Modify refresh interval (default: 30s)
- Change selection limits
- Add filters (league, country, etc.)

## 🎨 UI Features

- **Responsive design**: Works on desktop, tablet, mobile
- **Dark theme**: Modern gradient background
- **Smooth animations**: Hover effects, transitions
- **Visual feedback**: Selection states, loading indicators
- **Accessibility**: Clear status indicators, readable text

---

**Dashboard URL**: http://localhost:3000  
**WebSocket API**: ws://localhost:8083/matches  
**Redis**: localhost:6379
