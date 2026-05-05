package com.travellapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume

data class ParsedMapPlace(
    val name: String,
    val address: String,
    val lat: Double,
    val lon: Double
)

/**
 * Loads a shared Google Maps list URL in a hidden WebView,
 * waits for JavaScript to render the page, then extracts
 * place names and coordinates from the DOM / embedded JSON.
 */
object GoogleMapsListParser {

    // JavaScript injected after the page finishes loading.
    // It scans the rendered DOM for place cards and extracts
    // whatever structured data Google embeds in the page.
    private val EXTRACTOR_JS = """
        (function() {
            var results = [];

            // Strategy 1: Look for AF_initDataCallback JSON blobs
            var scripts = document.querySelectorAll('script');
            for (var i = 0; i < scripts.length; i++) {
                var src = scripts[i].textContent || '';
                if (src.indexOf('AF_initDataCallback') === -1) continue;
                try {
                    // Extract the data array from AF_initDataCallback({key:'...',data:[...]})
                    var match = src.match(/AF_initDataCallback\(\{[^}]*data:([\s\S]*?)\}\s*\);/);
                    if (match) {
                        var raw = match[1].trim();
                        if (raw.endsWith(',')) raw = raw.slice(0, -1);
                        results.push({source:'AF', raw: raw.substring(0, 50000)});
                    }
                } catch(e) {}
            }

            // Strategy 2: Look for window.APP_INITIALIZATION_STATE
            try {
                var init = window.APP_INITIALIZATION_STATE;
                if (init) results.push({source:'INIT', raw: JSON.stringify(init).substring(0, 50000)});
            } catch(e) {}

            // Strategy 3: Extract from rendered DOM – place card titles and addresses
            var places = [];
            // Google Maps renders list items with aria-label on the container
            var cards = document.querySelectorAll('[data-index], [jsaction*="placeCard"], .hfpxzc, [data-result-index]');
            cards.forEach(function(el) {
                var nameEl = el.querySelector('[class*="fontHeadlineSmall"], h3, [aria-label]');
                var addrEl = el.querySelector('[class*="fontBodyMedium"], [class*="address"]');
                var name = (nameEl && nameEl.innerText) ? nameEl.innerText.trim() : (el.getAttribute('aria-label') || '');
                var addr = (addrEl && addrEl.innerText) ? addrEl.innerText.trim() : '';
                if (name && name.length > 1) {
                    places.push({name: name, address: addr, lat: 0, lon: 0});
                }
            });

            // Strategy 4: meta og:title fallback (single place pages)
            if (places.length === 0) {
                var og = document.querySelector('meta[property="og:title"]');
                if (og) places.push({name: og.content, address: '', lat: 0, lon: 0});
            }

            return JSON.stringify({scripts: results, places: places});
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun parse(context: Context, url: String): List<ParsedMapPlace> {
        return withTimeoutOrNull(20_000) {
            suspendCancellableCoroutine { cont ->
                val webView = WebView(context)
                webView.settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                        "AppleWebKit/537.36 (KHTML, like Gecko) " +
                        "Chrome/120.0.0.0 Mobile Safari/537.36"
                    cacheMode = WebSettings.LOAD_NO_CACHE
                }

                var resumed = false

                webView.addJavascriptInterface(object {
                    @JavascriptInterface
                    fun onResult(json: String) {
                        if (resumed) return
                        resumed = true
                        val places = extractPlaces(json)
                        cont.resume(places)
                        webView.post { webView.destroy() }
                    }
                }, "Android")

                webView.webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        // Give JS a moment to finish rendering, then inject extractor
                        view.postDelayed({
                            view.evaluateJavascript(
                                "$EXTRACTOR_JS\nAndroid.onResult(result);"
                                    .replace("return JSON.stringify", "var result = JSON.stringify")
                                    .replace("Android.onResult(result);",
                                        "\nAndroid.onResult(result);"),
                                null
                            )
                        }, 3000)
                    }
                }

                cont.invokeOnCancellation { webView.post { webView.destroy() } }
                webView.loadUrl(url)
            }
        } ?: emptyList()
    }

    private fun extractPlaces(json: String): List<ParsedMapPlace> {
        return try {
            val root = JSONObject(json)
            val domPlaces = root.optJSONArray("places") ?: JSONArray()
            val results = mutableListOf<ParsedMapPlace>()

            // DOM-extracted places (may lack coords)
            for (i in 0 until domPlaces.length()) {
                val obj = domPlaces.getJSONObject(i)
                val name = obj.optString("name").trim()
                if (name.isNotBlank()) {
                    results.add(
                        ParsedMapPlace(
                            name = name,
                            address = obj.optString("address"),
                            lat = obj.optDouble("lat", 0.0),
                            lon = obj.optDouble("lon", 0.0)
                        )
                    )
                }
            }

            // Try to enrich with coords from script blobs
            val scripts = root.optJSONArray("scripts") ?: JSONArray()
            for (i in 0 until scripts.length()) {
                val raw = scripts.getJSONObject(i).optString("raw")
                extractCoordsFromBlob(raw, results)
            }

            results.distinctBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Scans a raw JSON blob for patterns like [lat,lon] near place names
     * and tries to match them to already-found places.
     */
    private fun extractCoordsFromBlob(blob: String, places: MutableList<ParsedMapPlace>) {
        // Pattern: two consecutive numbers that look like lat/lon
        val coordRegex = Regex("""(-?\d{1,3}\.\d{4,}),(-?\d{1,3}\.\d{4,})""")
        val matches = coordRegex.findAll(blob).toList()
        if (matches.isEmpty()) return

        val coordPairs = matches.mapNotNull {
            val lat = it.groupValues[1].toDoubleOrNull() ?: return@mapNotNull null
            val lon = it.groupValues[2].toDoubleOrNull() ?: return@mapNotNull null
            if (lat in -90.0..90.0 && lon in -180.0..180.0) lat to lon else null
        }

        // Assign coords to places that don't have them yet (best-effort: order match)
        var coordIdx = 0
        for (i in places.indices) {
            if (places[i].lat == 0.0 && coordIdx < coordPairs.size) {
                val (lat, lon) = coordPairs[coordIdx++]
                places[i] = places[i].copy(lat = lat, lon = lon)
            }
        }
    }
}
