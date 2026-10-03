package app.akitostation.android
import android.content.*
import android.content.pm.*
import androidx.test.core.app.ApplicationProvider
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.json.*
@RunWith(RobolectricTestRunner::class) @Config(sdk = [26,35])
class RuntimeWorkflowTest {
 private lateinit var context: Context
 private val duck get() = builtInRuntimes.single { it.id == "duckstation" }
 @Before fun setup() { context = ApplicationProvider.getApplicationContext(); context.getSharedPreferences("station", 0).edit().clear().commit() }
 private fun install(c: RuntimeConfig, exported: Boolean = true) {
  val app = ApplicationInfo().apply { packageName=c.packageName; enabled=true }
  val activity = ActivityInfo().apply { packageName=c.packageName; name=c.activity; enabled=true; this.exported=exported; applicationInfo=app }
  Shadows.shadowOf(context.packageManager).installPackage(PackageInfo().apply { packageName=c.packageName; applicationInfo=app; activities=arrayOf(activity) })
 }
 @Test fun duckAbsent() { assertFalse(RuntimeRouter(context).installed(duck)) }
 @Test fun duckInstalledAndUnexportedRejected() { install(duck); assertTrue(RuntimeRouter(context).installed(duck)); install(duck,false); assertFalse(RuntimeRouter(context).installed(duck)) }
 @Test fun ps1MappingUsesFolderForSharedFormats() {
  for(ext in listOf("cue","chd","m3u","bin","img","iso","pbp","ecm","mds","mdf","ccd","exe")) {
   assertTrue(ext in Platform.scanExtensions)
   assertEquals(Platform.PS1,Platform.detect("Game.$ext", listOf("PlayStation")))
   assertEquals(Platform.UNKNOWN,Platform.detect("Game.$ext"))
  }
  assertEquals(Platform.PSP,Platform.detect("Game.pbp",listOf("PSP")))
  assertEquals(Platform.SATURN,Platform.detect("Game.chd",listOf("Saturn")))
 }
 @Test fun duckHandoffUsesBootPathAndReadGrant() {
  val intent=RuntimeRouter(context).intent(Game("x","content://provider/disc.chd","r","Game",Platform.PS1),duck)
  assertEquals("com.github.stenzek.duckstation.EmulationActivity",intent.component!!.className)
  assertEquals(intent.data.toString(),intent.getStringExtra("bootPath")); assertFalse(intent.getBooleanExtra("resumeState",true))
  assertEquals(intent.data,intent.clipData!!.getItemAt(0).uri)
  assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.flags)
 }
 @Test fun preferencesAndConsentSurviveNewSettingsInstance() {
  val s=StationSettings(context);s.select(Platform.PS1,duck.id);s.onlineArtwork=true;s.compact=true
  val reloaded=StationSettings(context);assertEquals(duck.id,reloaded.selected(Platform.PS1));assertTrue(reloaded.onlineArtwork);assertTrue(reloaded.compact)
 }
 @Test fun legacyCustomPreferenceMigrationDefaultsAction() {
  context.getSharedPreferences("station",0).edit().putString("custom",JSONArray().put(JSONObject().put("id","old").put("name","Old").put("package","org.example.custom").put("activity","").put("mime","application/octet-stream").put("systems",JSONArray().put("PS1"))).toString()).putString("runtime.PS1","old").commit()
  assertEquals(Intent.ACTION_VIEW,StationSettings(context).custom().single().action)
  assertEquals("old",StationSettings(context).selected(Platform.PS1))
 }
 @Test fun manualDuckPreferenceMigratesWithoutDuplicateOrConsentLoss() {
  val prefs=context.getSharedPreferences("station",0)
  prefs.edit().putString("custom",JSONArray().put(JSONObject().put("id","manual-duck").put("name","My Duck").put("package",duck.packageName).put("activity",duck.activity).put("mime",duck.mime).put("systems",JSONArray().put("PS1"))).toString()).putString("runtime.PS1","manual-duck").putBoolean("onlineArtworkConsent.v1",true).commit()
  val settings=StationSettings(context)
  assertTrue(settings.custom().isEmpty());assertEquals(duck.id,settings.selected(Platform.PS1));assertTrue(settings.onlineArtwork)
  assertEquals(duck.id,prefs.getString("runtime.PS1",null))
 }
 @Test fun advancedActionRoundTrip() {
  val s=StationSettings(context);val c=RuntimeConfig("custom","Custom","org.example.custom",systems=setOf(Platform.PS1),action="org.example.OPEN")
  s.save(c); assertEquals(c,StationSettings(context).custom().single())
 }
 @Test fun duplicateBuiltInAndCustomPrevented() {
  val s=StationSettings(context)
  assertThrows(IllegalArgumentException::class.java) { s.save(duck.copy(id="duplicate",builtIn=false)) }
  val c=RuntimeConfig("one","Custom","org.example.custom",systems=setOf(Platform.PS1));s.save(c)
  assertThrows(IllegalArgumentException::class.java) { s.save(c.copy(id="two")) };s.save(c.copy(name="Edited"));assertEquals(1,s.custom().size)
 }
 @Test fun knownInstalledEnumerationAndMissing() {
  val router=RuntimeRouter(context);assertTrue(router.installedApps(Platform.PS1).isEmpty());install(duck)
  assertEquals(listOf(duck),router.installedApps(Platform.PS1));assertTrue(router.installedApps(Platform.NES).isEmpty())
 }
 @Test fun enumerationRequiresLaunchableDocumentHandlerAndDeduplicates() {
  val pm=Shadows.shadowOf(context.packageManager)
  val app=ApplicationInfo().apply { packageName="org.example.viewer"; enabled=true }
  val activity=ActivityInfo().apply { packageName=app.packageName; name="org.example.viewer.Main"; enabled=true; exported=true; applicationInfo=app }
  val result=ResolveInfo().apply { activityInfo=activity; nonLocalizedLabel="Test Emulator" }
  val launcher=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
  pm.addResolveInfoForIntent(launcher,listOf(result,result))
  assertTrue(RuntimeRouter(context).installedApps(Platform.PS1).isEmpty())
  pm.addResolveInfoForIntent(Intent(Intent.ACTION_VIEW).setDataAndType(android.net.Uri.parse("content://app.akitostation.android/probe"),"application/octet-stream"),result)
  val choices=RuntimeRouter(context).installedApps(Platform.PS1)
  assertEquals(1,choices.size);assertEquals("Test Emulator",choices.single().name);assertEquals(setOf(Platform.PS1),choices.single().systems)
 }
 @Test fun singleRuntimeNeedsNoPreference() { assertEquals(duck, (runtimeChoice(Platform.PS1,builtInRuntimes,setOf(duck.id),null) as RuntimeChoice.Ready).runtime) }
 @Test fun multipleRuntimeChooserAndPreference() {
  val second=duck.copy(id="custom",packageName="org.example.ps1",builtIn=false)
  val all=listOf(duck,second);val installed=all.map { it.id }.toSet()
  assertEquals(all,(runtimeChoice(Platform.PS1,all,installed,null) as RuntimeChoice.Choose).runtimes)
  assertEquals(second,(runtimeChoice(Platform.PS1,all,installed,second.id) as RuntimeChoice.Ready).runtime)
  assertEquals(duck,(runtimeChoice(Platform.PS1,all,setOf(duck.id),second.id) as RuntimeChoice.Ready).runtime)
 }
 @Test fun missingRuntimeNamesSupportedApp() { val message=(runtimeChoice(Platform.PS1,builtInRuntimes,emptySet(),null) as RuntimeChoice.Missing).message;assertTrue(message.contains("DuckStation"));assertFalse(message.contains(duck.packageName)) }
 @Test fun update102SelectedBy101AndNotItself() {
  val repo="leoxcho/akito-station-android";val base="https://github.com/$repo/releases/"
  val json=JSONArray().put(JSONObject().put("tag_name","android-v1.0.2").put("html_url",base+"tag/android-v1.0.2").put("assets",JSONArray().put(JSONObject().put("name","Akito-Station-Android-v1.0.2.apk").put("state","uploaded").put("size",42).put("digest","sha256:"+"a".repeat(64)).put("browser_download_url",base+"download/android-v1.0.2/Akito-Station-Android-v1.0.2.apk")))).toString()
  assertEquals(3L,AndroidUpdates.select(json,2,repo)!!.code);assertNull(AndroidUpdates.select(json,3,repo))
 }
}
