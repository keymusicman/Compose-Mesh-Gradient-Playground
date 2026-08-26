package com.keymusicman.meshgradientplayground

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshCaptureTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun theCaptureLeavesOutTheVertexMarkers() {
        val image = captureMesh(showVertices = true)

        // Every vertex of the mesh is red, so the whole gradient is red. The markers are the only
        // thing that could put a white border ring in the picture.
        val pixels = image.toPixelMap()
        for (x in 0 until minOf(120, image.width)) {
            for (y in 0 until minOf(120, image.height)) {
                val pixel = pixels[x, y]
                assertTrue(
                    "marker pixel left in the capture at $x, $y: $pixel",
                    pixel.green < 0.5f && pixel.blue < 0.5f,
                )
            }
        }
    }

    @Test
    fun theCaptureIsTheSizeOfTheCanvas() {
        val image = captureMesh(showVertices = false)

        assertEquals(rule.density.run { 200.dp.roundToPx() }, image.width)
        assertEquals(rule.density.run { 200.dp.roundToPx() }, image.height)
    }

    private fun captureMesh(showVertices: Boolean) = runBlocking {
        lateinit var layer: GraphicsLayer
        val vertices = createVertices(rows = 1, columns = 1).map { it.copy(color = Color.Red) }
        rule.setContent {
            layer = rememberGraphicsLayer()
            MeshCanvas(
                painter = rememberMeshPainter(1, 1, hasBicubicColor = false, vertices = vertices),
                vertices = vertices,
                showVertices = showVertices,
                selectedVertexIndices = emptySet(),
                onVertexClick = {},
                onVertexDrag = { _, _ -> },
                captureLayer = layer,
                modifier = Modifier.size(200.dp),
            )
        }
        rule.waitForIdle()
        layer.toImageBitmap()
    }
}
