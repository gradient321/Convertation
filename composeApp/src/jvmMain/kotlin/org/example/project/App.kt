package org.example.project

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.text.contextmenu.modifier.appendTextContextMenuComponents
import androidx.compose.material.contextMenuOpenDetector
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.application
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Window

import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.compose_multiplatform

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "ComposeDemo",
        onKeyEvent = {event->
            println(event.key)
            true
        },
    ) {
        App()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun App() {
    MaterialTheme {
        var сообщение by remember { mutableStateOf("Нажмите ЛКМ или ПКМ") }
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .onPointerEvent(PointerEventType.Press) { event ->
                    // Обрабатываем только нажатие кнопки
//                    val mouseEvent = event.changes.first().mouseEvent
//                    if (mouseEvent?.isSecondaryPressed == true) {
                    if (event.button== PointerButton.Secondary) {
                        сообщение = "Вы нажали ПКМ!"
//                    } else if (mouseEvent?.isPrimaryPressed == true) {
                    }
                    
                    false // не перехватываем событие полностью (если не нужно)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(сообщение)
        }
    }
}

@Composable
@Preview
fun App1() {
    MaterialTheme {
        var showContent by remember { mutableStateOf(false) }
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            println("Нажата левая кнопка мыши (ЛКМ)")
                        },
//                        onSecondaryClick = {
//                            println("Нажата правая кнопка мыши (ПКМ)")
//                            // Ваше действие при ПКМ
//                        }
                    )
                }
            ,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(onClick = { showContent = !showContent }) {
                Text("Click me!")
            }
            AnimatedVisibility(showContent) {
                val greeting = remember { Greeting().greet() }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(painterResource(Res.drawable.compose_multiplatform), null)
                    Text("Compose: $greeting")
                }
            }
        }
    }
}