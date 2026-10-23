package com.v2ray.ang.ui.main

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.v2ray.ang.R
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.colorConfigType
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.colorPingRed
import java.util.Locale

/**
 * INCY-style home content, rendered in the app theme: a live stats row while
 * connected, a large ringed connect button with a shield glyph, the selected
 * plan label and connection status, the provider card (group name, relative
 * update time, subscription refresh, days/traffic usage, and the group's
 * server rows inline), and a support row that opens the Facebook Page /
 * Telegram bot through the same navigation path as the drawer entries.
 * Presentational only — all data arrives via parameters and every operation
 * leaves through [onAction] or [onNavigate].
 */
@Composable
internal fun MainHomeContent(
    groupRemarks: String,
    daysRemaining: Long?,
    trafficUsedBytes: Long?,
    trafficTotalBytes: Long?,
    lastUpdatedEpochMs: Long?,
    selectedRemarks: String,
    selectedGuid: String?,
    serverRows: List<ServerRowUiModel>,
    isRunning: Boolean,
    statusText: String,
    upSpeedBytesPerSec: Long,
    downSpeedBytesPerSec: Long,
    elapsedSeconds: Long,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val showCard = groupRemarks.isNotBlank() || serverRows.isNotEmpty() ||
            daysRemaining != null || trafficUsedBytes != null

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "↑ ${upSpeedBytesPerSec.toSpeedString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = formatElapsed(elapsedSeconds),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "↓ ${downSpeedBytesPerSec.toSpeedString()}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Connect medallion: thin outer ring and thick band in the scheme's
        // primary tokens around a filled disc with the shield glyph. Brand
        // tokens only. State is color-coded per the owner's request: while
        // disconnected the disc is the lighter mid blue (tertiaryContainer
        // #6F96D1 in the light scheme); once connected it switches to the
        // deep navy primary (#091F5B), each with its matching on-color.
        Box(
            modifier = Modifier
                .size(208.dp)
                .clickable { onAction(MainAction.ToggleService) }
                .semantics(mergeDescendants = true) {},
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .border(12.dp, MaterialTheme.colorScheme.primaryContainer, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(
                        if (isRunning) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_24dp),
                    contentDescription = stringResource(
                        if (isRunning) R.string.acc_stop else R.string.acc_start
                    ),
                    tint = if (isRunning) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(72.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = selectedRemarks.ifBlank {
                groupRemarks.ifBlank { stringResource(R.string.home_no_server_selected) }
            },
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onAction(MainAction.TestCurrentServer) }
        )

        if (showCard) {
            Spacer(modifier = Modifier.height(28.dp))

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (groupRemarks.isNotBlank()) {
                            Text(
                                text = groupRemarks,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                        if (lastUpdatedEpochMs != null) {
                            Text(
                                text = DateUtils.getRelativeTimeSpanString(
                                    lastUpdatedEpochMs,
                                    System.currentTimeMillis(),
                                    DateUtils.MINUTE_IN_MILLIS
                                ).toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onAction(MainAction.UpdateSubscriptions) }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_refresh_24dp),
                                contentDescription = stringResource(R.string.title_sub_update)
                            )
                        }
                    }

                    if (daysRemaining != null || trafficUsedBytes != null) {
                        if (daysRemaining != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (daysRemaining < 0L) {
                                    stringResource(R.string.home_subscription_expired)
                                } else {
                                    stringResource(R.string.home_days_remaining, daysRemaining)
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (trafficUsedBytes != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            if (trafficTotalBytes != null && trafficTotalBytes > 0L) {
                                LinearProgressIndicator(
                                    progress = {
                                        (trafficUsedBytes.toFloat() / trafficTotalBytes.toFloat())
                                            .coerceIn(0f, 1f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.home_traffic_format,
                                        trafficUsedBytes.toTrafficString(),
                                        trafficTotalBytes.toTrafficString()
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            } else {
                                Text(
                                    text = stringResource(
                                        R.string.home_traffic_total_unlimited,
                                        trafficUsedBytes.toTrafficString()
                                    ),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    if (serverRows.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        serverRows.forEachIndexed { index, row ->
                            if (index > 0) Spacer(modifier = Modifier.height(4.dp))
                            HomeServerRow(
                                row = row,
                                selected = row.guid == selectedGuid,
                                onClick = { onAction(MainAction.SelectServer(row.guid)) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Support marks keep their brands' own colors (drawn in the
        // drawables), so these icons render without a theme tint.
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconButton(onClick = { onNavigate(MainDestination.SupportFacebook) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_support_facebook_24dp),
                    contentDescription = stringResource(R.string.support_facebook),
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
            IconButton(onClick = { onNavigate(MainDestination.SupportTelegram) }) {
                Icon(
                    painter = painterResource(R.drawable.ic_support_telegram_24dp),
                    contentDescription = stringResource(R.string.support_telegram),
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@Composable
private fun HomeServerRow(
    row: ServerRowUiModel,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = row.remarks,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (row.typeDescription.isNotBlank()) {
                    Text(
                        text = row.typeDescription,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorConfigType
                    )
                }
                if (row.testDelayMillis != 0L) {
                    Text(
                        text = stringResource(
                            R.string.server_test_delay_value,
                            row.testDelayMillis
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (row.testDelayMillis < 0L) colorPingRed else colorPing,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

private fun formatElapsed(totalSeconds: Long): String {
    val seconds = totalSeconds.coerceAtLeast(0L)
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, secs)
    }
}
