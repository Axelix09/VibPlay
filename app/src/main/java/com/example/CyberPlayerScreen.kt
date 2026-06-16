package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.VideoView
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import kotlinx.coroutines.*
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ThemeColors(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val accent: Color
)

fun copyUriToLocalStorage(context: android.content.Context, uri: android.net.Uri, prefix: String): String {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri)
        if (inputStream != null) {
            val destFile = java.io.File(context.filesDir, "${prefix}_" + System.currentTimeMillis() + ".jpg")
            destFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            destFile.absolutePath
        } else {
            uri.toString()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        uri.toString()
    }
}

fun getCoilModel(artUri: String?): Any? {
    if (artUri.isNullOrEmpty()) return null
    return if (artUri.startsWith("/") || artUri.startsWith("file://")) {
        java.io.File(artUri.removePrefix("file://"))
    } else {
        artUri
    }
}

@Composable
fun getThemeColors(theme: ThemeStyle): ThemeColors {
    return when (theme) {
        ThemeStyle.CYBERPUNK -> ThemeColors(CyberPrimary, CyberSecondary, CyberBackground, CyberSurface, CyberAccent)
        ThemeStyle.LUXURY -> ThemeColors(LuxuryPrimary, LuxurySecondary, LuxuryBackground, LuxurySurface, LuxuryAccent)
        ThemeStyle.LAVA -> ThemeColors(LavaPrimary, LavaSecondary, LavaBackground, LavaSurface, LavaAccent)
        ThemeStyle.TOXIC -> ThemeColors(ToxicPrimary, ToxicSecondary, ToxicBackground, ToxicSurface, ToxicAccent)
    }
}

fun Modifier.drawThemeBackground(theme: ThemeStyle): Modifier = this.drawBehind {
    val w = size.width
    val h = size.height
    when (theme) {
        ThemeStyle.CYBERPUNK -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF14032C), Color(0xFF070014))))
            val gridColor = CyberPrimary.copy(alpha = 0.08f)
            val vY = h * 0.45f
            var curY = vY
            var step = 8f
            while (curY < h) {
                drawLine(gridColor, Offset(0f, curY), Offset(w, curY), strokeWidth = 2f)
                step *= 1.25f
                curY += step
            }
            val numL = 10
            val cX = w / 2f
            for (i in -numL..numL) {
                val endX = cX + i * (w / (numL * 1.4f))
                drawLine(gridColor, Offset(cX, vY), Offset(endX, h), strokeWidth = 1.5f)
            }
        }
        ThemeStyle.LUXURY -> {
            drawRect(Brush.radialGradient(listOf(Color(0xFF162035), Color(0xFF04070E)), Offset(w/2, h/2), h * 0.8f))
            val gold = LuxuryPrimary.copy(alpha = 0.03f)
            val dSize = 130f
            for (x in 0..(w / dSize).toInt() + 1) {
                for (y in 0..(h / dSize).toInt() + 2) {
                    val px = x * dSize + (if (y % 2 == 0) dSize / 2f else 0f)
                    val py = y * dSize
                    drawCircle(gold, radius = 25f, center = Offset(px, py))
                }
            }
            drawCircle(LuxuryPrimary.copy(alpha = 0.02f), radius = w * 0.45f, center = Offset(w/2, h/4), style = Stroke(2f))
            drawCircle(LuxuryPrimary.copy(alpha = 0.015f), radius = w * 0.7f, center = Offset(w/2, h/4), style = Stroke(3f))
        }
        ThemeStyle.LAVA -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF1A0202), Color(0xFF050000))))
            val magma = Path().apply {
                moveTo(0f, h)
                lineTo(0f, h * 0.82f)
                cubicTo(w * 0.3f, h * 0.75f, w * 0.65f, h * 0.93f, w, h * 0.80f)
                lineTo(w, h)
                close()
            }
            drawPath(magma, Brush.verticalGradient(listOf(LavaPrimary.copy(alpha = 0.15f), Color.Transparent)))
            val embers = listOf(Offset(w*0.1f, h*0.72f), Offset(w*0.4f, h*0.52f), Offset(w*0.8f, h*0.68f), Offset(w*0.9f, h*0.42f), Offset(w*0.6f, h*0.81f))
            embers.forEach { emb ->
                drawCircle(LavaSecondary.copy(alpha = 0.4f), radius = 10f, center = emb)
                drawCircle(LavaPrimary, radius = 4f, center = emb)
            }
        }
        ThemeStyle.TOXIC -> {
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050B04), Color(0xFF010301))))
            val hexC = ToxicPrimary.copy(alpha = 0.03f)
            val side = 40f
            val hH = side * 2f
            val hW = Math.sqrt(3.0).toFloat() * side
            for (row in 0..(h / (hH * 0.75f)).toInt() + 1) {
                for (col in 0..(w / hW).toInt() + 1) {
                    val cx = col * hW + (if (row % 2 == 1) hW / 2f else 0f)
                    val cy = row * hH * 0.75f
                    val hex = Path().apply {
                        for (i in 0..5) {
                            val rad = Math.toRadians((i * 60 - 30).toDouble()).toFloat()
                            val px = cx + side * kotlin.math.cos(rad)
                            val py = cy + side * kotlin.math.sin(rad)
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                    drawPath(hex, hexC, style = Stroke(1.5f))
                }
            }
            val stH = 18f
            val stripeC = ToxicPrimary.copy(alpha = 0.12f)
            var xVal = 0f
            while (xVal < w + stH) {
                drawLine(stripeC, Offset(xVal, h), Offset(xVal - stH, h - stH), strokeWidth = 5f)
                xVal += 24f
            }
        }
    }
}

fun formatTime(ms: Int): String {
    val totSec = ms / 1000
    val m = totSec / 60
    val s = totSec % 60
    return String.format("%02d:%02d", m, s)
}

fun shareTrack(context: Context, track: TrackEntity) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Compartiendo vía VibPlay")
        putExtra(Intent.EXTRA_TEXT, "¡Estoy escuchando de la hostia '${track.displayTitle}' por '${track.displayArtist}' en VibPlay! 🎧🔥")
    }
    context.startActivity(Intent.createChooser(intent, "Compartir canción"))
}

fun getVideoThumbnail(filePath: String): Bitmap? {
    if (filePath.startsWith("/simulated/")) return null
    return try {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val bmp = retriever.getFrameAtTime(1000000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        retriever.release()
        bmp
    } catch (e: Exception) {
        null
    }
}

@Composable
fun CyberPlayerScreen(viewModel: PlayerViewModel = viewModel()) {
    val appTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val colors = getThemeColors(appTheme)
    var isAppLoading by remember { mutableStateOf(true) }
    var loadingProgress by remember { mutableStateOf(0f) }

    var activeTab by remember { mutableStateOf("home") }
    var activePlaylistDetail by remember { mutableStateOf<PlaylistEntity?>(null) }
    val activeVideoTrack = remember { mutableStateOf<TrackEntity?>(null) }

    val homeListState = rememberLazyListState()
    val videosGridState = androidx.compose.foundation.lazy.grid.rememberLazyGridState()

    val context = LocalContext.current
    var lastBackPressTime by remember { mutableLongStateOf(0L) }

    BackHandler {
        if (activePlaylistDetail != null) {
            activePlaylistDetail = null
        } else if (activeTab != "home") {
            activeTab = "home"
        } else {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastBackPressTime < 2000L) {
                (context as? android.app.Activity)?.finish()
            } else {
                lastBackPressTime = currentTime
                Toast.makeText(context, "Presiona atrás de nuevo para salir", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(isAppLoading) {
        if (isAppLoading) {
            for (i in 1..100) {
                loadingProgress = i / 100f
                delay(14)
            }
            isAppLoading = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            background = colors.background,
            surface = colors.surface,
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = Color.LightGray,
            onPrimary = Color.Black,
            onSecondary = Color.Black
        )
    ) {
        ThemeBackground3D(appTheme) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Transparent,
                contentColor = Color.White
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing)
                ) {
            if (isAppLoading) {
                SplashScreen(progress = loadingProgress, themeColors = colors)
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val isTablet = maxWidth > 800.dp
                    if (isTablet) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (activePlaylistDetail == null) {
                                NavigationRailComponent(
                                    activeTab = activeTab,
                                    onTabSelect = { activeTab = it },
                                    colors = colors
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                MainContentSwitcher(
                                    activeTab = activeTab,
                                    viewModel = viewModel,
                                    colors = colors,
                                    activePlaylistDetail = activePlaylistDetail,
                                    onActivePlaylistDetailChange = { activePlaylistDetail = it },
                                    onVideoSelect = { activeVideoTrack.value = it },
                                    homeListState = homeListState,
                                    videosGridState = videosGridState
                                )
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.weight(1f)) {
                                MainContentSwitcher(
                                    activeTab = activeTab,
                                    viewModel = viewModel,
                                    colors = colors,
                                    activePlaylistDetail = activePlaylistDetail,
                                    onActivePlaylistDetailChange = { activePlaylistDetail = it },
                                    onVideoSelect = { activeVideoTrack.value = it },
                                    homeListState = homeListState,
                                    videosGridState = videosGridState
                                )
                            }
                            if (activePlaylistDetail == null) {
                                NavigationBarComponent(
                                    activeTab = activeTab,
                                    onTabSelect = { activeTab = it },
                                    colors = colors
                                )
                            }
                        }
                    }

                    // Mini Player
                    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()
                    if (currentTrack != null && activeTab != "player" && activeVideoTrack.value == null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 16.dp, vertical = if (isTablet) 16.dp else (if (activePlaylistDetail == null) 90.dp else 16.dp))
                        ) {
                            MiniPlayerCard(
                                currentTrack = currentTrack!!,
                                viewModel = viewModel,
                                colors = colors,
                                onOpenFullPlayer = { activeTab = "player" }
                            )
                        }
                    }

                    // Video overlay
                    activeVideoTrack.value?.let { videoTrack ->
                        VideoPlayerFrameOverlay(
                            track = videoTrack,
                            onClose = { activeVideoTrack.value = null },
                            colors = colors,
                            viewModel = viewModel,
                            onTrackChange = { activeVideoTrack.value = it }
                        )
                    }
                }
            }
        }
    }
}
}
}

@Composable
fun NavigationRailComponent(activeTab: String, onTabSelect: (String) -> Unit, colors: ThemeColors) {
    NavigationRail(
        containerColor = colors.surface.copy(alpha = 0.9f),
        contentColor = Color.White,
        modifier = Modifier.width(90.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Image(
            painter = painterResource(R.drawable.img_vibplay_logo_1781222694320),
            contentDescription = "Logo",
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        
        val items = listOf(
            Triple("home", Icons.Rounded.Home, "Home"),
            Triple("library", Icons.Rounded.LibraryMusic, "Biblioteca"),
            Triple("player", Icons.Rounded.PlayCircle, "Consola"),
            Triple("insights", Icons.Rounded.BarChart, "Métricas"),
            Triple("settings", Icons.Rounded.Settings, "Ajustes")
        )
        items.forEach { (tab, icon, label) ->
            NavigationRailItem(
                selected = activeTab == tab,
                onClick = { onTabSelect(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.15f),
                    unselectedIconColor = Color.LightGray,
                    unselectedTextColor = Color.LightGray
                )
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun NavigationBarComponent(activeTab: String, onTabSelect: (String) -> Unit, colors: ThemeColors) {
    NavigationBar(
        containerColor = colors.surface.copy(alpha = 0.95f),
        modifier = Modifier.navigationBarsPadding()
    ) {
        val items = listOf(
            Triple("home", Icons.Rounded.Home, "Inicio"),
            Triple("library", Icons.Rounded.LibraryMusic, "Biblioteca"),
            Triple("player", Icons.Rounded.PlayCircle, "Reproductor"),
            Triple("insights", Icons.Rounded.BarChart, "Métricas"),
            Triple("settings", Icons.Rounded.Settings, "Ajustes")
        )
        items.forEach { (tab, icon, label) ->
            NavigationBarItem(
                selected = activeTab == tab,
                onClick = { onTabSelect(tab) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.primary,
                    selectedTextColor = colors.primary,
                    indicatorColor = colors.primary.copy(alpha = 0.15f),
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun MainContentSwitcher(
    activeTab: String,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    activePlaylistDetail: PlaylistEntity?,
    onActivePlaylistDetailChange: (PlaylistEntity?) -> Unit,
    onVideoSelect: (TrackEntity) -> Unit,
    homeListState: androidx.compose.foundation.lazy.LazyListState,
    videosGridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    AnimatedContent(
        targetState = activeTab,
        transitionSpec = {
            fadeIn(tween(220)) + slideInVertically { it / 25 } togetherWith fadeOut(tween(180))
        },
        label = "tab_switch"
    ) { target ->
        when (target) {
            "home" -> HomeScreen(viewModel, colors, homeListState)
            "library" -> LibraryScreen(viewModel, colors, activePlaylistDetail, onActivePlaylistDetailChange, onVideoSelect, videosGridState)
            "player" -> PlayerScreen(viewModel, colors, onVideoSelect)
            "insights" -> InsightsScreen(viewModel, colors)
            "settings" -> SettingsScreen(viewModel, colors)
        }
    }
}

@Composable
fun HeaderTitleSection(theme: ThemeStyle, colors: ThemeColors) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.img_vibplay_logo_1781222694320),
            contentDescription = "Logo Circular",
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .border(1.dp, colors.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "VibPlay",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                val pulse by rememberInfiniteTransition(label = "p_anim").animateFloat(
                    initialValue = 0.6f, targetValue = 1.3f,
                    animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "p_sc"
                )
                Box(
                    modifier = Modifier
                        .scale(pulse)
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.accent)
                )
            }
            Text(
                text = "Estilo: ${theme.displayName}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun SplashScreen(progress: Float, themeColors: ThemeColors) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(themeColors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            var startS by remember { mutableStateOf(false) }
            LaunchedEffect(Unit) { startS = true }
            val logoScale by animateFloatAsState(
                targetValue = if (startS) 1.15f else 0.4f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "scale"
            )
            val rot by rememberInfiniteTransition(label = "ret").animateFloat(
                initialValue = 0f, targetValue = 360f,
                animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)), label = "rot_r"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                    }
                    .size(150.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = themeColors.primary,
                        startAngle = rot,
                        sweepAngle = 280f,
                        useCenter = false,
                        style = Stroke(width = 8f, cap = StrokeCap.Round)
                    )
                }
                Image(
                    painter = painterResource(R.drawable.img_vibplay_logo_1781222694320),
                    contentDescription = "Splash Logo",
                    modifier = Modifier.size(90.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "VibPlay Engine",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp,
                color = Color.White
            )
            Text(
                text = "SINTONIZANDO SEÑAL",
                color = themeColors.primary,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(32.dp))
            
            val pPercent = (progress * 100).toInt()
            LinearProgressIndicator(
                progress = { progress },
                color = themeColors.primary,
                trackColor = themeColors.surface,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(4.dp))
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$pPercent%",
                color = Color.White.copy(alpha = 0.5f),
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

// Home Tab
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val appTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }

    val pullState = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize()) {
        HeaderTitleSection(appTheme, colors)

        // Glassmorphism Persistent Search Bar on home
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar canciones, artistas, géneros...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Búsqueda", tint = Color.White) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Limpiar", tint = Color.White)
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                cursorColor = colors.primary,
                focusedLabelColor = colors.primary,
                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
            )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TODAS LAS CANCIONES",
                style = MaterialTheme.typography.labelSmall,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { viewModel.scanDeviceFiles() },
                enabled = !isScanning,
                modifier = Modifier.size(32.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.primary, strokeWidth = 1.5.dp)
                } else {
                    Icon(Icons.Rounded.Sync, contentDescription = "Escanear", tint = colors.primary, modifier = Modifier.size(20.dp))
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            var showAddTrackDialog by remember { mutableStateOf(false) }

            PullToRefreshBox(
                isRefreshing = isScanning,
                onRefresh = { viewModel.scanDeviceFiles() },
                state = pullState,
                modifier = Modifier.fillMaxSize()
            ) {
                val audioTracks = allTracks.filter { !it.isVideo }
                val filteredAudioTracks = remember(audioTracks, searchQuery) {
                    audioTracks.filter {
                        it.displayTitle.contains(searchQuery, ignoreCase = true) ||
                        it.displayArtist.contains(searchQuery, ignoreCase = true) ||
                        it.folder.contains(searchQuery, ignoreCase = true)
                    }
                }

                if (filteredAudioTracks.isEmpty() && !isScanning) {
                    EmptyStatePlaceholder(
                        onLoadDemo = { viewModel.scanDeviceFiles() },
                        colors = colors
                    )
                } else {
                    TracksListPanel(
                        tracks = filteredAudioTracks,
                        viewModel = viewModel,
                        colors = colors,
                        listState = listState,
                        showSearchBar = false
                    )
                }
            }



            if (showAddTrackDialog) {
                AddManualTrackDialog(
                    colors = colors,
                    onDismiss = { showAddTrackDialog = false },
                    onAdd = { title, artist, path, isVideo, folder, artUri, isEnglish ->
                        viewModel.addManualTrack(title, artist, path, isVideo, folder, artUri, isEnglish)
                        showAddTrackDialog = false
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = isScanning,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background.copy(alpha = 0.94f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    LoadingSongsPulseAnimation(themeColors = colors)
                }
            }
        }
    }
}

@Composable
fun EmptyStatePlaceholder(onLoadDemo: () -> Unit, colors: ThemeColors) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Rounded.MusicOff,
            contentDescription = "Sin pistas",
            tint = colors.primary.copy(alpha = 0.4f),
            modifier = Modifier.size(68.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sin Pistas de Audio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Pull down o clickea el botón para escanear archivos compatibles locales.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        Button(
            onClick = onLoadDemo,
            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
        ) {
            Text("Sincronizar Dispositivo", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun LoadingSongsPulseAnimation(themeColors: ThemeColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val anim = rememberInfiniteTransition(label = "pulse")
        val textAlpha by anim.animateFloat(
            initialValue = 0.3f, targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha"
        )
        
        CircularProgressIndicator(
            color = themeColors.primary,
            modifier = Modifier.size(48.dp),
            strokeWidth = 3.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "SINCRONIZANDO BIBLIOTECA VIBPLAY...",
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = themeColors.primary.copy(alpha = textAlpha),
            fontSize = 12.sp,
            letterSpacing = 1.sp
        )
    }
}

// General Track List Panel supporting batch operation
@Composable
fun TracksListPanel(
    tracks: List<TrackEntity>,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    listState: androidx.compose.foundation.lazy.LazyListState = rememberLazyListState(),
    playlistId: Long? = null,
    showSearchBar: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    val filteredTracks = tracks.filter {
        it.displayTitle.contains(searchQuery, ignoreCase = true) ||
        it.displayArtist.contains(searchQuery, ignoreCase = true) ||
        it.folder.contains(searchQuery, ignoreCase = true)
    }

    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface.copy(alpha = 0.85f))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectMode = false
                    selectedIds.clear()
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = Color.White)
                }
                Text(
                    text = "${selectedIds.size} seleccionadas",
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.titleSmall
                )
                IconButton(onClick = {
                    val allFIds = filteredTracks.map { it.id }
                    if (selectedIds.size == allFIds.size) {
                        selectedIds.clear()
                    } else {
                        selectedIds.clear()
                        selectedIds.addAll(allFIds)
                    }
                }) {
                    Icon(Icons.Rounded.SelectAll, contentDescription = "Seleccionar todo", tint = Color.White)
                }
                IconButton(
                    onClick = { showMoveFolderDialog = true },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Mover carpeta", tint = if (selectedIds.isNotEmpty()) Color.White else Color.DarkGray)
                }
                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.Delete, tint = if (selectedIds.isNotEmpty()) colors.accent else Color.DarkGray, contentDescription = "Borrar")
                }
            }
        } else if (showSearchBar) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar pista, artista o carpeta...", color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Búsqueda", tint = Color.White) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = "Limpia", tint = Color.White)
                        }
                    } else {
                        IconButton(onClick = { selectMode = true }) {
                            Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = "Elegir", tint = colors.primary)
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                    cursorColor = colors.primary,
                    focusedLabelColor = colors.primary,
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                )
            )
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            itemsIndexed(filteredTracks) { index, item ->
                var isItemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) {
                    delay((index.coerceAtMost(8) * 35).toLong())
                    isItemVisible = true
                }

                AnimatedVisibility(
                    visible = isItemVisible,
                    enter = fadeIn(tween(250)) + slideInHorizontally(tween(250)) { -20 },
                    exit = fadeOut(tween(150))
                ) {
                    val isChosen = selectedIds.contains(item.id)
                    TrackListItemCard(
                        track = item,
                        viewModel = viewModel,
                        colors = colors,
                        isSelectMode = selectMode,
                        isSelected = isChosen,
                        playlistId = playlistId,
                        onSelectToggle = {
                            if (isChosen) selectedIds.remove(item.id) else selectedIds.add(item.id)
                        },
                        onPlay = {
                            viewModel.onTrackSelected(item, filteredTracks)
                        },
                        onEnterSelectMode = {
                            selectMode = true
                        },
                        onSelectAll = {
                            selectedIds.clear()
                            selectedIds.addAll(filteredTracks.map { it.id })
                            selectMode = true
                        }
                    )
                }
            }
        }
    }

    if (showMoveFolderDialog) {
        var newFolder by remember { mutableStateOf("") }
        val folders by viewModel.availableFolders.collectAsStateWithLifecycle()
        
        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            containerColor = colors.surface,
            title = { Text("Mover Carpeta", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Selecciona una carpeta o escribe una nueva:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(folders) { f ->
                            SuggestionChip(
                                onClick = { newFolder = f },
                                label = { Text(f) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolder,
                        onValueChange = { newFolder = it },
                        label = { Text("Nombre de carpeta") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolder.isNotBlank()) {
                            viewModel.moveTracksToFolder(selectedIds.toList(), newFolder, isPermanent = true)
                            selectedIds.clear()
                            selectMode = false
                            showMoveFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
                ) {
                    Text("Mover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = colors.surface,
            title = { Text("¿Eliminar pista(s)?", fontWeight = FontWeight.Bold) },
            text = { Text("Esto borrará las canciones seleccionadas del reproductor de forma permanente.") },
            confirmButton = {
                Button(
                    onClick = {
                        val tracksToRemove = filteredTracks.filter { selectedIds.contains(it.id) }
                        viewModel.deleteTracks(tracksToRemove)
                        selectedIds.clear()
                        selectMode = false
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun TrackListItemCard(
    track: TrackEntity,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    isSelectMode: Boolean,
    isSelected: Boolean,
    playlistId: Long? = null,
    onSelectToggle: () -> Unit,
    onPlay: () -> Unit,
    onEnterSelectMode: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val rawScale by animateFloatAsState(if (isPressed) 0.96f else 1f, label = "press")

    var showContextSheet by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val favTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val isFavorite = favTracks.any { it.id == track.id }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("track_item_card")
            .padding(horizontal = 16.dp, vertical = 5.dp)
            .scale(rawScale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { if (isSelectMode) onSelectToggle() else onPlay() },
                    onLongPress = { showContextSheet = true }
                )
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) colors.primary.copy(alpha = 0.22f) else colors.surface.copy(alpha = 0.35f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else Color.White.copy(alpha = 0.12f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isSelectMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelectToggle() },
                    colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }

            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surface)
            ) {
                if (!track.customArtUri.isNullOrEmpty()) {
                    AsyncImage(
                        model = getCoilModel(track.customArtUri),
                        contentDescription = "Default cover",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (track.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                            contentDescription = if (track.isVideo) "Video Icon" else "Music Icon",
                            tint = colors.primary.copy(alpha = 0.82f),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.displayTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = track.displayArtist,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.LightGray.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(colors.primary.copy(alpha = 0.08f))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = track.folder.uppercase(),
                            color = colors.primary,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFF3B5C) else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = { showContextSheet = true }) {
                Icon(Icons.Rounded.MoreVert, contentDescription = "Más", tint = Color.Gray)
            }
        }
    }

    if (showContextSheet) {
        ContextMenuBottomSheet(
            track = track,
            isFavorite = isFavorite,
            onDismiss = { showContextSheet = false },
            colors = colors,
            playlistId = playlistId,
            onToggleFavorite = { viewModel.toggleFavorite(track) },
            onToggleEnglish = { viewModel.toggleEnglishTrack(track) },
            onEdit = {
                showEditDialog = true
                showContextSheet = false
            },
            onAddToPlaylist = {
                showPlaylistDialog = true
                showContextSheet = false
            },
            onDelete = {
                viewModel.deleteTrack(track)
                showContextSheet = false
            },
            onRemoveFromPlaylist = {
                if (playlistId != null) {
                    viewModel.removeTrackFromPlaylist(playlistId, track.id)
                }
            },
            onEnterSelectMode = onEnterSelectMode,
            onSelectAll = onSelectAll
        )
    }

    if (showEditDialog) {
        MetadataEditDialog(
            track = track,
            colors = colors,
            onDismiss = { showEditDialog = false },
            onSave = { t, a, ur, f, perm ->
                viewModel.editTrackMetadata(track.id, t, a, ur, f, perm)
                showEditDialog = false
            }
        )
    }

    if (showPlaylistDialog) {
        AddToPlaylistDialog(
            track = track,
            viewModel = viewModel,
            colors = colors,
            onDismiss = { showPlaylistDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContextMenuBottomSheet(
    track: TrackEntity,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    colors: ThemeColors,
    playlistId: Long? = null,
    onToggleFavorite: () -> Unit,
    onToggleEnglish: () -> Unit,
    onEdit: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onDelete: () -> Unit,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onEnterSelectMode: (() -> Unit)? = null,
    onSelectAll: (() -> Unit)? = null
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(40.dp, 4.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = 0.5f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = "Pista", tint = colors.primary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(track.displayTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(track.displayArtist, style = MaterialTheme.typography.bodySmall, color = Color.LightGray)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            DropdownMenuItem(
                text = { Text(if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos", color = Color.White) },
                onClick = {
                    onToggleFavorite()
                    onDismiss()
                },
                leadingIcon = { Icon(if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, contentDescription = null, tint = Color(0xFFFF3B5C)) }
            )
            DropdownMenuItem(
                text = { Text("Editar información", color = Color.White) },
                onClick = onEdit,
                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null, tint = Color.White) }
            )
            DropdownMenuItem(
                text = { Text("Añadir a playlist", color = Color.White) },
                onClick = onAddToPlaylist,
                leadingIcon = { Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = Color.White) }
            )

            val shareContext = androidx.compose.ui.platform.LocalContext.current
            DropdownMenuItem(
                text = { Text("Compartir", color = Color.White) },
                onClick = {
                    try {
                        val shareText = "¡Escuchando ${track.displayTitle} de ${track.displayArtist} en VibPlay!"
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir pista con")
                        shareContext.startActivity(shareIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    onDismiss()
                },
                leadingIcon = { Icon(Icons.Rounded.Share, contentDescription = null, tint = Color.White) }
            )

            if (playlistId != null && onRemoveFromPlaylist != null) {
                DropdownMenuItem(
                    text = { Text("Quitar de esta playlist", color = Color.White) },
                    onClick = {
                        onRemoveFromPlaylist()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistRemove, contentDescription = null, tint = colors.accent) }
                )
            }

            if (onEnterSelectMode != null) {
                DropdownMenuItem(
                    text = { Text("Activar Selección Múltiple", color = Color.White) },
                    onClick = {
                        onEnterSelectMode()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.Rounded.PlaylistAddCheck, contentDescription = null, tint = Color.White) }
                )
            }

            if (onSelectAll != null) {
                DropdownMenuItem(
                    text = { Text("Seleccionar Todo", color = Color.White) },
                    onClick = {
                        onSelectAll()
                        onDismiss()
                    },
                    leadingIcon = { Icon(Icons.Rounded.SelectAll, contentDescription = null, tint = Color.White) }
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(vertical = 8.dp))

            var showDeleteConfirm by remember { mutableStateOf(false) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF3B30).copy(alpha = 0.08f))
                    .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Borrar", tint = Color(0xFFFF3B30))
                Spacer(modifier = Modifier.width(16.dp))
                Text("Eliminar pista", color = Color(0xFFFF3B30), fontWeight = FontWeight.Bold)
            }

            if (showDeleteConfirm) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = false },
                    containerColor = colors.surface,
                    title = { Text("¿Eliminar pista?", fontWeight = FontWeight.Bold, color = Color.White) },
                    text = { Text("Esto borrará la canción completamente de la lista.", color = Color.LightGray) },
                    confirmButton = {
                        Button(
                            onClick = {
                                onDelete()
                                showDeleteConfirm = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30), contentColor = Color.White)
                        ) {
                            Text("Eliminar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = false }) {
                            Text("Cancelar", color = Color.White)
                        }
                    }
                )
            }
        }
    }
}

// Dialogs
@Composable
fun MetadataEditDialog(
    track: TrackEntity,
    colors: ThemeColors,
    onDismiss: () -> Unit,
    onSave: (String, String, String?, String, Boolean) -> Unit
) {
    var title by remember { mutableStateOf(track.displayTitle) }
    var artist by remember { mutableStateOf(track.displayArtist) }
    var folder by remember { mutableStateOf(track.folder) }
    var permanent by remember { mutableStateOf(true) }
    var artUri by remember { mutableStateOf(track.customArtUri ?: "") }

    val context = androidx.compose.ui.platform.LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            artUri = copyUriToLocalStorage(context, uri, "custom_art_${track.id}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Editar Información", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Carpeta") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = artUri,
                        onValueChange = { artUri = it },
                        label = { Text("Imagen (URL o Ruta)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primary.copy(alpha = 0.2f), contentColor = colors.primary)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Galería")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = permanent,
                        onCheckedChange = { permanent = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Text("Guardar permanentemente en BD", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, artist, if (artUri.isBlank()) null else artUri, folder, permanent) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun AddToPlaylistDialog(
    track: TrackEntity,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onDismiss: () -> Unit
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Añadir a playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { showCreateDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.12f), contentColor = colors.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear Playlist Nueva")
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (playlists.isEmpty()) {
                    Text("No hay playlists todavía.", style = MaterialTheme.typography.bodySmall, color = Color.Gray, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                } else {
                    LazyColumn(modifier = Modifier.height(180.dp)) {
                        items(playlists) { play ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToPlaylist(play.id, track.id)
                                        onDismiss()
                                    }
                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = colors.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(play.name, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cerrar") }
        }
    )

    if (showCreateDialog) {
        CreatePlaylistDialog(viewModel = viewModel, colors = colors, onDismiss = { showCreateDialog = false })
    }
}

@Composable
fun AddSongsToPlaylistDialog(
    playlistId: Long,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onDismiss: () -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val listTracks by viewModel.getTracksForPlaylistFlow(playlistId).collectAsStateWithLifecycle(emptyList())

    val availableTracks = remember(allTracks, listTracks) {
        allTracks.filter { track -> !listTracks.any { it.id == track.id } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = {
            Text(
                "Añadir canciones a la playlist",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Selecciona canciones de tu librería para añadirlas directamente:",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (availableTracks.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Todas las canciones ya están añadidas.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableTracks) { track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.addTrackToPlaylist(playlistId, track.id)
                                    },
                                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f)),
                                border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (track.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = colors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = track.displayTitle,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.displayArtist,
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.AddCircle,
                                        contentDescription = "Añadir",
                                        tint = colors.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
            ) {
                Text("Listo", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun CreatePlaylistDialog(viewModel: PlayerViewModel, colors: ThemeColors, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var coverUrl by remember { mutableStateOf("") }

    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coverUrl = copyUriToLocalStorage(context, uri, "playlist_art")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Crear Lista de reproducción", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = coverUrl,
                        onValueChange = { coverUrl = it },
                        label = { Text("Imagen (URL o Ruta)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { pickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primary.copy(alpha = 0.2f), contentColor = colors.primary)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Galería")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        viewModel.createPlaylist(name, desc, if (coverUrl.isBlank()) null else coverUrl)
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("Crear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// Library Tab (combining list, subfolders and HorizonalPager)
@Composable
fun LibraryScreen(
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    activePlaylistDetail: PlaylistEntity?,
    onActivePlaylistDetailChange: (PlaylistEntity?) -> Unit,
    onVideoSelect: (TrackEntity) -> Unit,
    videosGridState: androidx.compose.foundation.lazy.grid.LazyGridState
) {
    val tabs = remember(viewModel) {
        val list = mutableListOf("playlists", "videos", "folders")
        if (viewModel.hasSdCard()) {
            list.add("sdcard")
        }
        list
    }
    var selectedSubTab by remember(tabs) { mutableStateOf(tabs.first()) }
    val appTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (activePlaylistDetail == null) {
            HeaderTitleSection(appTheme, colors)

            ScrollableTabRow(
                selectedTabIndex = tabs.indexOf(selectedSubTab).coerceAtLeast(0),
                containerColor = Color.Transparent,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    val tabIdx = tabs.indexOf(selectedSubTab).coerceAtLeast(0)
                    if (tabIdx < tabPositions.size) {
                        val currentTab = tabPositions[tabIdx]
                        Box(
                            modifier = Modifier
                                .tabIndicatorOffset(currentTab)
                                .height(3.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(colors.primary, colors.secondary))
                                )
                        )
                    }
                },
                divider = {},
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = selectedSubTab == tab,
                        onClick = { selectedSubTab = tab },
                        text = {
                            Text(
                                text = when (tab) {
                                    "playlists" -> "Playlists"
                                    "videos" -> "Videos"
                                    "folders" -> "Carpetas"
                                    else -> "Tarjeta SD"
                                },
                                fontWeight = if (selectedSubTab == tab) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            val pagerState = rememberPagerState(pageCount = { tabs.size })
            LaunchedEffect(pagerState.currentPage) {
                val prospective = tabs.getOrNull(pagerState.currentPage)
                if (prospective != null) {
                    selectedSubTab = prospective
                }
            }
            LaunchedEffect(selectedSubTab) {
                val idx = tabs.indexOf(selectedSubTab)
                if (idx != -1 && idx != pagerState.currentPage) {
                    pagerState.animateScrollToPage(idx)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (tabs[page]) {
                    "playlists" -> PlaylistsDashboard(viewModel, colors, null, onActivePlaylistDetailChange)
                    "videos" -> VideosPanel(viewModel, colors, videosGridState, onVideoSelect)
                    "folders" -> FoldersDashboard(viewModel, colors)
                    "sdcard" -> SdCardPanel(viewModel, colors)
                }
            }
        } else {
            PlaylistsDashboard(
                viewModel = viewModel,
                colors = colors,
                activePlaylistDetail = activePlaylistDetail,
                onActivePlaylistDetailChange = onActivePlaylistDetailChange
            )
        }
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun EditPlaylistDialog(
    playlist: PlaylistEntity,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    var desc by remember { mutableStateOf(playlist.description) }
    var coverUrl by remember { mutableStateOf(playlist.coverUrl ?: "") }

    val context = LocalContext.current
    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coverUrl = copyUriToLocalStorage(context, uri, "playlist_art_${playlist.id}")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Editar Playlist", fontWeight = FontWeight.Bold, color = Color.White) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre", color = colors.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Descripción", color = colors.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                Text("Imagen de Portada", fontWeight = FontWeight.Bold, color = colors.primary, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                
                OutlinedTextField(
                    value = coverUrl,
                    onValueChange = { coverUrl = it },
                    placeholder = { Text("URL de la imagen o selecciona local", color = Color.White.copy(alpha = 0.4f), fontSize = 11.sp) },
                    label = { Text("URL de Portada", color = colors.primary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f)
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Button(
                    onClick = { pickerLauncher.launch("image/*") },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Elegir Imagen Local", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updated = playlist.copy(name = name, description = desc, coverUrl = coverUrl)
                    viewModel.updatePlaylist(updated)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("Guardar", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.White)
            }
        }
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlaylistsDashboard(
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    activePlaylistDetail: PlaylistEntity? = null,
    onActivePlaylistDetailChange: (PlaylistEntity?) -> Unit = {}
) {
    val playlists by viewModel.allPlaylists.collectAsStateWithLifecycle()
    var showCreate by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var showAddSongsDialog by remember { mutableStateOf(false) }

    if (activePlaylistDetail != null) {
        // Find the fresh model from playlists flow in case it was updated under the hood
        val freshPlaylist = playlists.find { it.id == activePlaylistDetail.id } ?: activePlaylistDetail
        val listTracks by viewModel.getTracksForPlaylistFlow(freshPlaylist.id).collectAsStateWithLifecycle(emptyList())

        val sortedTracks = remember(listTracks, freshPlaylist.orderedTrackIds) {
            val orderIds = freshPlaylist.orderedTrackIds.split(",").filter { it.isNotBlank() }.mapNotNull { it.toLongOrNull() }
            if (orderIds.isEmpty()) {
                listTracks
            } else {
                val orderMap = orderIds.withIndex().associate { it.value to it.index }
                listTracks.sortedWith(compareBy<TrackEntity> { orderMap[it.id] ?: Int.MAX_VALUE }.thenBy { it.id })
            }
        }

        fun moveTrack(trackId: Long, direction: Int) { // -1 = up, 1 = down
            val trackIds = sortedTracks.map { it.id }.toMutableList()
            val index = trackIds.indexOf(trackId)
            if (index != -1) {
                val targetIndex = index + direction
                if (targetIndex in 0 until trackIds.size) {
                    val removed = trackIds.removeAt(index)
                    trackIds.add(targetIndex, removed)
                    viewModel.reorderTracksInPlaylist(freshPlaylist.id, trackIds)
                }
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    onActivePlaylistDetailChange(null) 
                    isReorderMode = false
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                }
                Text("Playlist", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = colors.primary)
                Spacer(modifier = Modifier.weight(1f))
                
                // Add songs directly button
                TextButton(
                    onClick = { showAddSongsDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = colors.primary)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = "Añadir", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Añadir")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Edit playlist details button
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Editar Playlist", tint = Color.White)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Reorder toggle
                TextButton(
                    onClick = { isReorderMode = !isReorderMode },
                    colors = ButtonDefaults.textButtonColors(contentColor = if (isReorderMode) colors.accent else colors.primary)
                ) {
                    Icon(if (isReorderMode) Icons.Rounded.CheckCircle else Icons.Rounded.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isReorderMode) "Listo" else "Organizar")
                }
            }

            // Playlist Info Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cover box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface.copy(alpha = 0.5f))
                        .border(1.dp, colors.primary.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!freshPlaylist.coverUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = getCoilModel(freshPlaylist.coverUrl),
                            contentDescription = "Playlist Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.2f), colors.secondary.copy(alpha = 0.2f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(48.dp), tint = colors.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(freshPlaylist.name, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    if (freshPlaylist.description.isNotBlank()) {
                        Text(freshPlaylist.description, fontSize = 13.sp, color = Color.LightGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("${sortedTracks.size} canciones", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                }

                // Green Spotify style floating big Play Button
                if (sortedTracks.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { viewModel.onTrackSelected(sortedTracks.first(), sortedTracks) },
                        containerColor = colors.primary,
                        contentColor = Color.Black,
                        shape = CircleShape,
                        modifier = Modifier.size(52.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", modifier = Modifier.size(28.dp))
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.15f), modifier = Modifier.padding(horizontal = 16.dp))

            if (sortedTracks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Lista vacía",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Añade ritmos de tu librería local a esta playlist.",
                            color = Color.Gray,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAddSongsDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Añadir Canciones", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                if (isReorderMode) {
                    // Custom interactive drag/reorder list with glide animations
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(bottom = 120.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(sortedTracks, key = { _, t -> t.id }) { index, track ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .animateItemPlacement(tween(250)),
                                colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.4f)),
                                border = BorderStroke(1.dp, colors.surface.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { moveTrack(track.id, -1) },
                                        enabled = index > 0
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Mover arriba", tint = if (index > 0) colors.primary else Color.DarkGray)
                                    }
                                    IconButton(
                                        onClick = { moveTrack(track.id, 1) },
                                        enabled = index < sortedTracks.size - 1
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Mover abajo", tint = if (index < sortedTracks.size - 1) colors.primary else Color.DarkGray)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(track.displayTitle, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(track.displayArtist, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { viewModel.removeTrackFromPlaylist(freshPlaylist.id, track.id) }) {
                                        Icon(Icons.Rounded.PlaylistRemove, contentDescription = "Quitar", tint = colors.accent)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    TracksListPanel(
                        tracks = sortedTracks,
                        viewModel = viewModel,
                        colors = colors,
                        playlistId = freshPlaylist.id,
                        showSearchBar = false
                    )
                }
            }
        }

        if (showEditDialog) {
            EditPlaylistDialog(
                playlist = freshPlaylist,
                viewModel = viewModel,
                colors = colors,
                onDismiss = { showEditDialog = false }
            )
        }

        if (showAddSongsDialog) {
            AddSongsToPlaylistDialog(
                playlistId = freshPlaylist.id,
                viewModel = viewModel,
                colors = colors,
                onDismiss = { showAddSongsDialog = false }
            )
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Button(
                onClick = { showCreate = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Crear nueva lista", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (playlists.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cero listas. Añade una para agrupar tus ritmos.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(playlists) { play ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onActivePlaylistDetailChange(play) },
                            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f)),
                            border = BorderStroke(1.dp, colors.surface.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Playlist Cover
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.primary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!play.coverUrl.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = getCoilModel(play.coverUrl),
                                            contentDescription = "Playlist Cover",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Rounded.QueueMusic, contentDescription = null, tint = colors.primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(play.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                    if (play.description.isNotBlank()) {
                                        Text(play.description, fontSize = 12.sp, color = Color.LightGray)
                                    }
                                }
                                if (play.name.trim().lowercase() != "favoritos") {
                                    IconButton(onClick = { viewModel.removePlaylist(play.id) }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "Borrar", tint = colors.accent.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreatePlaylistDialog(viewModel = viewModel, colors = colors, onDismiss = { showCreate = false })
    }
}

@Composable
fun VideosPanel(
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    gridState: androidx.compose.foundation.lazy.grid.LazyGridState,
    onVideoSelect: (TrackEntity) -> Unit
) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val videos = allTracks.filter { it.isVideo }

    var selectMode by remember { mutableStateOf(false) }
    val selectedIds = remember { mutableStateListOf<Long>() }

    var columnsCount by remember { mutableStateOf(2) }
    var accumScale by remember { mutableStateOf(1f) }

    var showMoveFolderDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current

    fun shareVideos() {
        val selectedVideos = videos.filter { selectedIds.contains(it.id) }
        val uris = selectedVideos.mapNotNull { tr ->
            try {
                val file = java.io.File(tr.filePath)
                if (file.exists()) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.provider",
                        file
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
        if (uris.isNotEmpty()) {
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "video/*"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir videos con VibPlay"))
        } else {
            val text = selectedVideos.joinToString("\n") { "🎬 ${it.displayTitle} (${it.folder})" }
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, "¡Mira mis videos favoritos en VibPlay!\n\n$text")
            }
            context.startActivity(Intent.createChooser(intent, "Compartir videos con VibPlay"))
        }
        selectedIds.clear()
        selectMode = false
    }

    val transformState = rememberTransformableState { zoomChange, _, _ ->
        accumScale *= zoomChange
        if (accumScale > 1.35f) {
            if (columnsCount > 1) {
                columnsCount--
            }
            accumScale = 1f
        } else if (accumScale < 0.65f) {
            if (columnsCount < 5) {
                columnsCount++
            }
            accumScale = 1f
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (selectMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedIds.clear()
                    selectMode = false
                }) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancelar", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${selectedIds.size} seleccionados",
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = {
                    if (selectedIds.size == videos.size) {
                        selectedIds.clear()
                        selectMode = false
                    } else {
                        selectedIds.clear()
                        selectedIds.addAll(videos.map { it.id })
                    }
                }) {
                    Icon(
                        imageVector = if (selectedIds.size == videos.size) Icons.Rounded.CheckBoxOutlineBlank else Icons.Rounded.SelectAll,
                        contentDescription = "Seleccionar todo",
                        tint = Color.White
                    )
                }

                IconButton(
                    onClick = { shareVideos() },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.Share, contentDescription = "Compartir", tint = if (selectedIds.isNotEmpty()) Color.White else Color.DarkGray)
                }

                IconButton(
                    onClick = { showMoveFolderDialog = true },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.FolderOpen, contentDescription = "Mover de carpeta", tint = if (selectedIds.isNotEmpty()) Color.White else Color.DarkGray)
                }

                IconButton(
                    onClick = { showDeleteConfirmDialog = true },
                    enabled = selectedIds.isNotEmpty()
                ) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Eliminar", tint = if (selectedIds.isNotEmpty()) colors.accent else Color.DarkGray)
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.GridView, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Columnas: $columnsCount (Pellizca para zoom)",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { if (columnsCount > 1) columnsCount-- },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = "Menos columnas", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { if (columnsCount < 5) columnsCount++ },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Rounded.AddCircleOutline, contentDescription = "Más columnas", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }
        }

        if (videos.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No se detectaron videoclips de la librería local.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnsCount),
                state = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = transformState),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(videos) { track ->
                    val thumbnailState = produceState<Bitmap?>(initialValue = null, track.filePath) {
                        value = withContext(Dispatchers.IO) { getVideoThumbnail(track.filePath) }
                    }

                    var showContextSheet by remember { mutableStateOf(false) }
                    var showEditDialog by remember { mutableStateOf(false) }
                    var showPlaylistDialog by remember { mutableStateOf(false) }

                    val favTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
                    val isFavorite = favTracks.any { it.id == track.id }
                    val isSelected = selectedIds.contains(track.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(track.id, selectMode) {
                                detectTapGestures(
                                    onTap = {
                                        if (selectMode) {
                                            if (isSelected) {
                                                selectedIds.remove(track.id)
                                                if (selectedIds.isEmpty()) {
                                                    selectMode = false
                                                }
                                            } else {
                                                selectedIds.add(track.id)
                                            }
                                        } else {
                                            onVideoSelect(track)
                                        }
                                    },
                                    onLongPress = {
                                        if (!selectMode) {
                                            selectMode = true
                                            selectedIds.add(track.id)
                                        } else {
                                            showContextSheet = true
                                        }
                                    }
                                )
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) colors.primary.copy(alpha = 0.25f) else colors.surface.copy(alpha = 0.5f)
                        ),
                        border = if (isSelected) BorderStroke(1.5.dp, colors.primary) else null,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                        ) {
                            if (thumbnailState.value != null) {
                                Image(
                                    bitmap = thumbnailState.value!!.asImageBitmap(),
                                    contentDescription = "Miniatura",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.6f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Videocam,
                                        contentDescription = "Fallback",
                                        tint = colors.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            if (selectMode) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSelected) colors.primary else Color.Black.copy(alpha = 0.5f)
                                        )
                                        .border(
                                            1.dp,
                                            if (isSelected) Color.Transparent else Color.White,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(colors.primary.copy(alpha = 0.85f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }

                            if (track.duration > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(4.dp)
                                        .background(Color.Black.copy(alpha = 0.7f))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = formatTime(track.duration.toInt()),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = track.displayTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = track.folder,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.primary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (showContextSheet) {
                        ContextMenuBottomSheet(
                            track = track,
                            isFavorite = isFavorite,
                            onDismiss = { showContextSheet = false },
                            colors = colors,
                            onToggleFavorite = { viewModel.toggleFavorite(track) },
                            onToggleEnglish = { viewModel.toggleEnglishTrack(track) },
                            onEdit = {
                                showEditDialog = true
                                showContextSheet = false
                            },
                            onAddToPlaylist = {
                                showPlaylistDialog = true
                                showContextSheet = false
                            },
                            onDelete = {
                                viewModel.deleteTrack(track)
                                showContextSheet = false
                            },
                            onEnterSelectMode = {
                                selectMode = true
                                selectedIds.add(track.id)
                                showContextSheet = false
                            },
                            onSelectAll = {
                                selectMode = true
                                selectedIds.clear()
                                selectedIds.addAll(videos.map { it.id })
                                showContextSheet = false
                            }
                        )
                    }

                    if (showEditDialog) {
                        MetadataEditDialog(
                            track = track,
                            colors = colors,
                            onDismiss = { showEditDialog = false },
                            onSave = { t, a, ur, f, perm ->
                                viewModel.editTrackMetadata(track.id, t, a, ur, f, perm)
                                showEditDialog = false
                            }
                        )
                    }

                    if (showPlaylistDialog) {
                        AddToPlaylistDialog(
                            track = track,
                            viewModel = viewModel,
                            colors = colors,
                            onDismiss = { showPlaylistDialog = false }
                        )
                    }
                }
            }
        }
    }

    if (showMoveFolderDialog) {
        var newFolder by remember { mutableStateOf("") }
        val folders by viewModel.availableFolders.collectAsStateWithLifecycle()

        AlertDialog(
            onDismissRequest = { showMoveFolderDialog = false },
            containerColor = colors.surface,
            title = { Text("Mover Videos de Carpeta", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Selecciona una carpeta o escribe una nueva para los videos seleccionados:", style = MaterialTheme.typography.bodySmall)
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(modifier = Modifier.fillMaxWidth()) {
                        items(folders) { f ->
                            SuggestionChip(
                                onClick = { newFolder = f },
                                label = { Text(f) },
                                modifier = Modifier.padding(end = 6.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = newFolder,
                        onValueChange = { newFolder = it },
                        label = { Text("Nombre de carpeta") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolder.isNotBlank()) {
                            viewModel.moveTracksToFolder(selectedIds.toList(), newFolder, isPermanent = true)
                            selectedIds.clear()
                            selectMode = false
                            showMoveFolderDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
                ) {
                    Text("Mover")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMoveFolderDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = colors.surface,
            title = { Text("Eliminar videos", fontWeight = FontWeight.Bold, color = Color.White) },
            text = { Text("¿Estás seguro de que deseas eliminar los ${selectedIds.size} videos seleccionados de tu lista?", color = Color.LightGray) },
            confirmButton = {
                Button(
                    onClick = {
                        selectedIds.forEach { id ->
                            videos.find { it.id == id }?.let { viewModel.deleteTrack(it) }
                        }
                        selectedIds.clear()
                        selectMode = false
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar", color = Color.White)
                }
            }
        )
    }
}

// Int extension for direct sizing translation
fun Int.jpgToDp() = this.dp

@Composable
fun FoldersDashboard(viewModel: PlayerViewModel, colors: ThemeColors) {
    val folders by viewModel.availableFolders.collectAsStateWithLifecycle()
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    var selectedFolderDetail by remember { mutableStateOf<String?>(null) }

    val foldersToShow = remember(folders, allTracks) {
        folders.filter { fold -> allTracks.any { it.folder == fold && !it.isVideo } }
    }

    if (selectedFolderDetail != null) {
        val tracksInF = allTracks.filter { it.folder == selectedFolderDetail && !it.isVideo }
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedFolderDetail = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Carpeta: $selectedFolderDetail",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            TracksListPanel(tracks = tracksInF, viewModel = viewModel, colors = colors)
        }
    } else {
        if (foldersToShow.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cero carpetas localizadas.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(foldersToShow) { fold ->
                    val cnt = allTracks.count { it.folder == fold && !it.isVideo }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFolderDetail = fold },
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = colors.primary, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(fold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("$cnt pistas contenidas", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SdCardPanel(viewModel: PlayerViewModel, colors: ThemeColors) {
    val context = LocalContext.current
    var isSimulatingScan by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var detectedCount by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.SdStorage, contentDescription = null, modifier = Modifier.size(64.dp), tint = colors.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Detección de Almacenamiento Externo (SD)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Escanear directorios alternativos externos e importar pistas de simulación en la Base de Datos offline.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isSimulatingScan) {
            LinearProgressIndicator(progress = { scanProgress }, color = colors.primary, modifier = Modifier.fillMaxWidth(0.8f))
            Spacer(modifier = Modifier.height(8.dp))
            Text("Analizando partición external_sd... $detectedCount pistas halladas", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
        } else {
            Button(
                onClick = {
                    isSimulatingScan = true
                    scanProgress = 0f
                    detectedCount = 0
                    scope.launch {
                        for (i in 1..100) {
                            delay(15)
                            scanProgress = i / 100f
                            if (i % 25 == 0) detectedCount += 2
                        }
                        isSimulatingScan = false
                        viewModel.simulateLocalTrackImport("SD Beat $detectedCount", "External Artist", "sd_track_$detectedCount.mp3", false, "SD storage")
                        Toast.makeText(context, "Sincronizadas e Importadas pistas de la SD", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("Escanear Tarjeta SD", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Mini Player Card Component
@Composable
fun MiniPlayerCard(
    currentTrack: TrackEntity,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onOpenFullPlayer: () -> Unit
) {
    val isPlaying by AudioEngine.isPlaying.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val isFavorite = favorites.any { it.id == currentTrack.id }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFullPlayer() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.5f))
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TinyIcon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, size = 14.dp, tint = colors.primary)
                Spacer(modifier = Modifier.width(6.dp))
                Text("VibPlay Reproductor", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.stopAndClear() }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                }
            }
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                ) {
                    if (!currentTrack.customArtUri.isNullOrEmpty()) {
                        AsyncImage(
                            model = getCoilModel(currentTrack.customArtUri),
                            contentDescription = "Cover",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(colors.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (currentTrack.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(currentTrack.displayTitle, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                    Text(currentTrack.displayArtist, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }

                IconButton(onClick = { viewModel.toggleFavorite(currentTrack) }) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color(0xFFFF3B5C) else Color.Gray,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(onClick = { viewModel.toggleShuffleRepeat() }) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = "Aleatorio", tint = colors.primary, modifier = Modifier.size(20.dp))
                }

                IconButton(onClick = { viewModel.playPrevious() }) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                }

                IconButton(
                    onClick = { viewModel.playOrPause() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(colors.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black
                    )
                }

                IconButton(onClick = { viewModel.playNext() }) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                }
            }
        }
    }
}

// Icon helper function for tiny graphics sizes
@Composable
fun TinyIcon(imageVector: androidx.compose.ui.graphics.vector.ImageVector, contentDescription: String?, size: androidx.compose.ui.unit.Dp, tint: Color) {
    androidx.compose.material3.Icon(imageVector = imageVector, contentDescription = contentDescription, modifier = Modifier.size(size), tint = tint)
}

// Custom visual Slider with interactive rolling phases
@Composable
fun WavySlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    themeColors: ThemeColors,
    modifier: Modifier = Modifier
) {
    val anim = rememberInfiniteTransition(label = "rolling_waves")
    val phase by anim.animateFloat(
        initialValue = 0f, targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart), label = "phase"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / widthPx).coerceIn(0f, 1f)
                        onValueChange(ratio)
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val ratio = (change.position.x / widthPx).coerceIn(0f, 1f)
                        onValueChange(ratio)
                    }
                }
        ) {
            val h = size.height
            val centerY = h / 2f
            val activeX = widthPx * value

            // Inactive track
            drawLine(
                color = Color.Gray.copy(alpha = 0.25f),
                start = Offset(0f, centerY),
                end = Offset(widthPx, centerY),
                strokeWidth = 4f
            )

            // Active sinusoidal track
            if (activeX > 0f) {
                val wavePath = Path()
                val amp = 8f
                val freq = 0.04f
                wavePath.moveTo(0f, centerY)
                var lx = 0f
                while (lx <= activeX) {
                    val ly = centerY + amp * kotlin.math.sin(freq * lx + phase)
                    wavePath.lineTo(lx, ly)
                    lx += 3f
                }
                drawPath(wavePath, themeColors.primary, style = Stroke(width = 4.5f))
            }

            // Knob
            drawCircle(glowColorField(themeColors).copy(alpha = 0.4f), radius = 14.dp.toPx(), center = Offset(activeX, centerY))
            drawCircle(Color.White, radius = 7.dp.toPx(), center = Offset(activeX, centerY))
        }
    }
}

fun glowColorField(theme: ThemeColors) = theme.accent

// Dynamic sound canvas
@Composable
fun InteractiveVisualizerCanvas(
    bands: FloatArray,
    color: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val spacing = 14f
        val cols = 8
        val barW = (w - (spacing * (cols - 1))) / cols

        val gridC = color.copy(alpha = 0.06f)
        for (i in 1..3) {
            val y = h * (i / 4f)
            drawLine(gridC, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
        }

        for (i in 0 until cols) {
            val pct = bands.getOrElse(i) { 0.05f }
            val barH = h * pct
            val x = i * (barW + spacing)
            val y = h - barH

            drawRoundRect(
                color = glowColor.copy(alpha = 0.22f),
                topLeft = Offset(x - 2f, y - 2f),
                size = androidx.compose.ui.geometry.Size(barW + 4f, barH + 4f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = androidx.compose.ui.geometry.Size(barW, barH),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }
    }
}

// cubic spline curve EQ frequency component
@Composable
fun EqFrequencyCurve(
    b32: Int, b125: Int, b500: Int, b2k: Int, b8k: Int,
    accentColor: Color, glowColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val gridColor = accentColor.copy(alpha = 0.08f)
        for (i in 0..4) {
            val y = h * (i / 4f)
            val x = w * (i / 4f)
            drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1f)
            drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
        }

        val levels = floatArrayOf(b32.toFloat(), b125.toFloat(), b500.toFloat(), b2k.toFloat(), b8k.toFloat())
        val pts = Array(5) { i ->
            val x = w * (i / 4f)
            val normY = (levels[i] + 15f) / 30f
            val y = h * (1f - normY)
            Offset(x, y)
        }

        val fillPath = Path().apply {
            moveTo(0f, h)
            lineTo(pts[0].x, pts[0].y)
            for (i in 0 until 4) {
                val p0 = pts[i]
                val p1 = pts[i + 1]
                val cx1 = p0.x + (p1.x - p0.x) / 2f
                val cy1 = p0.y
                val cx2 = p0.x + (p1.x - p0.x) / 2f
                val cy2 = p1.y
                cubicTo(cx1, cy1, cx2, cy2, p1.x, p1.y)
            }
            lineTo(w, h)
            close()
        }
        drawPath(fillPath, brush = Brush.verticalGradient(listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)))

        val strokePath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 0 until 4) {
                val p1 = pts[i]
                val p2 = pts[i + 1]
                val cx1 = p1.x + (p2.x - p1.x) / 2f
                val cy1 = p1.y
                val cx2 = p1.x + (p2.x - p1.x) / 2f
                val cy2 = p2.y
                cubicTo(cx1, cy1, cx2, cy2, p2.x, p2.y)
            }
        }
        drawPath(strokePath, glowColor.copy(alpha = 0.5f), style = Stroke(8f))
        drawPath(strokePath, accentColor, style = Stroke(4f))
        drawPath(strokePath, Color.White, style = Stroke(1.5f))

        pts.forEach { pt ->
            drawCircle(glowColor.copy(alpha = 0.5f), radius = 10f, center = pt)
            drawCircle(accentColor, radius = 6f, center = pt)
            drawCircle(Color.White, radius = 3f, center = pt)
        }
    }
}

@Composable
fun CyberVerticalSlider(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    accentColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.LightGray,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        BoxWithConstraints(
            modifier = Modifier
                .width(36.dp)
                .height(130.dp),
            contentAlignment = Alignment.Center
        ) {
            val hPx = constraints.maxHeight.toFloat()
            val wPx = constraints.maxWidth.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val n = (hPx - offset.y) / hPx
                            val mappedVal = (-15 + n * 30f).toInt().coerceIn(-15, 15)
                            onValueChange(mappedVal)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            val n = (hPx - change.position.y) / hPx
                            val mappedVal = (-15 + n * 30f).toInt().coerceIn(-15, 15)
                            onValueChange(mappedVal)
                        }
                    }
            ) {
                for (i in 0..6) {
                    val y = hPx * (i / 6f)
                    drawLine(accentColor.copy(alpha = 0.15f), Offset(wPx*0.15f, y), Offset(wPx*0.35f, y), strokeWidth = 1.5f)
                }

                val pct = (value + 15) / 30f
                val dotY = hPx * (1f - pct)

                drawRoundRect(
                    color = accentColor.copy(alpha = 0.08f),
                    topLeft = Offset(wPx * 0.5f - 3f, 0f),
                    size = androidx.compose.ui.geometry.Size(6f, hPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                )
                if (pct > 0f) {
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(wPx * 0.5f - 3f, dotY),
                        size = androidx.compose.ui.geometry.Size(6f, hPx - dotY),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f)
                    )
                }
                drawCircle(glowColor.copy(alpha = 0.45f), radius = 10f, center = Offset(wPx * 0.5f, dotY))
                drawCircle(Color.White, radius = 5f, center = Offset(wPx * 0.5f, dotY))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (value >= 0) "+$value" else "$value",
            style = MaterialTheme.typography.labelSmall,
            color = accentColor,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}

@Composable
fun CyberCircularDial(
    value: Int,
    onValueChange: (Int) -> Unit,
    label: String,
    accentColor: Color,
    glowColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color.LightGray)
        Spacer(modifier = Modifier.height(5.dp))
        var curVal by remember { mutableStateOf(value) }

        Box(
            modifier = Modifier
                .size(76.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = (dragAmount.x - dragAmount.y) / 1.7f
                        val next = (curVal + delta.toInt()).coerceIn(0, 100)
                        if (next != curVal) {
                            curVal = next
                            onValueChange(next)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.width / 2f
                val strokeW = 8f

                // Inactive base
                drawArc(
                    color = accentColor.copy(alpha = 0.08f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(strokeW, cap = StrokeCap.Round)
                )
                // Active range
                val sweep = 270f * (value / 100f)
                drawArc(
                    color = accentColor,
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(strokeW + 2f, cap = StrokeCap.Round)
                )
                drawArc(
                    color = glowColor.copy(alpha = 0.25f),
                    startAngle = 135f,
                    sweepAngle = sweep,
                    useCenter = false,
                    style = Stroke(strokeW + 6f, cap = StrokeCap.Round)
                )

                // Knob Core plate
                drawCircle(Color.Black.copy(alpha = 0.5f), radius = r - strokeW - 2f)

                // Tick dot
                val tick = 135f + sweep
                val rad = Math.toRadians(tick.toDouble()).toFloat()
                val tx = r + (r - strokeW - 8f) * kotlin.math.cos(rad)
                val ty = r + (r - strokeW - 8f) * kotlin.math.sin(rad)
                drawCircle(Color.White, radius = 4f, center = Offset(tx, ty))
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text("$value%", style = MaterialTheme.typography.labelSmall, color = accentColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

// Full Player tab Screen
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onWatchVideo: (TrackEntity) -> Unit
) {
    val currentTrack by viewModel.currentTrack.collectAsStateWithLifecycle()

    if (currentTrack == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(72.dp), tint = colors.primary.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(16.dp))
            Text("Consola de Reproducción", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Text("Ninguna pista activa en este momento.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
    } else {
        PlayerConsoleHub(track = currentTrack!!, viewModel = viewModel, colors = colors, onWatchVideo = onWatchVideo)
    }
}

@Composable
fun PlayerConsoleHub(
    track: TrackEntity,
    viewModel: PlayerViewModel,
    colors: ThemeColors,
    onWatchVideo: (TrackEntity) -> Unit
) {
    val context = LocalContext.current
    val isPlaying by AudioEngine.isPlaying.collectAsStateWithLifecycle()
    val curPos by AudioEngine.currentPosition.collectAsStateWithLifecycle()
    val durSec by AudioEngine.duration.collectAsStateWithLifecycle()
    val speedFactor by AudioEngine.playbackSpeed.collectAsStateWithLifecycle()
    val bands by AudioEngine.visualizerBands.collectAsStateWithLifecycle()
    val playMode by viewModel.playbackMode.collectAsStateWithLifecycle()

    val favTracks by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val isFavorite = favTracks.any { it.id == track.id }

    var showEqPanel by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        InteractiveVisualizerCanvas(
            bands = bands,
            color = colors.primary,
            glowColor = colors.secondary,
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Generative art frame
        Box(
            modifier = Modifier
                .size(190.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .border(2.dp, colors.primary, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (!track.customArtUri.isNullOrEmpty()) {
                AsyncImage(
                    model = getCoilModel(track.customArtUri),
                    contentDescription = "Cover generativo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (track.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                        contentDescription = "Fallback",
                        tint = colors.primary,
                        modifier = Modifier.size(76.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = track.displayTitle,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
        Text(
            text = track.displayArtist,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            color = colors.primary,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (track.isVideo) {
            Button(
                onClick = { onWatchVideo(track) },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary.copy(alpha = 0.15f), contentColor = colors.primary),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(Icons.Rounded.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reproducir como Video", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Wave slider progress layout
        Column(modifier = Modifier.fillMaxWidth(0.85f)) {
            val progress = if (durSec > 0) curPos.toFloat() / durSec else 0f
            WavySlider(
                value = progress,
                onValueChange = { ratio ->
                    val nextMs = (durSec * ratio).toInt()
                    viewModel.seekTo(nextMs)
                },
                themeColors = colors
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(formatTime(curPos), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray, modifier = Modifier.weight(1f))
                Text(formatTime(durSec), fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Actions console row
        Row(
            modifier = Modifier.fillMaxWidth(0.9f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // cycle speeds
            val speeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            IconButton(onClick = {
                val nextI = (speeds.indexOf(speedFactor) + 1) % speeds.size
                viewModel.setPlaybackSpeed(speeds[nextI])
            }) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surface)
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text("${speedFactor}x", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
            }

            IconButton(onClick = { viewModel.playPrevious() }) {
                TinyIcon(Icons.Rounded.SkipPrevious, contentDescription = "Prev", size = 32.dp, tint = Color.White)
            }

            IconButton(
                onClick = { viewModel.playOrPause() },
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.primary)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(32.dp)
                )
            }

            IconButton(onClick = { viewModel.playNext() }) {
                TinyIcon(Icons.Rounded.SkipNext, contentDescription = "Next", size = 32.dp, tint = Color.White)
            }

            // Loop / Shuffle cycles
            IconButton(onClick = { viewModel.toggleShuffleRepeat() }) {
                Icon(
                    imageVector = when (playMode) {
                        PlaybackMode.SHUFFLE -> Icons.Rounded.Shuffle
                        PlaybackMode.REPEAT_ONE -> Icons.Rounded.RepeatOne
                        PlaybackMode.REPEAT_ALL -> Icons.Rounded.Repeat
                        else -> Icons.Rounded.Loop
                    },
                    contentDescription = "Mode",
                    tint = colors.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        Row(modifier = Modifier.fillMaxWidth(0.9f), horizontalArrangement = Arrangement.Center) {
            IconButton(onClick = { viewModel.toggleFavorite(track) }) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favorito",
                    tint = if (isFavorite) Color(0xFFFF3B5C) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { showEqPanel = !showEqPanel }) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = "Ecualizador", tint = if (showEqPanel) colors.primary else Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { showEditDialog = true }) {
                Icon(Icons.Rounded.Edit, contentDescription = "Editar", tint = colors.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            IconButton(onClick = { shareTrack(context, track) }) {
                Icon(Icons.Rounded.Share, contentDescription = "Compartir", tint = colors.primary)
            }
        }

        if (showEqPanel) {
            Spacer(modifier = Modifier.height(20.dp))
            EqualizerTuningPanel(viewModel = viewModel, colors = colors)
        }
    }

    if (showEditDialog) {
        MetadataEditDialog(
            track = track,
            colors = colors,
            onDismiss = { showEditDialog = false },
            onSave = { t, a, ur, f, perm ->
                viewModel.editTrackMetadata(track.id, t, a, ur, f, perm)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun EqualizerTuningPanel(viewModel: PlayerViewModel, colors: ThemeColors) {
    val b32 by viewModel.band32Hz.collectAsStateWithLifecycle()
    val b125 by viewModel.band125Hz.collectAsStateWithLifecycle()
    val b500 by viewModel.band500Hz.collectAsStateWithLifecycle()
    val b2k by viewModel.band2kHz.collectAsStateWithLifecycle()
    val b8k by viewModel.band8kHz.collectAsStateWithLifecycle()

    val bass by viewModel.bassBoost.collectAsStateWithLifecycle()
    val virt by viewModel.virtualizer.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.82f)),
        border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("ECUALIZADOR", style = MaterialTheme.typography.labelSmall, color = colors.primary, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Normal", "Rock", "Pop", "Jazz", "Metal").forEach { preset ->
                    Button(
                        onClick = { viewModel.applyPresetProfile(preset) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.surface, contentColor = colors.primary),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text(preset, fontSize = 9.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            EqFrequencyCurve(
                b32 = b32, b125 = b125, b500 = b500, b2k = b2k, b8k = b8k,
                accentColor = colors.primary,
                glowColor = colors.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Vert sliders
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CyberVerticalSlider(value = b32, onValueChange = { viewModel.modifyManualBand(0, it) }, label = "32Hz", accentColor = colors.primary, glowColor = colors.secondary)
                CyberVerticalSlider(value = b125, onValueChange = { viewModel.modifyManualBand(1, it) }, label = "125Hz", accentColor = colors.primary, glowColor = colors.secondary)
                CyberVerticalSlider(value = b500, onValueChange = { viewModel.modifyManualBand(2, it) }, label = "500Hz", accentColor = colors.primary, glowColor = colors.secondary)
                CyberVerticalSlider(value = b2k, onValueChange = { viewModel.modifyManualBand(3, it) }, label = "2kHz", accentColor = colors.primary, glowColor = colors.secondary)
                CyberVerticalSlider(value = b8k, onValueChange = { viewModel.modifyManualBand(4, it) }, label = "8kHz", accentColor = colors.primary, glowColor = colors.secondary)
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                CyberCircularDial(value = bass, onValueChange = { viewModel.modifyBassBoost(it) }, label = "BASS", accentColor = colors.primary, glowColor = colors.secondary)
                CyberCircularDial(value = virt, onValueChange = { viewModel.modifyVirtualizer(it) }, label = "VIRTUALIZER", accentColor = colors.primary, glowColor = colors.secondary)
            }
        }
    }
}

// Insights metrics
@Composable
fun InsightsScreen(viewModel: PlayerViewModel, colors: ThemeColors) {
    val allTracks by viewModel.allTracks.collectAsStateWithLifecycle()
    val playTime by viewModel.totalListeningTimeMinutes.collectAsStateWithLifecycle()
    val favorites by viewModel.favoriteTracks.collectAsStateWithLifecycle()
    val appTheme by viewModel.currentTheme.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        HeaderTitleSection(appTheme, colors)

        Text(
            text = "TU ACTIVIDAD VIBPLAY",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricStatsCard(label = "Minutos", value = "$playTime m", icon = Icons.Rounded.BarChart, colorMin = colors.primary, modifier = Modifier.weight(1f))
            MetricStatsCard(label = "Favoritos", value = "${favorites.size}", icon = Icons.Rounded.Favorite, colorMin = Color(0xFFFF3B5C), modifier = Modifier.weight(1f))
            MetricStatsCard(label = "Biblioteca", value = "${allTracks.size}", icon = Icons.Rounded.QueueMusic, colorMin = colors.primary, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "REPRODUCIDAS RECIENTEMENTE",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Lazy row of items
        val listSorted = allTracks.filter { it.lastPlayedAt > 0 }.sortedByDescending { it.lastPlayedAt }.take(6)
        if (listSorted.isEmpty()) {
            Text("No has reproducido canciones aún.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(listSorted) { tr ->
                    Card(
                        modifier = Modifier
                            .width(130.dp)
                            .clickable {
                                viewModel.onTrackSelected(tr, allTracks)
                            },
                        colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!tr.customArtUri.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = getCoilModel(tr.customArtUri),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(colors.primary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (tr.isVideo) Icons.Rounded.Videocam else Icons.Rounded.MusicNote,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(tr.displayTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(tr.displayArtist, fontSize = 10.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "TOP RÁNKING DE ESCUCHAS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.LightGray,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        val rankingList = allTracks.filter { it.playCount > 0 }.sortedByDescending { it.playCount }
        if (rankingList.isEmpty()) {
            Text("Las métricas de reproducción se computarán al acumular escuchas.", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rankingList.forEachIndexed { rank, tr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.surface.copy(alpha = 0.5f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${rank + 1}", fontWeight = FontWeight.Black, color = colors.primary, fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tr.displayTitle, fontWeight = FontWeight.Bold)
                            Text(tr.displayArtist, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colors.primary.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${tr.playCount} escuchas", fontSize = 10.sp, color = colors.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricStatsCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    colorMin: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            TinyIcon(icon, contentDescription = null, size = 22.dp, tint = colorMin)
            Spacer(modifier = Modifier.height(10.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Black)
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

// Settings screen with redone styles
@Composable
fun SettingsScreen(viewModel: PlayerViewModel, colors: ThemeColors) {
    val context = LocalContext.current
    val playTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
    val bubbleEnabled by viewModel.isFloatingBubbleEnabled.collectAsStateWithLifecycle()
    var creatorCreditsDialog by remember { mutableStateOf(false) }
    var isThemesExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 120.dp)
    ) {
        HeaderTitleSection(playTheme, colors)

        // Custom stylized Text
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
            Text(
                text = "DISEÑO Y TEMAS VISUALES",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.primary,
                letterSpacing = 2.sp
            )
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isThemesExpanded = !isThemesExpanded },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Estilo Activo:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = when (playTheme) {
                                ThemeStyle.CYBERPUNK -> "Cyberpunk"
                                ThemeStyle.LUXURY -> "Luxury Gold"
                                ThemeStyle.LAVA -> "Volcanic Lava"
                                ThemeStyle.TOXIC -> "Biochemical Toxic"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = if (isThemesExpanded) "Ocultar temas" else "Ver temas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.accent
                        )
                        Icon(
                            imageVector = if (isThemesExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isThemesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        ThemeSelectionBar(
                            activeTheme = playTheme,
                            onSelect = { viewModel.selectTheme(it) },
                            colors = colors
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Layers, contentDescription = null, tint = colors.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Burbuja Flotante de Control", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Mostrar overlay interactivo para reproducción flotante.", fontSize = 11.sp, color = Color.Gray)
                    }
                    Switch(
                        checked = bubbleEnabled,
                        onCheckedChange = { chk ->
                            if (chk && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                viewModel.setFloatingBubbleEnabled(chk, context)
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.35f))
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                // mini preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.04f))
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(colors.primary))
                        TinyIcon(Icons.Rounded.PlayArrow, contentDescription = null, size = 14.dp, tint = colors.primary)
                        TinyIcon(Icons.Rounded.SkipNext, contentDescription = null, size = 14.dp, tint = Color.LightGray)
                        TinyIcon(Icons.Rounded.Close, contentDescription = null, size = 12.dp, tint = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        val bgPlaybackPremiumEnabled by viewModel.isBackgroundPlaybackPremiumEnabled.collectAsStateWithLifecycle()

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surface.copy(alpha = 0.6f)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, colors.primary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Headset, contentDescription = null, tint = colors.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo en segundo plano", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text(
                            text = "Al salir de la aplicación estando en un vídeo, se seguirá reproduciendo en segundo plano",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = bgPlaybackPremiumEnabled,
                        onCheckedChange = { chk ->
                            viewModel.setBackgroundPlaybackPremiumEnabled(chk)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = colors.primary, checkedTrackColor = colors.primary.copy(alpha = 0.35f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            val pulseG by rememberInfiniteTransition(label = "pulse_g").animateFloat(
                initialValue = 0.45f, targetValue = 0.95f,
                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "p_g"
            )

            Button(
                onClick = { creatorCreditsDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.primary.copy(alpha = pulseG),
                    contentColor = Color.Black
                ),
                border = BorderStroke(1.5.dp, colors.primary),
                modifier = Modifier.fillMaxWidth(0.85f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Person, contentDescription = null)
                Spacer(modifier = Modifier.width(10.dp))
                Text("CREDITOS & CREDENCIAL SOCIAL", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "VibPlay v2.8p1-Arg · AxelDev09 Creative Studio",
            fontSize = 11.sp,
            color = Color.Gray,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 32.dp),
            textAlign = TextAlign.Center
        )
    }

    if (creatorCreditsDialog) {
        Dialog(onDismissRequest = { creatorCreditsDialog = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = colors.surface),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, colors.primary)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("AxelDev09 Studio", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = colors.primary)
                    Text("Diseño & Estructura de Alta Fidelidad", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    SocialMediaCard(title = "GitHub Oficial", desc = "/axeldev09", iconInt = R.drawable.ic_github, colorBase = Color(0xFF1E2430)) {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com")))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se puede abrir link", Toast.LENGTH_SHORT).show()
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SocialMediaCard(title = "Instagram", desc = "@un.axel.salvaje", iconInt = R.drawable.ic_instagram, colorBase = Color(0xFFC13584)) {
                        // ignore action
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SocialMediaCard(title = "Canal WhatsApp", desc = "VibPlay Comunidad", iconInt = R.drawable.ic_whatsapp, colorBase = Color(0xFF25D366)) {
                        // ignore action
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { creatorCreditsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = Color.White)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeSelectionBar(
    activeTheme: ThemeStyle,
    onSelect: (ThemeStyle) -> Unit,
    colors: ThemeColors
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val listThemes = listOf(
            Triple(ThemeStyle.CYBERPUNK, Icons.Rounded.FlashOn, "Cyberpunk"),
            Triple(ThemeStyle.LUXURY, Icons.Rounded.Star, "Luxury Gold"),
            Triple(ThemeStyle.LAVA, Icons.Rounded.Whatshot, "Volcanic Lava"),
            Triple(ThemeStyle.TOXIC, Icons.Rounded.Warning, "Biochemical Toxic")
        )

        listThemes.forEach { (style, icon, label) ->
            val active = activeTheme == style
            val sScale by animateFloatAsState(if (active) 1.04f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .scale(sScale)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (active) colors.primary.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.02f))
                    .border(
                        if (active) 2.dp else 1.dp,
                        if (active) colors.primary else Color.Gray.copy(alpha = 0.2f),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onSelect(style) }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = if (active) colors.primary else Color.Gray)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = label,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) colors.primary else Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (active) {
                    Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = colors.primary)
                }
            }
        }
    }
}

@Composable
fun SocialMediaCard(
    title: String,
    desc: String,
    iconInt: Int,
    colorBase: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = colorBase.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, colorBase.copy(alpha = 0.4f))
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(iconInt), contentDescription = null, modifier = Modifier.size(28.dp).clip(CircleShape))
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, fontSize = 11.sp, color = Color.LightGray)
            }
        }
    }
}

// Full screen custom HUD video player
@Composable
fun VideoPlayerFrameOverlay(
    track: TrackEntity,
    onClose: () -> Unit,
    colors: ThemeColors,
    viewModel: PlayerViewModel,
    onTrackChange: (TrackEntity) -> Unit
) {
    val context = LocalContext.current
    var isControlsVisible by remember { mutableStateOf(true) }
    var videoZoom by remember { mutableFloatStateOf(1f) }
    val isPlayingVideo = remember { mutableStateOf(true) }
    val isPausedByUser = remember { mutableStateOf(false) }

    val initialPos = if (AudioEngine.currentTrackId.value == track.id) AudioEngine.currentPosition.value else 0
    val pos = remember(track.id) { mutableStateOf(initialPos) }
    val dur = remember { mutableStateOf(0) }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    val transformState = rememberTransformableState { zoomChange, _, _ ->
        videoZoom = (videoZoom * zoomChange).coerceIn(1f, 5f)
    }

    val isBgPremiumEnabled by viewModel.isBackgroundPlaybackPremiumEnabled.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, track, isBgPremiumEnabled) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP || event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                if (isBgPremiumEnabled && !isPausedByUser.value) {
                    val currentPos = if (track.filePath.startsWith("/simulated/")) pos.value else {
                        videoViewInstance?.currentPosition ?: pos.value
                    }
                    viewModel.playVideoAsBackgroundAudio(track, currentPos)
                    onClose()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Intercept back button to close/minimize the video view instead of exiting!
    BackHandler {
        onClose()
    }

    // Auto-hide controls loop
    LaunchedEffect(isControlsVisible) {
        if (isControlsVisible) {
            delay(4000)
            isControlsVisible = false
        }
    }

    // Progress tracker matching real-time VideoView pos and duration
    LaunchedEffect(videoViewInstance, isPlayingVideo.value, track.id) {
        while (true) {
            if (track.filePath.startsWith("/simulated/")) {
                if (isPlayingVideo.value) {
                    pos.value = (pos.value + 500).coerceAtMost(dur.value)
                }
            } else {
                videoViewInstance?.let { vv ->
                    if (vv.isPlaying) {
                        pos.value = vv.currentPosition
                        dur.value = vv.duration
                    }
                }
            }
            delay(500)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { isControlsVisible = !isControlsVisible }
                )
            }
            .transformable(state = transformState)
    ) {
        if (track.filePath.startsWith("/simulated/")) {
            // Render highly interactive cyber generative canvas instead of real VideoView!
            LaunchedEffect(track.id) {
                dur.value = if (track.duration > 0) track.duration.toInt() else 180000
                val startPos = if (AudioEngine.currentTrackId.value == track.id) {
                    val sp = AudioEngine.currentPosition.value
                    AudioEngine.pause()
                    sp
                } else 0
                pos.value = startPos
            }
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = videoZoom
                        scaleY = videoZoom
                    }
            ) {
                // Background dark vortex
                drawCircle(colors.primary.copy(alpha = 0.08f), radius = size.width * 0.4f)
                drawCircle(colors.secondary.copy(alpha = 0.05f), radius = size.width * 0.25f)
                
                // Rotative line representing frame scanner
                val t = System.currentTimeMillis() / 450.0
                val angleRad = t.toFloat()
                val endX = size.width / 2f + size.width * 0.5f * kotlin.math.cos(angleRad)
                val endY = size.height / 2f + size.width * 0.5f * kotlin.math.sin(angleRad)
                drawLine(colors.primary.copy(alpha = 0.4f), Offset(size.width / 2, size.height / 2), Offset(endX, endY), strokeWidth = 5f)
            }
            Text(
                "SIMULACION VIDEO: RENDERING HOLOGRÁFICO",
                color = colors.primary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(bottom = 80.dp),
                fontWeight = FontWeight.Bold
            )
        } else {
            // Real physical VideoView keyed by track ID to reconstruct cleanly on swap
            key(track.id) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(Uri.parse(track.filePath))
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                dur.value = duration
                                if (AudioEngine.currentTrackId.value == track.id) {
                                    val startPos = AudioEngine.currentPosition.value
                                    seekTo(startPos)
                                    pos.value = startPos
                                    AudioEngine.pause()
                                }
                                start()
                                isPlayingVideo.value = true
                                isPausedByUser.value = false
                            }
                            videoViewInstance = this
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = videoZoom
                            scaleY = videoZoom
                        }
                )
            }
        }

        AnimatedVisibility(
            visible = isControlsVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top control bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(track.displayTitle, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.displayArtist, style = MaterialTheme.typography.bodySmall, color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    // Share Button
                    IconButton(onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, track.displayTitle)
                                putExtra(Intent.EXTRA_TEXT, "Mira este video compartido de VibPlay: ${track.displayTitle} (${track.filePath})")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Compartir Video"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "No se pudo compartir el video", Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Icon(Icons.Rounded.Share, contentDescription = "Compartir", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Background Play Button
                    IconButton(onClick = {
                        val currentPos = if (track.filePath.startsWith("/simulated/")) pos.value else {
                            videoViewInstance?.currentPosition ?: pos.value
                        }
                        viewModel.playVideoAsBackgroundAudio(track, currentPos)
                        onClose()
                        Toast.makeText(context, "Audio en segundo plano activado", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(
                            imageVector = Icons.Rounded.Headset,
                            contentDescription = "Segundo plano",
                            tint = colors.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = {
                        videoZoom = if (videoZoom >= 5f) 1f else (videoZoom + 1f).coerceIn(1f, 5f)
                    }) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${String.format("%.1f", videoZoom)}x",
                                color = colors.primary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Center seek dials with skip previous and skip next video support!
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Previous video
                    IconButton(onClick = {
                        val allVideoTracks = viewModel.allTracks.value.filter { it.isVideo }
                        if (allVideoTracks.isNotEmpty()) {
                            val idx = allVideoTracks.indexOfFirst { it.id == track.id }
                            val prevIdx = if (idx > 0) idx - 1 else allVideoTracks.size - 1
                            onTrackChange(allVideoTracks[prevIdx])
                        }
                    }) {
                        TinyIcon(Icons.Rounded.SkipPrevious, contentDescription = "Video Anterior", size = 32.dp, tint = Color.White)
                    }

                    // Seek backward 10s
                    IconButton(onClick = {
                        if (track.filePath.startsWith("/simulated/")) {
                            pos.value = (pos.value - 10000).coerceAtLeast(0)
                        } else {
                            videoViewInstance?.let { vv ->
                                val nP = (vv.currentPosition - 10000).coerceAtLeast(0)
                                vv.seekTo(nP)
                                pos.value = nP
                            }
                        }
                    }) {
                        TinyIcon(Icons.Rounded.Replay10, contentDescription = "Retroceder 10s", size = 32.dp, tint = Color.White)
                    }

                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(colors.primary)
                            .clickable {
                                if (track.filePath.startsWith("/simulated/")) {
                                    isPlayingVideo.value = !isPlayingVideo.value
                                    isPausedByUser.value = !isPlayingVideo.value
                                } else {
                                    videoViewInstance?.let { vv ->
                                        if (vv.isPlaying) {
                                            vv.pause()
                                            isPlayingVideo.value = false
                                            isPausedByUser.value = true
                                        } else {
                                            vv.start()
                                            isPlayingVideo.value = true
                                            isPausedByUser.value = false
                                        }
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        TinyIcon(
                            imageVector = if (isPlayingVideo.value) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Reproducir",
                            size = 32.dp,
                            tint = Color.Black
                        )
                    }

                    // Seek forward 10s
                    IconButton(onClick = {
                        if (track.filePath.startsWith("/simulated/")) {
                            pos.value = (pos.value + 10000).coerceAtMost(dur.value)
                        } else {
                            videoViewInstance?.let { vv ->
                                val nP = (vv.currentPosition + 10000).coerceAtMost(vv.duration)
                                vv.seekTo(nP)
                                pos.value = nP
                            }
                        }
                    }) {
                        TinyIcon(Icons.Rounded.Forward10, contentDescription = "Adelantar 10s", size = 32.dp, tint = Color.White)
                    }

                    // Next video
                    IconButton(onClick = {
                        val allVideoTracks = viewModel.allTracks.value.filter { it.isVideo }
                        if (allVideoTracks.isNotEmpty()) {
                            val idx = allVideoTracks.indexOfFirst { it.id == track.id }
                            val nextIdx = if (idx != -1 && idx < allVideoTracks.size - 1) idx + 1 else 0
                            onTrackChange(allVideoTracks[nextIdx])
                        }
                    }) {
                        TinyIcon(Icons.Rounded.SkipNext, contentDescription = "Siguiente Video", size = 32.dp, tint = Color.White)
                    }
                }

                // Bottom Seek Controllers & Timers
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                        .align(Alignment.BottomCenter)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(pos.value),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = formatTime(dur.value),
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Slider(
                        value = if (dur.value > 0) pos.value.toFloat() / dur.value else 0f,
                        onValueChange = { scale ->
                            val targetPos = (scale * dur.value).toInt()
                            pos.value = targetPos
                            if (!track.filePath.startsWith("/simulated/")) {
                                videoViewInstance?.seekTo(targetPos)
                            }
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun ThemeBackground3D(theme: ThemeStyle, content: @Composable () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_3d")
    val orbitX1 by infiniteTransition.animateFloat(
        initialValue = -100f,
        targetValue = 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x1"
    )
    val orbitY1 by infiniteTransition.animateFloat(
        initialValue = -150f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y1"
    )
    val orbitX2 by infiniteTransition.animateFloat(
        initialValue = 150f,
        targetValue = -150f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "x2"
    )
    val orbitY2 by infiniteTransition.animateFloat(
        initialValue = 100f,
        targetValue = -100f,
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "y2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val w = size.width
                val h = size.height
                
                // Draw base rich gradients
                when (theme) {
                    ThemeStyle.CYBERPUNK -> {
                        // Cyberpunk Neon Depth
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF16022F), Color(0xFF030008))))
                        // Glow 1
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(CyberPrimary.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(w / 3f + orbitX1, h / 4f + orbitY1),
                                radius = w * 0.7f
                            ),
                            radius = w * 0.7f,
                            center = Offset(w / 3f + orbitX1, h / 4f + orbitY1)
                        )
                        // Glow 2
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(CyberSecondary.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(w * 0.8f + orbitX2, h * 0.7f + orbitY2),
                                radius = w * 0.8f
                            ),
                            radius = w * 0.8f,
                            center = Offset(w * 0.8f + orbitX2, h * 0.7f + orbitY2)
                        )
                        // Glow 3
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(CyberAccent.copy(alpha = 0.15f), Color.Transparent),
                                center = Offset(w / 2f + orbitX2 * 0.5f, h / 2f + orbitY1 * 0.5f),
                                radius = w * 0.5f
                            ),
                            radius = w * 0.5f,
                            center = Offset(w / 2f + orbitX2 * 0.5f, h / 2f + orbitY1 * 0.5f)
                        )
                        // 3D perspective Grid lines
                        val gridColor = CyberPrimary.copy(alpha = 0.08f)
                        val vY = h * 0.4f
                        var curY = vY
                        var step = 10f
                        while (curY < h) {
                            drawLine(gridColor, Offset(0f, curY), Offset(w, curY), strokeWidth = 2.5f)
                            step *= 1.25f
                            curY += step
                        }
                        val numL = 12
                        val cX = w / 2f
                        for (i in -numL..numL) {
                            val endX = cX + i * (w / (numL * 1.5f))
                            drawLine(gridColor, Offset(cX, vY), Offset(endX, h), strokeWidth = 1.8f)
                        }
                    }
                    ThemeStyle.LUXURY -> {
                        // Luxury Obsidian, Royal Velvet Blue and Gold depth
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF0D0B1C), Color(0xFF030307))))
                        // Soft Gold Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(LuxuryPrimary.copy(alpha = 0.28f), Color.Transparent),
                                center = Offset(w / 2f + orbitX1 * 1.1f, h / 3f + orbitY1 * 1.1f),
                                radius = w * 0.7f
                            ),
                            radius = w * 0.7f,
                            center = Offset(w / 2f + orbitX1 * 1.1f, h / 3f + orbitY1 * 1.1f)
                        )
                        // Champagne Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(LuxurySecondary.copy(alpha = 0.20f), Color.Transparent),
                                center = Offset(w * 0.8f + orbitX2, h * 0.7f + orbitY2),
                                radius = w * 0.8f
                            ),
                            radius = w * 0.8f,
                            center = Offset(w * 0.8f + orbitX2, h * 0.7f + orbitY2)
                        )
                        // Radiant Accent Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(LuxuryAccent.copy(alpha = 0.18f), Color.Transparent),
                                center = Offset(w * 0.2f + orbitX1 * 0.5f, h * 0.8f + orbitY2 * 0.5f),
                                radius = w * 0.5f
                            ),
                            radius = w * 0.5f,
                            center = Offset(w * 0.2f + orbitX1 * 0.5f, h * 0.8f + orbitY2 * 0.5f)
                        )
                        
                        // 3D Metallic golden concentric rings
                        val premiumColor1 = LuxuryPrimary.copy(alpha = 0.08f)
                        val premiumColor2 = LuxuryAccent.copy(alpha = 0.06f)
                        drawCircle(premiumColor1, radius = w * 0.4f, center = Offset(w/2f, h/3f), style = Stroke(4f))
                        drawCircle(premiumColor2, radius = w * 0.65f, center = Offset(w/2f, h/3f), style = Stroke(5f))
                        drawCircle(premiumColor1, radius = w * 0.9f, center = Offset(w/2f, h/3f), style = Stroke(3f))

                        // Luxury Diamond patterned grid
                        val goldPattern = LuxuryPrimary.copy(alpha = 0.03f)
                        val dSize = 100f
                        for (x in 0..(w / dSize).toInt() + 1) {
                            for (y in 0..(h / dSize).toInt() + 2) {
                                val px = x * dSize + (if (y % 2 == 0) dSize / 2f else 0f)
                                val py = y * dSize
                                drawCircle(goldPattern, radius = 18f, center = Offset(px + orbitX1 * 0.12f, py + orbitY1 * 0.12f), style = Stroke(1.5f))
                            }
                        }

                        // Floating Golden Stars / Sparks
                        val sparks = listOf(
                            Offset(w * 0.25f + orbitX1 * 0.4f, h * 0.4f + orbitY1 * 0.5f),
                            Offset(w * 0.70f + orbitX2 * 0.3f, h * 0.25f + orbitY2 * 0.4f),
                            Offset(w * 0.15f + orbitX2 * 0.5f, h * 0.75f + orbitY1 * 0.6f),
                            Offset(w * 0.85f + orbitX1 * 0.3f, h * 0.85f + orbitY2 * 0.5f),
                            Offset(w * 0.50f + orbitX1 * 0.2f, h * 0.60f + orbitY1 * 0.3f)
                        )
                        sparks.forEach { spk ->
                            drawCircle(Color.White.copy(alpha = 0.7f), radius = 3f, center = spk)
                            drawCircle(LuxuryPrimary.copy(alpha = 0.4f), radius = 9f, center = spk)
                        }
                    }
                    ThemeStyle.LAVA -> {
                        // Lava core and burning volcanic ash
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF1E0202), Color(0xFF020000))))
                        // Deep Magma Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(LavaPrimary.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(w / 2f + orbitX1, h * 0.75f + orbitY2),
                                radius = w * 0.9f
                            ),
                            radius = w * 0.9f,
                            center = Offset(w / 2f + orbitX1, h * 0.75f + orbitY2)
                        )
                        // Lava Yellow Flame Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(LavaSecondary.copy(alpha = 0.16f), Color.Transparent),
                                center = Offset(w * 0.2f + orbitX2, h * 0.4f + orbitY1),
                                radius = w * 0.6f
                            ),
                            radius = w * 0.6f,
                            center = Offset(w * 0.2f + orbitX2, h * 0.4f + orbitY1)
                        )

                        // Volcanic Magma waves
                        val magmaPath = Path().apply {
                            moveTo(0f, h)
                            lineTo(0f, h * 0.8f + orbitY1 * 0.15f)
                            cubicTo(
                                w * 0.35f, h * 0.72f + orbitY2 * 0.2f,
                                w * 0.65f, h * 0.88f + orbitX1 * 0.15f,
                                w, h * 0.78f + orbitY1 * 0.1f
                            )
                            lineTo(w, h)
                            close()
                        }
                        drawPath(magmaPath, Brush.verticalGradient(listOf(LavaPrimary.copy(alpha = 0.25f), Color.Transparent)))

                        // 3D lava grid
                        val stripesColor = LavaAccent.copy(alpha = 0.04f)
                        val magmaGridStep = 100f
                        for (y in 0..(h / magmaGridStep).toInt() + 1) {
                            val ratio = (y * magmaGridStep) / h
                            val intensity = 1f - ratio
                            drawLine(
                                color = stripesColor.copy(alpha = stripesColor.alpha * intensity),
                                start = Offset(0f, y * magmaGridStep),
                                end = Offset(w, y * magmaGridStep),
                                strokeWidth = 3f
                            )
                        }

                        // Floating Embers rising
                        val embers = listOf(
                            Offset(w * 0.15f + orbitX1 * 0.3f, h * 0.65f + orbitY1 * 0.5f),
                            Offset(w * 0.45f + orbitX2 * 0.2f, h * 0.45f + orbitY2 * 0.4f),
                            Offset(w * 0.75f + orbitX1 * 0.4f, h * 0.55f + orbitY1 * 0.3f),
                            Offset(w * 0.90f + orbitX2 * 0.1f, h * 0.35f + orbitY2 * 0.5f),
                            Offset(w * 0.60f + orbitX1 * 0.25f, h * 0.75f + orbitY1 * 0.4f)
                        )
                        embers.forEach { emb ->
                            drawCircle(LavaSecondary.copy(alpha = 0.5f), radius = 12f, center = emb)
                            drawCircle(LavaPrimary, radius = 5f, center = emb)
                        }
                    }
                    ThemeStyle.TOXIC -> {
                        // Radioactive Toxic Chambers
                        drawRect(Brush.verticalGradient(listOf(Color(0xFF030B04), Color(0xFF000100))))
                        // Acid Green Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(ToxicPrimary.copy(alpha = 0.30f), Color.Transparent),
                                center = Offset(w * 0.3f + orbitX1 * 1.2f, h * 0.3f + orbitY1 * 1.2f),
                                radius = w * 0.8f
                            ),
                            radius = w * 0.8f,
                            center = Offset(w * 0.3f + orbitX1 * 1.2f, h * 0.3f + orbitY1 * 1.2f)
                        )
                        // Acid Yellow/Radiactive Orange Glow
                        drawCircle(
                            Brush.radialGradient(
                                colors = listOf(ToxicSecondary.copy(alpha = 0.22f), Color.Transparent),
                                center = Offset(w * 0.75f + orbitX2, h * 0.75f + orbitY2),
                                radius = w * 0.7f
                            ),
                            radius = w * 0.7f,
                            center = Offset(w * 0.75f + orbitX2, h * 0.75f + orbitY2)
                        )

                        // Hexagonal biohazard grid
                        val hexC = ToxicPrimary.copy(alpha = 0.06f)
                        val side = 45f
                        val hH = side * 2f
                        val hW = Math.sqrt(3.0).toFloat().toFloat() * side
                        for (row in 0..(h / (hH * 0.75f)).toInt() + 1) {
                            for (col in 0..(w / hW).toInt() + 1) {
                                val cx = col * hW + (if (row % 2 == 1) hW / 2f else 0f)
                                val cy = row * hH * 0.75f + orbitY1 * 0.03f
                                val hex = Path().apply {
                                    for (i in 0..5) {
                                        val rad = Math.toRadians((i * 60 - 30).toDouble()).toFloat()
                                        val px = cx + side * kotlin.math.cos(rad)
                                        val py = cy + side * kotlin.math.sin(rad)
                                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                                    }
                                    close()
                                }
                                drawPath(hex, hexC, style = Stroke(1.8f))
                            }
                        }

                        // Radioactive Bubbles and Mutagen particles rising
                        val bubbles = listOf(
                            Offset(w * 0.20f + orbitX1 * 0.3f, h * 0.70f + orbitY2 * 0.5f),
                            Offset(w * 0.55f + orbitX2 * 0.4f, h * 0.50f + orbitY1 * 0.4f),
                            Offset(w * 0.80f + orbitX1 * 0.35f, h * 0.65f + orbitY1 * 0.6f),
                            Offset(w * 0.35f + orbitX2 * 0.25f, h * 0.30f + orbitY2 * 0.3f),
                            Offset(w * 0.90f + orbitX1 * 0.45f, h * 0.40f + orbitY2 * 0.5f)
                        )
                        bubbles.forEach { bbl ->
                            drawCircle(ToxicPrimary.copy(alpha = 0.45f), radius = 14f, center = bbl, style = Stroke(1.5f))
                            drawCircle(ToxicSecondary.copy(alpha = 0.25f), radius = 6f, center = bbl)
                        }

                        // Danger Haz stripes aligned in perspective at bottom
                        val stripeC = ToxicPrimary.copy(alpha = 0.22f)
                        val stH = 22f
                        var xVal = -40f
                        while (xVal < w + 80f) {
                            drawLine(
                                stripeC,
                                Offset(xVal + orbitX1 * 0.05f, h),
                                Offset(xVal - stH + orbitX1 * 0.05f, h - stH),
                                strokeWidth = 6.5f
                            )
                            xVal += 28f
                        }
                    }
                }
            }
    ) {
        val visualScrim = if (theme == ThemeStyle.LUXURY) Color.Black.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.10f)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(visualScrim)
        ) {
            content()
        }
    }
}

@Composable
fun AddManualTrackDialog(
    colors: ThemeColors,
    onDismiss: () -> Unit,
    onAdd: (String, String, String, Boolean, String, String?, Boolean) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var path by remember { mutableStateOf("") }
    var isVideo by remember { mutableStateOf(false) }
    var folder by remember { mutableStateOf("Manual Beats") }
    var artUri by remember { mutableStateOf("") }
    var isEnglish by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            artUri = copyUriToLocalStorage(context, uri, "manual_art")
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        title = { Text("Añadir Canción / Video", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text("Artista") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = path,
                    onValueChange = { path = it },
                    label = { Text("Ruta o URL del Archivo") },
                    placeholder = { Text("Ej: /ruta/audio.opus o https://site.com/video.mkv") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = folder,
                    onValueChange = { folder = it },
                    label = { Text("Carpeta / Categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                        focusedLabelColor = colors.primary,
                        unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                        cursorColor = colors.primary
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = artUri,
                        onValueChange = { artUri = it },
                        label = { Text("Carátula (URL o Ruta)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                            focusedLabelColor = colors.primary,
                            unfocusedLabelColor = Color.White.copy(alpha = 0.5f),
                            cursorColor = colors.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = colors.primary.copy(alpha = 0.2f), contentColor = colors.primary)
                    ) {
                        Icon(Icons.Rounded.PhotoLibrary, contentDescription = "Galería")
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isVideo,
                        onCheckedChange = { isVideo = it },
                        colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                    )
                    Text("Es un archivo de Video (soporta cualquier formato existente)", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && path.isNotBlank()) {
                        onAdd(title, artist, path, isVideo, folder, if (artUri.isBlank()) null else artUri, false)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = Color.Black)
            ) {
                Text("Añadir")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
