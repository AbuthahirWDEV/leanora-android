package com.leanora.app

import android.graphics.Bitmap
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException

internal sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val message: String) : ApiResult<Nothing>
}

internal data class ApiConfig(val baseUrl: String, val token: String)
internal data class Estimate(val food: Food, val note: String)
internal data class Product(val name: String, val per100: Food, val servingGrams: Int?)
internal data class HttpReply(val code: Int, val body: String)
internal data class ChatTurn(val role: String, val text: String)

internal data class CoachPayload(
    val message: String,
    val language: String,
    val calorieGoal: Int,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
    val fitnessGoal: String,
    val weightKg: Float,
    val targetKg: Float,
    val recentFoods: List<String>,
    val history: List<ChatTurn>,
)

internal object Api {
    fun normalizeBaseUrl(raw: String): String? {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.startsWith("https://") && trimmed.length > 8 && trimmed.none { it.isWhitespace() }) trimmed else null
    }

    private fun call(
        cfg: ApiConfig, path: String, method: String, contentType: String?, body: ByteArray?,
        connectMs: Int, readMs: Int,
    ): HttpReply {
        val base = normalizeBaseUrl(cfg.baseUrl) ?: throw IllegalArgumentException("invalid backend URL")
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = connectMs
            connection.readTimeout = readMs
            if (cfg.token.isNotBlank()) connection.setRequestProperty("X-Leanora-Token", cfg.token.trim())
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", contentType ?: "application/json")
                connection.outputStream.use { it.write(body) }
            }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            return HttpReply(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    internal fun errorDetail(body: String): String =
        runCatching { JSONObject(body).optString("detail", "") }.getOrDefault("").take(180)

    internal fun describeFailure(code: Int, body: String): String = when (code) {
        401 -> "The server rejected the access token. Check Settings → AI backend → Access token."
        413 -> "The image is too large."
        415 -> "That image type isn't supported."
        429 -> "Too many requests. Please wait a minute and try again."
        502, 503, 504 -> "The AI server is waking up or unavailable. Try again in a minute."
        else -> "Server error (HTTP $code)" + errorDetail(body).let { if (it.isBlank()) "" else ": $it" }
    }

    private fun describeException(e: Exception): String = when (e) {
        is IllegalArgumentException -> "Set a valid https:// backend URL in Settings."
        is SocketTimeoutException -> "The server took too long to respond. Free servers can take a minute to wake — try again."
        is UnknownHostException -> "Could not find the server. Check the backend URL and your network."
        else -> "Could not connect: ${e.localizedMessage ?: "check the backend URL and network"}"
    }

    private inline fun <T> guarded(block: () -> ApiResult<T>): ApiResult<T> =
        try {
            block()
        } catch (e: Exception) {
            ApiResult.Err(describeException(e))
        }

    fun checkHealth(cfg: ApiConfig): Pair<Boolean, String> {
        var lastError = "The server did not respond. Check the URL and network."
        repeat(2) { attempt ->
            try {
                val reply = call(cfg.copy(token = ""), "/health", "GET", null, null, 90_000, 90_000)
                if (reply.code in 200..299) {
                    val ok = runCatching { JSONObject(reply.body).optString("status") }.getOrNull() == "ok"
                    return if (ok) true to "Connected • Leanora API is healthy" else false to "Server responded, but /health returned an unexpected response."
                }
                lastError = describeFailure(reply.code, reply.body)
                if (reply.code !in setOf(502, 503, 504)) return false to lastError
            } catch (e: Exception) {
                lastError = describeException(e)
                if (e is IllegalArgumentException || e is UnknownHostException) return false to lastError
            }
            if (attempt == 0) Thread.sleep(2000)
        }
        return false to lastError
    }

    /** Fire-and-forget request that wakes a sleeping free-tier server before the user needs it. */
    fun warmUp(cfg: ApiConfig) {
        runCatching { call(cfg.copy(token = ""), "/health", "GET", null, null, 60_000, 60_000) }
    }

    fun coach(cfg: ApiConfig, p: CoachPayload): ApiResult<String> = guarded {
        val history = JSONArray().also { array -> p.history.forEach { array.put(JSONObject().put("role", it.role).put("text", it.text.take(1500))) } }
        val payload = JSONObject().put("message", p.message).put("language", p.language).put("calorie_goal", p.calorieGoal)
            .put("calories_consumed", p.calories).put("protein_consumed_g", p.proteinG).put("carbs_consumed_g", p.carbsG)
            .put("fat_consumed_g", p.fatG).put("fitness_goal", p.fitnessGoal).put("weight_kg", p.weightKg.toDouble())
            .put("target_weight_kg", p.targetKg.toDouble()).put("recent_foods", JSONArray(p.recentFoods.take(30))).put("history", history)
        val reply = call(cfg, "/v1/coach", "POST", "application/json", payload.toString().toByteArray(Charsets.UTF_8), 30_000, 120_000)
        if (reply.code in 200..299) {
            ApiResult.Ok(JSONObject(reply.body).optString("answer", "").ifBlank { "No answer returned." })
        } else {
            ApiResult.Err(describeFailure(reply.code, reply.body))
        }
    }

    fun estimateText(cfg: ApiConfig, description: String): ApiResult<Estimate> = guarded {
        val payload = JSONObject().put("description", description.take(1000))
        val reply = call(cfg, "/v1/food-text-estimate", "POST", "application/json", payload.toString().toByteArray(Charsets.UTF_8), 30_000, 120_000)
        if (reply.code in 200..299) parseTextEstimate(JSONObject(reply.body), description) else ApiResult.Err(describeFailure(reply.code, reply.body))
    }

    internal fun parseTextEstimate(obj: JSONObject, description: String): ApiResult<Estimate> {
        val kcal = obj.optInt("calories_kcal", 0).coerceAtLeast(0)
        if (kcal <= 0) return ApiResult.Err("Couldn't estimate nutrition for “${description.take(50)}”. Try describing it differently.")
        val name = obj.optString("food_name").ifBlank { description.take(60) }
        val portion = obj.optString("portion").ifBlank { "estimated portion" }
        val food = Food(
            "$name ($portion)", kcal, obj.optInt("protein_g", 0).coerceAtLeast(0), obj.optInt("carbs_g", 0).coerceAtLeast(0),
            obj.optInt("fat_g", 0).coerceAtLeast(0), Nutrition.sanitizeMeal(obj.optString("meal")),
        )
        return ApiResult.Ok(Estimate(food, "Heard: “${description.take(120)}”. AI estimates are approximate — check before saving."))
    }

    fun estimatePhoto(cfg: ApiConfig, bitmap: Bitmap, fallbackMeal: String): ApiResult<Estimate> = guarded {
        val boundary = "LeanoraBoundary" + System.currentTimeMillis()
        val jpeg = ByteArrayOutputStream().also { downscale(bitmap, 1280).compress(Bitmap.CompressFormat.JPEG, 82, it) }.toByteArray()
        val body = ByteArrayOutputStream().also { out ->
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"meal\"\r\n\r\n$fallbackMeal\r\n".toByteArray())
            out.write("--$boundary\r\nContent-Disposition: form-data; name=\"image\"; filename=\"meal.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".toByteArray())
            out.write(jpeg)
            out.write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val reply = call(cfg, "/v1/food-estimate", "POST", "multipart/form-data; boundary=$boundary", body, 30_000, 120_000)
        if (reply.code in 200..299) parsePhotoEstimate(JSONObject(reply.body), fallbackMeal) else ApiResult.Err(describeFailure(reply.code, reply.body))
    }

    internal fun parsePhotoEstimate(obj: JSONObject, meal: String): ApiResult<Estimate> {
        val kcal = obj.optInt("calories_kcal", 0).coerceAtLeast(0)
        if (kcal <= 0) return ApiResult.Err("Couldn't estimate calories from that photo. Try a closer, well-lit shot.")
        val grams = obj.optInt("portion_g", 0)
        val name = obj.optString("food_name").ifBlank { "Estimated meal" } + if (grams > 0) " ($grams g)" else ""
        val note = buildString {
            append("Photo estimate")
            obj.optString("confidence").takeIf { it.isNotBlank() && it != "unknown" }?.let { append(" • confidence: $it") }
            obj.optString("assumptions").takeIf { it.isNotBlank() }?.let { append("\nAssumptions: $it") }
        }
        val food = Food(
            name, kcal, obj.optInt("protein_g", 0).coerceAtLeast(0), obj.optInt("carbs_g", 0).coerceAtLeast(0),
            obj.optInt("fat_g", 0).coerceAtLeast(0), Nutrition.sanitizeMeal(meal),
        )
        return ApiResult.Ok(Estimate(food, note))
    }

    private fun downscale(bitmap: Bitmap, maxSide: Int): Bitmap {
        val longest = maxOf(bitmap.width, bitmap.height)
        if (longest <= maxSide) return bitmap
        val ratio = maxSide.toFloat() / longest
        return Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true)
    }

    fun lookupBarcode(code: String): ApiResult<Product> = guarded {
        val connection = URL("https://world.openfoodfacts.org/api/v2/product/$code.json").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "Leanora Android nutrition tracker")
            val status = connection.responseCode
            if (status == 404) return@guarded ApiResult.Err("That barcode isn't in Open Food Facts. Try manual entry.")
            if (status !in 200..299) return@guarded ApiResult.Err("Barcode lookup failed (HTTP $status). Try again later.")
            parseProduct(JSONObject(connection.inputStream.bufferedReader().use { it.readText() }), code)
        } finally {
            connection.disconnect()
        }
    }

    internal fun parseProduct(json: JSONObject, code: String): ApiResult<Product> {
        val product = json.optJSONObject("product") ?: return ApiResult.Err("That barcode isn't in Open Food Facts. Try manual entry.")
        val n = product.optJSONObject("nutriments") ?: JSONObject()
        val kcal = when {
            n.has("energy-kcal_100g") -> n.optDouble("energy-kcal_100g", 0.0)
            n.has("energy-kcal") -> n.optDouble("energy-kcal", 0.0)
            n.has("energy_100g") -> n.optDouble("energy_100g", 0.0) / 4.184
            else -> 0.0
        }.toInt().coerceAtLeast(0)
        if (kcal <= 0) return ApiResult.Err("No nutrition data for this product. Try manual entry.")
        val name = product.optString("product_name").ifBlank { product.optString("brands") }.ifBlank { "Scanned product $code" }
        val per100 = Food(
            name, kcal, n.optDouble("proteins_100g", 0.0).toInt().coerceAtLeast(0), n.optDouble("carbohydrates_100g", 0.0).toInt().coerceAtLeast(0),
            n.optDouble("fat_100g", 0.0).toInt().coerceAtLeast(0), "Snack",
        )
        val serving = product.optString("serving_quantity").toDoubleOrNull()?.toInt()?.takeIf { it in 1..2000 }
        return ApiResult.Ok(Product(name, per100, serving))
    }
}
