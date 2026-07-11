package com.budgienews.app

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

internal data class HouseAd(
    val id: String,
    val title: String,
    val mediaUrl: String,
    val targetUrl: String,
    val isActive: Boolean,
)

internal object HouseAdRepository {
    private const val TAG = "HouseAdRepository"
    private const val BASE_URL = "https://house.monkey-network.xyz/api/public/ads"
    private const val REGISTERED_PACKAGE_NAME = "com.budgienews.app"

    /**
     * Retrieves the Google Advertising ID (GAID) off the main thread.
     * Respects the user's limit ad tracking preference: returns null if opted out or unavailable.
     */
    suspend fun getAdvertisingId(context: Context): String? = withContext(Dispatchers.IO) {
        runCatching {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (info.isLimitAdTrackingEnabled) {
                null
            } else {
                val id = info.id
                if (id.isNullOrBlank() || id == "00000000-0000-0000-0000-000000000000") null else id
            }
        }.onFailure { error ->
            Log.d(TAG, "GAID retrieval skipped or unavailable: ${error.message}")
        }.getOrNull()
    }

    /**
     * Fetches eligible House Ads from the self-hosted server off the main thread.
     * Never crashes or blocks the UI; returns an empty list on failure or empty response.
     */
    suspend fun fetchHouseAds(context: Context): List<HouseAd> = withContext(Dispatchers.IO) {
        if (BuildConfig.APPLICATION_ID != REGISTERED_PACKAGE_NAME) {
            Log.w(
                TAG,
                "FLAG: BuildConfig.APPLICATION_ID ('${BuildConfig.APPLICATION_ID}') does not equal expected registered package name '$REGISTERED_PACKAGE_NAME' in App Registry."
            )
        }

        runCatching {
            val gaid = getAdvertisingId(context)
            val uriBuilder = Uri.parse(BASE_URL).buildUpon()
                .appendQueryParameter("app_id", BuildConfig.APPLICATION_ID)
            if (!gaid.isNullOrBlank()) {
                uriBuilder.appendQueryParameter("device_id", gaid)
            }

            val url = java.net.URL(uriBuilder.build().toString())
            val connection = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "BudgieNews/${BuildConfig.VERSION_NAME} (Android)")
            }

            try {
                if (connection.responseCode != java.net.HttpURLConnection.HTTP_OK) {
                    return@withContext emptyList<HouseAd>()
                }
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                if (responseText.isBlank()) return@withContext emptyList<HouseAd>()

                val array = JSONArray(responseText)
                val result = mutableListOf<HouseAd>()
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "")
                    val title = obj.optString("title", "")
                    val mediaUrl = obj.optString("media_url", "")
                    val targetUrl = obj.optString("target_url", "")
                    val isActive = obj.optBoolean("is_active", true)

                    if (id.isNotBlank() && mediaUrl.isNotBlank() && targetUrl.isNotBlank() && isActive) {
                        result.add(HouseAd(id, title, mediaUrl, targetUrl, isActive))
                    }
                }
                result
            } finally {
                connection.disconnect()
            }
        }.onFailure { error ->
            Log.d(TAG, "Failed to fetch house ads: ${error.message}")
        }.getOrDefault(emptyList())
    }
}
