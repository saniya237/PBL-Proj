package com.example.nextwin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.nextwin.data.SampleRepository
import com.example.nextwin.data.Topology
import com.example.nextwin.ui.theme.NexTwinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val topology = SampleRepository(this).loadTopology()

        setContent {
            NexTwinTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    TopologyCheck(topology, Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun TopologyCheck(topology: Topology, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(16.dp)) {
        Text("Sample topology loaded")
        Text("Devices: ${topology.devices.size}   Links: ${topology.links.size}")
        LazyColumn {
            items(topology.devices) { d ->
                Text("${d.id} | ${d.name} | ${d.type} | ${d.status}")
            }
        }
    }
}