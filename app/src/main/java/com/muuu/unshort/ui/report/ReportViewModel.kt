package com.muuu.unshort.ui.report

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.muuu.unshort.data.statistics.StatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Report 화면 ViewModel
 *
 * 통계 데이터 로딩 및 상태 관리
 */
class ReportViewModel(application: Application) : AndroidViewModel(application) {

    /**
     * 날짜 선택 상태
     */
    data class ReportDateState(
        val selectedDate: Long,           // 선택된 날짜 (00:00:00 기준)
        val displayDate: String,          // 화면 표시용 포맷 "11/30 (토)"
        val canNavigatePrevious: Boolean, // 이전 날짜 이동 가능 여부
        val canNavigateNext: Boolean      // 다음 날짜 이동 가능 여부
    )

    companion object {
        private const val TAG = "ReportViewModel"
        private const val REPORT_DAYS_RANGE = 7
        private const val MAX_LOOKBACK_DAYS = 7
    }

    private val repository = StatisticsRepository(application)

    private val _dateState = MutableLiveData<ReportDateState>()
    val dateState: LiveData<ReportDateState> = _dateState

    private val _dailyStats = MutableLiveData<List<StatisticsRepository.DailyStats>>()
    val dailyStats: LiveData<List<StatisticsRepository.DailyStats>> = _dailyStats

    private val _todayStats = MutableLiveData<StatisticsRepository.DailyStats>()
    val todayStats: LiveData<StatisticsRepository.DailyStats> = _todayStats

    private val _appStats = MutableLiveData<List<StatisticsRepository.AppStats>>()
    val appStats: LiveData<List<StatisticsRepository.AppStats>> = _appStats

    private val _selectedMetric = MutableLiveData<BarChartView.MetricType>(BarChartView.MetricType.ATTEMPTS)
    val selectedMetric: LiveData<BarChartView.MetricType> = _selectedMetric

    init {
        val todayState = createDateState(System.currentTimeMillis())
        _dateState.value = todayState
        loadInitialData(todayState.selectedDate)
    }

    private fun getStartOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    private fun getEndOfDay(timestamp: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    private fun addDays(timestamp: Long, days: Int): Long {
        return Calendar.getInstance().apply {
            timeInMillis = timestamp
            add(Calendar.DAY_OF_YEAR, days)
        }.timeInMillis
    }

    private fun formatDateForDisplay(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SUNDAY -> "일"
            Calendar.MONDAY -> "월"
            Calendar.TUESDAY -> "화"
            Calendar.WEDNESDAY -> "수"
            Calendar.THURSDAY -> "목"
            Calendar.FRIDAY -> "금"
            Calendar.SATURDAY -> "토"
            else -> ""
        }

        return "$month/$day ($dayOfWeek)"
    }

    private fun createDateState(selectedDate: Long): ReportDateState {
        val normalizedDate = getStartOfDay(selectedDate)
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val oldestAllowedDate = addDays(todayStart, -MAX_LOOKBACK_DAYS + 1)

        return ReportDateState(
            selectedDate = normalizedDate,
            displayDate = formatDateForDisplay(normalizedDate),
            canNavigatePrevious = normalizedDate > oldestAllowedDate,
            canNavigateNext = normalizedDate < todayStart
        )
    }

    fun navigateToPreviousDay() {
        val currentDate = _dateState.value?.selectedDate ?: return
        val previousDate = addDays(currentDate, -1)
        selectDate(previousDate)
    }

    fun navigateToNextDay() {
        val currentDate = _dateState.value?.selectedDate ?: return
        val nextDate = addDays(currentDate, 1)
        selectDate(nextDate)
    }

    fun selectDate(timestamp: Long) {
        val newDateState = createDateState(timestamp)

        // 유효성 검증
        val todayStart = getStartOfDay(System.currentTimeMillis())
        val oldestAllowed = addDays(todayStart, -MAX_LOOKBACK_DAYS + 1)

        if (newDateState.selectedDate < oldestAllowed || newDateState.selectedDate > todayStart) {
            Log.w(TAG, "Invalid date selection: ${newDateState.displayDate}")
            return
        }

        _dateState.value = newDateState
        loadDataForDate(newDateState.selectedDate)
    }

    private fun loadInitialData(selectedDate: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading initial data...")

                val dayStart = getStartOfDay(selectedDate)
                val dayEnd = getEndOfDay(selectedDate)

                // 병렬 로딩
                val summary = async(Dispatchers.IO) {
                    repository.getStatsForDate(dayStart, dayEnd)
                }

                // Daily Trend는 한 번만 로드 (오늘 기준 최근 7일)
                val dailyTrend = async(Dispatchers.IO) {
                    repository.getDailyStats(REPORT_DAYS_RANGE)
                }

                val detail = async(Dispatchers.IO) {
                    repository.getAppStatsForDate(dayStart, dayEnd)
                }

                _todayStats.value = summary.await()
                _dailyStats.value = dailyTrend.await()
                _appStats.value = detail.await()

                Log.d(TAG, "Initial data loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data", e)
                e.printStackTrace()
            }
        }
    }

    private fun loadDataForDate(selectedDate: Long) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Loading data for date: ${formatDateForDisplay(selectedDate)}")

                val dayStart = getStartOfDay(selectedDate)
                val dayEnd = getEndOfDay(selectedDate)

                // 병렬 로딩 (Summary와 Detail만)
                val summary = async(Dispatchers.IO) {
                    repository.getStatsForDate(dayStart, dayEnd)
                }

                val detail = async(Dispatchers.IO) {
                    repository.getAppStatsForDate(dayStart, dayEnd)
                }

                _todayStats.value = summary.await()
                _appStats.value = detail.await()

                Log.d(TAG, "Data loaded successfully for ${formatDateForDisplay(selectedDate)}")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading data for date", e)
                e.printStackTrace()
            }
        }
    }

    fun selectMetric(metricType: BarChartView.MetricType) {
        _selectedMetric.value = metricType
    }

    fun refreshData() {
        val currentDate = _dateState.value?.selectedDate ?: System.currentTimeMillis()
        loadInitialData(currentDate)
    }
}
