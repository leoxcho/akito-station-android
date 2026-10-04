package app.akitostation.android

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable internal fun ScrapeSetup(allowed: Boolean, enable: () -> Unit, start: (ScrapeOptions) -> Unit, dismiss: () -> Unit) {
 var mode by remember { mutableStateOf(ScrapeMode.MISSING) }
 var replace by remember { mutableStateOf(false) }
 AlertDialog(onDismissRequest = dismiss, title = { Text("Scrape Box Art") }, text = {
  Column(Modifier.verticalScroll(rememberScrollState())) {
   if(!allowed) { Text("Bulk scraping requires enabling online artwork search."); Text(CoverPrivacy) }
   ScrapeMode.entries.forEach { option -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(mode == option, { mode = option }); Text(if(option == ScrapeMode.MISSING) "Missing Artwork Only" else "All Games") } }
   if(mode == ScrapeMode.ALL) {
    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(!replace, { replace = false }); Text("Keep existing artwork") }
    Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(replace, { replace = true }); Text("Replace existing artwork") }
   }
   Text("Libretro Thumbnails · exact title and system matches. Unmatched games stay unchanged. Completed artwork survives cancellation.")
  }
 }, confirmButton = { Button(onClick = { if(allowed) start(ScrapeOptions(mode, replace)) else enable() }) { Text(if(allowed) "Start scrape" else "Allow online artwork search") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
@Composable internal fun ScrapeProgressDialog(progress: ScrapeProgress, cancel: () -> Unit, dismiss: () -> Unit, review: () -> Unit) {
 AlertDialog(onDismissRequest = { if(progress.complete) dismiss() }, title = { Text(if(progress.cancelled) "Box Art Scrape Cancelled" else if(progress.complete) "Box Art Scrape Complete" else "Scraping Box Art") }, text = {
  Column(Modifier.verticalScroll(rememberScrollState())) {
   Text("${progress.scanned} / ${progress.total} games")
   if(!progress.complete) { LinearProgressIndicator(progress = { if(progress.total == 0) 0f else progress.scanned.toFloat() / progress.total }); Text("Current: ${progress.current}") }
   Text("Scanned: ${progress.scanned}\nArtwork Added: ${progress.added}\nAlready Had Artwork: ${progress.skipped}\nNot Found: ${progress.missing.size - progress.errors}\nErrors: ${progress.errors}")
   if(progress.complete && progress.missing.isNotEmpty()) TextButton(onClick = review) { Text("Review Missing Artwork") }
  }
 }, confirmButton = { TextButton(onClick = { if(progress.complete) dismiss() else cancel() }) { Text(if(progress.complete) "Done" else "Cancel scrape") } })
}
