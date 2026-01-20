import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

val BackgroundColor = Color(0xFF1E1E1E)
val CubeColor1 = Color(0xFF4A148C)
val CubeColor2 = Color(0xFF0288D1)
val SelectionBorderColor = Color.White
val BorderWidth = 2f

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Shift+Клик — множественное выделение",
        // Обрабатываем нажатия клавиш на уровне окна
        onPreviewKeyEvent = { event ->
            false // пропускаем все события дальше
        }
    ) {
        MultiSelectWithShift()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MultiSelectWithShift() {
    var cube1 by remember { mutableStateOf(Offset(0f, 0f)) }
    var cube2 by remember { mutableStateOf(Offset(200f, 150f)) }
    val cubeSize = Size(100f, 100f)
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedCubes by remember { mutableStateOf(setOf<SelectedCube>()) }

    // Глобальное состояние Shift
    var isShiftPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()            .onPreviewKeyEvent { event ->
                if (event.key == Key.ShiftLeft || event.key == Key.ShiftRight) {
                    isShiftPressed = event.type == KeyEventType.KeyDown
                    true
                } else {
                    false
                }
            }
    ) {
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
                .pointerInput(isShiftPressed) { // Перезапускается при изменении isShiftPressed
                    awaitPointerEventScope {
                        while (true) {
                            val down = awaitFirstDown(requireUnconsumed = false)

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
                                continue                            }

                            val newSelection = mutableSetOf<SelectedCube>()
                            if (isShiftPressed) {
                                // Добавляем к текущему выделению
                                newSelection.addAll(selectedCubes)
                                if (isOn1) {
                                    if (SelectedCube.Cube1 in newSelection) {
                                        newSelection.remove(SelectedCube.Cube1)
                                    } else {
                                        newSelection.add(SelectedCube.Cube1)
                                    }
                                }
                                if (isOn2) {
                                    if (SelectedCube.Cube2 in newSelection) {
                                        newSelection.remove(SelectedCube.Cube2)
                                    } else {
                                        newSelection.add(SelectedCube.Cube2)
                                    }
                                }
                            } else {
                                // Заменяем выделение
                                if (isOn1) newSelection.add(SelectedCube.Cube1)
                                if (isOn2) newSelection.add(SelectedCube.Cube2)
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
                }        ) {
            drawCube(worldToScreen(cube1, camera, zoom), CubeColor1, cubeSize * zoom, SelectedCube.Cube1 in selectedCubes)
            drawCube(worldToScreen(cube2, camera, zoom), CubeColor2, cubeSize * zoom, SelectedCube.Cube2 in selectedCubes)
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