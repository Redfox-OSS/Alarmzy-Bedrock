package dev.redfox.alarmzy.presentation.groups

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.presentation.common.clockTime
import dev.redfox.alarmzy.ui.components.drawableRes
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.bedrock.components.BrCard
import dev.redfox.bedrock.components.BrCheckbox
import dev.redfox.bedrock.components.BrDialog
import dev.redfox.bedrock.components.BrDivider
import dev.redfox.bedrock.components.BrFab
import dev.redfox.bedrock.components.BrIconButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrMenu
import dev.redfox.bedrock.components.BrMenuItem
import dev.redfox.bedrock.components.BrSwitch
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.EmptyState
import dev.redfox.bedrock.components.IconButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RevealScope
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.navigation.brNavClearance
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrSpacing

/** Room the list leaves under its last card for the FAB (56dp plus a 16dp gap). */
private val FabClearance = 72.dp

@Composable
fun GroupsScreen(
    uiState: GroupsUiState,
    onIntent: (GroupsIntent) -> Unit,
    onAddGroup: () -> Unit,
    onEditGroup: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // The count the dialog was opened for, so its text holds steady while it eases out after the
    // selection has already been cleared.
    var deleteCount by remember { mutableIntStateOf(0) }

    // Back leaves selection mode instead of closing the app.
    BackHandler(enabled = uiState.selectionMode) { onIntent(GroupsIntent.ClearSelection) }
    val navClearance = brNavClearance()
    val listState = rememberLazyListState()
    val collapse = rememberTopBarCollapse(listState)
    val title = when {
        !uiState.selectionMode -> "Alarm groups"
        uiState.selectedCount == 0 -> "Select groups"
        else -> "${uiState.selectedCount} selected"
    }

    Box(modifier.fillMaxSize().background(BedrockTheme.colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().glassSource(),
            contentPadding = PaddingValues(
                top = topBarClearance(),
                // Leave room for the FAB above the floating nav bar.
                bottom = navClearance + FabClearance
            ),
            verticalArrangement = Arrangement.spacedBy(BrSpacing.M)
        ) {
            item(key = "title") {
                BrLargeTitle(title = title, collapse = collapse, modifier = Modifier.revealOnEnter(0))
            }
            when {
                uiState.isLoading -> Unit
                uiState.groups.isEmpty() -> item(key = "empty") {
                    Box(
                        Modifier.fillParentMaxHeight(0.75f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = painterResource(R.drawable.ic_nav_groups),
                            title = "No groups yet",
                            message = "Group alarms to switch them all on or off at once. Tap + to create one."
                        )
                    }
                }
                else -> itemsIndexed(uiState.groups, key = { _, it -> it.id }) { index, group ->
                    GroupCard(
                        group = group,
                        isExpanded = !uiState.selectionMode && group.id in uiState.expandedGroupIds,
                        selectionMode = uiState.selectionMode,
                        isSelected = group.id in uiState.selectedIds,
                        onCardClick = {
                            if (uiState.selectionMode) onIntent(GroupsIntent.ToggleSelection(group.id))
                            else onIntent(GroupsIntent.ToggleExpand(group.id))
                        },
                        onLongClick = { onIntent(GroupsIntent.EnterSelection(group.id)) },
                        onToggleGroup = { enabled -> onIntent(GroupsIntent.ToggleGroup(group.id, enabled)) },
                        onEditGroup = { onEditGroup(group.id) },
                        onToggleAlarm = { alarmId, enabled ->
                            onIntent(GroupsIntent.ToggleAlarmInGroup(alarmId, enabled))
                        },
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = BrSpacing.Gutter)
                            .revealOnEnter(index + 1)
                    )
                }
            }
        }

        BrTopBar(
            title = title,
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            AnimatedContent(
                targetState = uiState.selectionMode,
                transitionSpec = { BrTransitions.swap },
                label = "groupsHeaderActions"
            ) { selecting ->
                Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.XS)) {
                    if (selecting) {
                        BrIconButton(
                            icon = rememberVectorPainter(Icons.Rounded.DoneAll),
                            contentDescription = "Select all",
                            onClick = { onIntent(GroupsIntent.SelectAll) },
                            variant = IconButtonVariant.Glass
                        )
                        BrIconButton(
                            icon = rememberVectorPainter(Icons.Rounded.DeleteOutline),
                            contentDescription = "Delete selected",
                            onClick = {
                                deleteCount = uiState.selectedCount
                                showDeleteDialog = true
                            },
                            variant = IconButtonVariant.Glass,
                            enabled = uiState.selectedCount > 0,
                            tint = BedrockTheme.colors.critical
                        )
                        BrIconButton(
                            icon = rememberVectorPainter(Icons.Rounded.Close),
                            contentDescription = "Exit selection",
                            onClick = { onIntent(GroupsIntent.ClearSelection) },
                            variant = IconButtonVariant.Glass
                        )
                    } else {
                        var menuOpen by remember { mutableStateOf(false) }
                        Box {
                            BrIconButton(
                                icon = painterResource(R.drawable.ic_more_vert),
                                contentDescription = "More options",
                                onClick = { menuOpen = true },
                                variant = IconButtonVariant.Glass
                            )
                            BrMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                BrMenuItem(
                                    text = "Select groups",
                                    onClick = {
                                        menuOpen = false
                                        onIntent(GroupsIntent.StartSelection)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = !uiState.selectionMode,
            // BrFab eases itself in (a short rise and fade) each time it appears.
            enter = EnterTransition.None,
            exit = fadeOut(tween(BrDurations.Fade)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = BrSpacing.Gutter, bottom = navClearance)
        ) {
            BrFab(
                icon = painterResource(R.drawable.ic_add),
                contentDescription = "Create group",
                onClick = onAddGroup
            )
        }
    }

    // Always composed so it can ease out after Delete or Cancel.
    BrDialog(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        title = if (deleteCount == 1) "Delete group?" else "Delete $deleteCount groups?",
        message = if (deleteCount == 1) "The group will be deleted. Its alarms will be kept and ungrouped."
        else "These $deleteCount groups will be deleted. Their alarms will be kept and ungrouped.",
        confirmLabel = "Delete",
        onConfirm = {
            showDeleteDialog = false
            onIntent(GroupsIntent.DeleteSelected)
        },
        destructive = true
    )
}

@Composable
private fun GroupCard(
    group: AlarmGroup,
    isExpanded: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onCardClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleGroup: (Boolean) -> Unit,
    onEditGroup: () -> Unit,
    onToggleAlarm: (Long, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val enabled = group.isEnabled
    val groupColor = parseGroupColor(group.color)
    val chevronRotation = animateFloatAsState(if (isExpanded) 180f else 0f, BrMotion.settle(), label = "chevron")
    val dim = animateFloatAsState(if (enabled || isSelected) 1f else 0.8f, BrMotion.settle(), label = "groupDim")
    // A selected group fills with the accent container rather than growing a border.
    val cardColor by animateColorAsState(
        when {
            isSelected -> colors.accentContainer
            enabled -> colors.surface
            else -> colors.surface.copy(alpha = 0.55f)
        },
        BrMotion.settle(),
        label = "groupBg"
    )
    val metaColor by animateColorAsState(if (enabled) colors.textSecondary else colors.textTertiary, BrMotion.settle(), label = "groupMeta")
    // Group swatches are the app's own colours; the tile carries them inside, as a soft fill.
    val tileTint = (groupColor ?: colors.accentSoft).copy(alpha = if (enabled) 1f else 0.6f)
    val tileFill = if (enabled) groupColor?.copy(alpha = 0.16f) ?: colors.accentContainer else colors.surfaceHigh

    BrCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dim.value; compositingStrategy = CompositingStrategy.ModulateAlpha },
        color = cardColor,
        contentPadding = PaddingValues(BrSpacing.L)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.99f, onLongClick = onLongClick, onClick = onCardClick),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = selectionMode,
                enter = BrTransitions.revealStart,
                exit = BrTransitions.concealStart
            ) {
                Row {
                    BrCheckbox(checked = isSelected, onCheckedChange = { onCardClick() })
                    Spacer(Modifier.width(BrSpacing.M))
                }
            }
            IconTile(
                icon = painterResource(group.icon.drawableRes),
                tint = tileTint,
                container = tileFill,
                size = 44.dp,
                iconSize = 22.dp
            )
            Spacer(Modifier.width(BrSpacing.M))
            Column(Modifier.weight(1f)) {
                Text(
                    group.name,
                    style = type.headline,
                    color = if (enabled) colors.textPrimary else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    if (group.alarmCount == 1) "1 alarm" else "${group.alarmCount} alarms",
                    // Tabular figures, so counts don't shift between cards.
                    style = type.caption.copy(fontFeatureSettings = type.figure.fontFeatureSettings),
                    color = metaColor
                )
            }
            // Edit sits in the header next to the group it edits, and only while the group is
            // open, so it's in thumb reach without scrolling past every alarm, and collapsed
            // cards stay clean.
            AnimatedVisibility(
                visible = isExpanded && !selectionMode,
                enter = BrTransitions.revealStart,
                exit = BrTransitions.concealStart
            ) {
                BrIconButton(
                    icon = painterResource(R.drawable.ic_tune),
                    contentDescription = "Edit ${group.name}",
                    onClick = onEditGroup,
                    modifier = Modifier.padding(start = BrSpacing.S),
                    variant = IconButtonVariant.Tonal,
                    size = 36.dp,
                    tint = colors.accentSoft
                )
            }
            AnimatedVisibility(
                visible = !selectionMode,
                enter = BrTransitions.revealStart,
                exit = BrTransitions.concealStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(BrSpacing.S))
                    BrSwitch(checked = enabled, onCheckedChange = onToggleGroup)
                    Spacer(Modifier.width(BrSpacing.XS))
                    BrIconButton(
                        icon = painterResource(R.drawable.ic_chevron_down),
                        contentDescription = if (isExpanded) "Collapse ${group.name}" else "Expand ${group.name}",
                        onClick = onCardClick,
                        modifier = Modifier.graphicsLayer { rotationZ = chevronRotation.value },
                        variant = IconButtonVariant.Plain,
                        size = 36.dp,
                        tint = colors.textSecondary
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = BrTransitions.expandIn,
            exit = BrTransitions.collapseOut
        ) {
            // Fresh scope: the member alarms cascade in each time the group opens.
            RevealScope {
                Column {
                    Spacer(Modifier.height(BrSpacing.M))
                    BrDivider()
                    if (group.alarms.isEmpty()) {
                        // Empty group: point straight at the next step instead of a dead end.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .revealOnEnter(1, offsetY = 12.dp)
                                .pressable(pressedScale = 0.99f, onClick = onEditGroup)
                                .padding(vertical = BrSpacing.M),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "No alarms in this group yet",
                                style = type.body,
                                color = colors.textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text("Add alarms", style = type.label, color = colors.accentSoft)
                        }
                    } else {
                        group.alarms.forEachIndexed { i, alarm ->
                            Column(Modifier.revealOnEnter(i + 1, offsetY = 12.dp)) {
                                if (i > 0) BrDivider()
                                GroupAlarmRow(alarm = alarm, onToggle = { onToggleAlarm(alarm.id, it) })
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One member alarm inside an open group: its time, label and its own switch, on a hairline row. */
@Composable
private fun GroupAlarmRow(alarm: Alarm, onToggle: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val time = alarm.clockTime
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = BrSpacing.S),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(Modifier.weight(1f)) {
            Text(
                time.digits,
                style = type.figureLarge,
                color = if (alarm.isEnabled) colors.textPrimary else colors.textSecondary,
                modifier = Modifier.alignByBaseline()
            )
            Spacer(Modifier.width(6.dp))
            Text(
                time.meridiem,
                style = type.labelSmall,
                color = if (alarm.isEnabled) colors.textSecondary else colors.textTertiary,
                modifier = Modifier.alignByBaseline()
            )
            if (alarm.label.isNotBlank()) {
                Spacer(Modifier.width(10.dp))
                Text(
                    alarm.label,
                    style = type.caption,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        Spacer(Modifier.width(BrSpacing.M))
        BrSwitch(checked = alarm.isEnabled, onCheckedChange = onToggle)
    }
}
