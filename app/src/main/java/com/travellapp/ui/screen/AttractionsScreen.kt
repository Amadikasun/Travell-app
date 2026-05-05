package com.travellapp.ui.screen

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.Attraction
import com.travellapp.data.model.AttractionType
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttractionsScreen(
    tripId: Int,
    tripViewModel: TripViewModel,
    onAddAttraction: () -> Unit,
    onEditAttraction: (Int) -> Unit,
    onGenerateItinerary: () -> Unit,
    onBack: () -> Unit
) {
    LaunchedEffect(tripId) { tripViewModel.selectTrip(tripId) }

    val trip by tripViewModel.currentTrip.collectAsState()
    val attractions by tripViewModel.attractions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Atrakce") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    if (attractions.isNotEmpty()) {
                        IconButton(onClick = onAddAttraction) {
                            Icon(Icons.Default.Add, contentDescription = "Přidat")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        floatingActionButton = {
            if (attractions.isEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAddAttraction,
                    icon = { Icon(Icons.Default.AddLocationAlt, null) },
                    text = { Text("Přidat atrakci") }
                )
            }
        },
        bottomBar = {
            if (attractions.isNotEmpty()) {
                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "${attractions.size} ${attractionLabel(attractions.size)} celkem • " +
                                "${TimeUtils.formatDuration(attractions.sumOf { it.estimatedDurationMinutes })} prohlídek",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onAddAttraction,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Přidat")
                            }
                            Button(
                                onClick = onGenerateItinerary,
                                modifier = Modifier.weight(2f)
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Vygenerovat itinerář")
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (attractions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.AddLocationAlt,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Text(
                        "Zatím žádné atrakce",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Přidejte místa, která chcete navštívit. Aplikace je optimálně rozvrhne do jednotlivých dní.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp,
                    top = padding.calculateTopPadding() + 12.dp,
                    bottom = padding.calculateBottomPadding() + 12.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(attractions, key = { it.id }) { attraction ->
                    AttractionCard(
                        attraction = attraction,
                        onEdit = { onEditAttraction(attraction.id) },
                        onDelete = { tripViewModel.deleteAttraction(attraction.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AttractionCard(
    attraction: Attraction,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Type icon
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (attraction.type == AttractionType.LARGE_PARK)
                    MaterialTheme.colorScheme.secondaryContainer
                else
                    MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (attraction.type == AttractionType.LARGE_PARK)
                            Icons.Default.Attractions else Icons.Default.Place,
                        contentDescription = null,
                        tint = if (attraction.type == AttractionType.LARGE_PARK)
                            MaterialTheme.colorScheme.onSecondaryContainer
                        else
                            MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attraction.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(attraction.type.label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.height(24.dp)
                    )
                    Text(
                        "~${TimeUtils.formatDuration(attraction.estimatedDurationMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (attraction.address.isNotBlank()) {
                    Text(
                        attraction.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            IconButton(onClick = onEdit) {
                Icon(Icons.Default.Edit, contentDescription = "Upravit", modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Smazat",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Smazat atrakci?") },
            text = { Text("Opravdu chcete smazat \"${attraction.name}\"?") },
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete() }) {
                    Text("Smazat", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Zrušit") }
            }
        )
    }
}

private fun attractionLabel(count: Int): String = when {
    count == 1 -> "atrakce"
    count in 2..4 -> "atrakce"
    else -> "atrakcí"
}
