package app.akitostation.android
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
class GridPreferenceTest {
 @get:Rule val compose = createAndroidComposeRule<MainActivity>()
 @Test fun allDensitiesPersistAcrossRecreationFiltersAndSettings() {
  for(preset in LibraryDensity.entries) {
   compose.onNodeWithText("View / Grid Size", substring = true).performClick()
   compose.onNodeWithText(preset.label, substring = false).performClick()
   compose.onNodeWithText("View / Grid Size · ${preset.label}").assertIsDisplayed()
   compose.onNodeWithText("Search games or systems").performTextReplacement("test")
   compose.onNodeWithText("Favorites").performClick()
   compose.onNodeWithText("Recently launched").performScrollTo().performClick()
   compose.onNodeWithText("All games").performClick()
   compose.onNode(hasText("All systems") or hasText("Game Boy Advance")).performClick()
   compose.onNode(hasText("Game Boy Advance") and hasAnyAncestor(isPopup())).performClick()
   compose.onNodeWithText("View / Grid Size · ${preset.label}").assertIsDisplayed()
   compose.activityRule.scenario.recreate()
   compose.waitUntil(5000) { compose.onAllNodesWithText("View / Grid Size · ${preset.label}").fetchSemanticsNodes().isNotEmpty() }
   assertEquals(preset, StationSettings(compose.activity).density)
   compose.onNodeWithText("Search games or systems").performTextClearance()
  }
  compose.onNodeWithText("Settings").performClick()
  compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("View / Grid Size", substring = true))
  compose.onNodeWithText("View / Grid Size · 6×6").assertIsDisplayed()
  StationSettings(compose.activity).density = LibraryDensity.FOUR
 }
}
