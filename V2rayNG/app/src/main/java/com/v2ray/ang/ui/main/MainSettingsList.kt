package com.v2ray.ang.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.v2ray.ang.ui.compose.AppDivider

/**
 * Settings tab content: the same destinations the navigation drawer launches,
 * as a simple in-body list on the navy brand backdrop, each destination a
 * white surface row (same treatment as the Home tab). Every row leaves
 * through [onNavigate], which the activity resolves exactly like a drawer
 * tap.
 */
@Composable
fun MainSettingsContent(
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val darkScheme = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val backdrop = if (darkScheme) MaterialTheme.colorScheme.surfaceContainer
    else MaterialTheme.colorScheme.primary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(backdrop)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        drawerItems.forEachIndexed { index, item ->
            if (index == primaryDrawerItems.size) AppDivider()
            ListItem(
                headlineContent = { Text(stringResource(item.labelRes)) },
                leadingContent = { MainDestinationIcon(item) },
                colors = ListItemDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onNavigate(item) }
            )
        }
    }
}
