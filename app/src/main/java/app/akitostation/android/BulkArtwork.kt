package app.akitostation.android

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

enum class ScrapeMode { MISSING, ALL }
data class ScrapeOptions(val mode: ScrapeMode = ScrapeMode.MISSING, val replaceExisting: Boolean = false)
data class ScrapeProgress(val total: Int = 0, val scanned: Int = 0, val added: Int = 0, val skipped: Int = 0,
 val missing: Set<String> = emptySet(), val errors: Int = 0, val current: String = "", val complete: Boolean = false, val cancelled: Boolean = false)
/** One in-flight game, exact normalized title and exact platform. Ambiguous titles require manual review. */
class BulkArtwork(private val allowed: () -> Boolean, private val search: suspend (Game) -> List<CoverResult>,
 private val apply: suspend (Game, CoverResult, Boolean) -> Boolean) {
 companion object {
  fun best(game: Game, results: List<CoverResult>): CoverResult? {
   if(game.system == Platform.UNKNOWN) return null
   val exact = results.filter { it.system == game.system && CoverSearch.normalize(it.title) == CoverSearch.normalize(game.displayTitle) }
   // Region/revision variants with the same normalized title are equivalent; no fuzzy or sequel substitutions.
   return exact.sortedBy { it.url }.firstOrNull()
  }
 }
 suspend fun run(games: List<Game>, options: ScrapeOptions, progress: (ScrapeProgress) -> Unit): ScrapeProgress {
  check(allowed()) { "Enable online artwork search in Settings to scrape box art" }
  var status = ScrapeProgress(total = games.size); progress(status)
  for(game in games) {
   currentCoroutineContext().ensureActive(); check(allowed()) { "Online artwork search disabled" }
   status = status.copy(current = "${game.displayTitle} · ${game.system.title}"); progress(status)
   if(game.artwork.isNotBlank() && (options.mode == ScrapeMode.MISSING || !options.replaceExisting)) status = status.copy(skipped = status.skipped + 1)
   else try {
    val match = best(game, search(game))
    status = if(match == null) status.copy(missing = status.missing + game.id)
    else if(apply(game, match, options.mode == ScrapeMode.ALL && options.replaceExisting)) status.copy(added = status.added + 1)
    else status.copy(skipped = status.skipped + 1)
   } catch(e: kotlinx.coroutines.CancellationException) { throw e }
   catch(e: Exception) { status = status.copy(errors = status.errors + 1, missing = status.missing + game.id) }
   status = status.copy(scanned = status.scanned + 1); progress(status)
  }
  return status.copy(current = "", complete = true).also(progress)
 }
}
