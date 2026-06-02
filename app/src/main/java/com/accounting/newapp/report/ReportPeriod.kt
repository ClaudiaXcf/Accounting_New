package com.accounting.newapp.report

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

enum class ReportPeriod {
    Day,
    Week,
    Month,
    Year
}

data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
)

object ReportRanges {
    fun current(period: ReportPeriod, clockMillis: Long = System.currentTimeMillis(), zoneId: ZoneId = ZoneId.systemDefault()): DateRange {
        val date = Instant.ofEpochMilli(clockMillis).atZone(zoneId).toLocalDate()
        val startDate = when (period) {
            ReportPeriod.Day -> date
            ReportPeriod.Week -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ReportPeriod.Month -> YearMonth.from(date).atDay(1)
            ReportPeriod.Year -> LocalDate.of(date.year, 1, 1)
        }
        val endDate = when (period) {
            ReportPeriod.Day -> startDate.plusDays(1)
            ReportPeriod.Week -> startDate.plusWeeks(1)
            ReportPeriod.Month -> startDate.plusMonths(1)
            ReportPeriod.Year -> startDate.plusYears(1)
        }
        return DateRange(
            startMillis = startDate.atStartOfDay(zoneId).toInstant().toEpochMilli(),
            endMillis = endDate.atStartOfDay(zoneId).toInstant().toEpochMilli() - 1,
        )
    }
}

fun Long.formatMoney(): String {
    val yuan = this / 100
    val cents = kotlin.math.abs(this % 100)
    return "¥$yuan.${cents.toString().padStart(2, '0')}"
}

