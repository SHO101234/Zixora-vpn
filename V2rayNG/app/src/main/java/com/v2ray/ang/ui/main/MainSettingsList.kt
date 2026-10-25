package com.v2ray.ang.ui.main

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.BuildConfig
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.appThemeDisplayName

/**
 * Settings tab content: the same destinations the navigation drawer launches,
 * regrouped into labeled sections on the shared backdrop. Rows are bordered
 * outlines on the backdrop — never filled surface panels — with the row value
 * on the right and a chevron. Every row leaves through [onNavigate], which
 * the activity resolves exactly like a drawer tap. The APPEARANCE section
 * also holds the app-theme picker. The TUNNEL section has no mappable drawer
 * destination: tunnel settings (VPN DNS, MTU, HEV tunnel, bypass LAN) live
 * inside the detailed settings screen, reached via APPLICATION -> Settings.
 */
@Composable
fun MainSettingsContent(
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel? = null
) {
    // Glass theme: the whole tab goes glass-light instead of navy — glass
    // backdrop, navy text, blue secondary text, navy-outline boxes. Other
    // themes keep their exact current behavior.
    val currentTheme by ThemeManager.appTheme.collectAsStateWithLifecycle()
    val isGlass = currentTheme == "glass"
    val darkScheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = if (isGlass) MaterialTheme.colorScheme.surfaceContainer
    else if (darkScheme) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.primary
    val primaryText = if (isGlass) MaterialTheme.colorScheme.onBackground
    else if (darkScheme) MaterialTheme.colorScheme.onBackground else Color.White
    val secondaryText = if (isGlass) MaterialTheme.colorScheme.onSurfaceVariant
    else if (darkScheme) MaterialTheme.colorScheme.onSurfaceVariant
    else MaterialTheme.colorScheme.primaryContainer
    val borderColor = if (isGlass) Color(0xFF091F5B).copy(alpha = 0.16f)
    else Color.White.copy(alpha = 0.16f)

    val subscriptionCount: Int? = if (mainViewModel != null) {
        val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
        // The synthetic "All" group (empty id, added when group-all display is
        // on) is not a provider, so it is excluded from the provider count.
        uiState.groups.count { it.id.isNotEmpty() }
    } else {
        null
    }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }

    val sections = listOf(
        SettingsSection(
            R.string.settings_section_per_app_proxy,
            listOf(SettingsEntry.Destination(MainDestination.PerAppProxy))
        ),
        SettingsSection(
            R.string.settings_section_appearance,
            listOf(SettingsEntry.Theme)
        ),
        SettingsSection(
            R.string.settings_section_connection,
            listOf(
                SettingsEntry.Destination(MainDestination.Routing),
                SettingsEntry.Destination(MainDestination.UserAssets)
            )
        ),
        SettingsSection(
            R.string.settings_section_providers,
            listOf(
                SettingsEntry.Destination(
                    MainDestination.Subscriptions,
                    subscriptionCount?.let {
                        pluralStringResource(R.plurals.servers_provider_count, it, it)
                    }
                )
            )
        ),
        SettingsSection(
            R.string.settings_section_application,
            listOf(
                SettingsEntry.Destination(MainDestination.Settings),
                SettingsEntry.Destination(MainDestination.Promotion),
                SettingsEntry.Destination(MainDestination.Logcat),
                SettingsEntry.Destination(MainDestination.BackupRestore),
                SettingsEntry.Destination(MainDestination.SupportFacebook),
                SettingsEntry.Destination(MainDestination.SupportTelegram),
                SettingsEntry.Destination(
                    MainDestination.About,
                    "v${BuildConfig.VERSION_NAME}"
                )
            )
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        sections.forEach { section ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(section.titleRes),
                    style = MaterialTheme.typography.titleSmall,
                    color = secondaryText,
                    modifier = Modifier.padding(top = 4.dp)
                )
                section.entries.forEach { entry ->
                    when (entry) {
                        is SettingsEntry.Destination -> SettingsDestinationRow(
                            item = entry.destination,
                            value = entry.value,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            borderColor = borderColor,
                            onNavigate = onNavigate
                        )

                        SettingsEntry.Theme -> SettingsThemeRow(
                            currentTheme = currentTheme,
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            borderColor = borderColor,
                            onClick = { showThemeDialog = true }
                        )
                    }
                }
            }
        }
    }

    if (showThemeDialog) {
        ThemePickerDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false },
            onThemeSelected = { theme ->
                ThemeManager.setAppTheme(theme)
                showThemeDialog = false
            }
        )
    }
}

private sealed interface SettingsEntry {
    data class Destination(val destination: MainDestination, val value: String? = null) : SettingsEntry
    data object Theme : SettingsEntry
}

private data class SettingsSection(
    @StringRes val titleRes: Int,
    val entries: List<SettingsEntry>
)

@Composable
private fun SettingsDestinationRow(
    item: MainDestination,
    value: String?,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    onNavigate: (MainDestination) -> Unit
) {
    ListItem(
        headlineContent = { Text(stringResource(item.labelRes)) },
        leadingContent = { MainDestinationIcon(item) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (value != null) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyMedium,
                        color = secondaryText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.width(8.dp))
                }
                ChevronIcon(color = secondaryText)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = primaryText,
            leadingIconColor = primaryText,
            trailingIconColor = secondaryText
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onNavigate(item) }
    )
}

@Composable
private fun SettingsThemeRow(
    currentTheme: String,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(stringResource(R.string.settings_theme)) },
        leadingContent = {
            Icon(
                painter = painterResource(R.drawable.ic_settings_24dp),
                contentDescription = null
            )
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = appThemeDisplayName(currentTheme),
                    style = MaterialTheme.typography.bodyMedium,
                    color = secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                ChevronIcon(color = secondaryText)
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = primaryText,
            leadingIconColor = primaryText,
            trailingIconColor = secondaryText
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    )
}

/** Right-pointing chevron drawn in code: no chevron asset exists in res/drawable
 *  and the project has no material-icons dependency to draw one from. */
@Composable
private fun ChevronIcon(color: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = 2.dp.toPx()
        val width = size.width
        val height = size.height
        drawLine(
            color = color,
            start = Offset(width * 0.35f, height * 0.25f),
            end = Offset(width * 0.65f, height * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(width * 0.65f, height * 0.5f),
            end = Offset(width * 0.35f, height * 0.75f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun ThemePickerDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    val options = listOf("navy", "amoled", "primary", "glass")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_title)) },
        text = {
            Column {
                options.forEach { theme ->
                    val isSelected = theme == currentTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onThemeSelected(theme) }
                            .semantics(mergeDescendants = true) {
                                role = Role.RadioButton
                                selected = isSelected
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(text = appThemeDisplayName(theme))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
