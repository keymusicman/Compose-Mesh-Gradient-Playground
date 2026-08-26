package com.keymusicman.meshgradientplayground

import android.content.Intent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PainterCodeTest {

    @Test
    fun theCodeConstructsAPainterForTheMesh() {
        val code = painterCode(
            rows = 1,
            columns = 2,
            hasBicubicColor = true,
            vertices = listOf(
                MeshVertex(
                    row = 0,
                    column = 1,
                    position = Offset(0.5f, 0f),
                    color = Color(0xFF13D8B5),
                ),
            ),
        )

        assertEquals(
            """
            MeshGradientPainter(
                rows = 1,
                columns = 2,
                hasBicubicColor = true,
            ) {
                setVertex(
                    row = 0,
                    column = 1,
                    position = Offset(0.5f, 0f),
                    color = Color(0xFF13D8B5),
                )
            }
            """.trimIndent(),
            code,
        )
    }

    @Test
    fun onlySpecifiedControlPointsAreWrittenOut() {
        val code = painterCode(
            rows = 1,
            columns = 1,
            hasBicubicColor = false,
            vertices = listOf(
                MeshVertex(
                    row = 0,
                    column = 0,
                    position = Offset.Zero,
                    color = Color.Red,
                    leftControlPoint = Offset(-0.33f, 0f),
                    bottomControlPoint = Offset(0f, 0.33f),
                ),
            ),
        )

        assertTrue(code, code.contains("leftControlPoint = Offset(-0.33f, 0f),"))
        assertTrue(code, code.contains("bottomControlPoint = Offset(0f, 0.33f),"))
        assertFalse(code, code.contains("rightControlPoint"))
        assertFalse(code, code.contains("topControlPoint"))
    }

    @Test
    fun draggedPositionsAreRoundedToReadableLiterals() {
        val code = painterCode(
            rows = 1,
            columns = 1,
            hasBicubicColor = true,
            vertices = listOf(MeshVertex(0, 0, Offset(0.43728914f, 1f), Color.Red)),
        )

        assertTrue(code, code.contains("position = Offset(0.4373f, 1f),"))
    }

    @Test
    fun everyVertexBecomesASetVertexCall() {
        val code = painterCode(2, 2, true, createVertices(rows = 2, columns = 2))

        assertEquals(9, code.split("setVertex(").size - 1)
    }

    @Test
    fun theShareIntentCarriesTheCodeAsText() {
        val intent = sharePainterCodeIntent("MeshGradientPainter(")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("text/plain", intent.type)
        assertEquals("MeshGradientPainter(", intent.getStringExtra(Intent.EXTRA_TEXT))
    }
}
