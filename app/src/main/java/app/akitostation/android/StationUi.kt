package app.akitostation.android

import android.content.Intent
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import java.text.DateFormat
import java.util.Date
import java.util.UUID

val Navy = Color(0xFF060916)
val Red = Color(0xFFFF061A)
val Cyan = Color(0xFF3DC7C2)
private val LocalAppLogo = staticCompositionLocalOf { AppLogo.AURORA }
private val StationColors = darkColorScheme(primary = Cyan, secondary = Red, background = Navy, surface = Color(0xFF16162C), surfaceVariant = Color(0xFF24223E))

@Composable fun StationApp(model: StationViewModel, controller: String) {
 val state by model.state.collectAsStateWithLifecycle()
 val configuration = LocalConfiguration.current
 val landscape = configuration.screenWidthDp > configuration.screenHeightDp
 val shortLandscape = landscape && configuration.screenHeightDp < 300
 var controlsExpanded by rememberSaveable { mutableStateOf(false) }

 var page by rememberSaveable { mutableStateOf("Games") }
 var query by rememberSaveable { mutableStateOf("") }
 var filter by rememberSaveable { mutableStateOf(LibraryFilter.ALL) }
 var system by rememberSaveable { mutableStateOf<Platform?>(null) }
 var scrapeSetup by remember { mutableStateOf(false) }
 var reviewIds by rememberSaveable { mutableStateOf<List<String>?>(null) }
 var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
 val selected = state.games.firstOrNull { it.id == selectedId }
 val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> if(uri != null) model.addRoot(uri) }
 CompositionLocalProvider(LocalAppLogo provides state.logo) {
 MaterialTheme(colorScheme = StationColors) {
  BackHandler(enabled = page != "Games" || selected != null) { if(selected != null) selectedId = null else page = "Games" }
  Scaffold(containerColor = Navy, topBar = {
   Column(Modifier.fillMaxWidth().background(Navy).statusBarsPadding().padding(horizontal = 20.dp, vertical = if(landscape) 0.dp else 4.dp)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
     Image(painterResource(state.logo.resource), "Akito Station logo", Modifier.size(if(landscape) 36.dp else 40.dp))
     if(shortLandscape) {
      OutlinedTextField(query, { query = it }, placeholder = { Text("Search games") }, singleLine = true, modifier = Modifier.weight(1f))
      IconButton(onClick = { controlsExpanded = !controlsExpanded }) { Icon(Icons.Default.Tune, "Filters") }
      GridSizeSelector(state.density, model::density)
      var navigationMenu by remember { mutableStateOf(false) }
      Box {
       IconButton(onClick = { navigationMenu = true }) { Icon(Icons.Default.MoreVert, "Navigation and library actions") }
       DropdownMenu(navigationMenu, { navigationMenu = false }) {
        listOf("Games", "Consoles", "Settings").forEach { name -> DropdownMenuItem(text = { Text(name) }, onClick = { page = name; navigationMenu = false }) }
        DropdownMenuItem(text = { Text("Add library") }, enabled = !state.busy, onClick = { folderPicker.launch(null); navigationMenu = false })
        DropdownMenuItem(text = { Text("Scrape Box Art") }, onClick = { scrapeSetup = true; navigationMenu = false })
        DropdownMenuItem(text = { Text(if(state.busy) "Cancel scan" else "Refresh library") }, onClick = { if(state.busy) model.cancelScan() else model.scan(); navigationMenu = false })
       }
      }
     }
     if(!shortLandscape && (!landscape || configuration.screenWidthDp >= 600)) Column(Modifier.padding(start = 12.dp).weight(1f)) { Text("Akito Station", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("アキト・ステーション", style = MaterialTheme.typography.labelSmall, color = Cyan) }
     if(landscape && !shortLandscape && configuration.screenWidthDp < 600) Spacer(Modifier.weight(1f))
     if(landscape && !shortLandscape) listOf("Games" to Icons.Default.GridView, "Consoles" to Icons.Default.SportsEsports, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
      IconButton(onClick = { page = name }) { Icon(icon, name, tint = if(page == name) Cyan else MaterialTheme.colorScheme.onSurface) }
     }
     if(!shortLandscape) IconButton(onClick = { folderPicker.launch(null) }, enabled = !state.busy) { Icon(Icons.Default.CreateNewFolder, "Add library") }
     if(landscape && !shortLandscape) IconButton(onClick = { if(state.busy) model.cancelScan() else model.scan() }) { Icon(if(state.busy) Icons.Default.Close else Icons.Default.Refresh, if(state.busy) "Cancel scan" else "Refresh library") }
    }
   }
  }, bottomBar = {
   if(!landscape) NavigationBar(containerColor = Navy) {
    listOf("Games" to Icons.Default.GridView, "Consoles" to Icons.Default.SportsEsports, "Settings" to Icons.Default.Settings).forEach { (name, icon) ->
     NavigationBarItem(selected = page == name, onClick = { page = name }, icon = { Icon(icon, name) }, label = { Text(name) })
    }
   }
  }) { padding ->
   Box(Modifier.fillMaxSize().padding(padding).background(Brush.linearGradient(listOf(Color(0xFF181330), Navy, Color(0xFF050917))))) {
    when(page) {
     "Games" -> Column(Modifier.fillMaxSize()) {
      if(!landscape) Text("Your collection", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
      if(!shortLandscape) Row(Modifier.fillMaxWidth().then(if(!landscape) Modifier.horizontalScroll(rememberScrollState()) else Modifier).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       if(landscape) OutlinedTextField(query, { query = it }, placeholder = { Text("Search games or systems") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.weight(1f))
       TextButton(onClick = { controlsExpanded = !controlsExpanded }) { Icon(Icons.Default.Tune, null); Text("Filters") }
       GridSizeSelector(state.density, model::density)
       IconButton(onClick = { scrapeSetup = true }, enabled = state.scrape?.complete != false) { Icon(Icons.Default.Image, "Scrape Box Art") }
      }
      if(!landscape) OutlinedTextField(query, { query = it }, label = { Text("Search games or systems") }, leadingIcon = { Icon(Icons.Default.Search, null) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
      if(controlsExpanded) {
       Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LibraryFilter.entries.forEach { f -> FilterChip(filter == f, { filter = f }, label = { Text(when(f) { LibraryFilter.ALL -> "All games"; LibraryFilter.FAVORITES -> "Favorites"; LibraryFilter.RECENT -> "Recently launched" }) }) }
        SystemPicker(system, { system = it }, allowAll = true, modifier = Modifier.width(180.dp))
        var sortMenu by remember { mutableStateOf(false) }
        Box { TextButton(onClick = { sortMenu = true }) { Icon(Icons.Default.Sort, null); Text("Sort") }; DropdownMenu(sortMenu, { sortMenu = false }) { SortOrder.entries.forEach { s -> DropdownMenuItem(text = { Text(s.name.lowercase().replaceFirstChar { it.uppercase() }) }, onClick = { model.sort(s); sortMenu = false }) } } }
       }
      }
      if(reviewIds != null) TextButton(onClick = { reviewIds = null }) { Text("Review missing artwork · Clear filter") }
      val games = remember(state.games, query, filter, system, state.sort, reviewIds) { visibleGames(state.games, query, filter, system, state.sort).filter { reviewIds == null || it.id in reviewIds!! } }
      if(state.games.isEmpty()) {
       BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
        val roomy = maxHeight >= 240.dp
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically), horizontalAlignment = Alignment.CenterHorizontally) {
         if(roomy) Icon(Icons.Default.SportsEsports, null, Modifier.size(40.dp), tint = Red)
         Text("Your next adventure starts here", style = MaterialTheme.typography.titleMedium)
         Text("Add your games. Originals stay untouched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
         Button(onClick = { folderPicker.launch(null) }, enabled = !state.busy) { Text("Choose library folder") }
        }
       }
      } else if(games.isEmpty()) { Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("No games match these filters") } }
      else LibraryGrid(games, state.density, Modifier.weight(1f)) { selectedId = it.id }
      Row(Modifier.fillMaxWidth().then(if(landscape) Modifier.height(24.dp) else Modifier).background(Color.Black.copy(alpha = .2f)).padding(horizontal = 20.dp, vertical = 0.dp), verticalAlignment = Alignment.CenterVertically) {
       if(state.busy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
       Text(state.activity, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f).padding(start = 8.dp))
       Text("${games.size} games", style = MaterialTheme.typography.labelSmall)
       if(!landscape) IconButton(onClick = { if(state.busy) model.cancelScan() else model.scan() }) { Icon(if(state.busy) Icons.Default.Close else Icons.Default.Refresh, if(state.busy) "Cancel scan" else "Refresh library") }
      }
     }
     "Consoles" -> ConsolesScreen(state, model)
     "Settings" -> SettingsScreen(state, model, controller, { folderPicker.launch(null) })
    }
   }
  }
  if(scrapeSetup) ScrapeSetup(state.onlineArtwork, { model.onlineArtwork(true) }, { options -> scrapeSetup = false; model.scrapeArtwork(options) }, { scrapeSetup = false })
  state.scrape?.let { progress -> ScrapeProgressDialog(progress, model::cancelScrape, model::dismissScrape) {
   reviewIds = progress.missing.toList(); query = ""; filter = LibraryFilter.ALL; system = null; page = "Games"; model.dismissScrape()
  } }
  if(selected != null) GameDialog(selected, state, model, { selectedId = null })
  if(state.launchGame != null) {
   var rememberChoice by remember { mutableStateOf(true) }
   AlertDialog(onDismissRequest = model::dismissLaunch, title = { Text("Choose emulator") }, text = {
    Column(Modifier.verticalScroll(rememberScrollState())) {
     Text(state.launchGame!!.system.title)
     state.launchChoices.forEach { runtime -> TextButton(onClick = { model.chooseLaunch(runtime, rememberChoice) }) { Text(runtime.name) } }
     Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(rememberChoice, { rememberChoice = it }); Text("Remember for this console") }
    }
   }, confirmButton = {}, dismissButton = { TextButton(onClick = model::dismissLaunch) { Text("Cancel") } })
  }
  if(state.message != null) AlertDialog(onDismissRequest = model::dismiss, title = { Text("Akito Station") }, text = { Text(state.message!!) }, confirmButton = { TextButton(onClick = model::dismiss) { Text("OK") } })
 }
}
}
@Composable internal fun GameCard(game: Game, modifier: Modifier = Modifier, onClick: () -> Unit) {
 Card(onClick = onClick, modifier = modifier.testTag("game-${game.id}").heightIn(min = 48.dp), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF17172D))) {
  Box(Modifier.fillMaxWidth().aspectRatio(.72f).background(Brush.verticalGradient(listOf(Color(0xFF292044), Navy))), contentAlignment = Alignment.Center) {
   if(game.artwork.isNotBlank()) AsyncImage(game.artwork, "Cover for ${game.displayTitle}", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, error = painterResource(LocalAppLogo.current.resource))
   else Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(8.dp)) { Icon(Icons.Default.SportsEsports, null, Modifier.size(32.dp), tint = Cyan.copy(alpha = .7f)); Text(game.system.title, style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp)) }
   if(game.favorite) Icon(Icons.Default.Favorite, "Favorite", tint = Red, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
  }
  Column(Modifier.padding(8.dp)) { Text(game.displayTitle, maxLines = 2, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold); Text(game.system.title, style = MaterialTheme.typography.labelSmall, color = Cyan, maxLines = 1, overflow = TextOverflow.Ellipsis) }
 }
}
@Composable fun SystemPicker(system: Platform?, onSelect: (Platform?) -> Unit, allowAll: Boolean = false, modifier: Modifier = Modifier) {
 var expanded by remember { mutableStateOf(false) }
 Box(modifier) { OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(system?.title ?: "All systems", maxLines = 1, overflow = TextOverflow.Ellipsis); Icon(Icons.Default.ArrowDropDown, null) }
  DropdownMenu(expanded, { expanded = false }) {
   if(allowAll) DropdownMenuItem(text = { Text("All systems") }, onClick = { onSelect(null); expanded = false })
   Platform.entries.forEach { p -> DropdownMenuItem(text = { Text(p.title) }, onClick = { onSelect(p); expanded = false }) }
  }
 }
}
@Composable private fun GameDialog(game: Game, state: StationState, model: StationViewModel, dismiss: () -> Unit) {
 var title by remember(game.id) { mutableStateOf(game.displayTitle) }
 var system by remember(game.id) { mutableStateOf(game.system) }
 var searching by remember { mutableStateOf(false) }
 var consent by remember { mutableStateOf(false) }
 if(searching) { CoverSearchDialog(game, title, system, model, { searching = false }); return }
 if(consent) AlertDialog(onDismissRequest = { consent = false }, title = { Text("Online cover-art privacy") }, text = { Text(CoverPrivacy) }, confirmButton = { Button(onClick = { model.onlineArtwork(true); consent = false; searching = true }) { Text("Allow and continue") } }, dismissButton = { TextButton(onClick = { consent = false }) { Text("Cancel") } })
 val artPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> if(uri != null) model.artwork(game, uri) }
 AlertDialog(onDismissRequest = dismiss, title = { Text(game.displayTitle) }, text = {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   Text(game.system.title, color = Cyan)
   Text("${game.bytes / 1024 / 1024} MB · ${if(game.lastLaunched == 0L) "Never launched" else "Last launch: " + DateFormat.getDateTimeInstance().format(Date(game.lastLaunched))}")
   val route = runtimeChoice(game.system, state.runtimes, state.installed, state.selectedRuntimes[game.system])
   Text("Emulator: " + when(route) { is RuntimeChoice.Ready -> route.runtime.name; is RuntimeChoice.Choose -> "Choose when you play"; is RuntimeChoice.Missing -> "No compatible emulator installed" })
   Text("Launch opens an external Android emulator. Saves and gameplay settings are managed by that emulator.", style = MaterialTheme.typography.bodySmall)
   OutlinedTextField(title, { title = it }, label = { Text("Game title") }, singleLine = true)
   SystemPicker(system, { if(it != null) system = it })
   TextButton(onClick = { model.metadata(game, title, system) }) { Text("Save metadata") }
   Row { TextButton(onClick = { model.favorite(game) }) { Text(if(game.favorite) "Unfavorite" else "Favorite") }; TextButton(onClick = { artPicker.launch(arrayOf("image/*")) }) { Text("Change cover") } }
   OutlinedButton(onClick = { if(state.onlineArtwork) searching = true else consent = true }) { Text("Search Cover Art") }
   TextButton(onClick = { model.resetArtwork(game) }) { Text("Remove cover") }
  }
 }, confirmButton = { Button(onClick = { model.launch(game); dismiss() }) { Icon(Icons.Default.PlayArrow, null); Text("Play") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Done") } })
}
@Composable private fun ConsolesScreen(state: StationState, model: StationViewModel) {
 var editing by remember { mutableStateOf<RuntimeConfig?>(null) }
 var adding by remember { mutableStateOf(false) }
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Text("Consoles & emulators", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("Install emulators separately. Select your preferred app for each system.", color = Cyan); Button(onClick = { adding = true }) { Text("Add emulator") } }
  items(Platform.entries.filter { it != Platform.UNKNOWN }) { platform ->
   Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
    Text(platform.title, fontWeight = FontWeight.Bold)
    Text("${state.games.count { it.system == platform }} games", style = MaterialTheme.typography.bodySmall)
    val choices = state.runtimes.filter { platform in it.systems }
    if(choices.isEmpty()) Text("No adapter configured · add a compatible Android app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    choices.forEach { runtime ->
     Row(verticalAlignment = Alignment.CenterVertically) {
      RadioButton(state.selectedRuntimes[platform] == runtime.id, { model.select(platform, runtime.id) }, enabled = runtime.id in state.installed)
      Text(runtime.name + if(runtime.id in state.installed) " · Installed" else " · Not Installed", Modifier.weight(1f))
      if(!runtime.builtIn) IconButton(onClick = { editing = runtime }) { Icon(Icons.Default.Edit, "Edit ${runtime.name}") }
     }
    }
    if(state.selectedRuntimes[platform] != null) TextButton(onClick = { model.select(platform, null) }) { Text("Clear selection") }
   } }
  }
 }
 if(adding) AddEmulatorDialog(state, { adding = false }, model)
 if(editing != null) RuntimeEditor(editing, { editing = null }, model)
}
@Composable private fun AddEmulatorDialog(state: StationState, dismiss: () -> Unit, model: StationViewModel) {
 var platform by rememberSaveable { mutableStateOf(Platform.PS1) }
 var advanced by rememberSaveable { mutableStateOf(false) }
 val choices = remember(platform, state.installed) { model.installedApps(platform) }
 var selected by remember(platform, choices) { mutableStateOf(choices.filter { it.builtIn }.singleOrNull()?.id) }
 if(advanced) { RuntimeEditor(null, { advanced = false }, model, platform); return }
 AlertDialog(onDismissRequest = dismiss, title = { Text("Add Emulator") }, text = {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
   Text("Select Console")
   SystemPicker(platform, { if(it != null) platform = it })
   Text("Select Installed Emulator")
   if(choices.isEmpty()) Text("No compatible app detected. Install " + builtInRuntimes.filter { platform in it.systems }.joinToString { it.name }.ifBlank { "a compatible emulator" } + " and return here.")
   choices.forEach { runtime ->
    val configured = state.runtimes.any { it.packageName == runtime.packageName && platform in it.systems }
    Row(verticalAlignment = Alignment.CenterVertically) {
     RadioButton(selected == runtime.id, { selected = runtime.id })
     EmulatorIcon(runtime)
     Column(Modifier.weight(1f)) {
      Text(runtime.name)
      Text(if(runtime.builtIn) "Already configured automatically" else if(configured) "Already configured" else "Document-opening app; confirm console support", style = MaterialTheme.typography.bodySmall)
     }
    }
   }
   TextButton(onClick = { advanced = true }) { Text("Advanced / Custom Emulator") }
  }
 }, confirmButton = { Button(onClick = { choices.firstOrNull { it.id == selected }?.let { model.addInstalled(platform, it); dismiss() } }, enabled = selected != null) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
@Composable private fun EmulatorIcon(runtime: RuntimeConfig) {
 val context = LocalContext.current
 val bitmap = remember(runtime.packageName) { runCatching { context.packageManager.getApplicationIcon(runtime.packageName).toBitmap(40, 40) }.getOrNull() }
 if(bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.padding(end = 8.dp).size(32.dp))
}
@Composable private fun RuntimeEditor(existing: RuntimeConfig?, dismiss: () -> Unit, model: StationViewModel, initialPlatform: Platform = Platform.NES) {
 var name by remember { mutableStateOf(existing?.name ?: "") }
 var pkg by remember { mutableStateOf(existing?.packageName ?: "") }
 var activity by remember { mutableStateOf(existing?.activity ?: "") }
 var action by remember { mutableStateOf(existing?.action ?: Intent.ACTION_VIEW) }
 var mime by remember { mutableStateOf(existing?.mime ?: "application/octet-stream") }
 var systems by remember { mutableStateOf(existing?.systems ?: emptySet()) }
 var p by remember { mutableStateOf(initialPlatform) }
 var error by remember { mutableStateOf<String?>(null) }
 AlertDialog(onDismissRequest = dismiss, title = { Text("Advanced / Custom Emulator") }, text = {
  Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
   Text("For unsupported/custom emulator applications. Passes a read-only content URI in data and ClipData. Your app must support this contract.", style = MaterialTheme.typography.bodySmall)
   OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true)
   OutlinedTextField(pkg, { pkg = it }, label = { Text("Package (org.example.emulator)") }, singleLine = true)
   OutlinedTextField(activity, { activity = it }, label = { Text("Exported activity (optional)") }, singleLine = true)
   OutlinedTextField(action, { action = it }, label = { Text("Intent action") }, singleLine = true)
   OutlinedTextField(mime, { mime = it }, label = { Text("MIME type") }, singleLine = true)
   SystemPicker(p, { if(it != null) p = it })
   TextButton(onClick = { systems = if(p in systems) systems - p else systems + p }) { Text(if(p in systems) "Remove system" else "Add system") }
   Text(systems.joinToString { it.title }.ifBlank { "No systems selected" })
   if(error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
   if(existing != null) TextButton(onClick = { model.removeRuntime(existing.id); dismiss() }) { Text("Remove registration") }
  }
 }, confirmButton = { TextButton(onClick = {
  val config = RuntimeConfig(existing?.id ?: UUID.randomUUID().toString(), name.trim(), pkg.trim(), activity.trim(), mime.trim(), systems, action = action.trim())
  runCatching { config.validate() }.onSuccess { model.saveRuntime(config); dismiss() }.onFailure { error = it.message }
 }) { Text("Save") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}
@Composable private fun SettingsScreen(state: StationState, model: StationViewModel, controller: String, addFolder: () -> Unit) {
 var repository by remember(state.repository) { mutableStateOf(state.repository) }
 var removing by remember { mutableStateOf<LibraryRoot?>(null) }
 val context = LocalContext.current
 LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
  item { Text("Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold) }
  item { SettingsPanel("Library & storage") {
   Text("Games stay in document-provider folders, including removable storage. Database and imported covers live in private app storage. No broad storage permission is requested.")
   Button(onClick = addFolder, enabled = !state.busy) { Text("Add library folder") }
   state.roots.forEach { root -> Row(verticalAlignment = Alignment.CenterVertically) { Text(root.name, Modifier.weight(1f)); TextButton(onClick = { removing = root }, enabled = !state.busy) { Text("Unregister") } } }
   Text("Missing files stay in your library so metadata survives disconnected storage. Reconnect the volume or reselect its folder before launching.", style = MaterialTheme.typography.bodySmall)
  } }
  item { SettingsPanel("Appearance") {
   Text("App Logo", style = MaterialTheme.typography.titleMedium)
   AppLogo.entries.chunked(3).forEach { logos -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
    logos.forEach { logo -> OutlinedButton(onClick = { model.logo(logo) }, modifier = Modifier.weight(1f), contentPadding = PaddingValues(4.dp)) {
     Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Image(painterResource(logo.resource), logo.label, Modifier.size(64.dp))
      Text(logo.label + if(logo == state.logo) " ✓" else "", style = MaterialTheme.typography.labelSmall)
     }
    } }
    repeat(3 - logos.size) { Spacer(Modifier.weight(1f)) }
   } }
   Text("Launcher refresh timing depends on your launcher.", style = MaterialTheme.typography.bodySmall)
  } }
  item { SettingsPanel("Library view") { GridSizeSelector(state.density, model::density); Text("Density adapts to screen width and text size. Applies to every library filter.") } }
  item { SettingsPanel("Online cover art") { Text(CoverPrivacy); Row(verticalAlignment = Alignment.CenterVertically) { Text("Allow online artwork search", Modifier.weight(1f)); Switch(state.onlineArtwork, model::onlineArtwork) } } }
  item { SettingsPanel("Controllers") { Text(controller); Text("D-pad / left stick moves focus. A selects; B goes back. Emulators manage gameplay mappings.") } }
  item { SettingsPanel("Android updates") {
   OutlinedTextField(repository, { repository = it }, label = { Text("Official GitHub owner/repository") }, singleLine = true, modifier = Modifier.fillMaxWidth())
   TextButton(onClick = { model.repository(repository) }) { Text("Save repository") }
   Button(onClick = model::checkUpdates, enabled = state.repository.isNotBlank()) { Text("Check Android updates") }
   Text("Only stable android-v tags with matching Android APK names and SHA-256 digests qualify. Updates open in your browser for owner-controlled installation.", style = MaterialTheme.typography.bodySmall)
   state.update?.let { update -> TextButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(update.releaseUrl))) }.onFailure { model.notify("No browser available") } }) { Text("Review Android ${update.version}") } }
  } }
  item { SettingsPanel("Akito Station Public / PRO") {
   Text("Android ${BuildConfig.VERSION_NAME} · build ${BuildConfig.VERSION_CODE}")
   Text("Public library and external launching are available. PRO purchases and entitlement verification are not connected on Android. No local setting unlocks PRO.")
  } }
  item { SettingsPanel("Privacy & preservation") { Text("No analytics, accounts, advertisements or game uploads. Networking occurs when you request an update check or consent to online cover search. Unregistering a library or emulator never deletes your original games, firmware or saves. Uninstalling Akito removes its private metadata and imported covers.") } }
 }
 if(removing != null) AlertDialog(onDismissRequest = { removing = null }, title = { Text("Unregister library?") }, text = { Text("Remove ${removing!!.name} and its metadata from Akito Station. Original files stay untouched.") }, confirmButton = { TextButton(onClick = { model.removeRoot(removing!!); removing = null }) { Text("Unregister") } }, dismissButton = { TextButton(onClick = { removing = null }) { Text("Cancel") } })
}
@Composable private fun SettingsPanel(title: String, content: @Composable ColumnScope.() -> Unit) {
 Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text(title, style = MaterialTheme.typography.titleMedium, color = Cyan); content() } }
}
