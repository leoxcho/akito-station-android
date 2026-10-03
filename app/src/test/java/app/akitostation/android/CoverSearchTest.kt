package app.akitostation.android
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [26,35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class CoverSearchTest {
 private val html = "<a href=\"Pokemon%20-%20Emerald%20Version%20%28USA%29.png\">cover</a><a href=\"Pokemon%20-%20Ruby%20Version.png\">cover</a>"
 @Test fun queryKeepsSequelAndLegitimateSubtitle() {
  assertEquals("pokemon emerald version", CoverSearch.normalize("Pokémon - Emerald Version (USA) [!]"))
  assertEquals("game 2 the lost worlds", CoverSearch.normalize("Game 2 (The Lost Worlds) (Europe)"))
 }
 @Test fun everyRuntimePlatformHasCatalog() { builtInRuntimes.flatMap { it.systems }.forEach { assertTrue(CoverSearch.repositories.containsKey(it)) } }
 @Test fun parsingRejectsForeignAndTraversalLinks() {
  val result = CoverSearch.parseDirectory(html + "<a href=\"https://evil.test/x.png\">x</a><a href=\"..%2Fx.png\">x</a>", Platform.GBA)
  assertEquals(2, result.size); assertTrue(result.all { it.url.startsWith("https://thumbnails.libretro.com/") })
 }
 @Test fun consentBlocksCatalogAndDownload() = runBlocking {
  var calls = 0; val service = CoverSearch({ false }) { calls++; byteArrayOf() }
  assertTrue(runCatching { service.search("Pokemon", Platform.GBA, false) }.isFailure)
  assertTrue(runCatching { service.download(CoverResult("x", Platform.GBA, CoverSearch.base(CoverSearch.repositories.getValue(Platform.GBA)) + "x.png")) }.isFailure)
  assertEquals(0, calls)
 }
 @Test fun resultsUseLocalWholeTokenMatchingAndCatalogCache() = runBlocking {
  var calls = 0; val urls = mutableListOf<String>()
  val service = CoverSearch({ true }) { calls++; urls += it; html.toByteArray() }
  assertEquals(1, service.search("Pokemon Emerald", Platform.GBA, false).size)
  assertEquals(0, service.search("Pok", Platform.GBA, false).size)
  assertEquals(1, calls); assertFalse(urls.single().contains("Pokemon"))
 }
 @Test fun failedAndOfflineSearchAreRetryableWithoutLooping() = runBlocking {
  var calls = 0; val service = CoverSearch({ true }) { calls++; throw java.io.IOException("Offline") }
  assertTrue(runCatching { service.search("Test", Platform.NES, false) }.isFailure)
  assertTrue(runCatching { service.search("Test", Platform.NES, false) }.isFailure)
  assertEquals(1, calls)
 }
 @Test fun malformedCatalogIsFailureAndNoMatchIsEmpty() = runBlocking {
  assertTrue(runCatching { CoverSearch({ true }) { "bad html".toByteArray() }.search("Test", Platform.NES, false) }.isFailure)
  assertTrue(CoverSearch({ true }) { html.toByteArray() }.search("Missing", Platform.NES, false).isEmpty())
 }
 @Test fun downloadRejectsForeignHostAndReturnsBoundedArtwork() {
  val url = CoverSearch.parseDirectory(html, Platform.GBA).first()
  val bytes = byteArrayOf(1,2,3); val service = CoverSearch({ true }) { bytes }
  assertArrayEquals(bytes, service.download(url))
  assertTrue(runCatching { service.download(url.copy(url = "https://evil.test/cover.png")) }.isFailure)
  assertTrue(runCatching { CoverSearch({ true }) { ByteArray(CoverSearch.LIMIT+1) }.download(url) }.isFailure)
 }
 @Test fun consentPersistsAndCanBeRevoked() {
  val context = ApplicationProvider.getApplicationContext<Context>()
  context.getSharedPreferences("station", 0).edit().clear().commit()
  assertFalse(StationSettings(context).onlineArtwork)
  StationSettings(context).onlineArtwork = true
  assertTrue(StationSettings(context).onlineArtwork)
  StationSettings(context).onlineArtwork = false
  assertFalse(StationSettings(context).onlineArtwork)
 }
 @Test fun artworkAndVersionOneDatabaseSurviveReopenAndRefresh() {
  val context = ApplicationProvider.getApplicationContext<Context>(); val storage = ArtworkStorage(context)
  val bytes = ByteArrayOutputStream().apply { Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
  val id = "cover-compatibility"; val first = storage.save(bytes, id); val second = storage.save(bytes, id)
  assertNotEquals(first, second); assertArrayEquals(bytes, File(second).readBytes())
  val game = Game(id, "content://test/game", "test-root", "Game", Platform.GBA)
  LibraryDatabase(context).let { db -> db.mergeRoot(game.root, listOf(game)); db.favorite(game); db.metadata(id, "Edited", Platform.NES); db.artwork(id, second); assertEquals(1, db.readableDatabase.version); db.close() }
  LibraryDatabase(context).let { db -> val saved = db.games().first { it.id == id }; assertTrue(saved.favorite); assertEquals("Edited", saved.displayTitle); assertEquals(Platform.NES, saved.system); assertEquals(second, saved.artwork); db.mergeRoot(game.root, listOf(game)); assertEquals(second, db.games().first { it.id == id }.artwork); db.close() }
  assertTrue(runCatching { storage.save(byteArrayOf(1, 2), id) }.isFailure)
  assertTrue(File(first).exists()); assertTrue(File(second).exists())
 }
 @Test fun androidUpdateVersionCodesAreCorrect() { assertEquals(1L, AndroidUpdates.versionCode("1.0.0")); assertEquals(2L, AndroidUpdates.versionCode("1.0.1")) }
 @Test fun exact101UpdateSelectedOnlyFrom100() {
  val repository = "leoxcho/akito-station-android"
  val prefix = "https://github.com/" + repository + "/releases/"
  val asset = org.json.JSONObject().put("name", "Akito-Station-Android-v1.0.1.apk").put("state", "uploaded").put("size", 1)
   .put("digest", "sha256:" + "a".repeat(64)).put("browser_download_url", prefix + "download/android-v1.0.1/Akito-Station-Android-v1.0.1.apk")
  val release = org.json.JSONObject().put("tag_name", "android-v1.0.1").put("html_url", prefix + "tag/android-v1.0.1").put("assets", org.json.JSONArray().put(asset))
  val json = org.json.JSONArray().put(release).toString()
  assertEquals("1.0.1", AndroidUpdates.select(json, 1, repository)!!.version)
  assertNull(AndroidUpdates.select(json, 2, repository))
 }
}
