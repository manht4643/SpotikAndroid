package com.spotik.app.ui.screens

import android.net.Uri
import android.os.Build
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Velocity
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.spotik.app.ui.effects.peekPop
import com.spotik.app.ui.theme.*
import com.spotik.app.ui.viewmodel.ProfileViewModel
import kotlin.math.abs
import kotlin.math.sin
import kotlinx.coroutines.launch

/* =======================================================================
 *  BENTO PROFILE - Telegram-style collapsing avatar into notch/island
 *  Pure spring physics, WindowInsets-driven geometry, Bento grid
 * ======================================================================= */

/* ── Haptic via Compose HapticFeedback — "expensive weight" feel ── */

@Composable
private fun rememberHaptic(): () -> Unit {
    val hf = LocalHapticFeedback.current
    return remember(hf) { { hf.performHapticFeedback(HapticFeedbackType.LongPress) } }
}

/* ── Avatar content (shared between profile & edit) ── */

@Composable
private fun AvatarContent(avatarUri: Uri?, name: String, fontSize: Float) {
    val p = LocalSpotikPalette.current
    if (avatarUri != null) {
        AsyncImage(
            model = avatarUri, contentDescription = "Avatar",
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
        )
    } else {
        Text(
            name.firstOrNull()?.uppercase() ?: "?",
            fontSize = fontSize.sp, fontWeight = FontWeight.Bold, color = p.textPrimary,
        )
    }
}

/* =======================================================================
 *  MAIN PROFILE SCREEN — collapsing avatar & Bento grid
 * ======================================================================= */

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit = {}, profileVm: ProfileViewModel = viewModel()) {
    val p = LocalSpotikPalette.current
    val density = LocalDensity.current
    val haptic = rememberHaptic()
    val scope = rememberCoroutineScope()

    /* ── Data ── */
    val name = profileVm.name
    val age = profileVm.age
    val bio = profileVm.bio
    val avatarUri = profileVm.avatarUri
    var showEdit by rememberSaveable { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? -> if (uri != null) profileVm.setEditAvatar(uri) }
    val pickPhoto = {
        photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    LaunchedEffect(showEdit) { if (showEdit) profileVm.beginEdit() }

    /* ── Exact camera cutout position (px) ── */
    val ctx = LocalContext.current
    val screenWidthPx = with(density) { ctx.resources.displayMetrics.widthPixels.toFloat() }
    val screenCenterXPx = screenWidthPx / 2f

    val statusBarTopPx = with(density) {
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding().toPx()
    }

    // Get exact cutout bounding rect from the Window
    val cutoutRect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        (ctx as? android.app.Activity)?.window?.decorView?.rootWindowInsets
            ?.displayCutout?.boundingRects?.firstOrNull()
    } else null

    // Exact centre of the camera hole
    val cameraCenterXPx = cutoutRect?.exactCenterX() ?: screenCenterXPx
    val cameraCenterYPx = cutoutRect?.exactCenterY() ?: (statusBarTopPx / 2f)

    val statusBarTopDp = with(density) { statusBarTopPx.toDp() }

    /* ── Scroll-driven collapse ── */
    val scrollState = rememberScrollState()
    val collapseThreshPx = with(density) { 220.dp.toPx() }

    /* ── Scroll fraction [0..1] — direct, no spring lag ── */
    val sf by remember {
        derivedStateOf { (scrollState.value / collapseThreshPx).coerceIn(0f, 1f) }
    }

    /* ── Avatar constants ── */
    val expandedSizeDp = 120.dp
    val collapsedScale = 0.25f
    val collapsedVisualDp = with(density) { (expandedSizeDp.toPx() * collapsedScale).toDp() }
    val expandedSizePx = with(density) { expandedSizeDp.toPx() }
    val toolbarHPx = with(density) { 56.dp.toPx() }
    val avatarTopPadPx = with(density) { 24.dp.toPx() }
    val originalCenterY = statusBarTopPx + toolbarHPx + avatarTopPadPx + expandedSizePx / 2f

    /* ── Interactive Avatar States (Telegram Style) ── */
    val screenHeightPx = with(density) { ctx.resources.displayMetrics.heightPixels.toFloat() }
    val stage1Thresh = remember(screenWidthPx, statusBarTopPx, toolbarHPx, avatarTopPadPx, expandedSizePx) {
        val h = screenWidthPx - (statusBarTopPx + toolbarHPx + avatarTopPadPx + expandedSizePx)
        if (h < 50f) 50f else h
    }
    val snapExpandedPx = stage1Thresh
    val snapFullscreenPx = stage1Thresh + with(density) { 300.dp.toPx() }
    val maxDragOffsetPx = snapFullscreenPx + with(density) { 150.dp.toPx() }

    val overscrollState = remember { Animatable(0f) }
    var triggerHapticFired by remember { mutableStateOf(false) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (overscrollState.value > 0f && available.y < 0f) {
                    val consumedY = available.y.coerceAtLeast(-overscrollState.value)
                    scope.launch { overscrollState.snapTo(overscrollState.value + consumedY) }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                // Ensure only Drag gestures cause pull-to-expand. Prevents bottom flings from triggering expansion.
                if (source == NestedScrollSource.Drag && available.y > 0f && scrollState.value == 0 && !showEdit) {
                    val rawProgress = (overscrollState.value / snapExpandedPx).coerceIn(0f, 1f)
                    val friction = lerp(0.85f, 0.4f, rawProgress)
                    
                    val newValue = (overscrollState.value + available.y * friction).coerceAtMost(maxDragOffsetPx)
                    scope.launch { overscrollState.snapTo(newValue) }

                    if (newValue >= snapExpandedPx && !triggerHapticFired) {
                        triggerHapticFired = true
                        haptic()
                    } else if (newValue < snapExpandedPx) {
                        triggerHapticFired = false
                    }
                    return Offset(0f, available.y) // consume completely
                }
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                if (overscrollState.value > 0f) {
                    val velocityY = available.y
                    val target = when {
                        overscrollState.value >= snapFullscreenPx * 0.8f || (overscrollState.value > snapExpandedPx && velocityY > 1000f) -> snapFullscreenPx
                        overscrollState.value >= snapExpandedPx * 0.4f || velocityY > 1000f -> snapExpandedPx
                        else -> 0f
                    }
                    
                    triggerHapticFired = false
                    scope.launch { 
                        overscrollState.animateTo(
                            targetValue = target, 
                            animationSpec = if (target == snapExpandedPx && overscrollState.value > snapExpandedPx) {
                                // No bounce when returning from Fullscreen (State 3) to Square (State 2)
                                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                            } else {
                                spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
                            }
                        ) 
                    }
                    return Velocity(0f, available.y) // stop list from flinging
                } else if (scrollState.value > 0 && scrollState.value < collapseThreshPx) {
                    // Snap the toolbar/avatar to be either fully collapsed or fully reset (State -1 or 1)
                    val targetScroll = if (available.y < -300f || scrollState.value > collapseThreshPx * 0.4f) {
                        collapseThreshPx.toInt()
                    } else 0
                    scope.launch { scrollState.animateScrollTo(targetScroll, spring(dampingRatio = 0.85f)) }
                    return Velocity(0f, available.y)
                }
                return Velocity.Zero
            }
        }
    }

    // Normalized progress for transformations
    val progress by remember { 
        derivedStateOf { 
            val p = (overscrollState.value / snapExpandedPx).coerceIn(0f, 1f) 
            // Barely increases initially, then shoots up to 1.0 (state 2)
            Math.pow(p.toDouble(), 3.0).toFloat()
        } 
    }
    val progress2 by remember { 
        derivedStateOf { 
            if (snapFullscreenPx > snapExpandedPx) {
                ((overscrollState.value - snapExpandedPx) / (snapFullscreenPx - snapExpandedPx)).coerceIn(0f, 1f)
            } else 0f
        } 
    }

    /* =================================================================
     *  All transforms via derivedStateOf → 120 FPS, zero recomposition.
     *  Ease-in curve: avatar accelerates toward camera at the end.
     * ================================================================= */

    // Ease-in fraction: sf² — slow start, fast finish
    val ef by remember {
        derivedStateOf { sf * sf }
    }

    // 1) Shape Morphing: Circle → Square
    val avatarCornerDp by remember {
        derivedStateOf { 
            val base = lerp(24f, expandedSizeDp.value / 2f, sf)
            if (overscrollState.value > 0f) {
                // Morph to 0dp (fully square) in first phase
                lerp(base, 0f, progress)
            } else base
        }
    }

    val expandedScaleTarget = screenWidthPx / expandedSizePx
    val fullscreenScaleTarget = expandedScaleTarget // Maintain ratio

    // 2) Scale
    val avatarScale by remember {
        derivedStateOf { 
            val base = lerp(1f, collapsedScale, sf)
            if (overscrollState.value > 0f) {
                val p1 = lerp(base, expandedScaleTarget, progress)
                lerp(p1, fullscreenScaleTarget, progress2)
            } else base
        }
    }
    
    // Background Scrim (only dims fully to black in Stage 3)
    val scrimAlpha by remember {
        derivedStateOf {
            progress2 // Only fades black moving into fullscreen
        }
    }

    // 3) Position — ease-in toward exact camera cutout centre
    val layoutCenterY by remember {
        derivedStateOf { originalCenterY - scrollState.value.toFloat() }
    }
    val desiredCenterY by remember {
        derivedStateOf { lerp(originalCenterY, cameraCenterYPx, ef) }
    }
    val translationYPx by remember {
        derivedStateOf { 
            val base = desiredCenterY - layoutCenterY
            if (overscrollState.value > 0f) {
                val cancelColumnDrag = -overscrollState.value
                val targetGlobalY1 = screenWidthPx / 2f // Top edge flush with y=0
                val targetGlobalY2 = screenHeightPx / 2f // Center of screen
                
                val currentTargetGlobalY = lerp(
                    lerp(originalCenterY, targetGlobalY1, progress),
                    targetGlobalY2,
                    progress2
                )
                
                currentTargetGlobalY - originalCenterY + cancelColumnDrag
            } else base
        }
    }
    // X: avatar starts at screen center, moves to camera X
    val translationXPx by remember {
        derivedStateOf { 
            val base = lerp(0f, cameraCenterXPx - screenCenterXPx, ef)
            if (overscrollState.value > 0f) {
                lerp(base, 0f, progress)
            } else base
        }
    }

    // 4) Blur: 0 → 12 dp (Android 12+ only) — late onset
    val blurRadiusDp by remember {
        derivedStateOf { lerp(0f, 12f, ((sf - 0.6f) / 0.4f).coerceIn(0f, 1f)) }
    }

    // 5) Shadow fades as avatar collapses or expands into flat view
    val avShadowDp by remember {
        derivedStateOf { 
            val base = lerp(12f, 0f, sf)
            if (overscrollState.value > 0f) { lerp(base, 0f, progress) } else base
        }
    }

    // 6) Font size for initials letter
    val avFontSz by remember {
        derivedStateOf { lerp(48f, 10f, sf) }
    }

    // 7) Text alphas: header fades early, toolbar name + X slide appears late
    val headAlpha by remember {
        derivedStateOf { (1f - sf * 2.5f).coerceIn(0f, 1f) }
    }
    val tbNameAlpha by remember {
        derivedStateOf { ((sf - 0.4f) / 0.6f).coerceIn(0f, 1f) }
    }
    // 8) Toolbar name slides from behind avatar (X offset shrinks to 0)
    val tbNameOffsetXDp by remember {
        derivedStateOf { lerp(-40f, 0f, ((sf - 0.4f) / 0.6f).coerceIn(0f, 1f)) }
    }

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        val sts = this
        Box(Modifier.fillMaxSize()) {
            with(sts) {

                /* ─────── VIEW MODE ─────── */
                AnimatedVisibility(
                    visible = !showEdit,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                ) {
                    val avs = this@AnimatedVisibility

                    Box(
                        Modifier
                            .fillMaxSize()
                            .drawBehind {
                                drawRect(p.deepBg)
                                if (scrimAlpha > 0f) {
                                    drawRect(Color.Black.copy(alpha = scrimAlpha))
                                }
                            }
                            .pointerInput(overscrollState.value) {
                                if (overscrollState.value > stage1Thresh) {
                                    detectTapGestures {
                                        scope.launch { overscrollState.animateTo(0f, spring(0.75f, Spring.StiffnessMedium)) }
                                    }
                                }
                            }
                    ) {

                        /* ─── Scrollable content ─── */
                        Column(
                            Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection)
                                .verticalScroll(scrollState)
                                .graphicsLayer { translationY = overscrollState.value }
                                .padding(top = statusBarTopDp + 56.dp),
                        ) {
                            /* Avatar & name header */
                            Column(
                                Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Spacer(Modifier.height(24.dp))

                                /* Avatar — fixed 120dp layout, all visual via graphicsLayer.
                                   zIndex(10) so avatar flies ABOVE sticky bar and status icons. */
                                Box(
                                    modifier = Modifier
                                        .zIndex(10f)
                                        .sharedBounds(rememberSharedContentState("avatar"), avs)
                                        .size(expandedSizeDp)
                                        .graphicsLayer {
                                            /* ── Uniform scale ── */
                                            scaleX = avatarScale
                                            scaleY = avatarScale

                                            /* ── Magnetic translation toward exact camera hole ── */
                                            this.translationY = translationYPx
                                            this.translationX = translationXPx

                                            /* ── Shape morphing: rounded rect → circle ── */
                                            clip = true
                                            shape = RoundedCornerShape(avatarCornerDp.dp)

                                            /* ── Shadow fades out ── */
                                            shadowElevation = avShadowDp
                                            ambientShadowColor = p.accent.copy(alpha = 0.15f)
                                            spotShadowColor = p.accent.copy(alpha = 0.1f)
                                        }
                                        .clip(RoundedCornerShape(avatarCornerDp.dp))
                                        .background(p.cardDark)
                                        /* ── Blur (Android 12+) — disappears under glass ── */
                                        .then(
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && blurRadiusDp > 0f) {
                                                Modifier.blur(
                                                    blurRadiusDp.dp,
                                                    edgeTreatment = BlurredEdgeTreatment(
                                                        RoundedCornerShape(avatarCornerDp.dp)
                                                    )
                                                )
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) { AvatarContent(avatarUri, name, avFontSz) }

                                Spacer(Modifier.height(16.dp))

                                /* Name + city (fades out on scroll) */
                                Column(
                                    Modifier.graphicsLayer { 
                                        alpha = if (overscrollState.value > 0f) {
                                            lerp(headAlpha, 0f, progress2)
                                        } else headAlpha 
                                    },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(name, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                            fontFamily = FindSansFont, color = p.textPrimary)
                                        if (age > 0) {
                                            Text(", ", fontSize = 26.sp, color = p.textPrimary)
                                            Text("$age", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                                                fontFamily = FindSansFont, color = p.textPrimary)
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("\uD83D\uDCCD", fontSize = 13.sp)
                                        Spacer(Modifier.width(4.dp))
                                        Text(profileVm.city.ifBlank { "Альметьевск" },
                                            fontSize = 14.sp, color = p.textSecondary, fontFamily = InterFont)
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            /* Bento grid */
                            val contentPushDown = lerp(0f, screenWidthPx - originalCenterY + avatarTopPadPx + toolbarHPx, progress)
                            Column(Modifier
                                .graphicsLayer { 
                                    alpha = if (overscrollState.value > 0f) {
                                        lerp(1f, 0f, progress2)
                                    } else 1f 
                                    translationY = contentPushDown
                                }
                                .padding(horizontal = 16.dp)
                            ) {
                                BentoStatsRow(48, 215, 3, haptic)
                                Spacer(Modifier.height(12.dp))
                                BentoActivityChart()
                                Spacer(Modifier.height(12.dp))
                                if (bio.isNotBlank()) {
                                    BentoAboutMe(bio)
                                    Spacer(Modifier.height(12.dp))
                                }
                                Spacer(Modifier.height(12.dp))
                                
                                // Text-only Logout Button
                                Box(
                                    Modifier.fillMaxWidth().clickable { onLogout(); haptic() }.padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Выйти из аккаунта", color = Color(0xFFFF453A), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                                }
                                
                                Spacer(Modifier.height(120.dp))
                            }
                        }

                        /* ─── Sticky top bar (fades in on collapse) ─── */
                        Box(
                            Modifier
                                .zIndex(5f)
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            p.deepBg.copy(alpha = tbNameAlpha),
                                            p.deepBg.copy(alpha = tbNameAlpha * 0.7f),
                                            Color.Transparent,
                                        )
                                    )
                                )
                                .statusBarsPadding()
                                .height(56.dp)
                                .padding(horizontal = 20.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                /* Collapsed name — slides out from behind avatar */
                                Row(
                                    Modifier.graphicsLayer {
                                        alpha = tbNameAlpha
                                        translationX = with(density) { tbNameOffsetXDp.dp.toPx() }
                                    },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Spacer(Modifier.width(collapsedVisualDp + 12.dp))
                                    Text(
                                        name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FindSansFont,
                                        color = p.textPrimary,
                                    )
                                }

                                /* Edit button */
                                Box {
                                    var showOptionsCollapsed by remember { mutableStateOf(false) }
                                    Box(
                                        Modifier
                                            .graphicsLayer { alpha = tbNameAlpha }
                                            .peekPop(1.06f)
                                            .clip(CircleShape)
                                            .background(p.cardDark)
                                            .clickable { haptic(); showOptionsCollapsed = true }
                                            .padding(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.MoreVert, null, tint = p.accent, modifier = Modifier.size(24.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showOptionsCollapsed,
                                        onDismissRequest = { showOptionsCollapsed = false },
                                        modifier = Modifier.background(p.cardDark)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Редактировать", color = p.textPrimary) },
                                            onClick = { showOptionsCollapsed = false; showEdit = true },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = p.accent) }
                                        )
                                    }
                                }
                            }
                        }

                        /* --- Edit button at top & Back Button --- */
                        Box(
                            Modifier
                                .zIndex(11f)
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            // Back Button (fade in state 3)
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    Modifier
                                        .graphicsLayer { 
                                            alpha = if (overscrollState.value > snapExpandedPx) {
                                                androidx.compose.ui.util.lerp(0f, 1f, ((overscrollState.value - snapExpandedPx) / (snapFullscreenPx - snapExpandedPx)).coerceIn(0f, 1f))
                                            } else 0f
                                        }
                                        .peekPop(1.1f)
                                        .clip(CircleShape)
                                        .background(Color.Black.copy(alpha = 0.3f))
                                        .clickable { 
                                            haptic()
                                            scope.launch { overscrollState.animateTo(snapExpandedPx, spring(0.75f, Spring.StiffnessMedium)) }
                                        }
                                        .padding(8.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Назад", tint = Color.White, modifier = Modifier.size(24.dp))
                                }
                            }
                            
                            // Edit Dropdown (fade in state 2+)
                            Box(
                                Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                var showOptions by remember { mutableStateOf(false) }
                                Box {
                                    Box(
                                        Modifier
                                            .graphicsLayer { alpha = headAlpha }
                                            .peekPop(1.08f)
                                            .clip(CircleShape)
                                            .background(p.cardDark)
                                            .clickable { haptic(); showOptions = true }
                                            .padding(8.dp),
                                    ) {
                                        Icon(Icons.Rounded.MoreVert, null, tint = p.accent, modifier = Modifier.size(24.dp))
                                    }
                                    DropdownMenu(
                                        expanded = showOptions,
                                        onDismissRequest = { showOptions = false },
                                        modifier = Modifier.background(p.cardDark)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("Редактировать", color = p.textPrimary) },
                                            onClick = { showOptions = false; showEdit = true },
                                            leadingIcon = { Icon(Icons.Rounded.Edit, null, tint = p.accent) }
                                        )
                                    }
                                }
                            }
                        }


                    }
                }

                /* ─────── EDIT MODE ─────── */
                AnimatedVisibility(
                    visible = showEdit,
                    enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
                    exit = fadeOut(spring(stiffness = Spring.StiffnessMedium)),
                ) {
                    val avs = this@AnimatedVisibility
                    BackHandler { showEdit = false }
                    EditScreen(profileVm, avs, sts, { showEdit = false },
                        { haptic(); profileVm.applyEdit(); showEdit = false }, pickPhoto)
                }

                // Handle back press to collapse expanded avatar
                val isExpandedOrFullscreen by remember {
                    derivedStateOf { overscrollState.value >= snapExpandedPx - 10f }
                }
                BackHandler(enabled = isExpandedOrFullscreen && !showEdit) {
                    scope.launch { overscrollState.animateTo(0f, spring(0.75f, Spring.StiffnessMedium)) }
                }
            }
        }
    }
}

/* =======================================================================
 *  BENTO TILE — matte tile with soft shadow, no blur
 * ======================================================================= */

@Composable
private fun BentoTile(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 28.dp,
    containerBrush: Brush? = null,
    containerColor: Color? = null,
    onClick: (() -> Unit)? = null,
    haptic: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = LocalSpotikPalette.current
    val shape = RoundedCornerShape(cornerRadius)
    val fallback = containerColor ?: p.cardDark
    Box(
        modifier = modifier
            .then(if (onClick != null) Modifier.peekPop(1.03f).clickable { haptic?.invoke(); onClick() } else Modifier)
            .shadow(
                elevation = 12.dp, 
                shape = shape, 
                ambientColor = Color.Black.copy(alpha = 0.5f), 
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(shape)
            .then(
                if (containerBrush != null) Modifier.background(containerBrush)
                else Modifier.background(fallback)
            ),
        content = content,
    )
}

/* ── Stats Row ── */

@Composable
private fun BentoStatsRow(likes: Int, views: Int, referrals: Int, haptic: () -> Unit) {
    val p = LocalSpotikPalette.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        BentoStatTile(
            "$likes", "Лайков", Icons.Rounded.FavoriteBorder,
            androidx.compose.ui.graphics.SolidColor(p.cardDark), p.accent,
            Modifier.weight(1f), haptic
        )
        BentoStatTile(
            "$views", "Просмотров", Icons.Rounded.Visibility,
            androidx.compose.ui.graphics.SolidColor(p.cardDark), p.accent,
            Modifier.weight(1f), haptic
        )
        BentoStatTile(
            "$referrals", "Рефералов", Icons.Rounded.Groups,
            androidx.compose.ui.graphics.SolidColor(p.cardDark), p.accent,
            Modifier.weight(1f), haptic
        )
    }
}

@Composable
private fun BentoStatTile(value: String, label: String, icon: ImageVector, brush: Brush, iconTint: Color, modifier: Modifier, haptic: () -> Unit) {
    BentoTile(modifier.aspectRatio(1f), cornerRadius = 24.dp, containerBrush = brush, onClick = {}, haptic = haptic) {
        Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Box(
                Modifier.size(32.dp).clip(CircleShape).background(iconTint.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(16.dp))
            }
            Column {
                Text(value, fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = FindSansFont, color = Color.White)
                Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = InterFont, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

/* ── Activity Chart (Bézier) ── */

@Composable
private fun BentoActivityChart() {
    val p = LocalSpotikPalette.current
    val data = remember { listOf(12f, 28f, 19f, 35f, 24f, 42f, 31f) }
    val maxVal = data.max()
    val labels = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    BentoTile(
        Modifier.fillMaxWidth().height(180.dp),
        cornerRadius = 28.dp,
        containerBrush = androidx.compose.ui.graphics.SolidColor(p.cardDark)
    ) {
        Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(p.accent))
                    Spacer(Modifier.width(8.dp))
                    Text("Активность", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FindSansFont, color = p.textPrimary)
                }
                Box(Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha=0.1f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                    Text("Неделя", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFont, color = Color.White)
                }
            }
            Spacer(Modifier.height(16.dp))
            Canvas(Modifier.fillMaxWidth().weight(1f)) {
                val w = size.width
                val h = size.height
                val chartH = h - 20f
                val stepX = w / (data.size - 1).coerceAtLeast(1).toFloat()
                val pts: List<Offset> = data.mapIndexed { i, v ->
                    Offset(stepX * i.toFloat(), chartH - (v / maxVal) * chartH * 0.85f)
                }
                val path = Path()
                if (pts.isNotEmpty()) {
                    path.moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) {
                        val prev = pts[i - 1]
                        val curr = pts[i]
                        path.cubicTo(
                            prev.x + (curr.x - prev.x) * 0.4f, prev.y,
                            prev.x + (curr.x - prev.x) * 0.6f, curr.y,
                            curr.x, curr.y,
                        )
                    }
                }
                val fill = Path()
                fill.addPath(path)
                if (pts.isNotEmpty()) {
                    fill.lineTo(pts.last().x, chartH)
                    fill.lineTo(pts.first().x, chartH)
                    fill.close()
                }
                drawPath(fill, Brush.verticalGradient(listOf(p.accent.copy(alpha = 0.4f), Color.Transparent)))
                drawPath(path, p.accent, style = Stroke(4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                // Add glowing shadow to path
                drawPath(path, p.accent.copy(alpha=0.5f), style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))

                for (pt in pts) {
                    drawCircle(p.cardDark, 6f, pt)
                    drawCircle(p.accent, 4f, pt)
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach {
                    Text(it, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = InterFont, color = p.textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/* ── About Me ── */

@Composable
private fun BentoAboutMe(bio: String) {
    BentoTile(
        Modifier.fillMaxWidth(), 
        cornerRadius = 28.dp,
        containerBrush = Brush.linearGradient(listOf(Color(0xFF56AB2F), Color(0xFFA8E063)))
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Info, null, tint = Color.White.copy(alpha=0.8f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("О себе", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FindSansFont, color = Color.White)
            }
            Spacer(Modifier.height(12.dp))
            Text(bio, fontSize = 14.sp, fontFamily = InterFont, color = Color.White.copy(alpha = 0.95f), lineHeight = 24.sp)
        }
    }
}


/* ── Logout tile ── */

@Composable
private fun BentoLogoutTile(onLogout: () -> Unit, haptic: () -> Unit) {
    val p = LocalSpotikPalette.current
    var showDialog by remember { mutableStateOf(false) }
    BentoTile(
        Modifier.fillMaxWidth(), 
        cornerRadius = 24.dp, 
        containerBrush = Brush.linearGradient(listOf(Color(0xFFcb2d3e), Color(0xFFef473a))), 
        onClick = { showDialog = true }, haptic = haptic
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ExitToApp, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Выйти", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, fontFamily = FindSansFont, color = Color.White)
                Text("Завершить текущий сеанс", fontSize = 13.sp, fontFamily = InterFont, color = Color.White.copy(alpha=0.8f))
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White.copy(alpha=0.6f), modifier = Modifier.size(24.dp))
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = p.cardDark,
            titleContentColor = p.textPrimary,
            textContentColor = p.textSecondary,
            title = { Text("Выйти из аккаунта?", fontFamily = FindSansFont, fontWeight = FontWeight.Bold) },
            text = { Text("Вы уверены, что хотите выйти?", fontFamily = InterFont) },
            confirmButton = {
                TextButton(onClick = { showDialog = false; onLogout() }) {
                    Text("Выйти", color = p.accentLike, fontWeight = FontWeight.Bold, fontFamily = InterFont)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Отмена", color = p.textMuted, fontFamily = InterFont)
                }
            },
        )
    }
}

/* =======================================================================
 *  EDIT SCREEN
 * ======================================================================= */

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun EditScreen(
    profileVm: ProfileViewModel,
    avs: AnimatedVisibilityScope,
    sts: SharedTransitionScope,
    onBack: () -> Unit,
    onSave: () -> Unit,
    pickPhoto: () -> Unit,
) {
    val p = LocalSpotikPalette.current
    val editScroll = rememberScrollState()
    var showAgePicker by remember { mutableStateOf(false) }
    val haptic = rememberHaptic()

    Box(Modifier.fillMaxSize().background(p.deepBg)) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            /* Top bar */
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BentoCircleButton(onBack, Icons.AutoMirrored.Rounded.ArrowBack)
                Spacer(Modifier.weight(1f))
                Text("Редактирование", fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    fontFamily = FindSansFont, color = p.textPrimary)
                Spacer(Modifier.weight(1f))
                BentoCircleButton(onSave, Icons.Rounded.Check, p.accent)
            }

            Column(
                Modifier.fillMaxSize().verticalScroll(editScroll).padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(12.dp))

                /* Avatar */
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    with(sts) {
                        Box(
                            Modifier
                                .sharedBounds(rememberSharedContentState("avatar"), avs)
                                .size(100.dp)
                                .shadow(8.dp, CircleShape, ambientColor = p.accent.copy(alpha = 0.15f))
                                .clip(CircleShape)
                                .background(p.cardDark)
                                .clickable { haptic(); pickPhoto() },
                            contentAlignment = Alignment.Center,
                        ) {
                            AvatarContent(profileVm.editAvatarUri, profileVm.editName, 38f)
                            Box(
                                Modifier.align(Alignment.BottomEnd).size(32.dp)
                                    .clip(CircleShape).background(p.accent),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(Icons.Rounded.CameraAlt, null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                val fc = TextFieldDefaults.colors(
                    focusedTextColor = p.textPrimary, unfocusedTextColor = p.textPrimary,
                    cursorColor = p.accent,
                    focusedContainerColor = p.cardDark, unfocusedContainerColor = p.cardDark,
                    focusedIndicatorColor = p.accent, unfocusedIndicatorColor = Color.Transparent,
                    focusedLabelColor = p.accent, unfocusedLabelColor = p.textMuted,
                )

                BentoTile(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(4.dp)) {
                        TextField(
                            profileVm.editName, { profileVm.editName = it },
                            label = { Text("Имя", fontFamily = InterFont) },
                            singleLine = true, colors = fc,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        TextField(
                            profileVm.editBio, { profileVm.editBio = it },
                            label = { Text("О себе", fontFamily = InterFont) },
                            minLines = 3, maxLines = 6, colors = fc,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                BentoTile(Modifier.fillMaxWidth().clickable { haptic(); showAgePicker = !showAgePicker }) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Возраст", color = p.textPrimary, fontFamily = InterFont, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.weight(1f))
                            Text("${profileVm.editAge}", color = p.textPrimary, fontWeight = FontWeight.Bold,
                                fontFamily = FindSansFont, fontSize = 20.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(
                                if (showAgePicker) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                                null, tint = p.accent, modifier = Modifier.size(22.dp),
                            )
                        }
                        AnimatedVisibility(
                            showAgePicker,
                            enter = expandVertically(spring(stiffness = Spring.StiffnessMedium)),
                            exit = shrinkVertically(spring(stiffness = Spring.StiffnessMedium)),
                        ) {
                            Column {
                                Spacer(Modifier.height(12.dp))
                                AgeWheelPicker(profileVm.editAge) { profileVm.editAge = it }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                BentoTile(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Оформление", fontSize = 16.sp, fontWeight = FontWeight.Bold,
                            fontFamily = FindSansFont, color = p.textPrimary)
                        Spacer(Modifier.height(14.dp))
                        ThemeCarousel()
                    }
                }

                Spacer(Modifier.height(120.dp))
            }
        }
    }
}

/* ── Circle button ── */

@Composable
private fun BentoCircleButton(
    onClick: () -> Unit,
    icon: ImageVector,
    tint: Color = LocalSpotikPalette.current.textPrimary,
) {
    val p = LocalSpotikPalette.current
    Box(
        Modifier
            .peekPop(1.12f)
            .size(42.dp)
            .shadow(6.dp, CircleShape, ambientColor = Color.Black.copy(alpha = 0.15f))
            .clip(CircleShape)
            .background(p.cardDark)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp)) }
}

/* =======================================================================
 *  AGE WHEEL PICKER
 * ======================================================================= */

@Composable
private fun AgeWheelPicker(selectedAge: Int, onAgeChanged: (Int) -> Unit) {
    val p = LocalSpotikPalette.current
    val ages = (14..99).toList()
    val initIdx = (selectedAge - 14).coerceIn(0, ages.size - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initIdx)
    val centerIdx by remember {
        derivedStateOf {
            val li = listState.layoutInfo
            val vpC = (li.viewportStartOffset + li.viewportEndOffset) / 2
            li.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - vpC) }?.index ?: initIdx
        }
    }
    LaunchedEffect(centerIdx) {
        val a = ages.getOrNull(centerIdx) ?: selectedAge
        if (a != selectedAge) onAgeChanged(a)
    }

    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth(0.5f).height(1.dp).background(p.accent.copy(alpha = 0.4f)))
            Spacer(Modifier.height(44.dp))
            Box(Modifier.fillMaxWidth(0.5f).height(1.dp).background(p.accent.copy(alpha = 0.4f)))
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 58.dp),
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        ) {
            items(ages.size) { index ->
                val a = ages[index]
                val li = listState.layoutInfo
                val ii = li.visibleItemsInfo.find { it.index == index }
                val d = if (ii != null) {
                    val vc = (li.viewportStartOffset + li.viewportEndOffset) / 2f
                    ((ii.offset + ii.size / 2f - vc) / vc.coerceAtLeast(1f)).coerceIn(-1f, 1f)
                } else 0f
                Box(
                    Modifier.height(44.dp).fillMaxWidth()
                        .graphicsLayer {
                            alpha = 1f - abs(d) * 0.7f
                            val s = 1f - abs(d) * 0.25f
                            scaleX = s; scaleY = s
                            rotationX = d * 45f
                            cameraDistance = 12f * density
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$a", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        fontFamily = FindSansFont, color = p.textPrimary, textAlign = TextAlign.Center)
                }
            }
        }
        Box(
            Modifier.fillMaxSize().drawWithContent {
                drawContent()
                val fp = size.height * 0.32f
                drawRect(Brush.verticalGradient(listOf(p.cardDark, Color.Transparent), 0f, fp))
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, p.cardDark), size.height - fp, size.height))
            },
        )
    }
}

/* =======================================================================
 *  THEME CAROUSEL
 * ======================================================================= */

@Composable
private fun ThemeCarousel() {
    val p = LocalSpotikPalette.current
    val cur = ThemeManager.currentTheme
    val haptic = rememberHaptic()
    LazyRow(
        contentPadding = PaddingValues(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        itemsIndexed(AppThemeType.entries) { _, theme ->
            val sel = theme == cur
            val pal = paletteFor(theme)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { haptic(); ThemeManager.setTheme(theme) }
                    .padding(6.dp),
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(Brush.verticalGradient(listOf(pal.deepBg, pal.techMid, pal.calmMid, pal.accent)))
                        .then(if (sel) Modifier.border(2.dp, Brush.linearGradient(listOf(pal.accent, pal.navGlow)), CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    if (sel) Box(
                        Modifier.size(20.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    theme.displayName, fontSize = 10.sp, fontFamily = InterFont,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) p.accent else p.textMuted,
                    textAlign = TextAlign.Center, maxLines = 1,
                )
            }
        }
    }
}
