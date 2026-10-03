package app.akitostation.android

import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.net.HttpURLConnection

data class AndroidUpdate(val version: String, val code: Long, val apkUrl: String, val releaseUrl: String, val digest: String)
object AndroidUpdates {
 private val assetPattern = Regex("Akito-Station-Android-v([0-9]+\\.[0-9]+\\.[0-9]+)\\.apk")
 fun select(json: String, currentCode: Long, repository: String): AndroidUpdate? {
  require(repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+")))
  val releases = JSONArray(json)
  val candidates = mutableListOf<AndroidUpdate>()
  for(i in 0 until releases.length()) {
   val r = releases.getJSONObject(i)
   if(r.optBoolean("draft") || r.optBoolean("prerelease") || !r.optString("tag_name").startsWith("android-v")) continue
   val version = r.getString("tag_name").removePrefix("android-v")
   val assets = r.optJSONArray("assets") ?: continue
   for(j in 0 until assets.length()) {
    val a = assets.getJSONObject(j); val match = assetPattern.matchEntire(a.optString("name")) ?: continue
    if(match.groupValues[1] != version || a.optString("state") != "uploaded") continue
    val code = versionCode(version) ?: continue
    val url = a.optString("browser_download_url"); val page = r.optString("html_url")
    val prefix = "https://github.com/$repository/releases/"
    if(url != prefix + "download/android-v$version/Akito-Station-Android-v$version.apk" || page != prefix + "tag/android-v$version") continue
    if(!url.endsWith("/Akito-Station-Android-v$version.apk") || a.optLong("size") <= 0) continue
    val digest = a.optString("digest")
    if(!digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))) continue
    if(code > currentCode) candidates.add(AndroidUpdate(version, code, url, page, digest))
   }
  }
  return candidates.maxByOrNull { it.code }
 }
 // Android versionCode stays monotonic; v1.0.0 is build 1.
 fun versionCode(version: String): Long? {
  val p = version.split('.').map { it.toLongOrNull() ?: return null }
  if(p.size != 3 || p[0] !in 1..2000 || p[1] !in 0..99 || p[2] !in 0..99) return null
  return (p[0] - 1) * 10000 + p[1] * 100 + p[2] + 1
 }
 fun check(repository: String, currentCode: Long): AndroidUpdate? {
  require(repository.matches(Regex("[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+"))) { "Set the official GitHub owner/repository first" }
  val connection = URI("https://api.github.com/repos/$repository/releases?per_page=100").toURL().openConnection() as HttpURLConnection
  try {
   connection.connectTimeout = 10000; connection.readTimeout = 10000; connection.instanceFollowRedirects = false
   connection.setRequestProperty("Accept", "application/vnd.github+json")
   require(connection.responseCode == 200) { "GitHub update check failed (${connection.responseCode})" }
   val bytes = connection.inputStream.use { it.readBounded(4 * 1024 * 1024 + 1) }
   require(bytes.size <= 4 * 1024 * 1024) { "Update response too large" }
   return select(bytes.toString(Charsets.UTF_8), currentCode, repository)
  } finally { connection.disconnect() }
 }
}
