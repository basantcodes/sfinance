package com.example.data.nepali

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Port of nepali-date-converter logic for Bikram Sambat (BS) <-> Gregorian (AD) conversions.
 * Supports BS calendar years 2000 to 2090 (covers 1943 to 2034+ AD).
 */
object NepaliDateConverter {

    val nepaliMonths = listOf(
        "Baisakh", "Jestha", "Ashar", "Shrawan", "Bhadra", "Ashwin",
        "Kartik", "Mangsir", "Poush", "Magh", "Falgun", "Chaitra"
    )

    val nepaliMonthsNp = listOf(
        "बैशाख", "जेठ", "असार", "श्रावण", "भाद्र", "असोज",
        "कार्तिक", "मंसिर", "पौष", "माघ", "फाल्गुन", "चैत"
    )

    // Number of days in each month of BS years starting from 2000 to 2090
    // Each entry has 12 integers representing the number of days in Baisakh..Chaitra
    private val bsYearData: Map<Int, IntArray> = mapOf(
        2000 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2001 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2002 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2003 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2004 to intArrayOf(30, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2005 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2006 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2007 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2008 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 29, 31),
        2009 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2010 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2011 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2012 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2013 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2014 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2015 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2016 to intArrayOf(31, 31, 31, 32, 31, 31, 29, 30, 30, 29, 30, 30),
        2017 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2018 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2019 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2020 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2021 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2022 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2023 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2024 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2025 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2026 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2027 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2028 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2029 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2030 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2031 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2032 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2033 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2034 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2035 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2036 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2037 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2038 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2039 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2040 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2041 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2042 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2043 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2044 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2045 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2046 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2047 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2048 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2049 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2050 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2051 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2052 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2053 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2054 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2055 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2056 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2057 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2058 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2059 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2060 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2061 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2062 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2063 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2064 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2065 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2066 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2067 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2068 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2069 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2070 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2071 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2072 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2073 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2074 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2075 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2076 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 30),
        2077 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2078 to intArrayOf(31, 31, 31, 32, 31, 31, 30, 29, 30, 29, 30, 30),
        2079 to intArrayOf(31, 31, 32, 31, 31, 31, 30, 29, 30, 29, 30, 30),
        2080 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2081 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2082 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2083 to intArrayOf(31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30),
        2084 to intArrayOf(31, 31, 32, 31, 31, 30, 30, 30, 29, 30, 30, 30),
        2085 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2086 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30),
        2087 to intArrayOf(31, 31, 32, 32, 31, 30, 30, 29, 30, 29, 30, 30),
        2088 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 29, 30, 31),
        2089 to intArrayOf(31, 32, 31, 32, 31, 30, 30, 30, 29, 30, 29, 31),
        2090 to intArrayOf(31, 31, 32, 31, 32, 30, 30, 29, 30, 29, 30, 30)
    )

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    private fun getBaisakh1Ad(bsYear: Int): Calendar {
        val adYear = bsYear - 57
        val day = if (isLeapYear(adYear)) 13 else 14
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(adYear, Calendar.APRIL, day, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    data class BsDate(val year: Int, val month: Int, val day: Int) {
        val monthName: String get() = nepaliMonths.getOrElse(month - 1) { "" }
        val formatted: String get() = String.format(Locale.US, "%04d/%02d/%02d", year, month, day)
        val readable: String get() = "$monthName $day, $year"
    }

    data class AdDate(val year: Int, val month: Int, val day: Int)

    fun adToBs(date: Date): BsDate {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            time = date
        }
        val y = calendar.get(Calendar.YEAR)
        val m = calendar.get(Calendar.MONTH) + 1
        val d = calendar.get(Calendar.DAY_OF_MONTH)
        return adToBs(y, m, d)
    }

    fun adToBs(epochMillis: Long): BsDate {
        return adToBs(Date(epochMillis))
    }

    fun adToBs(adYear: Int, adMonth: Int, adDay: Int): BsDate {
        val baisakh1DayInCurrentAdYear = if (isLeapYear(adYear)) 13 else 14
        val (bsYear, baisakh1Cal) = if (adMonth > 4 || (adMonth == 4 && adDay >= baisakh1DayInCurrentAdYear)) {
            val y = adYear + 57
            y to Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(adYear, Calendar.APRIL, baisakh1DayInCurrentAdYear, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } else {
            val y = adYear + 56
            val prevAdYear = adYear - 1
            val prevBaisakh1Day = if (isLeapYear(prevAdYear)) 13 else 14
            y to Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
                set(prevAdYear, Calendar.APRIL, prevBaisakh1Day, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val targetCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(adYear, adMonth - 1, adDay, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffDays = ((targetCal.timeInMillis - baisakh1Cal.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
        var remainingDays = diffDays.coerceAtLeast(0)

        var bsMonth = 1
        var bsDay = 1

        val months = bsYearData[bsYear] ?: bsYearData[2083]!!
        for (m in 0 until 12) {
            val daysInMonth = months[m]
            if (remainingDays >= daysInMonth) {
                remainingDays -= daysInMonth
                bsMonth++
            } else {
                bsDay += remainingDays
                remainingDays = 0
                break
            }
        }

        return BsDate(bsYear, bsMonth.coerceIn(1, 12), bsDay)
    }

    fun bsToAd(bsYear: Int, bsMonth: Int, bsDay: Int): Date {
        val baisakh1Cal = getBaisakh1Ad(bsYear)
        val months = bsYearData[bsYear] ?: bsYearData[2083]!!
        var dayOffset = (bsDay - 1).coerceAtLeast(0)
        val safeMonth = bsMonth.coerceIn(1, 12)
        for (m in 0 until (safeMonth - 1)) {
            dayOffset += months[m]
        }

        baisakh1Cal.add(Calendar.DAY_OF_YEAR, dayOffset)
        return baisakh1Cal.time
    }

    fun parseBsDate(bsStr: String): BsDate? {
        val parts = bsStr.split("/", "-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                return BsDate(y, m, d)
            }
        }
        return null
    }

    /**
     * Formats both Gregorian (AD) and Bikram Sambat (BS) together.
     * Example format specified in prompt: "Jul 19, 2026 (Ashar 5, 2083)"
     */
    fun formatDualDate(epochMillis: Long): String {
        val adFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val adStr = adFormat.format(Date(epochMillis))
        val bs = adToBs(epochMillis)
        return "$adStr (${bs.monthName} ${bs.day}, ${bs.year})"
    }

    fun formatDualDate(date: Date): String = formatDualDate(date.time)

    fun getDaysInBsMonth(year: Int, month: Int): Int {
        val months = bsYearData[year] ?: bsYearData[2080]!!
        return months.getOrElse(month - 1) { 30 }
    }
}
