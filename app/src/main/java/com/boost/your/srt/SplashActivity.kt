package com.boost.your.srt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.boost.your.srt.auth.KeyRepository
import com.boost.your.srt.auth.LoginActivity
import com.boost.your.srt.ui.theme.AccentAmber
import com.boost.your.srt.ui.theme.AccentCyan
import com.boost.your.srt.ui.theme.AccentPurple
import com.boost.your.srt.ui.theme.BgPrimary
import com.boost.your.srt.ui.theme.BoostMasterTheme
import com.boost.your.srt.ui.theme.DisplayFont
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { BoostMasterTheme { SplashScreen() } }

        lifecycleScope.launch {
            // Run the (encrypted-prefs) key check while the 1.5s animation plays.
            val valid = async(Dispatchers.IO) { KeyRepository(this@SplashActivity).isKeyLocallyValid() }
            delay(1500)
            val target = if (withContext(Dispatchers.Default) { valid.await() }) MainActivity::class.java
            else LoginActivity::class.java
            startActivity(Intent(this@SplashActivity, target))
            finish()
        }
    }
}

@Composable
private fun SplashScreen() {
    val transition = rememberInfiniteTransition(label = "splash")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(2400, easing = LinearEasing)),
        label = "angle"
    )
    val pulse by transition.animateFloat(
        0.55f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "pulse"
    )

    Box(Modifier.fillMaxSize().background(BgPrimary), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(170.dp)) {
                    val c = center
                    val r = size.minDimension / 2f - 8.dp.toPx()
                    drawCircle(AccentCyan.copy(alpha = 0.10f * pulse), radius = r + 6.dp.toPx())
                    rotate(angle, c) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, AccentCyan, AccentPurple, Color.Transparent),
                                center = c
                            ),
                            startAngle = 0f,
                            sweepAngle = 250f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r),
                            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
                            style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    drawCircle(AccentCyan.copy(alpha = 0.35f * pulse), radius = r - 14.dp.toPx(), style = Stroke(1.5.dp.toPx()))
                }
                Text("⚡", fontSize = 56.sp, color = AccentAmber)
            }
            Spacer(Modifier.height(28.dp))
            Text("BOOST", color = AccentCyan, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 40.sp, letterSpacing = 8.sp)
            Text("MASTER", color = Color.White, fontFamily = DisplayFont, fontWeight = FontWeight.Black, fontSize = 30.sp, letterSpacing = 12.sp)
        }
    }
}
