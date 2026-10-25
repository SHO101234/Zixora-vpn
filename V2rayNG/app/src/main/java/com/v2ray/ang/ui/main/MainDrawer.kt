package com.v2ray.ang.ui.main

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.verticalScrollbar

enum class MainDestination(@DrawableRes val iconRes: Int, @StringRes val labelRes: Int) {
    Subscriptions(R.drawable.ic_subscriptions_24dp, R.string.title_sub_setting),
    PerAppProxy(R.drawable.ic_per_apps_24dp, R.string.per_app_proxy_settings),
    Routing(R.drawable.ic_routing_24dp, R.string.routing_settings_title),
    UserAssets(R.drawable.ic_file_24dp, R.string.title_user_asset_setting),
    Settings(R.drawable.ic_settings_24dp, R.string.title_settings),
    Promotion(R.drawable.ic_promotion_24dp, R.string.title_pref_promotion),
    Logcat(R.drawable.ic_logcat_24dp, R.string.title_logcat),
    BackupRestore(R.drawable.ic_restore_24dp, R.string.title_configuration_backup_restore),
    SupportFacebook(R.drawable.ic_support_facebook_24dp, R.string.support_facebook),
    SupportTelegram(R.drawable.ic_support_telegram_24dp, R.string.support_telegram),
    About(R.drawable.ic_about_24dp, R.string.title_about)
}

internal val primaryDrawerItems = listOf(
    MainDestination.Subscriptions,
    MainDestination.PerAppProxy,
    MainDestination.Routing,
    MainDestination.UserAssets,
    MainDestination.Settings
)

internal val drawerItems = primaryDrawerItems + listOf(
    MainDestination.Promotion,
    MainDestination.Logcat,
    MainDestination.BackupRestore,
    MainDestination.SupportFacebook,
    MainDestination.SupportTelegram,
    MainDestination.About
)

/**
 * Icon for a drawer destination. The support entries carry their brands' own
 * colors in the drawable, so they render without a theme tint; every other
 * icon takes the theme tint.
 */
@Composable
internal fun MainDestinationIcon(item: MainDestination, modifier: Modifier = Modifier) {
    if (item == MainDestination.SupportFacebook || item == MainDestination.SupportTelegram) {
        Icon(
            painterResource(item.iconRes),
            contentDescription = null,
            modifier = modifier,
            tint = Color.Unspecified
        )
    } else {
        Icon(painterResource(item.iconRes), contentDescription = null, modifier = modifier)
    }
}

@Composable
fun MainDrawerContent(drawerState: DrawerState, onNavigate: (MainDestination) -> Unit) {
    val drawerScrollState = rememberScrollState()
    // Glass theme: the drawer goes glass-light with navy text instead of
    // the always-navy drawer. The launcher icon is a SrcIn-tinted mask, so
    // the same mechanism tints it navy here. Support icons keep their
    // brand colors via MainDestinationIcon (tint Unspecified), untouched.
    val appTheme by ThemeManager.appTheme.collectAsStateWithLifecycle()
    val isGlass = appTheme == "glass"
    val drawerNavy = Color(0xFF091F5B)
    val drawerContainer = if (isGlass) {
        MaterialTheme.colorScheme.surfaceContainer
    } else {
        drawerNavy
    }
    val drawerOnContainer = if (isGlass) drawerNavy else Color.White
    val drawerDivider = if (isGlass) drawerNavy.copy(alpha = 0.16f)
    else Color.White.copy(alpha = 0.16f)
    val drawerSelectedContainer = if (isGlass) drawerNavy.copy(alpha = 0.12f)
    else Color.White.copy(alpha = 0.12f)

    ModalDrawerSheet(
        drawerState = drawerState,
        modifier = Modifier.fillMaxWidth(0.75f),
        drawerContainerColor = drawerContainer
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(drawerScrollState)
                .verticalScrollbar(drawerScrollState)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                color = Color.Transparent
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(R.mipmap.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp),
                        colorFilter = ColorFilter.tint(
                            drawerOnContainer, BlendMode.SrcIn
                        )
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        color = drawerOnContainer
                    )
                }
            }
            drawerItems.forEachIndexed { index, item ->
                if (index == primaryDrawerItems.size) HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = drawerDivider
                )
                NavigationDrawerItem(
                    label = { Text(stringResource(item.labelRes)) },
                    selected = false,
                    onClick = { onNavigate(item) },
                    icon = { MainDestinationIcon(item) },
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = drawerSelectedContainer,
                        unselectedContainerColor = Color.Transparent,
                        selectedTextColor = drawerOnContainer,
                        unselectedTextColor = drawerOnContainer,
                        selectedIconColor = drawerOnContainer,
                        unselectedIconColor = drawerOnContainer
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    }
}
