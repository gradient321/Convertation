package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import java.util.UUID
import kotlin.math.*

// ===== ВСЕ ЭЛЕМЕНТЫ ВНУТРИ ОДНОГО ФАЙЛА =====

class Data

abstract class Element {
    var data: Data? = null
}

open class TextElement(var text: String = "") : Element()

abstract class NumberElement : Element() {
    open var number: Number = 0.0
}

open class IntElement(var int: Int = 0) : NumberElement() {
    override var number: Number
        get() = int
        set(value) { int = value.toInt() }
}

open class DoubleElement(var double: Double = 0.0) : NumberElement() {
    override var number: Number
        get() = double
        set(value) { double = value.toDouble() }
}

open class ChoiceElement(
    var text: String = "",
    var choices: List<String> = listOf(),
    var isOnlyChoices: Boolean = false
) : Element()

open class Block_(var text: String = "", var elements: Map<String, Element> = mapOf()) : Element()
open class BlockUnderText(text: String = "", elements: Map<String, Element> = mapOf()) : Block_(text, elements)

open class IntLimitElement(
    int: Int = 0,
    var limit: List<IntRange> = listOf(Int.MIN_VALUE..Int.MAX_VALUE)
) : IntElement(int)

data class DoubleRange(val from: Double, val to: Double)
fun Double.rangeTo(to: Double): DoubleRange = DoubleRange(this, to)

open class DoubleLimitElement(
    double: Double = 0.0,
    var limit: List<DoubleRange> = listOf(DoubleRange(Double.MIN_VALUE, Double.MAX_VALUE))
) : DoubleElement(double)

// ===== СТРЕЛКИ =====
data class ArrowStyle(
    val color: Color = Color(0xFF42A5F5),
    val thickness: Float = 2.5f,
    val arrowheadSize: Float = 12f  // Размер наконечника
)

data class ArrowElement(
    val id: String = UUID.randomUUID().toString(),
    val sourceBlockId: String,
    val targetBlockId: String,
    val style: ArrowStyle = ArrowStyle()
)

// ===== ОСНОВНОЙ РЕДАКТОР =====
val BackgroundColor = Color(0xFF1E1E1E)
val DefaultBlockColors = listOf(
    Color(0xFF4A148C), Color(0xFF0288D1), Color(0xFF2E7D32), Color(0xFFC62828),
    Color(0xFF5D4037), Color(0xFF6A1B9A), Color(0xFFFFA000), Color(0xFF37474F)
)
val SelectionBorderColor = Color.White
const val BorderWidth = 2f

data class Block(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset = Offset.Zero,
    val size: Size = Size(100f, 100f),
    val color: Color = DefaultBlockColors[0],
    val isSelected: Boolean = false,
    val content: Element? = null
)

private data class DragState(val offset: Offset)
private data class PanState(val initialCamera: Offset, val startPosition: Offset)
private data class ConnectionMode(val sourceBlockId: String)

@Composable
fun BlockComponent(
    position: Offset,
    size: Size,
    color: Color,
    isSelected: Boolean,
    content: Element?,
    zoom: Float
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(size.width.dp, size.height.dp)
            .background(color)
            .border(
                width = if (isSelected) BorderWidth.dp else 0.dp,
                color = SelectionBorderColor,
                shape = RoundedCornerShape(4.dp)  // Скруглённые углы для блоков
            )
    ) {
        if (content != null) {
            val text = when (content) {
                is TextElement -> content.text
                is IntElement -> content.int.toString()
                is DoubleElement -> content.double.toString()
                is ChoiceElement -> content.text
                is Block_ -> content.text
                is BlockUnderText -> content.text
                is IntLimitElement -> content.int.toString()
                is DoubleLimitElement -> content.double.toString()
                else -> "Unknown"
            }

            val baseFontSizePx = size.height * 0.3f
            val minFontSizePx = 6f
            val fontSizePx = baseFontSizePx.coerceAtLeast(minFontSizePx)
            val fontSize = fontSizePx.sp

            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),  // Больше отступов для текста
                style = TextStyle(
                    color = Color.Black,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.2f
                ),
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ArrowComponent(start: Offset, end: Offset, style: ArrowStyle, zoom: Float) {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.98f }) {
        // Основная линия стрелки
        drawLine(
            color = style.color,
            start = start,
            end = end,
            strokeWidth = style.thickness * zoom.coerceAtLeast(0.6f),
            cap = StrokeCap.Round
        )

        // Наконечник стрелки (треугольник) - только если достаточно близко для видимости
        if (zoom > 0.4f) {
            val arrowheadSize = style.arrowheadSize * zoom.coerceAtMost(1.5f)
            val angle = atan2(end.y - start.y, end.x - start.x)
            val arrowSideAngle = PI / 6  // 30 градусов

            // Точки треугольника наконечника
            val p1 = Offset(
                end.x - arrowheadSize * cos(angle - arrowSideAngle).toFloat(),
                end.y - arrowheadSize * sin(angle - arrowSideAngle).toFloat()
            )
            val p2 = Offset(
                end.x - arrowheadSize * cos(angle + arrowSideAngle).toFloat(),
                end.y - arrowheadSize * sin(angle + arrowSideAngle).toFloat()
            )

            // Рисуем закрашенный треугольник
            drawPath(
                path = Path().apply {
                    moveTo(end.x, end.y)
                    lineTo(p1.x, p1.y)
                    lineTo(p2.x, p2.y)
                    close()
                },
                color = style.color
            )
        }
    }
}

fun main() = application {
    Window(onCloseRequest = { exitApplication() }, title = "APP KT - Редактор блоков с соединениями") {
        Box(modifier = Modifier.fillMaxSize()) {
            DragWithSelectionBorder()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateMapOf<String, Block>() }
    val arrows = remember { mutableStateListOf<ArrowElement>() }
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createPosition by remember { mutableStateOf(Offset.Zero) }
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var selectedBlockForContextMenu by remember { mutableStateOf<Block?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<Block?>(null) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var panState by remember { mutableStateOf<PanState?>(null) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }

    // Для разъединения блоков
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var blockToDisconnect by remember { mutableStateOf<Block?>(null) }
    var connectionsToDisconnect by remember { mutableStateOf<List<ArrowElement>>(emptyList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                if (showCreateDialog || showEditDialog || showContextMenu || showDisconnectDialog) return@onPointerEvent
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
                        event.changes.firstOrNull()?.let { cursorPosition = it.position }

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
                                    selectedBlockForContextMenu = blocks.values.elementAt(clickedBlockIndex)
                                    contextMenuPosition = downChange.position
                                    showContextMenu = true
                                } else {
                                    connectionMode = null
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

                                if (connectionMode != null && clickedBlockIndex != -1) {
                                    val targetBlock = blocks.values.elementAt(clickedBlockIndex)
                                    val sourceId = connectionMode!!.sourceBlockId

                                    if (sourceId != targetBlock.id && !arrows.any {
                                            it.sourceBlockId == sourceId && it.targetBlockId == targetBlock.id
                                        }) {
                                        arrows.add(
                                            ArrowElement(
                                                sourceBlockId = sourceId,
                                                targetBlockId = targetBlock.id,
                                                style = ArrowStyle(
                                                    color = Color(0xFF42A5F5),
                                                    thickness = 2.5f,
                                                    arrowheadSize = 12f
                                                )
                                            )
                                        )
                                    }
                                    connectionMode = null
                                    downChange.consume()
                                    continue
                                }

                                if (clickedBlockIndex != -1) {
                                    val clickedBlock = blocks.values.elementAt(clickedBlockIndex)
                                    selectedBlockId = clickedBlock.id
                                    val cursorWorldPos = screenToWorld(downChange.position, camera, zoom)
                                    val offset = cursorWorldPos - clickedBlock.position
                                    dragState = DragState(offset)
                                    downChange.consume()

                                    while (true) {
                                        val moveEvent = awaitPointerEvent()
                                        val moveChange = moveEvent.changes.find { it.id == downChange.id }
                                        if (moveChange == null || !moveChange.pressed) break
                                        val cursorWorldPos = screenToWorld(moveChange.position, camera, zoom)
                                        val newPosition = cursorWorldPos - dragState!!.offset
                                        blocks[selectedBlockId!!] = blocks.getValue(selectedBlockId!!).copy(position = newPosition)
                                        moveChange.consume()
                                    }
                                    dragState = null
                                } else {
                                    selectedBlockId = null
                                    panState = PanState(camera, downChange.position)
                                    downChange.consume()

                                    while (true) {
                                        val moveEvent = awaitPointerEvent()
                                        val moveChange = moveEvent.changes.find { it.id == downChange.id }
                                        if (moveChange == null || !moveChange.pressed) break
                                        val delta = (moveChange.position - panState!!.startPosition) / zoom
                                        camera = panState!!.initialCamera - delta
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
        // Стрелки РИСУЮТСЯ ПОД блоками
        arrows.forEach { arrow ->
            val source = blocks[arrow.sourceBlockId]
            val target = blocks[arrow.targetBlockId]
            if (source != null && target != null) {
                // Вычисляем точки соединения по краям блоков (а не по центру)
                val sourceEdge = getEdgePoint(
                    source.position,
                    source.size,
                    target.position + Offset(target.size.width / 2f, target.size.height / 2f)
                )
                val targetEdge = getEdgePoint(
                    target.position,
                    target.size,
                    source.position + Offset(source.size.width / 2f, source.size.height / 2f)
                )

                ArrowComponent(
                    start = worldToScreen(sourceEdge, camera, zoom),
                    end = worldToScreen(targetEdge, camera, zoom),
                    style = arrow.style,
                    zoom = zoom
                )
            }
        }

        // Блоки поверх стрелок
        blocks.values.forEach { block ->
            BlockComponent(
                position = worldToScreen(block.position, camera, zoom),
                size = block.size * zoom,
                color = block.color,
                isSelected = block.id == selectedBlockId,
                content = block.content,
                zoom = zoom
            )
        }

        // Пунктирная линия в режиме соединения
        connectionMode?.let { mode ->
            blocks[mode.sourceBlockId]?.let { sourceBlock ->
                val sourceCenter = worldToScreen(
                    sourceBlock.position + Offset(sourceBlock.size.width / 2f, sourceBlock.size.height / 2f),
                    camera,
                    zoom
                )
                Canvas(Modifier.fillMaxSize()) {
                    drawLine(
                        color = Color(0x8042A5F5),
                        start = sourceCenter,
                        end = cursorPosition,
                        strokeWidth = 2.5f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateBlockDialog(
            initialWidth = 100f,
            initialHeight = 100f,
            initialColor = DefaultBlockColors[0],
            initialContent = null,
            onConfirm = { w, h, c, cont ->
                val newBlock = Block(
                    position = createPosition,
                    size = Size(w, h),
                    color = c,
                    content = cont
                )
                blocks[newBlock.id] = newBlock
                showCreateDialog = false
            },
            onCancel = { showCreateDialog = false }
        )
    }

    if (showEditDialog && blockToEdit != null) {
        CreateBlockDialog(
            initialWidth = blockToEdit!!.size.width,
            initialHeight = blockToEdit!!.size.height,
            initialColor = blockToEdit!!.color,
            initialContent = blockToEdit!!.content,
            onConfirm = { w, h, c, cont ->
                blocks[blockToEdit!!.id] = blockToEdit!!.copy(
                    size = Size(w, h),
                    color = c,
                    content = cont
                )
                showEditDialog = false
                blockToEdit = null
            },
            onCancel = {
                showEditDialog = false
                blockToEdit = null
            }
        )
    }

    if (showContextMenu && selectedBlockForContextMenu != null) {
        BlockContextMenu(
            position = contextMenuPosition,
            block = selectedBlockForContextMenu!!,
            arrows = arrows,
            onEdit = {
                blockToEdit = selectedBlockForContextMenu
                showEditDialog = true
                showContextMenu = false
            },
            onConnect = {
                connectionMode = ConnectionMode(selectedBlockForContextMenu!!.id)
                showContextMenu = false
            },
            onDisconnect = {
                blockToDisconnect = selectedBlockForContextMenu
                // Находим все стрелки, связанные с этим блоком
                connectionsToDisconnect = arrows.filter {
                    it.sourceBlockId == selectedBlockForContextMenu!!.id ||
                            it.targetBlockId == selectedBlockForContextMenu!!.id
                }
                showDisconnectDialog = true
                showContextMenu = false
            },
            onDelete = {
                selectedBlockForContextMenu?.let { b ->
                    blocks.remove(b.id)
                    arrows.removeAll { it.sourceBlockId == b.id || it.targetBlockId == b.id }
                }
                showContextMenu = false
            },
            onClose = { showContextMenu = false }
        )
    }

    // Диалог для разъединения блоков
    if (showDisconnectDialog && blockToDisconnect != null) {
        DisconnectDialog(
            block = blockToDisconnect!!,
            connections = connectionsToDisconnect,
            blocks = blocks,
            onDisconnect = { arrowId ->
                arrows.removeIf { it.id == arrowId }
            },
            onDisconnectAll = {
                val blockId = blockToDisconnect!!.id
                arrows.removeAll { it.sourceBlockId == blockId || it.targetBlockId == blockId }
                showDisconnectDialog = false
                blockToDisconnect = null
            },
            onCancel = {
                showDisconnectDialog = false
                blockToDisconnect = null
            }
        )
    }
}

// Вспомогательная функция для вычисления точки на краю блока, направленной к цели
private fun getEdgePoint(blockPos: Offset, blockSize: Size, targetPos: Offset): Offset {
    val blockCenter = blockPos + Offset(blockSize.width / 2f, blockSize.height / 2f)
    val dx = targetPos.x - blockCenter.x
    val dy = targetPos.y - blockCenter.y

    if (abs(dx) > abs(dy)) {
        // Горизонтальное направление
        val x = if (dx > 0) blockPos.x + blockSize.width else blockPos.x
        val y = blockCenter.y
        return Offset(x, y)
    } else {
        // Вертикальное направление
        val x = blockCenter.x
        val y = if (dy > 0) blockPos.y + blockSize.height else blockPos.y
        return Offset(x, y)
    }
}

@Composable
fun CreateBlockDialog(
    initialWidth: Float,
    initialHeight: Float,
    initialColor: Color,
    initialContent: Element?,
    onConfirm: (Float, Float, Color, Element?) -> Unit,
    onCancel: () -> Unit
) {
    var widthText by remember { mutableStateOf(initialWidth.toString()) }
    var heightText by remember { mutableStateOf(initialHeight.toString()) }
    var selectedColor by remember { mutableStateOf(initialColor) }
    val types = listOf(
        "TextElement", "IntElement", "DoubleElement", "ChoiceElement",
        "Block_", "BlockUnderText", "IntLimitElement", "DoubleLimitElement"
    )
    var type by remember { mutableStateOf("TextElement") }
    var expanded by remember { mutableStateOf(false) }
    var text by remember { mutableStateOf("") }
    var int by remember { mutableStateOf("0") }
    var dbl by remember { mutableStateOf("0.0") }
    var choices by remember { mutableStateOf("") }
    var onlyChoices by remember { mutableStateOf(false) }
    var intLimit by remember { mutableStateOf("0") }
    var dblLimit by remember { mutableStateOf("0.0") }
    var ranges by remember { mutableStateOf("") }

    LaunchedEffect(initialContent) {
        when (initialContent) {
            is TextElement -> {
                type = "TextElement"
                text = initialContent.text
            }
            is IntElement -> {
                if (initialContent is IntLimitElement) {
                    type = "IntLimitElement"
                    intLimit = initialContent.int.toString()
                    ranges = initialContent.limit.joinToString(", ") { "[${it.start}..${it.endInclusive}]" }
                } else {
                    type = "IntElement"
                    int = initialContent.int.toString()
                }
            }
            is DoubleElement -> {
                if (initialContent is DoubleLimitElement) {
                    type = "DoubleLimitElement"
                    dblLimit = initialContent.double.toString()
                    ranges = initialContent.limit.joinToString(", ") { "[${it.from}..${it.to}]" }
                } else {
                    type = "DoubleElement"
                    dbl = initialContent.double.toString()
                }
            }
            is ChoiceElement -> {
                type = "ChoiceElement"
                choices = initialContent.text
                onlyChoices = initialContent.isOnlyChoices
            }
            is Block_ -> {
                if (initialContent is BlockUnderText) {
                    type = "BlockUnderText"
                } else {
                    type = "Block_"
                }
                text = initialContent.text
            }
            else -> {
                type = "TextElement"
                text = ""
            }
        }
    }

    val w = widthText.toFloatOrNull() ?: 100f
    val h = heightText.toFloatOrNull() ?: 100f
    val valid = w in 10f..5000f && h in 10f..5000f

    val content: Element? = when (type) {
        "TextElement" -> TextElement(text)
        "IntElement" -> IntElement(int.toIntOrNull() ?: 0)
        "DoubleElement" -> DoubleElement(dbl.toDoubleOrNull() ?: 0.0)
        "ChoiceElement" -> ChoiceElement(
            choices,
            choices.split(",").filter { it.isNotBlank() },
            onlyChoices
        )
        "Block_" -> Block_(text, emptyMap())
        "BlockUnderText" -> BlockUnderText(text, emptyMap())
        "IntLimitElement" -> {
            val limits = ranges.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { rangeStr ->
                    try {
                        val clean = rangeStr.trim('[', ']', ' ')
                        val parts = clean.split("..").map { it.trim() }
                        if (parts.size == 2) {
                            val start = parts[0].toIntOrNull() ?: Int.MIN_VALUE
                            val end = parts[1].toIntOrNull() ?: Int.MAX_VALUE
                            start..end
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }.ifEmpty { listOf(Int.MIN_VALUE..Int.MAX_VALUE) }
            IntLimitElement(intLimit.toIntOrNull() ?: 0, limits)
        }
        "DoubleLimitElement" -> {
            val limits = ranges.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .mapNotNull { rangeStr ->
                    try {
                        val clean = rangeStr.trim('[', ']', ' ')
                        val parts = clean.split("..").map { it.trim() }
                        if (parts.size == 2) {
                            val start = parts[0].toDoubleOrNull() ?: Double.MIN_VALUE
                            val end = parts[1].toDoubleOrNull() ?: Double.MAX_VALUE
                            DoubleRange(start, end)
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        null
                    }
                }.ifEmpty { listOf(DoubleRange(Double.MIN_VALUE, Double.MAX_VALUE)) }
            DoubleLimitElement(dblLimit.toDoubleOrNull() ?: 0.0, limits)
        }
        else -> TextElement(text)
    }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .width(320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (initialContent == null) "Создать новый блок" else "Редактировать блок",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                )

                OutlinedTextField(
                    value = widthText,
                    onValueChange = { widthText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Ширина (10-5000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = w !in 10f..5000f
                )

                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Высота (10-5000)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = h !in 10f..5000f
                )

                Box {
                    Button(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                            Text(
                                text = "▼",
                                modifier = Modifier.padding(start = 8.dp),
                                fontSize = 18.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(260.dp)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = t,
                                        fontSize = 15.sp,
                                        color = if (t == type) Color(0xFF2196F3) else Color.Black,
                                        fontWeight = if (t == type) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    type = t
                                    expanded = false
                                },
                                modifier = Modifier.height(48.dp)
                            )
                        }
                    }
                }

                when (type) {
                    "TextElement", "Block_", "BlockUnderText" -> {
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            label = { Text("Текст") },
                            singleLine = true
                        )
                    }
                    "IntElement" -> {
                        OutlinedTextField(
                            value = int,
                            onValueChange = { int = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("Целое число") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                    "DoubleElement" -> {
                        OutlinedTextField(
                            value = dbl,
                            onValueChange = { dbl = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                            label = { Text("Дробное число") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                    "ChoiceElement" -> {
                        OutlinedTextField(
                            value = choices,
                            onValueChange = { choices = it },
                            label = { Text("Варианты (через запятую)") },
                            singleLine = true
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = onlyChoices,
                                onCheckedChange = { onlyChoices = it }
                            )
                            Text("Только выбор из вариантов")
                        }
                    }
                    "IntLimitElement" -> {
                        OutlinedTextField(
                            value = intLimit,
                            onValueChange = { intLimit = it.filter { c -> c.isDigit() || c == '-' } },
                            label = { Text("Целое число") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ranges,
                            onValueChange = { ranges = it },
                            label = { Text("Ограничения [0..100], [200..300]") },
                            singleLine = false,
                            maxLines = 2
                        )
                    }
                    "DoubleLimitElement" -> {
                        OutlinedTextField(
                            value = dblLimit,
                            onValueChange = { dblLimit = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                            label = { Text("Дробное число") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = ranges,
                            onValueChange = { ranges = it },
                            label = { Text("Ограничения [0.0..100.0], [200.0..300.0]") },
                            singleLine = false,
                            maxLines = 2
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Цвет блока:", style = MaterialTheme.typography.bodyLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    repeat(2) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(4) { col ->
                                val idx = row * 4 + col
                                val color = DefaultBlockColors[idx]
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .border(
                                            width = if (color == selectedColor) 3.dp else 2.dp,
                                            color = if (color == selectedColor) Color.White else Color.Gray,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(color, RoundedCornerShape(8.dp))
                                        .clickable { selectedColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (color == selectedColor) {
                                        Canvas(modifier = Modifier.size(20.dp)) {
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(5f, 10f),
                                                end = Offset(9f, 14f),
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(9f, 14f),
                                                end = Offset(15f, 6f),
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium)
                    }
                    Button(
                        onClick = { onConfirm(w, h, selectedColor, content) },
                        enabled = valid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text(
                            text = if (initialContent == null) "Создать" else "Сохранить",
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlockContextMenu(
    position: Offset,
    block: Block,
    arrows: List<ArrowElement>,
    onEdit: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    // Проверяем, есть ли у блока соединения для показа пункта "Разъединить"
    val hasConnections = arrows.any { it.sourceBlockId == block.id || it.targetBlockId == block.id }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clickable { onClose() }
        ) {
            Text(text = "×", fontSize = 24.sp, color = Color.Gray, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = position.x
                    translationY = position.y
                }
                .background(Color.White)
                .border(2.dp, Color(0xFF42A5F5), RoundedCornerShape(12.dp))  // Голубая рамка для выделения
                .padding(12.dp)
                .width(240.dp)
                .shadow(elevation = 8.dp)  // Тень для глубины
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Заголовок меню
                Text(
                    text = "Действия с блоком",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Кнопка Редактировать
                MenuItem(
                    text = "✏️ Редактировать",
                    backgroundColor = Color(0xFFF5F5F5),
                    onClick = { onEdit(); onClose() }
                )

                // Кнопка Объединить
                MenuItem(
                    text = "🔗 Объединить",
                    backgroundColor = Color(0xFFE3F2FD),
                    textColor = Color(0xFF1976D2),
                    onClick = { onConnect(); onClose() }
                )

                // Кнопка Разъединить (показывается только если есть соединения)
                if (hasConnections) {
                    MenuItem(
                        text = "✂️ Разъединить",
                        backgroundColor = Color(0xFFF3E5F5),
                        textColor = Color(0xFF6A1B9A),
                        onClick = { onDisconnect(); onClose() }
                    )
                }

                // Кнопка Удалить
                MenuItem(
                    text = "🗑️ Удалить",
                    backgroundColor = Color(0xFFFFE5E5),
                    textColor = Color(0xFFC62828),
                    onClick = { onDelete(); onClose() }
                )
            }
        }
    }
}

@Composable
private fun MenuItem(
    text: String,
    backgroundColor: Color,
    textColor: Color = Color.Black,
    onClick: () -> Unit
) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 16.dp)
            .background(backgroundColor, RoundedCornerShape(8.dp)),
        fontSize = 16.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        color = textColor
    )
}

@Composable
fun DisconnectDialog(
    block: Block,
    connections: List<ArrowElement>,
    blocks: Map<String, Block>,
    onDisconnect: (String) -> Unit,
    onDisconnectAll: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 10.dp,
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Разъединить блоки",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )

                Text(
                    text = "Выберите соединения для удаления:",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Список соединений
                connections.forEach { arrow ->
                    val otherBlock = if (arrow.sourceBlockId == block.id) {
                        blocks[arrow.targetBlockId]
                    } else {
                        blocks[arrow.sourceBlockId]
                    }

                    if (otherBlock != null) {
                        val direction = if (arrow.sourceBlockId == block.id) "→" else "←"
                        val otherText = when (otherBlock.content) {
                            is TextElement -> (otherBlock.content as TextElement).text
                            is IntElement -> (otherBlock.content as IntElement).int.toString()
                            is DoubleElement -> (otherBlock.content as DoubleElement).double.toString()
                            else -> "Блок"
                        }.take(20).let { if (it.length == 20) "$it..." else it }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDisconnect(arrow.id) }
                                .padding(12.dp)
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(10.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color(0xFF6A1B9A), RoundedCornerShape(4.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("×", color = Color.White, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }

                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(
                                    text = "$direction $otherText",
                                    fontSize = 16.sp,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                )
                                Text(
                                    text = "ID: ${otherBlock.id.take(8)}...",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                if (connections.isEmpty()) {
                    Text(
                        text = "Нет соединений для разъединения",
                        color = Color.Gray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }

                // Кнопки действий
                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }

                    if (connections.isNotEmpty()) {
                        Button(
                            onClick = onDisconnectAll,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            modifier = Modifier.width(140.dp)
                        ) {
                            Text("Разъединить все", fontSize = 16.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun worldToScreen(world: Offset, camera: Offset, zoom: Float): Offset = (world - camera) * zoom
private fun screenToWorld(screen: Offset, camera: Offset, zoom: Float): Offset = screen / zoom + camera
private fun isInside(point: Offset, rect: Offset, size: Size): Boolean =
    point.x >= rect.x && point.x <= rect.x + size.width &&
            point.y >= rect.y && point.y <= rect.y + size.height