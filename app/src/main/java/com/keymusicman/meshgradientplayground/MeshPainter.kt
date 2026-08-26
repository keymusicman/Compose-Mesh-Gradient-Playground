package com.keymusicman.meshgradientplayground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.MeshGradientPainter

/**
 * Builds the painter for a mesh. Vertices that fall outside the `rows` × `columns` grid are
 * skipped rather than allowed to throw from `setVertex`, so a hand-written entry in
 * [DEFAULT_MESHES] or an edited `meshes.json` cannot take the app down.
 */
@Composable
internal fun rememberMeshPainter(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    vertices: List<MeshVertex>,
): MeshGradientPainter {
    val safeRows = rows.coerceAtLeast(1)
    val safeColumns = columns.coerceAtLeast(1)
    return remember(safeRows, safeColumns, hasBicubicColor, vertices) {
        MeshGradientPainter(
            rows = safeRows,
            columns = safeColumns,
            hasBicubicColor = hasBicubicColor,
        ) {
            vertices.forEach { vertex ->
                if (vertex.row !in 0..safeRows || vertex.column !in 0..safeColumns) return@forEach
                setVertex(
                    row = vertex.row,
                    column = vertex.column,
                    position = vertex.position,
                    color = vertex.color,
                    leftControlPoint = vertex.leftControlPoint,
                    topControlPoint = vertex.topControlPoint,
                    rightControlPoint = vertex.rightControlPoint,
                    bottomControlPoint = vertex.bottomControlPoint,
                )
            }
        }
    }
}

@Composable
internal fun rememberMeshPainter(preset: MeshPreset): MeshGradientPainter = rememberMeshPainter(
    rows = preset.rows,
    columns = preset.columns,
    hasBicubicColor = preset.hasBicubicColor,
    vertices = preset.vertices,
)
