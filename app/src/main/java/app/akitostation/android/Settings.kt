package app.akitostation.android

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class RuntimeConfig(val id: String, val name: String, val packageName: String, val activity: String = "", val mime: String = "application/octet-stream", val systems: Set<Platform>, val builtIn: Boolean = false, val action: String = android.content.Intent.ACTION_VIEW) {
 fun validate() {
  require(packageName.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))) { "Enter a valid Android package name" }
  require(activity.isBlank() || activity.matches(Regex("[A-Za-z.][A-Za-z0-9_.$]*"))) { "Enter a valid exported activity name" }
  require(mime.matches(Regex("[a-zA-Z0-9.+-]+/[a-zA-Z0-9.+*-]+"))) { "Enter a valid MIME type" }
  require(action.matches(Regex("[A-Za-z][A-Za-z0-9_.]+"))) { "Enter a valid intent action" }
  require(name.isNotBlank() && systems.isNotEmpty()) { "Name and at least one system are required" }
 }
}
val builtInRuntimes = listOf(
 RuntimeConfig("ppsspp", "PPSSPP", "org.ppsspp.ppsspp", "org.ppsspp.ppsspp.PpssppActivity", systems = setOf(Platform.PSP), builtIn = true),
 RuntimeConfig("ppssppgold", "PPSSPP Gold", "org.ppsspp.ppssppgold", "org.ppsspp.ppsspp.PpssppActivity", systems = setOf(Platform.PSP), builtIn = true),
 RuntimeConfig("dolphin", "Dolphin", "org.dolphinemu.dolphinemu", "org.dolphinemu.dolphinemu.ui.main.MainActivity", systems = setOf(Platform.GC, Platform.WII), builtIn = true),
 RuntimeConfig("melonds", "melonDS", "me.magnum.melonds", "me.magnum.melonds.ui.emulator.EmulatorActivity", systems = setOf(Platform.NDS), builtIn = true, action = "me.magnum.melonds.LAUNCH_ROM"),
 RuntimeConfig("mupen64", "Mupen64Plus-AE alpha", "org.mupen64plusae.v3.alpha", "paulscode.android.mupen64plusae.SplashActivity", systems = setOf(Platform.N64), builtIn = true),
 RuntimeConfig("nesemu", "NES.emu", "com.explusalpha.NesEmu", "com.imagine.BaseActivity", systems = setOf(Platform.NES), builtIn = true),
 RuntimeConfig("snes9x", "Snes9x EX+", "com.explusalpha.Snes9xPlus", "com.imagine.BaseActivity", systems = setOf(Platform.SNES), builtIn = true),
 RuntimeConfig("gbcemu", "GBC.emu", "com.explusalpha.GbcEmu", "com.imagine.BaseActivity", systems = setOf(Platform.GB, Platform.GBC), builtIn = true),
 RuntimeConfig("gbaemu", "GBA.emu", "com.explusalpha.GbaEmu", "com.imagine.BaseActivity", systems = setOf(Platform.GBA), builtIn = true),
 RuntimeConfig("mdemu", "MD.emu", "com.explusalpha.MdEmu", "com.imagine.BaseActivity", systems = setOf(Platform.GENESIS), builtIn = true),
 RuntimeConfig("pceemu", "PCE.emu", "com.PceEmu", "com.imagine.BaseActivity", systems = setOf(Platform.PCE), builtIn = true),
 RuntimeConfig("duckstation", "DuckStation", "com.github.stenzek.duckstation", "com.github.stenzek.duckstation.EmulationActivity", systems = setOf(Platform.PS1), builtIn = true),
 RuntimeConfig("saturnemu", "Saturn.emu", "com.explusalpha.SaturnEmu", "com.imagine.BaseActivity", systems = setOf(Platform.SATURN), builtIn = true)
)
class StationSettings(context: Context) {
 private val prefs = context.getSharedPreferences("station", Context.MODE_PRIVATE)
 init {
  val originals = custom()
  val merged = originals.mapNotNull { custom ->
   val covered = builtInRuntimes.filter { it.packageName == custom.packageName }.flatMap { it.systems }.toSet()
   val remaining = custom.systems - covered
   if(remaining.isEmpty()) null else custom.copy(systems = remaining)
  }
  if(merged != originals) {
   val editor = prefs.edit()
   for(platform in Platform.entries) {
    val previous = originals.firstOrNull { it.id == prefs.getString("runtime.${platform.name}", null) } ?: continue
    val known = builtInRuntimes.firstOrNull { it.packageName == previous.packageName && platform in it.systems && platform in previous.systems } ?: continue
    editor.putString("runtime.${platform.name}", known.id)
   }
   editor.putString("custom", encode(merged)).commit()
  }
 }
 var onlineArtwork: Boolean get() = prefs.getBoolean("onlineArtworkConsent.v1", false); set(value) { prefs.edit().putBoolean("onlineArtworkConsent.v1", value).commit() }
 var compact: Boolean get() = prefs.getBoolean("compact", false); set(value) { prefs.edit().putBoolean("compact", value).apply() }
 var sort: SortOrder get() = runCatching { SortOrder.valueOf(prefs.getString("sort", "TITLE")!!) }.getOrDefault(SortOrder.TITLE); set(value) { prefs.edit().putString("sort", value.name).apply() }
 var updateRepository: String get() = prefs.getString("updateRepository", "leoxcho/akito-station-android")!!.ifBlank { "leoxcho/akito-station-android" }; set(value) { require(value.isBlank() || value.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) { "Use owner/repository" }; prefs.edit().putString("updateRepository", value.trim()).apply() }
 fun selected(platform: Platform) = prefs.getString("runtime.${platform.name}", null)
 fun select(platform: Platform, id: String?) { prefs.edit().putString("runtime.${platform.name}", id).apply() }
 fun custom(): List<RuntimeConfig> = runCatching {
  val a = JSONArray(prefs.getString("custom", "[]")); (0 until a.length()).map { i -> val o = a.getJSONObject(i); val s = o.getJSONArray("systems")
   RuntimeConfig(o.getString("id"), o.getString("name"), o.getString("package"), o.getString("activity"), o.getString("mime"), (0 until s.length()).map { Platform.valueOf(s.getString(it)) }.toSet(), action = o.optString("action", android.content.Intent.ACTION_VIEW)) }
 }.getOrDefault(emptyList())
 fun save(config: RuntimeConfig) {
  config.validate()
  require((builtInRuntimes + custom()).none { it.id != config.id && it.packageName == config.packageName && it.systems.any(config.systems::contains) }) { "This emulator is already configured for that console. Choose it in Consoles." }
  write(custom().filter { it.id != config.id } + config)
 }
 fun remove(id: String) { write(custom().filter { it.id != id }); Platform.entries.forEach { if(selected(it) == id) select(it, null) } }
 private fun encode(configs: List<RuntimeConfig>): String { val a = JSONArray(); configs.forEach { c -> a.put(JSONObject().put("id", c.id).put("name", c.name).put("package", c.packageName).put("activity", c.activity).put("mime", c.mime).put("action", c.action).put("systems", JSONArray(c.systems.map { it.name }))) }; return a.toString() }
 private fun write(configs: List<RuntimeConfig>) { prefs.edit().putString("custom", encode(configs)).apply() }
}
