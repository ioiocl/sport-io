function MatchCard({ match }) {
  const {
    homeTeam,
    awayTeam,
    homeScore,
    awayScore,
    minute,
    status,
    momentumMetrics,
    matchPrediction,
    goalForecast,
    matchState
  } = match

  // Get momentum color
  const getMomentumColor = (drift) => {
    const value = parseFloat(drift)
    if (value > 0.10) return 'bg-green-500'
    if (value > 0.03) return 'bg-green-400'
    if (value < -0.10) return 'bg-red-500'
    if (value < -0.03) return 'bg-red-400'
    return 'bg-yellow-400'
  }

  // Get momentum percentage for bar
  const getMomentumPercentage = (drift) => {
    const value = parseFloat(drift)
    // Map -0.2 to 0.2 range to 0-100%
    return Math.min(100, Math.max(0, (value + 0.2) / 0.4 * 100))
  }

  // Format percentage
  const formatPercent = (value) => {
    return (parseFloat(value) * 100).toFixed(1) + '%'
  }

  // Get match state emoji
  const getStateEmoji = (state) => {
    switch(state) {
      case 'HOME_DOMINATING': return '🔥'
      case 'HOME_SLIGHT_ADVANTAGE': return '📈'
      case 'BALANCED': return '⚖️'
      case 'AWAY_SLIGHT_ADVANTAGE': return '📉'
      case 'AWAY_DOMINATING': return '❄️'
      default: return '⚽'
    }
  }

  return (
    <div className="bg-white rounded-2xl shadow-2xl overflow-hidden transform transition-all hover:scale-105">
      {/* Header */}
      <div className="bg-gradient-to-r from-purple-600 to-blue-600 p-4 text-white">
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm font-semibold px-3 py-1 bg-white/20 rounded-full">
            {status}
          </span>
          <span className="text-2xl font-bold">{minute}'</span>
        </div>
      </div>

      {/* Score */}
      <div className="p-6 bg-gradient-to-b from-gray-50 to-white">
        <div className="flex justify-between items-center mb-4">
          {/* Home Team */}
          <div className="flex-1 text-center">
            <div className="text-sm font-semibold text-gray-600 mb-2 truncate">
              {homeTeam}
            </div>
            <div className="text-5xl font-bold text-gray-900">
              {homeScore}
            </div>
          </div>

          {/* VS */}
          <div className="px-4 text-2xl font-bold text-gray-400">
            -
          </div>

          {/* Away Team */}
          <div className="flex-1 text-center">
            <div className="text-sm font-semibold text-gray-600 mb-2 truncate">
              {awayTeam}
            </div>
            <div className="text-5xl font-bold text-gray-900">
              {awayScore}
            </div>
          </div>
        </div>

        {/* Match State */}
        <div className="text-center mb-4">
          <span className="inline-flex items-center gap-2 px-4 py-2 bg-purple-100 text-purple-800 rounded-full text-sm font-semibold">
            {getStateEmoji(matchState)} {matchState?.replace(/_/g, ' ')}
          </span>
        </div>

        {/* Momentum Bar */}
        {momentumMetrics && (
          <div className="mb-6">
            <div className="flex justify-between text-xs text-gray-600 mb-2">
              <span>← Away</span>
              <span className="font-semibold">Momentum</span>
              <span>Home →</span>
            </div>
            <div className="h-3 bg-gray-200 rounded-full overflow-hidden">
              <div 
                className={`h-full transition-all duration-500 ${getMomentumColor(momentumMetrics.drift)}`}
                style={{ width: `${getMomentumPercentage(momentumMetrics.drift)}%` }}
              />
            </div>
            <div className="text-center mt-1 text-xs text-gray-500">
              Drift: {parseFloat(momentumMetrics.drift).toFixed(3)} | 
              Vol: {parseFloat(momentumMetrics.volatility).toFixed(3)} | 
              Conf: {formatPercent(momentumMetrics.confidence)}
            </div>
          </div>
        )}

        {/* Win Probabilities */}
        {matchPrediction && (
          <div className="space-y-2 mb-6">
            <div className="text-sm font-semibold text-gray-700 mb-3">
              🎯 Win Probabilities
            </div>
            
            {/* Home Win */}
            <div className="flex items-center gap-2">
              <div className="w-20 text-xs text-gray-600">Home Win</div>
              <div className="flex-1 h-6 bg-gray-200 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-green-500 flex items-center justify-end pr-2"
                  style={{ width: formatPercent(matchPrediction.probabilityHomeWin) }}
                >
                  <span className="text-xs font-bold text-white">
                    {formatPercent(matchPrediction.probabilityHomeWin)}
                  </span>
                </div>
              </div>
            </div>

            {/* Draw */}
            <div className="flex items-center gap-2">
              <div className="w-20 text-xs text-gray-600">Draw</div>
              <div className="flex-1 h-6 bg-gray-200 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-yellow-500 flex items-center justify-end pr-2"
                  style={{ width: formatPercent(matchPrediction.probabilityDraw) }}
                >
                  <span className="text-xs font-bold text-white">
                    {formatPercent(matchPrediction.probabilityDraw)}
                  </span>
                </div>
              </div>
            </div>

            {/* Away Win */}
            <div className="flex items-center gap-2">
              <div className="w-20 text-xs text-gray-600">Away Win</div>
              <div className="flex-1 h-6 bg-gray-200 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-red-500 flex items-center justify-end pr-2"
                  style={{ width: formatPercent(matchPrediction.probabilityAwayWin) }}
                >
                  <span className="text-xs font-bold text-white">
                    {formatPercent(matchPrediction.probabilityAwayWin)}
                  </span>
                </div>
              </div>
            </div>

            {/* Expected Score */}
            <div className="mt-3 p-3 bg-purple-50 rounded-lg">
              <div className="text-xs text-gray-600 mb-1">Expected Final Score</div>
              <div className="text-2xl font-bold text-purple-600">
                {matchPrediction.expectedFinalScore}
              </div>
            </div>
          </div>
        )}

        {/* Goal Forecast */}
        {goalForecast && (
          <div className="p-4 bg-blue-50 rounded-lg">
            <div className="text-sm font-semibold text-gray-700 mb-2">
              ⚡ Next Goal Prediction
            </div>
            <div className="flex justify-between items-center">
              <div>
                <div className="text-xs text-gray-600">Probability</div>
                <div className="text-xl font-bold text-blue-600">
                  {formatPercent(goalForecast.nextGoalProbability)}
                </div>
              </div>
              <div className="text-right">
                <div className="text-xs text-gray-600">Team</div>
                <div className="text-xl font-bold text-blue-600">
                  {goalForecast.nextGoalTeam}
                </div>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Footer */}
      <div className="px-6 py-3 bg-gray-50 border-t border-gray-200">
        <div className="flex justify-between text-xs text-gray-500">
          <span>Sample: {match.sampleSize} events</span>
          <span>Updated: {new Date(match.timestamp).toLocaleTimeString()}</span>
        </div>
      </div>
    </div>
  )
}

export default MatchCard
