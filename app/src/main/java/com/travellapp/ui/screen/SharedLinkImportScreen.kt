package com.travellapp.ui.screen

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.AttractionType
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.GoogleMapsListParser
import com.travellapp.util.ParsedMapPlace
import com.travellapp.util.TakeoutPlace
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedLinkImportScreen(
    tripId: Int,
    tripViewModel: TripViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var url by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var places by remember { mutableStateOf<List<ParsedMapPlace>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sdílený odkaz z Google Maps") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
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
                        Text(
                            "Vybráno: ${selected.size} z ${places.size} míst",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    selected = if (selected.size == places.size)
                                        emptySet() else places.indices.toSet()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (selected.size == places.size) "Zrušit vše" else "Vybrat vše")
                            }
                            Button(
                                onClick = {
                                    val toImport = places
                                        .filterIndexed { i, _ -> i in selected }
                                        .map { p ->
                                            TakeoutPlace(p.name, p.address, p.lat, p.lon)
                                        }
                                    tripViewModel.importAttractions(toImport)
                                    onDone()
                                },
                                enabled = selected.isNotEmpty(),
                                modifier = Modifier.weight(2f)
                            ) {
                                Icon(Icons.Default.Download, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Přidat vybraná místa")
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
            // How-to card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp))
                        Text("Jak získat odkaz na seznam?",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary)
                    }
                    Text(
                        "V Google Maps → Uložené → váš seznam → ⋮ (tři tečky) → Sdílet seznam → Kopírovat odkaz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // URL input
            OutlinedTextField(
                value = url,
                onValueChange = { url = it; errorMessage = null; places = emptyList() },
                label = { Text("Odkaz na seznam Google Maps") },
                placeholder = { Text("https://maps.app.goo.gl/…") },
                leadingIcon = { Icon(Icons.Default.Link, null) },
                trailingIcon = {
                    Row {
                        // Paste from clipboard
                        if (url.isBlank()) {
                            IconButton(onClick = {
                                val clip = clipboard.getText()?.text ?: ""
                                if (clip.contains("maps") || clip.contains("goo.gl")) {
                                    url = clip
                                }
                            }) {
                                Icon(Icons.Default.ContentPaste, contentDescription = "Vložit")
                            }
                        } else {
                            IconButton(onClick = { url = ""; places = emptyList(); errorMessage = null }) {
                                Icon(Icons.Default.Clear, contentDescription = "Smazat")
                            }
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    if (url.isNotBlank() && !isLoading) {
                        scope.launch {
                            isLoading = true
                            errorMessage = null
                            places = emptyList()
                            val parsed = GoogleMapsListParser.parse(context, url.trim())
                            if (parsed.isEmpty()) {
                                errorMessage = "Nepodařilo se načíst místa. Zkontrolujte prosím odkaz a zkuste znovu."
                            } else {
                                places = parsed
                                selected = parsed.indices.toSet()
                            }
                            isLoading = false
                        }
                    }
                })
            )

            // Load button
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        errorMessage = null
                        places = emptyList()
                        val parsed = GoogleMapsListParser.parse(context, url.trim())
                        if (parsed.isEmpty()) {
                            errorMessage = "Nepodařilo se načíst místa ze zadaného odkazu.\n\nZkontrolujte, zda:\n• Je odkaz správně zkopírovaný\n• Je seznam veřejně sdílený\n• Máte připojení k internetu"
                        } else {
                            places = parsed
                            selected = parsed.indices.toSet()
                        }
                        isLoading = false
                    }
                },
                enabled = url.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Načítám seznam…")
                } else {
                    Icon(Icons.Default.CloudDownload, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Načíst seznam")
                }
            }

            // Error message
            errorMessage?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline, null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            err,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Results list
            if (places.isNotEmpty()) {
                Text(
                    "Nalezeno ${places.size} míst – vyberte která chcete přidat:",
                    style = MaterialTheme.typography.bodyMedium
                )
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(places.indices.toList()) { idx ->
                        val place = places[idx]
                        val isChecked = idx in selected
                        Card(
                            onClick = {
                                selected = if (isChecked) selected - idx else selected + idx
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isChecked)
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surface
                            ),
                            elevation = CardDefaults.cardElevation(1.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(checked = isChecked, onCheckedChange = {
                                    selected = if (isChecked) selected - idx else selected + idx
                                })
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        place.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (place.address.isNotBlank()) {
                                        Text(
                                            place.address,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    if (place.lat != 0.0) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.GpsFixed, null,
                                                modifier = Modifier.size(10.dp),
                                                tint = MaterialTheme.colorScheme.tertiary
                                            )
                                            Text(
                                                "GPS: %.4f, %.4f".format(place.lat, place.lon),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Empty state when not loading and no URL
            if (!isLoading && url.isBlank() && places.isEmpty() && errorMessage == null) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Share, null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Text(
                            "Vložte odkaz na sdílený seznam\nz Google Maps",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
