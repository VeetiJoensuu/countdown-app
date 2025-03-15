package com.example.eventcountdown

import com.example.eventcountdown.ui.theme.EventCountdownTheme
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.content.SharedPreferences


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

@Composable
fun Greeting(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf("") }
    var selectedTime by remember { mutableStateOf("") }
    var textList by remember { mutableStateOf(listOf<String>()) }

    // Get SharedPreferences
    val context = LocalContext.current
    val sharedPreferences: SharedPreferences = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)

    // Load stored events from SharedPreferences
    LaunchedEffect(Unit) {
        val savedEvents = sharedPreferences.getStringSet("events", setOf())?.toList() ?: emptyList()
        textList = savedEvents
    }

    val calendar = Calendar.getInstance()

    Column(modifier = modifier.padding(16.dp)) {
        // Event Name input
        Row {
            TextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.weight(1f),
                label = { Text("Event Name") }
            )
        }
        Spacer(modifier = Modifier.height(8.dp))

        // Date and Time Selection
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

        // Add Event Button
        Button(onClick = {
            if (text.isNotBlank() && selectedDate.isNotBlank() && selectedTime.isNotBlank()) {
                val event = "$text on $selectedDate at $selectedTime"
                textList = textList + event
                text = ""
                selectedDate = ""
                selectedTime = ""

                // Save events to SharedPreferences
                sharedPreferences.edit().putStringSet("events", textList.toSet()).apply()
            }
        }) {
            Text("Enter")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Displaying the list of events
        LazyColumn {
            items(textList) { item ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item)
                    Button(onClick = {
                        textList = textList - item
                        // Update SharedPreferences after removal
                        sharedPreferences.edit().putStringSet("events", textList.toSet()).apply()
                    }) {
                        Text("Remove")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    EventCountdownTheme {
        Greeting()
    }
}


