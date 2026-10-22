# Delivery plan — Pro Gallery X

## Phase 1 — Native gallery (current)
- [x] Native Kotlin + Compose application foundation.
- [x] No user-media uploads or cloud dependencies.
- [x] MediaStore image/video access with runtime permission request.
- [x] Incremental MediaStore query, bounded thumbnails.
- [x] Photo/video filter chips.
- [x] Tap photo to preview, open video in external player, share.
- [x] CI debug APK build workflow.
- [x] CI debug APK build completed successfully on 2026-10-23 (run 39857966928, branch ci/phase1-verify-20261023); artifact Pro-Gallery-X-debug uploaded.
- [ ] Confirm APK install and runtime on a physical Android device.
- [ ] Test real device startup, permissions, 1k / 10k / 50k / 100k assets.
- [x] Album selector, local favorites, Android 11+ system-confirmed trash implemented (device behavior remains untested).
- [ ] Screen reader, Arabic RTL and slow-device polish.

Phase 1 is **NOT ACCEPTED** until the missing tasks pass.

## Phase 2 — Photo and video editors
- [x] Initial offline 90-degree JPEG rotation and export with bounded decoding (CI #24; device testing pending).
- Non-destructive edit history, crop, rotate, tone, HSL, curves, text and batch exports.
- Multi-clip video editor, trim, split, speed, text, audio, resolution controls.
- Safe offline export and recovery, thermal/memory constraints.

- [x] Initial on-device Media3 1.11.1 single-video trim and MP4 export; CI #32 passed. Physical playback/output quality tests pending. This is NOT a multitrack editor.

## Phase 3 — Documents, privacy, lifetime Pro, release
- [x] Initial local multi-image PDF export (up to 100 images; CI #24; device testing pending).
- Local image-to-PDF, rearrange pages, merge, PDF scan and tested OCR languages.
- Encrypted private vault and duplicate detection with user-approved deletion.
- Lifetime, non-consumable Google Play purchase; verify/recover entitlement.
- Accessibility, device matrix testing, privacy declaration, signed AAB and store listing.

## Non-negotiable constraints
No server for media; no recurring per-user image/video processing fees.
No ads SDK, no third-party photo tracking. Originals never overwritten automatically.
Do not market a module as finished before functional tests pass.
All third-party libraries and ML models require commercially compatible licensing.
