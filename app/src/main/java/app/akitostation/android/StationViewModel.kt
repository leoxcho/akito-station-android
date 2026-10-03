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
 val activity: String = "Ready", val message: String? = null, val compact: Boolean = false, val sort: SortOrder = SortOrder.TITLE,
 val repository: String = "", val update: AndroidUpdate? = null)
class StationViewModel(application: Application) : AndroidViewModel(application) {
 private val db = LibraryDatabase(application)
 val settings = StationSettings(application)
 private val router = RuntimeRouter(application)
 private val mutable = MutableStateFlow(StationState())
 val state = mutable.asStateFlow()
 private val workMutex = Mutex()
 private var scanJob: Job? = null
 init { refresh() }
 private suspend fun load() {
  val runtimes = builtInRuntimes + settings.custom()
  mutable.update { current -> current.copy(games = db.games(), roots = db.roots(), runtimes = runtimes,
   selectedRuntimes = Platform.entries.associateWith(settings::selected), installed = runtimes.filter(router::installed).map { it.id }.toSet(),
   compact = settings.compact, sort = settings.sort, repository = settings.updateRepository) }
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
 fun resetArtwork(game: Game) = work { db.artwork(game.id, ""); load() }
 fun launch(game: Game) = work {
  val runtimeId = settings.selected(game.system) ?: error("Choose an emulator for ${game.system.title} in Consoles. No default runtime is selected.")
  val runtime = (builtInRuntimes + settings.custom()).firstOrNull { it.id == runtimeId } ?: error("Selected emulator was removed; choose another in Consoles")
  // Main-thread activity launch; recent records only after Android accepts the handoff.
  withContext(Dispatchers.Main) { router.launch(game, runtime) }
  db.launched(game); load()
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
 override fun onCleared() { scanJob?.cancel(); db.close() }
}
