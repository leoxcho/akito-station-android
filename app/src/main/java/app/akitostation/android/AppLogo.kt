package app.akitostation.android

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

enum class AppLogo(val label: String, val resource: Int) {
 AURORA("Aurora", R.drawable.logo_aurora), CLASSIC("Classic", R.drawable.logo_classic),
 CHROME("Chrome", R.drawable.logo_chrome), NEON("Neon", R.drawable.logo_neon),
 ICE("Ice", R.drawable.logo_ice), GOLD("Gold", R.drawable.logo_gold), BLUEPRINT("Blueprint", R.drawable.logo_blueprint);
 fun applyLauncher(context: Context) {
  val manager = context.packageManager
  // Manifest aliases use the fixed namespace; R8 may relocate this enum, and debug builds add an application ID suffix.
  fun component(logo: AppLogo) = ComponentName(context, "app.akitostation.android.Logo${logo.name}")
  if(Build.VERSION.SDK_INT >= 33) {
   manager.setComponentEnabledSettings(entries.map { logo -> PackageManager.ComponentEnabledSetting(component(logo), if(logo == this) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) })
  } else {
   // Enable first so a launcher entry always exists on older Android versions.
   manager.setComponentEnabledSetting(component(this), PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
   entries.filter { it != this }.forEach { manager.setComponentEnabledSetting(component(it), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP) }
  }
 }
}
