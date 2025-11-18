package com.muuu.unshort.promo

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Lifetime Premium 프로모션 코드 검증
 *
 * 로컬에서 HMAC-SHA256으로 서명을 검증하여 서버 없이 코드 유효성을 확인합니다.
 *
 * 코드 형식: UNSHORT-{ID}-{SIGNATURE}
 * 예시: UNSHORT-GIFT2024-A3F9C8D2
 */
object PromoCodeValidator {

    // 프로모션 코드 서명을 위한 비밀키
    // TODO: BuildConfig로 이동하여 난독화 강화
    private const val SECRET_KEY = "unshort_lifetime_premium_secret_2024"

    // 코드 형식 검증 정규식
    private val CODE_PATTERN = Regex("^UNSHORT-([A-Z0-9]+)-([A-F0-9]{8})$")

    /**
     * 프로모션 코드 검증 결과
     */
    sealed class ValidationResult {
        object Valid : ValidationResult()
        object InvalidFormat : ValidationResult()
        object InvalidSignature : ValidationResult()
        data class Error(val message: String) : ValidationResult()
    }

    /**
     * 프로모션 코드 유효성 검증
     *
     * @param code 사용자가 입력한 프로모션 코드
     * @return ValidationResult
     */
    fun validate(code: String): ValidationResult {
        try {
            // 1. 공백 제거 및 대문자 변환
            val trimmedCode = code.trim().uppercase()

            // 2. 형식 검증
            val matchResult = CODE_PATTERN.matchEntire(trimmedCode)
                ?: return ValidationResult.InvalidFormat

            val (id, providedSignature) = matchResult.destructured

            // 3. 서명 검증
            val expectedSignature = generateSignature(id)

            if (expectedSignature.equals(providedSignature, ignoreCase = true)) {
                return ValidationResult.Valid
            } else {
                return ValidationResult.InvalidSignature
            }

        } catch (e: Exception) {
            return ValidationResult.Error(e.message ?: "Unknown error")
        }
    }

    /**
     * ID에 대한 HMAC-SHA256 서명 생성
     *
     * @param id 프로모션 코드 ID (예: GIFT2024)
     * @return 8자리 HEX 서명 (예: A3F9C8D2)
     */
    private fun generateSignature(id: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)

        val hash = mac.doFinal(id.toByteArray())

        // SHA256 결과의 앞 4바이트를 사용하여 8자리 HEX로 변환
        return hash.take(4).joinToString("") {
            String.format("%02X", it)
        }
    }

    /**
     * 개발자용: 새 프로모션 코드 생성
     *
     * @param id 코드 ID (예: GIFT2024, BETA001 등)
     * @return 완성된 프로모션 코드 (예: UNSHORT-GIFT2024-A3F9C8D2)
     */
    fun generateCode(id: String): String {
        val upperID = id.uppercase()
        val signature = generateSignature(upperID)
        return "UNSHORT-$upperID-$signature"
    }
}
