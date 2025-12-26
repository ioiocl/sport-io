import { useState, useEffect } from 'react'
import MatchCard from './components/MatchCard'
import HomePage from './HomePage'

function App() {
  const [view, setView] = useState('home') // 'home' or 'analysis'
  const [selectedMatchIds, setSelectedMatchIds] = useState([])
  const [matches, setMatches] = useState({})
  const [connected, setConnected] = useState(false)
  const [error, setError] = useState(null)

  const handleStartAnalysis = async (matchIds) => {
    setSelectedMatchIds(matchIds)
    
    // Send selected match IDs to backend to start ingestion
    try {
      await fetch('/api/start-analysis', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({ matchIds })
      })
      console.log('Started analysis for matches:', matchIds)
    } catch (err) {
      console.error('Error starting analysis:', err)
    }
    
    setView('analysis')
  }

  const handleBackToHome = () => {
    setView('home')
    setMatches({})
  }

  useEffect(() => {
    if (view !== 'analysis') return

  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const ws = new WebSocket(`${protocol}//${window.location.host}/matches`)

    ws.onopen = () => {
      console.log('Connected to WebSocket')
      setConnected(true)
      setError(null)
    }

    ws.onmessage = (event) => {
      try {
        const snapshot = JSON.parse(event.data)
        setMatches(prev => ({
          ...prev,
          [snapshot.matchId]: snapshot
        }))
      } catch (err) {
        console.error('Error parsing message:', err)
      }
    }

    ws.onerror = (err) => {
      console.error('WebSocket error:', err)
      setError('Connection error')
      setConnected(false)
    }

    ws.onclose = () => {
      console.log('Disconnected from WebSocket')
      setConnected(false)
      // Reconnect after 5 seconds
      setTimeout(() => {
        window.location.reload()
      }, 5000)
    }

    return () => {
      ws.close()
    }
  }, [view])

  if (view === 'home') {
    return <HomePage onStartAnalysis={handleStartAnalysis} />
  }

  return (
    <div className="min-h-screen p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8">
          <div className="flex items-center justify-between mb-4">
            <h1 className="text-4xl font-bold text-white flex items-center gap-3">
              ⚽ Sportbot
              <span className="text-sm font-normal px-3 py-1 rounded-full bg-white/20">
                {connected ? '🟢 Live' : '🔴 Disconnected'}
              </span>
            </h1>
            <button
              onClick={handleBackToHome}
              className="px-4 py-2 bg-white/10 hover:bg-white/20 text-white rounded-lg transition-colors"
            >
              ← Back to Home
            </button>
          </div>
          <p className="text-white/80">
            Real-time football analytics powered by Bayesian, ARIMA, and Monte Carlo
          </p>
          <p className="text-white/60 text-sm mt-1">
            Analyzing {selectedMatchIds.length} match{selectedMatchIds.length !== 1 ? 'es' : ''}
          </p>
        </div>

        {/* Error Message */}
        {error && (
          <div className="mb-6 p-4 bg-red-500/20 border border-red-500 rounded-lg text-white">
            <p className="font-semibold">⚠️ {error}</p>
            <p className="text-sm mt-1">Reconnecting in 5 seconds...</p>
          </div>
        )}

        {/* No Data Message */}
        {connected && Object.keys(matches).length === 0 && (
          <div className="text-center py-12">
            <div className="inline-block p-8 bg-white/10 rounded-2xl backdrop-blur-sm">
              <div className="text-6xl mb-4">⏳</div>
              <h2 className="text-2xl font-bold text-white mb-2">
                Waiting for match data...
              </h2>
              <p className="text-white/70">
                Make sure matches are configured and services are running
              </p>
            </div>
          </div>
        )}

        {/* Match Cards Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 xl:grid-cols-3 gap-6">
          {Object.values(matches).map(match => (
            <MatchCard key={match.matchId} match={match} />
          ))}
        </div>

        {/* Footer */}
        {Object.keys(matches).length > 0 && (
          <div className="mt-8 text-center text-white/60 text-sm">
            <p>
              Analyzing {Object.keys(matches).length} match{Object.keys(matches).length !== 1 ? 'es' : ''}
              {' • '}
              Updates every 5 seconds
            </p>
          </div>
        )}
      </div>
    </div>
  )
}

export default App
