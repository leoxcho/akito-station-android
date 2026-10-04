package app.akitostation.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect

@Composable fun GridSizeSelector(density: LibraryDensity, select: (LibraryDensity) -> Unit, modifier: Modifier = Modifier) {
 var expanded by remember { mutableStateOf(false) }
 Box(modifier) {
  TextButton(onClick = { expanded = true }) { Text("Grid · ${density.label}") }
  DropdownMenu(expanded, { expanded = false }) {
   LibraryDensity.entries.forEach { preset -> DropdownMenuItem(text = { Text(preset.label) }, onClick = { select(preset); expanded = false }) }
  }
 }
}
@Composable internal fun LibraryGrid(games: List<Game>, density: LibraryDensity, modifier: Modifier = Modifier, select: (Game) -> Unit) {
 val gridState = rememberLazyGridState()
 var focusedId by rememberSaveable { mutableStateOf<String?>(null) }
 var restoreId by remember { mutableStateOf<String?>(null) }
 val requester = remember { FocusRequester() }
 val fontScale = LocalDensity.current.fontScale
 LaunchedEffect(density) {
  val id = focusedId
  val index = games.indexOfFirst { it.id == id }
  if(index >= 0) { restoreId = id; gridState.scrollToItem(index) }
 }
 BoxWithConstraints(modifier.fillMaxWidth()) {
  val columns = density.columns(maxWidth.value, fontScale)
  var previousWidth by rememberSaveable { mutableStateOf(maxWidth.value) }
  var anchorId by rememberSaveable { mutableStateOf<String?>(null) }
  var adapting by remember { mutableStateOf(previousWidth != maxWidth.value) }
  LaunchedEffect(maxWidth.value) {
   if(previousWidth != maxWidth.value) {
    adapting = true
    val index = games.indexOfFirst { it.id == anchorId }
    if(index >= 0) gridState.scrollToItem(index)
    previousWidth = maxWidth.value
    adapting = false
   }
  }
  LaunchedEffect(gridState) {
   snapshotFlow { gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.offset.y + it.size.height > 0 }?.key as? String }
    .collect { id -> if(!adapting && id != null) anchorId = id }
  }
  LazyVerticalGrid(columns = GridCells.Fixed(columns), state = gridState, contentPadding = PaddingValues(20.dp),
   horizontalArrangement = Arrangement.spacedBy(20.dp), verticalArrangement = Arrangement.spacedBy(24.dp), modifier = Modifier.fillMaxSize().testTag("library-grid-$columns")) {
   items(games, key = { it.id }, contentType = { "game" }) { game ->
    val restoring = restoreId == game.id
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
    GameCard(game, Modifier.widthIn(max = (density.targetWidth * fontScale.coerceAtLeast(1f)).dp).fillMaxWidth().then(if(restoring) Modifier.focusRequester(requester) else Modifier).onFocusChanged { if(it.isFocused) focusedId = game.id }) { select(game) }
    }
    if(restoring) LaunchedEffect(game.id, density) { requester.requestFocus(); restoreId = null }
   }
  }
 }
}
