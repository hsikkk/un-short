package com.muuu.unshort.ui.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.muuu.unshort.R
import com.muuu.unshort.data.statistics.StatisticsRepository
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

/**
 * 일별 통계를 표시하는 커스텀 바 차트 뷰
 */
class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var dailyStats: List<StatisticsRepository.DailyStats> = emptyList()
    private var metricType: MetricType = MetricType.ATTEMPTS

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val todayBarPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val chartPadding = 40f.dpToPx()
    private val chartPaddingTop = 16f.dpToPx()
    private val chartPaddingRight = 8f.dpToPx()
    private val chartPaddingBottom = 32f.dpToPx()
    private val barSpacing = 12f.dpToPx()

    enum class MetricType {
        ATTEMPTS,   // 진입
        WATCHED,    // 시청
        WATCH_TIME  // 시청 시간
    }

    init {
        // 일반 바 색상 (회색)
        barPaint.color = context.getColor(R.color.gray_400)
        barPaint.style = Paint.Style.FILL

        // 오늘 바 색상 (검정)
        todayBarPaint.color = context.getColor(R.color.gray_900)
        todayBarPaint.style = Paint.Style.FILL

        // 텍스트 페인트
        textPaint.color = context.getColor(R.color.gray_600)
        textPaint.textSize = 11f.dpToPx()
        textPaint.textAlign = Paint.Align.CENTER

        // 가이드라인 페인트
        linePaint.color = context.getColor(R.color.gray_900)
        linePaint.strokeWidth = 1f.dpToPx()
        linePaint.alpha = (255 * 0.1).toInt()
    }

    fun setData(stats: List<StatisticsRepository.DailyStats>, type: MetricType = MetricType.ATTEMPTS) {
        this.dailyStats = stats
        this.metricType = type
        invalidate()
    }

    fun setMetricType(type: MetricType) {
        this.metricType = type
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (dailyStats.isEmpty()) return

        val chartWidth = width - chartPadding - chartPaddingRight
        val chartHeight = height - chartPaddingTop - chartPaddingBottom

        // 데이터 값 추출
        val values = dailyStats.map { stat ->
            when (metricType) {
                MetricType.ATTEMPTS -> stat.attemptCount.toFloat()
                MetricType.WATCHED -> stat.watchedCount.toFloat()
                MetricType.WATCH_TIME -> (stat.watchTimeMs / 60000f) // 분 단위
            }
        }

        val maxValue = calculateMaxValue(values.maxOrNull() ?: 0f)

        // Y축 라벨 그리기
        drawYAxisLabels(canvas, maxValue, chartHeight)

        // 가이드라인 그리기
        drawGuideLines(canvas, chartWidth, chartHeight)

        // 바 그리기
        val barWidth = (chartWidth / dailyStats.size) - barSpacing
        dailyStats.forEachIndexed { index, stat ->
            val value = values[index]
            val barHeight = (value / maxValue) * chartHeight

            val x = chartPadding + (index * (barWidth + barSpacing))
            val y = chartPaddingTop + chartHeight - barHeight

            val rect = RectF(x, y, x + barWidth, chartPaddingTop + chartHeight)

            // 마지막 바는 오늘 (검정색), 나머지는 회색
            val paint = if (index == dailyStats.size - 1) todayBarPaint else barPaint
            canvas.drawRoundRect(rect, 4f.dpToPx(), 4f.dpToPx(), paint)
        }

        // X축 라벨 (날짜) 그리기
        drawXAxisLabels(canvas, barWidth)
    }

    /**
     * Y축 최대값 및 간격 계산
     */
    private fun calculateYAxisParams(dataMax: Float): Pair<Float, Int> {
        if (dataMax <= 0f) return Pair(5f, 1)

        // 적절한 간격 선택 (1, 2, 5, 10, 20, 50, 100...)
        val interval = when {
            dataMax <= 5 -> 1
            dataMax <= 10 -> 2
            dataMax <= 25 -> 5
            dataMax <= 50 -> 10
            dataMax <= 100 -> 20
            dataMax <= 250 -> 50
            else -> 100
        }

        // 간격에 맞춰 올림
        val maxValue = kotlin.math.ceil(dataMax / interval) * interval
        val finalMaxValue = max(maxValue, 5f)

        return Pair(finalMaxValue, interval)
    }

    private fun calculateMaxValue(dataMax: Float): Float {
        return calculateYAxisParams(dataMax).first
    }

    private fun drawYAxisLabels(canvas: Canvas, maxValue: Float, chartHeight: Float) {
        val dataMax = dailyStats.map { stat ->
            when (metricType) {
                MetricType.ATTEMPTS -> stat.attemptCount.toFloat()
                MetricType.WATCHED -> stat.watchedCount.toFloat()
                MetricType.WATCH_TIME -> (stat.watchTimeMs / 60000f)
            }
        }.maxOrNull() ?: 0f

        val (_, interval) = calculateYAxisParams(dataMax)
        val labelCount = (maxValue / interval).toInt()

        val labelPaint = Paint(textPaint).apply {
            textAlign = Paint.Align.RIGHT
        }

        for (i in 0..labelCount) {
            val value = i * interval
            val labelText = when (metricType) {
                MetricType.WATCH_TIME -> "${value}m"
                else -> value.toString()
            }

            val y = chartPaddingTop + chartHeight - ((value / maxValue) * chartHeight)
            canvas.drawText(labelText, chartPadding - 8f.dpToPx(), y + 4f.dpToPx(), labelPaint)
        }
    }

    private fun drawGuideLines(canvas: Canvas, chartWidth: Float, chartHeight: Float) {
        val dataMax = dailyStats.map { stat ->
            when (metricType) {
                MetricType.ATTEMPTS -> stat.attemptCount.toFloat()
                MetricType.WATCHED -> stat.watchedCount.toFloat()
                MetricType.WATCH_TIME -> (stat.watchTimeMs / 60000f)
            }
        }.maxOrNull() ?: 0f

        val (maxValue, interval) = calculateYAxisParams(dataMax)
        val lineCount = (maxValue / interval).toInt()

        for (i in 0..lineCount) {
            val value = i * interval
            val y = chartPaddingTop + chartHeight - ((value / maxValue) * chartHeight)
            canvas.drawLine(chartPadding, y, chartPadding + chartWidth, y, linePaint)
        }
    }

    private fun drawXAxisLabels(canvas: Canvas, barWidth: Float) {
        val dateFormat = SimpleDateFormat("M/d", Locale.getDefault())
        val todayLabel = context.getString(R.string.report_today_label)

        dailyStats.forEachIndexed { index, stat ->
            val x = chartPadding + (index * (barWidth + barSpacing)) + (barWidth / 2)
            val y = height - 8f.dpToPx()

            val label = if (index == dailyStats.size - 1) {
                todayLabel
            } else {
                dateFormat.format(Date(stat.date))
            }

            canvas.drawText(label, x, y, textPaint)
        }
    }

    private fun Float.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }
}
