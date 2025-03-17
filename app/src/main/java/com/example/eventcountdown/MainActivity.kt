package com.example.eventcountdown

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.eventcountdown.ui.theme.EventCountdownTheme
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EventCountdownTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.HONEYCOMB)
@Composable
fun Greeting(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var textList by remember { mutableStateOf(listOf<String>()) }

    val context = LocalContext.current
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        try {
            val savedEvents = sharedPreferences.getStringSet("events", null)
            textList = savedEvents?.toList() ?: emptyList()
            Log.d("EventCountdown", "Loaded events: $textList")
        } catch (e: Exception) {
            Log.e("EventCountdown", "Error loading events", e)
        }
    }

    val calendar = Calendar.getInstance()

    Column(modifier = modifier.padding(16.dp)) {
        Row {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                label = { Text("Event Name") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        Row {
            Button(onClick = {
                DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        selectedDate = "$year-${month + 1}-$dayOfMonth"
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
            }) {
                Text(if (selectedDate.isBlank()) "Select Date" else selectedDate)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        selectedTime = String.format("%02d:%02d", hour, minute)
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                ).show()
            }) {
                Text(if (selectedTime.isBlank()) "Select Time" else selectedTime)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (text.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
                val event = "$text;$selectedDate;$selectedTime"
                textList = textList + event
                text = ""
                selectedDate = ""
                selectedTime = ""

                try {
                    sharedPreferences.edit().putStringSet("events", textList.toSet()).apply()
                    Log.d("EventCountdown", "Saved events: $textList")
                } catch (e: Exception) {
                    Log.e("EventCountdown", "Error saving events", e)
                }
            } else {
                Log.w("EventCountdown", "Invalid event data (either text, date or time is missing)")
            }
        }) {
            Text("Enter")
        }

        Spacer(modifier = Modifier.height(16.dp))

        val sortedAndFilteredEvents = textList.filter { event ->
            val parts = event.split(";")
            if (parts.size < 3) return@filter false

            try {
                val dateTime = "${parts[1]} ${parts[2]}"
                val eventTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateTime)?.time
                    ?: return@filter false

                val twentyFourHoursInMillis = 24 * 60 * 60 * 1000L
                System.currentTimeMillis() - eventTime <= twentyFourHoursInMillis
            } catch (e: Exception) {
                Log.e("EventCountdown", "Error filtering events: $event", e)
                false
            }
        }.sortedBy { event ->
            try {
                val parts = event.split(";")
                val dateTime = "${parts[1]} ${parts[2]}"
                SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(dateTime)?.time ?: Long.MAX_VALUE
            } catch (e: Exception) {
                Log.e("EventCountdown", "Error parsing event for sorting: $event", e)
                Long.MAX_VALUE
            }
        }

        LazyColumn {
            items(sortedAndFilteredEvents) { item ->
                val parts = item.split(";")
                val eventName = parts[0]
                val eventDate = parts[1]
                val eventTime = parts[2]

                val eventDateTime = try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse("$eventDate $eventTime")
                } catch (e: Exception) {
                    Log.e("EventCountdown", "Error parsing event date/time", e)
                    null
                }
                val remainingTime = eventDateTime?.time?.minus(System.currentTimeMillis()) ?: 0L

                val days = (remainingTime / (1000 * 60 * 60 * 24)).toInt()
                val hours = (remainingTime / (1000 * 60 * 60) % 24).toInt()
                val minutes = (remainingTime / (1000 * 60) % 60).toInt()

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            "$eventName on $eventDate at $eventTime",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (remainingTime > 0) {
                            Text(
                                "$days days, $hours hours, $minutes minutes remaining",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        } else {
                            Text("Countdown finished (deleted within 24 hours)", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                textList = textList - item
                                try {
                                    sharedPreferences.edit()
                                        .putStringSet("events", textList.toSet()).apply()
                                    Log.d("EventCountdown", "Deleted event: $item")
                                } catch (e: Exception) {
                                    Log.e("EventCountdown", "Error deleting event", e)
                                }
                            }
                        ) {
                            Text("Delete")
                        }
                    }
                }
            }
        }
    }
}


