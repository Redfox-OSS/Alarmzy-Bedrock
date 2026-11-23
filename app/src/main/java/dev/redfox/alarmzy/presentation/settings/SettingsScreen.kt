package dev.redfox.alarmzy.presentation.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.alarm.BackgroundRestrictions
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.alarmzy.presentation.livealerts.BackgroundRunFooter
import dev.redfox.alarmzy.presentation.livealerts.BackgroundRunRows
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsSettingsFooter
import dev.redfox.alarmzy.presentation.livealerts.LiveAlertsSettingsRows
import dev.redfox.bedrock.components.BrChip
import dev.redfox.bedrock.components.BrDivider
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrSwitch
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.SegmentedControl
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RevealColumn
import dev.redfox.bedrock.navigation.brNavClearance
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrSpacing

private val SnoozeOptions = listOf(5, 10, 15, 20, 30)

/** Where a [ListRow]'s text starts when its leading slot is a default 34dp [IconTile]. */
private val RowTextInset: Dp = BrSpacing.L + 34.dp + BrSpacing.M

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onIntent: (SettingsIntent) -> Unit,
    overlayPermissionGranted: Boolean = true,
    onRequestOverlayPermission: () -> Unit = {},
    onPickRingtone: () -> Unit,
    onCustomizeAlarmScreen: () -> Unit = {},
    onCustomizeWidget: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val scrollState = rememberScrollState()
    val collapse = rememberTopBarCollapse(scrollState)

    Box(modifier.fillMaxSize().background(colors.background)) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(scrollState)
                .padding(top = topBarClearance(), bottom = brNavClearance())
        ) {
            BrLargeTitle(title = "Settings", collapse = collapse)

            // Only shown while the overlay permission is missing: nothing actionable once granted.
            if (!overlayPermissionGranted) {
                SettingsSection("Permissions") {
                    ListGroup {
                        ListRow(
                            title = "Display over other apps",
                            subtitle = "Required for the full-screen alarm screen. Tap to grant.",
                            leading = {
                                IconTile(
                                    rememberVectorPainter(Icons.Rounded.WarningAmber),
                                    tint = colors.caution,
                                    container = colors.caution.copy(alpha = 0.16f)
                                )
                            },
                            chevron = true,
                            onClick = onRequestOverlayPermission
                        )
                    }
                }
            }

            SettingsSection("Appearance") {
                ListGroup {
                    ThemeVariantRow(
                        selected = uiState.darkVariant,
                        onSelect = { onIntent(SettingsIntent.SetDarkVariant(it)) }
                    )
                    BrDivider(inset = RowTextInset)
                    AccentColorPicker(
                        selected = uiState.accentColor,
                        onSelect = { onIntent(SettingsIntent.SetAccentColor(it)) }
                    )
                }
            }

            SettingsSection("Alarm settings") {
                ListGroup {
                    ListRow(
                        title = "Customize alarm screen",
                        subtitle = "Background, buttons, clock position and size",
                        leading = { IconTile(painterResource(R.drawable.ic_tune)) },
                        chevron = true,
                        divider = true,
                        onClick = onCustomizeAlarmScreen
                    )
                    ListRow(
                        title = "Ringtone",
                        subtitle = uiState.defaultRingtoneName,
                        leading = { IconTile(painterResource(R.drawable.ic_bell)) },
                        chevron = true,
                        divider = true,
                        onClick = onPickRingtone
                    )
                    SnoozeRow(
                        selectedMinutes = uiState.defaultSnoozeDuration,
                        onSelect = { onIntent(SettingsIntent.SetDefaultSnoozeDuration(it)) }
                    )
                    BrDivider(inset = RowTextInset)
                    ListRow(
                        title = "Vibration",
                        subtitle = "Vibrate when an alarm rings",
                        leading = { IconTile(painterResource(R.drawable.ic_vibration)) },
                        trailing = {
                            BrSwitch(
                                checked = uiState.defaultVibration,
                                onCheckedChange = { onIntent(SettingsIntent.SetDefaultVibration(it)) }
                            )
                        },
                        divider = true,
                        onClick = { onIntent(SettingsIntent.SetDefaultVibration(!uiState.defaultVibration)) }
                    )
                    ListRow(
                        title = "Gradual volume increase",
                        subtitle = "Start quiet and get louder",
                        leading = { IconTile(painterResource(R.drawable.ic_volume)) },
                        trailing = {
                            BrSwitch(
                                checked = uiState.defaultGradualVolume,
                                onCheckedChange = { onIntent(SettingsIntent.SetDefaultGradualVolume(it)) }
                            )
                        },
                        onClick = { onIntent(SettingsIntent.SetDefaultGradualVolume(!uiState.defaultGradualVolume)) }
                    )
                }
            }

            SettingsSection("Widget") {
                ListGroup {
                    ListRow(
                        title = "Home-screen widget",
                        subtitle = "Date, time and weather: layout, size, tap actions",
                        leading = { IconTile(painterResource(R.drawable.ic_widgets)) },
                        chevron = true,
                        onClick = onCustomizeWidget
                    )
                }
            }

            SettingsSection("Live alerts") {
                ListGroup {
                    LiveAlertsSettingsRows(onPermissionsChanged = { onIntent(SettingsIntent.RestoreLiveAlerts) })
                }
                LiveAlertsSettingsFooter()
            }

            if (BackgroundRestrictions.isXiaomi()) {
                SettingsSection("Background running") {
                    ListGroup { BackgroundRunRows() }
                    BackgroundRunFooter()
                }
            }

            SettingsSection("About") {
                ListGroup {
                    ListRow(
                        title = "Alarmzy",
                        subtitle = "Version ${appVersionName()}",
                        leading = { IconTile(painterResource(R.drawable.ic_info)) }
                    )
                }
            }
        }

        BrTopBar(title = "Settings", collapse = collapse, modifier = Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun appVersionName(): String {
    val context = LocalContext.current
    return remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull() ?: "1.0"
    }
}

/** A titled group on the page: the gutter at the edges, and the section gap below it. */
@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = BrSpacing.Gutter)
            .padding(bottom = BrSpacing.XXL)
    ) {
        SectionHeader(title)
        content()
    }
}

/** The chevron on a row that opens in place. It turns on the settling spring, read in draw. */
@Composable
private fun ExpandChevron(expanded: Boolean) {
    val rotation = animateFloatAsState(if (expanded) 180f else 0f, BrMotion.settle(), label = "expandChevron")
    Icon(
        painterResource(R.drawable.ic_chevron_down),
        contentDescription = null,
        tint = BedrockTheme.colors.textTertiary,
        modifier = Modifier
            .size(18.dp)
            .graphicsLayer { rotationZ = rotation.value }
    )
}

@Composable
private fun ThemeVariantRow(selected: DarkVariant, onSelect: (DarkVariant) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ListRow(
            title = "Theme",
            subtitle = selected.displayName,
            leading = { IconTile(painterResource(R.drawable.ic_palette)) },
            trailing = { ExpandChevron(expanded) },
            modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            onClick = { expanded = !expanded }
        )
        AnimatedVisibility(visible = expanded, enter = BrTransitions.expandIn, exit = BrTransitions.collapseOut) {
            SegmentedControl(
                options = DarkVariant.entries.map { it.displayName },
                selectedIndex = DarkVariant.entries.indexOf(selected),
                onSelect = { onSelect(DarkVariant.entries[it]) },
                // Full card width, so "Pitch black" never has to ellipsize.
                modifier = Modifier.padding(start = BrSpacing.L, end = BrSpacing.L, bottom = BrSpacing.L)
            )
        }
    }
}

@Composable
private fun AccentColorPicker(selected: AccentColor, onSelect: (AccentColor) -> Unit) {
    Column {
        ListRow(
            title = "Accent color",
            subtitle = selected.displayName,
            leading = { IconTile(painterResource(R.drawable.ic_format_paint)) }
        )
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = BrSpacing.S, end = BrSpacing.S, bottom = BrSpacing.M)
                .selectableGroup(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AccentColor.entries.forEach { accent ->
                AccentSwatch(
                    color = Color(accent.swatch),
                    name = accent.displayName,
                    selected = accent == selected,
                    onClick = { onSelect(accent) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * One accent choice: a round swatch. When chosen, a calm 2dp ring eases in around it while the
 * swatch settles a little smaller inside the ring (the catalog's Swatch). No pop, no glow.
 */
@Composable
private fun AccentSwatch(
    color: Color,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ringColor = BedrockTheme.colors.textPrimary
    val ring = animateFloatAsState(if (selected) 1f else 0f, BrMotion.settle(), label = "swatchRing")
    Box(
        modifier
            .height(52.dp)
            .pressable(role = Role.RadioButton, pressedScale = 0.94f, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .semantics {
                contentDescription = "$name accent"
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(44.dp)
                .graphicsLayer { alpha = ring.value }
                .border(2.dp, ringColor.copy(alpha = 0.85f), CircleShape)
        )
        Box(
            Modifier
                .size(40.dp)
                .graphicsLayer {
                    val s = 1f - 0.16f * ring.value
                    scaleX = s
                    scaleY = s
                }
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
private fun SnoozeRow(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column {
        ListRow(
            title = "Snooze duration",
            value = "$selectedMinutes min",
            leading = { IconTile(painterResource(R.drawable.ic_snooze)) },
            trailing = { ExpandChevron(expanded) },
            modifier = Modifier.semantics { stateDescription = if (expanded) "Expanded" else "Collapsed" },
            onClick = { expanded = !expanded }
        )
        AnimatedVisibility(visible = expanded, enter = BrTransitions.expandIn, exit = BrTransitions.collapseOut) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(start = RowTextInset, end = BrSpacing.L, bottom = BrSpacing.L),
                horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)
            ) {
                SnoozeOptions.forEach { minutes ->
                    BrChip(
                        label = "$minutes min",
                        selected = minutes == selectedMinutes,
                        onClick = { onSelect(minutes) }
                    )
                }
            }
        }
    }
}
