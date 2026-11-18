package com.praveen.hypertrophy.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.praveen.hypertrophy.fmt

@Composable
fun RestTimerHalo(
    remSec: Int,
    totSec: Int,
    onAdjust: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val total = if (totSec <= 0) 60 else totSec
    val progress = (remSec.toFloat() / total.toFloat()).coerceIn(0f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = progress * 360f,
        animationSpec = tween(durationMillis = 300),
        label = "haloSweep"
    )

    val voltLime = Color(0xFFE2F952)
    val haloTrack = MaterialTheme.colorScheme.surfaceContainerHighest
    val activeStroke = if (remSec <= 10) Color(0xFFFF5555) else voltLime

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
            Canvas(modifier = Modifier.size(180.dp)) {
                // Background Track
                drawCircle(
                    color = haloTrack,
                    radius = size.minDimension / 2f,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
                // Active Countdown Arc
                drawArc(
                    color = activeStroke,
                    startAngle = -90f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = fmt(remSec),
                    style = MaterialTheme.typography.displayMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "RECOVERING",
                    style = MaterialTheme.typography.labelSmall,
                    color = activeStroke,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Tactile Quick-Time Steppers (48dp min targets)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { onAdjust(-15) }) { Text("-15s") }
            FilledTonalButton(onClick = { onAdjust(15) }) { Text("+15s") }
            FilledTonalButton(onClick = { onAdjust(30) }) { Text("+30s") }
            OutlinedButton(onClick = onSkip) { Text("Skip") }
        }
    }
}
