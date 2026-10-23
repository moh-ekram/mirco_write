package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainViewModel
import com.example.ui.theme.extendedColors

@Composable
fun SectionTimerControl(
    viewModel: MainViewModel,
    initialSeconds: Int,
    modifier: Modifier = Modifier,
    onTimerFinished: () -> Unit = {}
) {
    if (initialSeconds <= 0) return

    val haptic = LocalHapticFeedback.current

    LaunchedEffect(initialSeconds) {
        viewModel.initTimer(initialSeconds)
    }

    val remainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val totalSeconds by viewModel.timerTotalSeconds.collectAsState()
    val isRunning by viewModel.timerIsRunning.collectAsState()
    val isFinished by viewModel.timerIsFinished.collectAsState()
    val focusLockEnabled by viewModel.focusLockEnabled.collectAsState()

    // Trigger soft haptic feedback when timer finishes
    LaunchedEffect(isFinished) {
        if (isFinished) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onTimerFinished()
        }
    }

    val minutes = remainingSeconds / 60
    val seconds = remainingSeconds % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)

    val progress = if (totalSeconds > 0) {
        (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val isLastMinute = isRunning && remainingSeconds in 1..60

    val extColors = MaterialTheme.extendedColors
    val ringColor = when {
        isFinished -> extColors.danger
        isLastMinute -> extColors.warning
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Timer",
                tint = ringColor,
                modifier = Modifier.size(20.dp)
            )

            Text(
                text = formattedTime,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isFinished) extColors.danger else MaterialTheme.colorScheme.onSurface
            )

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape),
                color = ringColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            IconButton(
                onClick = { if (isRunning) viewModel.pauseTimer() else viewModel.startTimer() },
                modifier = Modifier
                    .size(34.dp)
                    .testTag(if (isRunning) "timer_pause_button" else "timer_start_button")
            ) {
                Icon(
                    imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pause" else "Start",
                    tint = if (isRunning) extColors.warning else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(
                onClick = { viewModel.resetTimer(initialSeconds) },
                modifier = Modifier
                    .size(34.dp)
                    .testTag("timer_reset_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = extColors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
