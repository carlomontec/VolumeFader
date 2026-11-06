package com.carlomontec.volumefader.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.carlomontec.volumefader.audio.FadeCurve
import com.carlomontec.volumefader.model.FadeStatus
import com.carlomontec.volumefader.service.VolumeFadeService
import com.carlomontec.volumefader.ui.theme.AccentRed
import com.carlomontec.volumefader.ui.theme.AccentTeal
import com.carlomontec.volumefader.ui.theme.CardDark
import com.carlomontec.volumefader.ui.theme.DeepBlack
import com.carlomontec.volumefader.ui.theme.SurfaceDark
import com.carlomontec.volumefader.ui.theme.TextPrimary
import com.carlomontec.volumefader.ui.theme.TextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MainFaderScreen(
    onStartFade: (durationMinutes: Int, curve: FadeCurve, pauseOnFinish: Boolean) -> Unit,
    onStopFade: () -> Unit
) {
    val status by VolumeFadeService.statusFlow.collectAsState()

    var durationMinutes by remember { mutableFloatStateOf(30f) }
    var selectedCurve by remember { mutableStateOf(FadeCurve.LOGARITHMIC) }
    var pauseOnFinish by remember { mutableStateOf(true) }

    val isRunning = status is FadeStatus.Running
    val runningStatus = status as? FadeStatus.Running

    Column(
        modifier = compColumnModifier(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = null,
                tint = AccentTeal,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "VolumeFader",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Acoustic sleep timer",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        // Active Timer Display
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isRunning && runningStatus != null) {
                    val remainingSeconds = (runningStatus.remainingMs / 1000).coerceAtLeast(0)
                    val mins = remainingSeconds / 60
                    val secs = remainingSeconds % 60

                    Text(
                        text = String.format("%02d:%02d", mins, secs),
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = AccentTeal
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Volume: ${runningStatus.currentVolumeStep} / ${runningStatus.maxVolumeStep}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = {
                            val total = runningStatus.totalDurationMs.toFloat()
                            if (total > 0) (runningStatus.remainingMs.toFloat() / total).coerceIn(0f, 1f) else 0f
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = AccentTeal,
                        trackColor = CardDark,
                    )
                } else {
                    Text(
                        text = "${durationMinutes.toInt()} min",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Light,
                        fontFamily = FontFamily.Monospace,
                        color = TextPrimary
                    )
                    Text(
                        text = "Fade duration",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (!isRunning) {
            // Preset Pills
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(15, 30, 45, 60).forEach { mins ->
                    val isSelected = durationMinutes.toInt() == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = { durationMinutes = mins.toFloat() },
                        label = { Text("${mins}m") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                            selectedLabelColor = AccentTeal
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Duration Slider
            Slider(
                value = durationMinutes,
                onValueChange = { durationMinutes = it },
                valueRange = 1f..120f,
                steps = 118,
                colors = SliderDefaults.colors(
                    thumbColor = AccentTeal,
                    activeTrackColor = AccentTeal,
                    inactiveTrackColor = CardDark
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Fade Curve Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Fade Characteristic",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FadeCurve.entries.forEach { curve ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedCurve == curve) CardDark else SurfaceDark)
                                .clickable { selectedCurve = curve }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = curve.displayName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = if (selectedCurve == curve) AccentTeal else TextPrimary
                                )
                                Text(
                                    text = curve.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action on Finish: Pause Media
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicOff,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Pause Media On Zero",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Text(
                                text = "Stop music playback when timer concludes",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                    Switch(
                        checked = pauseOnFinish,
                        onCheckedChange = { pauseOnFinish = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AccentTeal,
                            checkedTrackColor = AccentTeal.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f, fill = false))
        Spacer(modifier = Modifier.height(28.dp))

        // Main CTA Button
        if (isRunning) {
            Button(
                onClick = onStopFade,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Cancel Fade",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
        } else {
            Button(
                onClick = {
                    onStartFade(durationMinutes.toInt(), selectedCurve, pauseOnFinish)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = DeepBlack)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Start Sleep Timer",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlack
                )
            }
        }
    }
}

@Composable
private fun compColumnModifier(): Modifier {
    return Modifier
        .fillMaxSize()
        .background(DeepBlack)
        .padding(horizontal = 24.dp, vertical = 32.dp)
        .verticalScroll(rememberScrollState())
}
