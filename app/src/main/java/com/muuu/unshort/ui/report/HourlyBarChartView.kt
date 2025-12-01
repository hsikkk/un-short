package com.muuu.unshort.ui.report

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.muuu.unshort.R
import com.muuu.unshort.data.statistics.StatisticsRepository
import kotlin.math.max

/**
 * 시간대별 통계를 표시하는 커스텀 바 차트 뷰
 */
class HourlyBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class MetricType {
        ATTEMPTS,   // 진입
        WATCHED,    // 시청
        WATCH_TIME  // 시청 시간
    }

    private var hourlyStats: List<StatisticsRepository.HourlyStats> = emptyList()
    private var metricType: MetricType = MetricType.ATTEMPTS
    private var selectedDate: Long = 0L

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val countTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val separatorLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val futureHourBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val chartPaddingLeft = 48f.dpToPx()
    private val chartPaddingTop = 16f.dpToPx()
    private val chartPaddingBottom = 16f.dpToPx()

    init {
        // 바 색상 (accent_purple)
        barPaint.color = context.getColor(R.color.accent_purple)
        barPaint.style = Paint.Style.FILL

        // 일반 텍스트 페인트
        textPaint.color = context.getColor(R.color.gray_600)
        textPaint.textSize = 11f.dpToPx()
        textPaint.textAlign = Paint.Align.CENTER

        // 라벨 페인트 (시간)
        labelPaint.color = context.getColor(R.color.gray_600)
        labelPaint.textSize = 11f.dpToPx()
        labelPaint.textAlign = Paint.Align.CENTER

        // 카운트 텍스트 페인트 (N/M)
        countTextPaint.color = context.getColor(R.color.gray_500)
        countTextPaint.textSize = 10f.dpToPx()
        countTextPaint.textAlign = Paint.Align.CENTER

        // 구분선 페인트
        separatorLinePaint.color = context.getColor(R.color.gray_900)
        separatorLinePaint.strokeWidth = 1f.dpToPx()
        separatorLinePaint.alpha = (255 * 0.1).toInt()

        // 미래 시간대 배경 페인트
        futureHourBgPaint.color = context.getColor(R.color.gray_200)
        futureHourBgPaint.style = Paint.Style.FILL
        futureHourBgPaint.alpha = (255 * 0.7).toInt()  // 70% 투명도
    }

    fun setData(stats: List<StatisticsRepository.HourlyStats>, type: MetricType = MetricType.ATTEMPTS, selectedDate: Long = 0L) {
        hourlyStats = stats
        metricType = type
        this.selectedDate = selectedDate
        invalidate()
    }

    fun setMetricType(type: MetricType) {
        metricType = type
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (hourlyStats.isEmpty()) return

        // 데이터 값 추출
        val values = hourlyStats.map { stat ->
            when (metricType) {
                MetricType.ATTEMPTS -> stat.attemptCount.toFloat()
                MetricType.WATCHED -> stat.watchedCount.toFloat()
                MetricType.WATCH_TIME -> (stat.watchTimeMs / 60000f) // 분 단위
            }
        }

        // 최대값 계산 (ATTEMPTS와 WATCHED는 데이터의 최대값, WATCH_TIME은 60분 고정)
        val maxValue = when (metricType) {
            MetricType.ATTEMPTS, MetricType.WATCHED -> values.maxOrNull() ?: 0f
            MetricType.WATCH_TIME -> 60f // 60분 고정
        }

        // 가장 긴 텍스트의 너비를 계산하여 동적 오른쪽 패딩 설정
        val maxTextWidth = values.maxOfOrNull { value ->
            val valueText = when (metricType) {
                MetricType.WATCH_TIME -> String.format("%.0fm", value)
                else -> value.toInt().toString()
            }
            countTextPaint.measureText(valueText)
        } ?: 0f
        val chartPaddingRight = maxTextWidth + 24f.dpToPx() // 텍스트 너비 + 최소 여유 공간

        val chartWidth = width - chartPaddingLeft - chartPaddingRight
        val chartHeight = height - chartPaddingTop - chartPaddingBottom

        // 막대 그리기 (세로 방향)
        val barCount = hourlyStats.size
        val barHeight = 16f.dpToPx()  // 고정 막대 높이 (12 -> 16)
        val barSpacing = 8f.dpToPx()   // 고정 간격

        val isSelectedDateToday = isToday(selectedDate)
        val currentHour = if (isSelectedDateToday) getCurrentHour() else -1

        hourlyStats.forEachIndexed { index, stat ->
            val y = chartPaddingTop + index * (barHeight + barSpacing)
            val value = values[index]

            // 미래 시간대 여부 판단
            val isFutureHour = isSelectedDateToday && stat.hour > currentHour
            val isFirstFutureHour = isFutureHour && (index == 0 || hourlyStats[index - 1].hour <= currentHour)

            // 미래 시간대 배경 그리기 (전체 행 + 간격 포함)
            if (isFutureHour) {
                val bgStartY = if (isFirstFutureHour) {
                    y - barSpacing / 2  // 첫 미래 시간대는 위쪽 간격의 절반만 포함
                } else {
                    y
                }
                val bgRect = RectF(
                    0f,
                    bgStartY,
                    width.toFloat(),
                    y + barHeight + barSpacing
                )
                canvas.drawRect(bgRect, futureHourBgPaint)
            }

            // 데이터가 있을 때만 막대 그리기 (미래 시간대는 제외)
            if (maxValue > 0f && value > 0f && !isFutureHour) {
                // 60분 초과 시 60분으로 제한 (WATCH_TIME만)
                val displayValue = if (metricType == MetricType.WATCH_TIME && value > 60f) 60f else value

                // 모든 메트릭 타입에서 동일하게 처리
                val barWidth = (displayValue / maxValue) * chartWidth
                val x = chartPaddingLeft

                // 막대 그리기 (왼쪽에서 오른쪽으로)
                val rect = RectF(x, y, x + barWidth, y + barHeight)
                canvas.drawRoundRect(rect, 4f.dpToPx(), 4f.dpToPx(), barPaint)

                // 막대 오른쪽에 값 텍스트 그리기
                val valueText = when (metricType) {
                    MetricType.WATCH_TIME -> String.format("%.0fm", displayValue)
                    else -> displayValue.toInt().toString()
                }
                val valueX = x + barWidth + 8f.dpToPx()
                val valueY = y + barHeight / 2 + 4f.dpToPx()
                canvas.drawText(valueText, valueX, valueY, countTextPaint)
            }

            // Y축 라벨 (시간) - 모든 시간 표시 (0-23시)
            val hourLabel = String.format("%02d", stat.hour)  // 2자리로 포맷 (00, 01, ..., 23)
            val labelX = 8f.dpToPx()
            val labelY = y + barHeight / 2 + 4f.dpToPx()

            // 미래 시간대는 회색/투명하게
            val labelPaintLeft = if (isFutureHour) {
                Paint(labelPaint).apply {
                    textAlign = Paint.Align.LEFT
                    alpha = (255 * 0.3).toInt()  // 30% 투명도
                    color = context.getColor(R.color.gray_400)
                }
            } else {
                Paint(labelPaint).apply {
                    textAlign = Paint.Align.LEFT
                }
            }
            canvas.drawText(hourLabel, labelX, labelY, labelPaintLeft)
        }

        // 구분선 그리기 (라벨과 그래프 사이) - 배경 위에 다시 그리기
        // 마지막 시간대의 끝 위치까지만 그리기
        val lastIndex = hourlyStats.size - 1
        val lastY = chartPaddingTop + lastIndex * (barHeight + barSpacing)
        val separatorEndY = lastY + barHeight + barSpacing

        val separatorX = chartPaddingLeft - 4f.dpToPx()
        canvas.drawLine(
            separatorX,
            chartPaddingTop,
            separatorX,
            separatorEndY,
            separatorLinePaint
        )
    }

    private fun Float.dpToPx(): Float {
        return this * context.resources.displayMetrics.density
    }

    private fun isToday(date: Long): Boolean {
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis

        return date == today
    }

    private fun getCurrentHour(): Int {
        return java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    }
}
