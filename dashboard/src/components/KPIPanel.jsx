import { useState } from 'react'

function KPIPanel({ match }) {
  const [activeTab, setActiveTab] = useState('overview')
  
  const { kpis, homeStats, awayStats, homeTeam, awayTeam } = match
  
  if (!kpis) {
    return (
      <div className="bg-white/5 rounded-xl p-4 text-white/60 text-center">
        <p>KPI data not available yet</p>
      </div>
    )
  }

  const formatPercent = (value) => {
    if (value === null || value === undefined) return '-'
    return (parseFloat(value) * 100).toFixed(1) + '%'
  }

  const formatDecimal = (value) => {
    if (value === null || value === undefined) return '-'
    return parseFloat(value).toFixed(2)
  }

  const getBarColor = (value, isHome = true) => {
    const v = parseFloat(value) || 0
    if (v > 0.7) return isHome ? 'bg-green-500' : 'bg-red-500'
    if (v > 0.4) return isHome ? 'bg-green-400' : 'bg-red-400'
    return 'bg-gray-400'
  }

  const ComparisonBar = ({ homeValue, awayValue, label, inverse = false }) => {
    const home = parseFloat(homeValue) || 0
    const away = parseFloat(awayValue) || 0
    const total = home + away || 1
    const homePercent = (home / total) * 100
    
    return (
      <div className="mb-3">
        <div className="flex justify-between text-xs text-white/70 mb-1">
          <span>{formatPercent(homeValue)}</span>
          <span className="font-medium">{label}</span>
          <span>{formatPercent(awayValue)}</span>
        </div>
        <div className="h-2 bg-white/10 rounded-full overflow-hidden flex">
          <div 
            className={`h-full ${inverse ? 'bg-red-500' : 'bg-green-500'} transition-all duration-500`}
            style={{ width: `${homePercent}%` }}
          />
          <div 
            className={`h-full ${inverse ? 'bg-green-500' : 'bg-red-500'} transition-all duration-500`}
            style={{ width: `${100 - homePercent}%` }}
          />
        </div>
      </div>
    )
  }

  const AlertBadge = ({ active, label, color }) => {
    if (!active) return null
    return (
      <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs font-semibold ${color} animate-pulse`}>
        {label}
      </span>
    )
  }

  const StatTile = ({ label, homeValue, awayValue, unit = '' }) => (
    <div className="bg-white/5 rounded-lg p-3">
      <div className="text-xs text-white/60 mb-2 text-center">{label}</div>
      <div className="flex justify-between items-center">
        <span className="text-lg font-bold text-green-400">{homeValue}{unit}</span>
        <span className="text-white/40">vs</span>
        <span className="text-lg font-bold text-red-400">{awayValue}{unit}</span>
      </div>
    </div>
  )

  return (
    <div className="bg-white/5 backdrop-blur-sm rounded-xl overflow-hidden">
      {/* Tab Navigation */}
      <div className="flex border-b border-white/10">
        {['overview', 'attack', 'defense', 'timeline'].map(tab => (
          <button
            key={tab}
            onClick={() => setActiveTab(tab)}
            className={`flex-1 px-4 py-3 text-sm font-medium transition-colors ${
              activeTab === tab 
                ? 'bg-white/10 text-white border-b-2 border-purple-500' 
                : 'text-white/60 hover:text-white hover:bg-white/5'
            }`}
          >
            {tab.charAt(0).toUpperCase() + tab.slice(1)}
          </button>
        ))}
      </div>

      <div className="p-4">
        {/* Alert Badges */}
        <div className="flex flex-wrap gap-2 mb-4">
          <AlertBadge active={kpis.homeHighPressAlert} label="🔥 Home High Press" color="bg-orange-500/20 text-orange-300" />
          <AlertBadge active={kpis.awayHighPressAlert} label="🔥 Away High Press" color="bg-orange-500/20 text-orange-300" />
          <AlertBadge active={kpis.homeCardRiskAlert} label="🟨 Home Card Risk" color="bg-yellow-500/20 text-yellow-300" />
          <AlertBadge active={kpis.awayCardRiskAlert} label="🟨 Away Card Risk" color="bg-yellow-500/20 text-yellow-300" />
          <AlertBadge active={kpis.homeGoalImminentAlert} label="⚽ Home Goal Imminent" color="bg-green-500/20 text-green-300" />
          <AlertBadge active={kpis.awayGoalImminentAlert} label="⚽ Away Goal Imminent" color="bg-red-500/20 text-red-300" />
        </div>

        {/* Overview Tab */}
        {activeTab === 'overview' && (
          <div className="space-y-4">
            {/* Possession Momentum */}
            <div className="bg-white/5 rounded-lg p-4">
              <div className="text-sm font-semibold text-white mb-3">📊 Possession Momentum</div>
              <div className="flex items-center justify-center">
                <div className="text-3xl font-bold">
                  {parseFloat(kpis.possessionMomentum) > 0 ? (
                    <span className="text-green-400">↗ {formatDecimal(kpis.possessionMomentum)}</span>
                  ) : parseFloat(kpis.possessionMomentum) < 0 ? (
                    <span className="text-red-400">↘ {formatDecimal(kpis.possessionMomentum)}</span>
                  ) : (
                    <span className="text-yellow-400">→ 0.00</span>
                  )}
                </div>
              </div>
              <div className="text-xs text-white/50 text-center mt-2">
                {parseFloat(kpis.possessionMomentum) > 0 ? `${homeTeam} gaining control` : 
                 parseFloat(kpis.possessionMomentum) < 0 ? `${awayTeam} gaining control` : 'Balanced'}
              </div>
            </div>

            {/* Key Comparisons */}
            <div className="grid grid-cols-2 gap-3">
              <StatTile 
                label="Shot Pressure" 
                homeValue={formatPercent(kpis.homeShotPressure)} 
                awayValue={formatPercent(kpis.awayShotPressure)} 
              />
              <StatTile 
                label="Shot Quality" 
                homeValue={formatPercent(kpis.homeShotQuality)} 
                awayValue={formatPercent(kpis.awayShotQuality)} 
              />
              <StatTile 
                label="Direct Speed" 
                homeValue={formatPercent(kpis.homeDirectSpeed)} 
                awayValue={formatPercent(kpis.awayDirectSpeed)} 
              />
              <StatTile 
                label="Counter Threat" 
                homeValue={formatPercent(kpis.homeCounterThreat)} 
                awayValue={formatPercent(kpis.awayCounterThreat)} 
              />
            </div>
          </div>
        )}

        {/* Attack Tab */}
        {activeTab === 'attack' && (
          <div className="space-y-4">
            <div className="text-xs text-white/60 text-center mb-2">
              <span className="text-green-400">{homeTeam}</span> vs <span className="text-red-400">{awayTeam}</span>
            </div>
            
            <ComparisonBar 
              homeValue={kpis.homeAttackingTerritory} 
              awayValue={kpis.awayAttackingTerritory} 
              label="Attacking Territory" 
            />
            <ComparisonBar 
              homeValue={kpis.homeShotPressure} 
              awayValue={kpis.awayShotPressure} 
              label="Shot Pressure" 
            />
            <ComparisonBar 
              homeValue={kpis.homeShotQuality} 
              awayValue={kpis.awayShotQuality} 
              label="Shot Quality" 
            />
            <ComparisonBar 
              homeValue={kpis.homeDirectSpeed} 
              awayValue={kpis.awayDirectSpeed} 
              label="Direct Speed" 
            />
            <ComparisonBar 
              homeValue={kpis.homeCounterThreat} 
              awayValue={kpis.awayCounterThreat} 
              label="Counter Threat" 
            />

            {/* Raw Stats */}
            {homeStats && awayStats && (
              <div className="mt-4 pt-4 border-t border-white/10">
                <div className="text-sm font-semibold text-white mb-3">📈 Raw Statistics</div>
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <div className="text-green-400 text-right">{homeStats.totalShots || 0}</div>
                  <div className="text-white/60 text-center">Total Shots</div>
                  <div className="text-red-400">{awayStats.totalShots || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.shotsOnGoal || 0}</div>
                  <div className="text-white/60 text-center">On Target</div>
                  <div className="text-red-400">{awayStats.shotsOnGoal || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.shotsInsideBox || 0}</div>
                  <div className="text-white/60 text-center">Inside Box</div>
                  <div className="text-red-400">{awayStats.shotsInsideBox || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.cornerKicks || 0}</div>
                  <div className="text-white/60 text-center">Corners</div>
                  <div className="text-red-400">{awayStats.cornerKicks || 0}</div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Defense Tab */}
        {activeTab === 'defense' && (
          <div className="space-y-4">
            <div className="text-xs text-white/60 text-center mb-2">
              <span className="text-green-400">{homeTeam}</span> vs <span className="text-red-400">{awayTeam}</span>
            </div>

            <ComparisonBar 
              homeValue={kpis.homeHighPress} 
              awayValue={kpis.awayHighPress} 
              label="High Press Intensity" 
            />
            <ComparisonBar 
              homeValue={kpis.homeFoulIntensity} 
              awayValue={kpis.awayFoulIntensity} 
              label="Foul Intensity" 
              inverse={true}
            />
            <ComparisonBar 
              homeValue={kpis.homeDisciplinaryBalance} 
              awayValue={kpis.awayDisciplinaryBalance} 
              label="Disciplinary Balance" 
              inverse={true}
            />
            <ComparisonBar 
              homeValue={kpis.homeKeeperPressure} 
              awayValue={kpis.awayKeeperPressure} 
              label="Keeper Pressure" 
              inverse={true}
            />
            <ComparisonBar 
              homeValue={kpis.homeKeeperEfficiency} 
              awayValue={kpis.awayKeeperEfficiency} 
              label="Keeper Efficiency" 
            />

            {/* Raw Stats */}
            {homeStats && awayStats && (
              <div className="mt-4 pt-4 border-t border-white/10">
                <div className="text-sm font-semibold text-white mb-3">🛡️ Discipline Stats</div>
                <div className="grid grid-cols-3 gap-2 text-xs">
                  <div className="text-green-400 text-right">{homeStats.fouls || 0}</div>
                  <div className="text-white/60 text-center">Fouls</div>
                  <div className="text-red-400">{awayStats.fouls || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.yellowCards || 0}</div>
                  <div className="text-white/60 text-center">Yellow Cards</div>
                  <div className="text-red-400">{awayStats.yellowCards || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.redCards || 0}</div>
                  <div className="text-white/60 text-center">Red Cards</div>
                  <div className="text-red-400">{awayStats.redCards || 0}</div>
                  
                  <div className="text-green-400 text-right">{homeStats.goalkeeperSaves || 0}</div>
                  <div className="text-white/60 text-center">GK Saves</div>
                  <div className="text-red-400">{awayStats.goalkeeperSaves || 0}</div>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Timeline Tab */}
        {activeTab === 'timeline' && (
          <div className="space-y-4">
            {/* Momentum Shift */}
            <div className="bg-white/5 rounded-lg p-4">
              <div className="text-sm font-semibold text-white mb-3">📈 Half-by-Half Momentum Shift</div>
              <div className="flex justify-around">
                <div className="text-center">
                  <div className="text-xs text-white/60 mb-1">{homeTeam}</div>
                  <div className={`text-2xl font-bold ${
                    parseFloat(kpis.homeMomentumShift) > 0 ? 'text-green-400' : 
                    parseFloat(kpis.homeMomentumShift) < 0 ? 'text-red-400' : 'text-yellow-400'
                  }`}>
                    {parseFloat(kpis.homeMomentumShift) > 0 ? '↗' : parseFloat(kpis.homeMomentumShift) < 0 ? '↘' : '→'}
                    {formatDecimal(kpis.homeMomentumShift)}
                  </div>
                </div>
                <div className="text-center">
                  <div className="text-xs text-white/60 mb-1">{awayTeam}</div>
                  <div className={`text-2xl font-bold ${
                    parseFloat(kpis.awayMomentumShift) > 0 ? 'text-green-400' : 
                    parseFloat(kpis.awayMomentumShift) < 0 ? 'text-red-400' : 'text-yellow-400'
                  }`}>
                    {parseFloat(kpis.awayMomentumShift) > 0 ? '↗' : parseFloat(kpis.awayMomentumShift) < 0 ? '↘' : '→'}
                    {formatDecimal(kpis.awayMomentumShift)}
                  </div>
                </div>
              </div>
            </div>

            {/* Possession Timeline Chart */}
            {kpis.possessionTimeline && kpis.possessionTimeline.length > 0 && (
              <div className="bg-white/5 rounded-lg p-4">
                <div className="text-sm font-semibold text-white mb-3">⏱️ Possession Over Time</div>
                <div className="h-32 flex items-end gap-1">
                  {kpis.possessionTimeline.slice(-20).map((point, idx) => (
                    <div key={idx} className="flex-1 flex flex-col gap-0.5">
                      <div 
                        className="bg-green-500/60 rounded-t"
                        style={{ height: `${parseFloat(point.homeValue) * 1.2}px` }}
                        title={`${point.minute}' - Home: ${parseFloat(point.homeValue).toFixed(0)}%`}
                      />
                      <div 
                        className="bg-red-500/60 rounded-b"
                        style={{ height: `${parseFloat(point.awayValue) * 1.2}px` }}
                        title={`${point.minute}' - Away: ${parseFloat(point.awayValue).toFixed(0)}%`}
                      />
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-xs text-white/40 mt-2">
                  <span>Earlier</span>
                  <span>Now</span>
                </div>
              </div>
            )}

            {/* Shot Pressure Timeline */}
            {kpis.shotPressureTimeline && kpis.shotPressureTimeline.length > 0 && (
              <div className="bg-white/5 rounded-lg p-4">
                <div className="text-sm font-semibold text-white mb-3">🎯 Shot Pressure Trend</div>
                <div className="h-24 flex items-end gap-1">
                  {kpis.shotPressureTimeline.slice(-20).map((point, idx) => (
                    <div key={idx} className="flex-1 flex flex-col justify-end h-full">
                      <div className="relative h-full flex flex-col justify-end">
                        <div 
                          className="bg-green-500/80 rounded-t absolute bottom-0 left-0 right-0"
                          style={{ height: `${parseFloat(point.homeValue) * 100}%` }}
                        />
                        <div 
                          className="bg-red-500/40 rounded-t absolute bottom-0 left-0 right-0"
                          style={{ height: `${parseFloat(point.awayValue) * 100}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
                <div className="flex justify-between text-xs text-white/40 mt-2">
                  <span>Earlier</span>
                  <span>Now</span>
                </div>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  )
}

export default KPIPanel
