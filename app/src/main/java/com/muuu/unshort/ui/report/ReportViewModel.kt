package com.muuu.unshort.ui.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.muuu.unshort.data.statistics.StatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Report 화면 ViewModel
 *
 * 통계 데이터 로딩 및 상태 관리
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "ReportViewModel"
    }

    private val repository = StatisticsRepository(application)

    private val _dailyStats = MutableLiveData<List<StatisticsRepository.DailyStats>>()
    val dailyStats: LiveData<List<StatisticsRepository.DailyStats>> = _dailyStats

    private val _todayStats = MutableLiveData<StatisticsRepository.DailyStats>()
    val todayStats: LiveData<StatisticsRepository.DailyStats> = _todayStats

    private val _appStats = MutableLiveData<List<StatisticsRepository.AppStats>>()
    val appStats: LiveData<List<StatisticsRepository.AppStats>> = _appStats

    private val _selectedMetric = MutableLiveData<BarChartView.MetricType>(BarChartView.MetricType.ATTEMPTS)
    val selectedMetric: LiveData<BarChartView.MetricType> = _selectedMetric

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading report data...")

                val daily = withContext(Dispatchers.IO) {
                    repository.getDailyStats(7)
                }
                val today = withContext(Dispatchers.IO) {
                    repository.getTodayStats()
                }
                val apps = withContext(Dispatchers.IO) {
                    repository.getTodayAppStats()
                }

                Log.d(TAG, "Daily stats loaded: ${daily.size} days")
                daily.forEachIndexed { index, stat ->
                    Log.d(TAG, "Day $index: attempts=${stat.attemptCount}, watched=${stat.watchedCount}, time=${stat.watchTimeMs}ms")
                }
                Log.d(TAG, "Today stats: attempts=${today.attemptCount}, watched=${today.watchedCount}, time=${today.watchTimeMs}ms")
                Log.d(TAG, "App stats loaded: ${apps.size} apps")

                _dailyStats.value = daily
                _todayStats.value = today
                _appStats.value = apps
            } catch (e: Exception) {
                // Handle error
                Log.e(TAG, "Error loading data", e)
                e.printStackTrace()
            }
        }
    }

    fun selectMetric(metricType: BarChartView.MetricType) {
        _selectedMetric.value = metricType
    }

    fun refreshData() {
        loadData()
    }
}
