package com.spotik.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spotik.app.data.api.LoginRequest
import com.spotik.app.data.api.RetrofitClient
import com.spotik.app.data.auth.AuthManager
import com.spotik.app.ui.components.GlassCard
import com.spotik.app.ui.components.MeshGradientBackground
import com.spotik.app.ui.effects.peekPop
import com.spotik.app.ui.theme.*
import kotlinx.coroutines.launch

/**
 * Login screen — email + password, with Telegram login option.
 *
 * @param onLoggedIn  called after successful login
 * @param onGoToRegister  navigate to 5-step registration
 */
@Composable
fun AuthScreen(
    onLoggedIn: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    val p = LocalSpotikPalette.current
    val scope = rememberCoroutineScope()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    /* Entrance animation */
    val enterScale = remember { Animatable(0.92f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { enterAlpha.animateTo(1f, tween(500)) }
        enterScale.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
    }

    MeshGradientBackground {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .graphicsLayer {
                    scaleX = enterScale.value
                    scaleY = enterScale.value
                    alpha = enterAlpha.value
                },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(80.dp))

                /* Logo / Title */
                Box(
                    Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(p.accent, p.calmMid),
                            )
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("S", fontSize = 36.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FindSansFont, color = Color.White)
                }

                Spacer(Modifier.height(20.dp))
                Text("Spotik", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FindSansFont, color = p.textPrimary)
                Spacer(Modifier.height(6.dp))
                Text("Войди, чтобы продолжить", fontSize = 14.sp,
                    fontFamily = InterFont, color = p.textSecondary)

                Spacer(Modifier.height(40.dp))

                /* ── Login form ── */
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        val fc = TextFieldDefaults.colors(
                            focusedTextColor = p.textPrimary,
                            unfocusedTextColor = p.textPrimary,
                            cursorColor = p.accent,
                            focusedContainerColor = p.cardDark.copy(alpha = 0.55f),
                            unfocusedContainerColor = p.cardDark.copy(alpha = 0.55f),
                            focusedIndicatorColor = p.accent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedLabelColor = p.accent,
                            unfocusedLabelColor = p.textMuted,
                        )

                        TextField(
                            value = email,
                            onValueChange = { email = it; errorMsg = null },
                            label = { Text("Email", fontFamily = InterFont) },
                            leadingIcon = { Icon(Icons.Rounded.Email, null, tint = p.textMuted) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Email,
                                imeAction = ImeAction.Next,
                            ),
                            colors = fc,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )

                        TextField(
                            value = password,
                            onValueChange = { password = it; errorMsg = null },
                            label = { Text("Пароль", fontFamily = InterFont) },
                            leadingIcon = { Icon(Icons.Rounded.Lock, null, tint = p.textMuted) },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Rounded.VisibilityOff
                                        else Icons.Rounded.Visibility,
                                        null, tint = p.textMuted,
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None
                            else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done,
                            ),
                            colors = fc,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                /* Error message */
                AnimatedVisibility(visible = errorMsg != null) {
                    Text(
                        errorMsg ?: "",
                        color = p.accentLike,
                        fontSize = 13.sp,
                        fontFamily = InterFont,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }

                Spacer(Modifier.height(24.dp))

                /* ── Login button ── */
                val canLogin = email.isNotBlank() && password.length >= 4 && !isLoading
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .peekPop(1.05f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            if (canLogin) Brush.linearGradient(listOf(p.accent, p.calmMid))
                            else Brush.linearGradient(listOf(p.textMuted.copy(0.3f), p.textMuted.copy(0.2f)))
                        )
                        .clickable(enabled = canLogin) {
                            isLoading = true
                            errorMsg = null
                            scope.launch {
                                try {
                                    val resp = RetrofitClient.api.login(
                                        LoginRequest(email.trim(), password)
                                    )
                                    if (resp.success && resp.token != null) {
                                        AuthManager.saveSession(
                                            token = resp.token,
                                            userId = resp.userId ?: "",
                                            email = email.trim(),
                                        )
                                        AuthManager.hasProfile = true
                                        onLoggedIn()
                                    } else {
                                        errorMsg = resp.message ?: "Неверный email или пароль"
                                    }
                                } catch (e: Exception) {
                                    errorMsg = "Сервер недоступен. Попробуйте позже."
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    } else {
                        Text("Войти", color = Color.White, fontWeight = FontWeight.Bold,
                            fontFamily = FindSansFont, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                /* ── Divider ── */
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f).height(0.5.dp).background(p.borderDark))
                    Text("  или  ", color = p.textMuted, fontSize = 12.sp, fontFamily = InterFont)
                    Box(Modifier.weight(1f).height(0.5.dp).background(p.borderDark))
                }

                Spacer(Modifier.height(16.dp))

                /* ── Telegram login button ── */
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .peekPop(1.05f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF2AABEE))
                        .clickable {
                            // TODO: Telegram OAuth flow
                            errorMsg = "Telegram-логин будет доступен после настройки бота"
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Send, null, tint = Color.White, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Войти через Telegram", color = Color.White,
                            fontWeight = FontWeight.SemiBold, fontFamily = InterFont, fontSize = 15.sp)
                    }
                }

                Spacer(Modifier.height(32.dp))

                /* ── Go to registration ── */
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Нет аккаунта? ", color = p.textMuted, fontSize = 14.sp, fontFamily = InterFont)
                    Text(
                        "Создать",
                        color = p.accent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = InterFont,
                        modifier = Modifier.clickable { onGoToRegister() },
                    )
                }

                Spacer(Modifier.height(40.dp))
            }
        }
    }
}

