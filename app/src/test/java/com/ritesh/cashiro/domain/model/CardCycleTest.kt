package com.ritesh.cashiro.domain.model

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class CardCycleTest {
    private fun d(m: Int, day: Int, y: Int = 2026) = LocalDate.of(y, m, day)

    @Test fun closingIsThisMonthOnceReached() {
        assertEquals(d(10, 5), CardCycle.lastClosing(5, d(10, 5)))
        assertEquals(d(9, 5), CardCycle.lastClosing(5, d(10, 4)))
        assertEquals(d(11, 5), CardCycle.nextClosing(5, d(10, 5)))
    }

    @Test fun lateDaysUseTheMonthsLastDay() {
        assertEquals(d(9, 30), CardCycle.lastClosing(31, d(10, 15)))
        assertEquals(d(2, 28), CardCycle.lastClosing(31, d(3, 10)))
    }

    @Test fun dueFallsInTheSameMonthWhenAfterClosing() {
        // 账单日 5 号，还款日 25 号
        assertEquals(d(10, 25), CardCycle.dueAfter(d(10, 5), 25))
        // 账单日 20 号，还款日次月 8 号
        assertEquals(d(11, 8), CardCycle.dueAfter(d(10, 20), 8))
        assertEquals(d(1, 8, 2027), CardCycle.dueAfter(d(12, 20), 8))
    }

    @Test fun nextDueIsTodayOrLater() {
        assertEquals(d(10, 25), CardCycle.nextDue(25, d(10, 25)))
        assertEquals(d(11, 25), CardCycle.nextDue(25, d(10, 26)))
    }
}
