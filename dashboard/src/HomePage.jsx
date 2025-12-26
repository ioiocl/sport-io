import { useState, useEffect } from 'react'

function HomePage({ onStartAnalysis }) {
  const [liveMatches, setLiveMatches] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)
  const [selectedMatches, setSelectedMatches] = useState(new Set())
  const [apiStatus, setApiStatus] = useState('checking') // checking, available, unavailable

  useEffect(() => {
    checkLiveMatches()
    const interval = setInterval(checkLiveMatches, 30000) // Check every 30 seconds
    return () => clearInterval(interval)
  }, [])

  const checkLiveMatches = async () => {
    try {
      setLoading(true)
      // Use backend proxy to avoid CORS issues
        const response = await fetch('/api/live-matches')
      
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }
      
      const data = await response.json()
      
      if (data.response && data.response.length > 0) {
        setLiveMatches(data.response)
        setApiStatus('available')
        setError(null)
      } else {
        setLiveMatches([])
        setApiStatus('unavailable')
        setError('No live matches currently available')
      }
    } catch (err) {
      console.error('Error fetching live matches:', err)
      setError('Failed to fetch live matches')
      setApiStatus('unavailable')
    } finally {
      setLoading(false)
    }
  }

  const toggleMatchSelection = (matchId) => {
    const newSelection = new Set(selectedMatches)
    if (newSelection.has(matchId)) {
      newSelection.delete(matchId)
    } else {
      newSelection.add(matchId)
    }
    setSelectedMatches(newSelection)
  }

  const handleStartAnalysis = () => {
    if (selectedMatches.size > 0) {
      onStartAnalysis(Array.from(selectedMatches))
    }
  }

  const getStatusColor = () => {
    switch (apiStatus) {
      case 'available': return 'bg-green-500'
      case 'unavailable': return 'bg-yellow-500'
      case 'checking': return 'bg-gray-500 animate-pulse'
      default: return 'bg-red-500'
    }
  }

  const getStatusText = () => {
    switch (apiStatus) {
      case 'available': return `${liveMatches.length} Live Matches Available`
      case 'unavailable': return 'No Live Matches'
      case 'checking': return 'Checking...'
      default: return 'Error'
    }
  }

  return (
    <div className="min-h-screen p-8">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="mb-8 text-center">
          <h1 className="text-5xl font-bold text-white mb-4">
            ⚽ Sportbot Analytics
          </h1>
          <p className="text-xl text-white/80 mb-6">
            Real-time football match analysis using Bayesian, ARIMA, and Monte Carlo methods
          </p>
          
          {/* Status Semaphore */}
          <div className="inline-flex items-center gap-3 px-6 py-3 bg-white/10 backdrop-blur-sm rounded-full">
            <div className={`w-4 h-4 rounded-full ${getStatusColor()}`}></div>
            <span className="text-white font-medium">{getStatusText()}</span>
            <button
              onClick={checkLiveMatches}
              disabled={loading}
              className="ml-2 px-3 py-1 bg-white/20 hover:bg-white/30 rounded-lg text-sm transition-colors disabled:opacity-50"
            >
              {loading ? '⟳' : '🔄'} Refresh
            </button>
          </div>
        </div>

        {/* Error Message */}
        {error && apiStatus === 'unavailable' && (
          <div className="mb-6 p-6 bg-yellow-500/20 border border-yellow-500 rounded-lg text-white text-center">
            <p className="text-2xl mb-2">⚠️</p>
            <p className="font-semibold mb-2">{error}</p>
            <p className="text-sm text-white/80">
              This could be because:
            </p>
            <ul className="text-sm text-white/70 mt-2 space-y-1">
              <li>• No matches are currently being played</li>
              <li>• It's off-season or between match days</li>
              <li>• Check back during typical match times (weekends, evenings)</li>
            </ul>
          </div>
        )}

        {/* Loading State */}
        {loading && liveMatches.length === 0 && (
          <div className="text-center py-12">
            <div className="inline-block p-8 bg-white/10 rounded-2xl backdrop-blur-sm">
              <div className="text-6xl mb-4 animate-bounce">⚽</div>
              <h2 className="text-2xl font-bold text-white mb-2">
                Scanning for live matches...
              </h2>
            </div>
          </div>
        )}

        {/* Live Matches Grid */}
        {!loading && liveMatches.length > 0 && (
          <>
            <div className="mb-6 text-center text-white/80">
              <p>Select matches to analyze ({selectedMatches.size} selected)</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mb-8">
              {liveMatches.map((match) => {
                const isSelected = selectedMatches.has(match.fixture.id.toString())
                return (
                  <div
                    key={match.fixture.id}
                    onClick={() => toggleMatchSelection(match.fixture.id.toString())}
                    className={`p-6 rounded-xl cursor-pointer transition-all ${
                      isSelected
                        ? 'bg-blue-500/30 border-2 border-blue-400 shadow-lg shadow-blue-500/50'
                        : 'bg-white/10 border-2 border-white/20 hover:bg-white/20'
                    } backdrop-blur-sm`}
                  >
                    {/* Match Status Badge */}
                    <div className="flex items-center justify-between mb-4">
                      <span className="px-3 py-1 bg-green-500 text-white text-xs font-bold rounded-full">
                        {match.fixture.status.short} {match.fixture.status.elapsed}'
                      </span>
                      {isSelected && (
                        <span className="text-2xl">✓</span>
                      )}
                    </div>

                    {/* Teams */}
                    <div className="space-y-3">
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <img 
                            src={match.teams.home.logo} 
                            alt={match.teams.home.name}
                            className="w-8 h-8"
                          />
                          <span className="text-white font-semibold">
                            {match.teams.home.name}
                          </span>
                        </div>
                        <span className="text-2xl font-bold text-white">
                          {match.goals.home ?? 0}
                        </span>
                      </div>

                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-3">
                          <img 
                            src={match.teams.away.logo} 
                            alt={match.teams.away.name}
                            className="w-8 h-8"
                          />
                          <span className="text-white font-semibold">
                            {match.teams.away.name}
                          </span>
                        </div>
                        <span className="text-2xl font-bold text-white">
                          {match.goals.away ?? 0}
                        </span>
                      </div>
                    </div>

                    {/* League Info */}
                    <div className="mt-4 pt-4 border-t border-white/20">
                      <p className="text-xs text-white/60">
                        {match.league.name} • {match.league.country}
                      </p>
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Start Analysis Button */}
            <div className="text-center">
              <button
                onClick={handleStartAnalysis}
                disabled={selectedMatches.size === 0}
                className="px-8 py-4 bg-gradient-to-r from-blue-500 to-purple-600 text-white text-xl font-bold rounded-xl hover:from-blue-600 hover:to-purple-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-lg hover:shadow-xl transform hover:scale-105"
              >
                {selectedMatches.size === 0 
                  ? '📊 Select matches to analyze'
                  : `🚀 Start Analysis (${selectedMatches.size} match${selectedMatches.size !== 1 ? 'es' : ''})`
                }
              </button>
            </div>
          </>
        )}

        {/* Instructions when no matches */}
        {!loading && liveMatches.length === 0 && (
          <div className="mt-8 p-8 bg-white/10 backdrop-blur-sm rounded-2xl text-white">
            <h3 className="text-2xl font-bold mb-4">📋 How to Use Sportbot</h3>
            <ol className="space-y-3 text-white/80">
              <li className="flex gap-3">
                <span className="font-bold">1.</span>
                <span>Wait for live football matches to start (typically weekends and weekday evenings)</span>
              </li>
              <li className="flex gap-3">
                <span className="font-bold">2.</span>
                <span>Click "Refresh" to check for new live matches</span>
              </li>
              <li className="flex gap-3">
                <span className="font-bold">3.</span>
                <span>Select the matches you want to analyze</span>
              </li>
              <li className="flex gap-3">
                <span className="font-bold">4.</span>
                <span>Click "Start Analysis" to begin real-time analytics</span>
              </li>
              <li className="flex gap-3">
                <span className="font-bold">5.</span>
                <span>View live predictions, momentum analysis, and match simulations</span>
              </li>
            </ol>
          </div>
        )}
      </div>
    </div>
  )
}

export default HomePage
