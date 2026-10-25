package com.v2ray.ang.ui.main

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateBottomPadding
import androidx.compose.foundation.layout.calculateTopPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.v2ray.ang.R
import com.v2ray.ang.dto.GroupMapItem
import com.v2ray.ang.dto.LocateTarget
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.extension.isComplexType
import com.v2ray.ang.extension.toTrafficString
import com.v2ray.ang.ui.compose.AppDropdownMenuItems
import com.v2ray.ang.ui.compose.LocalDarkTheme
import com.v2ray.ang.ui.compose.ReorderableListItem
import com.v2ray.ang.ui.compose.ThemeManager
import com.v2ray.ang.ui.compose.colorFabInactiveLight
import com.v2ray.ang.ui.compose.colorPing
import com.v2ray.ang.ui.compose.verticalScrollbar
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * INCY-pattern Servers screen: one scrollable list of group sections.
 *
 * The screen background shows through everywhere (uniform, INCY-style):
 * there are no white or light surface cards or panels under the rows.
 * Sections and rows are separated by thin white-alpha dividers and text
 * hierarchy only. Text uses the backdrop colors (white / pale blue on the
 * navy light-theme background, onBackground / onSurfaceVariant on dark).
 * A selected row uses only the drawer's subtle white 12% wash, never a
 * solid contrasting panel.
 *
 * Group sections are transient presentation state only: collapse state is
 * a saveable list of collapsed group ids. All data and operations go
 * through [MainAction] / [MainViewModel]; this file touches no repository,
 * MMKV, or settings store directly.
 */
@Composable
fun MainServersScreen(
    mainViewModel: MainViewModel,
    groups: List<GroupMapItem>,
    selectedGuid: String?,
    locateTarget: LocateTarget?,
    doubleColumnDisplay: Boolean,
    searchQuery: String,
    isLoading: Boolean,
    onAction: (MainAction) -> Unit,
    onSelectServer: (String) -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isDarkTheme = LocalDarkTheme.current
    val colorScheme = MaterialTheme.colorScheme
    // Glass theme: every backdrop color flips from white-on-navy to navy on
    // glass-light. White-alpha washes, dividers, chips and tracks would be
    // invisible on the glass backdrop, so they become navy low-alpha here.
    // All other themes keep their exact current behavior.
    val appTheme by ThemeManager.appTheme.collectAsStateWithLifecycle()
    val isGlass = appTheme == "glass"
    val navy = Color(0xFF091F5B)
    // Backdrop text colors, mirroring MainHome: white / pale blue on the
    // navy light-theme background, light grays on the dark background.
    val onBackdrop = if (isGlass) colorScheme.onBackground
    else if (isDarkTheme) colorScheme.onBackground else colorScheme.onPrimary
    val onBackdropVariant = if (isGlass) colorScheme.onSurfaceVariant
    else if (isDarkTheme) colorScheme.onSurfaceVariant else colorScheme.primaryContainer
    val dividerColor = if (isGlass) navy.copy(alpha = 0.16f)
    else Color.White.copy(alpha = 0.16f)
    val selectedWash = if (isGlass) navy.copy(alpha = 0.12f)
    else Color.White.copy(alpha = 0.12f)
    val chipBackground = if (isGlass) navy.copy(alpha = 0.12f)
    else Color.White.copy(alpha = 0.12f)
    val badgeBackground = if (isGlass) navy.copy(alpha = 0.16f)
    else Color.White.copy(alpha = 0.16f)
    val trackColor = if (isGlass) navy.copy(alpha = 0.16f)
    else Color.White.copy(alpha = 0.16f)
    // Bulk-test state for the "Testing..." row label and the dead-row
    // tap guard; collected once at screen level, not per row.
    val uiState by mainViewModel.uiState.collectAsStateWithLifecycle()
    val isTesting = uiState.isTesting
    // Dead-key grey: muted grey on the navy backdrop, the scheme's
    // onSurfaceVariant on dark.
    val deadColor = if (isDarkTheme) colorScheme.onSurfaceVariant
    else colorFabInactiveLight

    // Per-group row state derived from the existing per-group flows only;
    // no new data layer. combine() re-emits when any group's rows change.
    val groupIds = groups.map { it.id }
    val combinedFlow = remember(groupIds, mainViewModel) {
        if (groupIds.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(groupIds.map { mainViewModel.serverGroupState(it) }) { states ->
                groupIds.zip(states).toMap()
            }
        }
    }
    val groupStates by combinedFlow.collectAsStateWithLifecycle(
        initialValue = emptyMap<String, ServerGroupUiState>()
    )

    // Per-group subscription data from the existing public accessor (an
    // in-memory MMKV decode), recomputed only when the group list changes.
    // A group without subscription userinfo simply shows no provider sub-row.
    val subscriptionsById = remember(groupIds, mainViewModel) {
        mainViewModel.getSubscriptions().associateBy { it.guid }
    }

    val searching = searchQuery.isNotEmpty()
    var collapsedGroupIds by rememberSaveable { mutableStateOf(listOf<String>()) }

    // During a search, groups with no matching row are hidden and matching
    // groups are force-expanded so no match hides inside a collapsed card.
    val visibleGroups = remember(groups, groupStates, searching) {
        groups.filter { group ->
            !searching || (groupStates[group.id]?.rows?.isNotEmpty() == true)
        }
    }
    val totalServers = remember(visibleGroups, groupStates) {
        visibleGroups.sumOf { groupStates[it.id]?.rows?.size ?: 0 }
    }

    val flatItems = remember(
        visibleGroups, groupStates, collapsedGroupIds, searching, doubleColumnDisplay
    ) {
        buildServersFlatItems(
            visibleGroups, groupStates, collapsedGroupIds, searching, doubleColumnDisplay
        )
    }

    val listState = rememberLazyListState()
    // The reorder state is always created (unconditional composable call);
    // it is only used to wrap rows when reordering applies.
    val reorderableListState = rememberReorderableLazyListState(listState) { from, to ->
        val fromItem = flatItems.getOrNull(from.index) as? ServersFlatItem.ServerRow
            ?: return@rememberReorderableLazyListState
        val toItem = flatItems.getOrNull(to.index) as? ServersFlatItem.ServerRow
            ?: return@rememberReorderableLazyListState
        if (fromItem.groupId != toItem.groupId) return@rememberReorderableLazyListState
        val rows = groupStates[fromItem.groupId]?.rows
            ?: return@rememberReorderableLazyListState
        val fromLocal = rows.indexOfFirst { it.guid == fromItem.row.guid }
        val toLocal = rows.indexOfFirst { it.guid == toItem.row.guid }
        if (fromLocal < 0 || toLocal < 0 || fromLocal == toLocal) {
            return@rememberReorderableLazyListState
        }
        mainViewModel.moveServer(fromItem.groupId, fromLocal, toLocal)
    }
    // Drag-reorder needs a single-column list and unfiltered rows, same as
    // the old pager (canReorder there also required an empty search query).
    val canReorder = !searching && !doubleColumnDisplay

    // Locating: expand the target's group first, then scroll to the row
    // once it is laid out. A row that never appears (still loading, or
    // filtered out) leaves the target unconsumed, as before.
    LaunchedEffect(locateTarget) {
        val target = locateTarget ?: return@LaunchedEffect
        if (target.groupId in collapsedGroupIds) {
            collapsedGroupIds = collapsedGroupIds - target.groupId
        }
    }
    LaunchedEffect(locateTarget, flatItems) {
        val target = locateTarget ?: return@LaunchedEffect
        val index = flatItems.indexOfFirst { item ->
            when (item) {
                is ServersFlatItem.ServerRow -> item.row.guid == target.serverGuid
                is ServersFlatItem.ServerPair ->
                    item.first.guid == target.serverGuid ||
                        item.second?.guid == target.serverGuid
                else -> false
            }
        }
        if (index < 0) return@LaunchedEffect
        listState.scrollToItem(index, -listState.layoutInfo.viewportSize.height / 3)
        onAction(MainAction.LocateHandled)
    }

    var showAddMenu by remember { mutableStateOf(false) }
    val addMenuScrollState = rememberScrollState()
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val maxMenuHeight = LocalConfiguration.current.screenHeightDp.dp -
        statusBarHeight - navBarHeight - 20.dp

    Column(modifier = modifier.fillMaxSize()) {
        ServersHeader(
            totalServers = totalServers,
            // The synthetic "All" group (empty id) is a view, not a provider.
            visibleGroups = visibleGroups.count { it.id.isNotEmpty() },
            onBackdrop = onBackdrop,
            onBackdropVariant = onBackdropVariant,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (groups.isEmpty()) {
                if (isLoading) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = onBackdrop,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.servers_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = onBackdropVariant
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.servers_no_groups),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBackdropVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            } else if (flatItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.servers_search_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBackdropVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(listState),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(flatItems, key = { it.key }) { item ->
                        val showDivider = item != flatItems.last()
                        when (item) {
                            is ServersFlatItem.GroupHeader -> {
                                val groupState = groupStates[item.group.id]
                                    ?: ServerGroupUiState()
                                val expanded = searching ||
                                    item.group.id !in collapsedGroupIds
                                val subscription =
                                    subscriptionsById[item.group.id]?.subscription
                                GroupHeaderItem(
                                    group = item.group,
                                    rowCount = groupState.rows.size,
                                    expanded = expanded,
                                    subscription = subscription,
                                    onBackdrop = onBackdrop,
                                    onBackdropVariant = onBackdropVariant,
                                    trackColor = trackColor,
                                    onToggleExpand = {
                                        collapsedGroupIds =
                                            if (item.group.id in collapsedGroupIds) {
                                                collapsedGroupIds - item.group.id
                                            } else {
                                                collapsedGroupIds + item.group.id
                                            }
                                    },
                                    onRefresh = {
                                        mainViewModel.reloadAllGroups(
                                            listOf(item.group.id)
                                        )
                                    },
                                    onSpeedtest = {
                                        // Testing is scoped to the selected
                                        // group in the existing
                                        // implementation, so testing a card
                                        // first makes it the selected group
                                        // (the old pager tested the visible
                                        // tab, which was always selected).
                                        onAction(MainAction.SelectGroup(item.group.id))
                                        onAction(MainAction.TestRealAllServers)
                                    },
                                    onMoreAction = onAction,
                                )
                            }

                            is ServersFlatItem.ServerRow -> {
                                val rowContent: @Composable () -> Unit = {
                                    ServerRowContent(
                                        row = item.row,
                                        isSelected = item.row.guid == selectedGuid,
                                        doubleColumnDisplay = false,
                                        onBackdrop = onBackdrop,
                                        onBackdropVariant = onBackdropVariant,
                                        selectedWash = selectedWash,
                                        chipBackground = chipBackground,
                                        badgeBackground = badgeBackground,
                                        deadColor = deadColor,
                                        isTesting = isTesting,
                                        onSelectServer = onSelectServer,
                                        onDeadServerTap = {
                                            // Testing is scoped to the
                                            // selected group, so a dead-row
                                            // tap first selects its group,
                                            // mirroring the card speedtest.
                                            onAction(
                                                MainAction.SelectGroup(item.groupId)
                                            )
                                            onAction(MainAction.TestRealAllServers)
                                        },
                                        onEditServer = onEditServer,
                                        onShareServer = onShareServer,
                                        onMoreServer = onMoreServer,
                                        onRemoveServer = onRemoveServer,
                                    )
                                }
                                if (canReorder) {
                                    ReorderableItem(
                                        reorderableListState,
                                        key = item.row.guid
                                    ) { isDragging ->
                                        ReorderableListItem(
                                            scope = this,
                                            isDragging = isDragging
                                        ) { rowContent() }
                                    }
                                } else {
                                    rowContent()
                                }
                            }

                            is ServersFlatItem.ServerPair -> {
                                Row(modifier = Modifier.fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        ServerRowContent(
                                            row = item.first,
                                            isSelected = item.first.guid == selectedGuid,
                                            doubleColumnDisplay = true,
                                            onBackdrop = onBackdrop,
                                            onBackdropVariant = onBackdropVariant,
                                            selectedWash = selectedWash,
                                            chipBackground = chipBackground,
                                            badgeBackground = badgeBackground,
                                            deadColor = deadColor,
                                            isTesting = isTesting,
                                            onSelectServer = onSelectServer,
                                            onDeadServerTap = {
                                                // Testing is scoped to the
                                                // selected group, so a dead-row
                                                // tap first selects its group,
                                                // mirroring the card speedtest.
                                                onAction(
                                                    MainAction.SelectGroup(item.groupId)
                                                )
                                                onAction(MainAction.TestRealAllServers)
                                            },
                                            onEditServer = onEditServer,
                                            onShareServer = onShareServer,
                                            onMoreServer = onMoreServer,
                                            onRemoveServer = onRemoveServer,
                                        )
                                    }
                                    Box(modifier = Modifier.weight(1f)) {
                                        val second = item.second
                                        if (second != null) {
                                            ServerRowContent(
                                                row = second,
                                                isSelected = second.guid == selectedGuid,
                                                doubleColumnDisplay = true,
                                                onBackdrop = onBackdrop,
                                                onBackdropVariant = onBackdropVariant,
                                                selectedWash = selectedWash,
                                                chipBackground = chipBackground,
                                                badgeBackground = badgeBackground,
                                                deadColor = deadColor,
                                                isTesting = isTesting,
                                                onSelectServer = onSelectServer,
                                                onDeadServerTap = {
                                                    // Testing is scoped to the
                                                    // selected group, so a dead-row
                                                    // tap first selects its group,
                                                    // mirroring the card speedtest.
                                                    onAction(
                                                        MainAction.SelectGroup(item.groupId)
                                                    )
                                                    onAction(MainAction.TestRealAllServers)
                                                },
                                                onEditServer = onEditServer,
                                                onShareServer = onShareServer,
                                                onMoreServer = onMoreServer,
                                                onRemoveServer = onRemoveServer,
                                            )
                                        }
                                    }
                                }
                            }

                            is ServersFlatItem.GroupEmpty -> {
                                Text(
                                    text = stringResource(R.string.servers_group_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = onBackdropVariant,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                        if (showDivider) {
                            HorizontalDivider(
                                modifier = Modifier.fillMaxWidth(),
                                thickness = 1.dp,
                                color = dividerColor
                            )
                        }
                    }
                }
            }
        }

        ServersBottomBar(
            onBackdrop = onBackdrop,
            dividerColor = dividerColor,
            showAddMenu = showAddMenu,
            onAddMenuChange = { showAddMenu = it },
            addMenuScrollState = addMenuScrollState,
            maxMenuHeight = maxMenuHeight,
            onAction = onAction,
        )
    }
}

@Composable
private fun ServersHeader(
    totalServers: Int,
    visibleGroups: Int,
    onBackdrop: Color,
    onBackdropVariant: Color,
) {
    val resources = LocalContext.current.resources
    val serversText = resources.getQuantityString(
        R.plurals.servers_count, totalServers, totalServers
    )
    val providersText = resources.getQuantityString(
        R.plurals.servers_provider_count, visibleGroups, visibleGroups
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
    ) {
        Text(
            text = stringResource(R.string.home_show_servers),
            style = MaterialTheme.typography.titleLarge,
            color = onBackdrop
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$serversText · $providersText",
            style = MaterialTheme.typography.bodyMedium,
            color = onBackdropVariant
        )
    }
}

@Composable
private fun GroupHeaderItem(
    group: GroupMapItem,
    rowCount: Int,
    expanded: Boolean,
    subscription: com.v2ray.ang.dto.entities.SubscriptionItem?,
    onBackdrop: Color,
    onBackdropVariant: Color,
    trackColor: Color,
    onToggleExpand: () -> Unit,
    onRefresh: () -> Unit,
    onSpeedtest: () -> Unit,
    onMoreAction: (MainAction) -> Unit,
) {
    var showMoreMenu by remember { mutableStateOf(false) }
    // Reuses the exact home derivation (userinfo expiry / traffic / updated)
    // for this group; a group without userinfo yields no sub-row fields.
    val homeData = remember(subscription) {
        computeMainHomeData(profile = null, subscription = subscription)
    }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = group.remarks,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = onBackdrop,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "($rowCount)",
                style = MaterialTheme.typography.bodyMedium,
                color = onBackdropVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            IconButton(onClick = onRefresh, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh_24dp),
                    contentDescription = stringResource(R.string.servers_acc_refresh),
                    tint = onBackdrop,
                    modifier = Modifier.size(24.dp)
                )
            }
            IconButton(onClick = onSpeedtest, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_flash_on_24dp),
                    contentDescription = stringResource(R.string.title_real_ping_all_server),
                    tint = onBackdrop,
                    modifier = Modifier.size(24.dp)
                )
            }
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_more_vert_24dp),
                        contentDescription = stringResource(R.string.acc_more),
                        tint = onBackdrop,
                        modifier = Modifier.size(24.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    AppDropdownMenuItems(
                        items = cardMoreMenuActions,
                        labelRes = { it.labelRes },
                        onSelected = { action ->
                            showMoreMenu = false
                            cardMoreActionToMainAction(action)?.let(onMoreAction)
                        }
                    )
                }
            }
            IconButton(onClick = onToggleExpand, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(R.drawable.ic_expand_more_24dp),
                    contentDescription = stringResource(
                        if (expanded) {
                            R.string.servers_acc_collapse
                        } else {
                            R.string.servers_acc_expand
                        }
                    ),
                    tint = onBackdrop,
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(if (expanded) 180f else 0f)
                )
            }
        }
        if (expanded) {
            ProviderSubRow(
                homeData = homeData,
                onBackdrop = onBackdrop,
                onBackdropVariant = onBackdropVariant,
                trackColor = trackColor
            )
        }
    }
}

@Composable
private fun ProviderSubRow(
    homeData: MainHomeData,
    onBackdrop: Color,
    onBackdropVariant: Color,
    trackColor: Color,
) {
    val daysRemaining = homeData.daysRemaining
    val trafficUsedBytes = homeData.trafficUsedBytes
    val lastUpdatedEpochMs = homeData.homeLastUpdatedEpochMs
    if (daysRemaining == null && trafficUsedBytes == null && lastUpdatedEpochMs == null) {
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
    ) {
        if (daysRemaining != null) {
            Text(
                text = if (daysRemaining < 0L) {
                    stringResource(R.string.home_subscription_expired)
                } else {
                    stringResource(R.string.home_days_remaining, daysRemaining)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = onBackdrop
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (trafficUsedBytes != null) {
            val trafficTotalBytes = homeData.trafficTotalBytes
            if (trafficTotalBytes != null && trafficTotalBytes > 0L) {
                LinearProgressIndicator(
                    progress = {
                        (trafficUsedBytes.toFloat() / trafficTotalBytes.toFloat())
                            .coerceIn(0f, 1f)
                    },
                    color = MaterialTheme.colorScheme.tertiary,
                    trackColor = trackColor,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(
                        R.string.home_traffic_format,
                        trafficUsedBytes.toTrafficString(),
                        trafficTotalBytes.toTrafficString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackdropVariant
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.home_traffic_total_unlimited,
                        trafficUsedBytes.toTrafficString()
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = onBackdropVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        if (lastUpdatedEpochMs != null) {
            Text(
                text = DateUtils.getRelativeTimeSpanString(
                    lastUpdatedEpochMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                ).toString(),
                style = MaterialTheme.typography.bodySmall,
                color = onBackdropVariant
            )
        }
    }
}

/**
 * One server row on the uniform backdrop: no panel, no rounded card.
 * Separation comes from the divider drawn by the list item below this row.
 * The selected cue is the drawer's white 12% wash only.
 */
@Composable
private fun ServerRowContent(
    row: ServerRowUiModel,
    isSelected: Boolean,
    doubleColumnDisplay: Boolean,
    onBackdrop: Color,
    onBackdropVariant: Color,
    selectedWash: Color,
    chipBackground: Color,
    badgeBackground: Color,
    deadColor: Color,
    isTesting: Boolean,
    onSelectServer: (String) -> Unit,
    onDeadServerTap: () -> Unit,
    onEditServer: (String, ProfileItem) -> Unit,
    onShareServer: (String, ProfileItem) -> Unit,
    onMoreServer: (String, ProfileItem) -> Unit,
    onRemoveServer: (String, String) -> Unit,
) {
    // Dead keys show "Timeout" in grey (never the red number, never
    // blank); unmeasured rows show "Testing..." in grey while a bulk
    // test runs and stay blank otherwise.
    val testResult = when {
        row.testDelayMillis < 0L -> stringResource(R.string.server_test_timeout)
        row.testDelayMillis == 0L && isTesting ->
            stringResource(R.string.server_test_testing)
        row.testDelayMillis == 0L -> ""
        else -> stringResource(R.string.server_test_delay_value, row.testDelayMillis)
    }
    val selectedStateDescription = if (isSelected) {
        stringResource(R.string.acc_selected_server)
    } else {
        null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (isSelected) selectedWash else Color.Transparent)
            .semantics {
                if (selectedStateDescription != null) {
                    stateDescription = selectedStateDescription
                }
            }
            // Dead rows must not become the connectable config: tapping
            // one re-tests its group instead of selecting it (no-op
            // while a test is already running). Share/edit/delete/more
            // stay available on dead rows.
            .clickable {
                if (row.testDelayMillis < 0L) {
                    if (!isTesting) onDeadServerTap()
                } else {
                    onSelectServer(row.guid)
                }
            }
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = row.remarks,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge.copy(
                        lineBreak = LineBreak.Paragraph
                    ),
                    color = onBackdrop,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (doubleColumnDisplay) {
                    IconButton(
                        onClick = { onMoreServer(row.guid, row.profile) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_more_vert_24dp),
                            contentDescription = stringResource(R.string.acc_more),
                            tint = onBackdrop,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = { onShareServer(row.guid, row.profile) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_share_24dp),
                            contentDescription = stringResource(R.string.title_configuration_share),
                            tint = onBackdrop,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { onEditServer(row.guid, row.profile) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_edit_24dp),
                            contentDescription = stringResource(R.string.acc_edit),
                            tint = onBackdrop,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(
                        onClick = { onRemoveServer(row.guid, row.remarks) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_delete_24dp),
                            contentDescription = stringResource(R.string.acc_delete),
                            tint = onBackdrop,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            val tags = remember(row.profile) { protocolTags(row.profile) }
            if (tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(chipBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall,
                                color = onBackdrop
                            )
                        }
                    }
                }
            }
            if (row.subscriptionBadge.isNotBlank() ||
                row.statistics.isNotBlank() ||
                testResult.isNotBlank()
            ) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (row.subscriptionBadge.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(badgeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = row.subscriptionBadge.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = onBackdrop
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = row.statistics,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBackdropVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (testResult.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = testResult,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (row.testDelayMillis > 0L) colorPing
                            else deadColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ServersBottomBar(
    onBackdrop: Color,
    dividerColor: Color,
    showAddMenu: Boolean,
    onAddMenuChange: (Boolean) -> Unit,
    addMenuScrollState: androidx.compose.foundation.ScrollState,
    maxMenuHeight: androidx.compose.ui.unit.Dp,
    onAction: (MainAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = dividerColor
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                TextButton(
                    onClick = { onAddMenuChange(true) },
                    colors = ButtonDefaults.textButtonColors(contentColor = onBackdrop)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add_24dp),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.servers_action_add))
                }
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { onAddMenuChange(false) },
                    scrollState = addMenuScrollState,
                    containerColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .heightIn(max = maxMenuHeight)
                        .verticalScrollbar(addMenuScrollState)
                ) {
                    ImportMenuContent(
                        onAction = { action ->
                            onAddMenuChange(false)
                            onAction(action)
                        }
                    )
                }
            }
            TextButton(
                onClick = { onAction(MainAction.ImportClipboard) },
                colors = ButtonDefaults.textButtonColors(contentColor = onBackdrop)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_copy),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.servers_action_paste))
            }
            TextButton(
                onClick = { onAction(MainAction.ImportQRcode) },
                colors = ButtonDefaults.textButtonColors(contentColor = onBackdrop)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_qu_scan_24dp),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = stringResource(R.string.servers_action_qr))
            }
        }
    }
}

private sealed interface ServersFlatItem {
    val key: String

    data class GroupHeader(val group: GroupMapItem) : ServersFlatItem {
        override val key: String get() = "header:${group.id}"
    }

    data class ServerRow(val groupId: String, val row: ServerRowUiModel) : ServersFlatItem {
        override val key: String get() = "row:${row.guid}"
    }

    data class ServerPair(
        val groupId: String,
        val first: ServerRowUiModel,
        val second: ServerRowUiModel?,
    ) : ServersFlatItem {
        override val key: String get() = "pair:${first.guid}"
    }

    data class GroupEmpty(val groupId: String) : ServersFlatItem {
        override val key: String get() = "empty:$groupId"
    }
}

private fun buildServersFlatItems(
    visibleGroups: List<GroupMapItem>,
    groupStates: Map<String, ServerGroupUiState>,
    collapsedGroupIds: List<String>,
    searching: Boolean,
    pairMode: Boolean,
): List<ServersFlatItem> {
    val items = mutableListOf<ServersFlatItem>()
    for (group in visibleGroups) {
        val rows = groupStates[group.id]?.rows.orEmpty()
        items += ServersFlatItem.GroupHeader(group)
        val expanded = searching || group.id !in collapsedGroupIds
        if (!expanded) continue
        if (rows.isEmpty()) {
            items += ServersFlatItem.GroupEmpty(group.id)
            continue
        }
        if (pairMode) {
            rows.chunked(2).forEach { chunk ->
                items += ServersFlatItem.ServerPair(group.id, chunk[0], chunk.getOrNull(1))
            }
        } else {
            rows.forEach { row ->
                items += ServersFlatItem.ServerRow(group.id, row)
            }
        }
    }
    return items
}

/**
 * Protocol tag chips, tokenized exactly like the old typeDescription line
 * (serverProtocolDescription logic in MainServerRowModels.kt): protocol,
 * then network unless plain tcp, then security. Complex types show only
 * the type.
 */
private fun protocolTags(profile: ProfileItem): List<String> {
    if (profile.configType.isComplexType()) return listOf(profile.configType.name)
    val tags = mutableListOf(profile.configType.name)
    profile.network?.let { network ->
        if (network.isNotBlank() && !network.equals("tcp", ignoreCase = true)) {
            tags += network
        }
    }
    profile.security?.let { security ->
        if (security.isNotBlank()) {
            tags += security
        }
    }
    return tags
}

/**
 * Card "more" offers the existing global more-menu actions that need no
 * confirm dialog. The three delete actions stay in the top-bar menu only,
 * because their confirm dialogs are owned by MainScreen.
 */
private fun cardMoreActionToMainAction(action: MainMoreMenuAction): MainAction? =
    when (action) {
        MainMoreMenuAction.RestartService -> MainAction.RestartService
        MainMoreMenuAction.ExportAll -> MainAction.ExportAll
        MainMoreMenuAction.LocateSelected -> MainAction.LocateSelectedServer
        MainMoreMenuAction.SortByTestResults -> MainAction.SortByTestResults
        MainMoreMenuAction.TestAll -> MainAction.TestAllServers
        MainMoreMenuAction.TestAllRealPing -> MainAction.TestRealAllServers
        MainMoreMenuAction.UpdateSubscriptions -> MainAction.UpdateSubscriptions
        MainMoreMenuAction.DeleteAll,
        MainMoreMenuAction.DeleteDuplicate,
        MainMoreMenuAction.DeleteInvalid -> null
    }

private val cardMoreMenuActions: List<MainMoreMenuAction> =
    MainMoreMenuAction.entries.filter { cardMoreActionToMainAction(it) != null }
