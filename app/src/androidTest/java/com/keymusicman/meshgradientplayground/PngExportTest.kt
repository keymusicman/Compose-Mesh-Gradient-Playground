package com.keymusicman.meshgradientplayground

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.compose.ui.graphics.asImageBitmap
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
class PngExportTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(context.cacheDir, "png-export-test/mesh.png")
        file.parentFile?.deleteRecursively()
        file.parentFile?.mkdirs()
    }

    @After
    fun tearDown() {
        file.parentFile?.deleteRecursively()
    }

    @Test
    fun theWrittenPngKeepsTheImageSize() {
        val image = filled(width = 12, height = 7, color = Color.RED)

        val written = runBlocking { image.writePngTo(context, Uri.fromFile(file)) }

        assertTrue(written)
        val decoded = BitmapFactory.decodeFile(file.path)
        assertEquals(12, decoded.width)
        assertEquals(7, decoded.height)
        assertEquals(Color.RED, decoded.getPixel(6, 3))
    }

    @Test
    fun theWrittenPngKeepsTranslucency() {
        val translucent = Color.argb(0x80, 0x20, 0x40, 0x60)

        runBlocking { filled(4, 4, translucent).writePngTo(context, Uri.fromFile(file)) }

        val decoded = BitmapFactory.decodeFile(file.path)
        assertEquals(translucent, decoded.getPixel(2, 2))
    }

    @Test
    fun theSharedPngIsReadableThroughAContentUri() {
        val uri = runBlocking { filled(20, 10, Color.BLUE).toShareablePngUri(context) }

        assertEquals("content", uri?.scheme)
        val decoded = context.contentResolver.openInputStream(uri!!).use {
            BitmapFactory.decodeStream(it)
        }
        assertEquals(20, decoded.width)
        assertEquals(10, decoded.height)
    }

    @Test
    fun theShareIntentHandsOutReadAccessToTheImage() {
        val uri = runBlocking { filled(4, 4, Color.BLUE).toShareablePngUri(context) }!!

        val intent = sharePngIntent(uri)

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("image/png", intent.type)
        assertEquals(uri, intent.getParcelableExtra(Intent.EXTRA_STREAM))
        assertTrue(
            "the receiving app cannot read the image without the grant flag",
            intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0,
        )
    }

    private fun filled(width: Int, height: Int, color: Int) =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            .apply { eraseColor(color) }
            .asImageBitmap()
}
