# Pro Gallery X — Android Signing and Versioning Policy
**Scope:** Engineering Standards, CI/CD Pipeline, Release Engineering  
**Version:** 2.1  
**Status:** Mandatory Active Policy  

---

## 1. Principles and Objectives

To guarantee seamless upgrades, prevent installation failures on physical user devices, and maintain cryptographic chain-of-trust, Pro Gallery X enforces strict policies governing APK signing, semantic versioning, and artifact distribution.

---

## 2. Cryptographic Signing Policy

### 2.1 Supported Signing Schemes
All distributed APKs (both debug previews and release candidates) MUST be signed with:
1. **APK Signature Scheme v2 (Full APK binary hashing):** Mandatory for Android 7.0+ (API 24+). Protects against zip entry tampering and drastically accelerates installation speed on Android 12+ (One UI 4.1).
2. **JAR Signing (v1 scheme):** Maintained for broad compatibility.

### 2.2 Canonical Development Certificate
All non-Play Store distribution APKs (GitHub Releases, QA builds, internal testing) MUST be signed with the canonical project debug key:
- **Common Name (DN):** `C=US, O=Android, CN=Android Debug`
- **Key Algorithm:** RSA 2048-bit
- **Certificate SHA-256 Digest:**  
  `c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6`
- **Certificate SHA-1 Digest:**  
  `ba784da0ffe35d88a42ca8ab757bf2a5d6279ba9`

### 2.3 Protection of Canonical Keystore (No Keystore in Git)
- **Zero Keystore in Repository Rule:** Binary keystores (`.keystore`, `.jks`) are strictly forbidden from being committed into the Git repository to preserve security hygiene.
- **GitHub Secrets Storage:** The canonical debug keystore is securely stored as an encrypted repository secret (`SIGNING_KEYSTORE_BASE64`) in GitHub Actions.
- **Runner Decryption:** At workflow startup, GitHub Actions decodes the secret into the runner's isolated keystore path (`~/.android/debug.keystore`), guaranteeing every CI build signs with the canonical key.

### 2.4 Mandatory CI Signing Quality Gate
The CI workflow ([`.github/workflows/android.yml`](file:///C:/Users/loved/mmss/.github/workflows/android.yml)) strictly enforces this fingerprint on every pull request and push:
```bash
EXPECTED_SHA256="c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6"
ACTUAL_SHA256=$(grep "Signer #1 certificate SHA-256 digest:" apk-certs.txt | awk '{print $6}')
if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
  echo "::error::CRITICAL SIGNING ERROR: APK certificate does not match canonical project key!"
  exit 1
fi
```
Any build signed with an ephemeral or rogue key is rejected and blocked from publication.

---

## 3. Dynamic Versioning & Build Telemetry

### 3.1 Centralized Version Definition
Project versioning is governed by [`version.properties`](file:///C:/Users/loved/mmss/version.properties):

```properties
VERSION_MAJOR=2
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_BUILD=20003
VERSION_CHANNEL=rc
VERSION_NAME=2.0.0-rc2
```

### 3.2 Dynamic Git SHA and Build Timestamp
Hardcoding of Git commits or timestamps is strictly prohibited. `build.gradle.kts` dynamically computes them at build time:
- **Git Commit SHA:** Automatically queried from `GITHUB_SHA` (on CI) or via `git rev-parse --short HEAD` (local).
- **Build Timestamp:** Generated via `DateTimeFormatter.ISO_INSTANT.format(Instant.now())`.
- **In-App Exposure:** Embedded into `BuildConfig` (`GIT_COMMIT_SHA`, `BUILD_TIME`) and surfaced via the top-bar `AboutAppDialog`.

### 3.3 `versionCode` Formula and Monotonicity
The `versionCode` is a strictly positive, monotonically increasing 32-bit integer:
$$\text{versionCode} = (\text{MAJOR} \times 10000) + (\text{MINOR} \times 100) + \text{PATCH} + \text{CHANNEL\_OFFSET}$$

- Build 20003 represents Release Candidate 2.0.0-rc2.
- Monotonicity prevents version downgrade and ensures proper package manager upgrade behavior.

---

## 4. Standardized Distribution Naming Specification

All output APK assets distributed on GitHub Releases or via direct links MUST adhere to the naming convention:
```
ProGalleryX-<versionName>-<versionCode>-<gitCommitSha>-<buildType>.apk
```
**Current Release Asset:**  
`ProGalleryX-2.0.0-rc2-20003-ffa7eb8-debug.apk`

---

## 5. User Upgrade & Installation Protocol (Samsung Devices)

### Standard In-Place Upgrade (Recommended)
Because the new APK carries the exact canonical certificate matching previous official GitHub releases, **NO UNINSTALLATION OR DATA DELETION IS REQUIRED**:
1. Download the new APK: `ProGalleryX-2.0.0-rc2-20003-ffa7eb8-debug.apk`.
2. Tap the downloaded file in your browser or My Files.
3. Tap **Update (تحديث)**.
4. The system updates the app in-place. All photos, albums, favorites, and preferences remain 100% intact.

### Handling Signature Conflict (Edge Cases Only)
If a user previously installed a test build from an ephemeral CI artifact with an incompatible key, Android will display:
```text
INSTALL_FAILED_UPDATE_INCOMPATIBLE: Existing package com.progalleryx.app signatures do not match newer version
```
Only in this specific scenario is an uninstallation necessary:
1. Go to **Settings** -> **Apps** -> `Pro Gallery X` -> **Uninstall**.
2. Install the new verified canonical build.
*(Do not delete Secure Folder or user personal files unless a package conflict specifically within that container is confirmed).*
