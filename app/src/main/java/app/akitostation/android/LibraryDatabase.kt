package app.akitostation.android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LibraryDatabase(context: Context) : SQLiteOpenHelper(context, "library.db", null, 1) {
 override fun onCreate(db: SQLiteDatabase) {
  db.execSQL("CREATE TABLE roots(uri TEXT PRIMARY KEY, name TEXT NOT NULL)")
  db.execSQL("CREATE TABLE games(id TEXT PRIMARY KEY, uri TEXT UNIQUE NOT NULL, root TEXT NOT NULL, title TEXT NOT NULL, platform TEXT NOT NULL, bytes INTEGER NOT NULL, favorite INTEGER NOT NULL DEFAULT 0, lastLaunched INTEGER NOT NULL DEFAULT 0, artwork TEXT NOT NULL DEFAULT '', customTitle TEXT NOT NULL DEFAULT '', platformOverride TEXT)")
  db.execSQL("CREATE INDEX games_root ON games(root)")
 }
 override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { error("Unsupported database upgrade; existing data preserved") }
 fun roots(): List<LibraryRoot> = readableDatabase.query("roots", null, null, null, null, null, "name").use { c -> buildList { while(c.moveToNext()) add(LibraryRoot(c.getString(0), c.getString(1))) } }
 fun addRoot(root: LibraryRoot) { writableDatabase.insertWithOnConflict("roots", null, ContentValues().apply { put("uri", root.uri); put("name", root.name) }, SQLiteDatabase.CONFLICT_REPLACE) }
 fun removeRoot(uri: String) { writableDatabase.beginTransaction(); try { writableDatabase.delete("games", "root=?", arrayOf(uri)); writableDatabase.delete("roots", "uri=?", arrayOf(uri)); writableDatabase.setTransactionSuccessful() } finally { writableDatabase.endTransaction() } }
 fun game(id: String): Game? = readGames(id).firstOrNull()
 fun games(): List<Game> = readGames(null)
 private fun readGames(id: String?): List<Game> = readableDatabase.query("games", null, if(id == null) null else "id=?", id?.let { arrayOf(it) }, null, null, "title COLLATE NOCASE").use { c ->
  fun str(name: String) = c.getString(c.getColumnIndexOrThrow(name))
  fun num(name: String) = c.getLong(c.getColumnIndexOrThrow(name))
  buildList { while(c.moveToNext()) add(Game(str("id"), str("uri"), str("root"), str("title"), Platform.valueOf(str("platform")), num("bytes"), num("favorite") != 0L, num("lastLaunched"), str("artwork"), str("customTitle"), str("platformOverride")?.let(Platform::valueOf))) }
 }
 /** Called only after a complete successful root scan. Offline/failed roots never lose metadata. */
 fun mergeRoot(root: String, discovered: List<Game>) {
  val db = writableDatabase; db.beginTransaction()
  try {
   for (g in discovered) {
    val values = ContentValues().apply { put("id", g.id); put("uri", g.uri); put("root", root); put("title", g.title); put("platform", g.platform.name); put("bytes", g.bytes) }
    if (db.update("games", values, "id=?", arrayOf(g.id)) == 0) { values.put("artwork", g.artwork); db.insertOrThrow("games", null, values) }
   }
   // Retain missing entries so favorites, custom covers and corrections survive disconnected storage.
   db.setTransactionSuccessful()
  } finally { db.endTransaction() }
 }
 fun favorite(g: Game) { writableDatabase.execSQL("UPDATE games SET favorite = 1 - favorite WHERE id = ?", arrayOf(g.id)) }
 fun launched(g: Game, now: Long = System.currentTimeMillis()) = update(g.id, ContentValues().apply { put("lastLaunched", now) })
 fun metadata(id: String, title: String, platform: Platform?) = update(id, ContentValues().apply { put("customTitle", title.trim()); if(platform == null) putNull("platformOverride") else put("platformOverride", platform.name) })
 fun artwork(id: String, path: String) = update(id, ContentValues().apply { put("artwork", path) })
 private fun update(id: String, values: ContentValues) { writableDatabase.update("games", values, "id=?", arrayOf(id)) }
}
