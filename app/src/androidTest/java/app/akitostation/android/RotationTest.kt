package app.akitostation.android
import android.content.pm.ActivityInfo
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
@OptIn(ExperimentalTestApi::class)
class RotationTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun runningRotationPreservesLibraryFiltersAndScroll() {
  val root = "rotation-fixture"
  StationSettings(compose.activity).sort = SortOrder.TITLE
  val db = LibraryDatabase(compose.activity)
  db.mergeRoot(root, (0 until 120).map { Game("rotation-$it", "content://rotation/$it", root, "Rotation %03d".format(it), Platform.NES, favorite = true) }); db.games().filter { it.root == root && !it.favorite }.forEach(db::favorite); db.close()
  compose.activityRule.scenario.recreate()
  compose.waitUntil(10000) { compose.onAllNodesWithText("Rotation 000").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("Search games or systems").performTextInput("Rotation")
  compose.onNode(hasText("Filters") or hasContentDescription("Filters")).performClick()
  compose.onNodeWithText("Favorites").performClick()
  compose.onNode(hasText("Filters") or hasContentDescription("Filters")).performClick()
  for(preset in LibraryDensity.entries) {
   compose.onNodeWithText("Grid · ${StationSettings(compose.activity).density.label}").performClick()
   compose.onNodeWithText(preset.label).performClick()
   compose.onNode(hasScrollToIndexAction()).performScrollToIndex(60)
   for(orientation in listOf(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)) {
    InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("wm user-rotation lock ${if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) 1 else 0}").use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes() }
    compose.waitUntil(10000) { compose.activity.resources.configuration.orientation == if(orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) 2 else 1 }
    compose.waitForIdle()
    compose.onNode(hasScrollToIndexAction()).assertIsDisplayed()
    assertTrue("Rotation must retain the scrolled region", (57..62).any { id -> runCatching { compose.onNodeWithTag("game-rotation-$id").assertIsDisplayed() }.isSuccess })
    val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
    val folder = java.io.File(compose.activity.filesDir, "validation").apply { mkdirs() }
    automation.takeScreenshot()?.let { bitmap -> java.io.File(folder, "${compose.activity.resources.configuration.screenWidthDp}x${compose.activity.resources.configuration.screenHeightDp}-${preset.name}.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }; bitmap.recycle() }

    compose.onNode(hasSetTextAction()).assertTextContains("Rotation")
    assertEquals(preset, StationSettings(compose.activity).density)
    compose.onNode(hasText("Filters") or hasContentDescription("Filters")).performClick()
    compose.onNodeWithText("Favorites").assertIsSelected()
    compose.onNode(hasText("Filters") or hasContentDescription("Filters")).performClick()
   }
  }
  LibraryDatabase(compose.activity).use { it.removeRoot(root) }
  InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("wm user-rotation free").close()

 }
}
