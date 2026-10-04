# Android privacy

Akito Station stores game document references, titles, platforms, file sizes, favorites, last-launch timestamps, cover artwork and runtime/settings choices on the device. It does not collect analytics, upload games, register an account or process payments. Android document providers and external emulator apps have their own privacy policies.

Networking occurs when you explicitly check updates against your configured public GitHub repository. GitHub receives normal network request information, including IP address. Opening a release uses your browser. No library metadata is sent in the update request.

Android backup is disabled to avoid accidental backup of private game references. Akito's private database and imported covers are removed on uninstall. Games and external emulator saves are not deleted by unregistering or uninstalling Akito. There is no metadata export/restore flow in this version; users should understand this limitation before uninstalling.

## Online cover-art search (1.0.1)

Disabled by default, including upgrades. Before using online search, Akito presents a disclosure and explicit Allow and continue / Cancel choices. The choice is stored in private preferences; Settings → Online cover art lets you review and revoke it. No background library-wide search runs.

Libretro Thumbnails (https://thumbnails.libretro.com) receives the selected system/platform as part of its catalog URL. Game title matching happens locally, so the typed query is not sent to a web search engine. Previews and selected-cover downloads send the cover title and platform encoded in the cover URL. The provider sees normal HTTPS connection information including IP address and Akito's user agent. Covers are cached by the image loader and copied to application-private artwork storage when applied. No ROM contents, ROM hashes, saves, filesystem paths, credentials or unrelated library entries are sent for cover search. No Google or TheGamesDB requests are made by this implementation.

Revoking permission blocks subsequent searches/downloads and prevents results from being displayed; existing applied artwork remains on device. An already dispatched request cannot be recalled. Provider availability and catalog coverage vary. Local image import remains available offline.

Bulk artwork scraping uses the same consent and provider as manual cover search. Platform catalogs are fetched and searched locally; selected image URLs are downloaded. ROM contents, hashes, saves, credentials and filesystem paths are never uploaded. Grid density is stored locally in existing app preferences. No additional service or analytics is introduced.
