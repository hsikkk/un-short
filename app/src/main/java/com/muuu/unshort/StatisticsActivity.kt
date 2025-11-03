package com.muuu.unshort

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.muuu.unshort.data.statistics.StatisticsRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * 통계 화면
 *
 * 쇼츠 시청 통계를 표시
 * - 날짜별 시청 기록
 * - 첫 쇼츠/스크롤 시청 구분
 * - 앱별 통계
 * - 최근 7일 추세
 */
class StatisticsActivity : AppCompatActivity() {

    private lateinit var statisticsRepository: StatisticsRepository
    private var currentDate: Calendar = Calendar.getInstance()

    private lateinit var backButton: View
    private lateinit var datePrevButton: View
    private lateinit var dateNextButton: View
    private lateinit var dateText: TextView
    private lateinit var dateLabelText: TextView

    private lateinit var heroContextText: TextView
    private lateinit var heroValueText: TextView
    private lateinit var heroSubText: TextView

    private lateinit var firstWatchOnlyValue: TextView
    private lateinit var scrollWatchValue: TextView
    private lateinit var youtubeValue: TextView
    private lateinit var instagramValue: TextView
    private lateinit var tiktokValue: TextView
    private lateinit var insightText: TextView

    private lateinit var chartSection: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        statisticsRepository = StatisticsRepository(this)

        initViews()
        setupListeners()
        loadStatistics()
    }

    private fun initViews() {
        backButton = findViewById(R.id.backButton)
        datePrevButton = findViewById(R.id.datePrevButton)
        dateNextButton = findViewById(R.id.dateNextButton)
        dateText = findViewById(R.id.dateText)
        dateLabelText = findViewById(R.id.dateLabelText)

        heroContextText = findViewById(R.id.heroContextText)
        heroValueText = findViewById(R.id.heroValueText)
        heroSubText = findViewById(R.id.heroSubText)

        firstWatchOnlyValue = findViewById(R.id.firstWatchOnlyValue)
        scrollWatchValue = findViewById(R.id.scrollWatchValue)
        youtubeValue = findViewById(R.id.youtubeValue)
        instagramValue = findViewById(R.id.instagramValue)
        tiktokValue = findViewById(R.id.tiktokValue)
        insightText = findViewById(R.id.insightText)

        chartSection = findViewById(R.id.chartSection)
    }

    private fun setupListeners() {
        backButton.setOnClickListener {
            finish()
        }

        datePrevButton.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_MONTH, -1)
            updateDateDisplay()
            loadStatistics()
        }

        dateNextButton.setOnClickListener {
            val today = Calendar.getInstance()
            if (currentDate.before(today)) {
                currentDate.add(Calendar.DAY_OF_MONTH, 1)
                updateDateDisplay()
                loadStatistics()
            }
        }
    }

    private fun updateDateDisplay() {
        val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        dateText.text = dateFormat.format(currentDate.time)

        val today = Calendar.getInstance()
        val isToday = currentDate.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                      currentDate.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

        dateLabelText.text = if (isToday) getString(R.string.statistics_today) else ""
        dateLabelText.visibility = if (isToday) View.VISIBLE else View.GONE

        // Disable next button if current date is today
        dateNextButton.alpha = if (isToday) 0.2f else 0.6f
        dateNextButton.isEnabled = !isToday
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                // Get date range for selected date
                val (startTime, endTime) = getDateRange(currentDate)

                // Load statistics for selected date
                val totalAttempts = statisticsRepository.getSessionCountForDate(startTime, endTime)
                val watchedCount = statisticsRepository.getWatchCountForDate(startTime, endTime)
                val skippedCount = totalAttempts - watchedCount
                val savedMinutes = (skippedCount * 0.5).toInt() // Assuming 30 seconds per shorts

                // Update Hero section
                heroContextText.text = String.format("%d번 시도", totalAttempts)
                heroValueText.text = watchedCount.toString()
                heroSubText.text = String.format(
                    getString(R.string.statistics_saved_message),
                    skippedCount,
                    savedMinutes
                )

                // Load detail section
                val firstWatchOnly = statisticsRepository.getFirstWatchOnlyCountForDate(startTime, endTime)
                val scrollWatch = statisticsRepository.getScrollWatchCountForDate(startTime, endTime)

                firstWatchOnlyValue.text = firstWatchOnly.toString()
                scrollWatchValue.text = scrollWatch.toString()

                // Load app-specific statistics
                val youtubeCount = statisticsRepository.getWatchCountByAppForDate(
                    startTime, endTime, "com.google.android.youtube"
                )
                val instagramCount = statisticsRepository.getWatchCountByAppForDate(
                    startTime, endTime, "com.instagram.android"
                )
                val tiktokCount = statisticsRepository.getWatchCountByAppForDate(
                    startTime, endTime, "com.zhiliaoapp.musically"
                )

                youtubeValue.text = youtubeCount.toString()
                instagramValue.text = instagramCount.toString()
                tiktokValue.text = tiktokCount.toString()

                // Update insight message based on performance
                updateInsightMessage(firstWatchOnly, scrollWatch)

                // Load 7-day chart
                load7DaysChart()

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getDateRange(date: Calendar): Pair<Long, Long> {
        val start = Calendar.getInstance().apply {
            timeInMillis = date.timeInMillis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val end = Calendar.getInstance().apply {
            timeInMillis = start.timeInMillis
            add(Calendar.DAY_OF_MONTH, 1)
        }

        return Pair(start.timeInMillis, end.timeInMillis)
    }

    private fun updateInsightMessage(firstWatchOnly: Int, scrollWatch: Int) {
        val message = when {
            scrollWatch == 0 && firstWatchOnly <= 3 -> "잘하고 있습니다"
            scrollWatch == 0 -> "첫 쇼츠만 보고 있네요"
            scrollWatch <= firstWatchOnly -> "조금씩 개선되고 있어요"
            else -> "스크롤을 줄여보세요"
        }
        insightText.text = message
    }

    private fun load7DaysChart() {
        lifecycleScope.launch {
            try {
                // Clear existing chart items
                chartSection.removeViews(1, chartSection.childCount - 1)

                val dayLabels = arrayOf(
                    getString(R.string.statistics_mon),
                    getString(R.string.statistics_tue),
                    getString(R.string.statistics_wed),
                    getString(R.string.statistics_thu),
                    getString(R.string.statistics_fri),
                    getString(R.string.statistics_sat),
                    getString(R.string.statistics_sun)
                )

                // Load data for last 7 days
                val data = IntArray(7)
                val today = Calendar.getInstance()
                val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                val todayIndex = (todayDayOfWeek + 5) % 7 // Convert to Monday=0 format

                for (i in 0..6) {
                    val date = Calendar.getInstance().apply {
                        // Calculate date: today - (6-i) days, starting from 6 days ago
                        val daysAgo = 6 - ((todayIndex - i + 7) % 7)
                        add(Calendar.DAY_OF_MONTH, -daysAgo)
                    }

                    val (startTime, endTime) = getDateRange(date)
                    data[i] = statisticsRepository.getWatchCountForDate(startTime, endTime)
                }

                val maxValue = data.maxOrNull() ?: 1

                for (i in data.indices) {
                    val chartRow = createChartRow(
                        dayLabels[i],
                        data[i],
                        maxValue,
                        i == todayIndex
                    )
                    chartSection.addView(chartRow)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createChartRow(
        label: String,
        value: Int,
        maxValue: Int,
        isToday: Boolean
    ): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
            }
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Label
        val labelView = TextView(this).apply {
            text = label
            textSize = 11f
            typeface = resources.getFont(R.font.spoqa_sans_neo_light)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = if (isToday) 1.0f else 0.5f
            layoutParams = LinearLayout.LayoutParams(
                resources.getDimensionPixelSize(R.dimen.spacing_large),
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        // Bar container
        val barContainer = View(this).apply {
            val width = ((value.toFloat() / maxValue) * (resources.displayMetrics.widthPixels - 200)).toInt()
            layoutParams = LinearLayout.LayoutParams(
                width,
                resources.getDimensionPixelSize(R.dimen.chart_bar_height)
            ).apply {
                marginStart = resources.getDimensionPixelSize(R.dimen.spacing_medium)
                marginEnd = resources.getDimensionPixelSize(R.dimen.spacing_medium)
            }
            setBackgroundColor(
                if (isToday)
                    ContextCompat.getColor(context, android.R.color.black)
                else
                    ContextCompat.getColor(context, R.color.gray_600)
            )
        }

        // Value
        val valueView = TextView(this).apply {
            text = value.toString()
            textSize = 11f
            typeface = resources.getFont(R.font.spoqa_sans_neo_light)
            setTextColor(ContextCompat.getColor(context, android.R.color.black))
            alpha = if (isToday) 1.0f else 0.5f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        row.addView(labelView)
        row.addView(barContainer)
        row.addView(valueView)

        return row
    }
}
