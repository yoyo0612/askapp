package com.example.healthapp.data

import com.example.healthapp.nutrition.Nutrients
import com.example.healthapp.nutrition.kilojouleToKcal
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/** バーコードで取得した商品情報。栄養素は100gあたり。 */
data class ProductInfo(
    val name: String,
    val per100g: Nutrients,
    /** エネルギー・たんぱく質・脂質・炭水化物が揃っているか。 */
    val complete: Boolean,
)

/**
 * Open Food Facts（公開の食品データベース）から商品情報を取得する。
 * 見つからない場合は null、通信に失敗した場合は IOException を投げる。
 */
class OpenFoodFactsClient {

    suspend fun fetch(barcode: String): ProductInfo? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://world.openfoodfacts.org/api/v2/product/$barcode.json" +
                "?fields=product_name,product_name_ja,nutriments",
        )
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.setRequestProperty("User-Agent", USER_AGENT)
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parse(barcode, body)
        } catch (e: JSONException) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(barcode: String, body: String): ProductInfo? {
        val json = JSONObject(body)
        // status 1 が「商品が見つかった」の意味
        if (json.optInt("status") != 1) return null
        val product = json.optJSONObject("product") ?: return null
        val nutriments = product.optJSONObject("nutriments") ?: JSONObject()

        val name = product.optString("product_name_ja").ifBlank {
            product.optString("product_name")
        }.ifBlank { "商品 $barcode" }

        // kcal が無い商品は kJ から換算する
        val kcal = nutriments.optDoubleOrNull("energy-kcal_100g")
            ?: nutriments.optDoubleOrNull("energy_100g")?.let { kilojouleToKcal(it) }
        val protein = nutriments.optDoubleOrNull("proteins_100g")
        val fat = nutriments.optDoubleOrNull("fat_100g")
        val carbs = nutriments.optDoubleOrNull("carbohydrates_100g")
        val fiber = nutriments.optDoubleOrNull("fiber_100g")
        // ナトリウムは g で返るので mg に直す
        val sodiumMg = nutriments.optDoubleOrNull("sodium_100g")?.let { it * 1000 }

        return ProductInfo(
            name = name,
            per100g = Nutrients(
                kcal = kcal ?: 0.0,
                proteinG = protein ?: 0.0,
                fatG = fat ?: 0.0,
                carbsG = carbs ?: 0.0,
                fiberG = fiber ?: 0.0,
                sodiumMg = sodiumMg ?: 0.0,
            ),
            complete = kcal != null && protein != null && fat != null && carbs != null,
        )
    }

    private fun JSONObject.optDoubleOrNull(key: String): Double? {
        if (!has(key) || isNull(key)) return null
        return optDouble(key).takeIf { !it.isNaN() }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000
        const val USER_AGENT = "HealthApp/0.1 (Android)"
    }
}
