package dev.redfox.alarmzy.ui.theme

import androidx.compose.runtime.Composable
import dev.redfox.alarmzy.domain.model.AccentColor
import dev.redfox.alarmzy.domain.model.DarkVariant
import dev.redfox.bedrock.theme.BedrockAccent
import dev.redfox.bedrock.theme.BedrockNeutrals
import dev.redfox.bedrock.theme.BedrockTheme

/**
 * Alarmzy's root theme: the Bedrock design system driven by the persisted [AccentColor] /
 * [DarkVariant] settings. Screens read tokens from `BedrockTheme.colors` /
 * `BedrockTheme.typography`.
 */
@Composable
fun AlarmzyTheme(
    accentColor: AccentColor = AccentColor.DEFAULT,
    darkVariant: DarkVariant = DarkVariant.DEFAULT,
    content: @Composable () -> Unit
) {
    BedrockTheme(
        accent = accentColor.toBedrock(),
        neutrals = darkVariant.toBedrock(),
        content = content
    )
}

fun AccentColor.toBedrock(): BedrockAccent = when (this) {
    AccentColor.EMBER -> BedrockAccent.Ember
    AccentColor.TIDE -> BedrockAccent.Tide
    AccentColor.MOSS -> BedrockAccent.Moss
    AccentColor.ROSE -> BedrockAccent.Rose
    AccentColor.IRIS -> BedrockAccent.Iris
    AccentColor.SAND -> BedrockAccent.Sand
}

fun DarkVariant.toBedrock(): BedrockNeutrals = when (this) {
    DarkVariant.BASALT -> BedrockNeutrals.Basalt
    DarkVariant.SLATE -> BedrockNeutrals.Slate
    DarkVariant.PITCH -> BedrockNeutrals.Pitch
}
