package app.akitostation.android

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [26,35])
class LibraryDensityTest {
 @Test fun presetsAndPhoneTabletProgression() {
  assertEquals(listOf("2×2", "3×3", "4×4", "5×5", "6×6"), LibraryDensity.entries.map { it.label })
  for(width in listOf(320f, 360f, 411f, 600f, 800f, 1280f)) {
   val columns = LibraryDensity.entries.map { it.columns(width) }
   assertEquals(columns.sorted(), columns)
   for(preset in LibraryDensity.entries) {
    val count = preset.columns(width)
    assertTrue(count >= 1)
    assertTrue((width - 40 - (count - 1) * 12) / count >= 48)
    assertTrue(preset.columns(width, 2f) <= count)
   }
  }
  assertEquals(2, LibraryDensity.FOUR.columns(411f))
  assertTrue(LibraryDensity.SIX.columns(800f) > LibraryDensity.SIX.columns(360f))
 }
 @Test fun existingCompactAndDefaultMigrationAndRestart() {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val prefs = context.getSharedPreferences("station", 0); prefs.edit().clear().commit()
  assertEquals(LibraryDensity.FOUR, StationSettings(context).density)
  prefs.edit().putBoolean("compact", true).putBoolean("onlineArtworkConsent.v1", true).putString("runtime.NES", "nesemu").commit()
  assertEquals(LibraryDensity.FIVE, StationSettings(context).density)
  for(preset in LibraryDensity.entries) {
   StationSettings(context).density = preset
   assertEquals(preset, StationSettings(context).density)
   assertTrue(StationSettings(context).onlineArtwork)
   assertEquals("nesemu", StationSettings(context).selected(Platform.NES))
  }
 }
 @Test fun searchFavoritesRecentAndSystemDoNotChangeDensity() {
  val context = ApplicationProvider.getApplicationContext<Context>(); val settings = StationSettings(context)
  val games = listOf(Game("g", "content://g", "root", "Mario", Platform.NES, favorite = true, lastLaunched = 10))
  for(preset in LibraryDensity.entries) {
   settings.density = preset
   for(filter in LibraryFilter.entries) {
    assertEquals(1, visibleGames(games, "Mario", filter, Platform.NES, SortOrder.TITLE).size)
    assertEquals(preset, StationSettings(context).density)
   }
  }
 }
 @Test fun update103SelectedBy102AndNotItself() {
  val base = "https://github.com/leoxcho/akito-station-android/releases/"
  val asset = org.json.JSONObject().put("name", "Akito-Station-Android-v1.0.3.apk").put("state", "uploaded").put("size", 100).put("digest", "sha256:" + "a".repeat(64)).put("browser_download_url", base + "download/android-v1.0.3/Akito-Station-Android-v1.0.3.apk")
  val releases = org.json.JSONArray().put(org.json.JSONObject().put("tag_name", "android-v1.0.3").put("html_url", base + "tag/android-v1.0.3").put("assets", org.json.JSONArray().put(asset))).toString()
  assertEquals(4L, AndroidUpdates.select(releases, 3, "leoxcho/akito-station-android")!!.code)
  assertNull(AndroidUpdates.select(releases, 4, "leoxcho/akito-station-android"))
 }
}
