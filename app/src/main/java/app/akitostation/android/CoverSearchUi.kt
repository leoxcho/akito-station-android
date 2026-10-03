package app.akitostation.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

const val CoverPrivacy = "Online artwork search contacts Libretro Thumbnails. The system/platform is sent to retrieve a catalog; matching your game title happens on this device. Viewing or downloading a cover sends that cover's title and platform in its URL, along with your IP address. No ROM contents, hashes, saves, filesystem paths, credentials or unrelated library information are sent. Permission is saved and can be disabled in Settings."
@Composable fun CoverSearchDialog(game: Game, initialTitle: String, system: Platform, model: StationViewModel, dismiss: () -> Unit) {
 var query by remember { mutableStateOf(initialTitle) }
 var all by remember { mutableStateOf(system == Platform.UNKNOWN) }
 var results by remember { mutableStateOf(emptyList<CoverResult>()) }
 var selected by remember { mutableStateOf<CoverResult?>(null) }
 var busy by remember { mutableStateOf(false) }
 var message by remember { mutableStateOf("Search Libretro Thumbnails or refine the title.") }
 val scope = rememberCoroutineScope()
 val state by model.state.collectAsState()
 Dialog(onDismissRequest = { if(!busy) dismiss() }) {
  Surface(shape = MaterialTheme.shapes.large) {
   Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
    Text("Search Cover Art", style = MaterialTheme.typography.titleLarge)
    Text(system.title)
    OutlinedTextField(query, { query = it }, label = { Text("Cover search title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
    Row { Checkbox(all, { all = it }, enabled = !busy); Text("All systems (other editions)") }
    Button(enabled = !busy && state.onlineArtwork && query.isNotBlank(), onClick = { scope.launch {
     busy = true; selected = null; results = emptyList(); message = "Searching…"
     try { results = model.searchCovers(query, system, all); message = if(results.isEmpty()) "No covers found. Try a shorter title, all systems or a local image." else "Select a cover, then Apply cover." }
     catch(e: CancellationException) { throw e } catch(e: Exception) { message = e.message ?: "Network unavailable. Try again or import a local image." } finally { busy = false }
    } }) { Text("Search") }
    Text(message, style = MaterialTheme.typography.bodySmall)
    if(busy) LinearProgressIndicator(Modifier.fillMaxWidth())
    LazyVerticalGrid(columns = GridCells.Adaptive(110.dp), modifier = Modifier.weight(1f, fill = false).heightIn(min = 120.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     if(state.onlineArtwork) items(results, key = { it.url }) { result ->
      OutlinedButton(onClick = { selected = result }, enabled = !busy, contentPadding = PaddingValues(6.dp)) {
       Column { AsyncImage(result.url, result.title, Modifier.fillMaxWidth().height(140.dp), contentScale = ContentScale.Fit)
        Text((if(selected == result) "Selected · " else "") + result.title + " · " + result.system.title, maxLines = 3) }
      }
     }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
     Button(enabled = selected != null && !busy && state.onlineArtwork, onClick = { scope.launch {
      busy = true
      try { model.applyCover(game, selected!!); dismiss() } catch(e: CancellationException) { throw e } catch(e: Exception) { message = e.message ?: "Could not save cover" } finally { busy = false }
     } }) { Text("Apply cover") }
     TextButton(onClick = dismiss, enabled = !busy) { Text("Done") }
    }
   }
  }
 }
}
