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
import com.travellapp.data.model.Trip
import com.travellapp.ui.viewmodel.AuthViewModel
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripSetupScreen(
    tripViewModel: TripViewModel,
    authViewModel: AuthViewModel,
    editTripId: Int?,
    onSaved: (Int) -> Unit,
    onBack: () -> Unit
) {
    val userEmail by authViewModel.userEmail.collectAsState()

    var tripName by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var durationDays by remember { mutableIntStateOf(3) }
    var breakfastHour by remember { mutableIntStateOf(7) }
    var breakfastMinute by remember { mutableIntStateOf(0) }
    var departureHour by remember { mutableIntStateOf(8) }
    var departureMinute by remember { mutableIntStateOf(0) }
    var endOfDayHour by remember { mutableIntStateOf(22) }
    var hotelAddress by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf(false) }

    val currentTrip by tripViewModel.currentTrip.collectAsState()

    LaunchedEffect(editTripId) {
        if (editTripId != null) {
            tripViewModel.selectTrip(editTripId)
        }
    }
    LaunchedEffect(currentTrip) {
        val t = currentTrip
        if (t != null && editTripId != null) {
            tripName = t.name
            destination = t.destination
            durationDays = t.durationDays
            breakfastHour = t.breakfastHour
            breakfastMinute = t.breakfastMinute
            departureHour = t.departureHour
            departureMinute = t.departureMinute
            endOfDayHour = t.endOfDayHour
            hotelAddress = t.hotelAddress
        }
    }

    LaunchedEffect(Unit) {
        tripViewModel.saveResult.collect { id -> onSaved(id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editTripId != null) "Upravit výlet" else "Nový výlet") },
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
            // Trip name
            OutlinedTextField(
                value = tripName,
                onValueChange = { tripName = it; nameError = false },
                label = { Text("Název výletu *") },
                isError = nameError,
                supportingText = if (nameError) ({ Text("Název je povinný") }) else null,
                leadingIcon = { Icon(Icons.Default.DriveFileRenameOutline, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
            )

            // Destination
            OutlinedTextField(
                value = destination,
                onValueChange = { destination = it },
                label = { Text("Cílová destinace") },
                leadingIcon = { Icon(Icons.Default.LocationOn, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            )

            // Duration
            SectionCard(title = "Délka výletu") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(
                        onClick = { if (durationDays > 1) durationDays-- },
                        enabled = durationDays > 1
                    ) { Icon(Icons.Default.Remove, null) }

                    Text(
                        text = "$durationDays ${dayLabel(durationDays)}",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    IconButton(
                        onClick = { if (durationDays < 30) durationDays++ },
                        enabled = durationDays < 30
                    ) { Icon(Icons.Default.Add, null) }
                }
                Slider(
                    value = durationDays.toFloat(),
                    onValueChange = { durationDays = it.toInt() },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Breakfast & Departure times
            SectionCard(title = "Denní harmonogram") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "Snídaně",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TimePickerRow(
                        hour = breakfastHour,
                        minute = breakfastMinute,
                        onHourChange = {
                            breakfastHour = it
                            if (departureHour <= it) departureHour = it + 1
                        },
                        onMinuteChange = { breakfastMinute = it },
                        icon = Icons.Default.FreeBreakfast
                    )
                    Text(
                        "Snídaně trvá 1 hodinu – odjezd z ubytování je po skončení snídaně.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    HorizontalDivider()

                    Text(
                        "Odjezd z ubytování",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TimePickerRow(
                        hour = departureHour,
                        minute = departureMinute,
                        onHourChange = { departureHour = it },
                        onMinuteChange = { departureMinute = it },
                        icon = Icons.Default.HotelClass
                    )

                    HorizontalDivider()

                    Text(
                        "Konec dne (návrat do ubytování)",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TimePickerRow(
                        hour = endOfDayHour,
                        minute = 0,
                        onHourChange = { endOfDayHour = it },
                        onMinuteChange = {},
                        icon = Icons.Default.NightShelter,
                        hideMinutes = true
                    )
                }
            }

            // Hotel address
            OutlinedTextField(
                value = hotelAddress,
                onValueChange = { hotelAddress = it },
                label = { Text("Adresa ubytování (volitelné)") },
                leadingIcon = { Icon(Icons.Default.Hotel, null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Save button
            Button(
                onClick = {
                    if (tripName.isBlank()) { nameError = true; return@Button }
                    tripViewModel.saveTrip(
                        Trip(
                            id = editTripId ?: 0,
                            name = tripName.trim(),
                            destination = destination.trim(),
                            durationDays = durationDays,
                            breakfastHour = breakfastHour,
                            breakfastMinute = breakfastMinute,
                            departureHour = departureHour,
                            departureMinute = departureMinute,
                            endOfDayHour = endOfDayHour,
                            hotelAddress = hotelAddress.trim()
                        ),
                        userEmail = userEmail ?: ""
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(Icons.Default.NavigateNext, null)
                Spacer(Modifier.width(8.dp))
                Text("Uložit a přidat atrakce", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerRow(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    hideMinutes: Boolean = false
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)

        // Hour dropdown
        var hourExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = hourExpanded,
            onExpandedChange = { hourExpanded = it },
            modifier = Modifier.width(90.dp)
        ) {
            OutlinedTextField(
                value = "%02d".format(hour),
                onValueChange = {},
                readOnly = true,
                label = { Text("Hod") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(hourExpanded) },
                modifier = Modifier.menuAnchor()
            )
            ExposedDropdownMenu(expanded = hourExpanded, onDismissRequest = { hourExpanded = false }) {
                TimeUtils.hoursRange().forEach { h ->
                    DropdownMenuItem(
                        text = { Text("%02d".format(h)) },
                        onClick = { onHourChange(h); hourExpanded = false }
                    )
                }
            }
        }

        if (!hideMinutes) {
            Text(":", style = MaterialTheme.typography.titleLarge)

            // Minute dropdown
            var minExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = minExpanded,
                onExpandedChange = { minExpanded = it },
                modifier = Modifier.width(90.dp)
            ) {
                OutlinedTextField(
                    value = "%02d".format(minute),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Min") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(minExpanded) },
                    modifier = Modifier.menuAnchor()
                )
                ExposedDropdownMenu(expanded = minExpanded, onDismissRequest = { minExpanded = false }) {
                    TimeUtils.minuteSteps().forEach { m ->
                        DropdownMenuItem(
                            text = { Text("%02d".format(m)) },
                            onClick = { onMinuteChange(m); minExpanded = false }
                        )
                    }
                }
            }
        }
    }
}

private fun dayLabel(days: Int): String = when {
    days == 1 -> "den"
    days in 2..4 -> "dny"
    else -> "dní"
}
