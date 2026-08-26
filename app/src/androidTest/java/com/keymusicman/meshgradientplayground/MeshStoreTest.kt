package com.keymusicman.meshgradientplayground

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshStoreTest {

    private lateinit var file: File

    private val defaults = listOf(
        MeshPreset("Default one", 1, 1, true, createVertices(1, 1)),
        MeshPreset("Default two", 2, 2, false, createVertices(2, 2)),
    )

    @Before
    fun setUp() {
        val cacheDir = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        file = File(cacheDir, "mesh-store-test/meshes.json")
        file.parentFile?.deleteRecursively()
    }

    @After
    fun tearDown() {
        file.parentFile?.deleteRecursively()
    }

    @Test
    fun firstLoadSeedsTheDefaultMeshes() = runBlocking {
        val loaded = MeshStore(file, defaults).load()

        assertEquals(defaults.map { it.name }, loaded.map { it.name })
    }

    @Test
    fun seededMeshesKeepTheirIdsAcrossLoads() = runBlocking {
        val store = MeshStore(file, defaults)

        val seeded = store.load()
        val reloaded = store.load()

        assertEquals(seeded, reloaded)
    }

    @Test
    fun savedMeshesAreReadBackByANewStore() = runBlocking {
        val saved = listOf(MeshPreset("Mine", 3, 2, false, createVertices(3, 2)))

        MeshStore(file, defaults).save(saved)

        assertEquals(saved, MeshStore(file, defaults).load())
    }

    @Test
    fun deletingEveryMeshDoesNotBringTheDefaultsBack() = runBlocking {
        val store = MeshStore(file, defaults)
        store.load()

        store.save(emptyList())

        assertEquals(emptyList<MeshPreset>(), MeshStore(file, defaults).load())
    }

    @Test
    fun anUnreadableFileLoadsAsNoMeshes() = runBlocking {
        file.parentFile?.mkdirs()
        file.writeText("}not json{")

        assertTrue(MeshStore(file, defaults).load().isEmpty())
    }
}
