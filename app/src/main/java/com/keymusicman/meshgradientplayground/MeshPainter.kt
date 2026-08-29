package com.keymusicman.meshgradientplayground

import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.StartOffsetType
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.MeshGradientPainter
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * Builds the painter for a mesh. Vertices that fall outside the `rows` × `columns` grid are
 * skipped rather than allowed to throw from `setVertex`, so a hand-written entry in
 * [DEFAULT_MESHES] or an edited `meshes.json` cannot take the app down.
 *
 * @param hasOrganicDrift when on, interior vertices sway independently on both axes — each one
 *   gets its own amplitude, speed and starting phase (see [rememberVertexDrift]), so the mesh
 *   doesn't pulse as a single unit the way one shared offset would. Border vertices are left
 *   alone so the gradient's outer edge stays put; a mesh with no interior vertices (fewer than 2
 *   rows or columns) has nothing to drift.
 */
@Composable
internal fun rememberMeshPainter(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    vertices: List<MeshVertex>,
    hasOrganicDrift: Boolean = false,
): MeshGradientPainter {
    val safeRows = rows.coerceAtLeast(1)
    val safeColumns = columns.coerceAtLeast(1)

    // One shared clock for every drifting vertex — each still moves independently (see
    // rememberVertexDrift), it just doesn't need its own clock subscription to do that.
    val infiniteTransition = rememberInfiniteTransition(label = "organicDrift")
    val driftByVertex: Map<Pair<Int, Int>, VertexDrift> = if (hasOrganicDrift) {
        buildMap {
            vertices.forEach { vertex ->
                if (vertex.row in 1 until safeRows && vertex.column in 1 until safeColumns) {
                    val drift = key(vertex.row, vertex.column) {
                        rememberVertexDrift(infiniteTransition, vertex.row, vertex.column)
                    }
                    put(vertex.row to vertex.column, drift)
                }
            }
        }
    } else {
        emptyMap()
    }

    return remember(safeRows, safeColumns, hasBicubicColor, vertices, hasOrganicDrift) {
        MeshGradientPainter(
            rows = safeRows,
            columns = safeColumns,
            hasBicubicColor = hasBicubicColor,
        ) {
            vertices.forEach { vertex ->
                if (vertex.row !in 0..safeRows || vertex.column !in 0..safeColumns) return@forEach
                val drift = driftByVertex[vertex.row to vertex.column]
                // .value is read here, inside the draw-time block, which is what lets the mesh
                // animate every frame without rebuilding this painter.
                val offset = if (drift != null) Offset(drift.x.value, drift.y.value) else Offset.Zero
                setVertex(
                    row = vertex.row,
                    column = vertex.column,
                    position = vertex.position + offset,
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
internal fun rememberMeshPainter(
    preset: MeshPreset,
    hasOrganicDrift: Boolean = false,
): MeshGradientPainter = rememberMeshPainter(
    rows = preset.rows,
    columns = preset.columns,
    hasBicubicColor = preset.hasBicubicColor,
    vertices = preset.vertices,
    hasOrganicDrift = hasOrganicDrift,
)

private data class VertexDrift(val x: State<Float>, val y: State<Float>)

/**
 * One vertex's drift, animated independently on each axis: its own amplitude, its own period, and
 * a random start offset fast-forwarded into the cycle so it isn't in step with its neighbours.
 * Everything is derived from [row] and [column] alone, so a vertex keeps the same "personality"
 * across recompositions and even across toggling the checkbox off and on — it's not reshuffled
 * every time, just never in lockstep with the others.
 */
@Composable
private fun rememberVertexDrift(
    infiniteTransition: InfiniteTransition,
    row: Int,
    column: Int,
): VertexDrift {
    val params = remember(row, column) { VertexDriftParams.forVertex(row, column) }
    val x = infiniteTransition.animateFloat(
        initialValue = -params.amplitudeX,
        targetValue = params.amplitudeX,
        animationSpec = infiniteRepeatable(
            animation = tween(params.periodXMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(params.phaseXMs, StartOffsetType.FastForward),
        ),
        label = "driftX",
    )
    val y = infiniteTransition.animateFloat(
        initialValue = -params.amplitudeY,
        targetValue = params.amplitudeY,
        animationSpec = infiniteRepeatable(
            animation = tween(params.periodYMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(params.phaseYMs, StartOffsetType.FastForward),
        ),
        label = "driftY",
    )
    return VertexDrift(x, y)
}

private class VertexDriftParams(
    val amplitudeX: Float,
    val amplitudeY: Float,
    val periodXMs: Int,
    val periodYMs: Int,
    val phaseXMs: Int,
    val phaseYMs: Int,
) {
    companion object {
        fun forVertex(row: Int, column: Int): VertexDriftParams {
            // Any fixed seed works here — the point isn't unpredictability, it's that vertex
            // (1, 2) and vertex (2, 1) don't end up swinging the same way.
            val random = Random(row * 92_821 + column * 6_151 + 17)
            val periodXMs = (ORGANIC_DRIFT_PERIOD_MS * random.nextFloatIn(0.7f, 1.4f)).roundToInt()
            val periodYMs = (ORGANIC_DRIFT_PERIOD_MS * random.nextFloatIn(0.7f, 1.4f)).roundToInt()
            return VertexDriftParams(
                amplitudeX = ORGANIC_DRIFT_AMPLITUDE * random.nextFloatIn(0.5f, 1.5f),
                amplitudeY = ORGANIC_DRIFT_AMPLITUDE * random.nextFloatIn(0.5f, 1.5f),
                periodXMs = periodXMs,
                periodYMs = periodYMs,
                phaseXMs = random.nextInt(periodXMs),
                phaseYMs = random.nextInt(periodYMs),
            )
        }
    }
}

private fun Random.nextFloatIn(from: Float, until: Float): Float = from + nextFloat() * (until - from)

/** How far, in normalized mesh coordinates, a drifting vertex swings from its base position. */
private const val ORGANIC_DRIFT_AMPLITUDE = 0.1f

/** One full swing out and back, in milliseconds, before per-vertex randomization spreads it out. */
private const val ORGANIC_DRIFT_PERIOD_MS = 2500
