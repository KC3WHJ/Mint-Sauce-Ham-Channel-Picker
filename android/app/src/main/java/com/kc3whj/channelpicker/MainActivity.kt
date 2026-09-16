package com.kc3whj.channelpicker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                ChannelViewerApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelViewerApp() {
    val context = LocalContextCompat()
    var selectedRadio by remember { mutableStateOf(RADIO_SOURCES.first()) }
    var query by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val allChannels = remember(selectedRadio) {
        selectedRadio.assetFile?.let { loadChannels(context, it) } ?: emptyList()
    }

    // Display numbers are computed from each channel's position in this
    // original, as-loaded list (matching the order it was actually
    // programmed onto the radio in) - never from a filtered/grouped
    // subset's index, which wouldn't match the radio's real numbering.
    val displayed = remember(allChannels, selectedRadio) {
        allChannels.mapIndexed { i, ch ->
            DisplayChannel(ch, displayNumber(selectedRadio.numbering, ch, i))
        }
    }

    val filtered = remember(displayed, query) {
        if (query.isBlank()) displayed
        else displayed.filter {
            it.channel.name.contains(query, ignoreCase = true) ||
                it.displayNum == query.trim() ||
                formatFreq(it.channel.rxMhz).contains(query)
        }
    }

    val grouped = remember(filtered) { filtered.groupBy { it.channel.section } }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Channel Viewer") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize().padding(12.dp)) {
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = selectedRadio.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Radio") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    RADIO_SOURCES.forEach { source ->
                        DropdownMenuItem(
                            text = { Text(source.label) },
                            onClick = {
                                selectedRadio = source
                                query = ""
                                expanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search name, channel #, or frequency") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(8.dp))

            if (selectedRadio.assetFile == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        selectedRadio.note ?: "No channel data for this radio.",
                        modifier = Modifier.padding(24.dp)
                    )
                }
            } else {
                selectedRadio.note?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                Text(
                    "${filtered.size} of ${allChannels.size} channels",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    grouped.forEach { (section, channels) ->
                        item {
                            Text(
                                section,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 4.dp)
                            )
                        }
                        items(channels) { dc -> ChannelRow(dc) }
                    }
                }
            }
        }
    }
}

data class DisplayChannel(val channel: Channel, val displayNum: String)

@Composable
fun ChannelRow(dc: DisplayChannel) {
    val ch = dc.channel
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            dc.displayNum,
            modifier = Modifier.width(48.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(ch.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                formatFreq(ch.rxMhz) + " MHz" + if (ch.rxMhz != ch.txMhz) " (TX ${formatFreq(ch.txMhz)})" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            ch.mode,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.width(56.dp)
        )
    }
    HorizontalDivider()
}

fun formatFreq(mhz: Double): String = String.format(Locale.US, "%.6f", mhz)

@Composable
fun LocalContextCompat() = androidx.compose.ui.platform.LocalContext.current
