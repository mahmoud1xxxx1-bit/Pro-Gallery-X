# Pro Gallery X — Critical Installation Root Cause Analysis
**Target Acceptance Device:** Samsung Galaxy M31 (SM-M315F), Android 12 (One UI 4.1)  
**Package Name:** `com.progalleryx.app`  
**Date of Audit:** October 24, 2026  
**Engineering Directive:** P0 Emergency Installation Recovery & Forensic Audit  

---

## 1. Executive Summary

A critical issue prevented installation and testing of Pro Gallery X APKs on real devices, particularly the primary acceptance device (Samsung Galaxy M31 running Android 12). Users reported:
1. The installation prompt hanging on **"جارٍ التثبيت..." (Installing...)** indefinitely before terminating or failing without an explicit error explanation.
2. Older versions of the application reappearing after attempts to delete or reinstall.
3. Inability to distinguish whether a newly installed APK was genuinely the latest build or an earlier obsolete artifact.

A forensic audit of all historical GitHub releases (`preview-batch1`, `preview-studio-rebuild`, `preview-studio-v2`, `v2.0.0-rc1-review`), coupled with runtime reproduction on a live Android 12 (API 32) system image, identified **four interlocking root causes**. None of these issues were transient glitches; they were architectural omissions in build versioning, signature lifecycle, and distribution hygiene.

---

## 2. Root Cause 1: Mismatched Signing Keys across Distribution Paths

### The Mechanism
Android's `PackageManager` enforces strict cryptographic signing guarantees. An installed application can only be upgraded by an APK signed with an identical certificate (unless APK Signature Scheme v3/v3.1 key rotation with proof-of-rotation is explicitly configured).

When an APK update is initiated:
1. `PackageInstaller` copies the incoming APK to `/data/app/vmdl<session_id>.tmp`.
2. `PackageManagerService` parses the package, extracts the signing certificate, and queries `PackageManager.getPackageInfo(packageName, GET_SIGNING_CERTIFICATES)`.
3. If the certificates do not match, `PackageManager` throws `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.

### The Distribution Discrepancy
- **GitHub Releases:** Uploaded manually from a local Windows development environment signed with a persistent debug keystore (`SHA-256: c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6`).
- **GitHub Actions CI Artifacts:** Run on ephemeral `ubuntu-latest` virtual machines. Gradle automatically generates a fresh `~/.android/debug.keystore` on every individual workflow run when no custom keystore is supplied. Consequently, each CI build had a different, ephemeral certificate.
- **The Failure on Device:** If a user installed an APK downloaded from CI artifacts and subsequently downloaded an APK from GitHub Releases (or vice versa), Android detected a signature clash.

### Why Samsung One UI Displayed "جارٍ التثبيت..." Indefinitely
On Samsung One UI 4.1 (Android 12), `com.samsung.android.packageinstaller` runs a modal dialog. When a signature conflict occurs, the system initiates a verification handshake that rejects the staged session. In stock One UI, rather than surfacing a developer-centric dialog ("Signatures do not match"), the system package installer displays "جارٍ التثبيت..." while awaiting session rollback or timeout, eventually closing or presenting a generic "التطبيق ليس مثبتاً" (App not installed) without explaining why.

```text
// Reproduction Logcat on Android 12 Runtime:
W PackageManager: Existing package com.progalleryx.app signatures do not match newer version; ignoring!
D PackageInstallerSession: Marking session 1831754979 as failed: INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.progalleryx.app signatures do not match newer version; ignoring!
```

---

## 3. Root Cause 2: Version Code Stagnation (`versionCode = 1`, `versionName = "0.1.0"`)

### Forensic Analysis of Historical Releases
Every single APK distributed across GitHub Releases was inspected with `aapt dump badging`:

| Release Tag | File Name | Size (Bytes) | SHA-256 (File) | `versionCode` | `versionName` |
|---|---|---|---|---|---|
| `preview-batch1` | `app-debug.apk` | 22,685,195 | `1fc7341f...` | **1** | **0.1.0** |
| `preview-studio-rebuild` | `app-debug.apk` | 22,782,447 | `ac1b6a56...` | **1** | **0.1.0** |
| `preview-studio-v2` | `app-debug.apk` | 22,900,559 | `0aeac6abe...` | **1** | **0.1.0** |
| `v2.0.0-rc1-review` | `app-debug.apk` | 23,206,273 | `33585e93...` | **1** | **0.1.0** |

### Consequences of Stagnant Versioning
1. **No Upgrade Semantics:** Android treats two APKs with identical `versionCode` as a re-install rather than an upgrade.
2. **User Blindness:** In Samsung One UI Settings -> Apps -> Pro Gallery X, the version displayed was always `0.1.0`. The user could never verify if their phone had actually received the new build or remained on a previous build.
3. **Downgrade Inversion:** If an older build was installed, installing another build with `versionCode=1` could not trigger standard version comparison or downgrade protection.

---

## 4. Root Cause 3: Samsung Knox Secure Folder & Multi-User Profile Isolation

### The Knox Dual-Persona Architecture
Samsung Galaxy M31 devices feature Samsung Knox Secure Folder (المجلد الآمن) and Dual Messenger. Under Android's multi-user framework:
- Primary Personal Space = `User 0` (`UserInfo{0:Owner:13}`)
- Secure Folder Space = `User 150` (or `User 10` work profile)

### How App Ghosts Occur
1. If the user previously cloned or installed Pro Gallery X inside Secure Folder (or if Secure Folder automatically backed up media app state), `com.progalleryx.app` existed in `User 150`.
2. When the user uninstalls the app from the standard home screen, Android executes `pm uninstall --user 0 com.progalleryx.app`.
3. The package data is wiped for User 0, **but the package remains installed on the device for User 150**.
4. When the user downloads a new APK with a different signing key and attempts to install it in User 0:
   - Android checks system-wide package signatures across **all users**.
   - It discovers that `com.progalleryx.app` is already present on disk in User 150 signed with Key A.
   - It refuses to install the Key B APK in User 0, throwing `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
5. In addition, launcher search and Samsung Finder show the app icon from Secure Folder, leading the user to believe the app was never uninstalled.

---

## 5. Root Cause 4: Generic APK Asset Naming & Browser Collision

All four historical releases published APKs named simply `app-debug.apk`.
When downloading sequential builds from Chrome or Samsung Internet:
1. First download: `app-debug.apk`
2. Second download: `app-debug (1).apk`
3. Third download: `app-debug (2).apk`

When users click the notification or open My Files (ملفاتي), opening `app-debug.apk` repeatedly installs the oldest, initial download rather than the newest file. This created the illusion that updates were failing or having no effect.

---

## 6. Corrective Architecture Implemented

1. **Centralized Versioning (`version.properties`):**
   - Implemented a monotonic build counter (`VERSION_BUILD=20002`) and semantic versioning (`VERSION_NAME=2.0.0-rc2`).
   - Integrated build metadata into `BuildConfig` (`GIT_COMMIT_SHA`, `BUILD_TIME`, `RELEASE_CHANNEL`).

2. **In-App Diagnostic Verification (`AboutAppDialog`):**
   - Added an interactive "About App" dialog accessible from the top bar on both Arabic (RTL) and English layouts.
   - Displays real-time version name, version code, Git commit SHA, build timestamp, Android OS / API level, and the cryptographic certificate SHA-256 fingerprint.
   - Added a "Copy Diagnostics" button to allow users to verify their installed version with zero ambiguity.

3. **Standardized Distribution Naming:**
   - Distribution APKs are now strictly formatted as:
     `ProGalleryX-<versionName>-<versionCode>-<gitSha>-debug.apk`
   - Current release: `ProGalleryX-2.0.0-rc2-20002-6c0b403-debug.apk`.

4. **CI Quality Gate:**
   - Updated `.github/workflows/android.yml` to verify `version.properties`, assert package metadata and SDK levels with `aapt`, verify APK signing schemes, generate a `SHA256SUMS` file, and upload uniquely versioned artifacts.
