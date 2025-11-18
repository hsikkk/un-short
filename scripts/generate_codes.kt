import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

// PromoCodeValidator와 동일한 비밀키
const val SECRET_KEY = "unshort_lifetime_premium_secret_2024"

fun generateSignature(id: String): String {
    val mac = Mac.getInstance("HmacSHA256")
    val secretKeySpec = SecretKeySpec(SECRET_KEY.toByteArray(), "HmacSHA256")
    mac.init(secretKeySpec)
    val hash = mac.doFinal(id.toByteArray())
    return hash.take(4).joinToString("") { String.format("%02X", it) }
}

fun generatePromoCode(id: String): String {
    val upperID = id.uppercase()
    val signature = generateSignature(upperID)
    return "UNSHORT-$upperID-$signature"
}

fun generateRandomId(prefix: String, index: Int): String {
    val randomPart = (1..4).map {
        "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random()
    }.joinToString("")
    return "$prefix${String.format("%03d", index)}$randomPart"
}

fun main() {
    val count = 10
    val prefix = "FRIEND"

    println("=" .repeat(60))
    println("Lifetime Premium 프로모션 코드 생성")
    println("=" .repeat(60))
    println("개수: $count")
    println("접두사: $prefix")
    println("=" .repeat(60))
    println()

    val codes = mutableListOf<Triple<Int, String, String>>()

    for (i in 1..count) {
        val id = generateRandomId(prefix, i)
        val code = generatePromoCode(id)
        codes.add(Triple(i, id, code))
        println("[$i/$count] $code")
    }

    println()
    println("=" .repeat(60))

    // CSV 파일로 저장
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val filename = "/Users/muuu/Development/shortblock/scripts/promo_codes_$timestamp.csv"

    File(filename).bufferedWriter().use { writer ->
        writer.write("번호,ID,프로모션 코드,생성일시\n")
        codes.forEach { (index, id, code) ->
            writer.write("$index,$id,$code,$timestamp\n")
        }
    }

    println("✅ $count개의 프로모션 코드가 생성되었습니다!")
    println("📁 파일 저장됨: $filename")
    println("=" .repeat(60))
}

main()
