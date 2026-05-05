package com.travellapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.AttractionType
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.NominatimResult
import com.travellapp.util.NominatimService
import com.travellapp.util.TimeUtils
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAttractionScreen(
    tripId: Int,
    editAttractionId: Int?,
    tripViewModel: TripViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit
) {
    val attractions by tripViewModel.attractions.collectAsState()
    val editAttraction = remember(editAttractionId, attractions) {
        if (editAttractionId != null) attractions.find { it.id == editAttractionId } else null
    }

    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(AttractionType.REGULAR) }
    var durationMinutes by remember { mutableIntStateOf(AttractionType.REGULAR.defaultDurationMinutes) }
    var address by remember { mutableStateOf("") }
    var lat by remember { mutableDoubleStateOf(0.0) }
    var lon by remember { mutableDoubleStateOf(0.0) }
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    // OSM search state
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<NominatimResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchJob by remember { mutableStateOf<Job?>(null) }
    var showResults by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val searchFocus = remember { FocusRequester() }

    LaunchedEffect(editAttraction) {
        if (editAttraction != null) {
            name = editAttraction.name
            searchQuery = editAttraction.name
            selectedType = editAttraction.type
            durationMinutes = editAttraction.estimatedDurationMinutes
            address = editAttraction.address
            lat = editAttraction.latitude
            lon = editAttraction.longitude
            notes = editAttraction.notes
        }
    }

    // Auto-detect large park from name
    LaunchedEffect(name) {
        if (AttractionType.isLargePark(name) && selectedType == AttractionType.REGULAR) {
            selectedType = AttractionType.LARGE_PARK
            durationMinutes = AttractionType.LARGE_PARK.defaultDurationMinutes
        }
    }

    LaunchedEffect(selectedType) {
        durationMinutes = durationMinutes.coerceIn(
            selectedType.minDurationMinutes,
            selectedType.maxDurationMinutes
        )
    }

    fun triggerSearch(query: String) {
        searchJob?.cancel()
        if (query.length < 3) { searchResults = emptyList(); showResults = false; return }
        searchJob = scope.launch {
            delay(400)
            isSearching = true
            searchResults = NominatimService.search(query)
            isSearching = false
            showResults = searchResults.isNotEmpty()
        }
    }

    fun applyResult(result: NominatimResult) {
        name = result.shortName
        searchQuery = result.shortName
        address = result.address.ifBlank { result.displayName.substringAfter(", ") }
        lat = result.lat
        lon = result.lon
        showResults = false
        if (AttractionType.isLargePark(result.shortName)) {
            selectedType = AttractionType.LARGE_PARK
            durationMinutes = AttractionType.LARGE_PARK.defaultDurationMinutes
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editAttractionId != null) "Upravit atrakci" else "Přidat atrakci") },
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
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── OSM Search ──────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Vyhledat na mapě (OpenStreetMap)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { q ->
                            searchQuery = q
                            if (q != name) { name = q; nameError = false }
                            triggerSearch(q)
                        },
                        label = { Text("Hledat místo…") },
                        leadingIcon = {
                            if (isSearching)
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                            else
                                Icon(Icons.Default.Search, null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    searchQuery = ""; name = ""; searchResults = emptyList()
                                    showResults = false; address = ""; lat = 0.0; lon = 0.0
                                }) { Icon(Icons.Default.Clear, null) }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(searchFocus),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(onSearch = { triggerSearch(searchQuery) }),
                        placeholder = { Text("např. Eiffelova věž, Disneyland…") }
                    )

                    // Search results dropdown
                    if (showResults) {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                            ) {
                                items(searchResults) { result ->
                                    ListItem(
                                        headlineContent = {
                                            Text(result.shortName, style = MaterialTheme.typography.bodyMedium)
                                        },
                                        supportingContent = {
                                            Text(
                                                result.address.ifBlank { result.displayName },
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2
                                            )
                                        },
                                        leadingContent = {
                                            Icon(Icons.Default.Place, null,
                                                tint = MaterialTheme.colorScheme.primary)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    HorizontalDivider()
                                    // Invisible click overlay
                                    DisposableEffect(result) { onDispose {} }
                                    // Real click handler via wrapping
                                }
                                // Re-render as clickable items
                            }
                        }
                        // Clickable list outside LazyColumn to avoid nesting issues
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            searchResults.forEach { result ->
                                Surface(
                                    onClick = { applyResult(result) },
                                    shape = RoundedCornerShape(0.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.Place,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .size(20.dp)
                                                .padding(top = 2.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(result.shortName, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                result.address.ifBlank { result.displayName.take(80) },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }

                    if (lat != 0.0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.GpsFixed,
                                null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                "GPS nalezena: %.4f, %.4f".format(lat, lon),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }

            // ── Name (editable, filled from search) ────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Název atrakce *") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Název je povinný") }) else null,
                leadingIcon = { Icon(Icons.Default.Attractions, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // ── Address ─────────────────────────────────────────────────────
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Adresa") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Type selection ───────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Typ atrakce",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    AttractionType.values().forEach { type ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type; durationMinutes = type.defaultDurationMinutes }
                            )
                            Spacer(Modifier.width(4.dp))
                            Column {
                                Text(type.label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "Doporučená délka: ${TimeUtils.formatDuration(type.minDurationMinutes)} – ${TimeUtils.formatDuration(type.maxDurationMinutes)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // ── Duration ────────────────────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Odhadovaná délka návštěvy",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            TimeUtils.formatDuration(durationMinutes),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Slider(
                        value = durationMinutes.toFloat(),
                        onValueChange = { durationMinutes = it.toInt() },
                        valueRange = selectedType.minDurationMinutes.toFloat()..selectedType.maxDurationMinutes.toFloat(),
                        steps = ((selectedType.maxDurationMinutes - selectedType.minDurationMinutes) / 15) - 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(TimeUtils.formatDuration(selectedType.minDurationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(TimeUtils.formatDuration(selectedType.maxDurationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Notes ────────────────────────────────────────────────────────
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Poznámky (volitelné)") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // ── Save ─────────────────────────────────────────────────────────
            Button(
                onClick = {
                    if (name.isBlank()) { nameError = true; return@Button }
                    if (editAttractionId != null && editAttraction != null) {
                        tripViewModel.updateAttraction(
                            editAttraction.copy(
                                name = name.trim(),
                                type = selectedType,
                                estimatedDurationMinutes = durationMinutes,
                                address = address.trim(),
                                latitude = lat,
                                longitude = lon,
                                notes = notes.trim()
                            )
                        )
                    } else {
                        tripViewModel.addAttraction(
                            name = name.trim(),
                            type = selectedType,
                            durationMinutes = durationMinutes,
                            address = address.trim(),
                            lat = lat,
                            lon = lon,
                            notes = notes.trim()
                        )
                    }
                    onSaved()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.Save, null)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (editAttractionId != null) "Uložit změny" else "Přidat atrakci",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
