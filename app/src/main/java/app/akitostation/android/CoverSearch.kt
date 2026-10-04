package app.akitostation.android

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.text.Normalizer
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

data class CoverResult(val title: String, val system: Platform, val url: String)
/** Official Libretro directory catalogs, searched locally. No ROM access or web search scraping. */
class CoverSearch(private val allowed: () -> Boolean, private val retries: Int = 0, private val loader: (String) -> ByteArray = ::load) {
 private val catalogs = mutableMapOf<Platform, List<CoverResult>>()
 private val cooldown = mutableMapOf<String, Long>()
 suspend fun search(query: String, system: Platform, allSystems: Boolean): List<CoverResult> {
  check(allowed()) { "Enable online artwork search to continue" }
  val words = normalize(query).split(' ').filter(String::isNotBlank)
  require(words.isNotEmpty()) { "Enter a game title" }
  val systems = if(allSystems || system == Platform.UNKNOWN) repositories.keys.sortedBy { it != system } else listOf(system)
  val results = mutableListOf<CoverResult>(); var failures = 0
  for(platform in systems) {
   currentCoroutineContext().ensureActive(); check(allowed()) { "Online artwork search disabled" }
   val repo = repositories[platform] ?: continue
   try {
    val catalog = catalogs[platform] ?: run { kotlinx.coroutines.delay(400); parseDirectory(request(base(repo)).toString(Charsets.UTF_8), platform).also { catalogs[platform] = it } }
    results += catalog.filter { result -> val tokens = normalize(result.title).split(' ').toSet(); words.all { it in tokens } }
   } catch(e: kotlinx.coroutines.CancellationException) { throw e }
   catch(e: Exception) { failures++; if(!allSystems && system != Platform.UNKNOWN) throw e }
  }
  if(results.isEmpty() && failures > 0) error("Cover provider unavailable or offline. Try later or import a local image.")
  return results.distinctBy { it.url }.sortedWith(compareBy<CoverResult> { it.system != system }.thenBy { normalize(it.title).length }).take(120)
 }
 fun download(result: CoverResult): ByteArray {
  require(result.url.startsWith(base(repositories[result.system] ?: error("Unsupported cover platform"))))
  return request(result.url)
 }
 private fun request(url: String): ByteArray {
  check(allowed()) { "Online artwork search disabled" }
  val host = URI(url).host
  check((cooldown[host] ?: 0) <= System.currentTimeMillis()) { "Provider rate limited. Try again later." }
  try { return retryLoad(url).also { check(allowed()); require(it.size <= LIMIT) { "Cover response too large" } } }
  catch(e: Exception) { cooldown[host] = System.currentTimeMillis() + 60_000; throw e }
 }
 private fun retryLoad(url: String): ByteArray {
  var attempt = 0
  while(true) {
   check(allowed())
   try { return loader(url) }
   catch(e: java.io.IOException) {
    if(attempt >= retries) throw e
    Thread.sleep(500L shl attempt++)
   }
  }
 }
 companion object {
  const val LIMIT = 16 * 1024 * 1024
  val repositories = mapOf(
   Platform.NES to "Nintendo_-_Nintendo_Entertainment_System", Platform.SNES to "Nintendo_-_Super_Nintendo_Entertainment_System",
   Platform.GB to "Nintendo_-_Game_Boy", Platform.GBC to "Nintendo_-_Game_Boy_Color", Platform.GBA to "Nintendo_-_Game_Boy_Advance",
   Platform.N64 to "Nintendo_-_Nintendo_64", Platform.NDS to "Nintendo_-_Nintendo_DS", Platform.N3DS to "Nintendo_-_Nintendo_3DS",
   Platform.GC to "Nintendo_-_GameCube", Platform.WII to "Nintendo_-_Wii", Platform.WIIU to "Nintendo_-_Wii_U",
   Platform.PS1 to "Sony_-_PlayStation", Platform.PS2 to "Sony_-_PlayStation_2", Platform.PS3 to "Sony_-_PlayStation_3",
   Platform.PS4 to "Sony_-_PlayStation_4", Platform.PSP to "Sony_-_PlayStation_Portable", Platform.PSVITA to "Sony_-_PlayStation_Vita",
   Platform.GENESIS to "Sega_-_Mega_Drive_-_Genesis", Platform.PCE to "NEC_-_PC_Engine_-_TurboGrafx_16",
   Platform.SATURN to "Sega_-_Saturn", Platform.DREAMCAST to "Sega_-_Dreamcast", Platform.XBOX to "Microsoft_-_Xbox", Platform.XBOX360 to "Microsoft_-_Xbox_360",
   Platform.SWITCH to "Nintendo_-_Nintendo_Switch")
  private fun encode(value: String) = URLEncoder.encode(value, "UTF-8").replace("+", "%20")
  fun base(repo: String) = "https://thumbnails.libretro.com/${encode(repo.replace('_', ' '))}/Named_Boxarts/"
  fun normalize(title: String): String = Normalizer.normalize(title.replace(Regex("\\[[^]]*]"), " ")
   .replace(Regex("\\((?i:(?:USA|Europe|Japan|World)(?:,\\s*(?:USA|Europe|Japan|World))*|En|Fr|De|Rev[^)]*|Disc[^)]*|v[0-9][^)]*)\\)"), " "), Normalizer.Form.NFD)
   .replace(Regex("\\p{M}+"), "").lowercase(java.util.Locale.ROOT).replace(Regex("[^\\p{L}\\p{N}]+"), " ").trim()
  fun parseDirectory(html: String, platform: Platform): List<CoverResult> {
   val root = base(repositories.getValue(platform))
   val results = Regex("href=\"([^\"]+)\"", RegexOption.IGNORE_CASE).findAll(html).mapNotNull { match ->
    val raw = match.groupValues[1]
    runCatching {
     val name = URLDecoder.decode(raw.replace("+", "%2B"), "UTF-8")
     if(!name.endsWith(".png", true) || name.contains('/') || name.contains('\\') || name.contains("..")) null
     else CoverResult(name.removeSuffix(".png"), platform, root + encode(name))
    }.getOrNull()
   }.toList().distinctBy { it.url }
   require(results.isNotEmpty()) { "Cover catalog unavailable or its format changed" }
   return results
  }
  fun load(url: String): ByteArray {
   val uri = URI(url)
   require(uri.scheme == "https" && uri.host == "thumbnails.libretro.com" && uri.userInfo == null)
   val c = uri.toURL().openConnection() as HttpURLConnection
   try {
    c.connectTimeout = 10000; c.readTimeout = 15000; c.instanceFollowRedirects = false
    c.setRequestProperty("User-Agent", "AkitoStationAndroid/1.0.1 (cover artwork)")
    if(c.responseCode == 429 || c.responseCode >= 500) throw java.io.IOException("Temporary cover provider failure (${c.responseCode})")
    require(c.responseCode == 200) { "Cover provider unavailable (${c.responseCode}). Try later or import an image." }
    return c.inputStream.use { it.readBounded(LIMIT + 1) }.also { require(it.size <= LIMIT) { "Cover response too large" } }
   } finally { c.disconnect() }
  }
 }
}
