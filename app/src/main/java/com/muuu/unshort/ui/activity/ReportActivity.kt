package com.muuu.unshort.ui.activity

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.muuu.unshort.analytics.AnalyticsEvent
import com.muuu.unshort.analytics.AnalyticsManager
import com.muuu.unshort.data.statistics.StatisticsRepository
import com.muuu.unshort.prefs.PreferencesManager
import com.muuu.unshort.ui.report.BarChartView
import com.muuu.unshort.ui.report.ChartPagerAdapter
import com.muuu.unshort.ui.report.HourlyBarChartView
import com.muuu.unshort.ui.report.NestedViewPager2
import com.muuu.unshort.ui.report.ReportViewModel
import com.muuu.ad.core.adunit.MuuuBannerAdUnit
import com.muuu.ad.core.model.MuuuBannerSize
import com.muuu.unshort.ad.AdManager
import com.muuu.unshort.ui.report.DailyAnalysisPagerAdapter
import com.muuu.unshort.ui.report.HourlyChartPagerAdapter
import java.util.Calendar
import com.muuu.unshort.ui.dialog.WarningDialog
import com.muuu.unshort.config.AdConfig
import com.muuu.unshort.ui.activity.ReportActivity
import com.muuu.unshort.ui.activity.BaseActivity
import com.muuu.unshort.R

/**
 * 일일 리포트 화면
 *
 * 최근 7일간의 쇼츠 사용 통계를 보여줌:
 * - Daily Trend: 일별 추이 그래프 (진입/시청/시청 시간)
 * - Daily Analysis: 날짜별 ViewPager (Summary + App + Hourly)
 */
class ReportActivity : BaseActivity() {

    private val viewModel: ReportViewModel by viewModels()

    private lateinit var chartViewPager: ViewPager2
    private lateinit var chartPagerAdapter: ChartPagerAdapter

    private lateinit var dailyAnalysisViewPager: ViewPager2
    private lateinit var dailyAnalysisPagerAdapter: DailyAnalysisPagerAdapter

    // Sticky Date Chips
    private lateinit var scrollView: ScrollView
    private lateinit var stickyDateChipsContainer: LinearLayout
    private lateinit var stickyDateChips: LinearLayout

    // Daily Trend Tab views
    private lateinit var tabAttempts: TextView
    private lateinit var tabWatched: TextView
    private lateinit var tabWatchTime: TextView

    // Date chips container
    private lateinit var dateChipsContainer: LinearLayout
    private val dateChipViews = mutableListOf<TextView>()

    // 날짜 리스트 (최근 7일)
    private val dates = mutableListOf<Long>()

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

    override fun onResume() {
        super.onResume()

        // 리포트 화면 조회 추적
        AnalyticsManager.trackEvent(
            this,
            AnalyticsEvent.REPORT_SCREEN_VIEWED
        )
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

        // Chart ViewPager
        chartViewPager = findViewById(R.id.chartViewPager)
        chartPagerAdapter = ChartPagerAdapter(this)
        chartViewPager.adapter = chartPagerAdapter

        // 차트 클릭 리스너 설정 - 클릭한 날짜로 선택 변경
        chartPagerAdapter.onBarClickListener = { clickedDate ->
            viewModel.selectDate(clickedDate)
        }

        // ViewPager 페이지 변경 리스너 - 탭 동기화
        chartViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val metricType = when (position) {
                    0 -> BarChartView.MetricType.ATTEMPTS
                    1 -> BarChartView.MetricType.WATCHED
                    2 -> BarChartView.MetricType.WATCH_TIME
                    else -> BarChartView.MetricType.ATTEMPTS
                }
                viewModel.selectMetric(metricType)
            }
        })

        // Daily Analysis ViewPager
        dailyAnalysisViewPager = findViewById(R.id.dailyAnalysisViewPager)
        dailyAnalysisPagerAdapter = DailyAnalysisPagerAdapter(this)
        dailyAnalysisViewPager.adapter = dailyAnalysisPagerAdapter

        // 날짜 리스트 설정 (최근 7일)
        dates.clear()
        for (i in 6 downTo 0) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            dates.add(calendar.timeInMillis)
        }
        dailyAnalysisPagerAdapter.setDates(dates)

        // ViewPager 페이지 변경 시 ViewModel selectedDate 업데이트 및 높이 조정
        dailyAnalysisViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val selectedDate = dates[position]
                viewModel.selectDate(selectedDate)

                // ViewPager 높이 조정
                updateViewPagerHeight(dailyAnalysisViewPager)
            }
        })

        // 페이지 바인드 시 데이터 업데이트
        dailyAnalysisPagerAdapter.onPageBind = { position, view, date ->
            updatePageData(view, date)

            // 현재 표시 중인 페이지면 높이 재조정
            if (position == dailyAnalysisViewPager.currentItem) {
                dailyAnalysisViewPager.post {
                    updateViewPagerHeight(dailyAnalysisViewPager)
                }
            }
        }

        // Sticky Date Chips
        scrollView = findViewById(R.id.contentScrollView)
        stickyDateChipsContainer = findViewById(R.id.stickyDateChipsContainer)
        stickyDateChips = findViewById(R.id.stickyDateChips)

        // Daily Trend Tabs
        tabAttempts = findViewById(R.id.tabAttempts)
        tabWatched = findViewById(R.id.tabWatched)
        tabWatchTime = findViewById(R.id.tabWatchTime)

        // Date chips
        dateChipsContainer = findViewById(R.id.dateChipsContainer)

        // 초기 페이지 높이 설정
        dailyAnalysisViewPager.post {
            updateViewPagerHeight(dailyAnalysisViewPager)
        }
    }

    private fun setupTabs() {
        // Daily Trend Tabs - ViewPager와 동기화
        tabAttempts.setOnClickListener {
            chartViewPager.setCurrentItem(0, true)
        }

        tabWatched.setOnClickListener {
            chartViewPager.setCurrentItem(1, true)
        }

        tabWatchTime.setOnClickListener {
            chartViewPager.setCurrentItem(2, true)
        }
    }

    private fun createDateChip(text: String, dateMillis: Long, isSelected: Boolean): TextView {
        return TextView(this).apply {
            this.text = text
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

            if (isSelected) {
                setBackgroundResource(R.drawable.date_chip_selected)
                setTextColor(getColor(android.R.color.white))
            } else {
                setBackgroundResource(R.drawable.date_chip_unselected)
                setTextColor(getColor(R.color.gray_600))
            }

            setOnClickListener {
                // ViewPager 페이지 전환
                val pageIndex = dates.indexOfFirst { isSameDay(it, dateMillis) }
                if (pageIndex != -1) {
                    dailyAnalysisViewPager.setCurrentItem(pageIndex, true)
                }
            }
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
            val isChipSelected = (i == 0)  // 마지막 칩(오늘)은 선택 상태로

            // 원본 칩 생성
            val originalChip = createDateChip(dateFormat.format(calendar.time), dateMillis, isChipSelected)
            dateChipViews.add(originalChip)
            dateChipsContainer.addView(originalChip)

            // Sticky 칩 생성 (동일한 스타일)
            val stickyChip = createDateChip(dateFormat.format(calendar.time), dateMillis, isChipSelected)
            stickyDateChips.addView(stickyChip)
        }

        // 스크롤 리스너 설정
        setupScrollListener()

        // 초기 페이지 설정 (오늘)
        dailyAnalysisViewPager.setCurrentItem(6, false)
    }

    private fun setupScrollListener() {
        scrollView.viewTreeObserver.addOnScrollChangedListener {
            // 원본 Date Chips의 화면상 Y 좌표 계산
            val location = IntArray(2)
            dateChipsContainer.getLocationOnScreen(location)
            val chipY = location[1]

            // 헤더의 하단 Y 좌표 계산 (헤더 높이 하드코딩)
            val headerLocation = IntArray(2)
            findViewById<View>(R.id.backButton).getLocationOnScreen(headerLocation)
            val headerBottom = headerLocation[1] + findViewById<View>(R.id.backButton).height + 20  // 헤더 버튼 + 여유

            // 원본 칩이 헤더 아래로 가면 sticky 표시
            if (chipY < headerBottom) {
                if (stickyDateChipsContainer.visibility != View.VISIBLE) {
                    stickyDateChipsContainer.visibility = View.VISIBLE
                }
            } else {
                if (stickyDateChipsContainer.visibility != View.GONE) {
                    stickyDateChipsContainer.visibility = View.GONE
                }
            }
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

            // Daily Trend Chart 업데이트
            val stats = viewModel.dailyStats.value ?: emptyList()
            chartPagerAdapter.updateData(stats, dateState.selectedDate)

            // ViewPager 페이지도 업데이트 (날짜 변경 시)
            val pageIndex = dates.indexOfFirst { isSameDay(it, dateState.selectedDate) }
            if (pageIndex != -1 && dailyAnalysisViewPager.currentItem != pageIndex) {
                dailyAnalysisViewPager.setCurrentItem(pageIndex, true)
            }

            // 현재 페이지의 데이터 강제 업데이트
            dailyAnalysisPagerAdapter.notifyDataSetChanged()
        }

        viewModel.dailyStats.observe(this) { stats ->
            if (stats.isNotEmpty()) {
                val selectedDate = viewModel.dateState.value?.selectedDate ?: 0L
                chartPagerAdapter.updateData(stats, selectedDate)
            }
            // Daily Analysis 페이지들도 업데이트
            dailyAnalysisPagerAdapter.notifyDataSetChanged()
        }

        viewModel.todayStats.observe(this) {
            // 선택된 날짜의 Summary stats가 변경되면 현재 페이지 업데이트
            dailyAnalysisPagerAdapter.notifyDataSetChanged()
        }

        viewModel.hourlyStats.observe(this) {
            // Hourly stats가 변경되면 현재 페이지 업데이트
            dailyAnalysisPagerAdapter.notifyDataSetChanged()
        }

        viewModel.appStats.observe(this) {
            // App stats가 변경되면 현재 페이지 업데이트
            dailyAnalysisPagerAdapter.notifyDataSetChanged()
        }

        viewModel.selectedMetric.observe(this) { metricType ->
            updateTabUI(metricType)

            // ViewPager 위치 동기화 (탭 클릭 시)
            val position = when (metricType) {
                BarChartView.MetricType.ATTEMPTS -> 0
                BarChartView.MetricType.WATCHED -> 1
                BarChartView.MetricType.WATCH_TIME -> 2
            }
            if (chartViewPager.currentItem != position) {
                chartViewPager.setCurrentItem(position, true)
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


    private fun createAppCard(appStat: StatisticsRepository.AppStats): View {
        val cardView = LayoutInflater.from(this).inflate(R.layout.item_app_stat, null, false)

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
        val minuteUnit = getString(R.string.time_unit_minute)
        val hourUnit = getString(R.string.time_unit_hour)

        if (ms == 0L) return "0$minuteUnit"

        val totalMinutes = (ms / 60000).toInt()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60

        return when {
            hours == 0 -> "$minutes$minuteUnit"
            minutes == 0 -> "$hours$hourUnit"
            else -> "$hours$hourUnit $minutes$minuteUnit"
        }
    }

    private fun updateDateChips(selectedDateMillis: Long) {
        val calendar = java.util.Calendar.getInstance()

        // 원본 칩 업데이트
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

        // Sticky 칩 업데이트
        for (i in 0 until stickyDateChips.childCount) {
            val stickyChip = stickyDateChips.getChildAt(i) as TextView
            calendar.timeInMillis = System.currentTimeMillis()
            calendar.add(java.util.Calendar.DAY_OF_YEAR, -(6 - i))
            val chipDateMillis = calendar.timeInMillis

            val isSameDay = isSameDay(chipDateMillis, selectedDateMillis)
            updateChipAppearance(stickyChip, isSameDay)
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = java.util.Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = java.util.Calendar.getInstance().apply { timeInMillis = date2 }

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR)
    }

    private fun updatePageData(view: View, date: Long) {
        // Summary views
        val shortsEntryNumber = view.findViewById<TextView>(R.id.shortsEntryNumber)
        val shortsConsumptionNumber = view.findViewById<TextView>(R.id.shortsConsumptionNumber)
        val watchTimeText = view.findViewById<TextView>(R.id.watchTimeText)

        // App views
        val emptyStateView = view.findViewById<View>(R.id.emptyStateView)
        val appListContainer = view.findViewById<LinearLayout>(R.id.appListContainer)

        // Hourly views
        val hourlyChartViewPager = view.findViewById<NestedViewPager2>(R.id.hourlyChartViewPager)
        val hourlyTabAttempts = view.findViewById<TextView>(R.id.hourlyTabAttempts)
        val hourlyTabWatched = view.findViewById<TextView>(R.id.hourlyTabWatched)
        val hourlyTabWatchTime = view.findViewById<TextView>(R.id.hourlyTabWatchTime)

        // ViewModel에서 현재 선택된 날짜의 데이터를 사용
        // dateState가 변경될 때 이 콜백이 다시 호출되므로 현재 데이터를 직접 사용
        val todayStats = viewModel.todayStats.value
        val appStats = viewModel.appStats.value ?: emptyList()
        val hourlyStats = viewModel.hourlyStats.value ?: emptyList()

        // Summary 업데이트
        if (todayStats != null) {
            shortsEntryNumber.text = todayStats.attemptCount.toString()
            shortsConsumptionNumber.text = todayStats.watchedCount.toString()
            watchTimeText.text = formatWatchTime(todayStats.watchTimeMs)
        } else {
            shortsEntryNumber.text = "0"
            shortsConsumptionNumber.text = "0"
            watchTimeText.text = "0${getString(R.string.time_unit_minute)}"
        }

        // App List 업데이트
        appListContainer.removeAllViews()
        if (appStats.isEmpty()) {
            emptyStateView.visibility = View.VISIBLE
            appListContainer.visibility = View.GONE
        } else {
            emptyStateView.visibility = View.GONE
            appListContainer.visibility = View.VISIBLE

            appStats.forEach { appStat ->
                val appCard = createAppCard(appStat)
                appListContainer.addView(appCard)
            }
        }

        // Hourly Chart ViewPager 설정
        val hourlyChartPagerAdapter = HourlyChartPagerAdapter(this)
        hourlyChartViewPager.viewPager.adapter = hourlyChartPagerAdapter
        hourlyChartPagerAdapter.updateData(hourlyStats, date)

        // Hourly Chart 페이지 변경 리스너
        hourlyChartViewPager.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val metricType = when (position) {
                    0 -> HourlyBarChartView.MetricType.ATTEMPTS
                    1 -> HourlyBarChartView.MetricType.WATCHED
                    2 -> HourlyBarChartView.MetricType.WATCH_TIME
                    else -> HourlyBarChartView.MetricType.ATTEMPTS
                }
                updateHourlyTabUI(hourlyTabAttempts, hourlyTabWatched, hourlyTabWatchTime, metricType)
            }
        })

        // Hourly Tabs 클릭 리스너
        hourlyTabAttempts.setOnClickListener {
            hourlyChartViewPager.viewPager.setCurrentItem(0, true)
        }
        hourlyTabWatched.setOnClickListener {
            hourlyChartViewPager.viewPager.setCurrentItem(1, true)
        }
        hourlyTabWatchTime.setOnClickListener {
            hourlyChartViewPager.viewPager.setCurrentItem(2, true)
        }
    }

    private fun updateHourlyTabUI(
        tabAttempts: TextView,
        tabWatched: TextView,
        tabWatchTime: TextView,
        metricType: HourlyBarChartView.MetricType
    ) {
        val selectedColor = getColor(R.color.gray_900)
        val unselectedColor = getColor(R.color.gray_600)
        val selectedBg = R.drawable.tab_selected_background
        val unselectedBg = R.drawable.tab_unselected_background

        when (metricType) {
            HourlyBarChartView.MetricType.ATTEMPTS -> {
                tabAttempts.setTextColor(selectedColor)
                tabAttempts.setBackgroundResource(selectedBg)
                tabWatched.setTextColor(unselectedColor)
                tabWatched.setBackgroundResource(unselectedBg)
                tabWatchTime.setTextColor(unselectedColor)
                tabWatchTime.setBackgroundResource(unselectedBg)
            }
            HourlyBarChartView.MetricType.WATCHED -> {
                tabAttempts.setTextColor(unselectedColor)
                tabAttempts.setBackgroundResource(unselectedBg)
                tabWatched.setTextColor(selectedColor)
                tabWatched.setBackgroundResource(selectedBg)
                tabWatchTime.setTextColor(unselectedColor)
                tabWatchTime.setBackgroundResource(unselectedBg)
            }
            HourlyBarChartView.MetricType.WATCH_TIME -> {
                tabAttempts.setTextColor(unselectedColor)
                tabAttempts.setBackgroundResource(unselectedBg)
                tabWatched.setTextColor(unselectedColor)
                tabWatched.setBackgroundResource(unselectedBg)
                tabWatchTime.setTextColor(selectedColor)
                tabWatchTime.setBackgroundResource(selectedBg)
            }
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

    /**
     * ViewPager2의 높이를 현재 페이지의 실제 높이에 맞춰 동적으로 조정
     */
    private fun updateViewPagerHeight(viewPager: ViewPager2) {
        viewPager.post {
            // 현재 표시 중인 페이지의 뷰를 찾음
            val view = (viewPager.getChildAt(0) as? androidx.recyclerview.widget.RecyclerView)
                ?.findViewHolderForAdapterPosition(viewPager.currentItem)
                ?.itemView ?: return@post

            // 뷰의 실제 높이 측정
            view.measure(
                View.MeasureSpec.makeMeasureSpec(viewPager.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )

            // ViewPager 높이 업데이트
            val params = viewPager.layoutParams
            if (params.height != view.measuredHeight) {
                params.height = view.measuredHeight
                viewPager.layoutParams = params
            }
        }
    }
}
