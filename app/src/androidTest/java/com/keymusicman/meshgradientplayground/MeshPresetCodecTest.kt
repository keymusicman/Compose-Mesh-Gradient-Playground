package com.keymusicman.meshgradientplayground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The codec is exercised on a device because it is built on `org.json`, which is stubbed out in
 * local JVM unit tests.
 */
@RunWith(AndroidJUnit4::class)
class MeshPresetCodecTest {

    @Test
    fun decodeRestoresEveryFieldOfAnEncodedPreset() {
        val preset = MeshPreset(
            id = "preset-1",
            name = "Sunset",
            rows = 2,
            columns = 3,
            hasBicubicColor = false,
            vertices = listOf(
                MeshVertex(
                    row = 0,
                    column = 1,
                    position = Offset(0.25f, 0.5f),
                    color = Color(red = 0x12, green = 0xAB, blue = 0x34, alpha = 0x80),
                    leftControlPoint = Offset(0.1f, 0.2f),
                    topControlPoint = Offset(0.3f, 0.4f),
                    rightControlPoint = Offset(0.6f, 0.7f),
                    bottomControlPoint = Offset(0.8f, 0.9f),
                ),
                MeshVertex(
                    row = 1,
                    column = 2,
                    position = Offset(1f, 0f),
                    color = Color.White,
                ),
            ),
        )

        val decoded = decodeMeshPresets(encodeMeshPresets(listOf(preset)))

        assertEquals(listOf(preset), decoded)
    }

    @Test
    fun unspecifiedControlPointsStayUnspecified() {
        val preset = MeshPreset(
            name = "No control points",
            rows = 1,
            columns = 1,
            hasBicubicColor = true,
            vertices = createVertices(rows = 1, columns = 1),
        )

        val vertex = decodeMeshPresets(encodeMeshPresets(listOf(preset))).single().vertices.first()

        assertEquals(Offset.Unspecified, vertex.leftControlPoint)
        assertEquals(Offset.Unspecified, vertex.topControlPoint)
        assertEquals(Offset.Unspecified, vertex.rightControlPoint)
        assertEquals(Offset.Unspecified, vertex.bottomControlPoint)
    }

    @Test
    fun decodeReturnsNoPresetsForMalformedJson() {
        assertEquals(emptyList<MeshPreset>(), decodeMeshPresets("}not json{"))
    }

    @Test
    fun decodeSkipsPresetsThatAreNotObjects() {
        val encoded = encodeMeshPresets(
            listOf(MeshPreset("Kept", 1, 1, true, createVertices(1, 1))),
        )

        val decoded = decodeMeshPresets(encoded.replace("\"meshes\":[", "\"meshes\":[7,"))

        assertEquals(listOf("Kept"), decoded.map { it.name })
    }
}
