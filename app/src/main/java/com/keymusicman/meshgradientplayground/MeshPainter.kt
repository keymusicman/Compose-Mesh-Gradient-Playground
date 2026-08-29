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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.toArgb
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
 * @param hasColorDrift when on, every vertex's hue drifts independently around the color set for
 *   it — the vertex's own color stays the centre of the swing, so switching this off always lands
 *   back on exactly the color that was picked (see [rememberVertexColorDrift]).
 */
@Composable
internal fun rememberMeshPainter(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    vertices: List<MeshVertex>,
    hasOrganicDrift: Boolean = false,
    hasColorDrift: Boolean = false,
): MeshGradientPainter {
    val safeRows = rows.coerceAtLeast(1)
    val safeColumns = columns.coerceAtLeast(1)

    // One shared clock for every drifting vertex — each still moves independently (see
    // rememberVertexDrift / rememberVertexColorDrift), it just doesn't need its own clock
    // subscription to do that.
    val infiniteTransition = rememberInfiniteTransition(label = "meshAnimation")

    // A fixed amplitude looks fine on a coarse mesh but sends vertices swinging into their
    // neighbours' territory once the grid gets dense, so it's scaled to a fraction of one cell
    // instead — the same "one cell" unit ControlPointEditor already uses for tangent limits.
    val cellDriftAmplitudeX = (1f / safeColumns) * ORGANIC_DRIFT_CELL_FRACTION
    val cellDriftAmplitudeY = (1f / safeRows) * ORGANIC_DRIFT_CELL_FRACTION

    val positionDriftByVertex: Map<Pair<Int, Int>, VertexDrift> = if (hasOrganicDrift) {
        buildMap {
            vertices.forEach { vertex ->
                if (vertex.row in 1 until safeRows && vertex.column in 1 until safeColumns) {
                    val drift = key(vertex.row, vertex.column) {
                        rememberVertexDrift(
                            infiniteTransition = infiniteTransition,
                            row = vertex.row,
                            column = vertex.column,
                            baseAmplitudeX = cellDriftAmplitudeX,
                            baseAmplitudeY = cellDriftAmplitudeY,
                        )
                    }
                    put(vertex.row to vertex.column, drift)
                }
            }
        }
    } else {
        emptyMap()
    }

    // Every vertex, corners included — colour drift doesn't need to keep the mesh's outer shape
    // still the way position drift does.
    val colorDriftByVertex: Map<Pair<Int, Int>, VertexColorDrift> = if (hasColorDrift) {
        buildMap {
            vertices.forEach { vertex ->
                val drift = key(vertex.row, vertex.column) {
                    rememberVertexColorDrift(infiniteTransition, vertex.row, vertex.column)
                }
                put(vertex.row to vertex.column, drift)
            }
        }
    } else {
        emptyMap()
    }

    return remember(safeRows, safeColumns, hasBicubicColor, vertices, hasOrganicDrift, hasColorDrift) {
        MeshGradientPainter(
            rows = safeRows,
            columns = safeColumns,
            hasBicubicColor = hasBicubicColor,
        ) {
            vertices.forEach { vertex ->
                if (vertex.row !in 0..safeRows || vertex.column !in 0..safeColumns) return@forEach
                // .value is read here, inside the draw-time block, which is what lets the mesh
                // animate every frame without rebuilding this painter.
                val positionDrift = positionDriftByVertex[vertex.row to vertex.column]
                val offset = if (positionDrift != null) {
                    Offset(positionDrift.x.value, positionDrift.y.value)
                } else {
                    Offset.Zero
                }
                val colorDrift = colorDriftByVertex[vertex.row to vertex.column]
                val color = if (colorDrift != null) {
                    vertex.color.withHueShift(colorDrift.hueShiftDegrees.value)
                } else {
                    vertex.color
                }
                setVertex(
                    row = vertex.row,
                    column = vertex.column,
                    position = vertex.position + offset,
                    color = color,
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
    hasColorDrift: Boolean = false,
): MeshGradientPainter = rememberMeshPainter(
    rows = preset.rows,
    columns = preset.columns,
    hasBicubicColor = preset.hasBicubicColor,
    vertices = preset.vertices,
    hasOrganicDrift = hasOrganicDrift,
    hasColorDrift = hasColorDrift,
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
    baseAmplitudeX: Float,
    baseAmplitudeY: Float,
): VertexDrift {
    val params = remember(row, column, baseAmplitudeX, baseAmplitudeY) {
        VertexDriftParams.forVertex(row, column, baseAmplitudeX, baseAmplitudeY)
    }
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
        fun forVertex(
            row: Int,
            column: Int,
            baseAmplitudeX: Float,
            baseAmplitudeY: Float,
        ): VertexDriftParams {
            // Any fixed seed works here — the point isn't unpredictability, it's that vertex
            // (1, 2) and vertex (2, 1) don't end up swinging the same way.
            val random = Random(row * 92_821 + column * 6_151 + 17)
            val periodXMs = (ORGANIC_DRIFT_PERIOD_MS * random.nextFloatIn(0.7f, 1.4f)).roundToInt()
            val periodYMs = (ORGANIC_DRIFT_PERIOD_MS * random.nextFloatIn(0.7f, 1.4f)).roundToInt()
            return VertexDriftParams(
                amplitudeX = baseAmplitudeX * random.nextFloatIn(0.5f, 1.5f),
                amplitudeY = baseAmplitudeY * random.nextFloatIn(0.5f, 1.5f),
                periodXMs = periodXMs,
                periodYMs = periodYMs,
                phaseXMs = random.nextInt(periodXMs),
                phaseYMs = random.nextInt(periodYMs),
            )
        }
    }
}

private fun Random.nextFloatIn(from: Float, until: Float): Float = from + nextFloat() * (until - from)

/**
 * Fraction of one mesh cell a drifting vertex's amplitude is built from, before the per-vertex
 * 0.5×–1.5× randomization. At 0.3 the largest swing (≈0.45 of a cell) still stays clear of the
 * neighbouring vertex, on a coarse grid or a dense one alike.
 */
private const val ORGANIC_DRIFT_CELL_FRACTION = 0.3f

/** One full swing out and back, in milliseconds, before per-vertex randomization spreads it out. */
private const val ORGANIC_DRIFT_PERIOD_MS = 2500

private data class VertexColorDrift(val hueShiftDegrees: State<Float>)

/**
 * One vertex's colour drift: its hue swings by [VertexColorDriftParams.amplitudeDegrees] to
 * either side of whatever hue was picked for it, at its own speed and starting phase, the same
 * independence [rememberVertexDrift] gives position. Saturation, value and alpha are left alone —
 * only the hue moves — so a vertex never drifts into grey or fades out on its own.
 */
@Composable
private fun rememberVertexColorDrift(
    infiniteTransition: InfiniteTransition,
    row: Int,
    column: Int,
): VertexColorDrift {
    val params = remember(row, column) { VertexColorDriftParams.forVertex(row, column) }
    val hueShift = infiniteTransition.animateFloat(
        initialValue = -params.amplitudeDegrees,
        targetValue = params.amplitudeDegrees,
        animationSpec = infiniteRepeatable(
            animation = tween(params.periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(params.phaseMs, StartOffsetType.FastForward),
        ),
        label = "colorDriftHue",
    )
    return VertexColorDrift(hueShift)
}

private class VertexColorDriftParams(
    val amplitudeDegrees: Float,
    val periodMs: Int,
    val phaseMs: Int,
) {
    companion object {
        fun forVertex(row: Int, column: Int): VertexColorDriftParams {
            // A different mix from the position seed, so a vertex's colour doesn't peak in step
            // with its own position swing.
            val random = Random(row * 104_729 + column * 7_919 + 53)
            val periodMs = (COLOR_DRIFT_PERIOD_MS * random.nextFloatIn(0.7f, 1.4f)).roundToInt()
            return VertexColorDriftParams(
                amplitudeDegrees = COLOR_DRIFT_AMPLITUDE_DEGREES * random.nextFloatIn(0.5f, 1.5f),
                periodMs = periodMs,
                phaseMs = random.nextInt(periodMs),
            )
        }
    }
}

/** Rotates [this] around the HSV colour wheel by [degrees], keeping saturation, value and alpha. */
private fun Color.withHueShift(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[0] = (hsv[0] + degrees).mod(360f)
    val alpha255 = (alpha * 255f).roundToInt().coerceIn(0, 255)
    return Color(android.graphics.Color.HSVToColor(alpha255, hsv))
}

/** How far, in degrees around the colour wheel, a drifting vertex's hue swings either way. */
private const val COLOR_DRIFT_AMPLITUDE_DEGREES = 20f

/** One full swing out and back, in milliseconds, before per-vertex randomization spreads it out. */
private const val COLOR_DRIFT_PERIOD_MS = 3200
