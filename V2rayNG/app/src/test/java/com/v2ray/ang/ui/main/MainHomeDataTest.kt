package com.v2ray.ang.ui.main

import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.SubscriptionItem
import com.v2ray.ang.enums.EConfigType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainHomeDataTest {

    private val now = 1_700_000_000L

    private fun profile(remarks: String = "Server 1", subscriptionId: String = "sub-1") =
        ProfileItem(
            configType = EConfigType.VLESS,
            remarks = remarks,
            subscriptionId = subscriptionId
        )

    @Test
    fun noProfileAndNoSubscription_returnsEmptyHomeData() {
        val home = computeMainHomeData(null, null, now)

        assertEquals("", home.selectedRemarks)
        assertEquals("", home.groupRemarks)
        assertNull(home.daysRemaining)
        assertNull(home.trafficUsedBytes)
        assertNull(home.trafficTotalBytes)
    }

    @Test
    fun profileWithoutSubscription_showsOnlyServerRemarks() {
        val home = computeMainHomeData(profile(), null, now)

        assertEquals("Server 1", home.selectedRemarks)
        assertEquals("", home.groupRemarks)
        assertNull(home.daysRemaining)
        assertNull(home.trafficUsedBytes)
        assertNull(home.trafficTotalBytes)
    }

    @Test
    fun userinfoHeader_drivesDaysAndTraffic() {
        val expire = now + 10L * 86_400L
        val subscription = SubscriptionItem(
            remarks = "Premium-SanHtooOo|⌛1437D",
            userinfoRaw = "upload=100; download=200; total=1000; expire=$expire"
        )

        val home = computeMainHomeData(profile(), subscription, now)

        // Userinfo wins over the remarks marker for the day count.
        assertEquals(10L, home.daysRemaining)
        assertEquals(300L, home.trafficUsedBytes)
        assertEquals(1000L, home.trafficTotalBytes)
        assertEquals("Premium-SanHtooOo", home.groupRemarks)
    }

    @Test
    fun remarksMarker_isFallbackWhenNoUserinfo() {
        val subscription = SubscriptionItem(remarks = "Premium-SanHtooOo|⌛1437D")

        val home = computeMainHomeData(profile(), subscription, now)

        assertEquals(1437L, home.daysRemaining)
        assertNull(home.trafficUsedBytes)
        assertNull(home.trafficTotalBytes)
        assertEquals("Premium-SanHtooOo", home.groupRemarks)
    }

    @Test
    fun remarksMarker_isFallbackWhenUserinfoHasNoExpire() {
        val subscription = SubscriptionItem(
            remarks = "Premium|⌛30D",
            userinfoRaw = "upload=1; download=2; total=100"
        )

        val home = computeMainHomeData(profile(), subscription, now)

        assertEquals(30L, home.daysRemaining)
        assertEquals(3L, home.trafficUsedBytes)
        assertEquals(100L, home.trafficTotalBytes)
    }

    @Test
    fun userinfoWithZeroTotal_keepsUsedWithZeroTotal() {
        // A total of 0 means the provider reports no quota; the UI renders
        // the total as unlimited, so both values stay available.
        val subscription = SubscriptionItem(
            remarks = "Premium",
            userinfoRaw = "upload=1; download=2; total=0"
        )

        val home = computeMainHomeData(profile(), subscription, now)

        assertEquals(3L, home.trafficUsedBytes)
        assertEquals(0L, home.trafficTotalBytes)
    }

    @Test
    fun groupIdAndLastUpdated_comeFromProfileAndSubscription() {
        val subscription = SubscriptionItem(
            remarks = "Premium",
            lastUpdated = 1_699_000_000_000L
        )

        val home = computeMainHomeData(profile(subscriptionId = "sub-9"), subscription, now)

        assertEquals("sub-9", home.homeGroupId)
        assertEquals(1_699_000_000_000L, home.homeLastUpdatedEpochMs)
    }

    @Test
    fun lastUpdatedUnset_staysNull() {
        val subscription = SubscriptionItem(remarks = "Premium")

        val home = computeMainHomeData(profile(), subscription, now)

        assertEquals("sub-1", home.homeGroupId)
        assertNull(home.homeLastUpdatedEpochMs)
    }

    @Test
    fun expiredUserinfo_keepsNegativeDays() {
        val subscription = SubscriptionItem(
            remarks = "Premium|⌛1437D",
            userinfoRaw = "upload=0; download=0; total=100; expire=${now - 3_600L}"
        )

        val home = computeMainHomeData(profile(), subscription, now)

        // Authoritative userinfo says expired; the remarks marker must not win.
        assertEquals(-1L, home.daysRemaining)
    }
}
