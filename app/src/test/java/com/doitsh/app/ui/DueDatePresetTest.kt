package com.doitsh.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

/**
 * The new-task sheet's one-tap due dates, and the round trip between a date and the
 * preset that describes it.
 *
 * The round trip is what the sheet relies on to decide whether to highlight a preset
 * button or print a custom date row, so a date picked from the calendar that happens to
 * land on a preset day has to come back as that preset — otherwise the same day would
 * render two different ways depending on which control produced it.
 */
class DueDatePresetTest {

    private val today = LocalDate.of(2026, 10, 16) // a Friday

    @Test
    fun todayIsToday() {
        assertEquals(today, dueDateForPreset(DueDatePreset.TODAY, today))
    }

    @Test
    fun tomorrowIsTheNextCalendarDay() {
        assertEquals(LocalDate.of(2026, 10, 17), dueDateForPreset(DueDatePreset.TOMORROW, today))
    }

    @Test
    fun nextWeekIsSevenDaysOutNotTheComingMonday() {
        // The distinction that matters: 2026-10-16 is a Friday. "Next week" read as the
        // coming Monday would be the 19th — three days, not seven. The button is pinned
        // to a fixed offset so it does the same thing whatever day the sheet is opened.
        assertEquals(LocalDate.of(2026, 10, 23), dueDateForPreset(DueDatePreset.NEXT_WEEK, today))
        assertNull(presetForDueDate(LocalDate.of(2026, 10, 19), today))
    }

    @Test
    fun everyPresetResolvesToItsOwnDate() {
        // The three must not collide, or `presetForDueDate` would return whichever came
        // first in the enum rather than the one the user pressed.
        val dates = DueDatePreset.entries.map { dueDateForPreset(it, today) }
        assertEquals(dates.size, dates.toSet().size)
    }

    @Test
    fun aPresetDateResolvesBackToItsPreset() {
        DueDatePreset.entries.forEach { preset ->
            assertEquals(preset, presetForDueDate(dueDateForPreset(preset, today), today))
        }
    }

    @Test
    fun anUnrelatedDateResolvesToNoPreset() {
        // The custom-date case: the sheet shows the date itself with a clear button.
        assertNull(presetForDueDate(LocalDate.of(2026, 11, 30), today))
        assertNull(presetForDueDate(today.minusDays(1), today))
    }

    @Test
    fun thePresetsDoNotDriftWithTheDayOfWeek() {
        // Swept across a fortnight so a hand-written "+1 day / +7 days" cannot be
        // accidentally correct-looking on one starting day. The offsets must hold on
        // every day, including across a month and a weekend boundary.
        (0..13).forEach { offset ->
            val start = LocalDate.of(2026, 10, 26).plusDays(offset.toLong())
            assertEquals(start, dueDateForPreset(DueDatePreset.TODAY, start))
            assertEquals(start.plusDays(1), dueDateForPreset(DueDatePreset.TOMORROW, start))
            assertEquals(start.plusDays(7), dueDateForPreset(DueDatePreset.NEXT_WEEK, start))
        }
    }

    @Test
    fun presetDatesSurviveTheCalendarMonthBoundary() {
        // 2026-10-30 + 7 lands in November, and 2026-12-31 + 7 lands in January. The
        // offsets are `plusDays`/`plusWeeks` rather than field arithmetic precisely so
        // this is not something that has to be handled.
        assertEquals(
            LocalDate.of(2026, 11, 6),
            dueDateForPreset(DueDatePreset.NEXT_WEEK, LocalDate.of(2026, 10, 30)),
        )
        assertEquals(
            LocalDate.of(2027, 1, 7),
            dueDateForPreset(DueDatePreset.NEXT_WEEK, LocalDate.of(2026, 12, 31)),
        )
    }
}

/**
 * The `DatePicker` <-> epoch-millis conversion.
 *
 * `DatePickerState.selectedDateMillis` is the instant of *midnight UTC* on the tapped
 * day, and it expects the same shape when seeded. Converting through the device's local
 * zone instead shifts the day by one for every user west of Greenwich; these tests pin
 * the UTC reading, and they are the reason the two helpers live in `DueDatePreset.kt`
 * instead of being inlined at the call site.
 */
class DatePickerUtcConversionTest {

    @Test
    fun aDateConvertsToMidnightUtcOfThatSameDay() {
        assertEquals(0L, LocalDate.of(1970, 1, 1).toEpochMillisUtc())
        // 2026-10-16T00:00:00Z, checked against the platform rather than computed here.
        assertEquals(1_792_108_800_000L, LocalDate.of(2026, 10, 16).toEpochMillisUtc())
    }

    @Test
    fun millisConvertBackToTheSameDate() {
        val date = LocalDate.of(2026, 10, 16)
        assertEquals(date, date.toEpochMillisUtc().toLocalDateUtc())
    }

    @Test
    fun theRoundTripHoldsFarFromEpochAndAcrossYearEnds() {
        listOf(
            LocalDate.of(1970, 1, 1),
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 12, 31),
            LocalDate.of(2035, 6, 15),
        ).forEach { date ->
            assertEquals(date, date.toEpochMillisUtc().toLocalDateUtc())
        }
    }

    @Test
    fun aMidnightUtcInstantIsNotShiftedIntoTheAdjacentDay() {
        // The failure this guards: an implementation using the system default zone would
        // return the 15th for anyone west of UTC and the 16th for anyone east of it, so
        // the assertion is deliberately made against the UTC reading and would fail for
        // exactly one half of the world under a local-zone implementation.
        val millis = LocalDate.of(2026, 10, 16).toEpochMillisUtc()
        assertEquals(LocalDate.of(2026, 10, 16), millis.toLocalDateUtc())
        // One minute before midnight UTC is still the previous day.
        assertEquals(LocalDate.of(2026, 10, 15), (millis - 60_000L).toLocalDateUtc())
    }
}
