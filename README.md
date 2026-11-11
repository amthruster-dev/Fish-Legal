# FishLegal

Offline-first fish species identification and fishing regulation lookup.

- Take a photo of a fish → on-device ML identifies the species (no internet needed)
- GPS locates which regulation zone you're in (offline point-in-zone lookup)
- Notification shows size/bag/season limits for that species in that zone
- When online, the app automatically syncs: downloads updated species data,
  regulation rules, and ML model improvements, and uploads your catch log queue

## Build

APKs are built automatically by GitHub Actions on every push to `main` and
published to the [`release/`](release/) folder and the repo's Releases page.

To build locally:
```
./gradlew assembleDebug
```
