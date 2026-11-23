package dev.redfox.alarmzy.domain.model

/**
 * The dark neutral ramp the app sits on: Bedrock's Basalt (warm graphite), Slate (cool) and
 * Pitch (true black for OLED). Persisted by name.
 */
enum class DarkVariant(val displayName: String) {
    BASALT("Basalt"),
    SLATE("Slate"),
    PITCH("Pitch black");

    companion object {
        val DEFAULT = BASALT

        fun fromName(name: String?): DarkVariant = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}
