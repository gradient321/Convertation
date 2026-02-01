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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.UUID
import kotlin.math.*

// ===== ЭЛЕМЕНТЫ =====
abstract class Element

sealed class BlockType {
    object Function : BlockType()
    object Print : BlockType()
    object Variable : BlockType()
    object If : BlockType()
    object Return : BlockType()
}

data class FunctionElement(
    var name: String = "new_function",
    var parameters: String = "a: int, b: str"
) : Element()

data class PrintElement(
    var text: String = "\"Hello\"",
    var newLine: Boolean = true
) : Element()

data class VariableElement(
    var name: String = "x",
    var type: String = "int",
    var value: String = "0"
) : Element()

data class IfElement(
    var condition: String = "x > 0"
) : Element()

data class ReturnElement(
    var value: String = ""
) : Element()

// ===== СТРЕЛКИ =====
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

// ===== КОНСТАНТЫ =====
private val BackgroundColor = Color(0xFF1E1E1E)
private val DefaultBlockColors = mapOf(
    BlockType.Function to Color(0xFF6A1B9A),
    BlockType.Print to Color(0xFF0288D1),
    BlockType.Variable to Color(0xFF2E7D32),
    BlockType.If to Color(0xFFC62828),
    BlockType.Return to Color(0xFF5D4037)
)
private const val BlockWidth = 180f
private const val BlockHeight = 70f
private const val BlockSpacingY = 50f
private const val BranchOffsetX = 70f

// ===== МОДЕЛЬ БЛОКА =====
data class Block(
    val id: String = UUID.randomUUID().toString(),
    val position: Offset = Offset.Zero,
    val size: Size = Size(BlockWidth, BlockHeight),
    val blockType: BlockType,
    val content: Element,
    val nextBlockId: String? = null,
    val parentIfBlockId: String? = null,
    val branchIndex: Int = -1
)

// ===== ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ =====
private fun getBottomCenter(blockPos: Offset, blockSize: Size): Offset =
    Offset(blockPos.x + blockSize.width / 2f, blockPos.y + blockSize.height)

private fun getTopCenter(blockPos: Offset, blockSize: Size): Offset =
    Offset(blockPos.x + blockSize.width / 2f, blockPos.y)

private fun exportBlocksToPython(blocks: Map<String, Block>, arrows: List<ExecutionArrow>): String {
    if (blocks.isEmpty()) return "# Нет блоков для экспорта"
    
    // Находим ПЕРВЫЙ блок (у которого нет входящей стрелки И который не является веткой условия)
    val allTargetIds = arrows.map { it.toBlockId }.toSet()
    val firstBlock = blocks.values.firstOrNull {
        it.id !in allTargetIds && it.parentIfBlockId == null && it.branchIndex == -1
    } ?: return "# Ошибка: не найден начальный блок"
    
    val visited = mutableSetOf<String>()
    val lines = mutableListOf<String>()
    val indentStep = "    "
    
    fun traverse(blockId: String, indentLevel: Int = 0): Boolean {
        if (blockId !in blocks || blockId in visited) return false
        visited.add(blockId)
        
        val block = blocks[blockId]!!
        val indent = indentStep.repeat(indentLevel)
        
        when (block.blockType) {
            is BlockType.Function -> {
                val func = block.content as FunctionElement
                lines.add("${indent}def ${func.name}(${func.parameters}):")
                if (block.nextBlockId != null) {
                    val hasContent = traverse(block.nextBlockId, indentLevel + 1)
                    if (!hasContent) lines.add("${indent}${indentStep}pass")
                } else {
                    lines.add("${indent}${indentStep}pass")
                }
                lines.add("")
                return true
            }
            
            is BlockType.Print -> {
                val print = block.content as PrintElement
                val stmt = if (print.newLine)
                    "print(${print.text})"
                else
                    "print(${print.text}, end='')"
                lines.add("${indent}${stmt}")
            }
            
            is BlockType.Variable -> {
                val varEl = block.content as VariableElement
                val value = if (varEl.value.isEmpty()) "" else " = ${varEl.value}"
                lines.add("${indent}${varEl.name}${value}")
            }
            
            is BlockType.If -> {
                if (block.branchIndex == -1 && block.parentIfBlockId == null) {
                    val ifEl = block.content as IfElement
                    lines.add("${indent}if ${ifEl.condition}:")
                    
                    // Ветка "да" (branchIndex = 0)
                    val trueBranch = blocks.values
                        .firstOrNull { it.parentIfBlockId == block.id && it.branchIndex == 0 }
                    if (trueBranch != null) {
                        traverse(trueBranch.id, indentLevel + 1)
                    } else {
                        lines.add("${indent}${indentStep}pass")
                    }
                    
                    // Ветки elif
                    val elifBranches = blocks.values
                        .filter { it.parentIfBlockId == block.id && it.branchIndex > 0 }
                        .sortedBy { it.branchIndex }
                    
                    for (elifHeader in elifBranches) {
                        val elifCond = (elifHeader.content as IfElement).condition
                        lines.add("${indent}elif ${elifCond}:")
                        
                        val elifBody = blocks.values
                            .firstOrNull { it.parentIfBlockId == block.id && it.branchIndex == elifHeader.branchIndex && it.id != elifHeader.id }
                        if (elifBody != null) {
                            traverse(elifBody.id, indentLevel + 1)
                        } else {
                            lines.add("${indent}${indentStep}pass")
                        }
                    }
                    
                    // Ветка else
                    val elseHeader = blocks.values
                        .firstOrNull { it.parentIfBlockId == block.id && it.branchIndex == -2 }
                    if (elseHeader != null) {
                        lines.add("${indent}else:")
                        val elseBody = blocks.values
                            .firstOrNull { it.parentIfBlockId == block.id && it.branchIndex == -2 && it.id != elseHeader.id }
                        if (elseBody != null) {
                            traverse(elseBody.id, indentLevel + 1)
                        } else {
                            lines.add("${indent}${indentStep}pass")
                        }
                    }
                    
                    // Продолжение основной цепочки
                    if (block.nextBlockId != null) {
                        traverse(block.nextBlockId, indentLevel)
                    }
                    return true
                } else if (block.parentIfBlockId != null) {
                    // Блок внутри ветки условия
                    when (block.blockType) {
                        is BlockType.Print -> {
                            val print = block.content as PrintElement
                            val stmt = if (print.newLine)
                                "print(${print.text})"
                            else
                                "print(${print.text}, end='')"
                            lines.add("${indent}${stmt}")
                        }
                        is BlockType.Variable -> {
                            val varEl = block.content as VariableElement
                            val value = if (varEl.value.isEmpty()) "" else " = ${varEl.value}"
                            lines.add("${indent}${varEl.name}${value}")
                        }
                        is BlockType.Return -> {
                            val ret = block.content as ReturnElement
                            val value = if (ret.value.isEmpty()) "" else " ${ret.value}"
                            lines.add("${indent}return${value}")
                        }
                        is BlockType.If -> {
                            val ifEl = block.content as IfElement
                            lines.add("${indent}if ${ifEl.condition}:")
                            if (block.nextBlockId != null) {
                                traverse(block.nextBlockId!!, indentLevel + 1)
                            } else {
                                lines.add("${indent}${indentStep}pass")
                            }
                        }
                    }
                    
                    if (block.nextBlockId != null) {
                        traverse(block.nextBlockId!!, indentLevel)
                    }
                    return true
                }
            }
            
            is BlockType.Return -> {
                val ret = block.content as ReturnElement
                val value = if (ret.value.isEmpty()) "" else " ${ret.value}"
                lines.add("${indent}return${value}")
            }
        }
        
        if (block.nextBlockId != null) {
            traverse(block.nextBlockId!!, indentLevel)
        }
        
        return true
    }
    
    traverse(firstBlock.id)
    
    // Добавляем точку входа если нет функций верхнего уровня
    if (lines.isNotEmpty() && !lines.any { it.startsWith("def ") && !it.startsWith("    ") }) {
        val indentedLines = lines.map { "    $it" }
        lines.clear()
        lines.add("def main():")
        lines.addAll(indentedLines)
        lines.add("")
        lines.add("if __name__ == \"__main__\":")
        lines.add("    main()")
    }
    
    return lines.joinToString("\n").trimEnd()
}

// ===== ПАРСЕР PYTHON КОДА =====
fun importPythonToBlocks(code: String): Pair<Map<String, Block>, List<ExecutionArrow>> {
    val blocks = mutableMapOf<String, Block>()
    val arrows = mutableListOf<ExecutionArrow>()
    val lines = code.lines().map { it.trimEnd() }
    
    var currentY = 100f
    var lastBlockId: String? = null
    var indentStack = mutableListOf<Pair<Int, String>>()
    var ifStack = mutableListOf<Pair<String, Int>>() // (ifBlockId, currentBranchIndex)
    
    fun createBlock(
        blockType: BlockType,
        content: Element,
        x: Float = 100f,
        y: Float = currentY,
        nextBlockId: String? = null,
        parentIfBlockId: String? = null,
        branchIndex: Int = -1
    ): Block {
        val block = Block(
            position = Offset(x, y),
            blockType = blockType,
            content = content,
            nextBlockId = nextBlockId,
            parentIfBlockId = parentIfBlockId,
            branchIndex = branchIndex
        )
        blocks[block.id] = block
        currentY += BlockHeight + BlockSpacingY
        return block
    }
    
    fun connect(fromId: String, toId: String, color: Color = Color(0xFF42A5F5)) {
        arrows.add(ExecutionArrow(fromBlockId = fromId, toBlockId = toId, style = ArrowStyle(color = color)))
    }
    
    var i = 0
    while (i < lines.size) {
        val line = lines[i].trim()
        if (line.isEmpty() || line.startsWith("#") || line.startsWith("\"\"\"") || line.startsWith("'''")) {
            i++
            continue
        }
        
        val rawLine = lines[i]
        val indentSpaces = rawLine.takeWhile { it == ' ' }.length
        val indentLevel = indentSpaces / 4
        
        // Очистка стеков от более глубоких уровней
        indentStack.removeAll { it.first >= indentLevel }
        ifStack.removeAll { indentStack.none { s -> s.first == indentLevel - 1 } }
        
        when {
            line.startsWith("def ") -> {
                val match = Regex("""def\s+(\w+)\s*\(([^)]*)\)""").find(line)
                if (match != null) {
                    val (name, params) = match.destructured
                    val block = createBlock(
                        BlockType.Function,
                        FunctionElement(name, params.toString().replace(": Int", ": int").replace(": String", ": str").replace(": Boolean", ": bool"))
                    )
                    if (lastBlockId != null && indentLevel == 0) {
                        val lastBlock = blocks[lastBlockId]
                        if (lastBlock != null) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(lastBlockId!!, block.id)
                        }
                    }
                    lastBlockId = block.id
                    indentStack.add(Pair(indentLevel, block.id))
                    currentY += 20f
                }
            }
            
            line.startsWith("if ") -> {
                val cond = line.removePrefix("if ").removeSuffix(":").trim()
                val block = createBlock(
                    BlockType.If,
                    IfElement(cond),
                    y = currentY
                )
                if (lastBlockId != null && indentLevel == 0) {
                    val lastBlock = blocks[lastBlockId]
                    if (lastBlock != null) {
                        blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                        connect(lastBlockId!!, block.id)
                    }
                }
                lastBlockId = block.id
                indentStack.add(Pair(indentLevel, block.id))
                ifStack.add(Pair(block.id, 0)) // Начинаем ветку "да"
            }
            
            line.startsWith("elif ") -> {
                if (ifStack.isNotEmpty()) {
                    val (ifBlockId, _) = ifStack.last()
                    val cond = line.removePrefix("elif ").removeSuffix(":").trim()
                    val block = createBlock(
                        BlockType.If,
                        IfElement(cond),
                        x = 100f + BranchOffsetX * 1.5f,
                        y = currentY,
                        parentIfBlockId = ifBlockId,
                        branchIndex = ifStack.last().second + 1
                    )
                    // Соединяем с предыдущей веткой
                    val prevBranchBlocks = blocks.values
                        .filter { it.parentIfBlockId == ifBlockId && it.branchIndex == ifStack.last().second }
                        .sortedBy { it.position.y }
                    if (prevBranchBlocks.isNotEmpty()) {
                        val lastInPrev = prevBranchBlocks.last()
                        connect(lastInPrev.id, block.id, Color(0xFFFFA726))
                    } else {
                        connect(ifBlockId, block.id, Color(0xFFFFA726))
                    }
                    lastBlockId = block.id
                    ifStack[ifStack.lastIndex] = Pair(ifBlockId, ifStack.last().second + 1)
                }
            }
            
            line.startsWith("else:") -> {
                if (ifStack.isNotEmpty()) {
                    val (ifBlockId, lastBranchIndex) = ifStack.last()
                    val block = createBlock(
                        BlockType.If,
                        IfElement("else"),
                        x = 100f + BranchOffsetX * 2.5f,
                        y = currentY,
                        parentIfBlockId = ifBlockId,
                        branchIndex = -2
                    )
                    // Соединяем с последней веткой
                    val prevBranchBlocks = blocks.values
                        .filter { it.parentIfBlockId == ifBlockId && it.branchIndex == lastBranchIndex }
                        .sortedBy { it.position.y }
                    if (prevBranchBlocks.isNotEmpty()) {
                        val lastInPrev = prevBranchBlocks.last()
                        connect(lastInPrev.id, block.id, Color(0xFF4CAF50))
                    } else {
                        connect(ifBlockId, block.id, Color(0xFF4CAF50))
                    }
                    lastBlockId = block.id
                }
            }
            
            line.startsWith("print(") -> {
                val content = line.removePrefix("print(").removeSuffix(")").trim()
                var newLine = true
                var text = content
                if (content.endsWith(", end=''") || content.endsWith(", end=\"\"")) {
                    newLine = false
                    text = content.substringBeforeLast(",").trim()
                }
                val block = createBlock(
                    BlockType.Print,
                    PrintElement(text, newLine),
                    y = currentY
                )
                if (lastBlockId != null) {
                    val lastBlock = blocks[lastBlockId]
                    if (lastBlock != null) {
                        if (lastBlock.parentIfBlockId != null && lastBlock.branchIndex != -1) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(
                                lastBlockId!!, block.id, when (lastBlock.branchIndex) {
                                    0 -> Color(0xFF42A5F5)
                                    -2 -> Color(0xFF4CAF50)
                                    else -> Color(0xFFFFA726)
                                }
                            )
                        } else if (indentLevel <= (indentStack.lastOrNull()?.first ?: 0)) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(lastBlockId!!, block.id)
                        }
                    }
                }
                lastBlockId = block.id
                indentStack.add(Pair(indentLevel, block.id))
            }
            
            line.contains("=") && !line.startsWith("if") && !line.startsWith("def") && !line.startsWith("while") && !line.startsWith("for") && !line.startsWith("elif") && !line.startsWith("else") -> {
                val parts = line.split("=", limit = 2)
                val name = parts[0].trim()
                val value = parts[1].trim()
                val type = when {
                    value.startsWith("\"") || value.startsWith("'") -> "str"
                    value.equals("true", ignoreCase = true) || value.equals("false", ignoreCase = true) -> "bool"
                    value.toDoubleOrNull() != null -> if (value.contains('.')) "float" else "int"
                    else -> "any"
                }
                val block = createBlock(
                    BlockType.Variable,
                    VariableElement(name, type, value),
                    y = currentY
                )
                if (lastBlockId != null) {
                    val lastBlock = blocks[lastBlockId]
                    if (lastBlock != null) {
                        if (lastBlock.parentIfBlockId != null && lastBlock.branchIndex != -1) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(
                                lastBlockId!!, block.id, when (lastBlock.branchIndex) {
                                    0 -> Color(0xFF42A5F5)
                                    -2 -> Color(0xFF4CAF50)
                                    else -> Color(0xFFFFA726)
                                }
                            )
                        } else if (indentLevel <= (indentStack.lastOrNull()?.first ?: 0)) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(lastBlockId!!, block.id)
                        }
                    }
                }
                lastBlockId = block.id
                indentStack.add(Pair(indentLevel, block.id))
            }
            
            line.startsWith("return") -> {
                val value = line.removePrefix("return").trim()
                val block = createBlock(
                    BlockType.Return,
                    ReturnElement(value),
                    y = currentY
                )
                if (lastBlockId != null) {
                    val lastBlock = blocks[lastBlockId]
                    if (lastBlock != null) {
                        if (lastBlock.parentIfBlockId != null && lastBlock.branchIndex != -1) {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(
                                lastBlockId!!, block.id, when (lastBlock.branchIndex) {
                                    0 -> Color(0xFF42A5F5)
                                    -2 -> Color(0xFF4CAF50)
                                    else -> Color(0xFFFFA726)
                                }
                            )
                        } else {
                            blocks[lastBlockId!!] = lastBlock.copy(nextBlockId = block.id)
                            connect(lastBlockId!!, block.id)
                        }
                    }
                }
                lastBlockId = block.id
                indentStack.add(Pair(indentLevel, block.id))
            }
        }
        
        i++
    }
    
    return blocks to arrows
}

// ===== КОМПОНЕНТЫ =====
@Composable
fun BlockComponent(
    position: Offset,
    size: Size,
    color: Color,
    content: Element,
    blockType: BlockType,
    branchIndex: Int
) {
    val title = when {
        blockType is BlockType.If && branchIndex == -1 -> "Если"
        blockType is BlockType.If && branchIndex == 0 -> "Да"
        blockType is BlockType.If && branchIndex > 0 -> "Иначе если"
        blockType is BlockType.If && branchIndex == -2 -> "Иначе"
        blockType is BlockType.Function -> "Функция"
        blockType is BlockType.Print -> "Вывод"
        blockType is BlockType.Variable -> "Переменная"
        blockType is BlockType.Return -> "Возврат"
        else -> "блок"
    }
    
    val details = when (content) {
        is FunctionElement -> "${content.name}(${content.parameters})"
        is PrintElement -> content.text
        is VariableElement -> "${content.name} = ${content.value}"
        is IfElement -> content.condition
        is ReturnElement -> if (content.value.isEmpty()) "ничего" else content.value
        else -> "блок"
    }
    
    Box(
        modifier = Modifier
            .offset { androidx.compose.ui.unit.IntOffset(position.x.toInt(), position.y.toInt()) }
            .size(size.width.dp, size.height.dp)
            .background(color, RoundedCornerShape(10.dp))
            .shadow(elevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp),
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
                color = Color.White.copy(alpha = 0.95f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun ArrowComponent(start: Offset, end: Offset, style: ArrowStyle) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawLine(
            color = style.color,
            start = start,
            end = end,
            strokeWidth = style.thickness,
            cap = StrokeCap.Round
        )
        
        val arrowheadSize = style.arrowheadSize
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
    hasContinuation: Boolean,
    isIfHeader: Boolean,
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
                .offset { androidx.compose.ui.unit.IntOffset(position.x.toInt(), position.y.toInt()) }
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(2.dp, Color(0xFF1976D2), RoundedCornerShape(14.dp))
                .shadow(elevation = 12.dp)
                .padding(vertical = 14.dp)
                .width(280.dp)
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
                    text = "Продолжить цепочку",
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
                
                if (isIfHeader) {
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
                    text = if (hasContinuation) "Удалить с продолжением" else "Удалить блок",
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
    var name by remember { mutableStateOf("new_function") }
    var params by remember { mutableStateOf("a: int, b: str") }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(400.dp),
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
                    onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Название функции") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
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
                        onClick = { if (name.isNotBlank()) onConfirm(name, params) },
                        enabled = name.isNotBlank(),
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
fun PrintDialog(initial: PrintElement?, onConfirm: (Element) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf(initial?.text ?: "\"Hello\"") }
    var newLine by remember { mutableStateOf(initial?.newLine ?: true) }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(380.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Настройки вывода",
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
    var type by remember { mutableStateOf(initial?.type ?: "int") }
    var value by remember { mutableStateOf(initial?.value ?: "0") }
    val types = listOf("int", "float", "str", "bool")
    var expanded by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(380.dp),
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
                    onValueChange = { name = it.filter { c -> c.isLetterOrDigit() || c == '_' } },
                    label = { Text("Имя переменной") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )
                
                Box {
                    Button(
                        onClick = { expanded = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Тип: $type",
                                fontSize = 16.sp,
                                color = Color(0xFF1B5E20),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "▼",
                                fontSize = 20.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.width(180.dp)
                    ) {
                        types.forEach { t ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = t,
                                        fontSize = 16.sp,
                                        color = if (t == type) Color(0xFF2E7D32) else Color.Black,
                                        fontWeight = if (t == type) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
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
                    label = { Text("Значение") },
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
fun IfDialog(
    initial: IfElement?,
    onConfirm: (IfElement) -> Unit,
    onCancel: () -> Unit
) {
    var condition by remember { mutableStateOf(initial?.condition ?: "x > 0") }
    
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(380.dp),
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
                    label = { Text("Условие (например: x > 0)") },
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
                    .width(380.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text(
                    text = "Возврат значения",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF5D4037)
                )
                
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Возвращаемое значение (оставьте пустым для 'return')") },
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
    title: String = "Продолжить цепочку",
    types: List<Pair<BlockType, String>> = listOf(
        BlockType.Print to "Вывод (print)",
        BlockType.Variable to "Переменная",
        BlockType.If to "Условие (if)",
        BlockType.Return to "Возврат (return)"
    ),
    onSelect: (BlockType) -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(340.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    types.forEach { (type, label) ->
                        val color = DefaultBlockColors[type] ?: Color(0xFF1976D2)
                        Button(
                            onClick = { onSelect(type) },
                            colors = ButtonDefaults.buttonColors(containerColor = color),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(label, fontSize = 17.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

@Composable
fun CodePreviewDialog(
    code: String,
    title: String,
    onDismiss: () -> Unit,
    onCopy: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp, color = Color(0xFF2D2D2D)) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(600.dp)
                    .height(500.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4FC3F7)
                    )
                    TextButton(
                        onClick = onCopy,
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF81D4FA))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📋", fontSize = 18.sp, modifier = Modifier.padding(end = 4.dp))
                            Text("Копировать", fontSize = 14.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E1E1E),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        code.lines().forEachIndexed { index, line ->
                            Row {
                                Text(
                                    text = "${index + 1}.",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.width(40.dp),
                                    textAlign = TextAlign.End,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                                Text(
                                    text = line.ifEmpty { " " },
                                    fontSize = 13.sp,
                                    color = Color.LightGray,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Закрыть", fontSize = 16.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun DragWithSelectionBorder() {
    val blocks = remember { mutableStateMapOf<String, Block>() }
    val arrows = remember { mutableStateListOf<ExecutionArrow>() }
    var cameraX by remember { mutableStateOf(0f) }
    var cameraY by remember { mutableStateOf(0f) }
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
    var blockTypeDialogForBranch by remember { mutableStateOf<Pair<String, Int>?>(null) }
    var showIfDialog by remember { mutableStateOf(false) }
    var ifDialogSourceBlockId by remember { mutableStateOf<String?>(null) }
    var showCodePreview by remember { mutableStateOf(false) }
    var generatedCode by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importCode by remember { mutableStateOf(
        """def calculate(a: int, b: int):
    result = a + b
    if result > 0:
        print("Положительный результат")
    else:
        print("Неположительный результат")
    return result

if __name__ == "__main__":
    x = 10
    y = 20
    calculate(x, y)
"""
    ) }
    var isPanning by remember { mutableStateOf(false) }
    var panStart by remember { mutableStateOf(Offset.Zero) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        
                        when (event.type) {
                            PointerEventType.Press -> {
                                val position = event.changes.first().position
                                val isRightClick = event.buttons.isSecondaryPressed
                                val isLeftClick = event.buttons.isPrimaryPressed
                                
                                if (isRightClick) {
                                    // Правый клик — контекстное меню
                                    var clickedBlock: Block? = null
                                    for (block in blocks.values) {
                                        val displayPos = when {
                                            block.branchIndex == 0 -> Offset(block.position.x + BranchOffsetX, block.position.y)
                                            block.branchIndex > 0 -> Offset(block.position.x + BranchOffsetX * 1.5f, block.position.y)
                                            block.branchIndex == -2 -> Offset(block.position.x + BranchOffsetX * 2.5f, block.position.y)
                                            else -> block.position
                                        }
                                        val screenPos = Offset(displayPos.x - cameraX, displayPos.y - cameraY)
                                        
                                        if (position.x >= screenPos.x &&
                                            position.x <= screenPos.x + block.size.width &&
                                            position.y >= screenPos.y &&
                                            position.y <= screenPos.y + block.size.height) {
                                            clickedBlock = block
                                            break
                                        }
                                    }
                                    
                                    if (clickedBlock != null) {
                                        selectedBlockForContextMenu = clickedBlock
                                        contextMenuPosition = position
                                        showContextMenu = true
                                    } else {
                                        showCreateFunctionDialog = true
                                    }
                                    
                                    event.changes.forEach { it.consume() }
                                } else if (isLeftClick && !showContextMenu && !showCreateFunctionDialog) {
                                    // Левый клик — проверяем попадание в блок
                                    var clickedBlock: Block? = null
                                    for (block in blocks.values) {
                                        val displayPos = when {
                                            block.branchIndex == 0 -> Offset(block.position.x + BranchOffsetX, block.position.y)
                                            block.branchIndex > 0 -> Offset(block.position.x + BranchOffsetX * 1.5f, block.position.y)
                                            block.branchIndex == -2 -> Offset(block.position.x + BranchOffsetX * 2.5f, block.position.y)
                                            else -> block.position
                                        }
                                        val screenPos = Offset(displayPos.x - cameraX, displayPos.y - cameraY)
                                        
                                        if (position.x >= screenPos.x &&
                                            position.x <= screenPos.x + block.size.width &&
                                            position.y >= screenPos.y &&
                                            position.y <= screenPos.y + block.size.height) {
                                            clickedBlock = block
                                            break
                                        }
                                    }
                                    
                                    if (clickedBlock != null) {
                                        selectedBlockId = clickedBlock.id
                                    } else {
                                        // Начинаем панорамирование
                                        isPanning = true
                                        panStart = position
                                    }
                                    
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            
                            PointerEventType.Release -> {
                                isPanning = false
                                event.changes.forEach { it.consume() }
                            }
                            
                            PointerEventType.Move -> {
                                if (isPanning) {
                                    val position = event.changes.first().position
                                    val delta = position - panStart
                                    cameraX -= delta.x
                                    cameraY -= delta.y
                                    panStart = position
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            
                            PointerEventType.Scroll -> {
                                if (showCreateFunctionDialog || showEditDialog || showContextMenu ||
                                    showBlockTypeDialog || showIfDialog || showCodePreview || showImportDialog) {
                                    continue
                                }
                                
                                val delta = event.changes.first().scrollDelta.y
                                if (delta != 0f) {
                                    val oldZoom = zoom
                                    zoom = (zoom * (1f - delta * 0.1f)).coerceIn(0.3f, 3f)
                                    
                                    // Фокусировка на курсоре мыши при масштабировании
                                    val mousePos = event.changes.first().position
                                    val worldBefore = Offset(mousePos.x + cameraX, mousePos.y + cameraY) / oldZoom
                                    val worldAfter = Offset(mousePos.x + cameraX, mousePos.y + cameraY) / zoom
                                    val diff = (worldAfter - worldBefore) * zoom
                                    
                                    cameraX += diff.x
                                    cameraY += diff.y
                                    
                                    event.changes.forEach { it.consume() }
                                }
                            }
                            
                            else -> {}
                        }
                    }
                }
            }
    ) {
        // Стрелки (рисуем сначала)
        arrows.forEach { arrow ->
            val source = blocks[arrow.fromBlockId]
            val target = blocks[arrow.toBlockId]
            if (source != null && target != null) {
                val sourcePos = when {
                    source.branchIndex == 0 -> Offset(source.position.x + BranchOffsetX, source.position.y)
                    source.branchIndex > 0 -> Offset(source.position.x + BranchOffsetX * 1.5f, source.position.y)
                    source.branchIndex == -2 -> Offset(source.position.x + BranchOffsetX * 2.5f, source.position.y)
                    else -> source.position
                }
                val targetPos = when {
                    target.branchIndex == 0 -> Offset(target.position.x + BranchOffsetX, target.position.y)
                    target.branchIndex > 0 -> Offset(target.position.x + BranchOffsetX * 1.5f, target.position.y)
                    target.branchIndex == -2 -> Offset(target.position.x + BranchOffsetX * 2.5f, target.position.y)
                    else -> target.position
                }
                
                val start = getBottomCenter(sourcePos, source.size)
                val end = getTopCenter(targetPos, target.size)
                
                ArrowComponent(
                    start = Offset(start.x - cameraX, start.y - cameraY),
                    end = Offset(end.x - cameraX, end.y - cameraY),
                    style = arrow.style
                )
            }
        }
        
        // Блоки
        blocks.values.forEach { block ->
            val displayPosition = when {
                block.branchIndex == 0 -> Offset(block.position.x + BranchOffsetX, block.position.y)
                block.branchIndex > 0 -> Offset(block.position.x + BranchOffsetX * 1.5f, block.position.y)
                block.branchIndex == -2 -> Offset(block.position.x + BranchOffsetX * 2.5f, block.position.y)
                else -> block.position
            }
            
            BlockComponent(
                position = Offset(displayPosition.x - cameraX, displayPosition.y - cameraY),
                size = block.size,
                color = DefaultBlockColors[block.blockType] ?: Color.Gray,
                content = block.content,
                blockType = block.blockType,
                branchIndex = block.branchIndex
            )
        }
        
        // Панель инструментов
        Box(
            modifier = Modifier
                .padding(16.dp)
                .width(240.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 8.dp,
                color = Color(0xFF252526)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Редактор блок-схем",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    
                    Button(
                        onClick = {
                            val newY = blocks.values.maxOfOrNull { it.position.y + it.size.height }?.plus(60f) ?: 100f
                            val newBlock = Block(
                                position = Offset(100f, newY),
                                blockType = BlockType.Function,
                                content = FunctionElement("new_function", "a: int, b: str")
                            )
                            blocks[newBlock.id] = newBlock
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6A1B9A)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("➕ Новая функция", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = {
                            generatedCode = exportBlocksToPython(blocks.toMap(), arrows.toList())
                            showCodePreview = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0288D1)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        enabled = blocks.isNotEmpty()
                    ) {
                        Text("📤 Экспорт в Python", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Button(
                        onClick = {
                            showImportDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("📥 Импорт из Python", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Масштаб: ${zoom * 100f}%", fontSize = 13.sp, color = Color.LightGray)
                    }
                }
            }
        }
        
        // Диалоги
        if (showCreateFunctionDialog) {
            CreateFunctionDialog(
                onConfirm = { name, params ->
                    val newY = blocks.values.maxOfOrNull { it.position.y + it.size.height }?.plus(60f) ?: 100f
                    val newBlock = Block(
                        position = Offset(100f, newY),
                        blockType = BlockType.Function,
                        content = FunctionElement(name, params)
                    )
                    blocks[newBlock.id] = newBlock
                    showCreateFunctionDialog = false
                },
                onCancel = { showCreateFunctionDialog = false }
            )
        }
        
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
        
        if (showBlockTypeDialog) {
            val title = if (blockTypeDialogForBranch != null) {
                when (blockTypeDialogForBranch!!.second) {
                    0 -> "Действие для ветки \"да\""
                    -2 -> "Действие для ветки \"иначе\""
                    else -> "Действие для ветки \"иначе если\""
                }
            } else {
                "Продолжить цепочку"
            }
            
            BlockTypeSelectionDialog(
                title = title,
                onSelect = { type ->
                    if (blockTypeDialogForBranch != null) {
                        val (parentIfBlockId, branchIndex) = blockTypeDialogForBranch!!
                        val parentBlock = blocks[parentIfBlockId]
                        if (parentBlock != null) {
                            val lastBlockInBranch = blocks.values
                                .filter { it.parentIfBlockId == parentIfBlockId && it.branchIndex == branchIndex }
                                .maxByOrNull { it.position.y } ?: parentBlock
                            
                            val baseX = when (branchIndex) {
                                0 -> parentBlock.position.x + BranchOffsetX
                                -2 -> parentBlock.position.x + BranchOffsetX * 2.5f
                                else -> parentBlock.position.x + BranchOffsetX * 1.5f
                            }
                            
                            val newBlock = Block(
                                position = Offset(baseX, lastBlockInBranch.position.y + lastBlockInBranch.size.height + BlockSpacingY),
                                blockType = type,
                                content = when (type) {
                                    is BlockType.Print -> PrintElement()
                                    is BlockType.Variable -> VariableElement()
                                    is BlockType.If -> IfElement()
                                    is BlockType.Return -> ReturnElement()
                                    is BlockType.Function -> FunctionElement()
                                },
                                parentIfBlockId = parentIfBlockId,
                                branchIndex = branchIndex
                            )
                            
                            blocks[newBlock.id] = newBlock
                            
                            val sourceBlock = blocks.values
                                .filter { it.parentIfBlockId == parentIfBlockId && it.branchIndex == branchIndex }
                                .maxByOrNull { it.position.y } ?: parentBlock
                            
                            val arrowColor = when (branchIndex) {
                                0 -> Color(0xFF42A5F5)
                                -2 -> Color(0xFF4CAF50)
                                else -> Color(0xFFFFA726)
                            }
                            
                            arrows.add(
                                ExecutionArrow(
                                    fromBlockId = sourceBlock.id,
                                    toBlockId = newBlock.id,
                                    style = ArrowStyle(color = arrowColor)
                                )
                            )
                        }
                    } else if (blockTypeDialogSourceId != null) {
                        val sourceBlock = blocks[blockTypeDialogSourceId!!]
                        if (sourceBlock != null) {
                            val newY = sourceBlock.position.y + sourceBlock.size.height + BlockSpacingY
                            val newBlock = Block(
                                position = Offset(sourceBlock.position.x, newY),
                                blockType = type,
                                content = when (type) {
                                    is BlockType.Print -> PrintElement()
                                    is BlockType.Variable -> VariableElement()
                                    is BlockType.If -> IfElement()
                                    is BlockType.Return -> ReturnElement()
                                    is BlockType.Function -> FunctionElement()
                                }
                            )
                            
                            blocks[sourceBlock.id] = sourceBlock.copy(nextBlockId = newBlock.id)
                            blocks[newBlock.id] = newBlock
                            arrows.add(
                                ExecutionArrow(
                                    fromBlockId = sourceBlock.id,
                                    toBlockId = newBlock.id,
                                    style = ArrowStyle(color = Color(0xFF42A5F5))
                                )
                            )
                            
                            if (type is BlockType.If) {
                                ifDialogSourceBlockId = newBlock.id
                                showIfDialog = true
                            }
                        }
                    }
                    showBlockTypeDialog = false
                    blockTypeDialogSourceId = null
                    blockTypeDialogForBranch = null
                },
                onCancel = {
                    showBlockTypeDialog = false
                    blockTypeDialogSourceId = null
                    blockTypeDialogForBranch = null
                }
            )
        }
        
        if (showIfDialog && ifDialogSourceBlockId != null) {
            val ifBlock = blocks[ifDialogSourceBlockId!!]
            IfDialog(
                initial = ifBlock?.content as? IfElement,
                onConfirm = { content ->
                    blocks[ifDialogSourceBlockId!!] = ifBlock!!.copy(content = content)
                    blockTypeDialogForBranch = Pair(ifBlock.id, 0)
                    showBlockTypeDialog = true
                    showIfDialog = false
                    ifDialogSourceBlockId = null
                },
                onCancel = {
                    val ifBlock = blocks[ifDialogSourceBlockId!!]
                    if (ifBlock != null) {
                        val prevBlock = blocks.values.find { it.nextBlockId == ifDialogSourceBlockId }
                        if (prevBlock != null) {
                            blocks[prevBlock.id] = prevBlock.copy(nextBlockId = ifBlock.nextBlockId)
                        }
                        arrows.removeAll { it.fromBlockId == ifDialogSourceBlockId!! || it.toBlockId == ifDialogSourceBlockId!! }
                        blocks.remove(ifDialogSourceBlockId!!)
                    }
                    showIfDialog = false
                    ifDialogSourceBlockId = null
                }
            )
        }
        
        if (showContextMenu && selectedBlockForContextMenu != null) {
            val block = selectedBlockForContextMenu!!
            val hasContinuation = block.nextBlockId != null
            val isIfHeader = block.blockType is BlockType.If && block.branchIndex == -1 && block.parentIfBlockId == null
            val hasElseBranch = isIfHeader &&
              blocks.values.any { it.parentIfBlockId == block.id && it.branchIndex == -2 }
            
            BlockContextMenu(
                position = contextMenuPosition,
                block = block,
                hasContinuation = hasContinuation,
                isIfHeader = isIfHeader,
                hasElseBranch = hasElseBranch,
                onContinue = {
                    if (block.blockType !is BlockType.If || block.branchIndex != -1 || block.parentIfBlockId != null) {
                        blockTypeDialogSourceId = block.id
                        showBlockTypeDialog = true
                    } else {
                        val newY = block.position.y + block.size.height + BlockSpacingY
                        val ifBlock = Block(
                            position = Offset(block.position.x, newY),
                            blockType = BlockType.If,
                            content = IfElement("x > 0"),
                            branchIndex = -1
                        )
                        blocks[block.id] = block.copy(nextBlockId = ifBlock.id)
                        blocks[ifBlock.id] = ifBlock
                        arrows.add(
                            ExecutionArrow(
                                fromBlockId = block.id,
                                toBlockId = ifBlock.id,
                                style = ArrowStyle(color = Color(0xFF42A5F5))
                            )
                        )
                        ifDialogSourceBlockId = ifBlock.id
                        showIfDialog = true
                    }
                    showContextMenu = false
                },
                onEdit = {
                    blockToEdit = block
                    showEditDialog = true
                    showContextMenu = false
                },
                onAddElif = if (isIfHeader) {
                    {
                        val lastBranchIndex = blocks.values
                            .filter { it.parentIfBlockId == block.id && it.branchIndex >= 0 }
                            .maxOfOrNull { it.branchIndex } ?: -1
                        val newBranchIndex = lastBranchIndex + 1
                        
                        val lastBlockInLastBranch = blocks.values
                            .filter { it.parentIfBlockId == block.id && it.branchIndex == lastBranchIndex }
                            .maxByOrNull { it.position.y } ?: block
                        
                        val elifHeaderBlock = Block(
                            position = Offset(
                                block.position.x + BranchOffsetX * 1.5f,
                                lastBlockInLastBranch.position.y + BlockSpacingY
                            ),
                            blockType = BlockType.If,
                            content = IfElement("x == 0"),
                            parentIfBlockId = block.id,
                            branchIndex = newBranchIndex
                        )
                        blocks[elifHeaderBlock.id] = elifHeaderBlock
                        
                        if (lastBranchIndex >= 0) {
                            val lastBlocks = blocks.values
                                .filter { it.parentIfBlockId == block.id && it.branchIndex == lastBranchIndex }
                                .sortedBy { it.position.y }
                            if (lastBlocks.isNotEmpty()) {
                                val lastBlock = lastBlocks.last()
                                arrows.add(
                                    ExecutionArrow(
                                        fromBlockId = lastBlock.id,
                                        toBlockId = elifHeaderBlock.id,
                                        style = ArrowStyle(color = Color(0xFFFFA726))
                                    )
                                )
                            }
                        } else {
                            arrows.add(
                                ExecutionArrow(
                                    fromBlockId = block.id,
                                    toBlockId = elifHeaderBlock.id,
                                    style = ArrowStyle(color = Color(0xFFFFA726))
                                )
                            )
                        }
                        
                        blockTypeDialogForBranch = Pair(block.id, newBranchIndex)
                        showBlockTypeDialog = true
                        showContextMenu = false
                    }
                } else null,
                onAddElse = if (isIfHeader && !hasElseBranch) {
                    {
                        val lastBranchIndex = blocks.values
                            .filter { it.parentIfBlockId == block.id && it.branchIndex >= 0 }
                            .maxOfOrNull { it.branchIndex } ?: -1
                        
                        val lastBlockInLastBranch = blocks.values
                            .filter { it.parentIfBlockId == block.id && it.branchIndex == lastBranchIndex }
                            .maxByOrNull { it.position.y } ?: block
                        
                        val elseHeaderBlock = Block(
                            position = Offset(
                                block.position.x + BranchOffsetX * 2.5f,
                                lastBlockInLastBranch.position.y + BlockSpacingY
                            ),
                            blockType = BlockType.If,
                            content = IfElement("else"),
                            parentIfBlockId = block.id,
                            branchIndex = -2
                        )
                        blocks[elseHeaderBlock.id] = elseHeaderBlock
                        
                        if (lastBranchIndex >= 0) {
                            val lastBlocks = blocks.values
                                .filter { it.parentIfBlockId == block.id && it.branchIndex == lastBranchIndex }
                                .sortedBy { it.position.y }
                            if (lastBlocks.isNotEmpty()) {
                                val lastBlock = lastBlocks.last()
                                arrows.add(
                                    ExecutionArrow(
                                        fromBlockId = lastBlock.id,
                                        toBlockId = elseHeaderBlock.id,
                                        style = ArrowStyle(color = Color(0xFF4CAF50))
                                    )
                                )
                            }
                        } else {
                            arrows.add(
                                ExecutionArrow(
                                    fromBlockId = block.id,
                                    toBlockId = elseHeaderBlock.id,
                                    style = ArrowStyle(color = Color(0xFF4CAF50))
                                )
                            )
                        }
                        
                        blockTypeDialogForBranch = Pair(block.id, -2)
                        showBlockTypeDialog = true
                        showContextMenu = false
                    }
                } else null,
                onDelete = {
                    fun deleteWithContinuation(blockId: String) {
                        val currentBlock = blocks[blockId] ?: return
                        
                        if (currentBlock.nextBlockId != null &&
                            (currentBlock.blockType !is BlockType.If || currentBlock.branchIndex != -1 || currentBlock.parentIfBlockId != null)) {
                            deleteWithContinuation(currentBlock.nextBlockId!!)
                        }
                        
                        arrows.removeAll { it.fromBlockId == blockId || it.toBlockId == blockId }
                        blocks.remove(blockId)
                        
                        val prevBlock = blocks.values.find { it.nextBlockId == blockId }
                        if (prevBlock != null) {
                            blocks[prevBlock.id] = prevBlock.copy(nextBlockId = null)
                        }
                        
                        if (currentBlock.blockType is BlockType.If &&
                            currentBlock.branchIndex == -1 &&
                            currentBlock.parentIfBlockId == null) {
                            blocks.values
                                .filter { it.parentIfBlockId == blockId }
                                .forEach { deleteWithContinuation(it.id) }
                        }
                    }
                    
                    deleteWithContinuation(block.id)
                    showContextMenu = false
                },
                onClose = { showContextMenu = false }
            )
        }
        
        if (showCodePreview) {
            CodePreviewDialog(
                code = generatedCode,
                title = "Сгенерированный Python код",
                onDismiss = { showCodePreview = false },
                onCopy = {
                    try {
                        val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                        clipboard.setContents(StringSelection(generatedCode), null)
                    } catch (e: Exception) {
                        println("Ошибка копирования: ${e.message}")
                    }
                    showCodePreview = false
                }
            )
        }
        
        if (showImportDialog) {
            Dialog(onDismissRequest = { showImportDialog = false }) {
                Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 12.dp) {
                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .width(650.dp)
                            .height(550.dp)
                    ) {
                        Text(
                            text = "Импорт из Python",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32),
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Text(
                            text = "Вставьте Python код ниже (поддерживаются: функции, условия, переменные, вывод, возврат)",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2D2D2D),
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            TextField(
                                value = importCode,
                                onValueChange = { importCode = it },
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedTextColor = Color.LightGray,
                                    unfocusedTextColor = Color.LightGray,
                                    cursorColor = Color(0xFF00FF00)
                                ),
                                placeholder = {
                                    Text(
                                        """def example():
    x = 5
    if x > 0:
        print("Положительное")
    else:
        print("Не положительное")
    return x""",
                                        color = Color.Gray.copy(alpha = 0.7f)
                                    )
                                },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    fontSize = 13.sp
                                )
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { showImportDialog = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFBDBDBD)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Отмена", fontSize = 16.sp)
                            }
                            Button(
                                onClick = {
                                    val (newBlocks, newArrows) = importPythonToBlocks(importCode)
                                    blocks.clear()
                                    blocks.putAll(newBlocks)
                                    arrows.clear()
                                    arrows.addAll(newArrows)
                                    cameraX = 0f
                                    cameraY = 0f
                                    showImportDialog = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Импортировать", fontSize = 16.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
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
        is BlockType.If -> IfDialog(initialContent as? IfElement, { onConfirm(it) }, onCancel)
        is BlockType.Return -> ReturnDialog(initialContent as? ReturnElement, onConfirm, onCancel)
        is BlockType.Function -> onCancel()
    }
}

fun main() = application {
    Window(
        onCloseRequest = { exitApplication() },
        title = "Редактор блок-схем с экспорт в Python"
    ) {
        DragWithSelectionBorder()
    }
}