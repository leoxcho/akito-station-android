package app.akitostation.android

import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
class BulkArtworkTest {
 private fun game(id: String = "g", artwork: String = "") = Game(id, "content://$id", "root", "Mario 2 (USA)", Platform.NES, artwork = artwork)
 private fun cover(title: String = "Mario 2 (Europe)", system: Platform = Platform.NES) = CoverResult(title, system, "https://example.test/$title")
 @Test fun exactNormalizationAndPlatformOnly() {
  assertNotNull(BulkArtwork.best(game(), listOf(cover())))
  assertNotNull(BulkArtwork.best(game(), listOf(cover("Mario 2 (USA, Europe)"))))
  assertNull(BulkArtwork.best(game(), listOf(cover("Mario 3"))))
  assertNull(BulkArtwork.best(game(), listOf(cover(system = Platform.SNES))))
  assertNull(BulkArtwork.best(game(), listOf(cover("Mario 2 Deluxe"))))
  assertNull(BulkArtwork.best(game().copy(platform = Platform.UNKNOWN), listOf(cover())))
 }
 @Test fun missingAndAllPreserveOrReplace() = runBlocking {
  for(options in listOf(ScrapeOptions(), ScrapeOptions(ScrapeMode.ALL), ScrapeOptions(ScrapeMode.ALL, true))) {
   var applied = 0
   val scraper = BulkArtwork({ true }, { listOf(cover()) }, { _, _, _ -> applied++; true })
   val result = scraper.run(listOf(game(), game("custom", "custom.png")), options) {}
   assertEquals(if(options.replaceExisting) 2 else 1, applied)
   assertEquals(2, result.scanned)
   assertEquals(applied, result.added)
   assertEquals(2 - applied, result.skipped)
   assertTrue(result.complete)
  }
 }
 @Test fun notFoundErrorsAndSummaryContinue() = runBlocking {
  val statuses = mutableListOf<ScrapeProgress>()
  val result = BulkArtwork({ true }, { if(it.id == "error") error("offline") else emptyList() }, { _, _, _ -> error("must not apply") })
   .run(listOf(game(), game("error")), ScrapeOptions(), statuses::add)
  assertEquals(setOf("g", "error"), result.missing); assertEquals(1, result.errors); assertEquals(2, result.scanned)
  assertEquals(0, result.added); assertEquals(result, statuses.last())
 }
 @Test fun consentPreventsAnyAccess() = runBlocking {
  var searched = false
  assertTrue(runCatching { BulkArtwork({ false }, { searched = true; emptyList() }, { _, _, _ -> false }).run(listOf(game()), ScrapeOptions()) {} }.isFailure)
  assertFalse(searched)
 }
 @Test fun cancellationRetainsCompletedAndStopsNextGame() = runBlocking {
  var applied = 0; var last = ScrapeProgress()
  val job = launch {
   BulkArtwork({ true }, { if(it.id == "second") awaitCancellation() else listOf(cover()) }, { _, _, _ -> applied++; true })
    .run(listOf(game(), game("second")), ScrapeOptions()) { last = it }
  }
  yield(); job.cancelAndJoin()
  assertEquals(1, applied); assertEquals(1, last.scanned); assertEquals(1, last.added)
 }
 @Test fun temporaryFailuresRetryWithinBound() = runBlocking {
  var calls = 0
  val service = CoverSearch({ true }, loader = { calls++; if(calls < 3) throw java.io.IOException("temporary"); "<a href=\"Mario%202.png\">x</a>".toByteArray() }, retries = 2)
  assertEquals(1, service.search("Mario 2", Platform.NES, false).size); assertEquals(3, calls)
 }
}
