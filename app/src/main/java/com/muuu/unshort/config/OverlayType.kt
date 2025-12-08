package com.muuu.unshort.config
import com.muuu.unshort.config.OverlayType

/**
 * 오버레이 표시 타입
 */
enum class OverlayType {
    /**
     * 첫 진입 시 오버레이
     * - "다시 한번 생각해보기" 버튼 표시
     * - 클릭 시 타이머 액티비티 실행
     */
    INITIAL,

    /**
     * 타이머 완료 후 복귀 시 오버레이
     * - 타이머 건너뛰고 바로 확인 단계
     * - "볼래요" / "안볼래요" 버튼만 표시
     */
    CONFIRMATION
}
