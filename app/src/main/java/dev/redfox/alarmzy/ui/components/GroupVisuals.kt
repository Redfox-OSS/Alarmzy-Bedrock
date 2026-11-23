package dev.redfox.alarmzy.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import dev.redfox.alarmzy.R
import dev.redfox.alarmzy.domain.model.GroupIcon

@get:DrawableRes
val GroupIcon?.drawableRes: Int
    get() = when (this) {
        null -> R.drawable.ic_folder
        GroupIcon.MORNING -> R.drawable.ic_group_morning
        GroupIcon.FITNESS -> R.drawable.ic_group_fitness
        GroupIcon.MEDICATION -> R.drawable.ic_group_medication
        GroupIcon.WORK -> R.drawable.ic_group_work
        GroupIcon.COFFEE -> R.drawable.ic_group_coffee
        GroupIcon.NIGHT -> R.drawable.ic_group_night
        GroupIcon.STUDY -> R.drawable.ic_group_study
        GroupIcon.TRANSIT -> R.drawable.ic_group_transit
        GroupIcon.COOKING -> R.drawable.ic_group_cooking
        GroupIcon.MINDFULNESS -> R.drawable.ic_group_mindfulness
        GroupIcon.HOME -> R.drawable.ic_group_home
        GroupIcon.HYDRATION -> R.drawable.ic_group_hydration
        GroupIcon.DETOX -> R.drawable.ic_group_detox
        GroupIcon.PET -> R.drawable.ic_group_pet
        GroupIcon.TIMER -> R.drawable.ic_group_timer
        GroupIcon.SOCIAL -> R.drawable.ic_group_social
        GroupIcon.GAMING -> R.drawable.ic_group_gaming
        GroupIcon.MUSIC -> R.drawable.ic_group_music
    }

/**
 * Group color swatches (persisted as hex strings): ten mid-bright hues in Bedrock's range, so a
 * group's colour sits beside the accent instead of shouting over it. Where a hue matches a Bedrock
 * accent family, it's that family's main tone.
 */
val GroupColorSwatches: List<String> = listOf(
    "#7F86E0", // Indigo
    "#4DB6A6", // Teal
    "#E8676A", // Coral red
    "#F2884B", // Ember
    "#5CC28A", // Moss
    "#B07FD9", // Violet
    "#E3B04F", // Sand
    "#4FA6E6", // Tide
    "#EC6F8E", // Rose
    "#A88E7A", // Stone
)

/** Parses a "#RRGGBB" group color; null for missing/invalid values. */
fun parseGroupColor(hex: String?): Color? =
    hex?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
