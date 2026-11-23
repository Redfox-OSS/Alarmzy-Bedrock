package dev.redfox.alarmzy.presentation.stopwatch

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.StopwatchState
import dev.redfox.alarmzy.presentation.common.lapTime
import dev.redfox.alarmzy.presentation.common.stopwatchReadout
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrTopBar
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.EmptyState
import dev.redfox.bedrock.components.ListGroup
import dev.redfox.bedrock.components.ListRow
import dev.redfox.bedrock.components.SectionHeader
import dev.redfox.bedrock.components.rememberTopBarCollapse
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RollingNumber
import dev.redfox.bedrock.motion.SwapText
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.navigation.brNavClearance
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrSpacing
import kotlin.math.PI

@Composable
fun StopwatchScreen(
    uiState: StopwatchUiState,
    onIntent: (StopwatchIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val sw = uiState.stopwatch
    // Ticks every frame while running. Only the dial (in draw), the readings and the live lap
    // read it, each in its own small scope, so the list never recomposes per frame.
    val now = rememberFrameClock(active = sw.running)
    val listState = rememberLazyListState()
    val collapse = rememberTopBarCollapse(listState)

    Box(modifier.fillMaxSize().background(BedrockTheme.colors.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().glassSource(),
            contentPadding = PaddingValues(top = topBarClearance(), bottom = brNavClearance()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "title") {
                BrLargeTitle(title = "Stopwatch", collapse = collapse)
            }
            item(key = "dial") {
                StopwatchDial(
                    state = sw,
                    now = now,
                    modifier = Modifier.padding(horizontal = BrSpacing.Gutter).revealOnEnter(0)
                )
            }
            item(key = "controls") {
                Controls(
                    state = sw,
                    onIntent = onIntent,
                    modifier = Modifier.padding(horizontal = BrSpacing.Gutter).revealOnEnter(1)
                )
            }
            if (sw.laps.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        icon = painterResource(R.drawable.ic_stopwatch_empty),
                        title = "No laps yet",
                        message = if (sw.running) "Tap Lap to record a split. Each one will appear here."
                        else "Start the stopwatch, then tap Lap to record splits here.",
                        modifier = Modifier.animateItem().revealOnEnter(2)
                    )
                }
            } else {
                item(key = "laps_header") {
                    SectionHeader(
                        title = "Laps",
                        modifier = Modifier.animateItem().padding(horizontal = BrSpacing.Gutter).revealOnEnter(2)
                    )
                }
                item(key = "laps") {
                    LapList(
                        state = sw,
                        now = now,
                        modifier = Modifier.animateItem().padding(horizontal = BrSpacing.Gutter).revealOnEnter(3)
                    )
                }
            }
        }

        BrTopBar(title = "Stopwatch", collapse = collapse, modifier = Modifier.align(Alignment.TopCenter))
    }
}

/**
 * The wall clock, refreshed every frame while [active]. The same per-frame tick as
 * `rememberTicker`, handed out as a [State] so the dial reads it in draw and the readings through
 * derived state: the screen itself never recomposes per frame.
 */
@Composable
private fun rememberFrameClock(active: Boolean): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(active) {
        now.longValue = System.currentTimeMillis()
        while (active) {
            withFrameMillis { }
            now.longValue = System.currentTimeMillis()
        }
    }
    return now
}

// ---------------------------------------------------------------------------------------------
// Dial
// ---------------------------------------------------------------------------------------------

private val RingStroke = 6.dp
private val CaptionHeight = 22.dp

@Composable
private fun StopwatchDial(state: StopwatchState, now: State<Long>, modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    val active = !state.isIdle
    val paused = active && !state.running
    // Paused: the reading and the arc dim and hold still, a slow fade read in draw.
    val dim = animateFloatAsState(
        targetValue = if (paused) 1f else 0f,
        animationSpec = tween(BrDurations.Linger, easing = BrMotion.Calm),
        label = "pausedDim"
    )

    BoxWithConstraints(
        modifier = modifier.widthIn(max = 300.dp).fillMaxWidth().aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        val dialSize = maxWidth
        Canvas(Modifier.fillMaxSize()) {
            // The arc sweeps once a minute; the time is read here, in draw, every frame.
            val elapsed = state.elapsedAt(now.value)
            drawStopwatchRing(
                sweepFraction = if (active) (elapsed % 60_000L) / 60_000f else 0f,
                stroke = RingStroke.toPx(),
                track = colors.surfaceHigh,
                ticks = colors.lineStrong,
                arc = colors.accentMain.copy(alpha = 1f - 0.55f * dim.value),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // The caption fades in once started; on reset it fades out still showing its last lap.
            AnimatedContent(
                targetState = state,
                contentKey = { !it.isIdle },
                transitionSpec = {
                    fadeIn(tween(BrDurations.Swap, easing = BrMotion.Calm)) togetherWith fadeOut(tween(BrDurations.Fade))
                },
                contentAlignment = Alignment.Center,
                label = "dialCaption",
                modifier = Modifier.height(CaptionHeight)
            ) { shown ->
                if (!shown.isIdle) DialCaption(shown)
            }
            Spacer(Modifier.height(BrSpacing.XS))
            StopwatchReading(state = state, now = now, dialSize = dialSize, dim = dim)
            // Balances the caption so the reading sits in the true centre of the dial.
            Spacer(Modifier.height(CaptionHeight + BrSpacing.XS))
        }
    }
}

/** "Lap 3 · Running" above the reading while the stopwatch has been started. */
@Composable
private fun DialCaption(state: StopwatchState) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Lap ${state.laps.size + 1}",
            style = type.caption.copy(fontFeatureSettings = type.figure.fontFeatureSettings),
            color = colors.textSecondary
        )
        Text("  ·  ", style = type.caption, color = colors.textTertiary)
        SwapText(
            text = if (state.running) "Running" else "Paused",
            style = type.caption,
            color = if (state.running) colors.accentSoft else colors.textSecondary
        )
    }
}

/**
 * Minutes and seconds in Light tabular numerals sized to the dial, rolling as they change, with
 * the tenths smaller beside them. The big digits recompose once a second, the tenths ten times
 * a second, and nothing per frame.
 */
@Composable
private fun StopwatchReading(state: StopwatchState, now: State<Long>, dialSize: Dp, dim: State<Float>) {
    val colors = BedrockTheme.colors
    val main by remember(state) {
        derivedStateOf { stopwatchReadout(state.elapsedAt(now.value)).main.joinToString(":") }
    }
    // "04:42" fills the dial; "1:04:42" steps down so it still fits.
    val fontSize = (dialSize.value * if (main.length > 5) 0.15f else 0.19f).sp
    val digits = BedrockTheme.typography.numeral.copy(fontSize = fontSize, lineHeight = fontSize * 1.1f)
    Row(
        Modifier
            .graphicsLayer { alpha = 1f - 0.5f * dim.value }
            .clearAndSetSemantics { contentDescription = main }
    ) {
        RollingNumber(
            text = main,
            style = digits,
            color = colors.textPrimary,
            modifier = Modifier.alignByBaseline()
        )
        Tenths(
            state = state,
            now = now,
            style = digits.copy(fontSize = fontSize * 0.45f, lineHeight = fontSize * 0.5f),
            color = if (state.running) colors.textSecondary else colors.textTertiary,
            modifier = Modifier.alignByBaseline().padding(start = BrSpacing.XS)
        )
    }
}

@Composable
private fun Tenths(state: StopwatchState, now: State<Long>, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val tenths by remember(state) { derivedStateOf { stopwatchReadout(state.elapsedAt(now.value)).tenths } }
    Text(".$tenths", style = style, color = color, modifier = modifier)
}

private fun DrawScope.drawStopwatchRing(
    sweepFraction: Float,
    stroke: Float,
    track: Color,
    ticks: Color,
    arc: Color,
) {
    val c = center
    val r = (size.minDimension - stroke) / 2f
    // Track
    drawCircle(track, radius = r, center = c, style = Stroke(width = stroke))
    // Sixty quiet second marks just inside it.
    val tickRadius = r - stroke * 2.4f
    val step = (2f * PI.toFloat() * tickRadius) / 60f
    val dot = stroke * 0.3f
    drawCircle(
        ticks,
        radius = tickRadius,
        center = c,
        style = Stroke(width = stroke * 0.25f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dot, step - dot)))
    )
    if (sweepFraction <= 0f) return
    drawArc(
        color = arc,
        startAngle = -90f,
        sweepAngle = 360f * sweepFraction,
        useCenter = false,
        topLeft = Offset(c.x - r, c.y - r),
        size = Size(r * 2f, r * 2f),
        style = Stroke(width = stroke, cap = StrokeCap.Round)
    )
}

// ---------------------------------------------------------------------------------------------
// Controls
// ---------------------------------------------------------------------------------------------

@Composable
private fun Controls(state: StopwatchState, onIntent: (StopwatchIntent) -> Unit, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = when {
            state.running -> 1
            state.isIdle -> 0
            else -> 2
        },
        transitionSpec = { BrTransitions.swap },
        label = "stopwatchControls",
        modifier = modifier.fillMaxWidth().padding(top = BrSpacing.L, bottom = BrSpacing.XXL)
    ) { mode ->
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BrSpacing.M, Alignment.CenterHorizontally)
        ) {
            val play = rememberVectorPainter(Icons.Rounded.PlayArrow)
            when (mode) {
                0 -> BrButton(
                    text = "Start",
                    onClick = { onIntent(StopwatchIntent.Start) },
                    modifier = Modifier.fillMaxWidth(),
                    variant = ButtonVariant.Primary,
                    size = ButtonSize.Large,
                    icon = play
                )
                1 -> {
                    BrButton(
                        text = "Lap",
                        onClick = { onIntent(StopwatchIntent.Lap) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Large,
                        icon = painterResource(R.drawable.ic_lap)
                    )
                    BrButton(
                        text = "Pause",
                        onClick = { onIntent(StopwatchIntent.Pause) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                        icon = painterResource(R.drawable.ic_pause)
                    )
                }
                else -> {
                    BrButton(
                        text = "Reset",
                        onClick = { onIntent(StopwatchIntent.Reset) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Large,
                        icon = rememberVectorPainter(Icons.Rounded.Replay)
                    )
                    BrButton(
                        text = "Resume",
                        onClick = { onIntent(StopwatchIntent.Start) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                        icon = play
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Laps
// ---------------------------------------------------------------------------------------------

/**
 * The lap in progress on top (live), then recorded laps newest first, on one grouped card. Each
 * row shows the lap's split, and the running total beneath it. With two or more laps the fastest
 * and slowest are named and toned, so the difference never rests on colour alone.
 */
@Composable
private fun LapList(state: StopwatchState, now: State<Long>, modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    val splits = state.lapSplits
    val fastest = if (splits.size >= 2) splits.min() else null
    val slowest = if (splits.size >= 2) splits.max() else null

    ListGroup(modifier.animateContentSize(BrMotion.settle(IntSize.VisibilityThreshold))) {
        ListRow(
            title = "Lap ${state.laps.size + 1}",
            subtitle = "In progress",
            trailing = { LiveLapFigures(state = state, now = now) },
            divider = true,
            modifier = Modifier.semantics(mergeDescendants = true) {}
        )
        for (i in splits.indices.reversed()) {
            key(i) {
                val (tone, note) = when (splits[i]) {
                    fastest -> colors.positive to "Fastest"
                    slowest -> colors.caution to "Slowest"
                    else -> colors.textPrimary to null
                }
                ListRow(
                    title = "Lap ${i + 1}",
                    subtitle = note,
                    trailing = { LapFigures(split = lapTime(splits[i]), overall = lapTime(state.laps[i]), splitColor = tone) },
                    divider = i > 0,
                    modifier = Modifier.semantics(mergeDescendants = true) {}
                )
            }
        }
    }
}

/** The live lap's figures. The only lap content that reads the clock, so only it recomposes. */
@Composable
private fun LiveLapFigures(state: StopwatchState, now: State<Long>) {
    val elapsed = state.elapsedAt(now.value)
    LapFigures(
        split = lapTime(elapsed - (state.laps.lastOrNull() ?: 0L)),
        overall = lapTime(elapsed),
        splitColor = BedrockTheme.colors.accentSoft
    )
}

@Composable
private fun LapFigures(split: String, overall: String, splitColor: Color) {
    val type = BedrockTheme.typography
    Column(horizontalAlignment = Alignment.End) {
        Text(split, style = type.figure, color = splitColor, maxLines = 1)
        Text(
            overall,
            style = type.caption.copy(fontFeatureSettings = type.figure.fontFeatureSettings),
            color = BedrockTheme.colors.textTertiary,
            maxLines = 1
        )
    }
}
