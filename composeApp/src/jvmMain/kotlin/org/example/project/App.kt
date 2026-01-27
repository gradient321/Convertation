package org.example.project

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.util.*

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

// Модель блока (иммутабельная)
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset = Offset.Zero,
    val size: Size = Size(100f, 100f),
    val color: Color = DefaultBlockColors[0],
    val isSelected: Boolean = false
)

// Состояние перетаскивания
private data class DragState(
    val offset: Offset // Смещение между курсором и блоком
)

fun main() = application {
    Window(
        onCloseRequest = { exitApplication() },
        title = "APP KT - Редактор блоков (Исправленное перемещение)"
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            DragWithSelectionBorder()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateMapOf<String, Block>() }
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createPosition by remember { mutableStateOf(Offset.Zero) }

    var dragState by remember { mutableStateOf<DragState?>(null) }
    var panState by remember { mutableStateOf<PanState?>(null) }

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
                        val event = awaitPointerEvent()

                        // Обработка колеса мыши (уже обработано выше)
                        if (event.type == PointerEventType.Scroll) {
                            continue
                        }

                        val downChange = event.changes.find { it.pressed && !it.isConsumed }
                        if (downChange != null) {
                            val isRightClick = event.buttons.isSecondaryPressed
                            val isLeftClick = event.buttons.isPrimaryPressed

                            if (isRightClick) {
                                downChange.consume()
                                val clickedBlockIndex = blocks.values.indexOfFirst { block ->
                                    val screenPos = worldToScreen(block.position, camera, zoom)
                                    val screenSize = block.size * zoom
                                    isInside(downChange.position, screenPos, screenSize)
                                }

                                if (clickedBlockIndex != -1) {
                                    val clickedBlock = blocks.values.elementAt(clickedBlockIndex)
                                    blocks.remove(clickedBlock.id)
                                } else {
                                    createPosition = screenToWorld(downChange.position, camera, zoom)
                                    showCreateDialog = true
                                }
                                continue
                            }

                            if (isLeftClick) {
                                val clickedBlockIndex = blocks.values.indexOfFirst { block ->
                                    val screenPos = worldToScreen(block.position, camera, zoom)
                                    val screenSize = block.size * zoom
                                    isInside(downChange.position, screenPos, screenSize)
                                }

                                if (clickedBlockIndex != -1) {
                                    // Выделяем только один блок
                                    val clickedBlock = blocks.values.elementAt(clickedBlockIndex)
                                    selectedBlockId = clickedBlock.id

                                    // Начало перетаскивания: вычисляем смещение между курсором и блоком
                                    val cursorWorldPos = screenToWorld(downChange.position, camera, zoom)
                                    val offset = cursorWorldPos - clickedBlock.position

                                    dragState = DragState(offset)
                                    downChange.consume()

                                    while (true) {
                                        val moveEvent = awaitPointerEvent()

                                        if (moveEvent.type == PointerEventType.Scroll) {
                                            val scrollChange = moveEvent.changes.firstOrNull()
                                            if (scrollChange != null) {
                                                val delta = scrollChange.scrollDelta.y
                                                if (delta != 0f) {
                                                    val mousePos = scrollChange.position
                                                    val worldBefore = screenToWorld(mousePos, camera, zoom)
                                                    zoom = (zoom * (1f - delta * 0.1f)).coerceIn(0.2f, 5f)
                                                    val worldAfter = screenToWorld(mousePos, camera, zoom)
                                                    camera += (worldBefore - worldAfter)

                                                    // Обновляем позицию блока при изменении масштаба
                                                    if (dragState != null) {
                                                        val cursorWorldPos = screenToWorld(mousePos, camera, zoom)
                                                        val newPosition = cursorWorldPos - dragState!!.offset
                                                        val currentBlock = blocks[selectedBlockId!!]
                                                        if (currentBlock != null) {
                                                            blocks[selectedBlockId!!] = currentBlock.copy(position = newPosition)
                                                        }
                                                    }
                                                }
                                            }
                                            continue
                                        }

                                        val moveChange = moveEvent.changes.find { it.id == downChange.id }
                                        if (moveChange == null || !moveChange.pressed) break

                                        // Получаем текущую мировую позицию курсора
                                        val cursorWorldPos = screenToWorld(moveChange.position, camera, zoom)
                                        // Вычисляем новую позицию блока: курсор - смещение
                                        val newPosition = cursorWorldPos - dragState!!.offset

                                        val currentBlock = blocks[selectedBlockId!!]
                                        if (currentBlock != null) {
                                            blocks[selectedBlockId!!] = currentBlock.copy(position = newPosition)
                                        }

                                        moveChange.consume()
                                    }
                                    dragState = null
                                } else {
                                    // Клик на пустое место: сбросить выделение
                                    selectedBlockId = null
                                    // Начать панорамирование
                                    panState = PanState(
                                        initialCamera = camera,
                                        startPosition = downChange.position
                                    )
                                    downChange.consume()

                                    while (true) {
                                        val moveEvent = awaitPointerEvent()

                                        if (moveEvent.type == PointerEventType.Scroll) {
                                            continue
                                        }

                                        val moveChange = moveEvent.changes.find { it.id == downChange.id }
                                        if (moveChange == null || !moveChange.pressed) break

                                        val deltaScreen = moveChange.position - panState!!.startPosition
                                        val deltaWorld = deltaScreen / zoom
                                        camera = panState!!.initialCamera - deltaWorld

                                        moveChange.consume()
                                    }
                                    panState = null
                                }
                            }
                        }
                    }
                }
            }
    ) {
        blocks.values.forEach { block ->
            val screenPos = worldToScreen(block.position, camera, zoom)
            val screenSize = block.size * zoom
            drawBlock(screenPos, block.color, screenSize, block.id == selectedBlockId)
        }
    }

    if (showCreateDialog) {
        CreateBlockDialog(
            onConfirm = { width, height, color ->
                val newBlock = Block(
                    position = createPosition,
                    size = Size(width, height),
                    color = color
                )
                blocks[newBlock.id] = newBlock
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
                        widthText = it.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("Ширина (10-500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = width !in 10f..500f
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = {
                        heightText = it.filter { char -> char.isDigit() || char == '.' }
                    },
                    label = { Text("Высота (10-500)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = height !in 10f..500f
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Цвет блока:", style = MaterialTheme.typography.bodyMedium)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            repeat(4) { index ->
                                val color = DefaultBlockColors[index]
                                ColorOption(color, selectedColor) { selectedColor = color }
                            }
                        }
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

private data class PanState(
    val initialCamera: Offset,
    val startPosition: Offset
)

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