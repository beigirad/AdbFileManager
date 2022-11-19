import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice

@Composable
fun App() {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        val coroutines = rememberCoroutineScope { Dispatchers.IO }

        var selectedDevice by remember { mutableStateOf<JadbDevice?>(null) }
        var devices by remember { mutableStateOf(emptyList<JadbDevice>()) }

        LaunchedEffect(Unit) {
            coroutines.launch {
                devices = JadbConnection().devices
            }
        }

        LazyColumn {
            items(devices) {
                val isSelected by rememberUpdatedState(selectedDevice == it)
                Column(modifier = Modifier
                    .clickable { selectedDevice = it }
                    .background(if (isSelected) Color.LightGray else Color.Transparent)
                ) {
                    Text(text = it.state.toString())
                    Text(text = it.serial)
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            App()
        }
    }
}
