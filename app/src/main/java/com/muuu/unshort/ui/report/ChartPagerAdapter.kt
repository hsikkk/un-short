package com.muuu.unshort.ui.report

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muuu.unshort.data.statistics.StatisticsRepository

/**
 * ViewPager2 어댑터 - 스와이프로 차트 메트릭 타입 전환
 */
class ChartPagerAdapter(
    private val context: Context
) : RecyclerView.Adapter<ChartPagerAdapter.ChartViewHolder>() {

    private var dailyStats: List<StatisticsRepository.DailyStats> = emptyList()
    private var selectedDate: Long = 0L

    override fun getItemCount(): Int = 3  // 진입, 시청, 시청시간

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChartViewHolder {
        val chartView = BarChartView(context)
        chartView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return ChartViewHolder(chartView)
    }

    override fun onBindViewHolder(holder: ChartViewHolder, position: Int) {
        val metricType = when (position) {
            0 -> BarChartView.MetricType.ATTEMPTS
            1 -> BarChartView.MetricType.WATCHED
            2 -> BarChartView.MetricType.WATCH_TIME
            else -> BarChartView.MetricType.ATTEMPTS
        }
        holder.chartView.setData(dailyStats, metricType, selectedDate)
    }

    fun updateData(stats: List<StatisticsRepository.DailyStats>, selectedDate: Long) {
        this.dailyStats = stats
        this.selectedDate = selectedDate
        notifyDataSetChanged()
    }

    class ChartViewHolder(val chartView: BarChartView) : RecyclerView.ViewHolder(chartView)
}
