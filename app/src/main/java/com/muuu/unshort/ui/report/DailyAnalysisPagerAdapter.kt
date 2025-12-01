package com.muuu.unshort.ui.report

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.muuu.unshort.R

/**
 * ViewPager2 어댑터 - 스와이프로 날짜별 Daily Analysis 전환
 * 최근 7일간의 날짜별 페이지를 제공
 */
class DailyAnalysisPagerAdapter(
    private val context: Context
) : RecyclerView.Adapter<DailyAnalysisPagerAdapter.DailyPageViewHolder>() {

    val dates = mutableListOf<Long>()  // 최근 7일 날짜 리스트
    var onPageBind: ((Int, View, Long) -> Unit)? = null

    override fun getItemCount(): Int = dates.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DailyPageViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.page_daily_analysis, parent, false)
        return DailyPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: DailyPageViewHolder, position: Int) {
        val date = dates[position]
        onPageBind?.invoke(position, holder.itemView, date)
    }

    fun setDates(dateList: List<Long>) {
        dates.clear()
        dates.addAll(dateList)
        notifyDataSetChanged()
    }

    class DailyPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}
