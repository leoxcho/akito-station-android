# Cover search

From game editing choose Search Cover Art. Consent defaults off; the disclosure also appears in Settings. Enter/refine the prefilled game title, search the current system or choose All systems. Select a result then Apply cover. Artwork is downloaded within a 16 MB limit and validated against raster image dimension limits, saved atomically under a fresh private filename, recorded in the existing database and emitted through library state. New filenames avoid stale image-cache keys. Local import uses the same storage validation.

The Android implementation follows the macOS editor and Libretro directory provider behavior. It deliberately uses only the official Libretro directory provider, not the macOS TheGamesDB HTML fallback. Catalogs are cached in memory, requests are spaced, failures impose a cooldown, connections time out, redirects and foreign/traversal URLs are rejected, and responses are bounded. Queries strip recognized dump tags, fold accents/punctuation/case, preserve legitimate parenthesized words and sequel numbers, and match whole tokens. All systems includes editions for other systems. Maximum 120 results. No-result and network/provider failures are distinct and offer retry/local import.

All adapter platforms are mapped; recognized platforms without an available catalog may report no results/provider failure. Availability is not guaranteed. No automatic/bulk scraping or remote accounts. Runtime routing remains unchanged.

## Bulk scraping (v1.0.3)

Games → Scrape Box Art offers Missing Artwork Only (default) and All Games.
All Games keeps existing covers unless Replace existing artwork is explicitly selected.
Uses existing online-artwork consent and Libretro Thumbnails only. Catalogs are cached
for the run, searched locally, and constrained to the game's platform. Automatic matches
require identical normalized titles; sequel/subtitle differences are rejected. Equivalent
regional variants use a deterministic catalog URL. Unsupported platforms and unmatched
titles stay unchanged and can be opened through Review Missing Artwork for manual search.

One game is processed at a time off the UI thread. Temporary I/O/429/5xx failures receive
up to two retries with 500ms/1s backoff; failed hosts then cool down. Completed images are
validated, installed atomically, and stored by content hash to share identical downloads
on disk. Existing image files are retained. Cancellation stops subsequent work; completed
updates remain. Process termination leaves completed covers intact; a new missing-only run
skips them. Background execution is best effort; there is no persistent background worker.

## Library density (v1.0.3)

Games → View / Grid Size and Settings → Library view share 2×2, 3×3, 4×4,
5×5 and 6×6 density presets. These target 280, 210, 160, 120 and 96dp widths,
with fewer columns on phones and more on tablets. Font scaling reduces column count.
4×4 is the default; existing Compact users default to 5×5. The preference persists in
the existing station preferences, independent of search, favorites, recent and system filters.
Cards retain at least 48dp touch targets, two-line titles and proportional fitted artwork.
The library scrolls vertically and composes only visible cards. Stable keys retain game
identity; density changes bring the most recently focused game into view and restore focus.
