package app.akitostation.android
import java.io.ByteArrayOutputStream
import java.io.InputStream
fun InputStream.readBounded(limit: Int): ByteArray {
 val output = ByteArrayOutputStream(); val buffer = ByteArray(minOf(limit, 8192))
 while(output.size() < limit) { val count = read(buffer, 0, minOf(buffer.size, limit - output.size())); if(count < 0) break; if(count == 0) continue; output.write(buffer, 0, count) }
 return output.toByteArray()
}
