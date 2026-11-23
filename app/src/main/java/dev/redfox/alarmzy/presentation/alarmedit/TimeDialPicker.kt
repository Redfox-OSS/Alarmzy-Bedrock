package dev.redfox.alarmzy.presentation.alarmedit

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.redfox.bedrock.effects.pressable
import dev.redfox.bedrock.effects.rim
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.RollingNumber
import dev.redfox.bedrock.theme.BedrockFontFamily
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private enum class TimeField { HOUR, MINUTE }

/**
 * Time picker: tappable hour and minute tiles, a vertical a.m./p.m. toggle, and a radial dial you
 * tap or drag. Picking an hour on the dial advances to minutes. [hour] is 0–23.
 *
 * The active tile sits in a calm accent container; the dial's selection is an accent-tinted lens
 * riding a critically damped hand, so nothing glows and nothing overshoots. [tileSize] sizes the
 * hour and minute tiles (smaller inside a dialog); their Light, tabular digits scale with them.
 */
@Composable
fun TimeDialPicker(
    hour: Int,
    minute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    dialSize: Dp = 232.dp,
    tileSize: Dp = 94.dp,
) {
    var field by rememberSaveable { mutableStateOf(TimeField.HOUR) }
    val isPm = hour >= 12
    val hour12 = (hour % 12).let { if (it == 0) 12 else it }
    val colors = BedrockTheme.colors
    // Bedrock's numeral voice (Inter Light, tabular figures), sized to the tile.
    val digits = BedrockTheme.typography.numeral.copy(
        fontFamily = BedrockFontFamily,
        fontWeight = FontWeight.Light,
        fontSize = (tileSize.value * 0.46f).sp,
        lineHeight = (tileSize.value * 0.5f).sp,
    )

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            TimeTile(
                text = "%02d".format(hour12),
                active = field == TimeField.HOUR,
                description = "Hour",
                size = tileSize,
                style = digits,
                onClick = { field = TimeField.HOUR }
            )
            Text(
                ":",
                style = digits,
                color = colors.textTertiary,
                modifier = Modifier.padding(horizontal = BrSpacing.XS)
            )
            TimeTile(
                text = "%02d".format(minute),
                active = field == TimeField.MINUTE,
                description = "Minute",
                size = tileSize,
                style = digits,
                onClick = { field = TimeField.MINUTE }
            )
            Spacer(Modifier.width(BrSpacing.M))
            MeridiemToggle(
                isPm = isPm,
                onChange = { pm -> onTimeChange(hour % 12 + if (pm) 12 else 0, minute) }
            )
        }
        Spacer(Modifier.height(BrSpacing.XL))
        ClockDial(
            showMinutes = field == TimeField.MINUTE,
            hour12 = hour12,
            minute = minute,
            size = dialSize,
            onHourPicked = { h12 -> onTimeChange(h12 % 12 + if (isPm) 12 else 0, minute) },
            onMinutePicked = { m -> onTimeChange(hour, m) },
            onGestureEnd = { if (field == TimeField.HOUR) field = TimeField.MINUTE }
        )
    }
}

/** An hour or minute tile. Active: a calm accent container with soft-accent digits, no border. */
@Composable
private fun TimeTile(
    text: String,
    active: Boolean,
    description: String,
    size: Dp,
    style: TextStyle,
    onClick: () -> Unit,
) {
    val colors = BedrockTheme.colors
    val shape = BrShapes.Large
    val fill by animateColorAsState(
        if (active) colors.accentContainer else colors.surfaceRaised,
        BrMotion.quick(),
        label = "tileFill"
    )
    val edge by animateColorAsState(
        if (active) Color.Transparent else colors.line,
        BrMotion.quick(),
        label = "tileEdge"
    )
    val ink by animateColorAsState(
        if (active) colors.accentSoft else colors.textPrimary,
        BrMotion.quick(),
        label = "tileDigits"
    )
    Box(
        modifier = Modifier
            .semantics {
                contentDescription = "$description $text"
                selected = active
            }
            .pressable(pressedScale = 0.97f, haptic = HapticFeedbackType.SegmentTick, onClick = onClick)
            .size(size)
            .clip(shape)
            .background(fill)
            .border(1.dp, edge, shape),
        contentAlignment = Alignment.Center
    ) {
        RollingNumber(text, style = style, color = ink)
    }
}

/**
 * A vertical two-segment control: a lighter surfaceHigh lens with a lit rim glides under the
 * chosen half on a settling spring, like Bedrock's segmented control turned on its side.
 */
@Composable
private fun MeridiemToggle(isPm: Boolean, onChange: (Boolean) -> Unit) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val outer = BrShapes.Medium
    val inner = BrShapes.concentric(BrShapes.MediumRadius, MeridiemInset)
    val position = animateFloatAsState(if (isPm) 1f else 0f, BrMotion.settle(), label = "meridiem")
    Box(
        Modifier
            .clip(outer)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.line, outer)
            .padding(MeridiemInset)
    ) {
        Box(
            Modifier
                .size(MeridiemWidth, MeridiemHeight)
                .graphicsLayer { translationY = MeridiemHeight.toPx() * position.value }
                .clip(inner)
                .background(colors.surfaceHigh)
                .rim(inner)
        )
        Column {
            listOf(false to "a.m.", true to "p.m.").forEach { (pm, label) ->
                val chosen = pm == isPm
                val tint by animateColorAsState(
                    if (chosen) colors.textPrimary else colors.textSecondary,
                    BrMotion.quick(),
                    label = "meridiemText"
                )
                Box(
                    modifier = Modifier
                        .size(MeridiemWidth, MeridiemHeight)
                        .pressable(
                            role = Role.RadioButton,
                            pressedScale = 0.94f,
                            haptic = if (chosen) null else HapticFeedbackType.SegmentTick,
                        ) { onChange(pm) }
                        .semantics { selected = chosen },
                    contentAlignment = Alignment.Center
                ) {
                    Text(label, style = type.labelSmall, color = tint)
                }
            }
        }
    }
}

private val MeridiemInset = 4.dp
private val MeridiemWidth = 56.dp
private val MeridiemHeight = 40.dp

@Composable
private fun ClockDial(
    showMinutes: Boolean,
    hour12: Int,
    minute: Int,
    size: Dp,
    onHourPicked: (Int) -> Unit,
    onMinutePicked: (Int) -> Unit,
    onGestureEnd: () -> Unit,
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val haptics = LocalHapticFeedback.current
    val nodeSize = 36.dp
    val numberInset = 24.dp

    // Continuous angle (degrees, 0 = 12 o'clock, clockwise) so the hand takes the short way round.
    val targetDeg = if (showMinutes) minute * 6f else (hour12 % 12) * 30f
    val unwrapped = remember { floatArrayOf(targetDeg) }
    val continuousDeg = remember(targetDeg) {
        unwrapped[0] += ((targetDeg - unwrapped[0]) % 360f + 540f) % 360f - 180f
        unwrapped[0]
    }
    // Critically damped: quick while a finger leads it, settling when it sweeps on its own.
    var dragging by remember { mutableStateOf(false) }
    val angle = animateFloatAsState(
        continuousDeg,
        animationSpec = if (dragging) BrMotion.quick<Float>() else BrMotion.settle<Float>(),
        label = "dialAngle"
    )

    // pointerInput(Unit) outlives recompositions, so read the latest values/callbacks through these.
    val currentShowMinutes by rememberUpdatedState(showMinutes)
    val currentOnHourPicked by rememberUpdatedState(onHourPicked)
    val currentOnMinutePicked by rememberUpdatedState(onMinutePicked)
    val currentOnGestureEnd by rememberUpdatedState(onGestureEnd)
    val lastValue = remember { mutableFloatStateOf(-1f) }

    // Switching hours <-> minutes re-deals the numerals clockwise around the face.
    val deal = remember { Animatable(1f) }
    val firstFace = remember { booleanArrayOf(true) }
    LaunchedEffect(showMinutes) {
        if (firstFace[0]) {
            firstFace[0] = false
            return@LaunchedEffect
        }
        deal.snapTo(0f)
        deal.animateTo(1f, tween(DealMillis, easing = LinearEasing))
    }

    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .border(1.dp, colors.line, CircleShape)
            .pointerInput(Unit) {
                fun pick(pos: Offset) {
                    val dx = pos.x - this.size.width / 2f
                    val dy = pos.y - this.size.height / 2f
                    val deg = ((Math.toDegrees(atan2(dx.toDouble(), -dy.toDouble())) + 360.0) % 360.0).toFloat()
                    if (currentShowMinutes) {
                        val m = (deg / 6f).roundToInt() % 60
                        if (m.toFloat() != lastValue.floatValue) {
                            lastValue.floatValue = m.toFloat()
                            if (m % 5 == 0) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnMinutePicked(m)
                        }
                    } else {
                        val idx = (deg / 30f).roundToInt() % 12
                        val h = if (idx == 0) 12 else idx
                        if (h.toFloat() != lastValue.floatValue) {
                            lastValue.floatValue = h.toFloat()
                            haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                            currentOnHourPicked(h)
                        }
                    }
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    dragging = true
                    lastValue.floatValue = -1f
                    pick(down.position)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { change ->
                            if (change.pressed) {
                                pick(change.position)
                                change.consume()
                            }
                        }
                    } while (event.changes.any { it.pressed })
                    dragging = false
                    currentOnGestureEnd()
                }
            },
        contentAlignment = Alignment.Center
    ) {
        val hand = colors.accentMain
        Canvas(Modifier.size(size)) {
            val c = center
            // The hand stops at the lens's edge, so it never shows through it.
            val r = this.size.minDimension / 2f - numberInset.toPx() - nodeSize.toPx() / 2f
            val rad = Math.toRadians(angle.value.toDouble())
            val end = Offset(c.x + (r * sin(rad)).toFloat(), c.y - (r * cos(rad)).toFloat())
            drawLine(hand.copy(alpha = 0.55f), c, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(hand, radius = 4.dp.toPx(), center = c)
        }

        // Numerals
        val radius = size / 2 - numberInset
        for (i in 0 until 12) {
            val label = if (showMinutes) "%02d".format(i * 5) else (if (i == 0) "12" else "$i")
            val a = i * 30.0 * PI / 180.0
            val dirX = sin(a).toFloat()
            val dirY = -cos(a).toFloat()
            DialLabel(
                text = label,
                x = radius * dirX,
                y = radius * dirY,
                color = colors.textSecondary,
                style = type.figure,
                modifier = Modifier.graphicsLayer {
                    // Each numeral fades in while drifting a few dp in from the rim: no scale.
                    val t = ((deal.value * DealMillis - i * DealStaggerMillis) / BrDurations.Swap).coerceIn(0f, 1f)
                    val e = BrMotion.Calm.transform(t)
                    val drift = (1f - e) * DealDrift.toPx()
                    alpha = e
                    translationX = dirX * drift
                    translationY = dirY * drift
                }
            )
        }

        // Selection lens riding the hand: opaque, tinted with the accent, lit along its rim.
        Box(
            modifier = Modifier
                .offset {
                    val rad = angle.value * PI / 180.0
                    IntOffset(
                        (radius * sin(rad).toFloat()).roundToPx(),
                        (-radius * cos(rad).toFloat()).roundToPx()
                    )
                }
                .size(nodeSize)
                .clip(CircleShape)
                .background(colors.surfaceHigh)
                .background(colors.accentContainerStrong)
                .rim(CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (showMinutes) "%02d".format(minute) else "$hour12",
                style = type.figure.copy(fontWeight = FontWeight.Medium),
                color = colors.accentSoft
            )
        }
    }
}

private const val DealStaggerMillis = 18
private const val DealMillis = DealStaggerMillis * 11 + BrDurations.Swap
private val DealDrift = 6.dp

@Composable
private fun DialLabel(text: String, x: Dp, y: Dp, color: Color, style: TextStyle, modifier: Modifier = Modifier) {
    Box(Modifier.offset(x, y).size(28.dp).then(modifier), contentAlignment = Alignment.Center) {
        Text(text, style = style, color = color)
    }
}
