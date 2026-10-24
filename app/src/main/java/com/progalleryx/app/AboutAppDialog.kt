package com.progalleryx.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun AboutAppDialog(
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    val pkgInfo = remember {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    android.content.pm.PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
        } catch (e: Exception) {
            null
        }
    }

    val installedVersionName = pkgInfo?.versionName ?: BuildConfig.VERSION_NAME
    val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        pkgInfo?.longVersionCode ?: BuildConfig.VERSION_CODE.toLong()
    } else {
        @Suppress("DEPRECATION")
        pkgInfo?.versionCode?.toLong() ?: BuildConfig.VERSION_CODE.toLong()
    }

    val gitSha = BuildConfig.GIT_COMMIT_SHA
    val buildTime = BuildConfig.BUILD_TIME
    val channel = BuildConfig.RELEASE_CHANNEL.uppercase()
    val expectedCert = BuildConfig.EXPECTED_SIGNING_SHA256

    val diagnosticReport = remember {
        """
        === PRO GALLERY X DIAGNOSTIC AUDIT ===
        Package Name: ${context.packageName}
        Installed Version Name: $installedVersionName
        Installed Version Code: $installedVersionCode
        Build Config Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
        Distribution Channel: $channel
        Git Commit SHA: $gitSha
        Build Time: $buildTime
        Android OS: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
        Device Hardware: ${Build.MANUFACTURER} ${Build.MODEL} (${Build.PRODUCT})
        Target SDK: ${context.applicationInfo.targetSdkVersion}
        Min SDK: ${context.applicationInfo.minSdkVersion}
        Expected Signing Cert (SHA-256): $expectedCert
        Data Dir: ${context.applicationInfo.dataDir}
        =======================================
        """.trimIndent()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "حول Pro Gallery X" else "About Pro Gallery X",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = "CHANNEL: $channel",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Version Details Grid
                DetailRow(
                    label = if (isArabic) "رقم الإصدار (Version Name):" else "Version Name:",
                    value = installedVersionName
                )
                DetailRow(
                    label = if (isArabic) "كود الإصدار (Version Code):" else "Version Code:",
                    value = "$installedVersionCode"
                )
                DetailRow(
                    label = if (isArabic) "معرف الـ Commit:" else "Git Commit:",
                    value = gitSha
                )
                DetailRow(
                    label = if (isArabic) "تاريخ البناء:" else "Build Timestamp:",
                    value = buildTime
                )
                DetailRow(
                    label = if (isArabic) "الجهاز والنظام:" else "OS & Device:",
                    value = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) • ${Build.MODEL}"
                )

                Spacer(Modifier.height(12.dp))

                // Certificate Signature Verification Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isArabic) "بصمة التوقيع المعتمدة (SHA-256)" else "Verified Signing Fingerprint",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = expectedCert,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Actions: Copy Diagnostics & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Pro Gallery X Diagnostics", diagnosticReport)
                            clipboard.setPrimaryClip(clip)
                            copied = true
                            Toast.makeText(
                                context,
                                if (isArabic) "تم نسخ بيانات التشخيص إلى الحافظة" else "Diagnostics copied to clipboard",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (copied) Icons.Outlined.CheckCircle else Icons.Outlined.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (isArabic) "نسخ التشخيص" else "Copy Info")
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isArabic) "إغلاق" else "Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
