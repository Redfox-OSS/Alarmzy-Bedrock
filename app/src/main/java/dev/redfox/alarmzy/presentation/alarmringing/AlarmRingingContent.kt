package dev.redfox.alarmzy.presentation.alarmringing

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlarmOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.AlarmButtonLayout
import dev.redfox.alarmzy.domain.model.AlarmClockPosition
import dev.redfox.alarmzy.domain.model.Quote
import dev.redfox.bedrock.components.BrButton
import dev.redfox.bedrock.components.ButtonVariant
import dev.redfox.bedrock.components.GlassCard
import dev.redfox.bedrock.effects.Aura
import dev.redfox.bedrock.effects.GlassBackdrop
import dev.redfox.bedrock.effects.LocalHazeState
import dev.redfox.bedrock.effects.aura
import dev.redfox.bedrock.motion.BrDurations
import dev.redfox.bedrock.motion.BrMotion
import dev.redfox.bedrock.motion.RevealScope
import dev.redfox.bedrock.motion.revealOnEnter
import dev.redfox.bedrock.theme.BedrockTheme
import dev.redfox.bedrock.theme.BrShapes
import dev.redfox.bedrock.theme.BrSpacing
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** The ringing clock: Bedrock's `numeral`, scaled up to read from across a dark room. */
private val ClockTextSize = 88.sp
private val ClockLineHeight = 92.sp

/** Snooze / Dismiss are taller than a standard large button: they're hit by someone half asleep. */
private val RingButtonHeight = 64.dp

/**
 * One full breath of the aura (in and out). Half the aura's drift lap: about seven breaths a
 * minute, a resting rhythm, slow enough to be felt rather than watched.
 */
private const val BreathMillis = BrDurations.Ambient / 2

/** The breath ticks at the aura drift's cadence, so the glass above re-blurs no more often. */
private const val BreathFrameMillis = 48L

/** How much the page tone veils the aura at the top and bottom of a breath. */
private const val VeilBright = 0.12f
private const val VeilDim = 0.30f

/** A firmer floor than the stock aura, so the quote and buttons at the bottom stay readable. */
private const val RingAuraShade = 0.42f

/**
 * The full-screen alarm-ringing layout, shared by the real [dev.redfox.alarmzy.alarm.AlarmRingingActivity]
 * and the settings customization screen's live preview — keeping both in sync by construction.
 *
 * The default backdrop is a full-bleed, drifting aura in the current accent that breathes slowly;
 * a user-chosen photo replaces it. The time sits large and light in the middle (or near the top),
 * with Snooze and Dismiss as large frosted and ink pills at the bottom.
 */
@Composable
fun AlarmRingingContent(
    time: String,
    label: String,
    backgroundImagePath: String?,
    buttonLayout: AlarmButtonLayout,
    clockPosition: AlarmClockPosition,
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = "Dismiss",
    /** Wake-up quote shown in a frosted card above the buttons; null hides it. */
    quote: Quote? = null,
    /**
     * Live-blur the glass (quote card, Snooze). Off for the scaled-down Settings preview: Haze
     * samples in unscaled window coordinates, so inside a scaled mockup the blur layer misaligns
     * and leaves an unblurred strip. The preview uses glass's settled frosted fallback instead.
     */
    liveBlur: Boolean = true,
) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    val hasImage = backgroundImagePath != null
    val currentDate = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }
    val currentTime = remember {
        LocalTime.now().format(DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault()))
    }
    // "7:00 AM" -> digits + meridiem, styled separately like the alarm cards.
    val digits = time.substringBeforeLast(' ', time)
    val meridiem = time.substringAfterLast(' ', "").takeIf { it != time }.orEmpty()

    RevealScope {
        RingStage(
            liveBlur = liveBlur,
            modifier = modifier.fillMaxSize().background(colors.background),
            backdrop = {
                if (backgroundImagePath != null) {
                    AsyncImage(
                        model = File(backgroundImagePath),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Guarantees text/button legibility over an arbitrary user photo — not optional.
                    // (Bedrock has no text shadows, so the scrim carries the clock on its own.)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to Color.Black.copy(alpha = 0.22f),
                                    0.55f to Color.Black.copy(alpha = 0.42f),
                                    1f to Color.Black.copy(alpha = 0.78f)
                                )
                            )
                    )
                } else {
                    BreathingAura(Modifier.fillMaxSize())
                }
            }
        ) {
            // Independent of clockPosition — always pinned near the top.
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .revealOnEnter(0)
                        .padding(top = 64.dp)
                        .padding(horizontal = BrSpacing.XXL),
                    textAlign = TextAlign.Center,
                    style = type.headline,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Clock + date move together between top/middle per clockPosition.
            Column(
                modifier = Modifier
                    .align(if (clockPosition == AlarmClockPosition.TOP) Alignment.TopCenter else Alignment.Center)
                    .padding(top = if (clockPosition == AlarmClockPosition.TOP) 140.dp else 0.dp)
                    .padding(horizontal = BrSpacing.XXL)
                    .revealOnEnter(1, offsetY = BrSpacing.XL)
                    .semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row {
                    Text(
                        text = digits,
                        modifier = Modifier.alignByBaseline(),
                        maxLines = 1,
                        softWrap = false,
                        style = type.numeral.copy(fontSize = ClockTextSize, lineHeight = ClockLineHeight),
                        color = colors.textPrimary
                    )
                    if (meridiem.isNotEmpty()) {
                        Spacer(Modifier.width(BrSpacing.S))
                        Text(
                            text = meridiem,
                            style = type.title.copy(fontWeight = FontWeight.Light),
                            color = colors.textSecondary,
                            maxLines = 1,
                            modifier = Modifier.alignByBaseline()
                        )
                    }
                }
                Spacer(Modifier.height(BrSpacing.XS))
                Text(
                    text = currentDate,
                    maxLines = 1,
                    softWrap = false,
                    style = type.bodyLarge,
                    color = colors.textSecondary
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .revealOnEnter(3, offsetY = BrSpacing.XL)
                    .padding(horizontal = BrSpacing.XL, vertical = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (quote != null) {
                    QuoteCard(quote = quote, onPhoto = hasImage)
                    Spacer(Modifier.height(BrSpacing.XL))
                }
                when (buttonLayout) {
                    AlarmButtonLayout.BOTH -> Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(BrSpacing.M)
                    ) {
                        SnoozeButton(onSnooze, ButtonVariant.Glass, Modifier.weight(1f))
                        DismissButton(dismissLabel, onDismiss, Modifier.weight(1f))
                    }
                    // Alone, Snooze is the one action on screen, so it takes the ink pill.
                    AlarmButtonLayout.SNOOZE_ONLY ->
                        SnoozeButton(onSnooze, ButtonVariant.Ink, Modifier.fillMaxWidth())
                    AlarmButtonLayout.DISMISS_ONLY ->
                        DismissButton(dismissLabel, onDismiss, Modifier.fillMaxWidth())
                }

                Spacer(Modifier.height(BrSpacing.L))
                Text(
                    text = "It's currently $currentTime",
                    style = type.figure,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/**
 * The screen's stage: [backdrop] fills it and [content] floats above. With [liveBlur], glass in
 * [content] frosts the backdrop through a [GlassBackdrop]. Without it, glass is told there is no
 * blur source, so it settles to its solid frosted fallback (the scaled Settings preview).
 */
@Composable
private fun RingStage(
    liveBlur: Boolean,
    modifier: Modifier,
    backdrop: @Composable BoxScope.() -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    if (liveBlur) {
        GlassBackdrop(modifier = modifier, backdrop = backdrop, content = content)
    } else {
        Box(modifier) {
            Box(Modifier.matchParentSize(), content = backdrop)
            CompositionLocalProvider(LocalHazeState provides null) {
                content()
            }
        }
    }
}

/**
 * The default backdrop: a full-bleed aura in the current accent whose lights drift, veiled by the
 * page tone that thins and thickens over one slow breath. It replaces the old pulsing bloom and
 * sonar rings: alive, but nothing to look at. The breath is read in the draw phase and ticks at
 * the drift's cadence, so it never recomposes and doesn't add blur passes to the glass above.
 */
@Composable
private fun BreathingAura(modifier: Modifier = Modifier) {
    val colors = BedrockTheme.colors
    val accent = colors.accent
    val aura = remember(accent) { Aura.of(accent).copy(shade = RingAuraShade) }
    val veil = colors.background
    val breath = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = SystemClock.uptimeMillis()
        while (isActive) {
            val t = ((SystemClock.uptimeMillis() - start) % BreathMillis) / BreathMillis.toFloat()
            // 0 -> 1 -> 0 over one breath, easing in and out at both ends.
            val swing = if (t < 0.5f) t * 2f else (1f - t) * 2f
            breath.floatValue = BrMotion.Soft.transform(swing)
            delay(BreathFrameMillis)
        }
    }
    Box(
        modifier
            .aura(aura, drift = true)
            .drawBehind {
                val inhale = breath.floatValue
                drawRect(veil.copy(alpha = VeilDim + (VeilBright - VeilDim) * inhale))
            }
    )
}

/** Snooze: frosted beside Dismiss, ink when it's the only action. */
@Composable
private fun SnoozeButton(onSnooze: () -> Unit, variant: ButtonVariant, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    BrButton(
        text = "Snooze",
        onClick = {
            // Ink buttons tick on their own; glass ones don't, and Snooze always confirmed.
            if (variant != ButtonVariant.Ink) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            onSnooze()
        },
        modifier = modifier.height(RingButtonHeight),
        variant = variant,
        icon = painterResource(R.drawable.ic_snooze)
    )
}

/** Dismiss (or Stop, for timers): the warm-white ink pill, the one loud thing on screen. */
@Composable
private fun DismissButton(text: String, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    BrButton(
        text = text,
        onClick = onDismiss,
        modifier = modifier.height(RingButtonHeight),
        variant = ButtonVariant.Ink,
        icon = rememberVectorPainter(Icons.Rounded.AlarmOff)
    )
}

/**
 * The wake-up quote on frosted glass over the aura or photo: a quiet accent quotation mark, the
 * quote, and its author in a softer tone. Over a photo the glass is heavier, since the photo can
 * be anything.
 */
@Composable
private fun QuoteCard(quote: Quote, onPhoto: Boolean) {
    val colors = BedrockTheme.colors
    val type = BedrockTheme.typography
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = BrShapes.XLarge,
        tint = if (onPhoto) colors.glassStrong else colors.glass,
        contentPadding = PaddingValues(horizontal = BrSpacing.XL, vertical = BrSpacing.L)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "“",
                style = type.title.copy(lineHeight = type.title.fontSize),
                color = colors.accentSoft
            )
            Text(
                quote.text,
                style = type.bodyLarge,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(BrSpacing.S))
            Text(
                "— ${quote.author}",
                style = type.caption,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
