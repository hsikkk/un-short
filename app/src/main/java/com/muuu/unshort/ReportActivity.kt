package com.muuu.unshort

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.ui.report.BarChartView
import com.muuu.unshort.ui.report.ReportViewModel

/**
 * 일일 리포트 화면
 *
 * 최근 7일간의 쇼츠 사용 통계를 보여줌:
 * - Daily Trend: 일별 추이 그래프 (진입/시청/시청 시간)
 * - Summary: 오늘의 요약 통계
 * - Detail: 앱별 상세 통계
 */
class ReportActivity : BaseActivity() {

    private val viewModel: ReportViewModel by viewModels()

    private lateinit var chartView: BarChartView

    // Tab views
    private lateinit var tabAttempts: TextView
    private lateinit var tabWatched: TextView
    private lateinit var tabWatchTime: TextView

    // Summary views
    private lateinit var summaryAttemptValue: TextView
    private lateinit var summaryWatchedValue: TextView
    private lateinit var summaryTimeValue: TextView

    // Detail views
    private lateinit var appListContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        initViews()
        setupTabs()
        observeData()
    }

    private fun initViews() {
        // Back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Chart
        chartView = findViewById(R.id.chartView)

        // Tabs
        tabAttempts = findViewById(R.id.tabAttempts)
        tabWatched = findViewById(R.id.tabWatched)
        tabWatchTime = findViewById(R.id.tabWatchTime)

        // Summary
        summaryAttemptValue = findViewById(R.id.summaryAttemptValue)
        summaryWatchedValue = findViewById(R.id.summaryWatchedValue)
        summaryTimeValue = findViewById(R.id.summaryTimeValue)

        // Detail
        appListContainer = findViewById(R.id.appListContainer)
    }

    private fun setupTabs() {
        tabAttempts.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.ATTEMPTS)
        }

        tabWatched.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.WATCHED)
        }

        tabWatchTime.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.WATCH_TIME)
        }
    }

    private fun observeData() {
        viewModel.dailyStats.observe(this) { stats ->
            if (stats.isNotEmpty()) {
                val metricType = viewModel.selectedMetric.value ?: BarChartView.MetricType.ATTEMPTS
                chartView.setData(stats, metricType)
            }
        }

        viewModel.todayStats.observe(this) { stats ->
            updateSummary(stats)
        }

        viewModel.appStats.observe(this) { apps ->
            updateAppList(apps)
        }

        viewModel.selectedMetric.observe(this) { metricType ->
            updateTabUI(metricType)

            // Update chart with current data
            val stats = viewModel.dailyStats.value
            if (!stats.isNullOrEmpty()) {
                chartView.setData(stats, metricType)
            }
        }
    }

    private fun updateTabUI(metricType: BarChartView.MetricType) {
        val selectedColor = getColor(R.color.gray_900)
        val unselectedColor = getColor(R.color.gray_600)
        val selectedBg = R.drawable.tab_selected_background
        val unselectedBg = R.drawable.tab_unselected_background

        when (metricType) {
            BarChartView.MetricType.ATTEMPTS -> {
                tabAttempts.setTextColor(selectedColor)
                tabAttempts.setBackgroundResource(selectedBg)
                tabWatched.setTextColor(unselectedColor)
                tabWatched.setBackgroundResource(unselectedBg)
                tabWatchTime.setTextColor(unselectedColor)
                tabWatchTime.setBackgroundResource(unselectedBg)
            }
            BarChartView.MetricType.WATCHED -> {
                tabAttempts.setTextColor(unselectedColor)
                tabAttempts.setBackgroundResource(unselectedBg)
                tabWatched.setTextColor(selectedColor)
                tabWatched.setBackgroundResource(selectedBg)
                tabWatchTime.setTextColor(unselectedColor)
                tabWatchTime.setBackgroundResource(unselectedBg)
            }
            BarChartView.MetricType.WATCH_TIME -> {
                tabAttempts.setTextColor(unselectedColor)
                tabAttempts.setBackgroundResource(unselectedBg)
                tabWatched.setTextColor(unselectedColor)
                tabWatched.setBackgroundResource(unselectedBg)
                tabWatchTime.setTextColor(selectedColor)
                tabWatchTime.setBackgroundResource(selectedBg)
            }
        }
    }

    private fun updateSummary(stats: StatisticsRepository.DailyStats) {
        summaryAttemptValue.text = stats.attemptCount.toString()
        summaryWatchedValue.text = stats.watchedCount.toString()
        summaryTimeValue.text = formatWatchTime(stats.watchTimeMs)
    }

    private fun updateAppList(apps: List<StatisticsRepository.AppStats>) {
        appListContainer.removeAllViews()

        if (apps.isEmpty()) {
            return
        }

        apps.forEach { appStat ->
            val appCard = createAppCard(appStat)
            appListContainer.addView(appCard)
        }
    }

    private fun createAppCard(appStat: StatisticsRepository.AppStats): View {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_app_stat, appListContainer, false)

        val appIcon = cardView.findViewById<ImageView>(R.id.appIcon)
        val appName = cardView.findViewById<TextView>(R.id.appName)
        val attemptCount = cardView.findViewById<TextView>(R.id.attemptCount)
        val watchedCount = cardView.findViewById<TextView>(R.id.watchedCount)
        val watchTime = cardView.findViewById<TextView>(R.id.watchTime)

        // 앱 정보 설정
        val (icon, name) = getAppInfo(appStat.packageName)
        appIcon.setImageDrawable(icon)
        appName.text = name

        // 통계 설정
        attemptCount.text = appStat.attemptCount.toString()
        watchedCount.text = appStat.watchedCount.toString()
        watchTime.text = formatWatchTime(appStat.watchTimeMs)

        return cardView
    }

    private fun getAppInfo(packageName: String): Pair<Drawable?, String> {
        return try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(packageName, 0)
            val icon = packageManager.getApplicationIcon(appInfo)
            val name = packageManager.getApplicationLabel(appInfo).toString()
            Pair(icon, name)
        } catch (e: PackageManager.NameNotFoundException) {
            Pair(null, packageName)
        }
    }

    private fun formatWatchTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> "${hours}${getString(R.string.time_unit_hour)} ${minutes}${getString(R.string.time_unit_minute)}"
            minutes > 0 -> "${minutes}${getString(R.string.time_unit_minute)} ${seconds}${getString(R.string.time_unit_second)}"
            else -> "${seconds}${getString(R.string.time_unit_second)}"
        }
    }
}
