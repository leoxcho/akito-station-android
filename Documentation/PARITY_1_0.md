# Android 1.0 parity decision

Reference: current canonical macOS Public source, read-only. Android adapts the frontend to document providers and external Android apps; desktop paths and embedded macOS engines do not transfer directly.

## Required for 1.0

Native library browsing, bounded recursive import, system identification/manual correction, title editing, artwork import/replacement/removal, favorites, title/system search and filtering, recent accepted launches, sort/grid settings, settings persistence, phone/tablet layout, controller focus navigation, explicit runtime selection, missing-runtime errors, original-file preservation, Android-only update checking, owner signing process and device acceptance checklist are implemented. Automated acceptance is distinct from physical device/gameplay acceptance. Production signing and physical acceptance remain owner gates before publication.

## Optional / post-1.0

Bulk scraping, cosmetic themes and desktop layouts, PRO accounts/payment/restore, cloud/metadata backup, achievements, play-time measurement, save-state/profile browsers, embedded emulator rendering, automated runtime installation/updating and further system adapters. Existing local artwork editing is usable without online search. PRO is not sold or advertised as active. These gaps do not block a free Android frontend 1.0 candidate.

The earlier Google browser-search proposal was not applied. Android 1.0.1 resolves online artwork privacy through explicit saved consent and native Libretro catalog search; no Google requests are made.

Android 1.0.1 implements integrated online artwork search with saved consent, native results selection/application and immediate card refresh; see COVER_SEARCH.md.
