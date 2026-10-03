package app.akitostation.android

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

class RuntimeRouter(private val context: Context) {
 fun installed(config: RuntimeConfig): Boolean = try { context.packageManager.getApplicationInfo(config.packageName, 0).enabled } catch(_: PackageManager.NameNotFoundException) { false }
 fun intent(game: Game, runtime: RuntimeConfig): Intent {
  runtime.validate(); require(game.system in runtime.systems) { "Selected runtime does not support this system" }
  val uri = Uri.parse(game.uri); require(uri.scheme == "content") { "Only document-provider games are supported" }
  return Intent(runtime.action).apply {
   setDataAndType(uri, runtime.mime); setPackage(runtime.packageName)
   if(runtime.activity.isNotBlank()) component = ComponentName(runtime.packageName, runtime.activity)
   clipData = ClipData.newRawUri("Game", uri)
   addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
  }
 }
 fun launch(game: Game, runtime: RuntimeConfig) {
  context.contentResolver.openAssetFileDescriptor(Uri.parse(game.uri), "r")?.use { } ?: error("Game unavailable; reconnect storage or reselect the folder")
  require(!runtime.builtIn || installed(runtime)) { "${runtime.name} is not installed. Install it from its official publisher, then return to Akito Station." }
  val intent = intent(game, runtime)
  val target = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo
  if(target != null) require(target.exported && target.enabled) { "Runtime game-opening activity is unavailable" }
  // Android may hide a custom package from queries while allowing an explicit launch.
  // Let the OS enforce export/permission rules instead of treating hidden packages as missing.
  try { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
  catch(e: ActivityNotFoundException) { throw IllegalStateException("${runtime.name} is missing or cannot open this game. Check its package, activity and MIME type.", e) }
  catch(e: SecurityException) { throw IllegalStateException("${runtime.name} denied the launch or document access. Choose a compatible exported activity.", e) }
 }
}
