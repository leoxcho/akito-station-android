package app.akitostation.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
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
}
