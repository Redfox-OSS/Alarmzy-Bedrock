package dev.redfox.alarmzy.clock

/**
 * When the running timer's Live Update re-posts to move its progress bar. Pure maths, so it's unit
 * tested ([TimerTicksTest]).
 *
 * A promoted notification can't animate its own `ProgressStyle` bar the way the chronometer next
 * to it ticks, so the bar only moves when the notification is re-posted. Ticking once a minute,
 * 60s after each post, left the bar trailing the countdown by up to a minute and frozen for
 * short timers. Instead the bar moves in about [STEPS] even steps, and each tick lands exactly on
 * a step boundary counted back from the end time, so the bar always agrees with the chronometer.
 */
object TimerTicks {
    /** How many steps the bar drains in (about 1% each). */
    const val STEPS = 100

    /**
     * AlarmManager never fires an app's alarm sooner than this after it's set (`min_futurity`),
     * so it's also the shortest step.
     */
    const val MIN_STEP_MILLIS = 5_000L

    /**
     * A boundary this much closer than [MIN_STEP_MILLIS] is still aimed for: a tick delivered a few
     * ms late finds the next 5 s boundary at 4.99 s, and AlarmManager only nudges it by those ms.
     */
    const val LEAD_SLACK_MILLIS = 500L

    /** Long timers still refresh at least once a minute. */
    const val MAX_STEP_MILLIS = 60_000L

    /** The step for a timer of [durationMs]: 1% of it, between 5 seconds and a minute. */
    fun stepMillis(durationMs: Long): Long = (durationMs / STEPS).coerceIn(MIN_STEP_MILLIS, MAX_STEP_MILLIS)

    /**
     * The next tick for a timer ending at [endAtMillis], or null when there's no step boundary left
     * about [MIN_STEP_MILLIS] away before the end (the finish alarm takes over from there).
     * Boundaries sit at `endAt - k * step`; this picks the nearest one AlarmManager can still hit.
     */
    fun next(endAtMillis: Long, durationMs: Long, nowMillis: Long): Long? {
        val step = stepMillis(durationMs)
        val remaining = endAtMillis - nowMillis
        val lead = MIN_STEP_MILLIS - LEAD_SLACK_MILLIS
        if (remaining <= lead) return null
        val k = (remaining - lead) / step
        if (k <= 0L) return null
        return endAtMillis - k * step
    }
}
