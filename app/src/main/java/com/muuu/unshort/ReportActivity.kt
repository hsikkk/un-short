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
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.ui.report.BarChartView
import com.muuu.unshort.ui.report.HourlyBarChartView
import com.muuu.unshort.ui.report.ReportViewModel
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.unshort.ad.AdManager

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
    private lateinit var hourlyChartView: HourlyBarChartView

    // Daily Trend Tab views
    private lateinit var tabAttempts: TextView
    private lateinit var tabWatched: TextView
    private lateinit var tabWatchTime: TextView

    // Hourly Analysis Tab views
    private lateinit var hourlyTabAttempts: TextView
    private lateinit var hourlyTabWatched: TextView
    private lateinit var hourlyTabWatchTime: TextView

    // Summary views
    private lateinit var shortsEntryNumber: TextView
    private lateinit var shortsConsumptionNumber: TextView
    private lateinit var watchTimeText: TextView

    // Detail views
    private lateinit var emptyStateView: View
    private lateinit var appListContainer: LinearLayout

    // Date chips container
    private lateinit var dateChipsContainer: LinearLayout
    private val dateChipViews = mutableListOf<TextView>()

    // Hourly metric type state
    private var currentHourlyMetric = HourlyBarChartView.MetricType.ATTEMPTS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        // 통계 화면 방문 플래그 저장
        val prefsManager = PreferencesManager(this)
        prefsManager.hasVisitedStatistics = true

        // MREC 광고 설정
        val adViewContainer = findViewById<FrameLayout>(R.id.adViewReport)
         AdManager.setupBannerAd(
            context = this,
            container = adViewContainer,
            adUnit = MuuuBannerAdUnit(
                key = AdConfig.MREC_REPORT,
                placement = "report_top",
                bannerSize = MuuuBannerSize.MREC,
                refreshInterval = 7
            ),
             keepContainerSpace = false
        )

        initViews()
        setupTabs()
        setupDateChips()
        observeData()
    }

    private fun initViews() {
        // Header setup
        findViewById<TextView>(R.id.headerTitle).text = getString(R.string.report_title)

        // Back button
        findViewById<ImageView>(R.id.backButton).setOnClickListener {
            finish()
        }

        // Right button (Help)
        findViewById<ImageView>(R.id.rightButton).apply {
            visibility = android.view.View.VISIBLE
            setImageResource(R.drawable.ic_help)
            contentDescription = getString(R.string.report_help_button)
            setOnClickListener {
                showReportHelpDialog()
            }
        }

        // Chart
        chartView = findViewById(R.id.chartView)
        hourlyChartView = findViewById(R.id.hourlyChartView)

        // Daily Trend Tabs
        tabAttempts = findViewById(R.id.tabAttempts)
        tabWatched = findViewById(R.id.tabWatched)
        tabWatchTime = findViewById(R.id.tabWatchTime)

        // Hourly Analysis Tabs
        hourlyTabAttempts = findViewById(R.id.hourlyTabAttempts)
        hourlyTabWatched = findViewById(R.id.hourlyTabWatched)
        hourlyTabWatchTime = findViewById(R.id.hourlyTabWatchTime)

        // Summary
        shortsEntryNumber = findViewById(R.id.shortsEntryNumber)
        shortsConsumptionNumber = findViewById(R.id.shortsConsumptionNumber)
        watchTimeText = findViewById(R.id.watchTimeText)

        // Detail
        emptyStateView = findViewById(R.id.emptyStateView)
        appListContainer = findViewById(R.id.appListContainer)

        // Date chips
        dateChipsContainer = findViewById(R.id.dateChipsContainer)
    }

    private fun setupTabs() {
        // Daily Trend Tabs
        tabAttempts.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.ATTEMPTS)
        }

        tabWatched.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.WATCHED)
        }

        tabWatchTime.setOnClickListener {
            viewModel.selectMetric(BarChartView.MetricType.WATCH_TIME)
        }

        // Hourly Analysis Tabs
        hourlyTabAttempts.setOnClickListener {
            currentHourlyMetric = HourlyBarChartView.MetricType.ATTEMPTS
            updateHourlyChart()
            updateHourlyTabUI(HourlyBarChartView.MetricType.ATTEMPTS)
        }

        hourlyTabWatched.setOnClickListener {
            currentHourlyMetric = HourlyBarChartView.MetricType.WATCHED
            updateHourlyChart()
            updateHourlyTabUI(HourlyBarChartView.MetricType.WATCHED)
        }

        hourlyTabWatchTime.setOnClickListener {
            currentHourlyMetric = HourlyBarChartView.MetricType.WATCH_TIME
            updateHourlyChart()
            updateHourlyTabUI(HourlyBarChartView.MetricType.WATCH_TIME)
        }
    }

    private fun setupDateChips() {
        val calendar = java.util.Calendar.getInstance()
        val today = calendar.timeInMillis
        val dateFormat = java.text.SimpleDateFormat("M/d", java.util.Locale.getDefault())

        // 최근 7일 날짜 칩 생성 (6일 전부터 오늘까지, 역순)
        for (i in 6 downTo 0) {
            calendar.timeInMillis = today
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -i)
            val dateMillis = calendar.timeInMillis

            val chip = TextView(this).apply {
                text = dateFormat.format(calendar.time)
                textSize = 14f
                gravity = android.view.Gravity.CENTER
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.chip_padding_vertical),
                    resources.getDimensionPixelSize(R.dimen.chip_padding_vertical),
                    resources.getDimensionPixelSize(R.dimen.chip_padding_vertical),
                    resources.getDimensionPixelSize(R.dimen.chip_padding_vertical)
                )

                val params = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
                layoutParams = params

                // 마지막 칩(오늘)은 선택 상태로
                val isChipSelected = (i == 0)
                if (isChipSelected) {
                    setBackgroundResource(R.drawable.date_chip_selected)
                    setTextColor(getColor(android.R.color.white))
                } else {
                    setBackgroundResource(R.drawable.date_chip_unselected)
                    setTextColor(getColor(R.color.gray_600))
                }

                setOnClickListener {
                    viewModel.selectDate(dateMillis)
                }
            }

            dateChipViews.add(chip)
            dateChipsContainer.addView(chip)
        }
    }

    private fun updateChipAppearance(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.date_chip_selected)
            chip.setTextColor(getColor(android.R.color.white))
        } else {
            chip.setBackgroundResource(R.drawable.date_chip_unselected)
            chip.setTextColor(getColor(R.color.gray_600))
        }
    }

    private fun observeData() {
        // 날짜 상태 관찰
        viewModel.dateState.observe(this) { dateState ->
            updateDateChips(dateState.selectedDate)
            // 차트에 선택된 날짜 전달
            chartView.setSelectedDate(dateState.selectedDate)
            // Hourly chart도 날짜 업데이트
            updateHourlyChart()
        }

        viewModel.dailyStats.observe(this) { stats ->
            if (stats.isNotEmpty()) {
                val metricType = viewModel.selectedMetric.value ?: BarChartView.MetricType.ATTEMPTS
                val selectedDate = viewModel.dateState.value?.selectedDate ?: 0L
                chartView.setData(stats, metricType, selectedDate)
            }
        }

        viewModel.todayStats.observe(this) { stats ->
            updateSummary(stats)
        }

        viewModel.hourlyStats.observe(this) { stats ->
            updateHourlyChart()
        }

        viewModel.appStats.observe(this) { apps ->
            updateAppList(apps)
        }

        viewModel.selectedMetric.observe(this) { metricType ->
            updateTabUI(metricType)

            // Update chart with current data
            val stats = viewModel.dailyStats.value
            if (!stats.isNullOrEmpty()) {
                val selectedDate = viewModel.dateState.value?.selectedDate ?: 0L
                chartView.setData(stats, metricType, selectedDate)
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

    private fun updateHourlyTabUI(metricType: HourlyBarChartView.MetricType) {
        val selectedColor = getColor(R.color.gray_900)
        val unselectedColor = getColor(R.color.gray_600)
        val selectedBg = R.drawable.tab_selected_background
        val unselectedBg = R.drawable.tab_unselected_background

        when (metricType) {
            HourlyBarChartView.MetricType.ATTEMPTS -> {
                hourlyTabAttempts.setTextColor(selectedColor)
                hourlyTabAttempts.setBackgroundResource(selectedBg)
                hourlyTabWatched.setTextColor(unselectedColor)
                hourlyTabWatched.setBackgroundResource(unselectedBg)
                hourlyTabWatchTime.setTextColor(unselectedColor)
                hourlyTabWatchTime.setBackgroundResource(unselectedBg)
            }
            HourlyBarChartView.MetricType.WATCHED -> {
                hourlyTabAttempts.setTextColor(unselectedColor)
                hourlyTabAttempts.setBackgroundResource(unselectedBg)
                hourlyTabWatched.setTextColor(selectedColor)
                hourlyTabWatched.setBackgroundResource(selectedBg)
                hourlyTabWatchTime.setTextColor(unselectedColor)
                hourlyTabWatchTime.setBackgroundResource(unselectedBg)
            }
            HourlyBarChartView.MetricType.WATCH_TIME -> {
                hourlyTabAttempts.setTextColor(unselectedColor)
                hourlyTabAttempts.setBackgroundResource(unselectedBg)
                hourlyTabWatched.setTextColor(unselectedColor)
                hourlyTabWatched.setBackgroundResource(unselectedBg)
                hourlyTabWatchTime.setTextColor(selectedColor)
                hourlyTabWatchTime.setBackgroundResource(selectedBg)
            }
        }
    }

    private fun updateSummary(stats: StatisticsRepository.DailyStats) {
        shortsEntryNumber.text = stats.attemptCount.toString()
        shortsConsumptionNumber.text = stats.watchedCount.toString()
        watchTimeText.text = formatWatchTime(stats.watchTimeMs)
    }

    private fun updateAppList(apps: List<StatisticsRepository.AppStats>) {
        appListContainer.removeAllViews()

        if (apps.isEmpty()) {
            // Empty state 표시
            emptyStateView.visibility = View.VISIBLE
            appListContainer.visibility = View.GONE
            return
        }

        // 데이터 있을 때
        emptyStateView.visibility = View.GONE
        appListContainer.visibility = View.VISIBLE

        apps.forEach { appStat ->
            val appCard = createAppCard(appStat)
            appListContainer.addView(appCard)
        }
    }

    private fun createAppCard(appStat: StatisticsRepository.AppStats): View {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_app_stat, appListContainer, false)

        val appIcon = cardView.findViewById<ImageView>(R.id.appIcon)
        val appName = cardView.findViewById<TextView>(R.id.appName)
        val blockRateValue = cardView.findViewById<TextView>(R.id.blockRateValue)
        val attemptCount = cardView.findViewById<TextView>(R.id.attemptCount)
        val watchedCount = cardView.findViewById<TextView>(R.id.watchedCount)
        val watchTime = cardView.findViewById<TextView>(R.id.watchTime)

        // 앱 정보 설정
        val (icon, name) = getAppInfo(appStat.packageName)
        appIcon.setImageDrawable(icon)
        appName.text = name

        // 차단률 계산 및 표시
        val blockRate = calculateBlockRate(appStat)
        blockRateValue.text = "${blockRate}%"
        blockRateValue.setTextColor(getBlockRateColor(blockRate))

        // 통계 설정
        attemptCount.text = appStat.attemptCount.toString()
        watchedCount.text = appStat.watchedCount.toString()
        watchTime.text = formatWatchTime(appStat.watchTimeMs)

        return cardView
    }

    /**
     * 차단률 계산
     *
     * @param appStat 앱 통계
     * @return 차단률 (0-100)
     */
    private fun calculateBlockRate(appStat: StatisticsRepository.AppStats): Int {
        return if (appStat.attemptCount > 0) {
            ((appStat.attemptCount - appStat.watchedCount) * 100.0 / appStat.attemptCount).toInt()
        } else {
            0
        }
    }

    /**
     * 차단률에 따른 색상 반환
     *
     * @param rate 차단률 (0-100)
     * @return 색상 리소스
     */
    private fun getBlockRateColor(rate: Int): Int {
        return when {
            rate >= 80 -> getColor(R.color.success)  // 차단 성공
            rate < 50 -> getColor(R.color.error)     // 주의 필요
            else -> getColor(R.color.gray_900)       // 보통
        }
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

    private fun updateDateChips(selectedDateMillis: Long) {
        val calendar = java.util.Calendar.getInstance()

        dateChipViews.forEachIndexed { index, chip ->
            calendar.timeInMillis = System.currentTimeMillis()
            // index 0 = 6일 전, index 6 = 오늘
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -(6 - index))
            val chipDateMillis = calendar.timeInMillis

            // 같은 날짜인지 비교 (시간 제외)
            val isSameDay = isSameDay(chipDateMillis, selectedDateMillis)
            chip.isSelected = isSameDay
            updateChipAppearance(chip, isSameDay)
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = date2 }

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun updateHourlyChart() {
        val stats = viewModel.hourlyStats.value
        val selectedDate = viewModel.dateState.value?.selectedDate ?: 0L

        if (!stats.isNullOrEmpty()) {
            hourlyChartView.setData(stats, currentHourlyMetric, selectedDate)
        }
    }

    private fun showReportHelpDialog() {
        WarningDialog(
            context = this,
            titleResId = R.string.report_help_title,
            messageResId = R.string.report_help_message,
            positiveTextResId = R.string.report_help_confirm,
            negativeTextResId = null,
            canceledOnTouchOutside = true,
            onPositive = {}
        ).show()
    }
}
