package dev.redfox.alarmzy.presentation.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.TimerState
import dev.redfox.alarmzy.domain.model.TimerStatus
import dev.redfox.alarmzy.presentation.common.countdownReadout
import dev.redfox.alarmzy.presentation.common.hmsToMillis
import dev.redfox.alarmzy.presentation.common.millisToHms
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.BrChip
import dev.redfox.bedrock.components.BrLargeTitle
import dev.redfox.bedrock.components.BrTag
import dev.redfox.bedrock.components.ButtonSize
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.topBarClearance
import dev.redfox.bedrock.effects.glassSource
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.BrTransitions
import dev.redfox.bedrock.motion.RollingNumber
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.navigation.brNavClearance
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun TimerScreen(
    uiState: TimerUiState,
    onIntent: (TimerIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val timer = uiState.timer
    // The timer never scrolls, so its large title always rests fully shown.
    val collapse = remember { mutableFloatStateOf(0f) }
    val rise = with(LocalDensity.current) { 14.dp.roundToPx() }

    Box(modifier.fillMaxSize().background(BedrockTheme.colors.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .glassSource()
                .padding(top = topBarClearance(), bottom = brNavClearance())
        ) {
            BrLargeTitle(title = "Timer", collapse = collapse)
            // Each mode keeps its own last timer state, so the leaving one doesn't flash the
            // other's numbers while it fades.
            AnimatedContent(
                targetState = timer,
                contentKey = { it.status == TimerStatus.IDLE },
                transitionSpec = {
                    // The newcomer rises a short way as it fades in, a beat behind the one leaving.
                    (fadeIn(tween(BrDurations.Enter, delayMillis = 60, easing = BrMotion.Calm)) +
                        slideInVertically(tween(BrDurations.Enter, easing = BrMotion.Calm)) { rise }) togetherWith
                        fadeOut(tween(BrDurations.Fade))
                },
                label = "timerMode",
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = BrSpacing.Gutter)
            ) { state ->
                if (state.status == TimerStatus.IDLE) TimerSetup(state, onIntent) else TimerRunning(state, onIntent)
            }
        }
    }
}

/**
 * The wall clock, refreshed every frame while [active]. The same per-frame tick as
 * `rememberTicker`, handed out as a [State] so the ring reads it in draw and the readings through
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
// Idle: drum picker + presets + Start
// ---------------------------------------------------------------------------------------------

private val ColonWidth = 20.dp

@Composable
private fun TimerSetup(timer: TimerState, onIntent: (TimerIntent) -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val (h, m, s) = millisToHms(timer.pickerMs)
    val set = { hh: Int, mm: Int, ss: Int -> onIntent(TimerIntent.SetDuration(hmsToMillis(hh, mm, ss))) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))

        // Column headers aligned with the wheels below.
        Row(Modifier.fillMaxWidth().padding(bottom = BrSpacing.M).revealOnEnter(0)) {
            listOf("Hours", "Minutes", "Seconds").forEachIndexed { i, text ->
                if (i > 0) Spacer(Modifier.width(ColonWidth))
                Text(
                    text,
                    style = type.caption,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Box(Modifier.revealOnEnter(1), contentAlignment = Alignment.Center) {
            // The selected row rests on a quiet raised band: colour inside a surface, no glow.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(SelectionBandHeight)
                    .clip(BrShapes.Large)
                    .background(colors.surfaceRaised)
                    .border(1.dp, colors.line, BrShapes.Large)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(range = 100, value = h, onValueChange = { set(it, m, s) }, description = "Hours", modifier = Modifier.weight(1f))
                WheelColon()
                NumberWheel(range = 60, value = m, onValueChange = { set(h, it, s) }, description = "Minutes", modifier = Modifier.weight(1f))
                WheelColon()
                NumberWheel(range = 60, value = s, onValueChange = { set(h, m, it) }, description = "Seconds", modifier = Modifier.weight(1f))
            }
        }

        Spacer(Modifier.weight(1f))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BrSpacing.S, Alignment.CenterHorizontally)
        ) {
            TimerUiState.Presets.forEachIndexed { i, preset ->
                BrChip(
                    label = spokenDuration(preset),
                    selected = preset == timer.pickerMs,
                    onClick = { onIntent(TimerIntent.SetDuration(preset)) },
                    modifier = Modifier.revealOnEnter(2 + i)
                )
            }
        }
        Spacer(Modifier.height(BrSpacing.XL))
        BrButton(
            text = "Start",
            onClick = { onIntent(TimerIntent.Start) },
            modifier = Modifier.fillMaxWidth().revealOnEnter(2 + TimerUiState.Presets.size),
            variant = ButtonVariant.Primary,
            size = ButtonSize.Large,
            icon = rememberVectorPainter(Icons.Rounded.PlayArrow),
            enabled = timer.pickerMs > 0
        )
        Spacer(Modifier.height(BrSpacing.L))
    }
}

@Composable
private fun WheelColon() {
    Box(Modifier.width(ColonWidth), contentAlignment = Alignment.Center) {
        Text(
            ":",
            style = BedrockTheme.typography.numeral.copy(fontSize = 40.sp, lineHeight = 44.sp),
            color = BedrockTheme.colors.textTertiary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
    }
}

private val WheelItemHeight = 80.dp
private val WheelHeight = 240.dp
private val SelectionBandHeight = 68.dp
private const val WheelLoops = 1000

/**
 * Looping drum picker. The centered item is the value; it snaps to center, ticks a haptic per
 * value, and reports the value once scrolling settles. External [value] changes (presets,
 * restored state) scroll the wheel to match.
 */
@Composable
private fun NumberWheel(
    range: Int,
    value: Int,
    onValueChange: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val colors = BedrockTheme.colors
    val digits = BedrockTheme.typography.numeral.copy(fontSize = 52.sp, lineHeight = 56.sp)
    val haptics = LocalHapticFeedback.current
    val count = range * WheelLoops
    val middle = remember(range) { (count / 2) - (count / 2) % range }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = middle + value)
    val fling = rememberSnapFlingBehavior(lazyListState = state, snapPosition = SnapPosition.Center)
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentValue by rememberUpdatedState(value)

    val centerIndex by remember {
        derivedStateOf {
            val info = state.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index ?: (middle + currentValue)
        }
    }

    // Haptic tick as each value passes the center.
    LaunchedEffect(state) {
        snapshotFlow { centerIndex }.distinctUntilChanged().drop(1).collect {
            if (state.isScrollInProgress) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        }
    }
    // Report the settled value.
    LaunchedEffect(state) {
        snapshotFlow { state.isScrollInProgress }.distinctUntilChanged().filter { !it }.collect {
            val v = centerIndex % range
            if (v != currentValue) currentOnValueChange(v)
        }
    }
    // Follow external changes (presets / restored state).
    LaunchedEffect(value) {
        if (!state.isScrollInProgress && centerIndex % range != value) {
            val base = centerIndex - centerIndex % range
            state.animateScrollToItem(base + value)
        }
    }

    LazyColumn(
        state = state,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = (WheelHeight - WheelItemHeight) / 2),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .height(WheelHeight)
            .semantics { contentDescription = "$description ${"%02d".format(value)}" }
    ) {
        items(count) { index ->
            Box(
                Modifier
                    .height(WheelItemHeight)
                    .fillMaxWidth()
                    .graphicsLayer {
                        // A real drum: numbers curl away around a cylinder, shrinking and
                        // fading continuously with their distance from the center row.
                        val info = state.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
                        val d = if (item == null) 1.5f else {
                            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                            ((item.offset + item.size / 2f) - center) / item.size
                        }
                        val a = abs(d).coerceAtMost(1.5f)
                        val scale = 1f - 0.3f * a.coerceAtMost(1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - 0.8f * a).coerceIn(0.08f, 1f)
                        rotationX = -d * 32f
                        cameraDistance = 10f * density
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("%02d".format(index % range), style = digits, color = colors.textPrimary)
            }
        }
    }
}

/** "10 min", "1 h 30 min", "45 s": a duration the way a person would say it. */
private fun spokenDuration(ms: Long): String {
    val (h, m, s) = millisToHms(ms)
    val parts = buildList {
        if (h > 0) add("$h h")
        if (m > 0) add("$m min")
        if (s > 0) add("$s s")
    }
    return if (parts.isEmpty()) "0 s" else parts.joinToString(" ")
}

// ---------------------------------------------------------------------------------------------
// Running / paused / finished: progress ring + controls
// ---------------------------------------------------------------------------------------------

private val RingStroke = 6.dp

@Composable
private fun TimerRunning(timer: TimerState, onIntent: (TimerIntent) -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val running = timer.status == TimerStatus.RUNNING
    val finished = timer.status == TimerStatus.FINISHED
    val paused = timer.status == TimerStatus.PAUSED
    val now = rememberFrameClock(active = running)

    // Paused: the reading and the arc dim and hold still. Finished: the ring fills and its
    // inside takes on the accent. Both are slow fades, read in draw.
    val dim = animateFloatAsState(
        targetValue = if (paused) 1f else 0f,
        animationSpec = tween(BrDurations.Linger, easing = BrMotion.Calm),
        label = "pausedDim"
    )
    val done = animateFloatAsState(
        targetValue = if (finished) 1f else 0f,
        animationSpec = tween(BrDurations.Linger, easing = BrMotion.Calm),
        label = "finishedFill"
    )
    // The ring draws itself in when the countdown appears.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(BrDurations.Linger, easing = BrMotion.Calm)) }

    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(
            modifier = Modifier
                .widthIn(max = 288.dp)
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val ringSize: Dp = maxWidth
            TimerRing(
                timer = timer,
                now = now,
                intro = intro.asState(),
                dim = dim,
                done = done,
                modifier = Modifier.fillMaxSize()
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CountdownReading(timer = timer, now = now, ringSize = ringSize, finished = finished, dim = dim)
                Spacer(Modifier.height(BrSpacing.M))
                AnimatedContent(
                    targetState = timer.status,
                    transitionSpec = { BrTransitions.swap },
                    label = "timerStatus"
                ) { status ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        when (status) {
                            TimerStatus.FINISHED -> Text("Time's up", style = type.label, color = colors.accentSoft)
                            TimerStatus.RUNNING -> {
                                Icon(
                                    painterResource(R.drawable.ic_bell_ring),
                                    contentDescription = null,
                                    tint = colors.accentSoft,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    "Ends at ${endsAt(timer.endAtMillis)}",
                                    style = type.caption.copy(fontFeatureSettings = type.figure.fontFeatureSettings),
                                    color = colors.textSecondary
                                )
                            }
                            else -> Text("Paused", style = type.caption, color = colors.textSecondary)
                        }
                    }
                }
                Spacer(Modifier.height(BrSpacing.S))
                BrTag(text = spokenDuration(timer.durationMs))
            }
        }
        Spacer(Modifier.weight(1f))

        AnimatedContent(
            targetState = finished,
            transitionSpec = { BrTransitions.swap },
            label = "timerControls",
            modifier = Modifier.fillMaxWidth().padding(bottom = BrSpacing.L)
        ) { isFinished ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
            ) {
                if (isFinished) {
                    BrButton(
                        text = "+1 min",
                        onClick = { onIntent(TimerIntent.AddMinute) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Large
                    )
                    BrButton(
                        text = "Stop",
                        onClick = { onIntent(TimerIntent.Cancel) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large
                    )
                } else {
                    BrButton(
                        text = "Cancel",
                        onClick = { onIntent(TimerIntent.Cancel) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Tonal,
                        size = ButtonSize.Large,
                        icon = painterResource(R.drawable.ic_cancel)
                    )
                    BrButton(
                        text = if (running) "Pause" else "Resume",
                        onClick = { onIntent(if (running) TimerIntent.Pause else TimerIntent.Resume) },
                        modifier = Modifier.weight(1f),
                        variant = ButtonVariant.Primary,
                        size = ButtonSize.Large,
                        icon = if (running) painterResource(R.drawable.ic_pause)
                        else rememberVectorPainter(Icons.Rounded.PlayArrow)
                    )
                }
            }
        }
    }
}

/** Share of the duration still to run, 1 at the start and 0 when time is up. */
private fun TimerState.fractionAt(now: Long): Float =
    if (durationMs > 0) (remainingAt(now).toFloat() / durationMs).coerceIn(0f, 1f) else 0f

/**
 * The progress ring: an accent arc on a surfaceHigh track with round ends, draining
 * counter-clockwise from 12 o'clock. The time is read in draw, so it never recomposes per frame;
 * only the accessibility progress (in whole percent) goes through composition.
 */
@Composable
private fun TimerRing(
    timer: TimerState,
    now: State<Long>,
    intro: State<Float>,
    dim: State<Float>,
    done: State<Float>,
    modifier: Modifier = Modifier,
) {
    val colors = BedrockTheme.colors
    val percent by remember(timer) { derivedStateOf { (timer.fractionAt(now.value) * 100).roundToInt() } }
    val progress = percent / 100f
    Canvas(modifier.semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }) {
        val stroke = RingStroke.toPx()
        val r = (size.minDimension - stroke) / 2f
        val filled = done.value
        // Time's up: the accent settles inside the ring rather than glowing around it.
        if (filled > 0f) drawCircle(colors.accentContainer, radius = r - stroke / 2f, center = center, alpha = filled)
        drawCircle(colors.surfaceHigh, radius = r, center = center, style = Stroke(width = stroke))
        val f = timer.fractionAt(now.value)
        val sweep = 360f * (f + (1f - f) * filled) * intro.value
        if (sweep <= 0f) return@Canvas
        drawArc(
            color = colors.accentMain.copy(alpha = 1f - 0.55f * dim.value),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(center.x - r, center.y - r),
            size = Size(r * 2f, r * 2f),
            style = Stroke(width = stroke, cap = StrokeCap.Round)
        )
    }
}

/**
 * The big countdown: Light, tabular numerals sized to the ring, digits rolling down as they
 * change. It recomposes once a second (when the text changes), not every frame.
 */
@Composable
private fun CountdownReading(
    timer: TimerState,
    now: State<Long>,
    ringSize: Dp,
    finished: Boolean,
    dim: State<Float>,
) {
    val colors = BedrockTheme.colors
    val readout by remember(timer) { derivedStateOf { countdownReadout(timer.remainingAt(now.value)) } }
    val tone by animateColorAsState(
        targetValue = if (finished) colors.accentSoft else colors.textPrimary,
        animationSpec = tween(BrDurations.Swap, easing = BrMotion.Calm),
        label = "readoutTone"
    )
    // "04:42" fills the ring; "1:04:42" steps down so it still fits.
    val fontSize = (ringSize.value * if (readout.length > 5) 0.165f else 0.22f).sp
    RollingNumber(
        text = readout,
        style = BedrockTheme.typography.numeral.copy(fontSize = fontSize, lineHeight = fontSize * 1.1f),
        color = tone,
        modifier = Modifier
            .graphicsLayer { alpha = 1f - 0.5f * dim.value }
            .clearAndSetSemantics { contentDescription = readout }
    )
}

private fun endsAt(endAtMillis: Long): String =
    Instant.ofEpochMilli(endAtMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
