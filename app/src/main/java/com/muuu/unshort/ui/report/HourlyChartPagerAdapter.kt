package com.muuu.unshort.ui.report

import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muuu.unshort.data.statistics.StatisticsRepository

/**
 * ViewPager2 어댑터 - 스와이프로 시간별 차트 메트릭 타입 전환
 */
class HourlyChartPagerAdapter(
    private val context: Context
) : RecyclerView.Adapter<HourlyChartPagerAdapter.HourlyChartViewHolder>() {

    private var hourlyStats: List<StatisticsRepository.HourlyStats> = emptyList()
    private var selectedDate: Long = 0L

    override fun getItemCount(): Int = 3  // 진입, 시청, 시청시간

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourlyChartViewHolder {
        val chartView = HourlyBarChartView(context)
        chartView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return HourlyChartViewHolder(chartView)
    }

    override fun onBindViewHolder(holder: HourlyChartViewHolder, position: Int) {
        val metricType = when (position) {
            0 -> HourlyBarChartView.MetricType.ATTEMPTS
            1 -> HourlyBarChartView.MetricType.WATCHED
            2 -> HourlyBarChartView.MetricType.WATCH_TIME
            else -> HourlyBarChartView.MetricType.ATTEMPTS
        }
        holder.chartView.setData(hourlyStats, metricType, selectedDate)
    }

    fun updateData(stats: List<StatisticsRepository.HourlyStats>, selectedDate: Long) {
        this.hourlyStats = stats
        this.selectedDate = selectedDate
        notifyDataSetChanged()
    }

    class HourlyChartViewHolder(val chartView: HourlyBarChartView) : RecyclerView.ViewHolder(chartView)
}
