# Pro Gallery X — Android Signing and Versioning Policy
**Scope:** Engineering Standards, CI/CD Pipeline, Release Engineering  
**Version:** 2.0  
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

### 2.3 Prohibition of Ephemeral CI Keystores
- **Rule:** CI/CD runners (e.g. GitHub Actions) MUST NOT distribute APKs to end-users that were signed using randomly generated, runner-ephemeral keystores.
- **Enforcement:** If a CI workflow publishes artifacts intended for user device installation, it must utilize a secure repository secret (`DEBUG_KEYSTORE_BASE64`) matching the canonical certificate fingerprint. Any artifact carrying a rogue or ephemeral key must be tagged strictly as `ci-test-only` and never uploaded to GitHub Releases.

---

## 3. Versioning Scheme and Increment Policy

### 3.1 Centralized Version Definition
Project versioning is governed strictly by [`version.properties`](file:///C:/Users/loved/mmss/version.properties):

```properties
VERSION_MAJOR=2
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_BUILD=20002
VERSION_CHANNEL=rc
VERSION_NAME=2.0.0-rc2
GIT_COMMIT_SHA=6c0b403
```

### 3.2 `versionCode` Formula and Monotonicity
The `versionCode` is a strictly positive, monotonically increasing 32-bit integer. It MUST follow the formula:
$$\text{versionCode} = (\text{MAJOR} \times 10000) + (\text{MINOR} \times 100) + \text{PATCH} + \text{CHANNEL\_OFFSET}$$

- Major Release 2.0.0 RC2: $\text{versionCode} = 20002$
- Every subsequent PR, build, or patch MUST increment `VERSION_BUILD`.
- **Zero Stagnation Rule:** A PR or release candidate with a stagnant `versionCode` (such as `1`) will be rejected by CI pre-merge checks.

### 3.3 `versionName` Conventions
Format: `<major>.<minor>.<patch>[-<channel>]`
- Alpha builds: `2.0.0-alpha1`
- Beta builds: `2.0.0-beta1`
- Release candidates: `2.0.0-rc1`, `2.0.0-rc2`
- Stable releases: `2.0.0`

---

## 4. Standardized Distribution Naming Specification

All output APK assets distributed on GitHub Releases or via direct links MUST adhere to the following naming convention:
```
ProGalleryX-<versionName>-<versionCode>-<gitCommitSha>-<buildType>.apk
```
**Example:**  
`ProGalleryX-2.0.0-rc2-20002-6c0b403-debug.apk`

Generic names such as `app-debug.apk` are strictly forbidden for distribution assets to prevent browser file collisions (`app-debug (1).apk`) on user devices.

---

## 5. User Migration & Clean Installation Guide (Samsung Devices)

If a user previously encountered `INSTALL_FAILED_UPDATE_INCOMPATIBLE` or has lingering ghost installations from early testing, follow this exact recovery protocol on Samsung Galaxy devices (M31, S-series, A-series):

### Step 1: Check Standard Profile Uninstall
1. Go to **Settings (الضبط)** -> **Apps (التطبيقات)**.
2. Search for `Pro Gallery X`.
3. If present, tap **Uninstall (إلغاء التثبيت)**.
4. If a dialog asks *"Keep app data?" (الاحتفاظ ببيانات التطبيق؟)*, **UNCHECK** it to ensure clean signature removal.

### Step 2: Check Samsung Secure Folder (المجلد الآمن)
If the device has Secure Folder enabled:
1. Open **Secure Folder (المجلد الآمن)**.
2. Look for `Pro Gallery X` in the Secure Folder apps list.
3. If present, tap and hold -> **Uninstall (إلغاء التثبيت)**.
4. *Explanation:* Because Samsung Knox isolates Secure Folder as a separate user space (`User 150`), an app existing inside Secure Folder prevents the main user (`User 0`) from installing an APK with a differing signature.

### Step 3: Check Dual Messenger / Work Profile
1. Go to **Settings** -> **Advanced Features** -> **Dual Messenger**.
2. Verify that Pro Gallery X is not configured as a dual app.

### Step 4: ADB Command Recovery (For Developers / Testers)
If ADB is connected, execute:
```bash
# Uninstall package completely across all user profiles
adb uninstall com.progalleryx.app

# If still lingering on Knox profiles:
adb shell pm uninstall --user 0 com.progalleryx.app
adb shell pm uninstall --user 150 com.progalleryx.app

# Clean install of the verified build
adb install -r ProGalleryX-2.0.0-rc2-20002-6c0b403-debug.apk
```
