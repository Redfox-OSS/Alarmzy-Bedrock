package dev.redfox.alarmzy.presentation.alarmedit

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AlarmGroup
import dev.redfox.alarmzy.domain.model.RepeatMode
import dev.redfox.alarmzy.presentation.common.shortTime
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrChip
import dev.redfox.bedrock.components.BrDayChip
import dev.redfox.bedrock.components.BrDialog
import dev.redfox.bedrock.components.BrDialogFrame
import dev.redfox.bedrock.components.BrIconButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrMenu
import dev.redfox.bedrock.components.BrMenuItem
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.IconButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.SegmentedControl
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RevealColumn
import dev.redfox.bedrock.motion.RevealScope
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

private val SeriesIntervals = listOf(5, 10, 15, 20, 30, 60)

/**
 * Height of [BottomActionBar] above the navigation bar (a large button and its padding), for
 * screens to clear it at the end of their scrolling content.
 */
internal val BottomActionBarHeight = 54.dp + BrSpacing.Gutter + BrSpacing.L

@Composable
fun AlarmEditScreen(
    uiState: AlarmEditUiState,
    onIntent: (AlarmEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    var showEndTimePicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val collapse = rememberTopBarCollapse(scrollState)
    val title = if (uiState.isNew) "New alarm" else "Edit alarm"
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        BottomActionBarHeight + BrSpacing.XL

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(scrollState)
                .padding(top = topBarClearance(), bottom = bottomClearance)
        ) {
            BrLargeTitle(title = title, collapse = collapse)

            if (uiState.isNew) {
                SegmentedControl(
                    options = listOf("Single", "Series"),
                    selectedIndex = if (uiState.isSeries) 1 else 0,
                    onSelect = { onIntent(AlarmEditIntent.ToggleSeriesMode(it == 1)) },
                    modifier = Modifier.padding(horizontal = BrSpacing.Gutter)
                )
                Spacer(Modifier.height(BrSpacing.XL))
            }

            AnimatedVisibility(
                visible = uiState.isSeries,
                enter = BrTransitions.expandIn,
                exit = BrTransitions.collapseOut
            ) {
                Text(
                    "Start time",
                    style = type.label,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = BrSpacing.M)
                )
            }

            TimeDialPicker(
                hour = uiState.hour,
                minute = uiState.minute,
                onTimeChange = { h, m -> onIntent(AlarmEditIntent.SetTime(h, m)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BrSpacing.Gutter)
            )
            Spacer(Modifier.height(BrSpacing.Section))

            AnimatedVisibility(
                visible = uiState.isSeries,
                enter = BrTransitions.expandIn,
                exit = BrTransitions.collapseOut
            ) {
                SeriesSection(
                    uiState = uiState,
                    onEditEnd = { showEndTimePicker = true },
                    onIntervalSelected = { onIntent(AlarmEditIntent.SetSeriesInterval(it)) }
                )
            }

            RepeatSection(
                repeatMode = uiState.repeatMode,
                onRepeatChange = { onIntent(AlarmEditIntent.SetRepeatMode(it)) }
            )
            Spacer(Modifier.height(BrSpacing.Section))

            ListGroup(Modifier.padding(horizontal = BrSpacing.Gutter)) {
                if (uiState.availableGroups.isNotEmpty()) {
                    GroupRow(
                        selectedGroupId = uiState.selectedGroupId,
                        groups = uiState.availableGroups,
                        onSelect = { onIntent(AlarmEditIntent.SetGroup(it)) }
                    )
                }
                LabelRow(
                    label = uiState.label,
                    onLabelChange = { onIntent(AlarmEditIntent.SetLabel(it)) }
                )
            }
        }

        BrTopBar(
            title = title,
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onNavigateBack
        ) {
            if (!uiState.isNew) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    BrIconButton(
                        icon = painterResource(R.drawable.ic_more_vert),
                        contentDescription = "Alarm options",
                        onClick = { menuOpen = true },
                        variant = IconButtonVariant.Glass
                    )
                    BrMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        BrMenuItem(
                            text = "Delete alarm",
                            onClick = {
                                menuOpen = false
                                showDeleteDialog = true
                            },
                            destructive = true
                        )
                    }
                }
            }
        }

        BottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BrButton(
                text = when {
                    uiState.isSeries -> "Create ${uiState.seriesCount} alarms"
                    uiState.isNew -> "Create alarm"
                    else -> "Save changes"
                },
                onClick = { onIntent(AlarmEditIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                icon = if (uiState.isSeries) painterResource(R.drawable.ic_alarm_add) else null,
                // While saving the label becomes a spinner; loading also blocks a second tap.
                enabled = uiState.canSave || uiState.isSaving,
                loading = uiState.isSaving
            )
        }
    }

    // Overlays stay composed and are driven by `visible`, so their exits play in full.
    TimePickerDialog(
        visible = showEndTimePicker,
        title = "Ends at",
        initialHour = uiState.seriesEndHour,
        initialMinute = uiState.seriesEndMinute,
        onConfirm = { h, m ->
            onIntent(AlarmEditIntent.SetSeriesEnd(h, m))
            showEndTimePicker = false
        },
        onDismiss = { showEndTimePicker = false }
    )

    // Dismissing clears the times; keep the last ones so the message doesn't empty mid-exit.
    val lastDuplicates = remember { arrayOf(emptyList<String>()) }
    if (uiState.duplicateTimes.isNotEmpty()) lastDuplicates[0] = uiState.duplicateTimes
    BrDialog(
        visible = uiState.showDuplicateDialog,
        onDismiss = { onIntent(AlarmEditIntent.DismissDuplicateDialog) },
        title = "Duplicate alarms",
        message = "Alarms already exist at ${lastDuplicates[0].joinToString(", ")}. Create the series anyway?",
        confirmLabel = "Create",
        onConfirm = { onIntent(AlarmEditIntent.ConfirmSave) }
    )

    BrDialog(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        title = "Delete alarm?",
        message = "This alarm will be permanently deleted.",
        confirmLabel = "Delete",
        onConfirm = {
            showDeleteDialog = false
            onIntent(AlarmEditIntent.Delete)
        },
        destructive = true
    )
}

/**
 * The footer holding a detail screen's primary action above the gesture bar. The page fades into
 * the background behind it, so content scrolling underneath never collides with the button. It
 * slides away while the keyboard is open so it never covers the field being edited.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    AnimatedVisibility(
        visible = !WindowInsets.isImeVisible,
        enter = slideInVertically(BrMotion.settle(IntOffset.VisibilityThreshold)) { it } +
            fadeIn(tween(BrDurations.Enter, easing = BrMotion.Calm)),
        exit = slideOutVertically(BrMotion.quick(IntOffset.VisibilityThreshold)) { it } +
            fadeOut(tween(BrDurations.Fade)),
        modifier = modifier
    ) {
        val bg = BedrockTheme.colors.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(0f to bg.copy(alpha = 0f), 0.35f to bg.copy(alpha = 0.94f), 1f to bg))
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(start = BrSpacing.Gutter, end = BrSpacing.Gutter, top = BrSpacing.Gutter, bottom = BrSpacing.L)
        ) {
            content()
        }
    }
}

@Composable
private fun SeriesSection(
    uiState: AlarmEditUiState,
    onEditEnd: () -> Unit,
    onIntervalSelected: (Int) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = BrSpacing.Gutter)
            .padding(bottom = BrSpacing.Section),
        verticalArrangement = Arrangement.spacedBy(BrSpacing.M)
    ) {
        ListGroup {
            ListRow(
                title = "Ends at",
                leading = { RowIcon(R.drawable.ic_ends_at) },
                value = shortTime(uiState.seriesEndHour, uiState.seriesEndMinute),
                trailing = { PickerChevron() },
                divider = true,
                onClick = onEditEnd
            )
            var intervalMenuOpen by remember { mutableStateOf(false) }
            Box {
                ListRow(
                    title = "Interval",
                    leading = { RowIcon(R.drawable.ic_interval) },
                    value = "${uiState.seriesIntervalMinutes} min",
                    trailing = { PickerChevron() },
                    onClick = { intervalMenuOpen = true }
                )
                // Anchored at the row's bottom end, so the menu opens under the value it changes.
                Box(Modifier.align(Alignment.BottomEnd)) {
                    BrMenu(expanded = intervalMenuOpen, onDismissRequest = { intervalMenuOpen = false }) {
                        SeriesIntervals.forEach { minutes ->
                            BrMenuItem(
                                text = "Every $minutes min",
                                onClick = {
                                    onIntervalSelected(minutes)
                                    intervalMenuOpen = false
                                },
                                selected = minutes == uiState.seriesIntervalMinutes
                            )
                        }
                    }
                }
            }
        }

        SeriesSummary(uiState)
    }
}

/** What the series will create, in an accent container; turns critical when it can't be made. */
@Composable
private fun SeriesSummary(uiState: AlarmEditUiState) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val valid = uiState.seriesRangeValid && uiState.seriesWithinCap
    val tone by animateColorAsState(
        if (valid) colors.accentSoft else colors.critical,
        BrMotion.quick(),
        label = "summaryTone"
    )
    val fill by animateColorAsState(
        if (valid) colors.accentContainer else colors.critical.copy(alpha = 0.12f),
        BrMotion.quick(),
        label = "summaryFill"
    )
    // Body text with tabular figures: the counts and times change as you adjust the series.
    val body = type.body.copy(fontFeatureSettings = type.figure.fontFeatureSettings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(BrMotion.settle(IntSize.VisibilityThreshold))
            .clip(BrShapes.Large)
            .background(fill)
            .padding(horizontal = BrSpacing.L, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
    ) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(painterResource(R.drawable.ic_info_small), contentDescription = null, tint = tone)
        }
        when {
            !uiState.seriesRangeValid -> Text(
                "End time must be after the start time",
                style = body,
                color = colors.critical
            )
            !uiState.seriesWithinCap -> Text(
                "That's ${uiState.seriesCount} alarms — the limit is ${AlarmEditUiState.MAX_SERIES_ALARMS}. Try a longer interval.",
                style = body,
                color = colors.critical
            )
            else -> Text(
                buildAnnotatedString {
                    append("Creates ")
                    withStyle(SpanStyle(color = colors.accentSoft, fontWeight = FontWeight.Medium)) {
                        append("${uiState.seriesCount} alarms")
                    }
                    append(
                        " · ${shortTime(uiState.hour, uiState.minute)}–" +
                            "${shortTime(uiState.seriesEndHour, uiState.seriesEndMinute)} every ${uiState.seriesIntervalMinutes} min"
                    )
                },
                style = body,
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun RepeatSection(repeatMode: RepeatMode, onRepeatChange: (RepeatMode) -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader("Repeat", Modifier.padding(horizontal = BrSpacing.Gutter))
        // Scrolls edge to edge; the padding inside the scroll lines the first chip up with the gutter.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = BrSpacing.Gutter),
            horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)
        ) {
            listOf(
                "Once" to RepeatMode.OneTime,
                "Daily" to RepeatMode.Daily,
                "Weekdays" to RepeatMode.Weekdays,
                "Weekends" to RepeatMode.Weekends
            ).forEach { (label, mode) ->
                BrChip(label = label, selected = repeatMode == mode, onClick = { onRepeatChange(mode) })
            }
            val isCustom = repeatMode is RepeatMode.Custom
            BrChip(
                label = "Custom",
                selected = isCustom,
                onClick = {
                    if (!isCustom) onRepeatChange(RepeatMode.Custom(repeatMode.toDays()))
                }
            )
        }
        AnimatedVisibility(
            visible = repeatMode is RepeatMode.Custom,
            enter = BrTransitions.expandIn,
            exit = BrTransitions.collapseOut
        ) {
            // Fresh scope so the days deal in left to right each time Custom opens.
            RevealScope {
                val days = repeatMode.toDays()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = BrSpacing.Gutter)
                        .padding(top = BrSpacing.L),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DayOfWeek.entries.forEachIndexed { i, day ->
                        BrDayChip(
                            label = day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            selected = day in days,
                            onClick = {
                                val newDays = if (day in days) days - day else days + day
                                onRepeatChange(RepeatMode.Custom(newDays))
                            },
                            modifier = Modifier.revealOnEnter(i, offsetY = 10.dp),
                            contentDescription = day.getDisplayName(TextStyle.FULL, Locale.getDefault())
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GroupRow(selectedGroupId: Long?, groups: List<AlarmGroup>, onSelect: (Long?) -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    var open by remember { mutableStateOf(false) }
    val selected = groups.firstOrNull { it.id == selectedGroupId }
    Box {
        ListRow(
            title = "Group",
            leading = { RowIcon(R.drawable.ic_folder) },
            trailing = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)
                ) {
                    if (selected != null) {
                        GroupSwatch(parseGroupColor(selected.color) ?: colors.textSecondary)
                    }
                    Text(
                        selected?.name ?: "None",
                        style = type.body,
                        color = if (selected == null) colors.textTertiary else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 160.dp)
                    )
                    PickerChevron()
                }
            },
            divider = true,
            onClick = { open = true }
        )
        Box(Modifier.align(Alignment.BottomEnd)) {
            BrMenu(expanded = open, onDismissRequest = { open = false }) {
                GroupMenuItem(
                    name = "None",
                    swatch = null,
                    selected = selectedGroupId == null,
                    onClick = { onSelect(null); open = false }
                )
                groups.forEach { group ->
                    GroupMenuItem(
                        name = group.name,
                        swatch = parseGroupColor(group.color) ?: colors.textSecondary,
                        selected = group.id == selectedGroupId,
                        onClick = { onSelect(group.id); open = false }
                    )
                }
            }
        }
    }
}

/**
 * A [BrMenuItem] with the group's colour swatch leading it ([BrMenuItem]'s icon is a tinted
 * painter, which can't carry a group's own colour). A null [swatch] draws an empty ring, for "None".
 */
@Composable
private fun GroupMenuItem(name: String, swatch: Color?, selected: Boolean, onClick: () -> Unit) {
    val colors = BedrockTheme.colors
    val tint = if (selected) colors.accentSoft else colors.textPrimary
    Row(
        modifier = Modifier
            .widthIn(min = 200.dp)
            .pressable(pressedScale = 0.98f, onClick = onClick)
            .semantics { this.selected = selected }
            .padding(horizontal = BrSpacing.L, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        GroupSwatch(swatch)
        Spacer(Modifier.width(BrSpacing.M))
        Text(
            name,
            style = BedrockTheme.typography.bodyLarge,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Spacer(Modifier.width(BrSpacing.M))
            Icon(
                painterResource(R.drawable.ic_check),
                contentDescription = null,
                tint = colors.accentSoft,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** A group's colour dot; null draws an empty ring. */
@Composable
private fun GroupSwatch(color: Color?) {
    val base = Modifier
        .size(10.dp)
        .clip(CircleShape)
    if (color != null) {
        Box(base.background(color))
    } else {
        Box(base.border(1.5.dp, BedrockTheme.colors.textTertiary, CircleShape))
    }
}

/** The label, edited in place as the last row of the details group. */
@Composable
private fun LabelRow(label: String, onLabelChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 54.dp)
            .padding(horizontal = BrSpacing.L, vertical = BrSpacing.M),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
    ) {
        RowIcon(R.drawable.ic_label)
        BasicTextField(
            value = label,
            onValueChange = onLabelChange,
            singleLine = true,
            textStyle = type.label.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentMain),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (label.isEmpty()) {
                        Text("Label", style = type.label, color = colors.textTertiary)
                    }
                    inner()
                }
            }
        )
    }
}

@Composable
private fun RowIcon(@DrawableRes icon: Int) {
    IconTile(icon = painterResource(icon), tint = BedrockTheme.colors.textSecondary)
}

/** Trailing hint on rows that open a menu or picker for their value. */
@Composable
private fun PickerChevron() {
    Icon(
        painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = BedrockTheme.colors.textTertiary,
        modifier = Modifier.size(18.dp)
    )
}

/** A dialog holding a [TimeDialPicker]; Done hands back the picked time. */
@Composable
private fun TimePickerDialog(
    visible: Boolean,
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    BrDialogFrame(visible = visible, onDismiss = onDismiss) {
        // Remembered inside the frame, which leaves composition once closed, so every opening
        // starts from the current end time.
        var hour by remember { mutableIntStateOf(initialHour) }
        var minute by remember { mutableIntStateOf(initialMinute) }
        Text(title, style = type.headline, color = colors.textPrimary)
        Spacer(Modifier.height(BrSpacing.L))
        TimeDialPicker(
            hour = hour,
            minute = minute,
            onTimeChange = { h, m -> hour = h; minute = m },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            dialSize = 216.dp,
            tileSize = 70.dp
        )
        Spacer(Modifier.height(BrSpacing.XL))
        Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
            BrButton(
                text = "Cancel",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                variant = ButtonVariant.Tonal,
                size = ButtonSize.Medium
            )
            BrButton(
                text = "Done",
                onClick = { onConfirm(hour, minute) },
                modifier = Modifier.weight(1f),
                size = ButtonSize.Medium
            )
        }
    }
}
