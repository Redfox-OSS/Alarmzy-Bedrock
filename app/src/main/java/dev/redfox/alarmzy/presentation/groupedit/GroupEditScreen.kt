package dev.redfox.alarmzy.presentation.groupedit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.Alarm
import dev.redfox.alarmzy.domain.model.GroupIcon
import dev.redfox.alarmzy.presentation.alarmedit.BottomActionBar
import dev.redfox.alarmzy.presentation.common.clockTime
import dev.redfox.alarmzy.ui.components.GroupColorSwatches
import dev.redfox.alarmzy.ui.components.drawableRes
import dev.redfox.alarmzy.ui.components.parseGroupColor
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrCheckbox
import dev.redfox.bedrock.components.BrDialog
import dev.redfox.bedrock.components.BrDivider
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
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RevealColumn
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing

@Composable
fun GroupEditScreen(
    uiState: GroupEditUiState,
    onIntent: (GroupEditIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    onPickRingtone: () -> Unit = {},
) {
    val colors = BedrockTheme.colors
    var showDeleteDialog by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val collapse = rememberTopBarCollapse(scrollState)
    val title = if (uiState.isNew) "New group" else "Edit group"

    Box(
        modifier
            .fillMaxSize()
            .background(colors.background)
            .imePadding()
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(scrollState)
                // The bottom clears the floating save bar.
                .padding(top = topBarClearance(), bottom = 128.dp)
        ) {
            BrLargeTitle(title = title, collapse = collapse)
            RevealColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BrSpacing.Gutter),
                verticalArrangement = Arrangement.spacedBy(BrSpacing.Section)
            ) {
                Column {
                    SectionHeader("Name")
                    NameField(name = uiState.name, onNameChange = { onIntent(GroupEditIntent.SetName(it)) })
                }

                Column {
                    HeaderWithValue(title = "Group icon", value = uiState.icon?.label ?: "None")
                    IconGrid(
                        selected = uiState.icon,
                        tint = parseGroupColor(uiState.color) ?: colors.accentMain,
                        onSelect = { onIntent(GroupEditIntent.SetIcon(it)) }
                    )
                }

                Column {
                    SectionHeader("Group color")
                    ColorGrid(selected = uiState.color, onSelect = { onIntent(GroupEditIntent.SetColor(it)) })
                }

                Column {
                    SectionHeader("Alarm sound")
                    GroupRingtoneCard(
                        uiState = uiState,
                        onPick = onPickRingtone,
                        onUseSettings = { onIntent(GroupEditIntent.UseSettingsRingtone) }
                    )
                }

                Column {
                    HeaderWithValue(
                        title = "Select alarms for this group",
                        value = if (uiState.selectedAlarmIds.isNotEmpty()) "${uiState.selectedAlarmIds.size} selected" else null,
                        valueColor = colors.accentSoft
                    )
                    if (uiState.allAlarms.isEmpty()) {
                        Text(
                            "No alarms yet. Create an alarm first, then add it to a group.",
                            style = BedrockTheme.typography.body,
                            color = colors.textSecondary
                        )
                    } else {
                        ListGroup {
                            uiState.allAlarms.forEachIndexed { index, alarm ->
                                AlarmSelectionRow(
                                    alarm = alarm,
                                    editingGroupId = uiState.groupId,
                                    isSelected = alarm.id in uiState.selectedAlarmIds,
                                    divider = index < uiState.allAlarms.lastIndex,
                                    onToggle = { onIntent(GroupEditIntent.ToggleAlarmSelection(alarm.id)) }
                                )
                            }
                        }
                    }
                }
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
                        contentDescription = "Group options",
                        onClick = { menuOpen = true },
                        variant = IconButtonVariant.Glass
                    )
                    BrMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        BrMenuItem(
                            text = "Delete group",
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
                text = if (uiState.isNew) "Create group" else "Save group",
                onClick = { onIntent(GroupEditIntent.Save) },
                modifier = Modifier.fillMaxWidth(),
                icon = painterResource(R.drawable.ic_create_folder),
                enabled = uiState.name.isNotBlank(),
                loading = uiState.isSaving
            )
        }
    }

    BrDialog(
        visible = showDeleteDialog,
        onDismiss = { showDeleteDialog = false },
        title = "Delete group?",
        message = "The group will be deleted. Its alarms will be kept and ungrouped.",
        confirmLabel = "Delete",
        onConfirm = {
            showDeleteDialog = false
            onIntent(GroupEditIntent.Delete)
        },
        destructive = true
    )
}

/** A section header with a quiet value at its end ("None", "3 selected"). */
@Composable
private fun HeaderWithValue(title: String, value: String?, valueColor: Color = BedrockTheme.colors.textTertiary) {
    val type = BedrockTheme.typography
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = BrSpacing.S, bottom = BrSpacing.M),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(
            title,
            style = type.label,
            color = BedrockTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (value != null) {
            Spacer(Modifier.width(BrSpacing.M))
            Text(value, style = type.label.copy(fontFeatureSettings = "tnum"), color = valueColor, maxLines = 1)
        }
    }
}

/**
 * The group name: a raised field like Bedrock's text field (its edge warms to the accent while
 * focused) with a clear button that fades in once there's a name.
 */
@Composable
private fun NameField(name: String, onNameChange: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val edge by animateColorAsState(
        if (focused) colors.accentMain.copy(alpha = 0.6f) else colors.line,
        BrMotion.quick(),
        label = "nameEdge"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(BrShapes.Medium)
            .background(colors.surfaceRaised)
            .border(1.dp, edge, BrShapes.Medium)
            .padding(start = BrSpacing.L, end = BrSpacing.XS),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = name,
            onValueChange = onNameChange,
            singleLine = true,
            textStyle = type.bodyLarge.copy(color = colors.textPrimary),
            cursorBrush = SolidColor(colors.accentMain),
            interactionSource = interaction,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier.weight(1f),
            decorationBox = { inner ->
                Box(contentAlignment = Alignment.CenterStart) {
                    if (name.isEmpty()) {
                        Text("e.g. Work, Gym, Weekend", style = type.bodyLarge, color = colors.textTertiary)
                    }
                    inner()
                }
            }
        )
        AnimatedVisibility(
            visible = name.isNotEmpty(),
            enter = fadeIn(tween(BrDurations.Swap, easing = BrMotion.Calm)) +
                scaleIn(tween(BrDurations.Swap, easing = BrMotion.Calm), initialScale = 0.85f),
            exit = fadeOut(tween(BrDurations.Fade)) + scaleOut(tween(BrDurations.Fade), targetScale = 0.85f)
        ) {
            Box(
                Modifier
                    .size(44.dp)
                    .pressable(pressedScale = 0.94f) { onNameChange("") }
                    .semantics { contentDescription = "Clear name" },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(R.drawable.ic_clear),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private const val GridColumns = 6

/**
 * The group icons. The chosen one sits in a quiet fill of the group's colour with a fine ring of
 * it; no glow, no pop.
 */
@Composable
private fun IconGrid(selected: GroupIcon?, tint: Color, onSelect: (GroupIcon?) -> Unit) {
    val colors = BedrockTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 10.dp
        val cell = (maxWidth - gap * (GridColumns - 1)) / GridColumns
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            GroupIcon.entries.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { icon ->
                        val isSel = icon == selected
                        val fill by animateColorAsState(
                            if (isSel) tint.copy(alpha = 0.16f) else colors.surfaceRaised,
                            BrMotion.quick(),
                            label = "iconFill"
                        )
                        val ring by animateColorAsState(
                            if (isSel) tint.copy(alpha = 0.7f) else colors.line,
                            BrMotion.quick(),
                            label = "iconRing"
                        )
                        val glyph by animateColorAsState(
                            if (isSel) tint else colors.textSecondary,
                            BrMotion.quick(),
                            label = "iconGlyph"
                        )
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .semantics {
                                    contentDescription = icon.label
                                    this.selected = isSel
                                }
                                .pressable(
                                    pressedScale = 0.94f,
                                    role = Role.RadioButton,
                                    haptic = HapticFeedbackType.SegmentTick
                                ) { onSelect(if (isSel) null else icon) }
                                .clip(BrShapes.Medium)
                                .background(fill)
                                .border(if (isSel) 1.5.dp else 1.dp, ring, BrShapes.Medium),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(painterResource(icon.drawableRes), contentDescription = null, tint = glyph)
                        }
                    }
                }
            }
        }
    }
}

/**
 * The group colours. The chosen swatch eases in a little, leaving room for a ring around it and
 * a tick on it, so the choice reads without relying on colour alone.
 */
@Composable
private fun ColorGrid(selected: String?, onSelect: (String?) -> Unit) {
    val colors = BedrockTheme.colors
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gap = 14.dp
        val cell = (maxWidth - gap * (GridColumns - 1)) / GridColumns
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            GroupColorSwatches.chunked(GridColumns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { hex ->
                        val swatch = parseGroupColor(hex) ?: colors.surfaceHigh
                        val isSel = hex.equals(selected, ignoreCase = true)
                        val sel = animateFloatAsState(if (isSel) 1f else 0f, BrMotion.quick(), label = "swatchSel")
                        val ringColor = colors.textPrimary
                        Box(
                            modifier = Modifier
                                .size(cell)
                                .semantics {
                                    contentDescription = "Color $hex"
                                    this.selected = isSel
                                }
                                .pressable(
                                    pressedScale = 0.94f,
                                    role = Role.RadioButton,
                                    haptic = HapticFeedbackType.SegmentTick
                                ) { onSelect(if (isSel) null else hex) }
                                .drawBehind {
                                    val a = sel.value
                                    if (a > 0f) {
                                        val stroke = 2.dp.toPx()
                                        drawCircle(
                                            color = ringColor.copy(alpha = a),
                                            radius = size.minDimension / 2f - stroke / 2f,
                                            style = Stroke(stroke)
                                        )
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val s = 1f - 0.2f * sel.value
                                        scaleX = s
                                        scaleY = s
                                    }
                                    .clip(CircleShape)
                                    .background(swatch)
                            )
                            Icon(
                                painterResource(R.drawable.ic_check_bold),
                                contentDescription = null,
                                tint = colors.textPrimary,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { alpha = sel.value }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** One alarm in the member list: the whole row toggles its checkbox. */
@Composable
private fun AlarmSelectionRow(
    alarm: Alarm,
    editingGroupId: Long?,
    isSelected: Boolean,
    divider: Boolean,
    onToggle: () -> Unit
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val time = alarm.clockTime
    val inOtherGroup = alarm.groupId != null && alarm.groupId != editingGroupId
    val membership = when {
        alarm.groupId == null -> "Unassigned"
        !inOtherGroup -> "In this group"
        else -> "Currently in: ${alarm.groupName}"
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pressable(pressedScale = 0.99f, role = Role.Checkbox, onClick = onToggle)
                .semantics { toggleableState = ToggleableState(isSelected) }
                .heightIn(min = 64.dp)
                .padding(horizontal = BrSpacing.L, vertical = BrSpacing.M),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
        ) {
            BrCheckbox(checked = isSelected, onCheckedChange = { onToggle() })
            Column(Modifier.weight(1f)) {
                Row {
                    Text(
                        time.digits,
                        style = type.figure.copy(fontWeight = FontWeight.Medium),
                        color = colors.textPrimary,
                        modifier = Modifier.alignByBaseline()
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        time.meridiem,
                        style = type.caption,
                        color = colors.textSecondary,
                        modifier = Modifier.alignByBaseline()
                    )
                    if (alarm.label.isNotBlank()) {
                        Spacer(Modifier.width(BrSpacing.S))
                        Text(
                            alarm.label,
                            style = type.body,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(alarm.repeatMode.displayName, style = type.caption, color = colors.textSecondary)
                    Text(" · ", style = type.caption, color = colors.textTertiary)
                    Text(
                        membership,
                        style = type.caption,
                        color = if (inOtherGroup) colors.caution else colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        // Lines up with the text, past the checkbox, like a list row's hairline.
        if (divider) BrDivider(inset = BrSpacing.L + 32.dp + BrSpacing.M)
    }
}

/**
 * The group's alarm sound. Starts on "Same as Settings"; picking a sound overrides it for every
 * alarm in the group, and "Use Settings sound" reverts to following Settings.
 */
@Composable
private fun GroupRingtoneCard(uiState: GroupEditUiState, onPick: () -> Unit, onUseSettings: () -> Unit) {
    val colors = BedrockTheme.colors
    val custom = uiState.ringtoneUri != null
    ListGroup {
        ListRow(
            title = if (custom) uiState.ringtoneName ?: "Default" else uiState.settingsRingtoneName,
            subtitle = if (custom) "Only for this group" else "Same as Settings",
            leading = {
                IconTile(
                    painterResource(R.drawable.ic_bell),
                    tint = if (custom) colors.accentSoft else colors.textSecondary,
                    container = if (custom) colors.accentContainer else colors.surfaceHigh
                )
            },
            chevron = true,
            onClick = onPick
        )
        AnimatedVisibility(visible = custom, enter = BrTransitions.expandIn, exit = BrTransitions.collapseOut) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = BrSpacing.L, end = BrSpacing.L, bottom = BrSpacing.M),
                horizontalArrangement = Arrangement.End
            ) {
                BrButton(
                    text = "Use Settings sound",
                    onClick = onUseSettings,
                    variant = ButtonVariant.Tonal,
                    size = ButtonSize.Small
                )
            }
        }
    }
}
