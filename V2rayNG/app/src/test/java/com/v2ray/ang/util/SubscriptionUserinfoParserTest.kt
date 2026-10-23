package com.v2ray.ang.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class SubscriptionUserinfoParserTest {

    @Test
    fun parse_fullHeader_returnsAllFields() {
        val userinfo = SubscriptionUserinfoParser.parse(
            "upload=1024; download=2048; total=107374182400; expire=1893456000"
        )

        assertEquals(1024L, userinfo?.uploadBytes)
        assertEquals(2048L, userinfo?.downloadBytes)
        assertEquals(3072L, userinfo?.usedBytes)
        assertEquals(107374182400L, userinfo?.totalBytes)
        assertEquals(1893456000L, userinfo?.expireEpochSeconds)
    }

    @Test
    fun parse_missingFields_defaultToZeroAndNullExpire() {
        val userinfo = SubscriptionUserinfoParser.parse("total=5000")

        assertEquals(0L, userinfo?.uploadBytes)
        assertEquals(0L, userinfo?.downloadBytes)
        assertEquals(5000L, userinfo?.totalBytes)
        assertNull(userinfo?.expireEpochSeconds)
    }

    @Test
    fun parse_expireZero_meansNoExpiry() {
        val userinfo = SubscriptionUserinfoParser.parse("upload=1; expire=0")

        assertNull(userinfo?.expireEpochSeconds)
    }

    @Test
    fun parse_decimalValues_areTruncatedToLong() {
        val userinfo = SubscriptionUserinfoParser.parse("upload=1024.9; total=2048.0")

        assertEquals(1024L, userinfo?.uploadBytes)
        assertEquals(2048L, userinfo?.totalBytes)
    }

    @Test
    fun parse_blankOrGarbage_returnsNull() {
        assertNull(SubscriptionUserinfoParser.parse(null))
        assertNull(SubscriptionUserinfoParser.parse(""))
        assertNull(SubscriptionUserinfoParser.parse("   "))
        assertNull(SubscriptionUserinfoParser.parse("not-a-header"))
        assertNull(SubscriptionUserinfoParser.parse("upload=abc; other=1"))
    }

    @Test
    fun daysRemaining_nullExpire_returnsNull() {
        assertNull(SubscriptionUserinfoParser.daysRemaining(null, 1_700_000_000L))
    }

    @Test
    fun daysRemaining_futureExpire_countsPartialDayAsOne() {
        val now = 1_700_000_000L

        assertEquals(1L, SubscriptionUserinfoParser.daysRemaining(now + 3_600L, now))
        assertEquals(1L, SubscriptionUserinfoParser.daysRemaining(now + 86_400L, now))
        assertEquals(2L, SubscriptionUserinfoParser.daysRemaining(now + 86_401L, now))
        assertEquals(1437L, SubscriptionUserinfoParser.daysRemaining(now + 1437L * 86_400L, now))
    }

    @Test
    fun daysRemaining_pastExpire_isZeroOrNegative() {
        val now = 1_700_000_000L

        assertEquals(0L, SubscriptionUserinfoParser.daysRemaining(now, now))
        assertEquals(-1L, SubscriptionUserinfoParser.daysRemaining(now - 3_600L, now))
        assertEquals(-2L, SubscriptionUserinfoParser.daysRemaining(now - 90_000L, now))
    }

    @Test
    fun parseDaysFromRemarks_extractsMarker() {
        assertEquals(1437L, SubscriptionUserinfoParser.parseDaysFromRemarks("Premium-SanHtooOo|⌛1437D"))
        assertEquals(30L, SubscriptionUserinfoParser.parseDaysFromRemarks("Plan ⌛ 30D left"))
    }

    @Test
    fun parseDaysFromRemarks_missingMarker_returnsNull() {
        assertNull(SubscriptionUserinfoParser.parseDaysFromRemarks(null))
        assertNull(SubscriptionUserinfoParser.parseDaysFromRemarks(""))
        assertNull(SubscriptionUserinfoParser.parseDaysFromRemarks("Premium"))
    }

    @Test
    fun displayName_stripsSeparatorAndMarker() {
        assertEquals(
            "Premium-SanHtooOo",
            SubscriptionUserinfoParser.displayName("Premium-SanHtooOo|⌛1437D")
        )
        assertEquals(
            "Premium",
            SubscriptionUserinfoParser.displayName("Premium ⌛1437D")
        )
        assertEquals(
            "Premium",
            SubscriptionUserinfoParser.displayName("  Premium  ")
        )
    }

    @Test
    fun displayName_blank_returnsEmpty() {
        assertEquals("", SubscriptionUserinfoParser.displayName(null))
        assertEquals("", SubscriptionUserinfoParser.displayName(""))
        assertEquals("", SubscriptionUserinfoParser.displayName("   "))
    }

    @Test
    fun displayName_markerOnly_fallsBackToTrimmedRemarks() {
        assertEquals("⌛1437D", SubscriptionUserinfoParser.displayName("⌛1437D"))
    }
}
