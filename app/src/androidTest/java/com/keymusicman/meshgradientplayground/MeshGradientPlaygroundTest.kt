package com.keymusicman.meshgradientplayground

import android.app.KeyguardManager
import android.os.PowerManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.espresso.Espresso
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MeshGradientPlaygroundTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun aSavedMeshShowsUpInTheSavedMeshesList() {
        saveCurrentMeshAs("Test mesh")

        openSavedMeshes()

        scrollSavedMeshesTo("Test mesh")
        rule.onNodeWithText("Test mesh").assertIsDisplayed()
        delete("Test mesh")
    }

    @Test
    fun aNameIsRequiredToSave() {
        openMenu()
        rule.onNodeWithText("Save as…").performClick()

        rule.onNodeWithText("Save").assertIsNotEnabled()

        rule.onNode(hasSetTextAction()).performTextInput("Named at last")
        rule.onNodeWithText("Save").assertIsEnabled()
        rule.onNodeWithText("Cancel").performClick()
    }

    @Test
    fun aDeletedMeshLeavesTheSavedMeshesList() {
        saveCurrentMeshAs("Doomed mesh")
        openSavedMeshes()

        scrollSavedMeshesTo("Doomed mesh")
        delete("Doomed mesh")

        rule.onNodeWithText("Doomed mesh").assertDoesNotExist()
    }

    @Test
    fun theBundledDefaultMeshIsInTheSavedMeshes() {
        openSavedMeshes()

        scrollSavedMeshesTo(DEFAULT_MESHES.first().name)
        rule.onNodeWithText(DEFAULT_MESHES.first().name).assertIsDisplayed()
    }

    @Test
    fun fullScreenHidesTheChromeAndBackBringsItBack() {
        openMenu()
        rule.onNodeWithText("Full screen").performClick()

        rule.onNodeWithContentDescription("Full screen mesh").assertIsDisplayed()
        rule.onNodeWithText("Mesh gradient playground").assertDoesNotExist()
        rule.onNodeWithContentDescription("Vertex 0, 0").assertDoesNotExist()
        rule.onNodeWithText("Rows: 1").assertDoesNotExist()

        Espresso.pressBack()

        rule.onNodeWithText("Mesh gradient playground").assertIsDisplayed()
        rule.onNodeWithContentDescription("Vertex 0, 0").assertExists()
    }

    @Test
    fun draggingAVertexMovesIt() {
        rule.onNodeWithContentDescription("Vertex 0, 0").performClick()
        val before = positionLabel()
        assertEquals("Position: 0.0, 0.0", before)

        rule.onNodeWithContentDescription("Vertex 0, 0").performTouchInput {
            swipe(start = center, end = center + Offset(300f, 300f), durationMillis = 200)
        }

        assertNotEquals(before, positionLabel())
    }

    @Test
    fun selectAllSelectsEveryVertex() {
        scrollSettingsTo(hasText("Select all"))

        rule.onNodeWithText("Select all").performClick()

        // The default 1x1 mesh has a vertex at each of the four grid corners.
        rule.onNodeWithText("4 vertices selected").assertExists()
    }

    @Test
    fun unselectAllClearsTheSelection() {
        scrollSettingsTo(hasText("Select all"))
        rule.onNodeWithText("Select all").performClick()

        rule.onNodeWithText("Unselect all").performClick()

        rule.onNodeWithText("Select a vertex").assertExists()
    }

    @Test
    fun enablingAControlPointSeedsTheTangentTheRendererWouldHaveInferred() {
        rule.onNodeWithContentDescription("Vertex 0, 0").performClick()
        scrollSettingsTo(hasText("Left control point", substring = true))

        rule.onNodeWithContentDescription("Specify Left control point").performClick()

        // A 1x1 mesh spans the whole bounds, so the inferred tangent is a third of it, pointing
        // left — not the (0.5, 0.5) a plain position editor would seed.
        assertEquals("Left control point: -0.33, 0.0", labelStartingWith("Left control point"))
    }

    /**
     * The settings are a lazy list, so the editor has to be scrolled into view before it exists to
     * read at all.
     */
    private fun positionLabel(): String = labelStartingWith("Position:")

    private fun labelStartingWith(prefix: String): String {
        val label = hasText(prefix, substring = true)
        scrollSettingsTo(label)
        return rule.onNode(label).fetchSemanticsNode().config[SemanticsProperties.Text].first().text
    }

    /** The saved meshes are a lazy list too, and it is as long as the device's collection. */
    private fun scrollSavedMeshesTo(name: String) {
        rule.onNodeWithTag(SAVED_MESHES_TAG).performScrollToNode(hasText(name))
    }

    private fun scrollSettingsTo(matcher: SemanticsMatcher) {
        rule.onNode(hasScrollToNodeAction()).performScrollToNode(matcher)
    }

    /**
     * Only meaningful on an awake, unlocked device: with the screen off the system bars are never
     * reported visible, so the assertions would pass and fail for the wrong reasons.
     */
    @Test
    fun fullScreenHidesTheSystemBarsUntilBack() {
        val context = rule.activity
        assumeTrue(context.getSystemService(PowerManager::class.java).isInteractive)
        assumeFalse(context.getSystemService(KeyguardManager::class.java).isKeyguardLocked)

        openMenu()
        rule.onNodeWithText("Full screen").performClick()

        rule.waitUntil(timeoutMillis = 5_000) { !areSystemBarsVisible() }

        Espresso.pressBack()

        rule.waitUntil(timeoutMillis = 5_000) { areSystemBarsVisible() }
    }

    private fun areSystemBarsVisible(): Boolean {
        val insets = ViewCompat.getRootWindowInsets(rule.activity.window.decorView) ?: return true
        // Not Type.systemBars(): that composite also covers the caption bar, which a phone never
        // shows, so it reads as hidden even while the status and navigation bars are up.
        return insets.isVisible(WindowInsetsCompat.Type.statusBars()) ||
            insets.isVisible(WindowInsetsCompat.Type.navigationBars())
    }

    private fun openMenu() {
        rule.onNodeWithContentDescription("Menu").performClick()
    }

    private fun openSavedMeshes() {
        openMenu()
        rule.onNodeWithText("Saved meshes").performClick()
    }

    private fun saveCurrentMeshAs(name: String) {
        openMenu()
        rule.onNodeWithText("Save as…").performClick()
        rule.onNode(hasSetTextAction()).performTextInput(name)
        rule.onNodeWithText("Save").performClick()
    }

    /** Also keeps the device's own collection clean between runs. */
    private fun delete(name: String) {
        rule.onNodeWithContentDescription("Delete $name").performClick()
    }
}
