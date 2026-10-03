package app.akitostation.android
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.json.JSONArray
import org.json.JSONObject
@RunWith(RobolectricTestRunner::class) @Config(sdk = [26,35])
class AndroidBehaviorTest {
 private lateinit var context: Context
 private lateinit var db: LibraryDatabase
 @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.deleteDatabase("library.db"); context.getSharedPreferences("station",0).edit().clear().commit(); db=LibraryDatabase(context) }
 @After fun close() { db.close() }
 private fun game(id: String = "a") = Game(id,"content://provider/$id","content://provider/tree/root","Game",Platform.NES,bytes=128)
 @Test fun databasePreservesMetadataAcrossScan() { val g=game();db.mergeRoot(g.root,listOf(g));db.favorite(g);db.launched(g,42);db.artwork(g.id,"cover");db.metadata(g.id,"Edited",Platform.SNES);db.mergeRoot(g.root,listOf(g.copy(bytes=256)));val saved=db.games().single();assertTrue(saved.favorite);assertEquals(42,saved.lastLaunched);assertEquals("cover",saved.artwork);assertEquals("Edited",saved.displayTitle);assertEquals(Platform.SNES,saved.system);assertEquals(256,saved.bytes) }
 @Test fun missingScanDoesNotEraseMetadata() { val g=game();db.mergeRoot(g.root,listOf(g));db.mergeRoot(g.root,emptyList());assertEquals(1,db.games().size) }
 @Test fun repeatScanDoesNotDuplicate() { val g=game();repeat(3){db.mergeRoot(g.root,listOf(g))};assertEquals(1,db.games().size) }
 @Test fun unregisterOnlySelectedRoot() { val a=game();val b=game("b").copy(root="other");db.addRoot(LibraryRoot(a.root,"NES"));db.addRoot(LibraryRoot(b.root,"PSP"));db.mergeRoot(a.root,listOf(a));db.mergeRoot(b.root,listOf(b));db.removeRoot(a.root);assertEquals(listOf(b),db.games());assertEquals(1,db.roots().size) }
 @Test fun persistenceAcrossDatabaseOpen() { val g=game();db.mergeRoot(g.root,listOf(g));db.close();db=LibraryDatabase(context);assertEquals(g,db.games().single()) }
 @Test fun settingsPersistWithoutProFlag() { val s=StationSettings(context);s.compact=true;s.sort=SortOrder.RECENT;s.select(Platform.PSP,"ppsspp");s.updateRepository="owner/repository";val reloaded=StationSettings(context);assertTrue(reloaded.compact);assertEquals(SortOrder.RECENT,reloaded.sort);assertEquals("ppsspp",reloaded.selected(Platform.PSP));assertEquals("owner/repository",reloaded.updateRepository) }
 @Test fun customRuntimeRoundTripAndRemoval() { val s=StationSettings(context);val c=RuntimeConfig("one","Custom","org.example.emulator",systems=setOf(Platform.NES,Platform.SNES));s.save(c);s.select(Platform.NES,"one");assertEquals(listOf(c),s.custom());s.remove("one");assertTrue(s.custom().isEmpty());assertNull(s.selected(Platform.NES)) }
 @Test fun noSilentDefaultRuntime() { assertNull(StationSettings(context).selected(Platform.PSP)) }
 @Test fun invalidRuntimeRejected() { assertThrows(IllegalArgumentException::class.java) { RuntimeConfig("a","Bad","org.example;rm",systems=setOf(Platform.NES)).validate() } }
 @Test fun uriReadGrantAndExplicitRuntime() { val intent=RuntimeRouter(context).intent(game().copy(platform=Platform.PSP),builtInRuntimes.first());assertEquals(Intent.ACTION_VIEW,intent.action);assertEquals("org.ppsspp.ppsspp",intent.component!!.packageName);assertEquals(Uri.parse(game().uri),intent.data);assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.flags);assertEquals(0,intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION);assertEquals(intent.data,intent.clipData!!.getItemAt(0).uri) }
 @Test fun wrongSystemRejected() { assertThrows(IllegalArgumentException::class.java) { RuntimeRouter(context).intent(game(),builtInRuntimes.first()) } }
 @Test fun fileUriRejected() { assertThrows(IllegalArgumentException::class.java) { RuntimeRouter(context).intent(game().copy(uri="file:///private/game.iso",platform=Platform.PSP),builtInRuntimes.first()) } }
 @Test fun missingRuntimeDetected() { assertFalse(RuntimeRouter(context).installed(builtInRuntimes.first())) }
 private fun release(tag:String="android-v1.1.0", name:String="Akito-Station-Android-v1.1.0.apk", prerelease:Boolean=false, draft:Boolean=false, host:String="https://github.com/owner/repo"): JSONObject = JSONObject().put("tag_name",tag).put("prerelease",prerelease).put("draft",draft).put("html_url","$host/releases/tag/$tag").put("assets",JSONArray().put(JSONObject().put("name",name).put("state","uploaded").put("size",42).put("digest","sha256:"+"a".repeat(64)).put("browser_download_url","$host/releases/download/$tag/$name")))
 @Test fun androidUpdateSelected() { val u=AndroidUpdates.select(JSONArray().put(release()).toString(),1,"owner/repo");assertEquals("1.1.0",u!!.version);assertEquals(101,u.code) }
 @Test fun macosAssetsAndTagsExcluded() { assertNull(AndroidUpdates.select(JSONArray().put(release(tag="v1.1.0",name="Akito-Station-macOS.zip")).toString(),1,"owner/repo")) }
 @Test fun prereleaseAndDraftExcluded() { assertNull(AndroidUpdates.select(JSONArray().put(release(prerelease=true)).put(release(draft=true)).toString(),1,"owner/repo")) }
 @Test fun foreignDownloadExcluded() { assertNull(AndroidUpdates.select(JSONArray().put(release(host="https://evil.example/owner/repo")).toString(),1,"owner/repo")) }
 @Test fun sameOrOlderExcluded() { assertNull(AndroidUpdates.select(JSONArray().put(release()).toString(),101,"owner/repo")) }
 @Test fun mismatchTagAndAssetExcluded() { assertNull(AndroidUpdates.select(JSONArray().put(release(tag="android-v1.2.0")).toString(),1,"owner/repo")) }
 @Test fun noDigestExcluded() { val r=release();r.getJSONArray("assets").getJSONObject(0).remove("digest");assertNull(AndroidUpdates.select(JSONArray().put(r).toString(),1,"owner/repo")) }
 @Test fun repoInjectionRejected() { assertThrows(IllegalArgumentException::class.java) { StationSettings(context).updateRepository="owner/repo/../../other" } }
 @Test fun releaseTagUrlSuffixRejected() { val r=release();r.put("html_url",r.getString("html_url")+"-macos");assertNull(AndroidUpdates.select(JSONArray().put(r).toString(),1,"owner/repo")) }
 @Test fun downloadTraversalRejected() { val r=release();r.getJSONArray("assets").getJSONObject(0).put("browser_download_url","https://github.com/owner/repo/releases/download/android-v1.1.0/../Akito-Station-Android-v1.1.0.apk");assertNull(AndroidUpdates.select(JSONArray().put(r).toString(),1,"owner/repo")) }
}
