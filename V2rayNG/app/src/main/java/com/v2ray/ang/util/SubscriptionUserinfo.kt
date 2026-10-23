package com.v2ray.ang.util

/**
 * Parses subscription usage metadata.
 *
 * Providers report usage through the `subscription-userinfo` response header
 * (for example `upload=1024; download=2048; total=107374182400; expire=1893456000`),
 * and some panels embed the remaining days in the subscription remarks instead
 * (for example `Premium|⌛1437D`). Header values are the authoritative source;
 * the remarks marker is only a fallback.
 */
object SubscriptionUserinfoParser {

    /**
     * Parsed `subscription-userinfo` values. Byte counts default to 0 when the
     * provider omits them; [expireEpochSeconds] is null when the provider did
     * not report an expiry (or reported 0, which means no expiry).
     */
    data class Userinfo(
        val uploadBytes: Long,
        val downloadBytes: Long,
        val totalBytes: Long,
        val expireEpochSeconds: Long?
    ) {
        val usedBytes: Long
            get() = uploadBytes + downloadBytes
    }

    private val remarksDaysPattern = Regex("""⌛\s*(\d+)\s*D""")

    /**
     * Parses a raw `subscription-userinfo` header value. Returns null when the
     * value is blank or contains no recognized numeric field.
     */
    fun parse(raw: String?): Userinfo? {
        if (raw.isNullOrBlank()) return null
        var upload: Long? = null
        var download: Long? = null
        var total: Long? = null
        var expire: Long? = null
        raw.split(';').forEach { part ->
            val separator = part.indexOf('=')
            if (separator <= 0) return@forEach
            val key = part.substring(0, separator).trim().lowercase()
            val value = parseNumber(part.substring(separator + 1).trim()) ?: return@forEach
            when (key) {
                "upload" -> upload = value
                "download" -> download = value
                "total" -> total = value
                "expire" -> expire = value
            }
        }
        if (upload == null && download == null && total == null && expire == null) return null
        return Userinfo(
            uploadBytes = upload ?: 0L,
            downloadBytes = download ?: 0L,
            totalBytes = total ?: 0L,
            expireEpochSeconds = expire?.takeIf { it > 0L }
        )
    }

    /**
     * Whole days remaining until [expireEpochSeconds]. A partial final day
     * counts as one day; an expiry in the past yields zero or a negative value.
     * Returns null when there is no expiry.
     */
    fun daysRemaining(expireEpochSeconds: Long?, nowEpochSeconds: Long): Long? {
        if (expireEpochSeconds == null) return null
        val remainingSeconds = expireEpochSeconds - nowEpochSeconds
        return if (remainingSeconds <= 0L) {
            Math.floorDiv(remainingSeconds, SECONDS_PER_DAY)
        } else {
            (remainingSeconds + SECONDS_PER_DAY - 1L) / SECONDS_PER_DAY
        }
    }

    /**
     * Extracts the remaining-days marker (for example `⌛1437D`) that some
     * panels embed in subscription remarks. Returns null when absent.
     */
    fun parseDaysFromRemarks(remarks: String?): Long? {
        if (remarks.isNullOrBlank()) return null
        return remarksDaysPattern.find(remarks)?.groupValues?.get(1)?.toLongOrNull()
    }

    /**
     * Display name for a subscription: the remarks text before the `|`
     * separator, with any remaining-days marker removed. Falls back to the
     * trimmed remarks when cleaning would leave nothing.
     */
    fun displayName(remarks: String?): String {
        if (remarks.isNullOrBlank()) return ""
        val cleaned = remarks.substringBefore('|')
            .replace(remarksDaysPattern, "")
            .replace("⌛", "")
            .trim()
        return cleaned.ifEmpty { remarks.trim() }
    }

    private fun parseNumber(value: String): Long? =
        value.toLongOrNull() ?: value.toDoubleOrNull()?.toLong()

    private const val SECONDS_PER_DAY = 86_400L
}
