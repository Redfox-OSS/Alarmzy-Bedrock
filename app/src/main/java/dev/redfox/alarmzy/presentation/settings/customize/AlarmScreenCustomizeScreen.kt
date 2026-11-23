package dev.redfox.alarmzy.presentation.settings.customize

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.domain.model.Quotes
import dev.redfox.alarmzy.presentation.alarmringing.AlarmRingingContent
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrSwitch
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.IconTile
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.SegmentedControl
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.motion.RevealColumn
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import kotlin.math.roundToInt

@Composable
fun AlarmScreenCustomizeScreen(
    uiState: AlarmScreenCustomizeUiState,
    onIntent: (AlarmScreenCustomizeIntent) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = BedrockTheme.colors
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onIntent(AlarmScreenCustomizeIntent.PickedBackgroundImage(uri))
        }
    }
    val scrollState = rememberScrollState()
    val collapse = rememberTopBarCollapse(scrollState)

    Box(modifier.fillMaxSize().background(colors.background)) {
        RevealColumn(
            modifier = Modifier
                .fillMaxSize()
                .glassSource()
                .verticalScroll(scrollState)
                .padding(
                    top = topBarClearance(),
                    // Each section already leaves BrSpacing.XXL below itself.
                    bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                )
        ) {
            BrLargeTitle(title = "Alarm screen", collapse = collapse)

            CustomizeSection("Preview") {
                AlarmScreenPreview(
                    backgroundImagePath = uiState.backgroundImagePath,
                    buttonLayout = uiState.buttonLayout,
                    clockPosition = uiState.clockPosition,
                    showQuote = uiState.showQuote
                )
            }

            CustomizeSection("Background") {
                Row(horizontalArrangement = Arrangement.spacedBy(BrSpacing.S)) {
                    BrButton(
                        text = if (uiState.backgroundImagePath != null) "Change photo" else "Choose photo",
                        onClick = {
                            pickImageLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Medium
                    )
                    if (uiState.backgroundImagePath != null) {
                        BrButton(
                            text = "Remove photo",
                            onClick = { onIntent(AlarmScreenCustomizeIntent.RemoveBackgroundImage) },
                            variant = ButtonVariant.Plain,
                            size = ButtonSize.Medium
                        )
                    }
                }
            }

            CustomizeSection("Buttons") {
                SegmentedControl(
                    options = AlarmButtonLayout.entries.map { it.displayName },
                    selectedIndex = AlarmButtonLayout.entries.indexOf(uiState.buttonLayout),
                    onSelect = { onIntent(AlarmScreenCustomizeIntent.SetButtonLayout(AlarmButtonLayout.entries[it])) }
                )
            }

            CustomizeSection("Clock position") {
                SegmentedControl(
                    options = AlarmClockPosition.entries.map { it.displayName },
                    selectedIndex = AlarmClockPosition.entries.indexOf(uiState.clockPosition),
                    onSelect = { onIntent(AlarmScreenCustomizeIntent.SetClockPosition(AlarmClockPosition.entries[it])) }
                )
            }

            CustomizeSection("Quote") {
                ListGroup {
                    ListRow(
                        title = "Show a wake-up quote",
                        subtitle = "A new one each time an alarm rings",
                        leading = { IconTile(painterResource(R.drawable.ic_info)) },
                        trailing = {
                            BrSwitch(
                                checked = uiState.showQuote,
                                onCheckedChange = { onIntent(AlarmScreenCustomizeIntent.SetShowQuote(it)) }
                            )
                        },
                        onClick = { onIntent(AlarmScreenCustomizeIntent.SetShowQuote(!uiState.showQuote)) }
                    )
                }
            }
        }

        BrTopBar(
            title = "Alarm screen",
            collapse = collapse,
            modifier = Modifier.align(Alignment.TopCenter),
            onBack = onNavigateBack
        )
    }
}

/** A titled group on the page: the gutter at the edges, and the section gap below it. */
@Composable
private fun CustomizeSection(title: String, content: @Composable ColumnScope.() -> Unit) {
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

@Composable
private fun AlarmScreenPreview(
    backgroundImagePath: String?,
    buttonLayout: AlarmButtonLayout,
    clockPosition: AlarmClockPosition,
    modifier: Modifier = Modifier,
    showQuote: Boolean = true,
) {
    val configuration = LocalConfiguration.current
    val previewQuote = remember { Quotes.random() }
    val realWidthDp = configuration.screenWidthDp.dp
    val realHeightDp = configuration.screenHeightDp.dp

    // Preview keeps the device's real width/height ratio, so it reads as a phone
    // mockup rather than a square crop. The whole ringing layout is rendered at its
    // true real-device size and then uniformly scaled down as one image — so every
    // element (buttons, padding, icons, clock) shrinks together in proportion,
    // instead of only the clock text shrinking while buttons stay full-size and wrap.
    val previewWidth = 260.dp
    val previewHeight = previewWidth * (realHeightDp.value / realWidthDp.value)
    val scale = previewWidth.value / realWidthDp.value

    // A phone in the page, not floating over it: tone and a firm hairline, no shadow.
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(previewWidth)
                .height(previewHeight)
                .clip(BrShapes.XLarge)
                .border(1.dp, BedrockTheme.colors.lineStrong, BrShapes.XLarge)
        ) {
            AlarmRingingContent(
                time = "7:00 AM",
                label = "Alarm",
                backgroundImagePath = backgroundImagePath,
                buttonLayout = buttonLayout,
                clockPosition = clockPosition,
                onSnooze = {},
                onDismiss = {},
                quote = if (showQuote) previewQuote else null,
                liveBlur = false,
                modifier = Modifier.scaledMockup(realWidthDp, realHeightDp, scale)
            )
        }
    }
}

/**
 * Measures the content at its true [realWidth] x [realHeight] size, then draws that
 * whole layer scaled down by [scale] and reports the scaled size to the parent — so
 * every element inside (text, icons, padding, buttons) shrinks uniformly together,
 * instead of only individually-scaled values shrinking while the rest stay full-size.
 */
private fun Modifier.scaledMockup(realWidth: Dp, realHeight: Dp, scale: Float): Modifier =
    this.layout { measurable, _ ->
        val widthPx = realWidth.roundToPx()
        val heightPx = realHeight.roundToPx()
        val placeable = measurable.measure(Constraints.fixed(widthPx, heightPx))
        val scaledWidth = (widthPx * scale).roundToInt()
        val scaledHeight = (heightPx * scale).roundToInt()
        layout(scaledWidth, scaledHeight) {
            placeable.placeWithLayer(0, 0) {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0f, 0f)
            }
        }
    }
