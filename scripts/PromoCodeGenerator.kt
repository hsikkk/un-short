package com.muuu.unshort.scripts

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Lifetime Premium 프로모션 코드 생성 유틸리티
 *
 * Android 프로젝트에서 직접 실행 가능
 */
object PromoCodeGenerator {

    // PromoCodeValidator와 동일한 비밀키 사용
    private const val SECRET_KEY = "unshort_lifetime_premium_secret_2024"

    /**
     * ID에 대한 HMAC-SHA256 서명 생성
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
     * 프로모션 코드 생성
     */
    fun generatePromoCode(id: String): String {
        val upperID = id.uppercase()
        val signature = generateSignature(upperID)
        return "UNSHORT-$upperID-$signature"
    }

    /**
     * 랜덤 ID 생성
     */
    private fun generateRandomId(prefix: String, index: Int): String {
        val randomPart = (1..4).map {
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random()
        }.joinToString("")

        return "$prefix${String.format("%03d", index)}$randomPart"
    }

    /**
     * 여러 개의 프로모션 코드 생성
     */
    fun generateCodes(count: Int, prefix: String = "GIFT"): List<PromoCode> {
        require(count in 1..100) { "개수는 1~100 사이여야 합니다" }

        return (1..count).map { i ->
            val id = generateRandomId(prefix, i)
            val code = generatePromoCode(id)
            PromoCode(i, id, code, System.currentTimeMillis())
        }
    }

    /**
     * 코드를 CSV 형식으로 변환
     */
    fun toCsv(codes: List<PromoCode>): String {
        val builder = StringBuilder()
        builder.append("번호,ID,프로모션 코드,생성일시\n")

        codes.forEach { code ->
            val dateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(Date(code.createdAt))
            builder.append("${code.index},${code.id},${code.code},$dateTime\n")
        }

        return builder.toString()
    }

    data class PromoCode(
        val index: Int,
        val id: String,
        val code: String,
        val createdAt: Long
    )
}

/**
 * 콘솔에서 실행하는 메인 함수
 *
 * 사용법:
 *   kotlin PromoCodeGenerator.kt 10 FRIEND
 */
fun main(args: Array<String>) {
    if (args.isEmpty()) {
        println("사용법: kotlin PromoCodeGenerator.kt <개수> [prefix]")
        println("예시: kotlin PromoCodeGenerator.kt 10 FRIEND")
        return
    }

    val count = args[0].toIntOrNull() ?: run {
        println("오류: 개수는 숫자여야 합니다")
        return
    }

    val prefix = if (args.size > 1) args[1] else "GIFT"

    println("=".repeat(60))
    println("Lifetime Premium 프로모션 코드 생성")
    println("=".repeat(60))
    println("개수: $count")
    println("접두사: $prefix")
    println("=".repeat(60))
    println()

    val codes = PromoCodeGenerator.generateCodes(count, prefix)

    codes.forEach { code ->
        println("[${code.index}/$count] ${code.code}")
    }

    println()
    println("=".repeat(60))

    // CSV 파일로 저장
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "promo_codes_$timestamp.csv"

    File(filename).writeText(PromoCodeGenerator.toCsv(codes))

    println("✅ $count개의 프로모션 코드가 생성되었습니다!")
    println("📁 파일 저장됨: $filename")
    println("=".repeat(60))
}
