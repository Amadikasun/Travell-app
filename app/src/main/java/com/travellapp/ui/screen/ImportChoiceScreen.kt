package com.travellapp.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.AttractionType
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.TakeoutParser
import com.travellapp.util.TakeoutPlace

// ── Entry point: user picks how they want to add attractions ──────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportChoiceScreen(
    tripId: Int,
    tripViewModel: TripViewModel,
    onOpenSearch: () -> Unit,          // → AddAttractionScreen (OSM search)
    onOpenSharedLink: () -> Unit,      // → SharedLinkImportScreen
    onImportDone: () -> Unit,          // → back to AttractionsScreen
    onBack: () -> Unit
) {
    var showTakeoutFlow by remember { mutableStateOf(false) }

    if (showTakeoutFlow) {
        TakeoutImportScreen(
            tripId = tripId,
            tripViewModel = tripViewModel,
            onDone = onImportDone,
            onBack = { showTakeoutFlow = false }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Přidat atrakce") },
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
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Jak chcete přidat místa?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))

            // Option A – Sdílený odkaz (nejpohodlnější)
            ChoiceCard(
                icon = Icons.Default.Share,
                iconTint = MaterialTheme.colorScheme.primary,
                title = "Vložit sdílený odkaz ze Google Maps",
                description = "Sdílejte seznam z Google Maps a vložte odkaz sem. Aplikace seznam sama načte – žádný export, žádné soubory.",
                badge = "Nejjednodušší",
                onClick = onOpenSharedLink
            )

            // Option B – Google Takeout JSON
            ChoiceCard(
                icon = Icons.Default.FileDownload,
                iconTint = MaterialTheme.colorScheme.secondary,
                title = "Importovat ze souboru (Google Takeout)",
                description = "Jednorázový import JSON souboru z Google Takeout. Stáhnete archiv jednou a aplikace načte všechna uložená místa.",
                badge = null,
                onClick = { showTakeoutFlow = true }
            )

            // Option C – OSM search
            ChoiceCard(
                icon = Icons.Default.Search,
                iconTint = MaterialTheme.colorScheme.tertiary,
                title = "Vyhledat místo (OpenStreetMap)",
                description = "Vyhledejte jakékoliv místo na světě přímo v aplikaci. Název, adresa a GPS se doplní automaticky.",
                badge = null,
                onClick = onOpenSearch
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    badge: String?,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconTint.copy(alpha = 0.12f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(28.dp))
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(title, style = MaterialTheme.typography.titleSmall)
                    if (badge != null) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Takeout import flow ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TakeoutImportScreen(
    tripId: Int,
    tripViewModel: TripViewModel,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var places by remember { mutableStateOf<List<TakeoutPlace>>(emptyList()) }
    var selected by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        parseError = null
        runCatching {
            val stream = context.contentResolver.openInputStream(uri)
                ?: throw Exception("Soubor nelze otevřít")
            val parsed = TakeoutParser.parse(stream)
            if (parsed.isEmpty()) throw Exception("Nenašla jsem žádná místa. Je to správný soubor ze Google Takeout?")
            places = parsed
            selected = parsed.indices.toSet()
        }.onFailure {
            parseError = it.message
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import z Google Maps") },
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
            if (places.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    importing = true
                                    val toImport = places.filterIndexed { i, _ -> i in selected }
                                    tripViewModel.importAttractions(toImport)
                                    onDone()
                                },
                                enabled = selected.isNotEmpty() && !importing,
                                modifier = Modifier.weight(2f)
                            ) {
                                if (importing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(Icons.Default.FileDownload, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Importovat vybrané")
                                }
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
        ) {
            if (places.isEmpty()) {
                // File picker state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )

                    Text(
                        "Vyberte soubor ze Google Takeout",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )

                    // Step-by-step guide
                    StepCard(step = "1", text = "Otevřete takeout.google.com")
                    StepCard(step = "2", text = "Vyberte pouze Google Maps → Uložená místa")
                    StepCard(step = "3", text = "Stáhněte archiv a rozbalte ho")
                    StepCard(step = "4", text = "Najděte soubor \"Saved Places.json\" nebo \"Uložená místa.json\"")

                    if (parseError != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Text(
                                parseError!!,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Button(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Vybrat soubor JSON")
                    }
                }
            } else {
                // Places list with checkboxes
                LazyColumn(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 12.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            "Nalezeno ${places.size} míst – zaškrtněte, která chcete přidat:",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    items(places.indices.toList()) { idx ->
                        val place = places[idx]
                        val isSelected = idx in selected
                        PlaceCheckRow(
                            place = place,
                            checked = isSelected,
                            onToggle = {
                                selected = if (isSelected) selected - idx else selected + idx
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StepCard(step: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(28.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(step, style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlaceCheckRow(
    place: TakeoutPlace,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (checked)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Checkbox(checked = checked, onCheckedChange = { onToggle() })
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
                    Text(
                        "GPS: %.4f, %.4f".format(place.lat, place.lon),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
