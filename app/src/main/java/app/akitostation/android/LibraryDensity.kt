package app.akitostation.android

/** Presets keep the original 160dp/120dp layouts at Medium/Compact. */
enum class LibraryDensity(val label: String, val targetWidth: Int) {
 TWO("2×2", 280), THREE("3×3", 210), FOUR("4×4", 160), FIVE("5×5", 120), SIX("6×6", 96);
 fun columns(widthDp: Float, fontScale: Float = 1f): Int =
  ((widthDp - 40 + 20) / (targetWidth * fontScale.coerceAtLeast(1f) + 20)).toInt().coerceIn(1, ordinal + 2)
}
