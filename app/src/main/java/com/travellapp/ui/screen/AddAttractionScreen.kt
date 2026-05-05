package com.travellapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.AttractionType
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.TimeUtils

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
    var notes by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    LaunchedEffect(editAttraction) {
        if (editAttraction != null) {
            name = editAttraction.name
            selectedType = editAttraction.type
            durationMinutes = editAttraction.estimatedDurationMinutes
            address = editAttraction.address
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

    // Clamp duration when type changes
    LaunchedEffect(selectedType) {
        durationMinutes = durationMinutes.coerceIn(
            selectedType.minDurationMinutes,
            selectedType.maxDurationMinutes
        )
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
            // Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; nameError = false },
                label = { Text("Název atrakce *") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Název je povinný") }) else null,
                leadingIcon = { Icon(Icons.Default.Attractions, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                placeholder = { Text("např. Eiffelova věž, Disneyland…") }
            )

            // Type selection
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
                                onClick = {
                                    selectedType = type
                                    durationMinutes = type.defaultDurationMinutes
                                }
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

            // Duration slider
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            TimeUtils.formatDuration(selectedType.minDurationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            TimeUtils.formatDuration(selectedType.maxDurationMinutes),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Address
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                label = { Text("Adresa / umístění (volitelné)") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Poznámky (volitelné)") },
                leadingIcon = { Icon(Icons.Default.Notes, null) },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
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
                                notes = notes.trim()
                            )
                        )
                    } else {
                        tripViewModel.addAttraction(
                            name = name.trim(),
                            type = selectedType,
                            durationMinutes = durationMinutes,
                            address = address.trim(),
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
