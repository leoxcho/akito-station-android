package app.akitostation.android

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class StationState(val games: List<Game> = emptyList(), val roots: List<LibraryRoot> = emptyList(), val runtimes: List<RuntimeConfig> = builtInRuntimes,
 val selectedRuntimes: Map<Platform, String?> = emptyMap(), val installed: Set<String> = emptySet(), val busy: Boolean = false,
 val launchGame: Game? = null, val launchChoices: List<RuntimeConfig> = emptyList(), val activity: String = "Ready", val message: String? = null, val compact: Boolean = false, val sort: SortOrder = SortOrder.TITLE,
 val logo: AppLogo = AppLogo.AURORA, val density: LibraryDensity = LibraryDensity.FOUR, val scrape: ScrapeProgress? = null, val onlineArtwork: Boolean = false, val repository: String = "", val update: AndroidUpdate? = null)
class StationViewModel(application: Application) : AndroidViewModel(application) {
 private val db = LibraryDatabase(application)
 val settings = StationSettings(application)
 private val coverSearch = CoverSearch({ settings.onlineArtwork })
 private val router = RuntimeRouter(application)
 private val mutable = MutableStateFlow(StationState())
 val state = mutable.asStateFlow()
 private val workMutex = Mutex()
 private var scanJob: Job? = null
 private var scrapeJob: Job? = null
 init { refresh() }
 private suspend fun load() {
  val runtimes = builtInRuntimes + settings.custom()
  mutable.update { current -> current.copy(games = db.games(), roots = db.roots(), runtimes = runtimes,
   selectedRuntimes = Platform.entries.associateWith(settings::selected), installed = runtimes.filter(router::installed).map { it.id }.toSet(),
   logo = settings.logo, density = settings.density, onlineArtwork = settings.onlineArtwork, compact = settings.compact, sort = settings.sort, repository = settings.updateRepository) }
 }
 fun refresh() = work { load() }
 private fun work(action: suspend () -> Unit) { viewModelScope.launch(Dispatchers.IO) { try { workMutex.withLock { action() } } catch(e: CancellationException) { throw e } catch(e: Exception) { mutable.update { current -> current.copy(message = e.message ?: "Operation failed") } } } }
 fun dismiss() { mutable.update { current -> current.copy(message = null) } }
 fun notify(message: String) { mutable.update { current -> current.copy(message = message) } }
 fun addRoot(uri: Uri) = work {
  require(DocumentsContract.isTreeUri(uri)) { "Choose a library folder" }
  getApplication<Application>().contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
  val rootDoc = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
  val name = getApplication<Application>().contentResolver.query(rootDoc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c -> if(c.moveToFirst()) c.getString(0) else null } ?: "Library"
  db.addRoot(LibraryRoot(uri.toString(), name)); load(); scan()
 }
 fun removeRoot(root: LibraryRoot) = work {
  db.removeRoot(root.uri)
  // Roots may overlap: retain grants while any configured root still needs them.
  if(db.roots().isEmpty()) for(grant in getApplication<Application>().contentResolver.persistedUriPermissions) {
   runCatching { getApplication<Application>().contentResolver.releasePersistableUriPermission(grant.uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
  }
  load()
 }
 fun scan() {
  if(scanJob?.isActive == true) return
  scanJob = viewModelScope.launch(Dispatchers.IO) {
   mutable.update { current -> current.copy(busy = true, activity = "Scanning libraries…") }
   val failures = mutableListOf<String>()
   try {
    for(root in db.roots()) {
     ensureActive()
     try { val games = LibraryScanner(getApplication()).scan(root); db.mergeRoot(root.uri, games) }
     catch(e: CancellationException) { throw e }
     catch(e: Exception) { failures.add("${root.name}: ${e.message}") }
    }
    load()
    mutable.update { current -> current.copy(activity = "Scan complete · ${mutable.value.games.size} games", message = failures.takeIf { it.isNotEmpty() }?.joinToString("\n")) }
   } catch(e: CancellationException) { mutable.update { current -> current.copy(activity = "Scan cancelled; completed libraries retained") }; throw e }
   finally { mutable.update { current -> current.copy(busy = false) } }
  }
 }
 fun cancelScan() { scanJob?.cancel() }
 fun favorite(game: Game) = work { db.favorite(game); load() }
 fun metadata(game: Game, title: String, system: Platform) = work { db.metadata(game.id, title, system); load() }
 fun artwork(game: Game, uri: Uri) = work { db.artwork(game.id, ArtworkStorage(getApplication()).import(uri, game.id)); load() }
 fun onlineArtwork(value: Boolean) = work { settings.onlineArtwork = value; load() }
 suspend fun searchCovers(query: String, system: Platform, allSystems: Boolean) = withContext(Dispatchers.IO) { coverSearch.search(query, system, allSystems) }
 suspend fun applyCover(game: Game, result: CoverResult) = withContext(Dispatchers.IO) {
  workMutex.withLock {
   check(settings.onlineArtwork) { "Online artwork search disabled" }
   val bytes = coverSearch.download(result)
   check(settings.onlineArtwork) { "Online artwork search disabled" }
   val path = ArtworkStorage(getApplication()).save(bytes, game.id)
   db.artwork(game.id, path); load()
  }
 }
 fun logo(value: AppLogo) = work {
  value.applyLauncher(getApplication())
  settings.logo = value
  mutable.update { it.copy(logo = value) }
 }
 fun density(value: LibraryDensity) {
  mutable.update { it.copy(density = value) }
  work { settings.density = value }
 }
 fun scrapeArtwork(options: ScrapeOptions) {
  if(scrapeJob?.isActive == true) return
  scrapeJob = viewModelScope.launch(Dispatchers.IO) {
   val provider = CoverSearch({ settings.onlineArtwork }, retries = 2)
   val downloaded = mutableMapOf<String, String>()
   val scraper = BulkArtwork({ settings.onlineArtwork }, { game ->
    if(game.system !in CoverSearch.repositories) emptyList() else provider.search(game.displayTitle, game.system, false)
   }, { game, result, replace ->
    val cached = downloaded[result.url]?.takeIf { java.io.File(it).exists() }
    val bytes = if(cached == null) provider.download(result) else null
    ensureActive()
    workMutex.withLock {
     check(settings.onlineArtwork)
     val latest = db.game(game.id)
     if(latest == null || (!replace && latest.artwork.isNotBlank()) || latest.system != game.system || latest.displayTitle != game.displayTitle || latest.artwork != game.artwork) false
     else {
      val path = cached ?: ArtworkStorage(getApplication()).saveShared(bytes!!).also { downloaded[result.url] = it }
      db.artwork(game.id, path)
      mutable.update { it.copy(games = it.games.map { g -> if(g.id == game.id) g.copy(artwork = path) else g }) }
      true
     }
    }
   })
   try { scraper.run(db.games(), options) { progress -> mutable.update { it.copy(scrape = progress) } } }
   catch(e: CancellationException) { mutable.update { it.copy(scrape = it.scrape?.copy(complete = true, cancelled = true, current = "")) }; throw e }
   catch(e: Exception) { notify(e.message ?: "Scrape interrupted"); mutable.update { it.copy(scrape = it.scrape?.copy(complete = true, cancelled = true)) } }
  }
 }
 fun cancelScrape() { scrapeJob?.cancel() }
 fun dismissScrape() { if(scrapeJob?.isActive != true) mutable.update { it.copy(scrape = null) } }
 fun resetArtwork(game: Game) = work { db.artwork(game.id, ""); load() }
 fun launch(game: Game) = work {
  val runtimes = builtInRuntimes + settings.custom()
  when(val choice = runtimeChoice(game.system, runtimes, runtimes.filter(router::installed).map { it.id }.toSet(), settings.selected(game.system))) {
   is RuntimeChoice.Ready -> handoff(game, choice.runtime)
   is RuntimeChoice.Choose -> mutable.update { it.copy(launchGame = game, launchChoices = choice.runtimes) }
   is RuntimeChoice.Missing -> notify(choice.message)
  }
 }
 private suspend fun handoff(game: Game, runtime: RuntimeConfig) {
  withContext(Dispatchers.Main) { router.launch(game, runtime) }
  db.launched(game); load()
 }
 fun chooseLaunch(runtime: RuntimeConfig, remember: Boolean) = work {
  val game = mutable.value.launchGame ?: return@work
  dismissLaunch()
  if(remember) settings.select(game.system, runtime.id)
  handoff(game, runtime)
 }
 fun dismissLaunch() { mutable.update { it.copy(launchGame = null, launchChoices = emptyList()) } }
 fun installedApps(platform: Platform) = router.installedApps(platform)
 fun addInstalled(platform: Platform, runtime: RuntimeConfig) = work {
  if(!runtime.builtIn) {
   val existing = settings.custom().firstOrNull { it.packageName == runtime.packageName }
   if(existing == null) settings.save(runtime)
   else if(platform !in existing.systems) settings.save(existing.copy(systems = existing.systems + platform))
  }
  val configured = (builtInRuntimes + settings.custom()).first { it.packageName == runtime.packageName && platform in it.systems }
  settings.select(platform, configured.id); load()
 }
 fun select(platform: Platform, id: String?) = work { settings.select(platform, id); load() }
 fun saveRuntime(runtime: RuntimeConfig) = work { settings.save(runtime); load() }
 fun removeRuntime(id: String) = work { settings.remove(id); load() }
 fun compact(value: Boolean) = work { settings.compact = value; load() }
 fun sort(value: SortOrder) = work { settings.sort = value; load() }
 fun repository(value: String) = work { settings.updateRepository = value; load() }
 fun checkUpdates() = work {
  val update = AndroidUpdates.check(settings.updateRepository, BuildConfig.VERSION_CODE.toLong())
  mutable.update { current -> current.copy(update = update, message = if(update == null) "No newer stable Android APK found. macOS assets are excluded." else "Android ${update.version} is available. Review its release and checksum before installing.") }
 }
 override fun onCleared() { scanJob?.cancel(); scrapeJob?.cancel(); db.close() }
}
