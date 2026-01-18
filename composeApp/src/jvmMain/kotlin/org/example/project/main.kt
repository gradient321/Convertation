import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

val BackgroundColor = Color(0xFF1E1E1E)
val CubeColor1 = Color(0xFF4A148C) // Фиолетовый
val CubeColor2 = Color(0xFF0288D1) // Голубой
val SelectionBorderColor = Color.White // Цвет рамки
val SelectionBorderWidth = 3f         // Толщина рамки

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Кубик с рамкой при выборе"
    ) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundColor)) {
            DragWithSelectionBorder()
        }
    }
}

@Composable
fun DragWithSelectionBorder() {
    var cube1 by remember { mutableStateOf(Offset(0f, 0f)) }
    var cube2 by remember { mutableStateOf(Offset(200f, 150f)) }
    val cubeSize = Size(100f, 100f)
    var camera by remember { mutableStateOf(Offset.Zero) }

    // Какой кубик сейчас перетаскивается?
    var draggingCube by remember { mutableStateOf<DraggingCube?>(null) }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        val screen1 = cube1 - camera
                        val screen2 = cube2 - camera
                        val isOnCube1 = isInside(screen1, down.position, cubeSize)
                        val isOnCube2 = isInside(screen2, down.position, cubeSize)

                        down.consume()

                        val target = when {
                            isOnCube1 -> DraggingCube.Cube1
                            isOnCube2 -> DraggingCube.Cube2
                            else -> null
                        }

                        draggingCube = target

                        val initial1 = cube1
                        val initial2 = cube2
                        val initialCam = camera
                        val startPos = down.position

                        while (true) {
                            val event = awaitPointerEvent()
                            val move = event.changes.find { it.id == down.id }
                            if (move == null || !move.pressed) break

                            val delta = move.position - startPos
                            when (target) {
                                DraggingCube.Cube1 -> cube1 = initial1 + delta
                                DraggingCube.Cube2 -> cube2 = initial2 + delta
                                null -> camera = initialCam - delta
                            }
                            move.consume()
                        }

                        draggingCube = null
                        waitForUpOrCancellation()
                    }
                }
            }
    ) {
        // Рисуем оба кубика
        drawCube(cube1 - camera, CubeColor1, cubeSize, draggingCube == DraggingCube.Cube1)
        drawCube(cube2 - camera, CubeColor2, cubeSize, draggingCube == DraggingCube.Cube2)
    }
}

// Вспомогательная функция для рисования кубика с рамкой
private fun DrawScope.drawCube(
    topLeft: Offset,
    color: Color,
    size: Size,
    isSelected: Boolean
) {
    // Заливка
    drawRect(
        color = color,
        topLeft = topLeft,
        size = size
    )
    // Рамка, если выбран
    if (isSelected) {
        drawRect(
            color = SelectionBorderColor,
            topLeft = topLeft,
            size = size,
            style = Stroke(
                width = SelectionBorderWidth,
                cap = StrokeCap.Butt,
                join = StrokeJoin.Miter
            )
        )
    }
}

private fun isInside(topLeft: Offset, point: Offset, size: Size): Boolean {
    return point.x >= topLeft.x &&
            point.x <= topLeft.x + size.width &&
            point.y >= topLeft.y &&
            point.y <= topLeft.y + size.height
}

private sealed interface DraggingCube {
    object Cube1 : DraggingCube
    object Cube2 : DraggingCube
}