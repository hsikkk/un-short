package com.muuu.unshort.tasklock

import android.graphics.Bitmap
import org.json.JSONObject
import java.util.concurrent.Future

data class TaskVerificationResult(
    val status: String,
    val message: String
)

object OnDeviceTaskVerifier {
    const val VERIFIED = "verified"
    const val NOT_VERIFIED = "not_verified"
    const val UNCLEAR = "unclear"
    const val UNAVAILABLE = "unavailable"

    suspend fun verify(bitmap: Bitmap, taskTitle: String): TaskVerificationResult {
        return runCatching {
            // Reflection keeps the optional Gemini Nano runtime out of the Kotlin 2.0 compile classpath.
            val generationClass = Class.forName("com.google.mlkit.genai.prompt.Generation")
            val model = generationClass.getMethod("getClient").invoke(generationClass.getField("INSTANCE").get(null))
            val futuresClass = Class.forName("com.google.mlkit.genai.prompt.java.GenerativeModelFutures")
            val futures = futuresClass.getMethod(
                "from",
                Class.forName("com.google.mlkit.genai.prompt.GenerativeModel")
            ).invoke(null, model)
            val featureStatus = (futuresClass.getMethod("checkStatus").invoke(futures) as Future<*>).get() as Int
            if (featureStatus != 3) return TaskVerificationResult(UNAVAILABLE, "model_unavailable")

            val imagePartClass = Class.forName("com.google.mlkit.genai.prompt.ImagePart")
            val textPartClass = Class.forName("com.google.mlkit.genai.prompt.TextPart")
            val imagePart = imagePartClass.getConstructor(Bitmap::class.java).newInstance(bitmap)
            val prompt = """You verify whether a photo is relevant evidence that a user completed a task.
Task: ${taskTitle.take(120)}
Do not identify people, infer sensitive traits, or read unrelated private text.
Return only compact JSON: {"status":"verified|not_verified|unclear","reason":"short reason"}"""
            val textPart = textPartClass.getConstructor(String::class.java).newInstance(prompt)
            val builderClass = Class.forName("com.google.mlkit.genai.prompt.GenerateContentRequest\$Builder")
            val builder = builderClass.getConstructor(imagePartClass, textPartClass).newInstance(imagePart, textPart)
            builderClass.getMethod("setTemperature", java.lang.Float::class.java).invoke(builder, 0.2f)
            builderClass.getMethod("setMaxOutputTokens", Integer::class.java).invoke(builder, 100)
            val request = builderClass.getMethod("build").invoke(builder)
            val response = (futuresClass.getMethod(
                "generateContent",
                Class.forName("com.google.mlkit.genai.prompt.GenerateContentRequest")
            ).invoke(futures, request) as Future<*>).get()
            val candidates = response.javaClass.getMethod("getCandidates").invoke(response) as List<*>
            val raw = candidates.firstOrNull()?.let {
                it.javaClass.getMethod("getText").invoke(it) as? String
            }.orEmpty()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val json = JSONObject(raw)
            val status = json.optString("status", UNCLEAR).takeIf {
                it == VERIFIED || it == NOT_VERIFIED || it == UNCLEAR
            } ?: UNCLEAR
            TaskVerificationResult(status, json.optString("reason", ""))
        }.getOrElse { TaskVerificationResult(UNAVAILABLE, it.javaClass.simpleName) }
    }
}
