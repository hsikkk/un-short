package com.muuu.unshort.ui.report

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.widget.FrameLayout
import androidx.viewpager2.widget.ViewPager2
import kotlin.math.abs

/**
 * 중첩 ViewPager를 지원하는 커스텀 ViewPager2 래퍼
 *
 * 동작 방식:
 * - 첫 페이지에서 오른쪽 스와이프 → 부모 ViewPager로 전달
 * - 마지막 페이지에서 왼쪽 스와이프 → 부모 ViewPager로 전달
 * - 그 외의 경우 → 자신이 처리
 *
 * 사용 예:
 * - 외부: Daily Analysis ViewPager (날짜별 페이지)
 * - 내부: Hourly Chart ViewPager (메트릭별 차트)
 */
class NestedViewPager2 @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    val viewPager: ViewPager2

    private var initialX = 0f
    private var initialY = 0f
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    init {
        viewPager = ViewPager2(context, attrs)
        addView(viewPager, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = ev.x
                initialY = ev.y
            }
            MotionEvent.ACTION_MOVE -> {
                val dx = abs(ev.x - initialX)
                val dy = abs(ev.y - initialY)

                // 스크롤 의도가 명확해진 경우에만 처리
                if (dx > touchSlop || dy > touchSlop) {
                    // 가로 스크롤 의도가 더 강한 경우
                    if (dx > dy) {
                        val swipeRight = ev.x > initialX
                        val swipeLeft = ev.x < initialX

                        // 첫 페이지에서 오른쪽 스와이프 → 부모로 전달
                        if (viewPager.currentItem == 0 && swipeRight) {
                            parent?.requestDisallowInterceptTouchEvent(false)
                            return false
                        }

                        // 마지막 페이지에서 왼쪽 스와이프 → 부모로 전달
                        val adapter = viewPager.adapter
                        if (adapter != null && viewPager.currentItem == adapter.itemCount - 1 && swipeLeft) {
                            parent?.requestDisallowInterceptTouchEvent(false)
                            return false
                        }

                        // 내부에서 처리 가능한 가로 스크롤 → 부모 터치 차단
                        parent?.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // 세로 스크롤 의도 → 부모에게 전달
                        parent?.requestDisallowInterceptTouchEvent(false)
                        return false
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }

        return super.onInterceptTouchEvent(ev)
    }
}
