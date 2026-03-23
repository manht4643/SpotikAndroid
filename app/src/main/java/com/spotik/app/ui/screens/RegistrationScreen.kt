package com.spotik.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.spotik.app.data.City
import com.spotik.app.ui.components.GlassCard
import com.spotik.app.ui.components.MeshGradientBackground
import com.spotik.app.ui.effects.peekPop
import com.spotik.app.ui.theme.*
import com.spotik.app.ui.viewmodel.RegistrationViewModel
import kotlin.math.abs

/* ══════════════════════════════════════════════════════════
 *  REGISTRATION SCREEN  —  4-step onboarding
 *  Step 0 = City   Step 1 = Name   Step 2 = Age   Step 3 = Photo
 * ══════════════════════════════════════════════════════════ */

@Composable
fun RegistrationScreen(
    vm: RegistrationViewModel = viewModel(),
    onFinished: () -> Unit = {},
) {
    val p = LocalSpotikPalette.current

    MeshGradientBackground {
        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            /* ── Step content with slide animation ── */
            AnimatedContent(
                targetState = vm.step,
                transitionSpec = {
                    val forward = targetState > initialState
                    val enter = slideInHorizontally { if (forward) it else -it } + fadeIn(tween(350))
                    val exit = slideOutHorizontally { if (forward) -it else it } + fadeOut(tween(250))
                    enter togetherWith exit
                },
                modifier = Modifier.fillMaxSize(),
                label = "regStep",
            ) { step ->
                when (step) {
                    0 -> StepCity(vm)
                    1 -> StepName(vm)
                    2 -> StepAge(vm)
                    3 -> StepPhoto(vm)
                }
            }

            /* ── Bottom bar: dots + next button ── */
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                /* Step dots */
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(4) { i ->
                        val active = i == vm.step
                        val w by animateDpAsState(if (active) 24.dp else 8.dp, spring(stiffness = 400f), label = "dw")
                        Box(
                            Modifier
                                .height(8.dp)
                                .width(w)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (active) p.accent else p.textMuted.copy(alpha = 0.4f)),
                        )
                    }
                }

                /* Next / Finish button */
                val enabled = vm.canProceed
                val label = if (vm.isLastStep) "Готово" else "Далее"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .peekPop(1.08f)
                        .clip(RoundedCornerShape(50))
                        .background(if (enabled) p.accent else p.textMuted.copy(0.25f))
                        .clickable(enabled = enabled) {
                            if (vm.isLastStep) onFinished() else vm.nextStep()
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                ) {
                    Text(label, color = Color.White, fontWeight = FontWeight.Bold,
                        fontFamily = FindSansFont, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        if (vm.isLastStep) Icons.Rounded.Check else Icons.AutoMirrored.Rounded.ArrowForward,
                        null, tint = Color.White, modifier = Modifier.size(18.dp),
                    )
                }
            }

            /* Back arrow (steps 1-3) */
            AnimatedVisibility(
                visible = vm.step > 0,
                enter = fadeIn() + slideInHorizontally { -it / 2 },
                exit = fadeOut() + slideOutHorizontally { -it / 2 },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 12.dp),
            ) {
                Box(
                    Modifier
                        .peekPop(1.12f)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(p.cardDark.copy(alpha = 0.6f))
                        .border(0.5.dp, p.borderLight.copy(0.3f), CircleShape)
                        .clickable { vm.prevStep() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.ArrowBackIosNew, null, tint = p.textPrimary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/* ═══════════════════════  STEP 0 — CITY  ═══════════════════════ */

@Composable
private fun StepCity(vm: RegistrationViewModel) {
    val p = LocalSpotikPalette.current

    /* Globe zoom animation */
    val globeScale by animateFloatAsState(
        targetValue = if (vm.cityConfirmed) 1.35f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.6f),
        label = "globeS",
    )
    val globeRotation by animateFloatAsState(
        targetValue = if (vm.cityConfirmed) 360f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = 0.75f),
        label = "globeR",
    )

    /* Bloom glow alpha */
    var bloomTrigger by remember { mutableIntStateOf(0) }
    val bloomAlpha = remember { Animatable(0f) }
    LaunchedEffect(bloomTrigger) {
        if (bloomTrigger > 0) {
            bloomAlpha.snapTo(0.7f)
            bloomAlpha.animateTo(0f, tween(900, easing = LinearOutSlowInEasing))
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Выбери свой город", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            fontFamily = FindSansFont, color = p.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Мы покажем споты рядом с тобой", fontSize = 14.sp,
            fontFamily = InterFont, color = p.textSecondary)

        Spacer(Modifier.height(28.dp))

        /* ── Globe placeholder ── */
        Box(
            Modifier
                .size(140.dp)
                .drawBehind {
                    // Bloom glow behind globe
                    if (bloomAlpha.value > 0.01f) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(
                                    p.accent.copy(alpha = bloomAlpha.value),
                                    Color.Transparent,
                                ),
                                radius = size.minDimension * 1.1f,
                            ),
                            radius = size.minDimension * 1.1f,
                        )
                    }
                }
                .graphicsLayer {
                    scaleX = globeScale
                    scaleY = globeScale
                    rotationZ = globeRotation
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(p.calmMid.copy(0.5f), p.techMid.copy(0.3f), p.deepBg),
                        )
                    )
                    .border(1.dp, p.accent.copy(0.3f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🌍", fontSize = 56.sp)
            }
        }

        Spacer(Modifier.height(28.dp))

        /* ── Search field ── */
        OutlinedTextField(
            value = vm.searchQuery,
            onValueChange = {
                vm.searchQuery = it
            },
            placeholder = { Text("Поиск города…", fontFamily = InterFont, color = p.textMuted) },
            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = p.accent) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = p.textPrimary,
                unfocusedTextColor = p.textPrimary,
                cursorColor = p.accent,
                focusedBorderColor = p.accent,
                unfocusedBorderColor = p.borderDark,
                focusedContainerColor = p.cardDark.copy(0.5f),
                unfocusedContainerColor = p.cardDark.copy(0.4f),
            ),
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(14.dp))

        /* ── City list ── */
        val cities = vm.filteredCities
        GlassCard(Modifier.fillMaxWidth().weight(1f), padding = 0.dp) {
            if (cities.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Ничего не найдено", color = p.textMuted, fontFamily = InterFont)
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 6.dp),
                ) {
                    items(cities.size) { i ->
                        val city = cities[i]
                        val selected = city == vm.selectedCity
                        CityRow(city, selected) {
                            vm.selectCity(city)
                            bloomTrigger++
                        }
                        if (i < cities.size - 1) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(0.5.dp)
                                    .background(p.borderDark),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityRow(city: City, selected: Boolean, onClick: () -> Unit) {
    val p = LocalSpotikPalette.current
    val bg by animateColorAsState(
        if (selected) p.accent.copy(0.15f) else Color.Transparent,
        tween(250), label = "crBg",
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Place, null,
            tint = if (selected) p.accent else p.textMuted,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(city.name, fontWeight = FontWeight.SemiBold, fontFamily = FindSansFont,
                fontSize = 15.sp, color = p.textPrimary)
            Text("${city.spots.size} спотов", fontSize = 12.sp,
                fontFamily = InterFont, color = p.textMuted)
        }
        if (selected) {
            Icon(Icons.Rounded.CheckCircle, null, tint = p.accent, modifier = Modifier.size(20.dp))
        }
    }
}

/* ═══════════════════════  STEP 1 — NAME  ═══════════════════════ */

@Composable
private fun StepName(vm: RegistrationViewModel) {
    val p = LocalSpotikPalette.current

    /* Floating label animation */
    val labelOffset by animateFloatAsState(
        targetValue = if (vm.name.isNotEmpty()) -12f else 0f,
        spring(stiffness = 400f), label = "lbl",
    )

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))

        /* Animated emoji */
        val wave = rememberInfiniteTransition(label = "wave")
        val waveRot by wave.animateFloat(
            initialValue = -8f, targetValue = 8f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "wr",
        )
        Text("👋", fontSize = 64.sp, modifier = Modifier.graphicsLayer { rotationZ = waveRot })

        Spacer(Modifier.height(24.dp))
        Text("Как тебя зовут?", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            fontFamily = FindSansFont, color = p.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Представься, чтобы другие тебя узнали", fontSize = 14.sp,
            fontFamily = InterFont, color = p.textSecondary)
        Spacer(Modifier.height(40.dp))

        OutlinedTextField(
            value = vm.name,
            onValueChange = { vm.name = it },
            placeholder = { Text("Имя", fontFamily = InterFont, color = p.textMuted) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = p.textPrimary,
                unfocusedTextColor = p.textPrimary,
                cursorColor = p.accent,
                focusedBorderColor = p.accent,
                unfocusedBorderColor = p.borderDark,
                focusedContainerColor = p.cardDark.copy(0.5f),
                unfocusedContainerColor = p.cardDark.copy(0.4f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationY = labelOffset },
        )
    }
}

/* ═══════════════════════  STEP 2 — AGE  ═══════════════════════ */

@Composable
private fun StepAge(vm: RegistrationViewModel) {
    val p = LocalSpotikPalette.current
    val ages = (14..99).toList()
    val initIdx = (vm.age - 14).coerceIn(0, ages.size - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIdx)

    val centerIdx by remember {
        derivedStateOf {
            val li = listState.layoutInfo
            val vpCenter = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - vpCenter) }?.index ?: initIdx
        }
    }
    LaunchedEffect(centerIdx) {
        val newAge = ages.getOrNull(centerIdx) ?: vm.age
        if (newAge != vm.age) vm.age = newAge
    }

    /* Scale-up entrance */
    val enterScale = remember { Animatable(0.85f) }
    LaunchedEffect(Unit) {
        enterScale.animateTo(1f, spring(dampingRatio = 0.65f, stiffness = 300f))
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = enterScale.value; scaleY = enterScale.value }
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("🎂", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("Сколько тебе лет?", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            fontFamily = FindSansFont, color = p.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Мы подберём контент по возрасту", fontSize = 14.sp,
            fontFamily = InterFont, color = p.textSecondary)

        Spacer(Modifier.height(32.dp))

        /* Wheel picker */
        Box(
            Modifier.fillMaxWidth().height(200.dp),
            contentAlignment = Alignment.Center,
        ) {
            /* Selection pill */
            Box(
                Modifier.fillMaxWidth(0.45f).height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(p.accent.copy(0.06f), p.accent.copy(0.14f), p.accent.copy(0.06f)),
                        )
                    )
                    .border(0.5.dp, p.accent.copy(0.18f), RoundedCornerShape(24.dp)),
            )

            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                contentPadding = PaddingValues(vertical = 76.dp),
                flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            ) {
                items(ages.size) { index ->
                    val a = ages[index]
                    val li = listState.layoutInfo
                    val itemInfo = li.visibleItemsInfo.find { it.index == index }
                    val dist = if (itemInfo != null) {
                        val vpC = (li.viewportStartOffset + li.viewportEndOffset) / 2f
                        val itC = itemInfo.offset + itemInfo.size / 2f
                        ((itC - vpC) / vpC.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                    } else 0f

                    Box(
                        Modifier.height(48.dp).fillMaxWidth()
                            .graphicsLayer {
                                alpha = 1f - abs(dist) * 0.7f
                                val s = 1f - abs(dist) * 0.25f
                                scaleX = s; scaleY = s
                                rotationX = dist * 45f
                                cameraDistance = 12f * density
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("$a", fontSize = 28.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FindSansFont, color = p.textPrimary)
                    }
                }
            }

            /* Top / bottom fades */
            Box(
                Modifier.fillMaxSize().drawWithContent {
                    drawContent()
                    val fadePx = size.height * 0.3f
                    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(0.5f), Color.Transparent), 0f, fadePx))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.5f)), size.height - fadePx, size.height))
                },
            )
        }
    }
}

/* ═══════════════════════  STEP 3 — PHOTO  ═══════════════════════ */

@Composable
private fun StepPhoto(vm: RegistrationViewModel) {
    val p = LocalSpotikPalette.current

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) vm.setAvatar(uri) }

    /* Bounce entrance */
    val enterScale = remember { Animatable(0.8f) }
    val enterAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.launch { enterAlpha.animateTo(1f, tween(400)) }
        enterScale.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 250f))
    }

    Column(
        Modifier
            .fillMaxSize()
            .graphicsLayer { scaleX = enterScale.value; scaleY = enterScale.value; alpha = enterAlpha.value }
            .padding(horizontal = 24.dp)
            .padding(top = 60.dp, bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        Text("📸", fontSize = 64.sp)
        Spacer(Modifier.height(24.dp))
        Text("Добавь фото", fontSize = 24.sp, fontWeight = FontWeight.Bold,
            fontFamily = FindSansFont, color = p.textPrimary)
        Spacer(Modifier.height(8.dp))
        Text("Так тебя будут узнавать", fontSize = 14.sp,
            fontFamily = InterFont, color = p.textSecondary, textAlign = TextAlign.Center)

        Spacer(Modifier.height(40.dp))

        /* Avatar circle */
        Box(
            Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(p.cardDark.copy(0.55f))
                .border(
                    1.dp,
                    Brush.linearGradient(listOf(p.borderLight.copy(0.4f), p.borderDark.copy(0.15f))),
                    CircleShape,
                )
                .clickable {
                    photoLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (vm.avatarUri != null) {
                AsyncImage(
                    model = vm.avatarUri,
                    contentDescription = "Аватар",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.AddAPhoto, null, tint = p.textMuted, modifier = Modifier.size(36.dp))
                    Spacer(Modifier.height(6.dp))
                    Text("Нажми", fontSize = 12.sp, color = p.textMuted, fontFamily = InterFont)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        /* "Change photo" button */
        AnimatedVisibility(visible = vm.avatarUri != null, enter = fadeIn() + expandVertically(), exit = fadeOut()) {
            Text(
                "Изменить фото",
                Modifier
                    .peekPop(1.08f)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.linearGradient(
                            listOf(p.cardDark.copy(0.6f), p.cardDark.copy(0.35f)),
                        )
                    )
                    .border(0.5.dp, p.borderLight.copy(0.4f), RoundedCornerShape(50))
                    .clickable {
                        photoLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.White, fontWeight = FontWeight.SemiBold,
                fontFamily = InterFont, fontSize = 14.sp,
            )
        }
    }
}

