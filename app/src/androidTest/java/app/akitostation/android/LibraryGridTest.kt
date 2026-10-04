package app.akitostation.android

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
@OptIn(ExperimentalTestApi::class)
class LibraryGridTest {
 @get:Rule val compose = createComposeRule()
 private val games = (0 until 10000).map { Game("grid-$it", "content://grid/$it", "grid", "Game $it", Platform.NES, favorite = true, lastLaunched = 1) }
 @Test fun everyPresetImmediateSwitchAndLargeLibraryScrolling() {
  var density by mutableStateOf(LibraryDensity.FOUR)
  compose.setContent { MaterialTheme { Column { GridSizeSelector(density, { density = it }); LibraryGrid(games, density, Modifier.weight(1f)) {} } } }
  for(preset in LibraryDensity.entries) {
   compose.onNodeWithText("Grid · ${density.label}").performClick()
   compose.onNodeWithText(preset.label, substring = false).performClick()
   compose.onNodeWithText("Grid · ${preset.label}").assertIsDisplayed()
   compose.onNode(hasScrollToIndexAction()).performScrollToIndex(9999)
   compose.onNodeWithText("Game 9999").assertIsDisplayed()
   compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
  }
 }
 @Test fun densityChangeRestoresFocusedGameAndDpadMovesFocus() {
  var density by mutableStateOf(LibraryDensity.FOUR)
  compose.setContent {
   val input = androidx.compose.ui.platform.LocalInputModeManager.current
   SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
   MaterialTheme { LibraryGrid(games.take(20), density) {} }
  }
  compose.onNodeWithText("Game 0").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
  compose.onNodeWithText("Game 0").assertIsFocused()
  compose.runOnIdle { density = LibraryDensity.SIX }
  compose.waitForIdle()
  compose.onNodeWithText("Game 0").assertIsFocused()
  compose.onNodeWithText("Game 0").performKeyInput { pressKey(Key.DirectionDown) }
  compose.onNodeWithText("Game 0").assertIsNotFocused()
 }
 @Test fun sixDensityCardsDoNotOverlap() {
  compose.setContent { MaterialTheme { LibraryGrid(games.take(20), LibraryDensity.SIX) {} } }
  val nodes = compose.onAllNodes(hasClickAction()).fetchSemanticsNodes()
  assertTrue(nodes.size > 1)
  for((i, a) in nodes.withIndex()) for(b in nodes.drop(i + 1)) {
   val x = minOf(a.boundsInRoot.right, b.boundsInRoot.right) - maxOf(a.boundsInRoot.left, b.boundsInRoot.left)
   val y = minOf(a.boundsInRoot.bottom, b.boundsInRoot.bottom) - maxOf(a.boundsInRoot.top, b.boundsInRoot.top)
   assertFalse(x > 0 && y > 0)
  }
 }
 @Test fun gridSelectorSupportsDpadAndSelect() {
  var density by mutableStateOf(LibraryDensity.FOUR)
  var selections = 0
  compose.setContent {
   val input = androidx.compose.ui.platform.LocalInputModeManager.current
   SideEffect { input.requestInputMode(androidx.compose.ui.input.InputMode.Keyboard) }
   MaterialTheme { GridSizeSelector(density, { density = it; selections++ }) }
  }
  compose.onNodeWithText("Grid · 4×4").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus)
  compose.onNodeWithText("Grid · 4×4").performKeyInput { pressKey(Key.Enter) }
  compose.onNodeWithText("2×2").assertIsDisplayed()
  compose.onNodeWithText("2×2").performKeyInput { pressKey(Key.DirectionDown); pressKey(Key.Enter) }
  compose.runOnIdle { assertEquals(1, selections) }
  compose.onNodeWithText("Grid · ${density.label}").assertIsDisplayed()
 }
}
