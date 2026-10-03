package app.akitostation.android

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class RuntimeRouter(private val context: Context) {
 fun installed(config: RuntimeConfig): Boolean = try {
  val pm = context.packageManager
  val app = pm.getApplicationInfo(config.packageName, 0)
  app.enabled && (config.activity.isBlank() || pm.getActivityInfo(ComponentName(config.packageName, config.activity), 0).let {
   it.enabled && it.exported && (it.permission == null || pm.checkPermission(it.permission, context.packageName) == PackageManager.PERMISSION_GRANTED)
  })
 } catch(_: PackageManager.NameNotFoundException) { false }
 fun installedApps(platform: Platform): List<RuntimeConfig> {
  val pm = context.packageManager
  val known = builtInRuntimes.filter { platform in it.systems && installed(it) }
  val launchers = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
  val viewers = pm.queryIntentActivities(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse("content://app.akitostation.android/probe"), "application/octet-stream"), PackageManager.MATCH_DEFAULT_ONLY)
  val packages = viewers.filter { it.activityInfo.exported && it.activityInfo.enabled }.map { it.activityInfo.packageName }.toSet()
  val others = launchers.filter { it.activityInfo.packageName in packages && it.activityInfo.exported && it.activityInfo.enabled && it.activityInfo.applicationInfo.enabled }
   .filter { entry -> entry.activityInfo.packageName != context.packageName && builtInRuntimes.none { it.packageName == entry.activityInfo.packageName } }
   .distinctBy { it.activityInfo.packageName }.map { RuntimeConfig("app:" + it.activityInfo.packageName, it.loadLabel(pm).toString(), it.activityInfo.packageName, systems = setOf(platform)) }
  return known + others.sortedBy { it.name.lowercase() }
 }
 fun intent(game: Game, runtime: RuntimeConfig): Intent {
  runtime.validate(); require(game.system in runtime.systems) { "Selected runtime does not support this system" }
  val uri = Uri.parse(game.uri); require(uri.scheme == "content") { "Only document-provider games are supported" }
  return Intent(runtime.action).apply {
   setDataAndType(uri, runtime.mime); setPackage(runtime.packageName)
   if(runtime.activity.isNotBlank()) component = ComponentName(runtime.packageName, runtime.activity)
   if(runtime.id == "duckstation") {
    putExtra("bootPath", uri.toString())
    putExtra("resumeState", false)
   }
   clipData = ClipData.newRawUri("Game", uri)
   addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
 }
 fun launch(game: Game, runtime: RuntimeConfig) {
  try {
   context.contentResolver.openAssetFileDescriptor(Uri.parse(game.uri), "r")?.use { } ?: error("Game unavailable; reconnect storage or reselect the folder")
  } catch(e: java.io.FileNotFoundException) {
   throw IllegalStateException("Game unavailable. Reconnect storage or choose its library folder again.", e)
  } catch(e: SecurityException) {
   throw IllegalStateException("Game access expired. Choose its library folder again in Settings.", e)
  }
  require(installed(runtime)) { "${runtime.name} is not installed. Install it from its official publisher, then return to Akito Station." }
  val intent = intent(game, runtime)
  val target = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
  require(target != null && target.exported && target.enabled) { "${runtime.name} cannot open this game. Check its setup and folder access, or choose another emulator in Consoles." }
  // Android may hide a custom package from queries while allowing an explicit launch.
  // Let the OS enforce export/permission rules instead of treating hidden packages as missing.
  try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  catch(e: ActivityNotFoundException) { throw IllegalStateException("${runtime.name} is missing or cannot open this game. Complete its setup or choose another emulator in Consoles.", e) }
  catch(e: SecurityException) { throw IllegalStateException("${runtime.name} denied the launch or document access. Check its folder access or choose another emulator in Consoles.", e) }
 }
}

sealed interface RuntimeChoice {
 data class Ready(val runtime: RuntimeConfig): RuntimeChoice
 data class Choose(val runtimes: List<RuntimeConfig>): RuntimeChoice
 data class Missing(val message: String): RuntimeChoice
}
fun runtimeChoice(platform: Platform, runtimes: List<RuntimeConfig>, installed: Set<String>, preferred: String?): RuntimeChoice {
 val compatible = runtimes.filter { platform in it.systems && it.id in installed }
 compatible.firstOrNull { it.id == preferred }?.let { return RuntimeChoice.Ready(it) }
 if(compatible.size == 1) return RuntimeChoice.Ready(compatible.single())
 if(compatible.isNotEmpty()) return RuntimeChoice.Choose(compatible)
 val supported = runtimes.filter { platform in it.systems }.joinToString { it.name }
 return RuntimeChoice.Missing(if(supported.isBlank()) "Choose an emulator for ${platform.title}. Add a compatible app in Consoles." else "Choose an emulator for ${platform.title}. Install $supported from its publisher, complete setup, then return to Akito Station.")
}
