package com.progalleryx.app.studioplus.ui.hub

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.progalleryx.app.studioplus.core.theme.SamsungBlue

/**
 * The Studio + Hub Screen:
 * Designed as an extensible modular foundation for client tools & creative services.
 */
@Composable
fun StudioPlusHubScreen(
    onOpenPhotoEditor: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    SamsungBlue.copy(alpha = 0.18f),
                                    Color(0xFF845EF7).copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(22.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(SamsungBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "استديو بلس + الإضافات",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "منظومة أدوات متكاملة مصممة لخدمتك بكل قوة واحترافية. الأساس الآن جاهز ومستقر لاستقبال كل أداة بشكل منفصل.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Section Title
        item {
            Text(
                text = "الأدوات والخدمات القادمة",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
            )
        }

        // Tool 1: Photo Studio V2
        item {
            StudioToolCard(
                title = "محرر الصور المتقدم",
                description = "محرر احترافي متكامل: قص حر وبنسب ثابتة، فلاتر سينمائية حقيقية، كتابة نصوص عربية بأشكال وألوان One UI، رسم بأقلام فسفورية وتصدير فائق الدقة.",
                icon = Icons.Outlined.Brush,
                accentColor = Color(0xFF2E6EEF),
                status = "جاهز للتشغيل",
                onClick = onOpenPhotoEditor
            )
        }

        // Tool 2: Video Studio V2
        item {
            StudioToolCard(
                title = "استوديو صناعة الفيديو",
                description = "قص المقاطع، دمج الفيديوهات، تصدير عالي الدقة، وإضافة التأثيرات الصوتية بتزامن دقيق.",
                icon = Icons.Outlined.MovieCreation,
                accentColor = Color(0xFFE03131),
                status = "المرحلة القادمة"
            )
        }

        // Tool 3: PDF & Document Suite
        item {
            StudioToolCard(
                title = "أدوات PDF والمستندات",
                description = "تحويل الصور إلى مستندات PDF احترافية بضغطة زر مع الحفاظ على وضوح الخطوط وجودة الطباعة.",
                icon = Icons.Outlined.PictureAsPdf,
                accentColor = Color(0xFFE8590C),
                status = "مخطط"
            )
        }

        // Tool 4: Storage Cleaner & Duplicate Scanner
        item {
            StudioToolCard(
                title = "تنظيف الصور والمكررات",
                description = "فحص ذكي للصور المتطابقة والمتقاربة لتوفير مساحة الهاتف وتسريعه بأمان تام.",
                icon = Icons.Outlined.CleaningServices,
                accentColor = Color(0xFF0CA678),
                status = "مخطط"
            )
        }
    }
}

@Composable
private fun StudioToolCard(
    title: String,
    description: String,
    icon: ImageVector,
    accentColor: Color,
    status: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .clickable(enabled = onClick != null) { onClick?.invoke() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = status,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
    }
}
