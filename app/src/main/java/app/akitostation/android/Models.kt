package app.akitostation.android

import java.security.MessageDigest

enum class Platform(val title: String, val aliases: Set<String>, val extensions: Set<String>) {
 NES("Nintendo Entertainment System", setOf("nes"), setOf("nes", "fds")),
 SNES("Super Nintendo", setOf("snes"), setOf("sfc", "smc")),
 GB("Game Boy", setOf("gb"), setOf("gb")), GBC("Game Boy Color", setOf("gbc"), setOf("gbc")),
 GBA("Game Boy Advance", setOf("gba"), setOf("gba")),
 GENESIS("Sega Mega Drive", setOf("genesis", "megadrive"), setOf("md", "gen", "smd")),
 PCE("PC Engine", setOf("pce", "pcengine"), setOf("pce")),
 N64("Nintendo 64", setOf("n64"), setOf("z64", "n64", "v64")),
 NDS("Nintendo DS", setOf("nds", "ds"), setOf("nds")),
 N3DS("Nintendo 3DS", setOf("3ds", "n3ds"), setOf("3ds", "cci")),
 PS1("PlayStation", setOf("ps1", "psx", "playstation"), emptySet()),
 PS2("PlayStation 2", setOf("ps2", "playstation2"), emptySet()),
 PS3("PlayStation 3", setOf("ps3", "playstation3"), emptySet()),
 PS4("PlayStation 4", setOf("ps4", "playstation4"), emptySet()),
 PSP("PlayStation Portable", setOf("psp"), setOf("cso", "pbp")),
 PSVITA("PlayStation Vita", setOf("vita", "psvita"), setOf("vpk")),
 GC("GameCube", setOf("gc", "gamecube"), setOf("gcz")),
 WII("Wii", setOf("wii"), setOf("wbfs")),
 WIIU("Wii U", setOf("wiiu"), setOf("wud", "wux", "wua", "rpx")),
 XBOX("Xbox", setOf("xbox"), emptySet()), XBOX360("Xbox 360", setOf("xbox360"), setOf("xex")),
 DREAMCAST("Dreamcast", setOf("dreamcast"), setOf("gdi")), SATURN("Saturn", setOf("saturn"), emptySet()),
 SWITCH("Nintendo Switch", setOf("switch", "nintendoswitch"), setOf("nsp", "xci", "nro", "nsz", "xcz")),
 UNKNOWN("Unidentified", emptySet(), emptySet());
 companion object {
  val scanExtensions = entries.flatMap { it.extensions }.toSet() + setOf("iso", "chd", "cue", "m3u", "rvz")
  fun detect(name: String, folders: List<String> = emptyList(), header: ByteArray = byteArrayOf()): Platform {
   if (header.take(4) == listOf(0x4e.toByte(), 0x45.toByte(), 0x53.toByte(), 0x1a.toByte())) return NES
   val ext = name.substringAfterLast('.', "").lowercase()
   entries.firstOrNull { ext in it.extensions }?.let { return it }
   for (folder in folders.asReversed()) {
    val normalized = folder.lowercase().filter { it.isLetterOrDigit() }
    entries.firstOrNull { normalized in it.aliases || normalized == it.title.lowercase().filter(Char::isLetterOrDigit) }?.let { return it }
   }
   return UNKNOWN
  }
 }
}
fun stableId(uri: String): String = MessageDigest.getInstance("SHA-256").digest(uri.toByteArray()).joinToString("") { "%02x".format(it) }
data class Game(val id: String, val uri: String, val root: String, val title: String, val platform: Platform,
 val bytes: Long = 0, val favorite: Boolean = false, val lastLaunched: Long = 0,
 val artwork: String = "", val customTitle: String = "", val platformOverride: Platform? = null) {
 val displayTitle get() = customTitle.ifBlank { title }
 val system get() = platformOverride ?: platform
}
data class LibraryRoot(val uri: String, val name: String)
enum class LibraryFilter { ALL, FAVORITES, RECENT }
enum class SortOrder { TITLE, RECENT, SYSTEM }
fun visibleGames(games: List<Game>, query: String, filter: LibraryFilter, system: Platform?, sort: SortOrder): List<Game> {
 val visible = games.filter { (query.isBlank() || it.displayTitle.contains(query, true) || it.system.title.contains(query, true)) &&
  (system == null || it.system == system) && when(filter) { LibraryFilter.ALL -> true; LibraryFilter.FAVORITES -> it.favorite; LibraryFilter.RECENT -> it.lastLaunched > 0 } }
 return when(sort) { SortOrder.TITLE -> visible.sortedBy { it.displayTitle.lowercase() }; SortOrder.RECENT -> visible.sortedByDescending { it.lastLaunched }; SortOrder.SYSTEM -> visible.sortedWith(compareBy({ it.system.title }, { it.displayTitle.lowercase() })) }
}
