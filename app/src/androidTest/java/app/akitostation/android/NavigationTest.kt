package app.akitostation.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
@OptIn(androidx.compose.ui.test.ExperimentalTestApi::class)
class NavigationTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun startupAndNavigation() {
  compose.onNodeWithText("Akito Station").assertIsDisplayed()
  compose.onNodeWithText("Your next adventure starts here").assertIsDisplayed()
  compose.onNodeWithText("Choose library folder").assertIsDisplayed()
  compose.onNodeWithText("Consoles").performClick()
  compose.onNodeWithText("Consoles & emulators").assertIsDisplayed()
  compose.onNodeWithText("Settings").performClick()
  compose.onNodeWithText("Library & storage").assertIsDisplayed()
  compose.onNodeWithText("Games").performClick()
  compose.onNodeWithText("Your collection").assertIsDisplayed()
 }
 @Test fun simpleAndAdvancedEmulatorWorkflow() {
  compose.onNodeWithText("Consoles").performClick()
  compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("DuckStation · Not Installed"))
  compose.onNodeWithText("DuckStation · Not Installed").assertIsDisplayed()
  compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
  compose.onNodeWithText("Add emulator").performScrollTo().performClick()
  compose.onNodeWithText("Select Console").assertIsDisplayed()
  compose.onNodeWithText("Select Installed Emulator").assertIsDisplayed()
  compose.onNodeWithText("Package (org.example.emulator)").assertDoesNotExist()
  compose.onNodeWithText("Advanced / Custom Emulator").performClick()
  compose.onNodeWithText("Package (org.example.emulator)").assertIsDisplayed()
  compose.onNodeWithText("Name").performTextInput("Custom test")
  compose.onNodeWithText("Package (org.example.emulator)").performTextInput("org.example.testemulator")
  compose.onNodeWithText("Add system").performScrollTo().performClick()
  compose.onNodeWithText("Save").performClick()
  compose.waitUntil(5000) { StationSettings(compose.activity).custom().any { it.name == "Custom test" } }
  StationSettings(compose.activity).custom().filter { it.name == "Custom test" }.forEach { StationSettings(compose.activity).remove(it.id) }
  compose.onNodeWithText("Cancel").performClick()
 }
 @Test fun controllerFocusThroughConsolesAndAddEmulator() {
  compose.onNodeWithText("Consoles").performClick()
  compose.onNodeWithText("Add emulator").performClick()
  compose.onNodeWithText("Select Console").assertIsDisplayed()
  compose.onNodeWithText("Cancel").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionDown) }
  compose.onNodeWithText("Cancel").performKeyInput { pressKey(androidx.compose.ui.input.key.Key.DirectionUp) }
  compose.onNodeWithText("Cancel").performClick()
  compose.onNodeWithText("Consoles & emulators").assertIsDisplayed()
 }
 @Test fun searchAndFilter() {
  compose.onNodeWithText("Search games or systems").performTextInput("test")
  compose.onNodeWithText("Favorites").performClick()
  compose.onNodeWithText("Recently launched").performScrollTo().performClick()
  compose.onNodeWithText("All games").performClick()
  compose.onNodeWithText("Search games or systems").assertTextContains("test")
 }
 @Test fun libraryMetadataFavoriteAndMissingRuntime() {
  val db = LibraryDatabase(compose.activity)
  val game = Game("synthetic-test-record", "content://missing-provider/test", "synthetic-test-root", "Synthetic record", Platform.NES)
  db.mergeRoot(game.root, listOf(game)); db.close()
  compose.activityRule.scenario.recreate()
  compose.waitUntil(5000) { compose.onAllNodesWithText("Synthetic record").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("Synthetic record").performClick()
  compose.onNodeWithText("Favorite").performClick()
  compose.onNodeWithText("Game title").performTextReplacement("Edited record")
  compose.onNodeWithText("Save metadata").performClick()
  compose.onNodeWithText("Done").performClick()
  compose.onNodeWithText("Favorites").performClick()
  compose.waitUntil(5000) { compose.onAllNodesWithText("Edited record").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("Edited record").performClick()
  compose.onNodeWithText("Play").performClick()
  compose.waitUntil(5000) { compose.onAllNodes(hasText("Choose an emulator", substring = true)).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("OK").performClick()
  val verify = LibraryDatabase(compose.activity)
  val saved = verify.games().first { it.id == game.id }
  org.junit.Assert.assertTrue(saved.favorite)
  org.junit.Assert.assertEquals("Edited record", saved.displayTitle)
  org.junit.Assert.assertEquals(0L, saved.lastLaunched)
  verify.removeRoot(game.root); verify.close()
 }
 @Test fun artworkConsentCancelAndControllerNavigation() {
  val db = LibraryDatabase(compose.activity)
  val game = Game("art-consent-record", "content://missing-provider/art", "art-test-root", "Artwork consent game", Platform.GBA)
  db.mergeRoot(game.root, listOf(game)); db.close()
  StationSettings(compose.activity).onlineArtwork = false
  compose.activityRule.scenario.recreate()
  compose.waitUntil(5000) { compose.onAllNodesWithText(game.title).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText(game.title).performClick()
  compose.onNodeWithText("Search Cover Art").performScrollTo().performClick()
  compose.onNodeWithText("Online cover-art privacy").assertIsDisplayed()
  compose.onNodeWithText("Cancel").performClick()
  org.junit.Assert.assertFalse(StationSettings(compose.activity).onlineArtwork)
  compose.onNodeWithText("Search Cover Art").performClick()
  compose.onNodeWithText("Allow and continue").performClick()
  compose.waitUntil(5000) { StationSettings(compose.activity).onlineArtwork }
  compose.onNodeWithText("Cover search title").assertTextContains(game.title)
  compose.onNodeWithText("Apply cover").assertIsNotEnabled()
  compose.onNodeWithText("Done").performClick()
  compose.onNodeWithText("Change cover").assertExists()
  androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_DPAD_DOWN)
  androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BUTTON_B)
  LibraryDatabase(compose.activity).use { it.removeRoot(game.root) }
  StationSettings(compose.activity).onlineArtwork = false
 }
 @Test fun liveCoverSearchApplyAndImmediateLibraryRefresh() {
  val game = Game("live-cover-record", "content://missing-provider/live", "live-cover-root", "Pokemon Emerald Version", Platform.GBA)
  LibraryDatabase(compose.activity).use { it.mergeRoot(game.root, listOf(game)) }
  StationSettings(compose.activity).onlineArtwork = true
  compose.activityRule.scenario.recreate()
  compose.waitUntil(5000) { compose.onAllNodesWithText(game.title).fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText(game.title).performClick()
  compose.onNodeWithText("Search Cover Art").performScrollTo().performClick()
  compose.onNodeWithText("Search", substring = false).performClick()
  compose.waitUntil(60000) { compose.onAllNodes(hasText("Pokemon - Emerald Version", substring = true)).fetchSemanticsNodes().isNotEmpty() }
  compose.onAllNodes(hasText("Pokemon - Emerald Version", substring = true))[0].performClick()
  compose.onNodeWithText("Apply cover").performClick()
  compose.waitUntil(30000) { LibraryDatabase(compose.activity).use { db -> db.games().first { it.id == game.id }.artwork.isNotBlank() } }
  compose.waitUntil(5000) { compose.onAllNodesWithText("Game title").fetchSemanticsNodes().isNotEmpty() }
  compose.onNodeWithText("Done").performClick()
  compose.onNodeWithContentDescription("Cover for " + game.title).assertExists()
  LibraryDatabase(compose.activity).use { db ->
   val saved = db.games().first { it.id == game.id }; org.junit.Assert.assertTrue(java.io.File(saved.artwork).exists())
   org.junit.Assert.assertNotNull(android.graphics.BitmapFactory.decodeFile(saved.artwork)); db.removeRoot(game.root)
  }
  StationSettings(compose.activity).onlineArtwork = false
 }
}
