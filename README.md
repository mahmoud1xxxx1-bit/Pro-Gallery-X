# Pro Gallery X

Privacy-first, offline Android gallery. Native Kotlin + Jetpack Compose.

## Phase 1 scope (in development)
- Device photo and video browsing via Android MediaStore.
- Paginated grid, thumbnails, albums later; no cloud accounts or server.
- Android photo/video permissions and full-screen image viewer.
- Clean Arabic-first Material 3 visual identity.
- GitHub Actions Android debug build.

## Product roadmap
1. Reliable gallery + performance tests with 1k/10k/100k media items.
2. Non-destructive image editor and local export.
3. Local PDF and documents.
4. On-device video editor.
5. Optional offline intelligence, local encrypted vault.
6. One-time Pro purchase using Google Play Billing.

**Do not claim features beyond what has been implemented.** No subscription and no uploaded user media. This repository is public: do not add credentials or signing keys.

## Build
Open with Android Studio (JDK 17) and sync Gradle, or run `gradle :app:assembleDebug` with Android SDK 36 installed. CI publishes a debug APK artifact if successful.
