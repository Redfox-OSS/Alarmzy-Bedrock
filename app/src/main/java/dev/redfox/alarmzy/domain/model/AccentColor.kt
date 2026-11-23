package dev.redfox.alarmzy.domain.model

/**
 * Accent options: Bedrock's six mid-bright families. [swatch] is the family's main tone, shown in
 * the picker; the full tonal family lives in `ui/theme` (`toBedrock()`).
 */
enum class AccentColor(val displayName: String, val swatch: Long) {
    EMBER("Ember", 0xFFF2884B),
    TIDE("Tide", 0xFF4FA6E6),
    MOSS("Moss", 0xFF5CC28A),
    ROSE("Rose", 0xFFEC6F8E),
    IRIS("Iris", 0xFF8E8BF0),
    SAND("Sand", 0xFFE3B04F);

    companion object {
        val DEFAULT = EMBER

        /** Parses a persisted name, falling back to [DEFAULT] for removed/unknown accents. */
        fun fromName(name: String?): AccentColor =
            entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
