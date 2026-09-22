package com.example

import com.example.data.nepali.NepaliDateConverter
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ExampleUnitTest {
  @Test
  fun testNepaliDate() {
    val bsForSep18 = NepaliDateConverter.adToBs(2026, 9, 18)
    println("BS for 2026-09-18: ${bsForSep18.year}-${bsForSep18.month}-${bsForSep18.day} (${bsForSep18.monthName})")
    assertEquals(2083, bsForSep18.year)
    assertEquals(6, bsForSep18.month)
    assertEquals(2, bsForSep18.day)
    assertEquals("Ashwin", bsForSep18.monthName)

    val adForBhadra17 = NepaliDateConverter.bsToAd(2083, 5, 17)
    val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = adForBhadra17 }
    println("AD for Bhadra 17, 2083: ${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}")
    assertEquals(2026, cal.get(Calendar.YEAR))
    assertEquals(9, cal.get(Calendar.MONTH) + 1)
    assertEquals(2, cal.get(Calendar.DAY_OF_MONTH))
  }
}
