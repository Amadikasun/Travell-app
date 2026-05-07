package com.travellapp.util

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
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

object GoogleMapsListParser {

    // JS runs INSIDE the IIFE and calls Android.onResult() directly – no scope issue
    private val EXTRACTOR_JS = """
        (function() {
            try {
                var places = [];
                var seen = {};

                // Strategy 1: rendered place cards (multiple selector variants)
                var selectors = [
                    '.Nv2PK', '.hfpxzc', '[data-index]',
                    '[jsaction*="placeCard"]', '[data-result-index]',
                    'li[class]', '.m6QErb [role="article"]'
                ];
                selectors.forEach(function(sel) {
                    try {
                        document.querySelectorAll(sel).forEach(function(el) {
                            var name = '';
                            var nameEl = el.querySelector(
                                '.fontHeadlineSmall, [class*="fontHeadline"], h3, ' +
                                '.NrDZNb, .qBF1Pd, [class*="title"]'
                            );
                            if (nameEl) name = nameEl.textContent.trim();
                            if (!name) name = el.getAttribute('aria-label') || '';
                            name = name.trim();
                            if (name.length < 2 || seen[name]) return;
                            seen[name] = true;

                            var addr = '';
                            var addrEl = el.querySelector(
                                '.fontBodyMedium, [class*="fontBody"], ' +
                                '.W4Efsd, .UaQhfb, [class*="address"]'
                            );
                            if (addrEl) addr = addrEl.textContent.trim();

                            places.push({ name: name, address: addr, lat: 0, lon: 0 });
                        });
                    } catch(e2) {}
                });

                // Strategy 2: scan all text nodes for place-like entries
                if (places.length === 0) {
                    document.querySelectorAll('h2, h3, [role="heading"]').forEach(function(el) {
                        var name = el.textContent.trim();
                        if (name.length > 2 && name.length < 100 && !seen[name]) {
                            seen[name] = true;
                            places.push({ name: name, address: '', lat: 0, lon: 0 });
                        }
                    });
                }

                // Strategy 3: extract coords from embedded JSON blobs
                var coordRe = /(-?\d{1,3}\.\d{5,}),\s*(-?\d{1,3}\.\d{5,})/g;
                var allText = document.documentElement.innerHTML;
                var cm, coords = [];
                while ((cm = coordRe.exec(allText)) !== null) {
                    var la = parseFloat(cm[1]), lo = parseFloat(cm[2]);
                    if (la >= -90 && la <= 90 && lo >= -180 && lo <= 180) {
                        coords.push([la, lo]);
                    }
                    if (coords.length > 200) break;
                }
                // Assign unique coords to places without one
                var ci = 0;
                var usedCoords = {};
                for (var i = 0; i < places.length; i++) {
                    while (ci < coords.length) {
                        var key = coords[ci][0].toFixed(4) + ',' + coords[ci][1].toFixed(4);
                        if (!usedCoords[key]) { usedCoords[key] = true; break; }
                        ci++;
                    }
                    if (ci < coords.length) {
                        places[i].lat = coords[ci][0];
                        places[i].lon = coords[ci][1];
                        ci++;
                    }
                }

                Android.onResult(JSON.stringify({ ok: true, places: places }));
            } catch(e) {
                Android.onResult(JSON.stringify({ ok: false, error: e.toString(), places: [] }));
            }
        })();
    """.trimIndent()

    @SuppressLint("SetJavaScriptEnabled")
    suspend fun parse(context: Context, url: String): List<ParsedMapPlace> {
        // WebView MUST run on Main thread
        return withContext(Dispatchers.Main) {
            withTimeoutOrNull(30_000L) {
                suspendCancellableCoroutine { cont ->
                    val webView = WebView(context)

                    webView.settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadsImagesAutomatically = false   // faster load
                        blockNetworkImage = true           // faster load
                        cacheMode = WebSettings.LOAD_NO_CACHE
                        userAgentString =
                            "Mozilla/5.0 (Linux; Android 13; Pixel 7) " +
                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
                            "Chrome/124.0.0.0 Mobile Safari/537.36"
                    }

                    var done = false

                    webView.addJavascriptInterface(
                        object {
                            @JavascriptInterface
                            fun onResult(json: String) {
                                if (done) return
                                done = true
                                val places = parseJson(json)
                                webView.post { webView.destroy() }
                                if (cont.isActive) cont.resume(places)
                            }
                        },
                        "Android"
                    )

                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            // Wait 5s for JS rendering, then inject extractor
                            view.postDelayed({
                                if (!done) view.evaluateJavascript(EXTRACTOR_JS, null)
                            }, 5000L)
                        }
                    }

                    cont.invokeOnCancellation {
                        done = true
                        webView.post { webView.destroy() }
                    }

                    webView.loadUrl(url)
                }
            } ?: emptyList()
        }
    }

    private fun parseJson(json: String): List<ParsedMapPlace> {
        return try {
            val root = JSONObject(json)
            val arr = root.optJSONArray("places") ?: JSONArray()
            (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name").trim()
                if (name.isBlank()) null
                else ParsedMapPlace(
                    name = name,
                    address = obj.optString("address").trim(),
                    lat = obj.optDouble("lat", 0.0),
                    lon = obj.optDouble("lon", 0.0)
                )
            }.distinctBy { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
