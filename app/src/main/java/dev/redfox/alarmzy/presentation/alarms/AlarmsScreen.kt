package dev.redfox.alarmzy.presentation.alarms

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.alarm.RingingAlarm
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.presentation.common.ClockTime
import dev.redfox.alarmzy.presentation.common.caption
import dev.redfox.alarmzy.presentation.common.clockTime
import dev.redfox.alarmzy.presentation.common.detailText
import dev.redfox.alarmzy.presentation.common.relativeDay
import dev.redfox.alarmzy.presentation.livealerts.FullScreenAlarmWarning
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsHomePrompt
import dev.redfox.alarmzy.presentation.livealerts.rememberFullScreenAlarmsAllowed
import dev.redfox.bedrock.components.AuraCard
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrCard
import dev.redfox.bedrock.components.BrCheckbox
import dev.redfox.bedrock.components.BrDialog
import dev.redfox.bedrock.components.BrFab
import dev.redfox.bedrock.components.BrIconButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrMenu
import dev.redfox.bedrock.components.BrMenuItem
import dev.redfox.bedrock.components.BrSwitch
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.EmptyState
import dev.redfox.bedrock.components.IconButtonVariant
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.breathe
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.rememberAura
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.SwapText
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.navigation.brNavClearance
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrSpacing

/** Room the list leaves under its last card for the FAB (56dp plus a 16dp gap). */
private val FabClearance = 72.dp

@Composable
fun AlarmsScreen(
    uiState: AlarmsUiState,
    onIntent: (AlarmsIntent) -> Unit,
    onAddAlarm: () -> Unit,
    onAlarmClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    // The count the dialog was opened for, so its text holds steady while it eases out after the
    // selection has already been cleared.
    var deleteCount by remember { mutableIntStateOf(0) }
    val fullScreenAllowed = rememberFullScreenAlarmsAllowed()

    // Back leaves selection mode instead of closing the app.
    BackHandler(enabled = uiState.selectionMode) { onIntent(AlarmsIntent.ClearSelection) }
    val navClearance = brNavClearance()
    val listState = rememberLazyListState()
    val collapse = rememberTopBarCollapse(listState)
    val title = when {
        !uiState.selectionMode -> "Alarms"
        uiState.selectedCount == 0 -> "Select alarms"
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
                uiState.alarms.isEmpty() -> item(key = "empty") {
                    Box(
                        Modifier.fillParentMaxHeight(0.75f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = painterResource(R.drawable.ic_alarm),
                            title = "No alarms yet",
                            message = "Alarms you set will show up here. Tap + to add your first one."
                        )
                    }
                }
                else -> {
                    if (!uiState.selectionMode && !fullScreenAllowed) {
                        item(key = "full_screen_warning") {
                            FullScreenAlarmWarning(
                                Modifier.animateItem().padding(horizontal = BrSpacing.Gutter)
                            )
                        }
                    }
                    val ringing = uiState.ringingAlarm
                    if (!uiState.selectionMode && ringing != null) {
                        item(key = "ringing_now") {
                            RingingNowCard(
                                alarm = ringing,
                                onSnooze = { onIntent(AlarmsIntent.SnoozeRinging) },
                                onDismiss = { onIntent(AlarmsIntent.DismissRinging) },
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = BrSpacing.Gutter)
                                    .revealOnEnter(1)
                            )
                        }
                    }
                    val next = uiState.nextAlarm
                    if (!uiState.selectionMode && next != null && next.id != ringing?.id) {
                        item(key = "next_alarm_banner") {
                            NextAlarmCard(
                                alarm = next,
                                // The screen's one aura belongs to a ringing alarm when there is one.
                                hero = ringing == null,
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = BrSpacing.Gutter)
                                    .revealOnEnter(1)
                            )
                        }
                    }
                    itemsIndexed(uiState.alarms, key = { _, it -> it.id }) { index, alarm ->
                        AlarmCard(
                            alarm = alarm,
                            selectionMode = uiState.selectionMode,
                            isSelected = alarm.id in uiState.selectedIds,
                            onClick = {
                                if (uiState.selectionMode) onIntent(AlarmsIntent.ToggleSelection(alarm.id))
                                else onAlarmClick(alarm.id)
                            },
                            onLongClick = { onIntent(AlarmsIntent.EnterSelection(alarm.id)) },
                            onToggle = { enabled -> onIntent(AlarmsIntent.ToggleAlarm(alarm.id, enabled)) },
                            modifier = Modifier
                                .animateItem()
                                .padding(horizontal = BrSpacing.Gutter)
                                .revealOnEnter(index + 2)
                        )
                    }
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
                label = "alarmsHeaderActions"
            ) { selecting ->
                Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.XS)) {
                    if (selecting) {
                        BrIconButton(
                            icon = rememberVectorPainter(Icons.Rounded.DoneAll),
                            contentDescription = "Select all",
                            onClick = { onIntent(AlarmsIntent.SelectAll) },
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
                            onClick = { onIntent(AlarmsIntent.ClearSelection) },
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
                                    text = "Select alarms",
                                    onClick = {
                                        menuOpen = false
                                        onIntent(AlarmsIntent.StartSelection)
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
                contentDescription = "Add alarm",
                onClick = onAddAlarm
            )
        }
    }

    if (!uiState.isLoading) {
        LiveAlertsHomePrompt(
            asked = uiState.liveAlertsPromptAsked,
            onMarkAsked = { onIntent(AlarmsIntent.MarkLiveAlertsPromptAsked) },
            onPermissionsChanged = { onIntent(AlarmsIntent.RestoreLiveAlerts) }
        )
    }

    // Always composed so it can ease out after Delete or Cancel.
    BrDialog(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        title = if (deleteCount == 1) "Delete alarm?" else "Delete $deleteCount alarms?",
        message = if (deleteCount == 1) "This alarm will be permanently deleted."
        else "These $deleteCount alarms will be permanently deleted.",
        confirmLabel = "Delete",
        onConfirm = {
            showDeleteDialog = false
            onIntent(AlarmsIntent.DeleteSelected)
        },
        destructive = true
    )
}

/**
 * The next alarm to ring. As the [hero] it's the screen's aura card, with the time as a large
 * numeral; while another alarm is ringing (and owns the aura) it steps back to a plain card.
 */
@Composable
private fun NextAlarmCard(alarm: Alarm, hero: Boolean, modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val time = alarm.clockTime
    val day = if (alarm.nextTriggerTimeMillis > 0) relativeDay(alarm.nextTriggerTimeMillis) else null

    if (hero) {
        AuraCard(aura = rememberAura(colors.accent), modifier = modifier.fillMaxWidth()) {
            NextAlarmContent(
                time = time,
                day = day,
                digitsStyle = type.numeral,
                primary = colors.textPrimary,
                secondary = colors.textPrimary.copy(alpha = 0.72f)
            )
        }
    } else {
        BrCard(modifier = modifier.fillMaxWidth()) {
            NextAlarmContent(
                time = time,
                day = day,
                digitsStyle = type.figureLarge,
                primary = colors.textPrimary,
                secondary = colors.textSecondary
            )
        }
    }
}

@Composable
private fun NextAlarmContent(
    time: ClockTime,
    day: String?,
    digitsStyle: TextStyle,
    primary: Color,
    secondary: Color
) {
    val type = BedrockTheme.typography
    Text("Next alarm", style = type.label, color = secondary)
    Spacer(Modifier.height(BrSpacing.XS))
    // Swap the whole line so the time and its AM/PM stay on one baseline
    // (AnimatedContent doesn't expose a baseline to align against).
    AnimatedContent(
        targetState = time,
        transitionSpec = { BrTransitions.swap },
        contentAlignment = Alignment.CenterStart,
        label = "nextAlarmTime"
    ) { t ->
        Row {
            Text(t.digits, style = digitsStyle, color = primary, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(6.dp))
            Text(t.meridiem, style = type.bodyLarge, color = secondary, modifier = Modifier.alignByBaseline())
        }
    }
    if (day != null) {
        SwapText(day, type.body, secondary)
    }
}

@Composable
private fun AlarmCard(
    alarm: Alarm,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val enabled = alarm.isEnabled
    val time = alarm.clockTime
    // Switching an alarm off lets the card sink back instead of snapping grey; a selected card
    // fills with the accent container rather than growing a border.
    val dim = animateFloatAsState(if (enabled || isSelected) 1f else 0.7f, BrMotion.settle(), label = "cardDim")
    val cardColor by animateColorAsState(
        when {
            isSelected -> colors.accentContainer
            enabled -> colors.surface
            else -> colors.surface.copy(alpha = 0.55f)
        },
        BrMotion.settle(),
        label = "cardBg"
    )
    val timeColor by animateColorAsState(if (enabled) colors.textPrimary else colors.textSecondary, BrMotion.settle(), label = "timeColor")
    val metaColor by animateColorAsState(if (enabled) colors.textSecondary else colors.textTertiary, BrMotion.settle(), label = "metaColor")

    BrCard(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = dim.value; compositingStrategy = CompositingStrategy.ModulateAlpha },
        color = cardColor,
        contentPadding = PaddingValues(horizontal = BrSpacing.XL, vertical = BrSpacing.L),
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedVisibility(
                visible = selectionMode,
                enter = BrTransitions.revealStart,
                exit = BrTransitions.concealStart
            ) {
                Row {
                    BrCheckbox(checked = isSelected, onCheckedChange = { onClick() })
                    Spacer(Modifier.width(BrSpacing.M))
                }
            }
            Column(Modifier.weight(1f)) {
                alarm.caption?.let { caption ->
                    Text(
                        caption,
                        style = type.caption,
                        color = metaColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(BrSpacing.XS))
                }
                Row {
                    Text(
                        time.digits,
                        style = type.numeral,
                        color = timeColor,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(Modifier.width(BrSpacing.XS))
                    Text(
                        time.meridiem,
                        style = type.labelSmall,
                        color = metaColor,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Spacer(Modifier.height(BrSpacing.XXS))
                Text(
                    alarm.repeatMode.detailText(),
                    style = type.body,
                    color = metaColor
                )
            }
            AnimatedVisibility(
                visible = !selectionMode,
                enter = BrTransitions.revealStart,
                exit = BrTransitions.concealStart
            ) {
                Row {
                    Spacer(Modifier.width(BrSpacing.M))
                    BrSwitch(checked = enabled, onCheckedChange = onToggle)
                }
            }
        }
    }
}

/**
 * "Ringing now" hero card: the always-available way to stop a sounding alarm from inside the app,
 * even after the full-screen ringer was closed. It's the screen's aura, drifting slowly, with a
 * bell that breathes beside the label.
 */
@Composable
private fun RingingNowCard(
    alarm: RingingAlarm,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val digits = alarm.time.substringBeforeLast(' ', alarm.time)
    val meridiem = alarm.time.substringAfterLast(' ', "").takeIf { it != alarm.time }.orEmpty()

    AuraCard(
        aura = rememberAura(colors.accent),
        modifier = modifier.fillMaxWidth(),
        drift = true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painterResource(R.drawable.ic_bell_ring),
                contentDescription = null,
                tint = colors.textPrimary,
                modifier = Modifier.size(16.dp).breathe()
            )
            Spacer(Modifier.width(BrSpacing.S))
            Text("Ringing now", style = type.label, color = colors.textPrimary.copy(alpha = 0.8f))
        }
        Spacer(Modifier.height(BrSpacing.XS))
        Row {
            Text(
                digits,
                style = type.numeral,
                color = colors.textPrimary,
                modifier = Modifier.alignByBaseline()
            )
            if (meridiem.isNotEmpty()) {
                Spacer(Modifier.width(6.dp))
                Text(
                    meridiem,
                    style = type.bodyLarge,
                    color = colors.textPrimary.copy(alpha = 0.72f),
                    modifier = Modifier.alignByBaseline()
                )
            }
        }
        if (alarm.label.isNotBlank()) {
            Text(
                alarm.label,
                style = type.body,
                color = colors.textPrimary.copy(alpha = 0.72f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(BrSpacing.XL))
        Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
            BrButton(
                text = "Snooze",
                onClick = onSnooze,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.Tonal,
                size = ButtonSize.Medium,
                icon = painterResource(R.drawable.ic_snooze)
            )
            BrButton(
                text = "Dismiss",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.Ink,
                size = ButtonSize.Medium,
                icon = rememberVectorPainter(Icons.Rounded.AlarmOff)
            )
        }
    }
}
