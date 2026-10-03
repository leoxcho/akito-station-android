package app.akitostation.android
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowContentResolver
import java.io.File
import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

class FixtureProvider : ContentProvider() {
 var offline = false
 var image = byteArrayOf()
 val opened = mutableListOf<String>()
 override fun onCreate() = true
 override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
  if(offline) throw SecurityException("Storage permission revoked")
  val cols = projection ?: arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE)
  val c = MatrixCursor(cols)
  fun row(id:String,name:String,mime:String,size:Long) { val v = mapOf<String,Any>(DocumentsContract.Document.COLUMN_DOCUMENT_ID to id,DocumentsContract.Document.COLUMN_DISPLAY_NAME to name,DocumentsContract.Document.COLUMN_MIME_TYPE to mime,DocumentsContract.Document.COLUMN_SIZE to size);c.addRow(cols.map { v[it] }.toTypedArray()) }
  val id=DocumentsContract.getDocumentId(uri)
  if(uri.path!!.endsWith("/children")) {
   if(id == "root") { row("nes","NES",DocumentsContract.Document.MIME_TYPE_DIR,0);row("bios","[BIOS] private.nes","application/octet-stream",16);row("keys","keys.bin","application/octet-stream",16) }
   if(id == "nes") { row("game","Synthetic.nes","application/octet-stream",16);row("art","Synthetic.png","image/png",32);row("hidden",".hidden.nes","application/octet-stream",16);row("ambiguous","Other.iso","application/octet-stream",16) }
  }
  return c
 }
 override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
  if(offline) throw java.io.FileNotFoundException("Missing storage")
  val id=uri.lastPathSegment!!;opened.add(id)
  val file=File(context!!.cacheDir,"synthetic-${id.hashCode()}.dat")
  file.writeBytes(if(id == "image") image else byteArrayOf(0x4e,0x45,0x53,0x1a)+ByteArray(12))
  return ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)
 }
 override fun getType(uri: Uri) = "application/octet-stream"
 override fun insert(uri: Uri, values: ContentValues?): Uri? = error("read-only")
 override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = error("read-only")
 override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = error("read-only")
}
@RunWith(RobolectricTestRunner::class) @Config(sdk = [26,35]) @GraphicsMode(GraphicsMode.Mode.NATIVE)
class StorageTest {
 private lateinit var context:Context
 private lateinit var provider:FixtureProvider
 @Before fun setup() { context=ApplicationProvider.getApplicationContext();provider=FixtureProvider();provider.attachInfo(context,android.content.pm.ProviderInfo().apply { authority="fixture" });ShadowContentResolver.registerProviderInternal("fixture",provider) }
 @Test fun recursiveScanIdentifiesAndFindsCover() = runBlocking {
  val root=LibraryRoot("content://fixture/tree/root","Games")
  val games=LibraryScanner(context).scan(root)
  assertEquals(2,games.size);val game=games.first { it.title == "Synthetic" };assertEquals(Platform.NES,game.platform);assertTrue(game.artwork.contains("art"));assertEquals(stableId(game.uri),game.id);assertFalse(provider.opened.contains("bios"));assertFalse(provider.opened.contains("keys"));assertFalse(provider.opened.contains("hidden"))
 }
 @Test fun revokedPermissionFailsWithoutDatabaseMutation() = runBlocking {
  provider.offline=true
  try { LibraryScanner(context).scan(LibraryRoot("content://fixture/tree/root","Games"));fail("must fail") } catch(e:SecurityException) { assertTrue(e.message!!.contains("revoked")) }
 }
 @Test fun scannerRejectsFilePaths() = runBlocking {
  try { LibraryScanner(context).scan(LibraryRoot("file:///games","Games"));fail("must fail") } catch(_:IllegalArgumentException) { }
 }
 @Test fun artworkCopiedIntoPrivateStorage() {
  val bitmap=Bitmap.createBitmap(2,2,Bitmap.Config.ARGB_8888);val output=ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,output);provider.image=output.toByteArray()
  val path=ArtworkStorage(context).import(Uri.parse("content://fixture/image"),stableId("game"))
  assertTrue(File(path).canonicalPath.startsWith(context.filesDir.canonicalPath+"/"));assertArrayEquals(provider.image,File(path).readBytes())
 }
 @Test fun malformedArtworkRejected() { provider.image=byteArrayOf(1,2,3);assertThrows(IllegalArgumentException::class.java) { ArtworkStorage(context).import(Uri.parse("content://fixture/image"),"bad") } }
 @Test fun oversizedArtworkRejected() { provider.image=ByteArray(16*1024*1024+1);assertThrows(IllegalArgumentException::class.java) { ArtworkStorage(context).import(Uri.parse("content://fixture/image"),"large") } }
 @Test fun missingGameHandledBeforeRuntimeLaunch() { provider.offline=true;assertThrows(IllegalStateException::class.java) { RuntimeRouter(context).launch(Game("id","content://fixture/game","r","Game",Platform.PSP),builtInRuntimes.first()) } }
}
