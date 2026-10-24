# Pro Gallery X — Forensic APK Release Audit
**Audit Date:** October 25, 2026  
**Audited Artifacts:** All Historical GitHub Releases + New Versioned Release Candidate (Build 20003)  
**Auditing Tools:** `aapt` (Build Tools 35.0.0), `apksigner` (Build Tools 34.0.0), Python cryptographic hash libraries  

---

## 1. Overview of Audited Artifacts

| Release Tag | File Name | Size (Bytes) | SHA-256 File Hash |
|---|---|---|---|
| `preview-batch1` | `app-debug.apk` | 22,685,195 | `1fc7341f5abc3a0531e8e3f72f890aa6bee59b8d9e4f2f052bfc30ead65a532f` |
| `preview-studio-rebuild` | `app-debug.apk` | 22,782,447 | `ac1b6a56dc787e13bd1e18a992e8356a1672c36221c91239018975c70d824ad1` |
| `preview-studio-v2` | `app-debug.apk` | 22,900,559 | `0aeac6abe1a1844f4487c81d6dfad8c1532dad0e7b13d8f9f254fc8c19edc88b` |
| `v2.0.0-rc1-review` | `app-debug.apk` | 23,206,273 | `33585e93d9de2c641f104df47529c1634fdd173d64633c0d117da0fd81fdf55d` |
| **New Release (`rc2` Build 20003)** | `ProGalleryX-2.0.0-rc2-20003-ffa7eb8-debug.apk` | **22,866,733** | **`1ff2b4bbbf257fc27ff59511b30652220c23f7f9e0b5deaf8f551ca24d1c53e5`** |

---

## 2. Forensic Badging & Version Metadata

Using `aapt dump badging <apk>` across all artifacts:

```text
================================================================================
NEW AUDITED RELEASE: ProGalleryX-2.0.0-rc2-20003-ffa7eb8-debug.apk
================================================================================
package: name='com.progalleryx.app' versionCode='20003' versionName='2.0.0-rc2'
sdkVersion:'26'
targetSdkVersion:'36'
application-label:'Pro Gallery X'
application-label-ar:'Pro Gallery X'
```

---

## 3. Cryptographic Certificate Signer Analysis

Using `apksigner verify --verbose --print-certs <apk>`:

### Canonical Certificate Verification
```text
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Number of signers: 1
Signer #1 certificate DN: C=US, O=Android, CN=Android Debug
Signer #1 certificate SHA-256 digest: c1ea4285c43ccc6da990b93e9a323494609aaa68dbe2d1f500c24c3d6956c1f6
Signer #1 certificate SHA-1 digest: ba784da0ffe35d88a42ca8ab757bf2a5d6279ba9
Signer #1 certificate MD5 digest: 3f9c954673c4806926c765a668a95c16
Signer #1 key algorithm: RSA (2048-bit)
```

**Key Finding:** Build 20003 carries the **exact canonical certificate** matching all historical GitHub releases. In-place upgrade over previous releases succeeds without requiring data deletion.

---

## 4. Key Comparative Differences

| Metric / Property | Historical Releases (`rc1` and prior) | New Release (`2.0.0-rc2` Build 20003) | Impact & Benefit |
|---|---|---|---|
| **Version Code** | `1` (Stagnant across all PRs) | **`20003`** | Enables standard Android upgrade semantics, prevents rollback. |
| **Version Name** | `0.1.0` (Unchanged) | **`2.0.0-rc2`** | User and system can clearly identify build in App Info. |
| **Git Commit SHA** | None / Hardcoded | **`ffa7eb8` (Dynamic)** | Queried dynamically from Git HEAD at build time. |
| **Build Timestamp** | Stale / Hardcoded | **Dynamic ISO-8601** | Generated in real-time at build invocation. |
| **File Naming** | Generic `app-debug.apk` | **`ProGalleryX-2.0.0-rc2-20003-ffa7eb8-debug.apk`** | Eliminates browser download collision (`app-debug (1).apk`). |
| **In-App Verification** | None | **`AboutAppDialog` with Copy Diagnostics** | Displays commit SHA, build time, and verified signing fingerprint directly on device. |
| **CI Automation** | Ephemeral keys on CI artifacts | **Protected secret + mandatory verification** | CI strictly validates certificate against canonical fingerprint. |
