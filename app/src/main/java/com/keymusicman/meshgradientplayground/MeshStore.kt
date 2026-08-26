package com.keymusicman.meshgradientplayground

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Saved meshes, kept as a single JSON file.
 *
 * The absence of [file] is what marks a fresh install, so [defaults] are written out on the first
 * [load] and behave like ordinary saved meshes from then on — deleting them all leaves the
 * collection empty instead of seeding it again.
 */
internal class MeshStore(
    private val file: File,
    private val defaults: List<MeshPreset> = DEFAULT_MESHES,
) {
    suspend fun load(): List<MeshPreset> = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            return@withContext defaults.also { write(it) }
        }
        try {
            decodeMeshPresets(file.readText())
        } catch (_: IOException) {
            emptyList()
        }
    }

    suspend fun save(presets: List<MeshPreset>) {
        withContext(Dispatchers.IO) { write(presets) }
    }

    private fun write(presets: List<MeshPreset>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(encodeMeshPresets(presets))
        } catch (_: IOException) {
            // A playground mesh is not worth crashing over; the collection stays in memory.
        }
    }
}

internal fun MeshStore(context: Context): MeshStore =
    MeshStore(File(context.filesDir, "meshes.json"))
