package com.travellapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.travellapp.data.model.AttractionType
import com.travellapp.data.model.DayPlan
import com.travellapp.data.model.Itinerary
import com.travellapp.data.model.ScheduledAttraction
import com.travellapp.ui.viewmodel.TripViewModel
import com.travellapp.util.TimeUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    tripViewModel: TripViewModel,
    onBack: () -> Unit,
    onEditAttractions: (Int) -> Unit
) {
    val itinerary by tripViewModel.itinerary.collectAsState()
    val trip by tripViewModel.currentTrip.collectAsState()

    var selectedDay by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip?.name ?: "Itinerář") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zpět")
                    }
                },
                actions = {
                    IconButton(onClick = { trip?.let { onEditAttractions(it.id) } }) {
                        Icon(Icons.Default.Edit, contentDescription = "Upravit atrakce")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (itinerary == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val plan = itinerary!!
            Column(modifier = Modifier.padding(padding)) {
                // Summary bar
                ItinerarySummaryBar(plan)

                // Day tabs
                if (plan.days.size > 1) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedDay,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        plan.days.forEachIndexed { idx, day ->
                            Tab(
                                selected = selectedDay == idx,
                                onClick = { selectedDay = idx },
                                text = {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("Den ${day.dayNumber}", style = MaterialTheme.typography.labelMedium)
                                        Text(
                                            "${day.scheduledAttractions.size} atrakcí",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // Day content
                val dayPlan = plan.days.getOrNull(selectedDay)
                if (dayPlan != null) {
                    DayPlanContent(
                        dayPlan = dayPlan,
                        trip = plan.trip,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unscheduled warning
                if (plan.unscheduledAttractions.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(
                                "${plan.unscheduledAttractions.size} atrakcí se nevešlo do plánu: " +
                                    plan.unscheduledAttractions.joinToString(", ") { it.name },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ItinerarySummaryBar(itinerary: Itinerary) {
    val totalAttractions = itinerary.days.sumOf { it.scheduledAttractions.size }
    val activeDays = itinerary.days.count { it.scheduledAttractions.isNotEmpty() }

    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryItem(Icons.Default.CalendarMonth, "$activeDays", "aktivních dní")
            SummaryItem(Icons.Default.Attractions, "$totalAttractions", "atrakcí")
            SummaryItem(
                Icons.Default.Schedule,
                TimeUtils.formatDuration(itinerary.days.sumOf { it.totalAttractionMinutes }),
                "prohlídek"
            )
        }
    }
}

@Composable
private fun SummaryItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayPlanContent(
    dayPlan: DayPlan,
    trip: com.travellapp.data.model.Trip,
    modifier: Modifier = Modifier
) {
    if (dayPlan.scheduledAttractions.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.BeachAccess,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                )
                Text(
                    "Volný den – odpočinek!",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Hotel departure
        item {
            TimelineEvent(
                time = TimeUtils.minutesToHhmm(dayPlan.departureTimeMinutes),
                title = "Odjezd z ubytování",
                subtitle = trip.hotelAddress.ifBlank { "Vaše ubytování" },
                icon = Icons.Default.Hotel,
                isFirst = true,
                isLast = false,
                isHotel = true
            )
        }

        itemsIndexed(dayPlan.scheduledAttractions) { idx, scheduled ->
            // Travel segment
            TravelSegment(travelMinutes = scheduled.travelTimeFromPreviousMinutes)

            // Attraction event
            TimelineEvent(
                time = TimeUtils.minutesToHhmm(scheduled.arrivalTimeMinutes),
                title = scheduled.attraction.name,
                subtitle = buildAttractionSubtitle(scheduled),
                icon = if (scheduled.attraction.type == AttractionType.LARGE_PARK)
                    Icons.Default.Attractions else Icons.Default.Place,
                isFirst = false,
                isLast = idx == dayPlan.scheduledAttractions.lastIndex,
                isLargePark = scheduled.attraction.type == AttractionType.LARGE_PARK,
                departureTime = TimeUtils.minutesToHhmm(scheduled.departureTimeMinutes)
            )
        }
    }
}

@Composable
private fun TimelineEvent(
    time: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isFirst: Boolean,
    isLast: Boolean,
    isHotel: Boolean = false,
    isLargePark: Boolean = false,
    departureTime: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Timeline line + dot
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            if (!isFirst) Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .then(
                        if (isHotel || isLargePark)
                            Modifier
                        else
                            Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        isHotel -> MaterialTheme.colorScheme.tertiary
                        isLargePark -> MaterialTheme.colorScheme.secondary
                        else -> MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(if (departureTime != null) 56.dp else 32.dp)
                        .padding(vertical = 2.dp)
                ) {
                    Divider(
                        modifier = Modifier
                            .width(2.dp)
                            .fillMaxHeight(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }

        // Content
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 0.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = when {
                    isHotel -> MaterialTheme.colorScheme.tertiaryContainer
                    isLargePark -> MaterialTheme.colorScheme.secondaryContainer
                    else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                }
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        time,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (departureTime != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Odchod: $departureTime",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun TravelSegment(travelMinutes: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 19.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Vertical line piece
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(28.dp)
        ) {
            Divider(
                modifier = Modifier
                    .width(2.dp)
                    .fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(start = 32.dp)
        ) {
            Icon(
                Icons.Default.DirectionsWalk,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Přesun: ~${TimeUtils.formatDuration(travelMinutes)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun buildAttractionSubtitle(scheduled: ScheduledAttraction): String {
    val duration = TimeUtils.formatDuration(
        scheduled.departureTimeMinutes - scheduled.arrivalTimeMinutes
    )
    return buildString {
        append("Délka návštěvy: ~$duration")
        if (scheduled.attraction.address.isNotBlank()) {
            append(" • ${scheduled.attraction.address}")
        }
        if (scheduled.attraction.notes.isNotBlank()) {
            append("\n${scheduled.attraction.notes}")
        }
    }
}
