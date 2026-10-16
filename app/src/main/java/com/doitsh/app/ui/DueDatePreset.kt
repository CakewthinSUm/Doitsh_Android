package com.doitsh.app.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The one-tap due dates the new-task sheet offers ahead of the calendar.
 *
 * These exist because the overwhelming majority of tasks are due today, tomorrow or
 * some time next week, and making the user open a month grid to say "today" is three
 * taps for something that deserves one.
 */
enum class DueDatePreset { TODAY, TOMORROW, NEXT_WEEK }

/**
 * Resolves a preset against the current day.
 *
 * [DueDatePreset.NEXT_WEEK] is deliberately **seven days out, not next Monday**. "Next
 * week" is genuinely ambiguous — it can mean the coming Monday, or a week from today —
 * and the two disagree by up to six days, which is too large a gap to leave to a coin
 * flip. A fixed offset is the reading that is stable regardless of what day the user
 * opens the sheet, so the button does the same thing every time it is pressed.
 *
 * `today` is a parameter rather than a `LocalDate.now()` call so the rule is a pure
 * function that can be tested without freezing the clock — the same reason
 * `Task.isDueTodayOrEarlier` takes one.
 */
internal fun dueDateForPreset(preset: DueDatePreset, today: LocalDate): LocalDate =
    when (preset) {
        DueDatePreset.TODAY -> today
        DueDatePreset.TOMORROW -> today.plusDays(1)
        DueDatePreset.NEXT_WEEK -> today.plusWeeks(1)
    }

/**
 * The reverse of [dueDateForPreset]: which preset, if any, a chosen date corresponds to.
 *
 * This is what keeps the sheet honest about which of its date affordances is showing as
 * selected. A date that came from the calendar and happens to land on one of the preset
 * days is *reported* as that preset: the row can then highlight the button instead of
 * printing a date underneath it, which is what the user would expect — they cannot tell,
 * and should not have to care, which control produced today's date.
 *
 * `null` means the date matches no preset, so the sheet shows the date itself with a
 * clear button.
 */
internal fun presetForDueDate(dueDate: LocalDate, today: LocalDate): DueDatePreset? =
    DueDatePreset.entries.firstOrNull { dueDateForPreset(it, today) == dueDate }

/**
 * `DatePicker` works in UTC: `selectedDateMillis` is the epoch instant of *midnight UTC*
 * on the day the user tapped, and it expects the same when seeding an initial selection.
 *
 * Converting through the local zone — the intuitive reading of a bare epoch millis —
 * shifts the date by a day for every user west of Greenwich, so a date picked in
 * California would be stored as the day before. These two helpers are the only place
 * that conversion happens, and they are unit-tested in `DueDatePresetTest`.
 */
internal fun LocalDate.toEpochMillisUtc(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

internal fun Long.toLocalDateUtc(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
