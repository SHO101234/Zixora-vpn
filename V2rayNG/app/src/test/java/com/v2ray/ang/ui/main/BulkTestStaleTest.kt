package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BulkTestStaleTest {

    private val now = 1_700_000_000_000L

    @Test
    fun neverTested_isStale() {
        assertTrue(isBulkTestStale(0L, now))
    }

    @Test
    fun justTested_isFresh() {
        assertFalse(isBulkTestStale(now, now))
    }

    @Test
    fun exactlyAtThreshold_isFresh() {
        assertFalse(isBulkTestStale(now - BULK_TEST_STALE_MS, now))
    }

    @Test
    fun oneMillisecondPastThreshold_isStale() {
        assertTrue(isBulkTestStale(now - BULK_TEST_STALE_MS - 1L, now))
    }

    @Test
    fun futureTimestamp_isFresh() {
        assertFalse(isBulkTestStale(now + 60_000L, now))
    }
}
