package com.boost.your.srt.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.boost.your.srt.MainActivity
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentCyanDim
import com.boost.your.srt.ui.theme.AccentGreen
import com.boost.your.srt.ui.theme.AccentRed
import com.boost.your.srt.ui.theme.BgCard
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BoostMasterTheme
import com.boost.your.srt.ui.theme.BorderColor
import com.boost.your.srt.ui.theme.DisplayFont
import com.boost.your.srt.ui.theme.TextMuted
import com.boost.your.srt.ui.theme.TextPrimary
import com.boost.your.srt.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.random.Random

class LoginActivity : ComponentActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BoostMasterTheme {
                LoginScreen(
                    viewModel = viewModel,
                    onOpenFreeKey = {
                        runCatching {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://cheats.xo.je")))
                        }
                    },
                    onAuthenticated = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onOpenFreeKey: () -> Unit,
    onAuthenticated: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val keyText by viewModel.keyInput.collectAsStateWithLifecycle()

    val isError = state is LoginUiState.Error
    val isSuccess = state is LoginUiState.Success
    val isBusy = state is LoginUiState.Verifying || state is LoginUiState.CheckingSaved

    // Shake on every new error
    val shake = remember { Animatable(0f) }
    LaunchedEffect(state) {
        if (state is LoginUiState.Error) {
            for (dx in listOf(-18f, 16f, -12f, 10f, -6f, 4f, 0f)) {
                shake.animateTo(dx, tween(45))
            }
        }
    }

    // Navigate 1s after success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            delay(1000)
            onAuthenticated()
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BgPrimary)
    ) {
        CyberBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                "BOOST",
                color = AccentCyan,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Black,
                fontSize = 46.sp,
                letterSpacing = 8.sp
            )
            Text(
                "MASTER",
                color = Color.White,
                fontFamily = DisplayFont,
                fontWeight = FontWeight.Black,
                fontSize = 34.sp,
                letterSpacing = 12.sp
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "GAMING PERFORMANCE SUITE",
                color = AccentAmber,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp
            )

            Spacer(Modifier.height(40.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { translationX = shake.value }
            ) {
                KeyInputCard(
                    value = keyText,
                    onValueChange = viewModel::onKeyChange,
                    isError = isError,
                    enabled = !isBusy && !isSuccess,
                    onDone = { viewModel.login() }
                )

                if (state is LoginUiState.Error) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        (state as LoginUiState.Error).message,
                        color = AccentRed,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (state is LoginUiState.CheckingSaved) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Verifying saved license…",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(22.dp))

            ActivateButton(
                loading = isBusy,
                enabled = !isBusy && !isSuccess,
                onClick = { viewModel.login() }
            )

            Spacer(Modifier.height(22.dp))

            HwidRow(hwid = viewModel.hwid, context = context)

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onOpenFreeKey) {
                Text("Get Free Key →", color = AccentCyan, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }

        if (isSuccess) SuccessOverlay()
    }
}

@Composable
private fun CyberBackground() {
    val transition = rememberInfiniteTransition(label = "bg")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(24_000, easing = LinearEasing)),
        label = "ring"
    )
    // Deterministic circuit layout
    val nodes = remember {
        val rnd = Random(7)
        List(46) { Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextInt(3)) }
    }

    Canvas(Modifier.fillMaxSize()) {
        val grid = 44.dp.toPx()
        val line = Color(0xFF00C8FF).copy(alpha = 0.05f)
        var x = 0f
        while (x < size.width) {
            drawLine(line, Offset(x, 0f), Offset(x, size.height), 1f)
            x += grid
        }
        var y = 0f
        while (y < size.height) {
            drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
            y += grid
        }
        // Traces + solder pads
        nodes.forEach { (nx, ny, kind) ->
            val px = (nx * (size.width / grid)).toInt() * grid
            val py = (ny * (size.height / grid)).toInt() * grid
            val trace = Color(0xFF00C8FF).copy(alpha = 0.13f)
            when (kind) {
                0 -> drawLine(trace, Offset(px, py), Offset(px + grid * 2, py), 2f)
                1 -> drawLine(trace, Offset(px, py), Offset(px, py + grid * 2), 2f)
                else -> {
                    drawLine(trace, Offset(px, py), Offset(px + grid, py), 2f)
                    drawLine(trace, Offset(px + grid, py), Offset(px + grid, py + grid), 2f)
                }
            }
            drawCircle(trace.copy(alpha = 0.22f), radius = 3.5f, center = Offset(px, py))
        }

        // Slowly rotating cyan ring
        val c = Offset(size.width / 2f, size.height * 0.36f)
        val r = size.minDimension * 0.62f
        rotate(angle, c) {
            drawCircle(
                color = AccentCyan.copy(alpha = 0.10f),
                radius = r,
                center = c,
                style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(28f, 22f)))
            )
            drawArc(
                brush = Brush.sweepGradient(
                    listOf(Color.Transparent, AccentCyan.copy(alpha = 0.55f), Color.Transparent),
                    center = c
                ),
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        drawCircle(
            color = AccentCyan.copy(alpha = 0.05f),
            radius = r * 0.82f,
            center = c,
            style = Stroke(1.dp.toPx())
        )
    }
}

@Composable
private fun KeyInputCard(
    value: String,
    onValueChange: (String) -> Unit,
    isError: Boolean,
    enabled: Boolean,
    onDone: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val validFormat = LoginViewModel.KEY_REGEX.matches(value)

    val borderColor by animateColorAsState(
        when {
            isError -> AccentRed
            validFormat -> AccentGreen
            focused -> AccentCyan
            else -> BorderColor
        },
        tween(250), label = "border"
    )

    val glow = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glow.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "glowA"
    )
    val glowColor = if (validFormat && !isError) AccentGreen else borderColor

    val prefix = detectPrefix(value)

    Column(
        Modifier
            .fillMaxWidth()
            .then(
                if (validFormat && !isError)
                    Modifier.shadow(14.dp * glowAlpha, RoundedCornerShape(16.dp), ambientColor = glowColor, spotColor = glowColor)
                else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(BgCard.copy(alpha = 0.85f))
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("LICENSE KEY", color = TextSecondary, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.weight(1f))
            if (prefix != null) {
                Text(
                    prefix,
                    color = BgPrimary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (validFormat) AccentGreen else AccentAmber)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            interactionSource = interaction,
            textStyle = TextStyle(
                color = TextPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            ),
            cursorBrush = SolidColor(AccentCyan),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrect = false,
                imeAction = androidx.compose.ui.text.input.ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onDone() }),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text(
                            "SRT_XXXXXXXX",
                            color = TextMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 20.sp,
                            letterSpacing = 2.sp
                        )
                    }
                    inner()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun detectPrefix(value: String): String? {
    val head = value.substringBefore('_', missingDelimiterValue = value)
    LoginViewModel.PREFIXES.firstOrNull { it == head }?.let { return it }
    if (value.isNotEmpty()) {
        LoginViewModel.PREFIXES.firstOrNull { it.startsWith(value) || value.startsWith(it) }?.let { return it }
    }
    return null
}

@Composable
private fun ActivateButton(loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val progress by shimmer.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "shimmerX"
    )
    val spin by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "spin"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(listOf(AccentCyanDim, AccentCyan)))
            .clickable(enabled = enabled, onClick = onClick)
            .drawBehind {
                val w = size.width
                val x = progress * w
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.35f), Color.Transparent),
                        startX = x - w * 0.2f,
                        endX = x + w * 0.2f
                    )
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (loading) {
            Canvas(Modifier.size(26.dp)) {
                rotate(spin) {
                    drawArc(
                        color = BgPrimary,
                        startAngle = 0f,
                        sweepAngle = 270f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }
        } else {
            Text("ACTIVATE", color = BgPrimary, fontWeight = FontWeight.Black, fontSize = 16.sp, letterSpacing = 4.sp)
        }
    }
}

@Composable
private fun HwidRow(hwid: String, context: Context) {
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1500)
            copied = false
        }
    }
    val masked = if (hwid.length > 8) hwid.take(4) + "..." + hwid.takeLast(4) else hwid
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Device ID: $masked",
            color = TextSecondary,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp
        )
        IconButton(onClick = {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("device_id", hwid))
            copied = true
        }) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy device ID", tint = AccentCyan, modifier = Modifier.size(18.dp))
        }
        if (copied) Text("Copied", color = AccentGreen, fontSize = 11.sp)
    }
}

@Composable
private fun SuccessOverlay() {
    val progress = remember { Animatable(0f) }
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, tween(300))
        progress.animateTo(1f, tween(500))
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(BgPrimary.copy(alpha = 0.88f)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(
                Modifier
                    .size(120.dp)
                    .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
            ) {
                drawCircle(AccentGreen.copy(alpha = 0.15f), radius = size.minDimension / 2f)
                drawCircle(AccentGreen, radius = size.minDimension / 2f - 4f, style = Stroke(5.dp.toPx()))
                val path = Path().apply {
                    moveTo(size.width * 0.28f, size.height * 0.52f)
                    lineTo(size.width * 0.44f, size.height * 0.68f)
                    lineTo(size.width * 0.74f, size.height * 0.34f)
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val partial = Path()
                measure.getSegment(0f, measure.length * progress.value, partial, true)
                drawPath(partial, AccentGreen, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
            }
            Spacer(Modifier.height(16.dp))
            Text("LICENSE ACTIVATED", color = AccentGreen, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        }
    }
}
