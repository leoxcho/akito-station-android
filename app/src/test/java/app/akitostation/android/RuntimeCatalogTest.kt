package app.akitostation.android
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26,35])
class RuntimeCatalogTest {
 @Test fun catalogRoutesEveryDeclaredSystemWithReadOnlyDocumentGrant() {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val router = RuntimeRouter(context)
  assertEquals(13, builtInRuntimes.size)
  assertEquals(13, builtInRuntimes.map { it.packageName }.distinct().size)
  for(runtime in builtInRuntimes) for(system in runtime.systems) {
   val intent = router.intent(Game("test","content://provider/game","root","Game",system),runtime)
   assertEquals(runtime.packageName,intent.component!!.packageName)
   assertEquals(runtime.activity,intent.component!!.className)
   assertEquals(Uri.parse("content://provider/game"),intent.data)
   assertEquals(intent.data,intent.clipData!!.getItemAt(0).uri)
   assertEquals(Intent.FLAG_GRANT_READ_URI_PERMISSION,intent.flags)
   assertEquals(if(runtime.id=="melonds") "me.magnum.melonds.LAUNCH_ROM" else Intent.ACTION_VIEW,intent.action)
  }
 }
 @Test fun officialChannelIsDefault() {
  val settings=StationSettings(ApplicationProvider.getApplicationContext())
  assertEquals("leoxcho/akito-station-android",settings.updateRepository)
  settings.updateRepository=""
  assertEquals("leoxcho/akito-station-android",settings.updateRepository)
 }
 @Test fun missingApplicationsAreDetected() {
  val router=RuntimeRouter(ApplicationProvider.getApplicationContext())
  assertTrue(builtInRuntimes.none { router.installed(it) })
 }
 @Test fun pceAndroidOverrideIsPreserved() { assertEquals("com.PceEmu",builtInRuntimes.single { it.id=="pceemu" }.packageName) }
 @Test fun installedAndDisabledPackagesAreDistinguished() {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val manager=org.robolectric.Shadows.shadowOf(context.packageManager)
  for(runtime in builtInRuntimes) {
   val info=android.content.pm.PackageInfo().apply {
    packageName=runtime.packageName
    applicationInfo=android.content.pm.ApplicationInfo().apply { packageName=runtime.packageName; enabled=true }
   }
   info.activities = arrayOf(android.content.pm.ActivityInfo().apply {
    packageName = runtime.packageName; name = runtime.activity; enabled = true; exported = true; applicationInfo = info.applicationInfo
   })
   manager.installPackage(info)
   assertTrue(RuntimeRouter(context).installed(runtime))
   info.applicationInfo!!.enabled=false
   assertFalse(RuntimeRouter(context).installed(runtime))
  }
 }
}
