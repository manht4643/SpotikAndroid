package com.spotik.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotik.app.ui.components.GlassCard
import com.spotik.app.ui.theme.*

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // ── Title ──
            Text(
                text = "Профиль",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Avatar ──
            AvatarSection(name = "Артём", age = 22, city = "Альметьевск")

            Spacer(modifier = Modifier.height(32.dp))

            // ── Stats ──
            StatsRow(likes = 48, views = 215, referrals = 3)

            Spacer(modifier = Modifier.height(24.dp))

            // ── Bio ──
            Text(
                text = "О себе",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            GlassCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Занимаюсь программированием и спортом 💪\nИщу свою половинку",
                    color = TextPrimary.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Premium banner ──
            PremiumBanner()

            Spacer(modifier = Modifier.height(24.dp))

            // ── Settings ──
            Text(
                text = "Настройки",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsMenu()

            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

// ── Avatar ──────────────────────────────────────────────────────
@Composable
private fun AvatarSection(name: String, age: Int, city: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(CardDark),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = name.first().toString(),
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Name, age
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = name,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Text(
                text = ", ",
                fontSize = 22.sp,
                color = TextPrimary,
            )
            Text(
                text = "$age",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // City
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "📍", fontSize = 13.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = city,
                fontSize = 14.sp,
                color = TextSecondary,
            )
        }
    }
}

// ── Stats ───────────────────────────────────────────────────────
@Composable
private fun StatsRow(likes: Int, views: Int, referrals: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatBlock(value = "$likes", label = "Лайков", accent = AccentLike, modifier = Modifier.weight(1f))
        StatBlock(value = "$views", label = "Просмотров", accent = Accent, modifier = Modifier.weight(1f))
        StatBlock(value = "$referrals", label = "Рефералов", accent = NavGlow, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatBlock(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    GlassCard(
        modifier = modifier.aspectRatio(1f),
        padding = 14.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(accent),
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = TextMuted,
            )
        }
    }
}

// ── Premium ─────────────────────────────────────────────────────
@Composable
private fun PremiumBanner() {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Premium.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "👑", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Spotik Premium",
                    fontWeight = FontWeight.SemiBold,
                    color = Premium,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Безлимитные лайки и приоритет в показах",
                    color = TextSecondary,
                    fontSize = 12.sp,
                )
            }
            Text(text = "›", fontSize = 22.sp, color = Premium)
        }
    }
}

// ── Settings ────────────────────────────────────────────────────
@Composable
private fun SettingsMenu() {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        padding = 0.dp,
    ) {
        Column {
            MenuItem(icon = "✏️", label = "Редактировать профиль")
            MenuDivider()
            MenuItem(icon = "📷", label = "Обновить фото")
            MenuDivider()
            MenuItem(icon = "🔗", label = "Пригласить друзей")
            MenuDivider()
            MenuItem(icon = "🌙", label = "Тёмная тема")
            MenuDivider()
            MenuItem(icon = "🔔", label = "Уведомления")
            MenuDivider()
            MenuItem(icon = "❓", label = "Помощь")
        }
    }
}

@Composable
private fun MenuItem(icon: String, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = icon, fontSize = 18.sp)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            fontSize = 15.sp,
        )
        Text(text = "›", fontSize = 16.sp, color = TextMuted)
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 16.dp)
            .height(0.5.dp)
            .background(BorderDark),
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ProfileScreenPreview() {
    SpotikTheme {
        ProfileScreen()
    }
}
