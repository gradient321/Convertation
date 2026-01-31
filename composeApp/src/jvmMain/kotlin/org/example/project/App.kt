package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import java.util.UUID
import kotlin.math.*

// ===== ИСПРАВЛЕННЫЕ ЭЛЕМЕНТЫ (КРИТИЧЕСКИ ВАЖНО!) =====
class Data

// ✅ ПРАВИЛЬНОЕ ОБЪЯВЛЕНИЕ СВОЙСТВА "data" (имя + двоеточие + тип + инициализация)
abstract class Element {
    var data: Data? = null
}

open class TextElement(var text: String = "") : Element()
abstract class NumberElement : Element() { open var number: Number = 0.0 }

// ✅ ПРАВИЛЬНЫЙ СИНТАКСИС ГЕТТЕРА/СЕТТЕРА (с отступами!)
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
open class IntLimitElement(int: Int = 0, var limit: List<IntRange> = listOf(Int.MIN_VALUE..Int.MAX_VALUE)) : IntElement(int)

data class DoubleRange(val from: Double, val to: Double)
fun Double.rangeTo(to: Double): DoubleRange = DoubleRange(this, to)

open class DoubleLimitElement(
    double: Double = 0.0,
    var limit: List<DoubleRange> = listOf(DoubleRange(Double.MIN_VALUE, Double.MAX_VALUE))
) : DoubleElement(double)

// ===== ЭЛЕМЕНТЫ ПОТОКА УПРАВЛЕНИЯ =====
data class PrintElement(
    var text: String = "\"Привет\"",
    var newLine: Boolean = true
) : Element()

data class VariableElement(
    var name: String = "age",
    var type: String = "Int",
    var value: String = "0"
) : Element()

data class IfElement(
    var condition: String = "age > 18",
    var hasElse: Boolean = false,
    var elseIfCount: Int = 0
) : Element()

data class ReturnElement(
    var value: String = ""
) : Element()

// ===== СТРЕЛКИ И СОЕДИНЕНИЯ =====
data class ArrowStyle(
    val color: Color = Color(0xFF42A5F5),
    val thickness: Float = 2.5f,
    val arrowheadSize: Float = 12f
)

data class ArrowElement(
    val id: String = UUID.randomUUID().toString(),
    val sourceBlockId: String,
    val targetBlockId: String,
    val style: ArrowStyle = ArrowStyle()
)

// Соединение "продолжение" (последовательность выполнения)
data class Continuation(
    val id: String = UUID.randomUUID().toString(),
    val sourceBlockId: String,
    val targetBlockId: String
)

// ===== ГЛОБАЛЬНЫЕ КОНСТАНТЫ (ТОЛЬКО ОДИН РАЗ!) =====
private val BackgroundColor = Color(0xFF1E1E1E)
private val DefaultBlockColors = listOf(
    Color(0xFF4A148C), Color(0xFF0288D1), Color(0xFF2E7D32), Color(0xFFC62828),
    Color(0xFF5D4037), Color(0xFF6A1B9A), Color(0xFFFFA000), Color(0xFF37474F)
)
private val SelectionBorderColor = Color.White
private const val BorderWidth = 2f
private val ContinuationButtonColor = Color(0xFF2196F3) // Синяя кнопка продолжения

// ===== МОДЕЛИ ДАННЫХ =====
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset = Offset.Zero,
    val size: Size = Size(140f, 60f),
    val color: Color = DefaultBlockColors[1],
    val isSelected: Boolean = false,
    val content: Element? = null,
    val isConditionBlock: Boolean = false
)

private data class DragState(val offset: Offset)
private data class PanState(val initialCamera: Offset, val startPosition: Offset)
private data class ConnectionMode(val sourceBlockId: String)
private data class ContinuationMode(val sourceBlockId: String)

// ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====
private fun getEdgePoint(blockPos: Offset, blockSize: Size, targetPos: Offset): Offset {
    val blockCenter = blockPos + Offset(blockSize.width / 2f, blockSize.height / 2f)
    val dx = targetPos.x - blockCenter.x
    val dy = targetPos.y - blockCenter.y
    return if (abs(dx) > abs(dy)) {
        val x = if (dx > 0) blockPos.x + blockSize.width else blockPos.x
        Offset(x, blockCenter.y)
    } else {
        val y = if (dy > 0) blockPos.y + blockSize.height else blockPos.y
        Offset(blockCenter.x, y)
    }
}

private fun worldToScreen(world: Offset, camera: Offset, zoom: Float): Offset = (world - camera) * zoom
private fun screenToWorld(screen: Offset, camera: Offset, zoom: Float): Offset = screen / zoom + camera
private fun isInside(point: Offset, rect: Offset, size: Size): Boolean =
    point.x >= rect.x && point.x <= rect.x + size.width &&
            point.y >= rect.y && point.y <= rect.y + size.height

// ===== КОМПОНЕНТЫ =====
@Composable
fun BlockComponent(
    position: Offset,
    size: Size,
    color: Color,
    isSelected: Boolean,
    content: Element?,
    zoom: Float,
    isConditionBlock: Boolean = false
) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(size.width.dp, size.height.dp)
            .background(if (isConditionBlock) Color(0xFFFFF9C4) else color)
            .border(
                width = if (isSelected) BorderWidth.dp else 0.dp,
                color = SelectionBorderColor,
                shape = if (isConditionBlock) RoundedCornerShape(12.dp) else RoundedCornerShape(8.dp)
            )
            .shadow(elevation = if (isSelected) 8.dp else 4.dp)
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
                is PrintElement -> "🖨️ ${content.text}${if (content.newLine) " +\\n" else ""}"
                is VariableElement -> "📝 ${content.name}: ${content.type}${if (content.value.isNotEmpty()) " = ${content.value}" else ""}"
                is IfElement -> "❓ if (${content.condition})"
                is ReturnElement -> "↩️ return${if (content.value.isNotEmpty()) " ${content.value}" else ""}"
                else -> "Unknown"
            }
            val baseFontSizePx = size.height * 0.35f
            val minFontSizePx = 8f
            val fontSize = baseFontSizePx.coerceAtLeast(minFontSizePx).sp
            Text(
                text = text,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                style = TextStyle(
                    color = Color.Black,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                softWrap = true,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ContinuationButton(position: Offset, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(28.dp)
            .background(ContinuationButtonColor, CircleShape)
            .border(2.dp, Color.White, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text("➕", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ArrowComponent(start: Offset, end: Offset, style: ArrowStyle, zoom: Float) {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.98f }) {
        drawLine(
            color = style.color,
            start = start,
            end = end,
            strokeWidth = style.thickness * zoom.coerceAtLeast(0.6f),
            cap = StrokeCap.Round
        )

        if (zoom > 0.4f) {
            val arrowheadSize = style.arrowheadSize * zoom.coerceAtMost(1.5f)
            val angle = atan2(end.y - start.y, end.x - start.x)
            val arrowSideAngle = PI / 6

            val p1 = Offset(
                end.x - arrowheadSize * cos(angle - arrowSideAngle).toFloat(),
                end.y - arrowheadSize * sin(angle - arrowSideAngle).toFloat()
            )
            val p2 = Offset(
                end.x - arrowheadSize * cos(angle + arrowSideAngle).toFloat(),
                end.y - arrowheadSize * sin(angle + arrowSideAngle).toFloat()
            )

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

@Composable
fun MenuItemButton(
    icon: String,
    text: String,
    backgroundColor: Color,
    iconColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable { onClick() }
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 20.sp, color = iconColor, fontWeight = FontWeight.Bold)
            }
            Text(
                text = text,
                modifier = Modifier.padding(start = 16.dp),
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF212121)
            )
        }
    }
}

@Composable
fun BlockContextMenu(
    position: Offset,
    block: Block,
    arrows: List<ArrowElement>,
    continuations: List<Continuation>,
    onEdit: () -> Unit,
    onConnect: () -> Unit,
    onContinue: () -> Unit,
    onDisconnect: () -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    val hasConnections = arrows.any { it.sourceBlockId == block.id || it.targetBlockId == block.id }
    val hasContinuations = continuations.any { it.sourceBlockId == block.id }
    val isIfBlock = block.content is IfElement

    Box(modifier = Modifier.fillMaxSize().clickable { onClose() }) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clickable { onClose() }
                .size(36.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(18.dp))
                .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "×", fontSize = 28.sp, color = Color(0xFF616161), fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = position.x
                    translationY = position.y
                }
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, if (isIfBlock) Color(0xFFFFA726) else Color(0xFF1976D2), RoundedCornerShape(16.dp))
                .shadow(elevation = 12.dp)
                .padding(vertical = 16.dp)
                .width(280.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier.size(32.dp)
                            .background(if (isIfBlock) Color(0xFFFFA726) else Color(0xFF1976D2), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(if (isIfBlock) "❓" else "⋮", fontSize = 20.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = if (isIfBlock) "Условие" else "Действия с блоком",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isIfBlock) Color(0xFFEF6C00) else Color(0xFF1976D2),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                if (!hasContinuations) {
                    MenuItemButton(
                        icon = "⏬",
                        text = "Продолжение",
                        backgroundColor = Color(0xFFE8F5E9),
                        iconColor = Color(0xFF2E7D32),
                        onClick = { onContinue(); onClose() }
                    )
                }

                MenuItemButton(
                    icon = "✏️",
                    text = "Редактировать",
                    backgroundColor = Color(0xFFF5F5F5),
                    iconColor = Color(0xFF2196F3),
                    onClick = { onEdit(); onClose() }
                )

                MenuItemButton(
                    icon = "🔗",
                    text = "Объединить",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1976D2),
                    onClick = { onConnect(); onClose() }
                )

                if (hasConnections) {
                    MenuItemButton(
                        icon = "✂️",
                        text = "Разъединить",
                        backgroundColor = Color(0xFFF3E5F5),
                        iconColor = Color(0xFF6A1B9A),
                        onClick = { onDisconnect(); onClose() }
                    )
                }

                MenuItemButton(
                    icon = "🗑️",
                    text = "Удалить блок",
                    backgroundColor = Color(0xFFFFE5E5),
                    iconColor = Color(0xFFC62828),
                    onClick = { onDelete(); onClose() }
                )
            }
        }
    }
}

@Composable
fun CreateContinuationDialog(onSelect: (String) -> Unit, onCancel: () -> Unit) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(20.dp), tonalElevation = 14.dp, modifier = Modifier.width(320.dp)) {
            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Выберите действие",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                ContinuationItem(icon = "🖨️", text = "Принт", color = Color(0xFF42A5F5)) { onSelect("print") }
                ContinuationItem(icon = "📝", text = "Переменная", color = Color(0xFF66BB6A)) { onSelect("variable") }
                ContinuationItem(icon = "❓", text = "Если", color = Color(0xFFFFA726)) { onSelect("if") }
                ContinuationItem(icon = "↩️", text = "Ретюрн", color = Color(0xFFAB47BC)) { onSelect("return") }
                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Отмена", fontSize = 17.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun ContinuationItem(icon: String, text: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable { onClick() }
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .border(2.dp, color, RoundedCornerShape(16.dp))
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(color, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 22.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
            Text(
                text = text,
                modifier = Modifier.padding(start = 16.dp),
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = color
            )
        }
    }
}

@Composable
fun CreatePrintDialog(
    initialText: String = "\"Привет\"",
    initialNewLine: Boolean = true,
    onConfirm: (PrintElement) -> Unit,
    onCancel: () -> Unit
) {
    var text by remember { mutableStateOf(initialText) }
    var newLine by remember { mutableStateOf(initialNewLine) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(text = "🖨️ Принт", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Что выводить") },
                    singleLine = false,
                    maxLines = 3
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                    Checkbox(checked = newLine, onCheckedChange = { newLine = it })
                    Text("Перенос строки", fontSize = 16.sp)
                }
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = { onConfirm(PrintElement(text, newLine)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A5F5)),
                        modifier = Modifier.width(120.dp)
                    ) { Text("Добавить") }
                }
            }
        }
    }
}

@Composable
fun CreateVariableDialog(
    initialName: String = "age",
    initialType: String = "Int",
    initialValue: String = "0",
    onConfirm: (VariableElement) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var type by remember { mutableStateOf(initialType) }
    var value by remember { mutableStateOf(initialValue) }
    val types = listOf("String", "Int", "Double", "Boolean")
    var expanded by remember { mutableStateOf(false) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(text = "📝 Переменная", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.filter { it.isLetterOrDigit() || it == '_' } },
                    label = { Text("Имя") },
                    singleLine = true
                )
                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Тип") },
                        trailingIcon = { Text("▼", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(unfocusedTrailingIconColor = Color.Gray)
                    )
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(150.dp)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, fontSize = 16.sp) },
                                onClick = { type = t; expanded = false },
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Значение (опционально)") },
                    singleLine = true
                )
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = { onConfirm(VariableElement(name, type, value)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF66BB6A)),
                        modifier = Modifier.width(120.dp)
                    ) { Text("Добавить") }
                }
            }
        }
    }
}

@Composable
fun CreateIfDialog(
    initialCondition: String = "age > 18",
    onConfirm: (IfElement) -> Unit,
    onCancel: () -> Unit
) {
    var condition by remember { mutableStateOf(initialCondition) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(text = "❓ Условие", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFFFA726))
                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Условие") },
                    singleLine = true,
                    placeholder = { Text("age > 18") }
                )
                Text("Подсказка: используйте операторы >, <, ==, &&, ||", fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = { onConfirm(IfElement(condition)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA726)),
                        modifier = Modifier.width(120.dp)
                    ) { Text("Добавить") }
                }
            }
        }
    }
}

@Composable
fun CreateReturnDialog(
    initialValue: String = "",
    onConfirm: (ReturnElement) -> Unit,
    onCancel: () -> Unit
) {
    var value by remember { mutableStateOf(initialValue) }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(modifier = Modifier.padding(24.dp).width(360.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text(text = "↩️ Ретюрн", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFAB47BC))
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Возвращаемое значение (опционально)") },
                    singleLine = true,
                    placeholder = { Text("result или оставьте пустым") }
                )
                Row(
                    modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) { Text("Отмена") }
                    Button(
                        onClick = { onConfirm(ReturnElement(value)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFAB47BC)),
                        modifier = Modifier.width(120.dp)
                    ) { Text("Добавить") }
                }
            }
        }
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
            is TextElement -> { type = "TextElement"; text = initialContent.text }
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
            is ChoiceElement -> { type = "ChoiceElement"; choices = initialContent.text; onlyChoices = initialContent.isOnlyChoices }
            is Block_ -> {
                if (initialContent is BlockUnderText) "BlockUnderText" else "Block_"
                text = initialContent.text
            }
            else -> { type = "TextElement"; text = "" }
        }
    }

    val w = widthText.toFloatOrNull() ?: 100f
    val h = heightText.toFloatOrNull() ?: 100f
    val valid = w in 10f..5000f && h in 10f..5000f

    val content: Element? = when (type) {
        "TextElement" -> TextElement(text)
        "IntElement" -> IntElement(int.toIntOrNull() ?: 0)
        "DoubleElement" -> DoubleElement(dbl.toDoubleOrNull() ?: 0.0)
        "ChoiceElement" -> ChoiceElement(choices, choices.split(",").filter { it.isNotBlank() }, onlyChoices)
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
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(360.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = if (initialContent == null) "Создать новый блок" else "Редактировать блок",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2)
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
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8EAF6))
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = type, fontSize = 16.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1976D2))
                            Text(
                                text = "▼",
                                modifier = Modifier.padding(start = 8.dp),
                                fontSize = 18.sp,
                                color = Color(0xFF546E7A)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(280.dp)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = t,
                                        fontSize = 16.sp,
                                        color = if (t == type) Color(0xFF1976D2) else Color(0xFF424242),
                                        fontWeight = if (t == type) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                onClick = {
                                    type = t
                                    expanded = false
                                },
                                modifier = Modifier.height(52.dp)
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

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Цвет блока:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    repeat(2) { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            repeat(4) { col ->
                                val idx = row * 4 + col
                                val color = DefaultBlockColors[idx]
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .border(
                                            width = if (color == selectedColor) 3.dp else 2.dp,
                                            color = if (color == selectedColor) Color.White else Color.Gray,
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                        .background(color, RoundedCornerShape(10.dp))
                                        .clickable { selectedColor = color },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (color == selectedColor) {
                                        Canvas(modifier = Modifier.size(24.dp)) {
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(6f, 12f),
                                                end = Offset(11f, 17f),
                                                strokeWidth = 3f,
                                                cap = StrokeCap.Round
                                            )
                                            drawLine(
                                                color = Color.White,
                                                start = Offset(11f, 17f),
                                                end = Offset(18f, 8f),
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
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(110.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    }
                    Button(
                        onClick = { onConfirm(w, h, selectedColor, content) },
                        enabled = valid,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.width(130.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (initialContent == null) "Создать" else "Сохранить",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ===== СЕРИАЛИЗАЦИЯ В СПИСОК ФУНКЦИЙ =====
data class FunctionBlock(
    val name: String,
    val actions: List<Element>,
    val position: Offset
)

fun blocksToFunctions(blocks: Map<String, Block>, continuations: List<Continuation>): List<FunctionBlock> {
    val result = mutableListOf<FunctionBlock>()
    val visited = mutableSetOf<String>()

    // Находим все начальные блоки (без входящих продолжений)
    val allTargetIds = continuations.map { it.targetBlockId }.toSet()
    val startBlocks = blocks.values.filter { it.id !in allTargetIds }

    for (startBlock in startBlocks) {
        val actions = mutableListOf<Element>()
        var currentId = startBlock.id
        visited.add(currentId)

        // Собираем цепочку продолжений
        while (true) {
            val block = blocks[currentId] ?: break
            if (block.content != null) {
                actions.add(block.content)
            }

            // Находим следующий блок в цепочке
            val next = continuations.find { it.sourceBlockId == currentId }
            if (next != null && !visited.contains(next.targetBlockId)) {
                currentId = next.targetBlockId
                visited.add(currentId)
            } else {
                break
            }
        }

        result.add(FunctionBlock(
            name = "function_${startBlock.id.take(8)}",
            actions = actions,
            position = startBlock.position
        ))
    }

    return result
}

fun functionsToBlocks(functions: List<FunctionBlock>): Pair<Map<String, Block>, List<Continuation>> {
    val blocks = mutableMapOf<String, Block>()
    val continuations = mutableListOf<Continuation>()

    for (func in functions) {
        var prevBlockId: String? = null

        for ((index, action) in func.actions.withIndex()) {
            val blockId = UUID.randomUUID().toString()
            val yPos = func.position.y + index * 80f
            val block = Block(
                id = blockId,
                position = Offset(func.position.x, yPos),
                size = Size(140f, 60f),
                color = when (action) {
                    is PrintElement -> Color(0xFFB3E5FC)
                    is VariableElement -> Color(0xFFC8E6C9)
                    is IfElement -> Color(0xFFFFF9C4)
                    is ReturnElement -> Color(0xFFE1BEE7)
                    else -> DefaultBlockColors[1]
                },
                content = action,
                isConditionBlock = action is IfElement
            )
            blocks[blockId] = block

            // Создаём соединение продолжения
            if (prevBlockId != null) {
                continuations.add(Continuation(sourceBlockId = prevBlockId, targetBlockId = blockId))
            }
            prevBlockId = blockId
        }
    }

    return blocks to continuations
}

// ===== ОСНОВНОЙ РЕДАКТОР =====
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateMapOf<String, Block>() }
    val arrows = remember { mutableStateListOf<ArrowElement>() }
    val continuations = remember { mutableStateListOf<Continuation>() }
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var createPosition by remember { mutableStateOf(Offset.Zero) }
    var connectionMode by remember { mutableStateOf<ConnectionMode?>(null) }
    var continuationMode by remember { mutableStateOf<ContinuationMode?>(null) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var selectedBlockForContextMenu by remember { mutableStateOf<Block?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<Block?>(null) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var panState by remember { mutableStateOf<PanState?>(null) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }
    var showDisconnectDialog by remember { mutableStateOf(false) }
    var blockToDisconnect by remember { mutableStateOf<Block?>(null) }
    var connectionsToDisconnect by remember { mutableStateOf<List<ArrowElement>>(emptyList()) }
    var showContinuationDialog by remember { mutableStateOf(false) }
    var showPrintDialog by remember { mutableStateOf(false) }
    var showVariableDialog by remember { mutableStateOf(false) }
    var showIfDialog by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var continuationSourceId by remember { mutableStateOf<String?>(null) }

    // Кнопка для экспорта/импорта функций
    var showFunctionsDialog by remember { mutableStateOf(false) }
    var functionsText by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                if (showCreateDialog || showEditDialog || showContextMenu || showDisconnectDialog ||
                    showContinuationDialog || showPrintDialog || showVariableDialog || showIfDialog ||
                    showReturnDialog || showFunctionsDialog) return@onPointerEvent
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
                                    continuationMode = null
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
        // Стрелки
        arrows.forEach { arrow ->
            val source = blocks[arrow.sourceBlockId]
            val target = blocks[arrow.targetBlockId]
            if (source != null && target != null) {
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

        // Блоки
        blocks.values.forEach { block ->
            BlockComponent(
                position = worldToScreen(block.position, camera, zoom),
                size = block.size * zoom,
                color = block.color,
                isSelected = block.id == selectedBlockId,
                content = block.content,
                zoom = zoom,
                isConditionBlock = block.isConditionBlock
            )

            // Кнопка продолжения под блоком (если нет продолжений от этого блока)
            if (continuations.none { it.sourceBlockId == block.id }) {
                val buttonPos = worldToScreen(
                    block.position + Offset(block.size.width / 2f - 14f, block.size.height + 8f),
                    camera,
                    zoom
                )
                ContinuationButton(position = buttonPos) {
                    continuationMode = ContinuationMode(block.id)
                    showContinuationDialog = true
                }
            }
        }

        // Визуальная подсказка в режиме соединения
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

    // Верхняя панель с кнопками экспорта/импорта
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFF263238))
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val functions = blocksToFunctions(blocks, continuations)
                    functionsText = functions.joinToString("\n\n") { func ->
                        "Функция: ${func.name}\n${func.actions.joinToString("\n") { "  - $it" }}"
                    }
                    showFunctionsDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("📤 Экспорт в функции")
            }
            Button(
                onClick = {
                    // Пример импорта (в реальном приложении парсить текст)
                    val exampleFunctions = listOf(
                        FunctionBlock(
                            name = "main",
                            actions = listOf(
                                PrintElement("\"Начало программы\"", true),
                                VariableElement("x", "Int", "5"),
                                IfElement("x > 3"),
                                PrintElement("\"x больше 3\"", true),
                                ReturnElement("x")
                            ),
                            position = Offset(100f, 100f)
                        )
                    )
                    val (newBlocks, newContinuations) = functionsToBlocks(exampleFunctions)
                    blocks.clear()
                    blocks.putAll(newBlocks)
                    continuations.clear()
                    continuations.addAll(newContinuations)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
            ) {
                Text("📥 Импорт из функций")
            }
        }
    }

    // Диалоги
    if (showCreateDialog) {
        CreateBlockDialog(
            initialWidth = 140f,
            initialHeight = 60f,
            initialColor = DefaultBlockColors[1],
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
            continuations = continuations,
            onEdit = {
                blockToEdit = selectedBlockForContextMenu
                showEditDialog = true
                showContextMenu = false
            },
            onConnect = {
                connectionMode = ConnectionMode(selectedBlockForContextMenu!!.id)
                showContextMenu = false
            },
            onContinue = {
                continuationMode = ContinuationMode(selectedBlockForContextMenu!!.id)
                showContinuationDialog = true
                showContextMenu = false
            },
            onDisconnect = {
                blockToDisconnect = selectedBlockForContextMenu
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
                    continuations.removeAll { it.sourceBlockId == b.id || it.targetBlockId == b.id }
                    // Удаляем все дочерние блоки продолжений
                    val children = continuations.filter { it.sourceBlockId == b.id }.map { it.targetBlockId }
                    children.forEach { childId ->
                        blocks.remove(childId)
                        continuations.removeAll { it.sourceBlockId == childId || it.targetBlockId == childId }
                    }
                    continuations.removeAll { it.sourceBlockId == b.id }
                }
                showContextMenu = false
            },
            onClose = { showContextMenu = false }
        )
    }

    if (showDisconnectDialog && blockToDisconnect != null) {
        Dialog(onDismissRequest = { showDisconnectDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 6.dp) {
                Column(modifier = Modifier.padding(24.dp).width(300.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Разъединить блок?", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Это удалит все соединения с этим блоком.", color = Color.Gray)
                    Row(modifier = Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Button(
                            onClick = { showDisconnectDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) { Text("Отмена") }
                        Button(
                            onClick = {
                                val blockId = blockToDisconnect!!.id
                                arrows.removeAll { it.sourceBlockId == blockId || it.targetBlockId == blockId }
                                showDisconnectDialog = false
                                blockToDisconnect = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828))
                        ) { Text("Разъединить") }
                    }
                }
            }
        }
    }

    if (showContinuationDialog && continuationMode != null) {
        CreateContinuationDialog(
            onSelect = { elementType ->
                continuationSourceId = continuationMode!!.sourceBlockId
                when (elementType) {
                    "print" -> showPrintDialog = true
                    "variable" -> showVariableDialog = true
                    "if" -> showIfDialog = true
                    "return" -> showReturnDialog = true
                }
                showContinuationDialog = false
                continuationMode = null
            },
            onCancel = {
                showContinuationDialog = false
                continuationMode = null
            }
        )
    }

    if (showPrintDialog) {
        CreatePrintDialog(
            onConfirm = { element ->
                val sourceBlock = blocks[continuationSourceId!!]
                if (sourceBlock != null) {
                    val newPos = Offset(sourceBlock.position.x, sourceBlock.position.y + sourceBlock.size.height + 40f)
                    val newBlock = Block(
                        position = newPos,
                        size = Size(140f, 60f),
                        color = Color(0xFFB3E5FC),
                        content = element
                    )
                    blocks[newBlock.id] = newBlock
                    continuations.add(Continuation(sourceBlockId = continuationSourceId!!, targetBlockId = newBlock.id))
                }
                showPrintDialog = false
                continuationSourceId = null
            },
            onCancel = { showPrintDialog = false; continuationSourceId = null }
        )
    }

    if (showVariableDialog) {
        CreateVariableDialog(
            onConfirm = { element ->
                val sourceBlock = blocks[continuationSourceId!!]
                if (sourceBlock != null) {
                    val newPos = Offset(sourceBlock.position.x, sourceBlock.position.y + sourceBlock.size.height + 40f)
                    val newBlock = Block(
                        position = newPos,
                        size = Size(140f, 60f),
                        color = Color(0xFFC8E6C9),
                        content = element
                    )
                    blocks[newBlock.id] = newBlock
                    continuations.add(Continuation(sourceBlockId = continuationSourceId!!, targetBlockId = newBlock.id))
                }
                showVariableDialog = false
                continuationSourceId = null
            },
            onCancel = { showVariableDialog = false; continuationSourceId = null }
        )
    }

    if (showIfDialog) {
        CreateIfDialog(
            onConfirm = { element ->
                val sourceBlock = blocks[continuationSourceId!!]
                if (sourceBlock != null) {
                    val newPos = Offset(sourceBlock.position.x, sourceBlock.position.y + sourceBlock.size.height + 40f)
                    val newBlock = Block(
                        position = newPos,
                        size = Size(140f, 60f),
                        color = Color(0xFFFFF9C4),
                        content = element,
                        isConditionBlock = true
                    )
                    blocks[newBlock.id] = newBlock
                    continuations.add(Continuation(sourceBlockId = continuationSourceId!!, targetBlockId = newBlock.id))
                }
                showIfDialog = false
                continuationSourceId = null
            },
            onCancel = { showIfDialog = false; continuationSourceId = null }
        )
    }

    if (showReturnDialog) {
        CreateReturnDialog(
            onConfirm = { element ->
                val sourceBlock = blocks[continuationSourceId!!]
                if (sourceBlock != null) {
                    val newPos = Offset(sourceBlock.position.x, sourceBlock.position.y + sourceBlock.size.height + 40f)
                    val newBlock = Block(
                        position = newPos,
                        size = Size(140f, 60f),
                        color = Color(0xFFE1BEE7),
                        content = element
                    )
                    blocks[newBlock.id] = newBlock
                    continuations.add(Continuation(sourceBlockId = continuationSourceId!!, targetBlockId = newBlock.id))
                }
                showReturnDialog = false
                continuationSourceId = null
            },
            onCancel = { showReturnDialog = false; continuationSourceId = null }
        )
    }

    if (showFunctionsDialog) {
        Dialog(onDismissRequest = { showFunctionsDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp, modifier = Modifier.width(500.dp)) {
                Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Список функций",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Первая функция в списке — главная точка входа.\nДалее следуют действия в порядке выполнения.",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    BasicTextField(
                        value = functionsText,
                        onValueChange = { functionsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .background(Color(0xFF263238))
                            .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        textStyle = TextStyle(color = Color.White, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, fontSize = 14.sp)
                    )
                    Row(
                        modifier = Modifier.align(Alignment.End),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { showFunctionsDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Закрыть")
                        }
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(
        onCloseRequest = { exitApplication() },
        title = "APP KT - Редактор блоков с потоком управления"
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DragWithSelectionBorder()
        }
    }
}