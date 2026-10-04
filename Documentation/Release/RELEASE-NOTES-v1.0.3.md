# Android v1.0.3

- Bulk Scrape Box Art with missing-only and preserve/replace modes, progress, cancellation,
  summary and manual review of unmatched games. Reuses Libretro and existing consent.
- Persistent 2×2 through 6×6 library density in Games and Settings, responsive phone/tablet
  columns, fitted covers, readable titles and controller focus restoration.
- Same package and schema-1 library; build 4. Existing Compact preference migrates to 5×5;
  other users default to 4×4. No reimport required.

Validated with 155 JVM tests, phone/tablet 13/13 each and a production v1.0.2 upgrade. Same permanent production certificate; install over the existing app without uninstalling. Controller testing used simulated D-pad input; physical hardware was not tested in this run.
