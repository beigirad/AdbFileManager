import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.CopyAll
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Folder
import androidx.compose.material.icons.twotone.UploadFile
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberCursorPositionProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import se.vidstige.jadb.JadbConnection
import se.vidstige.jadb.JadbDevice
import se.vidstige.jadb.RemoteFile
import javax.swing.JFileChooser

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameWindowScope.App() {
    Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
        val coroutines = rememberCoroutineScope { Dispatchers.IO }

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
            var version by remember { mutableStateOf(0) }
            val path =
                if (fullPath.size == 1) "/"
                else fullPath.drop(1).take(index).joinToString(prefix = "/", separator = "/")

            LaunchedEffect(pathSection, selectedDevice, version) {
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
                            .background(Color.LightGray)
                            .padding(4.dp)
                    )
                }
                items(folders) { file ->
                    FileItem(
                        file = file,
                        isSelected = fullPath.take(index + 1).plus(file.path) == fullPath.take(index + 2),
                        onSelect = {
                            fullPath = fullPath.take(index + 1).plus(file.path)
                        },
                        onDelete = {
                            coroutines.launch {
                                val stream = if (file.isDirectory)
                                    selectedDevice!!.executeShell("rm", "-r", "$path/${file.path}")
                                else
                                    selectedDevice!!.executeShell("rm", "$path/${file.path}")
                                val message = stream.readBytes().decodeToString()
                                if (message.isNotBlank())
                                    println(message)
                                version++
                            }
                        },
                        onDuplicate = {
                            coroutines.launch {
                                val stream =
                                    selectedDevice!!.executeShell(
                                        "cp",
                                        "$path/${file.path}",
                                        "$path/${file.path}_2"
                                    )
                                val message = stream.readBytes().decodeToString()
                                if (message.isNotBlank())
                                    println(message)
                                version++
                            }
                        },
                        onUploadClick = {
                            val home = System.getProperty("user.home")
                            val fileChooser = JFileChooser(home).apply {
                                fileSelectionMode = JFileChooser.FILES_AND_DIRECTORIES
                            }

                            fileChooser.showOpenDialog(window)
                            val result = fileChooser.selectedFile
                            println(result)
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
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    onUploadClick: () -> Unit,
) {
    var showPopUp by remember { mutableStateOf(false) }

    if (showPopUp) {
        Popup(
            onDismissRequest = { showPopUp = false },
            focusable = true,
            popupPositionProvider = rememberCursorPositionProvider()
        ) {
            Card {
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .padding(4.dp)
                ) {
                    PopupItem(
                        onClick = {
                            onDelete()
                            showPopUp = false
                        },
                        content = {
                            Icon(imageVector = Icons.TwoTone.Delete, contentDescription = null)
                            Text(text = "Delete")
                        })

                    PopupItem(
                        onClick = {
                            onDuplicate()
                            showPopUp = false
                        },
                        content = {
                            Icon(imageVector = Icons.TwoTone.CopyAll, contentDescription = null)
                            Text(text = "Duplicate")
                        })

                    PopupItem(
                        onClick = {
                            onUploadClick()
                            showPopUp = false
                        },
                        content = {
                            Icon(imageVector = Icons.TwoTone.UploadFile, contentDescription = null)
                            Text(text = "Upload")
                        })
                }
            }
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(if (isSelected) Color.LightGray else Color.Transparent)
            .clickable {
                onSelect()
            }
            .onPointerEvent(PointerEventType.Press) {
                if (it.buttons.isSecondaryPressed) {
                    onSelect()
                    showPopUp = true
                }
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

@Composable
private fun PopupItem(
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(4.dp),
        content = content
    )
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication) {
        MaterialTheme {
            App()
        }
    }
}
