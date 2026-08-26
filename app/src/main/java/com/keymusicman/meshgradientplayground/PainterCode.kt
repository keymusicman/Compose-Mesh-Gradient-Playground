package com.keymusicman.meshgradientplayground

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import kotlin.math.roundToInt

/**
 * Renders the mesh as the Kotlin that would build it: a `MeshGradientPainter` call with one
 * `setVertex` per vertex. Control points left unspecified are omitted rather than written out, so
 * the code keeps the renderer's inferred tangents instead of freezing them.
 */
internal fun painterCode(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    vertices: List<MeshVertex>,
): String = buildString {
    appendLine("MeshGradientPainter(")
    appendLine("    rows = $rows,")
    appendLine("    columns = $columns,")
    appendLine("    hasBicubicColor = $hasBicubicColor,")
    appendLine(") {")
    vertices.forEach { vertex ->
        appendLine("    setVertex(")
        appendLine("        row = ${vertex.row},")
        appendLine("        column = ${vertex.column},")
        appendLine("        position = ${vertex.position.literal()},")
        appendLine("        color = ${vertex.color.literal()},")
        appendControlPoint("leftControlPoint", vertex.leftControlPoint)
        appendControlPoint("topControlPoint", vertex.topControlPoint)
        appendControlPoint("rightControlPoint", vertex.rightControlPoint)
        appendControlPoint("bottomControlPoint", vertex.bottomControlPoint)
        appendLine("    )")
    }
    append("}")
}

internal fun sharePainterCodeIntent(code: String): Intent = Intent(Intent.ACTION_SEND).apply {
    type = "text/plain"
    putExtra(Intent.EXTRA_TEXT, code)
}

private fun StringBuilder.appendControlPoint(name: String, controlPoint: Offset) {
    if (!controlPoint.isSpecified) return
    appendLine("        $name = ${controlPoint.literal()},")
}

private fun Offset.literal(): String = "Offset(${x.literal()}, ${y.literal()})"

private fun Color.literal(): String =
    "Color(0x${toArgb().toUInt().toString(16).uppercase().padStart(8, '0')})"

/** Dragging a vertex lands on floats like 0.43728914; nobody wants that pasted into their code. */
private fun Float.literal(): String {
    val rounded = (this * LITERAL_SCALE).roundToInt() / LITERAL_SCALE
    return if (rounded == rounded.toInt().toFloat()) "${rounded.toInt()}f" else "${rounded}f"
}

private const val LITERAL_SCALE = 10_000f
