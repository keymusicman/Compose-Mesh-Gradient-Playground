package com.keymusicman.meshgradientplayground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

/**
 * Meshes copied into the user's collection the first time the app runs. They behave like any other
 * saved mesh afterwards: editable, overwritable, deletable. Adding entries here only affects fresh
 * installs — existing installs already have their copies.
 *
 * TODO: fill in the rest of the defaults.
 *
 * A vertex takes normalized 0..1 coordinates, and every control point is optional:
 * ```
 * MeshVertex(
 *     row = 0,
 *     column = 1,
 *     position = Offset(x = 0.5f, y = 0f),
 *     color = Color(0xFFFF3D8D),
 *     leftControlPoint = Offset(x = 0.25f, y = 0.1f),
 *     topControlPoint = Offset.Unspecified,
 *     rightControlPoint = Offset(x = 0.75f, y = 0.1f),
 *     bottomControlPoint = Offset.Unspecified,
 * )
 * ```
 * A mesh of `rows` × `columns` cells needs `(rows + 1) * (columns + 1)` vertices, one per grid
 * corner. [createVertices] builds an evenly spaced grid to start from.
 */
internal val DEFAULT_MESHES: List<MeshPreset> = listOf(
    MeshPreset(
        name = "Sunset",
        rows = 1,
        columns = 1,
        hasBicubicColor = true,
        vertices = listOf(
            MeshVertex(
                row = 0,
                column = 0,
                position = Offset(x = 0f, y = 0f),
                color = Color(0xFFFFC857),
            ),
            MeshVertex(
                row = 0,
                column = 1,
                position = Offset(x = 1f, y = 0f),
                color = Color(0xFFFF6B35),
            ),
            MeshVertex(
                row = 1,
                column = 0,
                position = Offset(x = 0f, y = 1f),
                color = Color(0xFFFF3D8D),
            ),
            MeshVertex(
                row = 1,
                column = 1,
                position = Offset(x = 1f, y = 1f),
                color = Color(0xFF7047EB),
            ),
        ),
    ),
)
