package app.akitostation.android
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
class CoreTest {
 @Test fun extensionDetection() { for(p in Platform.entries) for(ext in p.extensions) assertEquals(p, Platform.detect("Game.$ext", listOf(p.title))) }
 @Test fun ambiguousDiscNeedsSystem() { assertEquals(Platform.UNKNOWN, Platform.detect("Game.iso")); assertEquals(Platform.PS2, Platform.detect("Game.iso", listOf("Games", "PlayStation 2"))) }
 @Test fun nearestFolderWins() { assertEquals(Platform.PSP, Platform.detect("Game.iso", listOf("ps2", "psp"))) }
 @Test fun headerWins() { assertEquals(Platform.NES, Platform.detect("Game.iso", listOf("ps2"), byteArrayOf(0x4e,0x45,0x53,0x1a))) }
 @Test fun scanningAvoidsFirmwareExtensions() { assertFalse(Platform.scannable("keys.bin", listOf("PS1"))); assertFalse(Platform.scannable("scph5501.bin", listOf("PS1"))); assertFalse(Platform.scannable("Game.bin")); assertTrue(Platform.scannable("Game.bin", listOf("PS1"))); assertFalse("keys" in Platform.scanExtensions) }
 @Test fun stableIdentity() { assertEquals(stableId("content://a/1"), stableId("content://a/1")); assertNotEquals(stableId("content://a/1"), stableId("content://a/2")); assertEquals(64, stableId("test").length) }
 private val games = listOf(Game("a","content://a/1","r","Zelda",Platform.NES, favorite = true), Game("b","content://a/2","r","Alpha",Platform.PSP,lastLaunched = 10), Game("c","content://a/3","r","Beta",Platform.PS2,customTitle = "Custom"))
 @Test fun favoritesFilter() { assertEquals(listOf("a"), visibleGames(games,"",LibraryFilter.FAVORITES,null,SortOrder.TITLE).map { it.id }) }
 @Test fun recentIsLaunchHistory() { assertEquals(listOf("b"), visibleGames(games,"",LibraryFilter.RECENT,null,SortOrder.RECENT).map { it.id }) }
 @Test fun searchIncludesSystemAndEditedTitle() { assertEquals(1,visibleGames(games,"custom",LibraryFilter.ALL,null,SortOrder.TITLE).size); assertEquals(1,visibleGames(games,"portable",LibraryFilter.ALL,null,SortOrder.TITLE).size) }
 @Test fun systemFilter() { assertEquals(1,visibleGames(games,"",LibraryFilter.ALL,Platform.NES,SortOrder.TITLE).size) }
 @Test fun sortsTitle() { assertEquals(listOf("b","c","a"),visibleGames(games,"",LibraryFilter.ALL,null,SortOrder.TITLE).map { it.id }) }
 @Test fun boundedRead() { assertArrayEquals(byteArrayOf(1,2), ByteArrayInputStream(byteArrayOf(1,2,3)).readBounded(2)) }
 @Test fun boundedEmpty() { assertEquals(0,ByteArrayInputStream(byteArrayOf()).readBounded(16).size) }
 @Test fun versionCodeMonotonic() { assertEquals(1L,AndroidUpdates.versionCode("1.0.0")); assertEquals(102L,AndroidUpdates.versionCode("1.1.1")); assertNull(AndroidUpdates.versionCode("1.100.1")) }
 @Test fun controllerDeadzoneAndHat() { assertNull(ControllerInput.direction(.4f,.2f)); assertEquals(21,ControllerInput.direction(-1f,0f)); assertEquals(20,ControllerInput.direction(0f,0f,0f,1f)) }
}
