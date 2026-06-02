package com.accounting.newapp.report

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class ReportRangesTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private val clock = LocalDateTime.of(2026, 6, 2, 14, 30).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun weekStartsOnMonday() {
        val range = ReportRanges.current(ReportPeriod.Week, clock, zone)
        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(range.startMillis), zone)

        assertEquals(2026, start.year)
        assertEquals(6, start.monthValue)
        assertEquals(1, start.dayOfMonth)
    }

    @Test
    fun monthStartsOnFirstDay() {
        val range = ReportRanges.current(ReportPeriod.Month, clock, zone)
        val start = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(range.startMillis), zone)

        assertEquals(1, start.dayOfMonth)
    }
}

