package dev.redfox.alarmzy.presentation.ringtone

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.presentation.alarmedit.BottomActionBar
import dev.redfox.alarmzy.presentation.alarmedit.BottomActionBarHeight
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrIconButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrRadio
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.IconButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing

@Composable
fun RingtonePickerScreen(
    uiState: RingtonePickerUiState,
    onIntent: (RingtonePickerIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = BedrockTheme.colors
    val pickAudioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            // Persist read access so the alarm can still play this file later
            // (across process death and reboots).
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onIntent(RingtonePickerIntent.AddCustomRingtone(uri.toString()))
        }
    }
    val listState = rememberLazyListState()
    val collapse = rememberTopBarCollapse(listState)
    val bottomClearance = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
        BottomActionBarHeight + BrSpacing.XL
    val count = uiState.ringtones.size

    Box(modifier.fillMaxSize().background(colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().glassSource(),
            contentPadding = PaddingValues(top = topBarClearance(), bottom = bottomClearance)
        ) {
            item(key = "title") {
                BrLargeTitle(title = "Ringtone", collapse = collapse)
            }

            item(key = "choose_from_device") {
                Column(Modifier.padding(horizontal = BrSpacing.Gutter)) {
                    ListGroup(Modifier.revealOnEnter(0)) {
                        ListRow(
                            title = "Choose from device",
                            subtitle = "Pick any audio file from your device",
                            leading = {
                                IconTile(
                                    icon = rememberVectorPainter(Icons.Rounded.LibraryMusic),
                                    tint = colors.accentSoft,
                                    container = colors.accentContainer
                                )
                            },
                            chevron = true,
                            onClick = { pickAudioLauncher.launch(arrayOf("audio/*")) }
                        )
                    }
                    Spacer(Modifier.height(BrSpacing.Section))
                    SectionHeader("Sounds", Modifier.revealOnEnter(1))
                }
            }

            itemsIndexed(uiState.ringtones, key = { _, it -> it.uri }) { index, ringtone ->
                // Rows draw as one continuous group: only the outer corners are rounded.
                val segment = when {
                    count == 1 -> Segment.Single
                    index == 0 -> Segment.First
                    index == count - 1 -> Segment.Last
                    else -> Segment.Middle
                }
                RingtoneRow(
                    modifier = Modifier.revealOnEnter(index + 2),
                    ringtone = ringtone,
                    isSelected = ringtone.uri == uiState.selectedUri,
                    isPlaying = ringtone.uri == uiState.playingUri && uiState.isPlaying,
                    segment = segment,
                    showDivider = index < count - 1,
                    onSelect = { onIntent(RingtonePickerIntent.SelectRingtone(ringtone.uri, ringtone.name)) },
                    onTogglePreview = { playing ->
                        if (playing) onIntent(RingtonePickerIntent.StopPreview)
                        else onIntent(RingtonePickerIntent.PreviewRingtone(ringtone.uri))
                    }
                )
            }
        }

        BrTopBar(
            title = "Ringtone",
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onNavigateBack
        )

        BottomActionBar(modifier = Modifier.align(Alignment.BottomCenter)) {
            BrButton(
                text = "Use this sound",
                onClick = { onIntent(RingtonePickerIntent.Confirm) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * One sound: the whole row is the radio target, with a radio showing the choice and a preview
 * button that plays or stops the sound.
 */
@Composable
private fun RingtoneRow(
    ringtone: RingtoneItem,
    isSelected: Boolean,
    isPlaying: Boolean,
    segment: Segment,
    showDivider: Boolean,
    onSelect: () -> Unit,
    onTogglePreview: (playing: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = BedrockTheme.colors
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = BrSpacing.Gutter)
            .groupSegment(segment, fill = colors.surface, edge = colors.line)
    ) {
        ListRow(
            title = ringtone.name,
            modifier = Modifier
                .pressable(
                    role = Role.RadioButton,
                    pressedScale = 0.99f,
                    haptic = if (isSelected) null else HapticFeedbackType.SegmentTick,
                    onClick = onSelect
                )
                .semantics { selected = isSelected },
            subtitle = if (ringtone.isCustom) "From device" else null,
            leading = {
                // The row carries the radio semantics; the radio itself is only the indicator
                // (still tappable, doing the same thing), so screen readers hear one control.
                BrRadio(
                    selected = isSelected,
                    onClick = onSelect,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            },
            trailing = {
                BrIconButton(
                    icon = rememberVectorPainter(if (isPlaying) Icons.Rounded.Stop else Icons.Rounded.PlayArrow),
                    contentDescription = if (isPlaying) "Stop preview" else "Preview",
                    onClick = { onTogglePreview(isPlaying) },
                    variant = IconButtonVariant.Tonal,
                    size = 36.dp,
                    tint = if (isPlaying) colors.accentSoft else colors.textSecondary
                )
            },
            divider = showDivider
        )
    }
}

/** Where a row sits in its group, which decides which corners it rounds. */
private enum class Segment { Single, First, Middle, Last }

/**
 * Draws this row's slice of one continuous grouped card, matching [ListGroup] (surface fill and a
 * hairline edge, rounded only at the group's outer corners), so a lazy list can look like a
 * [ListGroup] without composing every row at once. Each slice draws the group's rounded outline
 * extended past its own top and bottom and clips it, leaving just its part of the edge.
 */
private fun Modifier.groupSegment(segment: Segment, fill: Color, edge: Color): Modifier = drawBehind {
    val radius = BrShapes.LargeRadius.toPx()
    val stroke = 1.dp.toPx()
    val reach = radius * 2
    val top = if (segment == Segment.First || segment == Segment.Single) 0f else -reach
    val bottom = if (segment == Segment.Last || segment == Segment.Single) size.height else size.height + reach
    clipRect {
        drawRoundRect(
            color = fill,
            topLeft = Offset(0f, top),
            size = Size(size.width, bottom - top),
            cornerRadius = CornerRadius(radius)
        )
        drawRoundRect(
            color = edge,
            topLeft = Offset(stroke / 2, top + stroke / 2),
            size = Size(size.width - stroke, bottom - top - stroke),
            cornerRadius = CornerRadius(radius - stroke / 2),
            style = Stroke(stroke)
        )
    }
}
