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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.extension.toSpeedString
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.colorConfigType
import com.v2ray.ang.ui.compose.colorFabInactiveLight
import com.v2ray.ang.ui.compose.colorPing
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
    isTesting: Boolean,
    lastBulkTestEpochMs: Long,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val showCard = groupRemarks.isNotBlank() || serverRows.isNotEmpty() ||
            daysRemaining != null || trafficUsedBytes != null

    // Navy brand backdrop (owner's logo-theme request): the light scheme's
    // primary and the dark scheme's surfaceContainer both resolve to navy
    // #091F5B; the paired colors below resolve to white / light blue in
    // both schemes, so every element stays readable on the navy.
    // Glass theme: the whole screen goes glass-light instead — glass
    // backdrop, navy text, blue secondary text. Other themes are unchanged.
    val appTheme by ThemeManager.appTheme.collectAsStateWithLifecycle()
    val isGlass = appTheme == "glass"
    val darkScheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = if (isGlass) MaterialTheme.colorScheme.surfaceContainer
    else if (darkScheme) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.primary
    val onBackdrop = if (isGlass) MaterialTheme.colorScheme.onBackground
    else if (darkScheme) MaterialTheme.colorScheme.onBackground
    else MaterialTheme.colorScheme.onPrimary
    val onBackdropVariant = if (isGlass) MaterialTheme.colorScheme.onSurfaceVariant
    else if (darkScheme) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primaryContainer

    // Auto real-ping once per group so ping numbers show without a manual
    // test (owner request). Re-fires when the group/server set changes,
    // when the screen is re-entered, and when the last bulk test goes
    // stale — but never while a test is already running, and never on a
    // ticking clock: the key carries only a stale boolean, the clock
    // comparison happens inside the effect body via isBulkTestStale.
    val bulkTestStale = isBulkTestStale(
        lastBulkTestEpochMs, System.currentTimeMillis()
    )
    val pingKey = groupRemarks + "|" + serverRows.size + "|" + bulkTestStale
    LaunchedEffect(pingKey) {
        if (!isTesting && serverRows.isNotEmpty() &&
            (serverRows.all { it.testDelayMillis == 0L } || bulkTestStale)
        ) {
            onAction(MainAction.TestRealAllServers)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
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
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackdropVariant
                )
                Text(
                    text = formatElapsed(elapsedSeconds),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackdrop
                )
                Text(
                    text = "↓ ${downSpeedBytesPerSec.toSpeedString()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackdropVariant
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Connect medallion on the navy backdrop. State colors SWAPPED per
        // the owner's call: disconnected = light blue disc (#D0E4FD) with a
        // navy shield; connected = GREEN disc (#009966) with a white shield,
        // so "connected" reads instantly. Ring follows the disc state; the
        // band is deep blue when idle, pale green when connected.
        val ringColor = if (isRunning) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primaryContainer
        val bandColor = if (isRunning) MaterialTheme.colorScheme.tertiaryContainer
        else MaterialTheme.colorScheme.secondary
        val discColor = if (isRunning) MaterialTheme.colorScheme.tertiary
        else MaterialTheme.colorScheme.primaryContainer
        val discTint = if (isRunning) MaterialTheme.colorScheme.onTertiary
        else MaterialTheme.colorScheme.onPrimaryContainer
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
                    .border(2.dp, ringColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .border(12.dp, bandColor, CircleShape)
            )
            Box(
                modifier = Modifier
                    .size(156.dp)
                    .clip(CircleShape)
                    .background(discColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_shield_24dp),
                    contentDescription = stringResource(
                        if (isRunning) R.string.acc_stop else R.string.acc_start
                    ),
                    tint = discTint,
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
            color = onBackdrop,
            textAlign = TextAlign.Center
        )

        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = onBackdropVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 4.dp)
                .clickable { onAction(MainAction.TestCurrentServer) }
        )

        if (showCard) {
            Spacer(modifier = Modifier.height(28.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    // In Glass mode the card must stay visible against the
                    // glass backdrop, so it goes white instead of glass.
                    containerColor = if (isGlass) {
                        MaterialTheme.colorScheme.surfaceContainerLowest
                    } else {
                        MaterialTheme.colorScheme.surface
                    }
                )
            ) {
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
                        // Manual bulk retest for this group (ping numbers go
                        // stale; the auto-ping only re-fires on group/set
                        // change or staleness). Disabled while a test runs.
                        if (serverRows.isNotEmpty()) {
                            IconButton(
                                onClick = { onAction(MainAction.TestRealAllServers) },
                                enabled = !isTesting
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_flash_on_24dp),
                                    contentDescription = stringResource(
                                        R.string.home_retest_all_servers
                                    )
                                )
                            }
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
                                isTesting = isTesting,
                                // Dead rows must not become the connectable
                                // config: tapping one re-tests the group
                                // instead of selecting it (no-op while a
                                // test is already running).
                                onClick = {
                                    if (row.testDelayMillis < 0L) {
                                        if (!isTesting) {
                                            onAction(MainAction.TestRealAllServers)
                                        }
                                    } else {
                                        onAction(MainAction.SelectServer(row.guid))
                                    }
                                }
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
    isTesting: Boolean,
    onClick: () -> Unit
) {
    // Dead keys show "Timeout" in grey (never the red number, never blank);
    // unmeasured rows show "Testing..." in grey while a bulk test runs and
    // stay blank otherwise.
    val delayText: String?
    val delayColor: Color
    when {
        row.testDelayMillis < 0L -> {
            delayText = stringResource(R.string.server_test_timeout)
            delayColor = colorFabInactiveLight
        }
        row.testDelayMillis == 0L && isTesting -> {
            delayText = stringResource(R.string.server_test_testing)
            delayColor = colorFabInactiveLight
        }
        row.testDelayMillis != 0L -> {
            delayText = stringResource(
                R.string.server_test_delay_value,
                row.testDelayMillis
            )
            delayColor = colorPing
        }
        else -> {
            delayText = null
            delayColor = Color.Unspecified
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainer
            )
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) { this.selected = selected },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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
                if (delayText != null) {
                    Text(
                        text = delayText,
                        style = MaterialTheme.typography.bodySmall,
                        color = delayColor,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_done),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp)
                )
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
