package com.travellapp.ui.screen

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.ParsedMapPlace
import com.travellapp.util.TakeoutPlace
import org.json.JSONObject
import java.net.URLDecoder

// Extracts place list from Google Maps saved list page.
// Calls Android.onResult() inside IIFE. Returns debug title so error messages are informative.
private val EXTRACTOR_JS = """
(function() {
    try {
        var places = [];
        var seen = {};

        // Strategy 1: standard place card selectors
        var selectors = [
            '.Nv2PK','.hfpxzc','[data-index]','[jsaction*="placeCard"]',
            '[data-result-index]','.m6QErb [role="article"]',
            '[data-place-id]','[data-item-id]','.VkpGBb'
        ];
        selectors.forEach(function(sel) {
            try {
                document.querySelectorAll(sel).forEach(function(el) {
                    var name = '';
                    var nameEl = el.querySelector(
                        '.fontHeadlineSmall,[class*="fontHeadline"],h3,.NrDZNb,.qBF1Pd,[class*="title"],[class*="name"]');
                    if (nameEl) name = nameEl.textContent.trim();
                    if (!name) name = (el.getAttribute('aria-label') || '').trim();
                    if (!name) name = (el.getAttribute('data-name') || '').trim();
                    if (name.length < 2 || seen[name]) return;
                    seen[name] = true;
                    var addrEl = el.querySelector(
                        '.fontBodyMedium,[class*="fontBody"],.W4Efsd,.UaQhfb,[class*="address"]');
                    places.push({name:name, address:addrEl ? addrEl.textContent.trim() : '', lat:0, lon:0});
                });
            } catch(e2) {}
        });

        // Strategy 2: heading elements fallback
        if (places.length === 0) {
            document.querySelectorAll('h2,h3,h4,[role="heading"]').forEach(function(el) {
                var name = el.textContent.trim();
                if (name.length > 2 && name.length < 100 && !seen[name]) {
                    seen[name] = true;
                    places.push({name:name, address:'', lat:0, lon:0});
                }
            });
        }

        // Strategy 3: parse "name" fields from inline JSON blobs that contain placeId
        if (places.length === 0) {
            try {
                var nameRe = /"name"\s*:\s*"([^"]{2,80})"/g;
                document.querySelectorAll('script:not([src])').forEach(function(s) {
                    var t = s.textContent;
                    if (t.indexOf('placeId') < 0 && t.indexOf('place_id') < 0) return;
                    var m;
                    while ((m = nameRe.exec(t)) !== null && places.length < 60) {
                        var n = m[1];
                        if (!seen[n]) { seen[n] = true; places.push({name:n, address:'', lat:0, lon:0}); }
                    }
                });
            } catch(e3) {}
        }

        // Strategy 4: GPS coordinate extraction and assignment
        var coordRe = /(-?\d{1,3}\.\d{5,}),\s*(-?\d{1,3}\.\d{5,})/g;
        var allText = document.documentElement.innerHTML.substring(0, 500000);
        var cm, coords = [], used = {};
        while ((cm = coordRe.exec(allText)) !== null && coords.length < 300) {
            var la = parseFloat(cm[1]), lo = parseFloat(cm[2]);
            var k = la.toFixed(4)+','+lo.toFixed(4);
            if (la>=-90&&la<=90&&lo>=-180&&lo<=180&&!used[k]) { used[k]=true; coords.push([la,lo]); }
        }
        for (var i = 0, ci = 0; i < places.length; i++) {
            if (ci < coords.length) { places[i].lat=coords[ci][0]; places[i].lon=coords[ci][1]; ci++; }
        }

        Android.onResult(JSON.stringify({ok:true, places:places, title:document.title}));
    } catch(e) {
        Android.onResult(JSON.stringify({ok:false, error:e.toString(), places:[], title:document.title}));
    }
})();
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLinkImportScreen(
    tripId: Int,
    tripViewModel: TripViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val clipboard = LocalClipboardManager.current

    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var places by remember { mutableStateOf<List<ParsedMapPlace>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var activeLoadUrl by remember { mutableStateOf<String?>(null) }

    fun parseResult(json: String): Pair<List<ParsedMapPlace>, String> {
        return try {
            val root = JSONObject(json)
            val title = root.optString("title", "")
            val arr = root.optJSONArray("places") ?: return Pair(emptyList(), title)
            val list = (0 until arr.length()).mapNotNull { i ->
                val obj = arr.getJSONObject(i)
                val name = obj.optString("name").trim()
                if (name.isBlank()) null
                else ParsedMapPlace(name, obj.optString("address").trim(),
                    obj.optDouble("lat", 0.0), obj.optDouble("lon", 0.0))
            }.distinctBy { it.name }
            Pair(list, title)
        } catch (e: Exception) { Pair(emptyList(), "") }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sdileny odkaz z Google Maps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpet")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        bottomBar = {
            AnimatedVisibility(visible = places.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Vybrano: ${selected.size} z ${places.size} mist",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selected = if (selected.size == places.size)
                                        emptySet() else places.indices.toSet()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(if (selected.size == places.size) "Zrusit vse" else "Vybrat vse") }
                            Button(
                                onClick = {
                                    val toImport = places.filterIndexed { i, _ -> i in selected }
                                        .map { TakeoutPlace(it.name, it.address, it.lat, it.lon) }
                                    tripViewModel.importAttractions(toImport)
                                    onDone()
                                },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(2f)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pridat vybrana mista")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text("Jak ziskat odkaz na seznam?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text("V Google Maps -> Ulozene -> vas seznam -> tri tecky -> Sdilet seznam -> Kopirovat odkaz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            OutlinedTextField(
                value = url,
                onValueChange = { url = it; errorMessage = null; places = emptyList(); activeLoadUrl = null },
                label = { Text("Odkaz na seznam Google Maps") },
                placeholder = { Text("https://maps.app.goo.gl/...") },
                leadingIcon = { Icon(Icons.Default.Link, null) },
                trailingIcon = {
                    if (url.isBlank()) {
                        IconButton(onClick = {
                            val clip = clipboard.getText()?.text ?: ""
                            if (clip.contains("maps") || clip.contains("goo.gl")) url = clip
                        }) { Icon(Icons.Default.ContentPaste, contentDescription = "Vlozit") }
                    } else {
                        IconButton(onClick = {
                            url = ""; places = emptyList(); errorMessage = null; activeLoadUrl = null
                        }) { Icon(Icons.Default.Clear, contentDescription = "Smazat") }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (url.isNotBlank() && !isLoading) {
                        isLoading = true; errorMessage = null
                        places = emptyList(); activeLoadUrl = url.trim()
                    }
                })
            )

            Button(
                onClick = {
                    if (!isLoading) {
                        isLoading = true; errorMessage = null
                        places = emptyList(); activeLoadUrl = url.trim()
                    }
                },
                enabled = url.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Nacitam seznam... (az 30 sekund)")
                } else {
                    Icon(Icons.Default.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nacist seznam")
                }
            }

            // WebView attached to Compose hierarchy (1dp invisible) so JS executes properly.
            // Desktop UA avoids "Open in Maps app" interstitials.
            // shouldOverrideUrlLoading catches intent:// deep-links and loads the fallback https URL.
            if (activeLoadUrl != null) {
                AndroidView(
                    modifier = Modifier.size(1.dp),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadsImagesAutomatically = false
                                blockNetworkImage = true
                                userAgentString =
                                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                                    "AppleWebKit/537.36 (KHTML, like Gecko) " +
                                    "Chrome/124.0.0.0 Safari/537.36"
                            }
                            addJavascriptInterface(object {
                                @JavascriptInterface
                                fun onResult(json: String) {
                                    val (parsed, title) = parseResult(json)
                                    post {
                                        activeLoadUrl = null
                                        isLoading = false
                                        if (parsed.isEmpty()) {
                                            val hint = if (title.isNotBlank()) " (stranka: $title)" else ""
                                            errorMessage = "Mista nenalezena$hint. Zkuste jiny odkaz nebo pouzijte Takeout import."
                                        } else {
                                            places = parsed
                                            selected = parsed.indices.toSet()
                                            errorMessage = null
                                        }
                                    }
                                }
                            }, "Android")
                            webViewClient = object : WebViewClient() {
                                // Intercept intent:// deep-links (maps.app.goo.gl redirects to these on Android).
                                // Extract browser_fallback_url and load that instead.
                                override fun shouldOverrideUrlLoading(
                                    view: WebView, request: WebResourceRequest
                                ): Boolean {
                                    val u = request.url.toString()
                                    if (u.startsWith("intent://")) {
                                        try {
                                            val fallbackEncoded = Regex("""S\.browser_fallback_url=([^;]+)""")
                                                .find(u)?.groupValues?.get(1)
                                            if (fallbackEncoded != null) {
                                                view.loadUrl(URLDecoder.decode(fallbackEncoded, "UTF-8"))
                                            } else {
                                                // Fallback: strip intent scheme and use https
                                                val path = u.removePrefix("intent://").substringBefore("#")
                                                view.loadUrl("https://$path")
                                            }
                                        } catch (e: Exception) { /* ignore */ }
                                        return true
                                    }
                                    return false
                                }

                                // Use a counter so only the LAST finished page triggers extraction.
                                // This handles multi-step redirects correctly.
                                private var loadSeq = 0
                                override fun onPageFinished(view: WebView, url: String) {
                                    loadSeq++
                                    val seq = loadSeq
                                    view.postDelayed({
                                        if (loadSeq == seq) {
                                            view.evaluateJavascript(EXTRACTOR_JS, null)
                                        }
                                    }, 12000L)
                                }
                            }
                            loadUrl(activeLoadUrl!!)
                        }
                    }
                )
            }

            errorMessage?.let { err ->
                Card(colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)) {
                    Row(modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp))
                        Text(err, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            if (places.isNotEmpty()) {
                Text("Nalezeno ${places.size} mist – vyberte ktera chcete pridat:",
                    style = MaterialTheme.typography.bodyMedium)
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(places.indices.toList()) { idx ->
                        val place = places[idx]
                        val isChecked = idx in selected
                        Card(
                            onClick = { selected = if (isChecked) selected - idx else selected + idx },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Checkbox(checked = isChecked,
                                    onCheckedChange = { selected = if (isChecked) selected - idx else selected + idx })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(place.name, style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (place.address.isNotBlank()) {
                                        Text(place.address, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    if (place.lat != 0.0) {
                                        Row(verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(Icons.Default.GpsFixed, null,
                                                modifier = Modifier.size(10.dp),
                                                tint = MaterialTheme.colorScheme.tertiary)
                                            Text("GPS: %.4f, %.4f".format(place.lat, place.lon),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isLoading && url.isBlank() && places.isEmpty() && errorMessage == null) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Share, null, modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        Text("Vlozit odkaz na sdileny seznam\nz Google Maps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}
