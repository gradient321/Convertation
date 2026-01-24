package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.runtime.Composable
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput

val BackgroundColor = Color(0xFF1E1E1E)
val CubeColor1 = Color(0xFF4A148C)
val CubeColor2 = Color(0xFF0288D1)
val SelectionBorderColor = Color.White
const val BorderWidth = 2f
val MarqueeColor = Color(0x80FFFFFF) // Полупрозрачный белый

//fun main() = application {
//    Window(
//        onCloseRequest = ::exitApplication,
//        title = "Рамочное выделение (Ctrl+ЛКМ)"
//    ) {
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .onPreviewKeyEvent { event ->
//
//                    if (event.key == Key.CtrlLeft) {
//                        isCtrlLeftPressed = (event.type == KeyEventType.KeyDown)
//                        println("Глобальный CtrlLeft нажат")
//                        true
//                    } else {
//                        false
//                    }
//                }
//        ) {
//            DragWithSelectionBorder()
//        }
//    }
//}




fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Кубик с рамкой при выборе",
        onKeyEvent = { event ->
            if (event.key == Key.CtrlLeft && event.type == KeyEventType.KeyDown) {
                println("Глобальный CtrlLeft нажат")
                true
            } else {
                false
            }
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
            DragWithSelectionBorder()
        }
    }
}




// ПКМ в инете так прописано но тут не работает все импорты и т п есть все сверил но нихуя не работает
//fun RightClickExample() {
//    Box(
//        modifier = Modifier
//            .size(200.dp)
//            .background(Color.LightGray)
//            .pointerInput(Unit) {
//                detectTapGestures(
//                    onSecondaryTap = {
//                        println("Нажата правая кнопка мыши (ПКМ)")
//                        // Действие при клике
//                    }
//                )
//            }
//    )
//}











// Глобальное состояние Ctrl (доступно везде)
var isCtrlLeftPressed by mutableStateOf(false)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    var cube1 by remember { mutableStateOf(Offset(0f, 0f)) }
    var cube2 by remember { mutableStateOf(Offset(200f, 150f)) }
    val cubeSize = Size(100f, 100f)
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedCubes by remember { mutableStateOf(setOf<SelectedCube>()) }

    // Состояние рамочного выделения
    var marqueeStart by remember { mutableStateOf<Offset?>(null) }
    var marqueeEnd by remember { mutableStateOf<Offset?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                val delta = event.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                if (delta == 0f) return@onPointerEvent

                val mousePos = event.changes.first().position
                val worldBefore = screenToWorld(mousePos, camera, zoom)
                zoom = (zoom * (1f - delta * 0.1f)).coerceIn(0.2f, 5f)
                val worldAfter = screenToWorld(mousePos, camera, zoom)
                camera += (worldBefore - worldAfter)
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Если зажат Ctrl → начинаем рамочное выделение
                        if (isCtrlLeftPressed) {
                            marqueeStart = down.position
                            marqueeEnd = down.position
                            down.consume()

                            // Рисуем рамку при движении
                            while (true) {
                                val event = awaitPointerEvent()
                                val move = event.changes.find { it.id == down.id }
                                if (move == null || !move.pressed) break
                                marqueeEnd = move.position
move.consume()
}
// По завершении — выделяем кубики внутри рамки
marqueeStart?.let { start ->
    marqueeEnd?.let { end ->
        val rect = Rect(
            topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
            bottomRight = Offset(maxOf(start.x, end.x), maxOf(start.y, end.y))
        )

        val screen1 = worldToScreen(cube1, camera, zoom)
        val screen2 = worldToScreen(cube2, camera, zoom)
        val size1 = cubeSize * zoom
        val size2 = cubeSize * zoom

        val newSelection = mutableSetOf<SelectedCube>()
        if (rect.overlaps(Rect(screen1, size1))) {
            newSelection.add(SelectedCube.Cube1)
        }
        if (rect.overlaps(Rect(screen2, size2))) {
            newSelection.add(SelectedCube.Cube2)
        }
        selectedCubes = newSelection
    }
}

marqueeStart = null
marqueeEnd = null
} else {
    // Обычное перетаскивание
    val screen1 = worldToScreen(cube1, camera, zoom)
    val screen2 = worldToScreen(cube2, camera, zoom)
    val size1 = cubeSize * zoom
    val size2 = cubeSize * zoom

    val isOn1 = isInside(down.position, screen1, size1)
    val isOn2 = isInside(down.position, screen2, size2)

    if (!isOn1 && !isOn2) {
        selectedCubes = emptySet()
        val initialCam = camera
        val startPos = down.position
        down.consume()
        while (true) {
            val event = awaitPointerEvent()
            val move = event.changes.find { it.id == down.id }
            if (move == null || !move.pressed) break
            val deltaWorld = (move.position - startPos) / zoom
            camera = initialCam - deltaWorld
            move.consume()
        }
        continue
    }

    val newSelection = mutableSetOf<SelectedCube>().apply {
        if (isOn1) add(SelectedCube.Cube1)
        if (isOn2) add(SelectedCube.Cube2)
    }
    selectedCubes = newSelection

    val initial1 = cube1
    val initial2 = cube2
    val startPos = down.position
    down.consume()

    while (true) {
        val event = awaitPointerEvent()
        val move = event.changes.find { it.id == down.id }
        if (move == null || !move.pressed) break

        val deltaWorld = (move.position - startPos) / zoom
        if (SelectedCube.Cube1 in selectedCubes) {
            cube1 = initial1 + deltaWorld
        }
        if (SelectedCube.Cube2 in selectedCubes) {
            cube2 = initial2 + deltaWorld
        }
        move.consume()
    }
}
}
}
}
) {
    // Рисуем кубики
    drawCube(worldToScreen(cube1, camera, zoom), CubeColor1, cubeSize * zoom, SelectedCube.Cube1 in selectedCubes)
    drawCube(worldToScreen(cube2, camera, zoom), CubeColor2, cubeSize * zoom, SelectedCube.Cube2 in selectedCubes)

    // Рисуем рамку выделения
    marqueeStart?.let { start ->
        marqueeEnd?.let { end ->
            val left = minOf(start.x, end.x)
            val top = minOf(start.y, end.y)
            val right = maxOf(start.x, end.x)
            val bottom = maxOf(start.y, end.y)
            drawRect(
                color = MarqueeColor,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                style = Stroke(
                    width = 1f,                        cap = StrokeCap.Butt,
                    join = StrokeJoin.Miter
                )
            )
        }
    }
}
}

// Вспомогательные функции
private fun worldToScreen(world: Offset, camera: Offset, zoom: Float): Offset = (world - camera) * zoom
private fun screenToWorld(screen: Offset, camera: Offset, zoom: Float): Offset = screen / zoom + camera

private fun isInside(point: Offset, rectTopLeft: Offset, size: Size): Boolean {
    return point.x >= rectTopLeft.x &&
            point.x <= rectTopLeft.x + size.width &&
            point.y >= rectTopLeft.y &&
            point.y <= rectTopLeft.y + size.height
}

private fun DrawScope.drawCube(topLeft: Offset, color: Color, size: Size, isSelected: Boolean) {
    drawRect(color = color, topLeft = topLeft, size = size)
    if (isSelected) {
        drawRect(
            color = SelectionBorderColor,
            topLeft = topLeft,
            size = size,
            style = Stroke(width = BorderWidth)
        )
    }
}

private sealed interface SelectedCube {
    object Cube1 : SelectedCube
    object Cube2 : SelectedCube
}