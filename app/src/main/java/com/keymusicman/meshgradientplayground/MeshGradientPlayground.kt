package com.keymusicman.meshgradientplayground

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.paint
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.keymusicman.meshgradientplayground.ui.theme.MeshGradientPlaygroundTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshGradientPlayground() {
    var rows by remember { mutableIntStateOf(DEFAULT_ROWS) }
    var columns by remember { mutableIntStateOf(DEFAULT_COLUMNS) }
    var hasBicubicColor by remember { mutableStateOf(true) }
    var showVertices by remember { mutableStateOf(true) }
    var vertices by remember { mutableStateOf(createVertices(rows, columns)) }
    var selectedVertexIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var isMenuExpanded by remember { mutableStateOf(false) }
    var isSaveDialogVisible by remember { mutableStateOf(false) }
    var areSavedMeshesVisible by remember { mutableStateOf(false) }
    var isFullScreen by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val store = remember(context) { MeshStore(context) }
    val coroutineScope = rememberCoroutineScope()
    var savedMeshes by remember { mutableStateOf<List<MeshPreset>>(emptyList()) }
    LaunchedEffect(store) { savedMeshes = store.load() }

    fun resizeMesh(newRows: Int = rows, newColumns: Int = columns) {
        if (newRows == rows && newColumns == columns) return
        rows = newRows
        columns = newColumns
        vertices = createVertices(rows, columns)
        selectedVertexIndices = emptySet()
    }

    fun updateSelectedVertices(transform: (MeshVertex) -> MeshVertex) {
        if (selectedVertexIndices.isEmpty()) return
        vertices = vertices.mapIndexed { index, vertex ->
            if (index in selectedVertexIndices) transform(vertex) else vertex
        }
    }

    fun updateSavedMeshes(meshes: List<MeshPreset>) {
        savedMeshes = meshes
        coroutineScope.launch { store.save(meshes) }
    }

    fun openMesh(preset: MeshPreset) {
        rows = preset.rows
        columns = preset.columns
        hasBicubicColor = preset.hasBicubicColor
        vertices = preset.vertices
        selectedVertexIndices = emptySet()
    }

    val painter = rememberMeshPainter(rows, columns, hasBicubicColor, vertices)

    // Captured when the menu item is tapped, so what gets shared is what was on screen at that
    // moment rather than whatever the layer holds once the share sheet is up.
    val exportLayer = rememberGraphicsLayer()

    fun shareAsPng() {
        if (exportLayer.size.width <= 0 || exportLayer.size.height <= 0) {
            Toast.makeText(context, "Nothing to share yet", Toast.LENGTH_SHORT).show()
            return
        }
        coroutineScope.launch {
            val uri = exportLayer.toImageBitmap().toShareablePngUri(context)
            if (uri == null) {
                Toast.makeText(context, "Could not write the PNG", Toast.LENGTH_SHORT).show()
                return@launch
            }
            context.startActivity(
                Intent.createChooser(sharePngIntent(uri), "Share mesh gradient"),
            )
        }
    }

    fun sharePainterCode() {
        context.startActivity(
            Intent.createChooser(
                sharePainterCodeIntent(painterCode(rows, columns, hasBicubicColor, vertices)),
                "Share painter code",
            ),
        )
    }

    if (isFullScreen) {
        FullScreenMesh(painter = painter, onExit = { isFullScreen = false })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh gradient playground") },
                actions = {
                    IconButton(onClick = { isMenuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(
                        expanded = isMenuExpanded,
                        onDismissRequest = { isMenuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Save as…") },
                            onClick = {
                                isMenuExpanded = false
                                isSaveDialogVisible = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share as PNG") },
                            onClick = {
                                isMenuExpanded = false
                                shareAsPng()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Share painter code") },
                            onClick = {
                                isMenuExpanded = false
                                sharePainterCode()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Full screen") },
                            onClick = {
                                isMenuExpanded = false
                                isFullScreen = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Saved meshes") },
                            onClick = {
                                isMenuExpanded = false
                                areSavedMeshesVisible = true
                            },
                        )
                    }
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            MeshCanvas(
                painter = painter,
                vertices = vertices,
                showVertices = showVertices,
                selectedVertexIndices = selectedVertexIndices,
                onVertexClick = { index ->
                    selectedVertexIndices = if (index in selectedVertexIndices) {
                        selectedVertexIndices - index
                    } else {
                        selectedVertexIndices + index
                    }
                },
                onVertexDrag = { index, position ->
                    vertices = vertices.mapIndexed { vertexIndex, vertex ->
                        if (vertexIndex == index) vertex.copy(position = position) else vertex
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                captureLayer = exportLayer,
            )
            MeshSettings(
                modifier = Modifier.weight(1f),
                rows = rows,
                columns = columns,
                hasBicubicColor = hasBicubicColor,
                showVertices = showVertices,
                selectedVertex = selectedVertexIndices.firstOrNull()?.let(vertices::get),
                selectedVertexCount = selectedVertexIndices.size,
                areAllVerticesSelected = selectedVertexIndices.size == vertices.size,
                onRowsChange = { resizeMesh(newRows = it) },
                onColumnsChange = { resizeMesh(newColumns = it) },
                onHasBicubicColorChange = { hasBicubicColor = it },
                onShowVerticesChange = { showVertices = it },
                onPositionChange = { value -> updateSelectedVertices { it.copy(position = value) } },
                onColorChange = { value -> updateSelectedVertices { it.copy(color = value) } },
                onLeftControlPointChange = { value ->
                    updateSelectedVertices { it.copy(leftControlPoint = value) }
                },
                onTopControlPointChange = { value ->
                    updateSelectedVertices { it.copy(topControlPoint = value) }
                },
                onRightControlPointChange = { value ->
                    updateSelectedVertices { it.copy(rightControlPoint = value) }
                },
                onBottomControlPointChange = { value ->
                    updateSelectedVertices { it.copy(bottomControlPoint = value) }
                },
                onSelectAllToggle = {
                    selectedVertexIndices = if (selectedVertexIndices.size == vertices.size) {
                        emptySet()
                    } else {
                        vertices.indices.toSet()
                    }
                },
            )
        }
    }

    if (isSaveDialogVisible) {
        SaveMeshDialog(
            onSave = { name ->
                isSaveDialogVisible = false
                updateSavedMeshes(
                    savedMeshes + MeshPreset(
                        name = name,
                        rows = rows,
                        columns = columns,
                        hasBicubicColor = hasBicubicColor,
                        vertices = vertices,
                    ),
                )
            },
            onDismiss = { isSaveDialogVisible = false },
        )
    }

    if (areSavedMeshesVisible) {
        SavedMeshesSheet(
            meshes = savedMeshes,
            onOpen = { preset ->
                openMesh(preset)
                areSavedMeshesVisible = false
            },
            onDelete = { preset -> updateSavedMeshes(savedMeshes - preset) },
            onDismiss = { areSavedMeshesVisible = false },
        )
    }
}

/**
 * The mesh on its own: no top bar, no settings sheet, no vertex markers, and no system bars. Back
 * is the only way out, hence the toast.
 */
@Composable
private fun FullScreenMesh(painter: MeshGradientPainter, onExit: () -> Unit) {
    val view = LocalView.current
    val context = LocalContext.current
    DisposableEffect(view) {
        val window = context.findActivity()?.window
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
        Toast.makeText(context, "Press back to exit full screen", Toast.LENGTH_SHORT).show()
        onDispose { insetsController?.show(WindowInsetsCompat.Type.systemBars()) }
    }
    BackHandler(onBack = onExit)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .paint(painter)
            .semantics { contentDescription = "Full screen mesh" },
    )
}

@Composable
internal fun MeshCanvas(
    painter: MeshGradientPainter,
    vertices: List<MeshVertex>,
    showVertices: Boolean,
    selectedVertexIndices: Set<Int>,
    onVertexClick: (Int) -> Unit,
    onVertexDrag: (Int, Offset) -> Unit,
    modifier: Modifier = Modifier,
    captureLayer: GraphicsLayer? = null,
) {
    BoxWithConstraints(
        modifier = modifier
            .drawWithContent {
                // Recorded from the painter rather than from the content, so that the export holds
                // the gradient alone — no vertex markers.
                captureLayer?.record { with(painter) { draw(size) } }
                drawContent()
            }
            .paint(painter),
    ) {
        val markerSize = 32.dp
        // The travel of a marker across the canvas, i.e. the distance covered by a full 0..1 offset.
        val trackSize = with(LocalDensity.current) {
            Size(
                width = (maxWidth - markerSize).toPx().coerceAtLeast(1f),
                height = (maxHeight - markerSize).toPx().coerceAtLeast(1f),
            )
        }
        if (showVertices) {
            vertices.forEachIndexed { index, vertex ->
                key(index) {
                    val isSelected = index in selectedVertexIndices
                    val currentPosition by rememberUpdatedState(vertex.position)
                    // Unclamped position of the gesture in progress, so that dragging past an edge
                    // and back does not shift the marker away from the finger.
                    var dragPosition by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        modifier = Modifier
                            .offset(
                                x = (maxWidth - markerSize) * vertex.position.x,
                                y = (maxHeight - markerSize) * vertex.position.y,
                            )
                            .size(markerSize)
                            .background(vertex.color, CircleShape)
                            .border(
                                width = if (isSelected) 4.dp else 2.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                shape = CircleShape,
                            )
                            .semantics {
                                contentDescription = "Vertex ${vertex.row}, ${vertex.column}"
                            }
                            .clickable { onVertexClick(index) }
                            .pointerInput(trackSize) {
                                detectDragGestures(
                                    onDragStart = { dragPosition = currentPosition },
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragPosition += Offset(
                                        x = dragAmount.x / trackSize.width,
                                        y = dragAmount.y / trackSize.height,
                                    )
                                    onVertexDrag(
                                        index,
                                        Offset(
                                            x = dragPosition.x.coerceIn(0f, 1f),
                                            y = dragPosition.y.coerceIn(0f, 1f),
                                        ),
                                    )
                                }
                            },
                    )
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Preview(showBackground = true)
@Composable
private fun MeshGradientPlaygroundPreview() {
    MeshGradientPlaygroundTheme(dynamicColor = false) {
        MeshGradientPlayground()
    }
}
