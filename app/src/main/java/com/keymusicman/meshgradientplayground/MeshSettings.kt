package com.keymusicman.meshgradientplayground

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The editing panel, split in two: the mesh as a whole on one tab, the selected vertex on the
 * other. Only one tab is composed at a time, so each keeps its own scroll position.
 */
@Composable
internal fun MeshSettings(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    showVertices: Boolean,
    hasOrganicDrift: Boolean,
    selectedVertex: MeshVertex?,
    selectedVertexCount: Int,
    areAllVerticesSelected: Boolean,
    onRowsChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onHasBicubicColorChange: (Boolean) -> Unit,
    onShowVerticesChange: (Boolean) -> Unit,
    onHasOrganicDriftChange: (Boolean) -> Unit,
    onPositionChange: (Offset) -> Unit,
    onColorChange: (Color) -> Unit,
    onLeftControlPointChange: (Offset) -> Unit,
    onTopControlPointChange: (Offset) -> Unit,
    onRightControlPointChange: (Offset) -> Unit,
    onBottomControlPointChange: (Offset) -> Unit,
    onSelectAllToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(MESH_TAB) }
    val hasSelection = selectedVertex != null
    // Picking a vertex on the canvas brings its editors up, so that a tap still does something
    // visible while the mesh tab is the one on screen.
    LaunchedEffect(hasSelection) {
        if (hasSelection) selectedTab = VERTEX_TAB
    }

    Column(modifier = modifier.fillMaxWidth()) {
        PrimaryTabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == MESH_TAB,
                onClick = { selectedTab = MESH_TAB },
                text = { Text("Mesh") },
            )
            Tab(
                selected = selectedTab == VERTEX_TAB,
                onClick = { selectedTab = VERTEX_TAB },
                text = { Text("Vertex") },
            )
        }
        // Checkboxes and sliders both reserve a 48.dp touch target by default, which is a lot of
        // dead vertical space in a panel this dense. Both tabs are edited with a thumb, not tapped
        // one-handed across a whole screen, so a smaller minimum is a reasonable trade here.
        CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides COMPACT_TOUCH_TARGET) {
            when (selectedTab) {
                MESH_TAB -> MeshTab(
                    rows = rows,
                    columns = columns,
                    hasBicubicColor = hasBicubicColor,
                    showVertices = showVertices,
                    hasOrganicDrift = hasOrganicDrift,
                    onRowsChange = onRowsChange,
                    onColumnsChange = onColumnsChange,
                    onHasBicubicColorChange = onHasBicubicColorChange,
                    onShowVerticesChange = onShowVerticesChange,
                    onHasOrganicDriftChange = onHasOrganicDriftChange,
                )
                else -> VertexTab(
                    rows = rows,
                    columns = columns,
                    selectedVertex = selectedVertex,
                    selectedVertexCount = selectedVertexCount,
                    areAllVerticesSelected = areAllVerticesSelected,
                    onPositionChange = onPositionChange,
                    onColorChange = onColorChange,
                    onLeftControlPointChange = onLeftControlPointChange,
                    onTopControlPointChange = onTopControlPointChange,
                    onRightControlPointChange = onRightControlPointChange,
                    onBottomControlPointChange = onBottomControlPointChange,
                    onSelectAllToggle = onSelectAllToggle,
                )
            }
        }
    }
}

@Composable
private fun MeshTab(
    rows: Int,
    columns: Int,
    hasBicubicColor: Boolean,
    showVertices: Boolean,
    hasOrganicDrift: Boolean,
    onRowsChange: (Int) -> Unit,
    onColumnsChange: (Int) -> Unit,
    onHasBicubicColorChange: (Boolean) -> Unit,
    onShowVerticesChange: (Boolean) -> Unit,
    onHasOrganicDriftChange: (Boolean) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { MeshDimensionSlider("Rows", rows, onRowsChange) }
        item { MeshDimensionSlider("Columns", columns, onColumnsChange) }
        val checkboxes = listOf(
            MeshCheckboxItem("Bicubic color", hasBicubicColor, onHasBicubicColorChange),
            MeshCheckboxItem("Show vertices", showVertices, onShowVerticesChange),
            MeshCheckboxItem("Organic drift", hasOrganicDrift, onHasOrganicDriftChange),
        )
        items(checkboxes, key = { it.label }) { checkbox ->
            SettingCheckbox(checkbox.label, checkbox.checked, checkbox.onCheckedChange)
        }
        if (hasOrganicDrift && (rows < 2 || columns < 2)) {
            item {
                Text(
                    text = "Only interior vertices drift, and this mesh has none yet — corners " +
                        "and edges stay put. Try 2 rows and 2 columns or more.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun VertexTab(
    rows: Int,
    columns: Int,
    selectedVertex: MeshVertex?,
    selectedVertexCount: Int,
    areAllVerticesSelected: Boolean,
    onPositionChange: (Offset) -> Unit,
    onColorChange: (Color) -> Unit,
    onLeftControlPointChange: (Offset) -> Unit,
    onTopControlPointChange: (Offset) -> Unit,
    onRightControlPointChange: (Offset) -> Unit,
    onBottomControlPointChange: (Offset) -> Unit,
    onSelectAllToggle: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when {
                        selectedVertex == null -> "Select a vertex"
                        selectedVertexCount == 1 ->
                            "Vertex ${selectedVertex.row}, ${selectedVertex.column}"
                        else -> "$selectedVertexCount vertices selected"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                TextButton(onClick = onSelectAllToggle) {
                    Text(if (areAllVerticesSelected) "Unselect all" else "Select all")
                }
            }
        }

        if (selectedVertex == null) {
            item {
                Text(
                    text = "Tap one on the canvas to edit its position, color and control " +
                        "points. Tap again to unselect, and pick several to edit them together.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (selectedVertex != null) {
            item { OffsetEditor("Position", selectedVertex.position, onPositionChange) }
            item { ColorEditor(selectedVertex.color, onColorChange) }
            // One cell in each direction; the renderer's own inferred tangent is a third of that.
            val limit = Offset(x = 1f / columns, y = 1f / rows)
            val tangent = limit * INFERRED_TANGENT_FRACTION
            item {
                ControlPointEditor(
                    label = "Left control point",
                    value = selectedVertex.leftControlPoint,
                    neutral = Offset(-tangent.x, 0f),
                    limit = limit,
                    onValueChange = onLeftControlPointChange,
                )
            }
            item {
                ControlPointEditor(
                    label = "Top control point",
                    value = selectedVertex.topControlPoint,
                    neutral = Offset(0f, -tangent.y),
                    limit = limit,
                    onValueChange = onTopControlPointChange,
                )
            }
            item {
                ControlPointEditor(
                    label = "Right control point",
                    value = selectedVertex.rightControlPoint,
                    neutral = Offset(tangent.x, 0f),
                    limit = limit,
                    onValueChange = onRightControlPointChange,
                )
            }
            item {
                ControlPointEditor(
                    label = "Bottom control point",
                    value = selectedVertex.bottomControlPoint,
                    neutral = Offset(0f, tangent.y),
                    limit = limit,
                    onValueChange = onBottomControlPointChange,
                )
            }
        }
    }
}

private const val MESH_TAB = 0
private const val VERTEX_TAB = 1

private data class MeshCheckboxItem(
    val label: String,
    val checked: Boolean,
    val onCheckedChange: (Boolean) -> Unit,
)

@Composable
private fun SettingCheckbox(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeshDimensionSlider(label: String, value: Int, onValueChange: (Int) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors()
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("$label: $value", style = MaterialTheme.typography.labelLarge)
        Slider(
            value = value.toFloat(),
            onValueChange = { newValue ->
                val roundedValue = newValue.roundToInt()
                if (roundedValue != value) onValueChange(roundedValue)
            },
            colors = colors,
            interactionSource = interactionSource,
            valueRange = MIN_MESH_SIZE.toFloat()..MAX_MESH_SIZE.toFloat(),
            steps = MAX_MESH_SIZE - MIN_MESH_SIZE - 1,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    thumbSize = COMPACT_SLIDER_THUMB_SIZE,
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = colors,
                    drawStopIndicator = null,
                )
            },
        )
    }
}

@Composable
private fun OffsetEditor(
    label: String,
    value: Offset,
    onValueChange: (Offset) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "$label: ${value.x.formatOffset()}, ${value.y.formatOffset()}",
            style = MaterialTheme.typography.labelLarge,
        )
        OffsetPad(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.72f)
                .aspectRatio(1f),
        )
    }
}

/**
 * A control point is a tangent *offset from* the vertex, not a position, so it needs a pad centred
 * on zero that reaches into negative territory — a left or top tangent is negative by nature.
 *
 * @param neutral seeded when the control point is switched on: the tangent the renderer would have
 *   inferred anyway, so enabling it leaves the gradient looking the same.
 * @param limit how far the pad reaches on each axis, one mesh cell in either direction.
 */
@Composable
private fun ControlPointEditor(
    label: String,
    value: Offset,
    neutral: Offset,
    limit: Offset,
    onValueChange: (Offset) -> Unit,
) {
    val isSpecified = value.isSpecified
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isSpecified) {
                    "$label: ${value.x.formatOffset()}, ${value.y.formatOffset()}"
                } else {
                    "$label: Unspecified"
                },
                style = MaterialTheme.typography.labelLarge,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Specified", style = MaterialTheme.typography.labelSmall)
                Checkbox(
                    checked = isSpecified,
                    onCheckedChange = { checked ->
                        onValueChange(if (checked) neutral else Offset.Unspecified)
                    },
                    modifier = Modifier.semantics { contentDescription = "Specify $label" },
                )
            }
        }
        if (isSpecified) {
            OffsetPad(
                value = value,
                onValueChange = onValueChange,
                xRange = -limit.x..limit.x,
                yRange = -limit.y..limit.y,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .fillMaxWidth(0.72f)
                    .aspectRatio(1f),
            )
            Text(
                text = "Offset from the vertex. Unspecified lets the renderer infer a smooth " +
                    "tangent from the neighbouring vertices.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OffsetPad(
    value: Offset,
    onValueChange: (Offset) -> Unit,
    modifier: Modifier = Modifier,
    xRange: ClosedFloatingPointRange<Float> = 0f..1f,
    yRange: ClosedFloatingPointRange<Float> = 0f..1f,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val markerColor = MaterialTheme.colorScheme.primary
    val borderColor = MaterialTheme.colorScheme.outline
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .pointerInput(xRange, yRange) {
                val inset = OFFSET_PAD_INSET.toPx()
                val coordinateWidth = size.width - inset * 2
                val coordinateHeight = size.height - inset * 2
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.firstOrNull { it.pressed }?.let { change ->
                            currentOnValueChange(
                                Offset(
                                    x = xRange.valueAt((change.position.x - inset) / coordinateWidth),
                                    y = yRange.valueAt((change.position.y - inset) / coordinateHeight),
                                ),
                            )
                            change.consume()
                        }
                    }
                }
            },
    ) {
        val inset = OFFSET_PAD_INSET.toPx()
        val coordinateSize = Size(size.width - inset * 2, size.height - inset * 2)
        drawRect(borderColor, Offset(inset, inset), coordinateSize, style = Stroke(1.dp.toPx()))
        for (step in 1 until OFFSET_GRID_DIVISIONS) {
            val fraction = step / OFFSET_GRID_DIVISIONS.toFloat()
            drawLine(
                gridColor,
                Offset(inset + coordinateSize.width * fraction, inset),
                Offset(inset + coordinateSize.width * fraction, inset + coordinateSize.height),
            )
            drawLine(
                gridColor,
                Offset(inset, inset + coordinateSize.height * fraction),
                Offset(inset + coordinateSize.width, inset + coordinateSize.height * fraction),
            )
        }
        // Zero is the value that matters on a control point pad, so give its axes some weight.
        if (xRange.spansZero()) {
            val x = inset + coordinateSize.width * xRange.fractionOf(0f)
            drawLine(borderColor, Offset(x, inset), Offset(x, inset + coordinateSize.height), 2.dp.toPx())
        }
        if (yRange.spansZero()) {
            val y = inset + coordinateSize.height * yRange.fractionOf(0f)
            drawLine(borderColor, Offset(inset, y), Offset(inset + coordinateSize.width, y), 2.dp.toPx())
        }
        val markerPosition = Offset(
            inset + xRange.fractionOf(value.x) * coordinateSize.width,
            inset + yRange.fractionOf(value.y) * coordinateSize.height,
        )
        drawCircle(Color.Black.copy(alpha = 0.5f), 13.dp.toPx(), markerPosition)
        drawCircle(markerColor, 9.dp.toPx(), markerPosition)
        drawCircle(Color.White, 9.dp.toPx(), markerPosition, style = Stroke(2.dp.toPx()))
    }
}

private fun ClosedFloatingPointRange<Float>.fractionOf(value: Float): Float =
    ((value - start) / (endInclusive - start)).coerceIn(0f, 1f)

private fun ClosedFloatingPointRange<Float>.valueAt(fraction: Float): Float =
    start + (endInclusive - start) * fraction.coerceIn(0f, 1f)

private fun ClosedFloatingPointRange<Float>.spansZero(): Boolean = start < 0f && endInclusive > 0f

@Composable
private fun ColorEditor(color: Color, onColorChange: (Color) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("Color", style = MaterialTheme.typography.labelLarge)
        ColorSwatch(color)
        ColorSlider("Red", color.red, Color.Red) { onColorChange(color.copy(red = it)) }
        ColorSlider("Green", color.green, Color.Green) { onColorChange(color.copy(green = it)) }
        ColorSlider("Blue", color.blue, Color.Blue) { onColorChange(color.copy(blue = it)) }
        ColorSlider("Alpha", color.alpha, MaterialTheme.colorScheme.onSurface) {
            onColorChange(color.copy(alpha = it))
        }
    }
}

@Composable
private fun ColorSwatch(color: Color) {
    val lightSquare = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val darkSquare = MaterialTheme.colorScheme.surface
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        val squareSize = 16.dp.toPx()
        var row = 0
        var y = 0f
        while (y < size.height) {
            var column = 0
            var x = 0f
            while (x < size.width) {
                drawRect(
                    if ((row + column) % 2 == 0) lightSquare else darkSquare,
                    Offset(x, y),
                    Size(squareSize, squareSize),
                )
                x += squareSize
                column++
            }
            y += squareSize
            row++
        }
        drawRect(color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    sliderColor: Color,
    onValueChange: (Float) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors(
        activeTrackColor = sliderColor,
        inactiveTrackColor = sliderColor.copy(alpha = 0.2f),
        thumbColor = sliderColor,
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            if (label == "Alpha") "$label: ${(value * 100).roundToInt()}%"
            else "$label: ${(value * 255).roundToInt()}",
            style = MaterialTheme.typography.labelSmall,
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = colors,
            interactionSource = interactionSource,
            thumb = {
                SliderDefaults.Thumb(
                    interactionSource = interactionSource,
                    colors = colors,
                    thumbSize = COMPACT_SLIDER_THUMB_SIZE,
                )
            },
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    modifier = Modifier.height(2.dp),
                    colors = colors,
                    drawStopIndicator = null,
                )
            },
        )
    }
}

private fun Float.formatOffset(): String = ((this * 100).roundToInt() / 100f).toString()

/** Matches the tangent length the renderer infers for an unspecified control point. */
private const val INFERRED_TANGENT_FRACTION = 0.33f
private const val OFFSET_GRID_DIVISIONS = 4
private val OFFSET_PAD_INSET = 16.dp
private val COMPACT_SLIDER_THUMB_SIZE = DpSize(12.dp, 12.dp)

/**
 * Replaces Material's default 48.dp minimum touch target for checkboxes and sliders in this
 * panel. Still comfortably tappable, just without the extra padding that made a stack of them so
 * tall.
 */
private val COMPACT_TOUCH_TARGET = 32.dp
