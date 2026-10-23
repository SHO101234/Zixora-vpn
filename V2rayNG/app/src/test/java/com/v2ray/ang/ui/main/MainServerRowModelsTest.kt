package com.v2ray.ang.ui.main

import com.v2ray.ang.enums.EConfigType
import com.v2ray.ang.dto.entities.ProfileItem
import com.v2ray.ang.dto.entities.ServersCache
import com.v2ray.ang.handler.AngConfigManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainServerRowModelsTest {

    private fun profile(description: String?): ProfileItem = ProfileItem(
        configType = EConfigType.VLESS,
        remarks = "Premium-Test",
        description = description,
        server = "gov.innyshop.example.com",
        serverPort = "443",
        network = "ws",
        security = "tls",
    )

    @Test
    fun storedGeneratedAddressDescriptionIsHidden() {
        val p = profile(null)
        val generated = AngConfigManager.generateDescription(p)
        val row = buildServerRowUiModel(
            ServersCache(guid = "g1", profile = p.copy(description = generated)),
            "Sub",
        )
        assertEquals("", row.statistics)
    }

    @Test
    fun missingDescriptionDoesNotFallBackToAddress() {
        val row = buildServerRowUiModel(
            ServersCache(guid = "g1", profile = profile(null)),
            "Sub",
        )
        assertEquals("", row.statistics)
    }

    @Test
    fun blankDescriptionDoesNotFallBackToAddress() {
        val row = buildServerRowUiModel(
            ServersCache(guid = "g1", profile = profile("  ")),
            "Sub",
        )
        assertEquals("", row.statistics)
    }

    @Test
    fun userWrittenDescriptionIsKept() {
        val row = buildServerRowUiModel(
            ServersCache(guid = "g1", profile = profile("My note")),
            "Sub",
        )
        assertEquals("My note", row.statistics)
    }
}
