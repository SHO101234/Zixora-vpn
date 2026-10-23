package com.v2ray.ang.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.v2ray.ang.ui.compose.AppDivider

/**
 * Settings tab content: the same destinations the navigation drawer launches,
 * as a simple in-body list. Every row leaves through [onNavigate], which the
 * activity resolves exactly like a drawer tap.
 */
@Composable
fun MainSettingsContent(
    onNavigate: (MainDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        drawerItems.forEachIndexed { index, item ->
            if (index == primaryDrawerItems.size) AppDivider()
            ListItem(
                headlineContent = { Text(stringResource(item.labelRes)) },
                leadingContent = { MainDestinationIcon(item) },
                modifier = Modifier.clickable { onNavigate(item) }
            )
        }
    }
}
