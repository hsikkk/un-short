#!/usr/bin/env kotlin

/**
 * Lifetime Premium 프로모션 코드 생성 스크립트
 *
 * 사용법:
 *   kotlinc -script generate_promo_codes.kts <개수> [prefix]
 *
 * 예시:
 *   kotlinc -script generate_promo_codes.kts 10
 *   kotlinc -script generate_promo_codes.kts 5 GIFT2024
 *
 * 출력:
 *   - 콘솔에 생성된 코드 출력
 *   - promo_codes_<timestamp>.csv 파일로 저장
 */

import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// PromoCodeValidator와 동일한 비밀키 사용
const val SECRET_KEY = "unshort_lifetime_premium_secret_2024"

/**
 * ID에 대한 HMAC-SHA256 서명 생성
 */
fun generateSignature(id: String): String {
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
fun generateRandomId(prefix: String, index: Int): String {
    val randomPart = (1..4).map {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random()
    }.joinToString("")

    return "$prefix${String.format("%03d", index)}$randomPart"
}

// ===== 메인 로직 =====

val args = args

if (args.isEmpty()) {
    println("사용법: kotlinc -script generate_promo_codes.kts <개수> [prefix]")
    println("예시: kotlinc -script generate_promo_codes.kts 10 GIFT2024")
    kotlin.system.exitProcess(1)
}

val count = args[0].toIntOrNull() ?: run {
    println("오류: 개수는 숫자여야 합니다")
    kotlin.system.exitProcess(1)
}

if (count <= 0 || count > 100) {
    println("오류: 개수는 1~100 사이여야 합니다")
    kotlin.system.exitProcess(1)
}

val prefix = if (args.size > 1) args[1] else "GIFT"

// 타임스탬프
val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
val filename = "promo_codes_$timestamp.csv"

println("=" .repeat(60))
println("Lifetime Premium 프로모션 코드 생성")
println("=" .repeat(60))
println("개수: $count")
println("접두사: $prefix")
println("출력 파일: $filename")
println("=" .repeat(60))
println()

// 코드 생성
val codes = mutableListOf<Pair<String, String>>()

for (i in 1..count) {
    val id = generateRandomId(prefix, i)
    val code = generatePromoCode(id)
    codes.add(Pair(id, code))

    println("[$i/$count] $code")
}

println()
println("=" .repeat(60))

// CSV 파일로 저장
File(filename).bufferedWriter().use { writer ->
    writer.write("번호,ID,프로모션 코드,생성일시\n")
    codes.forEachIndexed { index, (id, code) ->
        writer.write("${index + 1},$id,$code,$timestamp\n")
    }
}

println("✅ $count개의 프로모션 코드가 생성되었습니다!")
println("📁 파일 저장됨: $filename")
println("=" .repeat(60))
