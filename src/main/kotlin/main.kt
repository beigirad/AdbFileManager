import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.RemoteFile

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun App() {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        var selectedDevice by remember { mutableStateOf<JadbDevice?>(null) }
        var devices by remember { mutableStateOf(emptyList<JadbDevice>()) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                devices = JadbConnection().devices
            }
        }

        var fullPath by remember { mutableStateOf(listOf("")) }

        LazyColumn {
            items(devices) {
                val isSelected by rememberUpdatedState(selectedDevice == it)
                Column(modifier = Modifier
                    .clickable {
                        selectedDevice = it
                        fullPath = listOf("")
                    }
                    .background(if (isSelected) Color.LightGray else Color.Transparent)
                ) {
                    Text(text = it.state.toString())
                    Text(text = it.serial)
                }
            }
        }

        Spacer(
            modifier = Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(Color.DarkGray)
        )
        fullPath.forEachIndexed { index, pathSection ->
            var folders by remember { mutableStateOf(listOf<RemoteFile>()) }
            val path =
                if (fullPath.size == 1) "/"
                else fullPath.drop(1).take(index).joinToString(prefix = "/", separator = "/")

            LaunchedEffect(pathSection, selectedDevice) {
                withContext(Dispatchers.IO) {
                    folders =
                        selectedDevice?.list(path)
                            .orEmpty()
                            .filterNot { it.path == "." || it.path == ".." }
                            .sortedBy { it.path }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .width(250.dp)
                    .padding(horizontal = 4.dp)
            ) {
                stickyHeader {
                    Text(
                        text = path,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Gray)
                            .padding(4.dp)
                    )
                }
                items(folders) { file ->
                    FileItem(
                        file = file,
                        isSelected = fullPath.take(index + 1).plus(file.path) == fullPath.take(index + 2),
                        onSelect = {
                            fullPath = fullPath.take(index + 1).plus(file.path)
                        })
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(Color.Black)
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun FileItem(
    file: RemoteFile,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) Color.LightGray else Color.Transparent)
            .clickable {
                onSelect()
            }
            .padding(4.dp)
    ) {
        if (file.isDirectory)
            Icon(imageVector = Icons.TwoTone.Folder, contentDescription = null)

        Text(
            text = file.path,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            App()
        }
    }
}
