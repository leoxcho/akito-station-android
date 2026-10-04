package app.akitostation.android
import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.ByteArrayOutputStream
import java.io.File
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [26,35])
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
class BulkPersistenceTest {
 private fun withDatabase(context: Context, action: (LibraryDatabase) -> Unit) { val db = LibraryDatabase(context); try { action(db) } finally { db.close() } }
 @Test fun duplicateArtworkStoredOnceAndLibraryReopenPreservesMetadata() {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val bytes = ByteArrayOutputStream().apply { Bitmap.createBitmap(3, 4, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, this) }.toByteArray()
  val storage = ArtworkStorage(context); val first = storage.saveShared(bytes)
  assertEquals(first, storage.saveShared(bytes))
  assertArrayEquals(bytes, File(first).readBytes())
  val game = Game("upgrade103", "content://upgrade/103", "upgrade103-root", "Game", Platform.NES)
  withDatabase(context) { db -> db.mergeRoot(game.root, listOf(game)); db.favorite(game); db.launched(game, 77); db.metadata(game.id, "Custom", Platform.SNES); db.artwork(game.id, first) }
  StationSettings(context).density = LibraryDensity.SIX
  withDatabase(context) { db -> val saved = db.game(game.id)!!; assertTrue(saved.favorite); assertEquals(77L, saved.lastLaunched); assertEquals("Custom", saved.displayTitle); assertEquals(first, saved.artwork); assertEquals(Platform.SNES, saved.system); assertEquals(1, db.readableDatabase.version); db.removeRoot(game.root) }
  assertEquals(LibraryDensity.SIX, StationSettings(context).density)
 }
}
