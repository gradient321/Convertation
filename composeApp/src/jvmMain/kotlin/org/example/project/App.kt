package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.util.UUID

val BackgroundColor = Color(0xFF1E1E1E)
val DefaultBlockColors = listOf(
    Color(0xFF4A148C),  // Фиолетовый
    Color(0xFF0288D1),  // Синий
    Color(0xFF2E7D32),  // Зелёный
    Color(0xFFC62828),  // Красный
    Color(0xFF5D4037),  // Коричневый
    Color(0xFF6A1B9A),  // Тёмно-фиолетовый
    Color(0xFFFFA000),  // Оранжевый
    Color(0xFF37474F)   // Серый
)
val SelectionBorderColor = Color.White
const val BorderWidth = 2f
val MarqueeColor = Color(0x80FFFFFF) // Полупрозрачный белый

// Глобальное состояние Ctrl (доступно везде)
var isCtrlLeftPressed by mutableStateOf(false)

// Модель блока
data class Block(
    val id: String = UUID.randomUUID().toString(),
    var position: Offset = Offset.Zero,
    var size: Size = Size(100f, 100f),
    var color: Color = DefaultBlockColors[0],
    var isSelected: Boolean = false
)

fun main() = application {
    Window(
        onCloseRequest = { exitApplication() },
        title = "APP KT - Редактор блоков"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.key == Key.CtrlLeft) {
                        isCtrlLeftPressed = (event.type == KeyEventType.KeyDown)
                        true
                    } else {
                        false
                    }
                }
        ) {
            DragWithSelectionBorder()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateListOf<Block>() }
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedBlockIds by remember { mutableStateOf(setOf<String>()) }
    var marqueeStart by remember { mutableStateOf<Offset?>(null) }
    var marqueeEnd by remember { mutableStateOf<Offset?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createPosition by remember { mutableStateOf(Offset.Zero) }

    // Добавляем 2 начальных блока для демонстрации
    LaunchedEffect(Unit) {
        if (blocks.isEmpty()) {
            blocks.add(Block(position = Offset(0f, 0f), color = DefaultBlockColors[0]))
            blocks.add(Block(position = Offset(200f, 150f), color = DefaultBlockColors[1]))
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .onPointerEvent(PointerEventType.Press) { pointerEvent -> // ИСПРАВЛЕНО: правильное имя параметра
                println(1)
                if (pointerEvent.button== PointerButton.Secondary) {
                    val delta = pointerEvent.changes.firstOrNull()?.scrollDelta?.y ?: return@onPointerEvent
                    if (delta == 0f) return@onPointerEvent

                    val mousePos = pointerEvent.changes.first().position
                    val worldBefore = screenToWorld(mousePos, camera, zoom)
                    zoom = (zoom * (1f - delta * 0.1f)).coerceIn(0.2f, 5f)
                    val worldAfter = screenToWorld(mousePos, camera, zoom)
                    camera += (worldBefore - worldAfter)
                }
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)

                        // Проверяем ПКМ (Secondary)
//                        if (down.button == MouseButton.Secondary) {
//                            // Проверяем, кликнули ли на существующий блок
//                            val clickedBlock = blocks.find { block ->
//                                val screenPos = worldToScreen(block.position, camera, zoom)
//                                val screenSize = block.size * zoom
//                                isInside(down.position, screenPos, screenSize)
//                            }
//
//                            if (clickedBlock != null) {
//                                // ПКМ на блоке → удаляем его
//                                blocks.remove(clickedBlock)
//                            } else {
//                                // ПКМ на фоне → показываем диалог создания
//                                createPosition = screenToWorld(down.position, camera, zoom)
//                                showCreateDialog = true
//                            }
//                            down.consume()
//                            continue
//                        }

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

                            // По завершении — выделяем блоки внутри рамки
                            marqueeStart?.let { start ->
                                marqueeEnd?.let { end ->
                                    val rect = Rect(
                                        topLeft = Offset(minOf(start.x, end.x), minOf(start.y, end.y)),
                                        bottomRight = Offset(maxOf(start.x, end.x), maxOf(start.y, end.y))
                                    )

                                    val newSelection = mutableSetOf<String>()
                                    blocks.forEach { block ->
                                        val screenPos = worldToScreen(block.position, camera, zoom)
                                        val screenSize = block.size * zoom
                                        if (rect.overlaps(Rect(screenPos, screenSize))) {
                                            newSelection.add(block.id)
                                        }
                                    }
                                    selectedBlockIds = newSelection
                                }
                            }

                            marqueeStart = null
                            marqueeEnd = null
                        } else {
                            // Обычное перетаскивание
                            val clickedBlock = blocks.find { block ->
                                val screenPos = worldToScreen(block.position, camera, zoom)
                                val screenSize = block.size * zoom
                                isInside(down.position, screenPos, screenSize)
                            }

                            if (clickedBlock == null) {
                                // Клик по фону → снимаем выделение
                                selectedBlockIds = emptySet()
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

                            // Выделяем блок(и)
                            selectedBlockIds = setOf(clickedBlock.id)

                            // Перетаскивание
                            val initialPositions = blocks.associate { it.id to it.position }
                            val startPos = down.position
                            down.consume()

                            while (true) {
                                val event = awaitPointerEvent()
                                val move = event.changes.find { it.id == down.id }
                                if (move == null || !move.pressed) break

                                val deltaWorld = (move.position - startPos) / zoom
                                blocks.forEach { block ->
                                    if (block.id in selectedBlockIds) {
                                        block.position = initialPositions[block.id]!! + deltaWorld
                                    }
                                }
                                move.consume()
                            }
                        }
                    }
                }
            }
    ) {
        // Рисуем все блоки
        blocks.forEach { block ->
            val screenPos = worldToScreen(block.position, camera, zoom)
            val screenSize = block.size * zoom
            drawBlock(screenPos, block.color, screenSize, block.id in selectedBlockIds)
        }

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
                        width = 1f,
                        cap = StrokeCap.Butt,
                        join = StrokeJoin.Miter
                    )
                )
            }
        }
    }

    // Диалог создания блока
    if (showCreateDialog) {
        CreateBlockDialog(
            onConfirm = { width, height, color ->
                blocks.add(
                    Block(
                        position = createPosition,
                        size = Size(width, height),
                        color = color
                    )
                )
                showCreateDialog = false
            },
            onCancel = { showCreateDialog = false }
        )
    }
}

@Composable
fun CreateBlockDialog(
    onConfirm: (Float, Float, Color) -> Unit,
    onCancel: () -> Unit
) {
    var widthText by remember { mutableStateOf("100") }
    var heightText by remember { mutableStateOf("100") }
    var selectedColor by remember { mutableStateOf(DefaultBlockColors[0]) }

    val width = widthText.toFloatOrNull() ?: 100f
    val height = heightText.toFloatOrNull() ?: 100f
    val isValid = width in 10f..500f && height in 10f..500f

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .width(300.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Создать новый блок",
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = widthText,
                    onValueChange = {
                        // Разрешаем только цифры и точку
                        widthText = it.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("Ширина (10-500)") },
                    keyboardOptions = KeyboardOptions.Default,
                    singleLine = true,
                    isError = width !in 10f..500f
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("Высота (10-500)") },
                    keyboardOptions = KeyboardOptions.Default,
                    singleLine = true,
                    isError = height !in 10f..500f
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Цвет блока:", style = MaterialTheme.typography.bodyMedium)
                    // Сетка 4x2 для цветов
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Первая строка цветов
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { index ->
                                val color = DefaultBlockColors[index]
                                ColorOption(color, selectedColor) { selectedColor = color }
                            }
                        }
                        // Вторая строка цветов
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { index ->
                                val color = DefaultBlockColors[4 + index]
                                ColorOption(color, selectedColor) { selectedColor = color }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = onCancel, colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)) {
                        Text("Отмена")
                    }
                    Button(
                        onClick = { onConfirm(width, height, selectedColor) },
                        enabled = isValid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                    ) {
                        Text("Создать")
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorOption(color: Color, selectedColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .border(
                width = if (color == selectedColor) 2.dp else 1.dp,
                color = if (color == selectedColor) Color.White else Color.Gray,
                shape = RoundedCornerShape(4.dp)
            )
            .background(color, RoundedCornerShape(4.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        // Ручная отрисовка галочки без использования иконок
        if (color == selectedColor) {
            Canvas(modifier = Modifier.size(16.dp)) {
                drawLine(
                    color = Color.White,
                    start = Offset(3f, 8f),
                    end = Offset(7f, 12f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = Offset(7f, 12f),
                    end = Offset(13f, 4f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
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

private fun DrawScope.drawBlock(topLeft: Offset, color: Color, size: Size, isSelected: Boolean) {
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