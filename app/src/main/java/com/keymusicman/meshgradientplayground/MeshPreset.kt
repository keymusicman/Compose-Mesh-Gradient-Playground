package com.keymusicman.meshgradientplayground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import java.util.UUID

/** A single control vertex of the mesh, in normalized 0..1 coordinates. */
internal data class MeshVertex(
    val row: Int,
    val column: Int,
    val position: Offset,
    val color: Color,
    val leftControlPoint: Offset = Offset.Unspecified,
    val topControlPoint: Offset = Offset.Unspecified,
    val rightControlPoint: Offset = Offset.Unspecified,
    val bottomControlPoint: Offset = Offset.Unspecified,
)

/**
 * A named mesh, as saved by "Save as". Meshes are identified by [id], so two of them may share a
 * name.
 */
internal data class MeshPreset(
    val name: String,
    val rows: Int,
    val columns: Int,
    val hasBicubicColor: Boolean,
    val vertices: List<MeshVertex>,
    val id: String = UUID.randomUUID().toString(),
)

internal fun createVertices(rows: Int, columns: Int): List<MeshVertex> = buildList {
    for (row in 0..rows) {
        for (column in 0..columns) {
            add(
                MeshVertex(
                    row = row,
                    column = column,
                    position = Offset(column / columns.toFloat(), row / rows.toFloat()),
                    color = VERTEX_COLORS[(row * (columns + 1) + column) % VERTEX_COLORS.size],
                ),
            )
        }
    }
}

internal const val DEFAULT_ROWS = 1
internal const val DEFAULT_COLUMNS = 1
internal const val MIN_MESH_SIZE = 1
internal const val MAX_MESH_SIZE = 10
internal val VERTEX_COLORS = listOf(
    Color(0xFFFF3D8D),
    Color(0xFF7047EB),
    Color(0xFF13D8B5),
    Color(0xFFFFC857),
    Color(0xFF2499FF),
    Color(0xFFFF6B35),
)
