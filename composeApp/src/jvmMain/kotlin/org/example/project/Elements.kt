//package org.example.project
//
//import androidx.compose.ui.graphics.Color
//
//class Data
//
//abstract class Element {
//    var data: Data? = null
//}
//
//open class TextElement(
//    var text: String = ""
//) : Element()
//
//abstract class NumberElement : Element() {
//    open var number: Number = 0.0
//}
//
//open class IntElement(
//    var int: Int = 0
//) : NumberElement() {
//    override var number: Number
//        get() = int
//        set(value) { int = value.toInt() }
//}
//
//open class DoubleElement(
//    var double: Double = 0.0
//) : NumberElement() {
//    override var number: Number
//        get() = double
//        set(value) { double = value.toDouble() }
//}
//
//open class ChoiceElement(
//    var text: String = "",
//    var choices: List<String> = listOf(),
//    var isOnlyChoices: Boolean = false
//) : Element()
//
//open class Block_(
//    var text: String = "",
//    var elements: Map<String, Element> = mapOf()
//) : Element()
//
//open class BlockUnderText(text: String = "", elements: Map<String, Element> = mapOf()) : Block_(text, elements)
//
//open class IntLimitElement(
//    int: Int = 0,
//    var limit: List<IntRange> = listOf(Int.MIN_VALUE..Int.MAX_VALUE)
//) : IntElement(int)
//
//open class DoubleLimitElement(
//    double: Double = 0.0,
//    var limit: List<DoubleRange> = listOf(Double.MIN_VALUE..Double.MAX_VALUE)
//) : DoubleElement(double)
//
//enum class Side { TOP, BOTTOM, LEFT, RIGHT, CENTER, CORNER }
//
//data class ConnectionPoint(
//    val type: ConnectionType,
//    val blockId: String? = null,
//    val side: Side? = null,
//    val offset: Double = 0.0,
//    val x: Double? = null,
//    val y: Double? = null
//)
//
//enum class ConnectionType { BLOCK, ABSOLUTE }
//
//data class ArrowElement(
//    val source: ConnectionPoint,
//    val target: ConnectionPoint,
//    val style: ArrowStyle,
//    val path: PathData
//)
//
//data class PathData(
//    val type: PathType,
//    val params: Map<String, Any> = emptyMap()
//)
//
//enum class PathType {
//    DIRECT,
//    QUADRATIC_BEZIER,
//    CUBIC_BEZIER,
//    POLYLINE,
//    CUSTOM_SVG
//}
//
//data class ArrowStyle(
//    val color: Color = Color.Black,
//    val thickness: Double = 2.0,
//    val lineStyle: LineStyle = LineStyle.SOLID,
//    val startTip: TipStyle = TipStyle(TipType.TRIANGLE),
//    val endTip: TipStyle = TipStyle(TipType.TRIANGLE)
//)
//
//enum class LineStyle { SOLID, DASHED, DOTTED }
//enum class TipType { NONE, TRIANGLE, CIRCLE, SQUARE, DIAMOND }
//
//data class TipStyle(val type: TipType, val size: Double = 10.0, val rotation: Double = 0.0)
//
//class DoubleRange(
//    var from: Double, var to: Double
//)
//
//operator fun Double.rangeTo(to: Double) = DoubleRange(this, to)