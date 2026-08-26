package com.keymusicman.meshgradientplayground

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * JSON codec for saved meshes. Decoding is deliberately lenient: a file we cannot make sense of
 * costs the user their saved meshes, but it should never crash the app on launch.
 */
internal fun encodeMeshPresets(presets: List<MeshPreset>): String {
    val meshes = JSONArray()
    presets.forEach { preset ->
        val vertices = JSONArray()
        preset.vertices.forEach { vertices.put(it.toJson()) }
        meshes.put(
            JSONObject().apply {
                put(KEY_ID, preset.id)
                put(KEY_NAME, preset.name)
                put(KEY_ROWS, preset.rows)
                put(KEY_COLUMNS, preset.columns)
                put(KEY_BICUBIC_COLOR, preset.hasBicubicColor)
                put(KEY_VERTICES, vertices)
            },
        )
    }
    return JSONObject().apply {
        put(KEY_VERSION, CODEC_VERSION)
        put(KEY_MESHES, meshes)
    }.toString()
}

internal fun decodeMeshPresets(json: String): List<MeshPreset> {
    val meshes = try {
        JSONObject(json).optJSONArray(KEY_MESHES)
    } catch (_: JSONException) {
        null
    } ?: return emptyList()

    return buildList {
        for (index in 0 until meshes.length()) {
            val mesh = meshes.optJSONObject(index) ?: continue
            add(
                MeshPreset(
                    id = mesh.optString(KEY_ID).ifEmpty { UUID.randomUUID().toString() },
                    name = mesh.optString(KEY_NAME),
                    rows = mesh.optInt(KEY_ROWS, DEFAULT_ROWS),
                    columns = mesh.optInt(KEY_COLUMNS, DEFAULT_COLUMNS),
                    hasBicubicColor = mesh.optBoolean(KEY_BICUBIC_COLOR, true),
                    vertices = mesh.optJSONArray(KEY_VERTICES).toMeshVertices(),
                ),
            )
        }
    }
}

private fun MeshVertex.toJson(): JSONObject = JSONObject().apply {
    put(KEY_ROW, row)
    put(KEY_COLUMN, column)
    put(KEY_X, position.x.toDouble())
    put(KEY_Y, position.y.toDouble())
    put(KEY_COLOR, color.toArgb())
    putControlPoint(KEY_LEFT, leftControlPoint)
    putControlPoint(KEY_TOP, topControlPoint)
    putControlPoint(KEY_RIGHT, rightControlPoint)
    putControlPoint(KEY_BOTTOM, bottomControlPoint)
}

private fun JSONArray?.toMeshVertices(): List<MeshVertex> {
    val vertices = this ?: return emptyList()
    return buildList {
        for (index in 0 until vertices.length()) {
            val vertex = vertices.optJSONObject(index) ?: continue
            add(
                MeshVertex(
                    row = vertex.optInt(KEY_ROW),
                    column = vertex.optInt(KEY_COLUMN),
                    position = Offset(
                        x = vertex.optDouble(KEY_X, 0.0).toFloat(),
                        y = vertex.optDouble(KEY_Y, 0.0).toFloat(),
                    ),
                    color = Color(vertex.optInt(KEY_COLOR)),
                    leftControlPoint = vertex.controlPoint(KEY_LEFT),
                    topControlPoint = vertex.controlPoint(KEY_TOP),
                    rightControlPoint = vertex.controlPoint(KEY_RIGHT),
                    bottomControlPoint = vertex.controlPoint(KEY_BOTTOM),
                ),
            )
        }
    }
}

/** Unspecified control points are simply left out of the JSON. */
private fun JSONObject.putControlPoint(key: String, controlPoint: Offset) {
    if (!controlPoint.isSpecified) return
    put(key, JSONArray().put(controlPoint.x.toDouble()).put(controlPoint.y.toDouble()))
}

private fun JSONObject.controlPoint(key: String): Offset {
    val controlPoint = optJSONArray(key) ?: return Offset.Unspecified
    return Offset(
        x = controlPoint.optDouble(0, 0.0).toFloat(),
        y = controlPoint.optDouble(1, 0.0).toFloat(),
    )
}

private const val CODEC_VERSION = 1
private const val KEY_VERSION = "version"
private const val KEY_MESHES = "meshes"
private const val KEY_ID = "id"
private const val KEY_NAME = "name"
private const val KEY_ROWS = "rows"
private const val KEY_COLUMNS = "columns"
private const val KEY_BICUBIC_COLOR = "hasBicubicColor"
private const val KEY_VERTICES = "vertices"
private const val KEY_ROW = "row"
private const val KEY_COLUMN = "column"
private const val KEY_X = "x"
private const val KEY_Y = "y"
private const val KEY_COLOR = "color"
private const val KEY_LEFT = "left"
private const val KEY_TOP = "top"
private const val KEY_RIGHT = "right"
private const val KEY_BOTTOM = "bottom"
