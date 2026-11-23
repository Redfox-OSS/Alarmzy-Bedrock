package dev.redfox.alarmzy.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTicksTest {

    private val end = 1_000_000_000L

    @Test
    fun stepIsOnePercentWithinBounds() {
        assertEquals(6_000L, TimerTicks.stepMillis(10 * 60_000L))      // 10 min → 6 s
        assertEquals(5_000L, TimerTicks.stepMillis(60_000L))           // 1 min → floor of 5 s
        assertEquals(60_000L, TimerTicks.stepMillis(3 * 3_600_000L))   // 3 h → capped at a minute
    }

    @Test
    fun firstTickLandsOnTheFirstBoundary() {
        val duration = 10 * 60_000L
        val start = end - duration
        assertEquals(start + 6_000L, TimerTicks.next(end, duration, start))
    }

    @Test
    fun ticksStayAlignedToTheEndAfterAResume() {
        // Resumed with 7m 12s left: the grid counts 6 s steps back from the end, so the next
        // tick is at 7m 06s left, not 6 s after the resume.
        val duration = 10 * 60_000L
        val now = end - (7 * 60_000L + 12_000L) + 1_000L // resumed 1 s off the grid
        assertEquals(end - (7 * 60_000L + 6_000L), TimerTicks.next(end, duration, now))
    }

    @Test
    fun skipsABoundaryAlarmManagerCantReach() {
        // 4 s to the next boundary is under the 5 s minimum, so it aims for the one after.
        val duration = 10 * 60_000L
        val now = end - (100 * 6_000L - 2_000L) // 2 s after a boundary → next is 4 s away
        val next = TimerTicks.next(end, duration, now)!!
        assertEquals(10_000L, next - now)
        assertEquals(0L, (end - next) % 6_000L)
    }

    @Test
    fun aLateTickDoesNotSkipTheNextFiveSecondStep() {
        // 5 s steps; this tick arrived 30 ms after its boundary (85 s left).
        val duration = 90_000L
        val now = end - 85_000L + 30L
        assertEquals(end - 80_000L, TimerTicks.next(end, duration, now))
    }

    @Test
    fun noTickInTheLastStep() {
        val duration = 10 * 60_000L
        assertNull(TimerTicks.next(end, duration, end - 8_000L))
        assertNull(TimerTicks.next(end, duration, end - 1_000L))
        assertNull(TimerTicks.next(end, duration, end + 1_000L))
    }

    @Test
    fun everyTickIsInTheFutureAndOnTheGrid() {
        val duration = 90_000L // 5 s steps
        var now = end - duration
        var count = 0
        while (true) {
            val next = TimerTicks.next(end, duration, now) ?: break
            assertTrue(next - now >= TimerTicks.MIN_STEP_MILLIS - TimerTicks.LEAD_SLACK_MILLIS)
            assertEquals(0L, (end - next) % 5_000L)
            now = next
            count++
        }
        assertEquals(17, count) // 85 s … 5 s left; the last step is left to the finish alarm
    }
}
