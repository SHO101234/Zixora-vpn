package com.v2ray.ang.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.QRCodeDialog
import com.v2ray.ang.ui.compose.ThemeManager
import kotlinx.coroutines.launch

/** Bottom-navigation tabs of the main screen. Home is the start tab. */
internal enum class MainTab { HOME, SERVERS, SETTINGS }

@Composable
fun MainScreen(
    mainViewModel: MainViewModel,
    onAction: (MainAction) -> Unit,
    onNavigate: (MainDestination) -> Unit,
) {
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val groups = uiState.groups
    val isLoading by mainViewModel.isLoading.collectAsStateWithLifecycle()
    val isRunning = uiState.isRunning
    val displayText = mainViewModel.formatStatus(uiState.status)
    val selectedGuid = uiState.selectedGuid
    val doubleColumnDisplay = uiState.doubleColumnDisplay
    val confirmRemove = uiState.confirmRemove
    val shareQRCodeBitmap = uiState.shareQRCodeBitmap

    val isDarkTheme = LocalDarkTheme.current
    // Glass theme: the Scaffold container is the Servers tab's backdrop, so
    // it must go glass-light instead of navy when Glass is active. Home and
    // Settings paint their own backdrops over it, so they are unaffected.
    val appTheme by ThemeManager.appTheme.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var selectedTab by rememberSaveable { mutableStateOf(MainTab.HOME) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var showDelAllConfirm by remember { mutableStateOf(false) }
    var showDelDuplicateConfirm by remember { mutableStateOf(false) }
    var showDelInvalidConfirm by remember { mutableStateOf(false) }
    var showRemoveConfirm by rememberSaveable(stateSaver = ServerDeleteTarget.Saver) {
        mutableStateOf<ServerDeleteTarget?>(null)
    }

    var shareTarget by remember { mutableStateOf<Triple<String, ProfileItem, Boolean>?>(null) }
    val removeServer: (String, String) -> Unit = { guid, profileName ->
        if (confirmRemove) {
            showRemoveConfirm = ServerDeleteTarget(guid, profileName)
        } else {
            onAction(MainAction.RemoveServer(guid))
        }
    }

    // Home is the start tab; from the other tabs, Back returns to Home
    // (on Home itself the activity's own BackHandler keeps its behavior).
    BackHandler(enabled = selectedTab != MainTab.HOME) {
        selectedTab = MainTab.HOME
    }

    // Locating the selected server only makes sense in the server list.
    LaunchedEffect(uiState.locateTarget) {
        if (uiState.locateTarget != null) selectedTab = MainTab.SERVERS
    }

    // Server rows of the selected profile's group, shown inline on Home.
    val homeGroupState by mainViewModel.serverGroupState(uiState.homeGroupId)
        .collectAsStateWithLifecycle()

    MainDialogs(
        showDelAllConfirm = showDelAllConfirm,
        onDismissDelAll = { showDelAllConfirm = false },
        onConfirmDelAll = { showDelAllConfirm = false; onAction(MainAction.RemoveAllServers) },
        showDelDuplicateConfirm = showDelDuplicateConfirm,
        onDismissDelDuplicate = { showDelDuplicateConfirm = false },
        onConfirmDelDuplicate = { showDelDuplicateConfirm = false; onAction(MainAction.RemoveDuplicateServers) },
        showDelInvalidConfirm = showDelInvalidConfirm,
        onDismissDelInvalid = { showDelInvalidConfirm = false },
        onConfirmDelInvalid = { showDelInvalidConfirm = false; onAction(MainAction.RemoveInvalidServers) },
        showRemoveConfirm = showRemoveConfirm,
        onDismissRemove = { showRemoveConfirm = null },
        onConfirmRemove = { guid -> showRemoveConfirm = null; onAction(MainAction.RemoveServer(guid)) }
    )

    if (shareTarget != null) {
        val (guid, profile, more) = shareTarget!!
        ShareMethodDialog(
            guid = guid,
            profile = profile,
            more = more,
            onDismiss = { shareTarget = null },
            onAction = onAction,
            onRemove = removeServer,
        )
    }
    if (shareQRCodeBitmap != null) {
        QRCodeDialog(bitmap = shareQRCodeBitmap, onDismiss = { onAction(MainAction.DismissQRCodeDialog) })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            MainDrawerContent(
                drawerState = drawerState,
                onNavigate = { route ->
                    scope.launch { drawerState.close() }
                    onNavigate(route)
                }
            )
        }
    ) {
        Scaffold(
            containerColor = if (appTheme == "glass") {
                MaterialTheme.colorScheme.surfaceContainer
            } else if (isDarkTheme) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.primary
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets,
            topBar = {
                MainTopBar(
                    isLoading = isLoading,
                    showSearch = showSearch,
                    searchQuery = searchQuery,
                    onSearchQueryChange = { query: String ->
                        searchQuery = query
                        onAction(MainAction.Search(query))
                    },
                    onSearchClose = {
                        searchQuery = ""
                        onAction(MainAction.Search(""))
                        showSearch = false
                    },
                    onSearchToggle = { show: Boolean -> showSearch = show },
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onAction = onAction,
                    onMoreMenuAction = { action ->
                        when (action) {
                            MainMoreMenuAction.RestartService -> onAction(MainAction.RestartService)
                            MainMoreMenuAction.DeleteAll -> showDelAllConfirm = true
                            MainMoreMenuAction.DeleteDuplicate -> showDelDuplicateConfirm = true
                            MainMoreMenuAction.DeleteInvalid -> showDelInvalidConfirm = true
                            MainMoreMenuAction.ExportAll -> onAction(MainAction.ExportAll)
                            MainMoreMenuAction.LocateSelected -> onAction(MainAction.LocateSelectedServer)
                            MainMoreMenuAction.SortByTestResults -> onAction(MainAction.SortByTestResults)
                            MainMoreMenuAction.TestAll -> onAction(MainAction.TestAllServers)
                            MainMoreMenuAction.TestAllRealPing -> onAction(MainAction.TestRealAllServers)
                            MainMoreMenuAction.UpdateSubscriptions -> onAction(MainAction.UpdateSubscriptions)
                        }
                    }
                )
            },
            bottomBar = {
                Column {
                    // The status/test bar stays with the Servers tab, where the
                    // test actions live; Home has its own status line and the
                    // big connect button instead.
                    if (selectedTab == MainTab.SERVERS) {
                        MainBottomBar(
                            displayText = displayText,
                            isRunning = isRunning,
                            isDarkTheme = isDarkTheme,
                            onAction = onAction
                        )
                    }
                    NavigationBar {
                        NavigationBarItem(
                            selected = selectedTab == MainTab.HOME,
                            onClick = { selectedTab = MainTab.HOME },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_home_24dp),
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.tab_home)) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.SERVERS,
                            onClick = { selectedTab = MainTab.SERVERS },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_subscriptions_24dp),
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.home_show_servers)) }
                        )
                        NavigationBarItem(
                            selected = selectedTab == MainTab.SETTINGS,
                            onClick = { selectedTab = MainTab.SETTINGS },
                            icon = {
                                Icon(
                                    painterResource(R.drawable.ic_settings_24dp),
                                    contentDescription = null
                                )
                            },
                            label = { Text(stringResource(R.string.title_settings)) }
                        )
                    }
                }
            },
            floatingActionButton = {},
        ) { innerPadding ->
            when (selectedTab) {
                MainTab.HOME -> MainHomeContent(
                    groupRemarks = uiState.groupRemarks,
                    daysRemaining = uiState.daysRemaining,
                    trafficUsedBytes = uiState.trafficUsedBytes,
                    trafficTotalBytes = uiState.trafficTotalBytes,
                    lastUpdatedEpochMs = uiState.homeLastUpdatedEpochMs,
                    selectedRemarks = uiState.selectedRemarks,
                    selectedGuid = selectedGuid,
                    serverRows = homeGroupState.rows,
                    isRunning = isRunning,
                    statusText = displayText,
                    upSpeedBytesPerSec = uiState.homeUpSpeedBytesPerSec,
                    downSpeedBytesPerSec = uiState.homeDownSpeedBytesPerSec,
                    elapsedSeconds = uiState.homeElapsedSeconds,
                    isTesting = uiState.isTesting,
                    lastBulkTestEpochMs = uiState.lastBulkTestEpochMs,
                    onAction = onAction,
                    onNavigate = onNavigate,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.SERVERS -> MainServersScreen(
                    mainViewModel = mainViewModel,
                    groups = groups,
                    selectedGuid = selectedGuid,
                    locateTarget = uiState.locateTarget,
                    doubleColumnDisplay = doubleColumnDisplay,
                    searchQuery = searchQuery,
                    isLoading = isLoading,
                    onAction = onAction,
                    onSelectServer = { guid -> onAction(MainAction.SelectServer(guid)) },
                    onEditServer = { guid, profile -> onAction(MainAction.EditServer(guid, profile)) },
                    onShareServer = { guid, profile ->
                        shareTarget = Triple(guid, profile, false)
                    },
                    onMoreServer = { guid, profile ->
                        shareTarget = Triple(guid, profile, true)
                    },
                    onRemoveServer = removeServer,
                    modifier = Modifier.padding(innerPadding)
                )

                MainTab.SETTINGS -> MainSettingsContent(
                    onNavigate = onNavigate,
                    modifier = Modifier.padding(innerPadding),
                    mainViewModel = mainViewModel
                )
            }
        }
    }
}
