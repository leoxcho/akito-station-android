package app.akitostation.android

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import android.graphics.BitmapFactory
import java.io.File

class LibraryScanner(private val context: Context) {
 suspend fun scan(root: LibraryRoot): List<Game> {
  val tree = Uri.parse(root.uri)
  require(tree.scheme == "content" && DocumentsContract.isTreeUri(tree)) { "Choose a document-provider folder" }
  val resolver = context.contentResolver
  val result = mutableListOf<Game>(); val visited = mutableSetOf<String>()
  data class Folder(val id: String, val names: List<String>, val depth: Int)
  val pending = ArrayDeque<Folder>(); pending.add(Folder(DocumentsContract.getTreeDocumentId(tree), listOf(root.name), 0))
  var count = 0
  while(pending.isNotEmpty()) {
   currentCoroutineContext().ensureActive()
   val folder = pending.removeFirst()
   check(folder.depth <= 64) { "Folder nesting exceeds safe scan limit" }
   if (!visited.add(folder.id)) continue
   val children = mutableListOf<Triple<String, String, Pair<String, Long>>>()
   val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, folder.id)
   val projection = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE, DocumentsContract.Document.COLUMN_SIZE)
   val cursor = resolver.query(uri, projection, null, null, null) ?: error("Storage unavailable; reconnect or reselect the folder")
   cursor.use { c -> while(c.moveToNext()) {
    currentCoroutineContext().ensureActive()
    check(++count <= 100000) { "Library exceeds 100,000 document scan limit; choose smaller folders" }
    children.add(Triple(c.getString(0), c.getString(1) ?: "", (c.getString(2) ?: "") to c.getLong(3)))
   } }
   val covers = children.filter { it.second.substringAfterLast('.').lowercase() in setOf("png", "jpg", "jpeg", "webp") }.associateBy { it.second.substringBeforeLast('.').lowercase() }
   for ((id, name, info) in children) {
    if (name.startsWith('.') || name.startsWith("[bios]", true)) continue
    if (info.first == DocumentsContract.Document.MIME_TYPE_DIR) { pending.add(Folder(id, folder.names + name, folder.depth + 1)); continue }
    if (name.substringAfterLast('.', "").lowercase() !in Platform.scanExtensions) continue
    val doc = DocumentsContract.buildDocumentUriUsingTree(tree, id)
    val header = resolver.openInputStream(doc)?.use { it.readBounded(16) } ?: error("Cannot read selected game")
    val cover = covers[name.substringBeforeLast('.').lowercase()]?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.first).toString() }.orEmpty()
    result.add(Game(stableId(doc.toString()), doc.toString(), root.uri, name.substringBeforeLast('.'), Platform.detect(name, folder.names, header), info.second, artwork = cover))
   }
  }
  return result.distinctBy { it.id }
 }
}
class ArtworkStorage(private val context: Context) {
 /** Copy only bounded, decodable raster artwork into application-owned storage. */
 fun import(uri: Uri, id: String): String {
  require(uri.scheme == "content") { "Choose an image through the document picker" }
  val bytes = context.contentResolver.openInputStream(uri)?.use { input -> input.readBounded(16 * 1024 * 1024 + 1) } ?: error("Image unavailable")
  require(bytes.size <= 16 * 1024 * 1024) { "Cover must be smaller than 16 MB" }
  val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
  BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
  require(bounds.outWidth in 1..8192 && bounds.outHeight in 1..8192) { "Unsupported or oversized cover image" }
  val dir = File(context.filesDir, "covers").apply { mkdirs() }
  val file = File(dir, "$id.cover"); val tmp = File(dir, "$id.tmp")
  tmp.writeBytes(bytes); check(tmp.renameTo(file)) { "Could not save cover" }
  return file.absolutePath
 }
}
