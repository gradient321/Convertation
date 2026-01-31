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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import java.util.UUID
import kotlin.math.*

// ===== ЭЛЕМЕНТЫ =====
class Data

abstract class Element {
    var data: Data? = null
}
// Типы блоков
sealed class BlockType {
    object Function : BlockType()
    object Print : BlockType()
    object Variable : BlockType()
    object If : BlockType()
    object Return : BlockType()
}

// Элементы для разных типов блоков
data class FunctionElement(
    var name: String = "newFunction",
    var parameters: String = "a: Int, b: String"
) : Element()

data class PrintElement(
    var text: String = "\"Hello\"",
    var newLine: Boolean = true
) : Element()

data class VariableElement(
    var name: String = "x",
    var type: String = "Int",
    var value: String = "0"
) : Element()

data class IfElement(
    var condition: String = "x > 0",
    var hasElse: Boolean = false
) : Element()

data class ReturnElement(
    var value: String = ""
) : Element()

// ===== СТРЕЛКИ ДЛЯ ЛИНЕЙНОЙ ЦЕПОЧКИ =====
data class ArrowStyle(
    val color: Color = Color(0xFF42A5F5),
    val thickness: Float = 2.5f,
    val arrowheadSize: Float = 10f
)

data class ExecutionArrow(
    val id: String = UUID.randomUUID().toString(),
    val fromBlockId: String,
    val toBlockId: String,
    val style: ArrowStyle = ArrowStyle()
)

// ===== ГЛОБАЛЬНЫЕ КОНСТАНТЫ =====
private val BackgroundColor = Color(0xFF1E1E1E)
private val DefaultBlockColors = mapOf(
    BlockType.Function to Color(0xFF6A1B9A),   // Фиолетовый для функций
    BlockType.Print to Color(0xFF0288D1),      // Синий для принта
    BlockType.Variable to Color(0xFF2E7D32),   // Зелёный для переменных
    BlockType.If to Color(0xFFC62828),         // Красный для условий
    BlockType.Return to Color(0xFF5D4037)      // Коричневый для ретюрна
)
private val SelectionBorderColor = Color.White
private const val BorderWidth = 2f

// ===== МОДЕЛЬ БЛОКА =====
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset = Offset.Zero,
    val size: Size = Size(160f, 60f),
    val blockType: BlockType,
    val content: Element,
    val nextBlockId: String? = null,           // Следующий блок в цепочке
    val parentIfBlockId: String? = null,       // ID родительского условия (для веток)
    val branchIndex: Int = -1                  // -1 = основная цепочка, 0 = if, 1+ = elif, -2 = else
)

private data class DragState(val offset: Offset)
private data class PanState(val initialCamera: Offset, val startPosition: Offset)
private data class ConnectionMode(val sourceBlockId: String)

// ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====
private fun getEdgePoint(blockPos: Offset, blockSize: Size, targetPos: Offset, isSource: Boolean): Offset {
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
    content: Element,
    blockType: BlockType,
    zoom: Float
) {
    val title = when (blockType) {
        is BlockType.Function -> "Функция"
        is BlockType.Print -> "Принт"
        is BlockType.Variable -> "Переменная"
        is BlockType.If -> "Если"
        is BlockType.Return -> "Ретюрн"
    }

    val details = when (content) {
        is FunctionElement -> "${content.name}(${content.parameters})"
        is PrintElement -> content.text
        is VariableElement -> "${content.name}: ${content.type} = ${content.value}"
        is IfElement -> content.condition
        is ReturnElement -> if (content.value.isEmpty()) "пусто" else content.value
        else -> "блок"
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(position.x.roundToInt(), position.y.roundToInt()) }
            .size(size.width.dp, size.height.dp)
            .background(color)
            .border(
                width = if (isSelected) BorderWidth.dp else 0.dp,
                color = SelectionBorderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .shadow(elevation = if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = details,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
fun ArrowComponent(start: Offset, end: Offset, style: ArrowStyle, zoom: Float) {
    Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { alpha = 0.95f }) {
        drawLine(
            color = style.color,
            start = start,
            end = end,
            strokeWidth = style.thickness * zoom.coerceAtLeast(0.6f),
            cap = StrokeCap.Round
        )

        if (zoom > 0.4f) {
            val arrowheadSize = style.arrowheadSize * zoom.coerceAtMost(1.2f)
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
fun MenuItemButton(icon: String, text: String, backgroundColor: Color, iconColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clickable { onClick() }
            .background(backgroundColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(iconColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, fontSize = 18.sp, color = iconColor, fontWeight = FontWeight.Bold)
            }
            Text(
                text = text,
                modifier = Modifier.padding(start = 14.dp),
                fontSize = 16.sp,
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
    hasChildren: Boolean,
    isIfBlock: Boolean,
    hasElseBranch: Boolean,
    onContinue: () -> Unit,
    onEdit: () -> Unit,
    onAddElif: (() -> Unit)? = null,
    onAddElse: (() -> Unit)? = null,
    onDelete: () -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() }
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .clickable { onClose() }
                .size(32.dp)
                .background(Color(0xFFF5F5F5), RoundedCornerShape(16.dp))
                .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = "×", fontSize = 24.sp, color = Color(0xFF616161), fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .graphicsLayer {
                    translationX = position.x
                    translationY = position.y
                }
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(14.dp))
                .shadow(elevation = 10.dp)
                .padding(vertical = 14.dp)
                .width(260.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 10.dp, start = 8.dp, end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(Color(0xFF1976D2), RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⋮", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "Действия с блоком",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1976D2),
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }

                MenuItemButton(
                    icon = "➡️",
                    text = "Продолжить",
                    backgroundColor = Color(0xFFE3F2FD),
                    iconColor = Color(0xFF1976D2),
                    onClick = { onContinue(); onClose() }
                )

                MenuItemButton(
                    icon = "✏️",
                    text = "Редактировать",
                    backgroundColor = Color(0xFFF5F5F5),
                    iconColor = Color(0xFF2196F3),
                    onClick = { onEdit(); onClose() }
                )

                if (isIfBlock) {
                    MenuItemButton(
                        icon = "➕",
                        text = "Добавить ветку (elif)",
                        backgroundColor = Color(0xFFF3E5F5),
                        iconColor = Color(0xFF6A1B9A),
                        onClick = { onAddElif?.invoke(); onClose() }
                    )

                    if (!hasElseBranch) {
                        MenuItemButton(
                            icon = "🔀",
                            text = "Добавить \"иначе\" (else)",
                            backgroundColor = Color(0xFFE8F5E9),
                            iconColor = Color(0xFF2E7D32),
                            onClick = { onAddElse?.invoke(); onClose() }
                        )
                    }
                }

                MenuItemButton(
                    icon = "🗑️",
                    text = if (hasChildren) "Удалить с продолжением" else "Удалить блок",
                    backgroundColor = Color(0xFFFFE5E5),
                    iconColor = Color(0xFFC62828),
                    onClick = { onDelete(); onClose() }
                )
            }
        }
    }
}

@Composable
fun CreateFunctionDialog(onConfirm: (String, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("newFunction") }
    var params by remember { mutableStateOf("a: Int, b: String") }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(380.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Создать функцию",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6A1B9A)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название функции") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = params,
                    onValueChange = { params = it },
                    label = { Text("Параметры (через запятую)") },
                    singleLine = false,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { onConfirm(name, params) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Создать", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateBlockDialog(
    blockType: BlockType,
    initialContent: Element?,
    onConfirm: (Element) -> Unit,
    onCancel: () -> Unit
) {
    when (blockType) {
        is BlockType.Print -> PrintDialog(initialContent as? PrintElement, onConfirm, onCancel)
        is BlockType.Variable -> VariableDialog(initialContent as? VariableElement, onConfirm, onCancel)
        is BlockType.If -> IfDialog(initialContent as? IfElement, onConfirm, onCancel)
        is BlockType.Return -> ReturnDialog(initialContent as? ReturnElement, onConfirm, onCancel)
        is BlockType.Function -> onCancel() // Функция создаётся отдельно
    }
}

@Composable
fun PrintDialog(initial: PrintElement?, onConfirm: (Element) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initial?.text ?: "\"Hello\"") }
    var newLine by remember { mutableStateOf(initial?.newLine ?: true) }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(360.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Настройки принта",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0288D1)
                )

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Текст для вывода") },
                    singleLine = false,
                    maxLines = 3
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = newLine, onCheckedChange = { newLine = it })
                    Text("Переносить на новую строку после вывода")
                }

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { onConfirm(PrintElement(text, newLine)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Готово", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun VariableDialog(initial: VariableElement?, onConfirm: (Element) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "x") }
    var type by remember { mutableStateOf(initial?.type ?: "Int") }
    var value by remember { mutableStateOf(initial?.value ?: "0") }
    val types = listOf("Int", "Double", "String", "Boolean")
    var expanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(360.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Настройки переменной",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Имя переменной") },
                    singleLine = true
                )

                Box {
                    OutlinedTextField(
                        value = type,
                        onValueChange = {},
                        label = { Text("Тип данных") },
                        readOnly = true,
                        trailingIcon = { Text("▼", color = Color.Gray) },
                        singleLine = true,
                        modifier = Modifier.clickable { expanded = true }
                    )

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(180.dp)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = { Text(t, fontSize = 16.sp) },
                                onClick = {
                                    type = t
                                    expanded = false
                                },
                                modifier = Modifier.height(44.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Начальное значение (необязательно)") },
                    singleLine = true
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { onConfirm(VariableElement(name, type, value)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Готово", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun IfDialog(initial: IfElement?, onConfirm: (Element) -> Unit, onCancel: () -> Unit) {
    var condition by remember { mutableStateOf(initial?.condition ?: "x > 0") }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(360.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Настройки условия",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )

                OutlinedTextField(
                    value = condition,
                    onValueChange = { condition = it },
                    label = { Text("Условие") },
                    singleLine = false,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { onConfirm(IfElement(condition)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Готово", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ReturnDialog(initial: ReturnElement?, onConfirm: (Element) -> Unit, onCancel: () -> Unit) {
    var value by remember { mutableStateOf(initial?.value ?: "") }

    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(360.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Настройки ретюрна",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Возвращаемое значение (оставьте пустым для пустого ретюрна)") },
                    singleLine = false,
                    maxLines = 2
                )

                Row(
                    modifier = Modifier.align(Alignment.End),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                        modifier = Modifier.width(100.dp)
                    ) {
                        Text("Отмена", fontSize = 16.sp)
                    }
                    Button(
                        onClick = { onConfirm(ReturnElement(value)) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D4037)),
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("Готово", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun BlockTypeSelectionDialog(
    onSelect: (BlockType) -> Unit,
    onCancel: () -> Unit,
    forIfBranch: Boolean = false
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(320.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (forIfBranch) "Выберите действие для ветки" else "Продолжить цепочку",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val types = if (forIfBranch) {
                    listOf(
                        BlockType.Print to "Принт",
                        BlockType.Variable to "Переменная",
                        BlockType.If to "Если",
                        BlockType.Return to "Ретюрн"
                    )
                } else {
                    listOf(
                        BlockType.Print to "Принт",
                        BlockType.Variable to "Переменная",
                        BlockType.If to "Если",
                        BlockType.Return to "Ретюрн"
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    types.forEach { (type, label) ->
                        val color = DefaultBlockColors[type]!!
                        Button(
                            onClick = { onSelect(type) },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(label, fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Button(
                    onClick = onCancel,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .padding(top = 16.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Отмена", fontSize = 16.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateMapOf<String, Block>() }
    val arrows = remember { mutableStateListOf<ExecutionArrow>() }
    var camera by remember { mutableStateOf(Offset.Zero) }
    var zoom by remember { mutableStateOf(1f) }
    var selectedBlockId by remember { mutableStateOf<String?>(null) }
    var showCreateFunctionDialog by remember { mutableStateOf(false) }
    var createPosition by remember { mutableStateOf(Offset.Zero) }
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuPosition by remember { mutableStateOf(Offset.Zero) }
    var selectedBlockForContextMenu by remember { mutableStateOf<Block?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var blockToEdit by remember { mutableStateOf<Block?>(null) }
    var showBlockTypeDialog by remember { mutableStateOf(false) }
    var blockTypeDialogSourceId by remember { mutableStateOf<String?>(null) }
    var blockTypeDialogForIfBranch by remember { mutableStateOf(false) }
    var dragState by remember { mutableStateOf<DragState?>(null) }
    var panState by remember { mutableStateOf<PanState?>(null) }
    var cursorPosition by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .onPointerEvent(PointerEventType.Scroll) { event ->
                if (showCreateFunctionDialog || showEditDialog || showContextMenu || showBlockTypeDialog) return@onPointerEvent
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
                                    createPosition = screenToWorld(downChange.position, camera, zoom)
                                    showCreateFunctionDialog = true
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
        // Стрелки выполнения
        arrows.forEach { arrow ->
            val source = blocks[arrow.fromBlockId]
            val target = blocks[arrow.toBlockId]
            if (source != null && target != null) {
                val sourceEdge = getEdgePoint(
                    source.position,
                    source.size,
                    target.position + Offset(target.size.width / 2f, target.size.height / 2f),
                    true
                )
                val targetEdge = getEdgePoint(
                    target.position,
                    target.size,
                    source.position + Offset(source.size.width / 2f, source.size.height / 2f),
                    false
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
            val color = DefaultBlockColors[block.blockType] ?: DefaultBlockColors.values.first()
            BlockComponent(
                position = worldToScreen(block.position, camera, zoom),
                size = block.size * zoom,
                color = color,
                isSelected = block.id == selectedBlockId,
                content = block.content,
                blockType = block.blockType,
                zoom = zoom
            )
        }
    }

    // Диалог создания функции
    if (showCreateFunctionDialog) {
        CreateFunctionDialog(
            onConfirm = { name, params ->
                val newBlock = Block(
                    position = createPosition,
                    blockType = BlockType.Function,
                    content = FunctionElement(name, params)
                )
                blocks[newBlock.id] = newBlock
                showCreateFunctionDialog = false
            },
            onCancel = { showCreateFunctionDialog = false }
        )
    }

    // Диалог редактирования блока
    if (showEditDialog && blockToEdit != null) {
        CreateBlockDialog(
            blockType = blockToEdit!!.blockType,
            initialContent = blockToEdit!!.content,
            onConfirm = { content ->
                blocks[blockToEdit!!.id] = blockToEdit!!.copy(content = content)
                showEditDialog = false
                blockToEdit = null
            },
            onCancel = {
                showEditDialog = false
                blockToEdit = null
            }
        )
    }

    // Диалог выбора типа блока для продолжения
    if (showBlockTypeDialog && blockTypeDialogSourceId != null) {
        BlockTypeSelectionDialog(
            onSelect = { type ->
                val sourceBlock = blocks[blockTypeDialogSourceId!!]
                if (sourceBlock != null) {
                    // Создаём новый блок рядом с источником
                    val newPosition = Offset(
                        sourceBlock.position.x,
                        sourceBlock.position.y + sourceBlock.size.height + 40f
                    )

                    val newContent = when (type) {
                        is BlockType.Print -> PrintElement()
                        is BlockType.Variable -> VariableElement()
                        is BlockType.If -> IfElement()
                        is BlockType.Return -> ReturnElement()
                        is BlockType.Function -> FunctionElement()
                    }

                    val newBlock = Block(
                        position = newPosition,
                        blockType = type,
                        content = newContent,
                        parentIfBlockId = if (blockTypeDialogForIfBranch) sourceBlock.id else sourceBlock.parentIfBlockId,
                        branchIndex = if (blockTypeDialogForIfBranch) {
                            when (sourceBlock.blockType) {
                                is BlockType.If -> 0 // первая ветка условия
                                else -> sourceBlock.branchIndex
                            }
                        } else {
                            sourceBlock.branchIndex
                        }
                    )

                    // Обновляем связь "следующий блок"
                    blocks[sourceBlock.id] = sourceBlock.copy(nextBlockId = newBlock.id)
                    blocks[newBlock.id] = newBlock

                    // Создаём стрелку
                    arrows.add(
                        ExecutionArrow(
                            fromBlockId = sourceBlock.id,
                            toBlockId = newBlock.id,
                            style = ArrowStyle(color = Color(0xFF42A5F5), thickness = 2.5f, arrowheadSize = 10f)
                        )
                    )
                }
                showBlockTypeDialog = false
                blockTypeDialogSourceId = null
                blockTypeDialogForIfBranch = false
            },
            onCancel = {
                showBlockTypeDialog = false
                blockTypeDialogSourceId = null
                blockTypeDialogForIfBranch = false
            },
            forIfBranch = blockTypeDialogForIfBranch
        )
    }

    // Контекстное меню
    if (showContextMenu && selectedBlockForContextMenu != null) {
        val block = selectedBlockForContextMenu!!
        val hasChildren = blocks.values.any { it.parentIfBlockId == block.id || it.nextBlockId == block.id }
        val isIfBlock = block.blockType is BlockType.If
        val hasElseBranch = isIfBlock && blocks.values.any { it.parentIfBlockId == block.id && it.branchIndex == -2 }

        BlockContextMenu(
            position = contextMenuPosition,
            block = block,
            hasChildren = hasChildren,
            isIfBlock = isIfBlock,
            hasElseBranch = hasElseBranch,
            onContinue = {
                blockTypeDialogSourceId = block.id
                blockTypeDialogForIfBranch = false
                showBlockTypeDialog = true
            },
            onEdit = {
                blockToEdit = block
                showEditDialog = true
            },
            onAddElif = if (isIfBlock) {
                {
                    // Добавляем новую ветку elif
                    val existingBranches = blocks.values.count { it.parentIfBlockId == block.id && it.branchIndex > 0 }
                    val newBranchIndex = existingBranches + 1

                    val newBlock = Block(
                        position = Offset(block.position.x + 40f * newBranchIndex, block.position.y + 80f),
                        blockType = BlockType.If,
                        content = IfElement("условие_$newBranchIndex"),
                        parentIfBlockId = block.id,
                        branchIndex = newBranchIndex
                    )

                    blocks[newBlock.id] = newBlock
                }
            } else null,
            onAddElse = if (isIfBlock && !hasElseBranch) {
                {
                    // Добавляем ветку else
                    val newBlock = Block(
                        position = Offset(block.position.x + 40f, block.position.y + 120f),
                        blockType = BlockType.If,
                        content = IfElement("иначе"),
                        parentIfBlockId = block.id,
                        branchIndex = -2
                    )

                    blocks[newBlock.id] = newBlock
                }
            } else null,
            onDelete = {
                // Удаляем блок и всё, что после него
                fun deleteChain(blockId: String) {
                    // Удаляем стрелки, связанные с этим блоком
                    arrows.removeAll { it.fromBlockId == blockId || it.toBlockId == blockId }

                    // Находим и удаляем следующий блок в цепочке
                    val nextBlock = blocks.values.find { it.nextBlockId == blockId }
                    if (nextBlock != null) {
                        blocks.remove(nextBlock.id)
                        deleteChain(nextBlock.id)
                    }

                    // Удаляем сам блок
                    blocks.remove(blockId)
                }

                deleteChain(block.id)
            },
            onClose = { showContextMenu = false }
        )
    }
}

fun main() = application {
    Window(
        onCloseRequest = { exitApplication() },
        title = "APP KT - Редактор блоков (цепочка выполнения)"
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            DragWithSelectionBorder()
        }
    }
}