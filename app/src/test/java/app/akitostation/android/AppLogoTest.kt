package app.akitostation.android
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [26, 35])
class AppLogoTest {
 @Test fun eachLogoPersistsAndHasExactlyOneLauncher() {
  val context = ApplicationProvider.getApplicationContext<Context>()
  val settings = StationSettings(context)
  for(logo in AppLogo.entries) {
   logo.applyLauncher(context); settings.logo = logo
   assertEquals(logo, StationSettings(context).logo)
   for(other in AppLogo.entries) {
    val state = context.packageManager.getComponentEnabledSetting(ComponentName(context, "app.akitostation.android.Logo${other.name}"))
    assertEquals(if(other == logo) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, state)
    assertNotNull(context.resources.getDrawable(other.resource, null))
   }
  }
 }
}
