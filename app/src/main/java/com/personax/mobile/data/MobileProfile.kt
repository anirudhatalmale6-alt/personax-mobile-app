package com.personax.mobile.data

import java.util.UUID
import kotlin.random.Random

data class MobileProfile(
    val id: String = UUID.randomUUID().toString().take(12),
    var name: String = "",
    var deviceModel: String = "",
    var manufacturer: String = "",
    var androidVer: String = "",
    var sdk: Int = 34,
    var resolution: String = "",
    var dpi: Int = 420,
    var androidId: String = "",
    var imei: String = "",
    var macAddr: String = "",
    var serialNo: String = "",
    var buildId: String = "",
    var fingerprint: String = "",
    var userAgent: String = "",
    var proxy: String = "",
    var proxyType: String = "none",
    var gpsLat: Double = 0.0,
    var gpsLng: Double = 0.0,
    var gpsEnabled: Boolean = false,
    var notes: String = "",
    var group: String = "",
    var isActive: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var lastUsed: Long = 0L
)

data class DeviceTemplate(
    val model: String,
    val manufacturer: String,
    val resolution: String,
    val dpi: Int,
    val androidVer: String,
    val sdk: Int,
    val buildIds: List<String>,
    val userAgents: List<String>
)

object DeviceDatabase {
    val devices = listOf(
        DeviceTemplate("SM-S926B", "Samsung", "1440x3120", 500, "14", 34,
            listOf("UP1A.231005.007"), listOf("Mozilla/5.0 (Linux; Android 14; SM-S926B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("SM-S921B", "Samsung", "1080x2340", 425, "14", 34,
            listOf("UP1A.231005.007"), listOf("Mozilla/5.0 (Linux; Android 14; SM-S921B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("SM-A546B", "Samsung", "1080x2340", 425, "14", 34,
            listOf("UP1A.231005.007"), listOf("Mozilla/5.0 (Linux; Android 14; SM-A546B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("SM-G991B", "Samsung", "1080x2400", 425, "13", 33,
            listOf("TP1A.220624.014"), listOf("Mozilla/5.0 (Linux; Android 13; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")),
        DeviceTemplate("Pixel 8 Pro", "Google", "1344x2992", 489, "14", 34,
            listOf("UD1A.230803.041", "AP2A.240805.005"), listOf("Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("Pixel 8", "Google", "1080x2400", 420, "14", 34,
            listOf("UD1A.230803.041"), listOf("Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("Pixel 7a", "Google", "1080x2400", 420, "14", 34,
            listOf("AP2A.240805.005"), listOf("Mozilla/5.0 (Linux; Android 14; Pixel 7a) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("23078RKD5C", "Xiaomi", "1220x2712", 446, "14", 34,
            listOf("UKQ1.231003.002"), listOf("Mozilla/5.0 (Linux; Android 14; 23078RKD5C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("22101316G", "Xiaomi", "1080x2400", 395, "13", 33,
            listOf("TKQ1.221114.001"), listOf("Mozilla/5.0 (Linux; Android 13; 22101316G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")),
        DeviceTemplate("CPH2449", "OPPO", "1080x2400", 425, "13", 33,
            listOf("TP1A.220905.001"), listOf("Mozilla/5.0 (Linux; Android 13; CPH2449) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")),
        DeviceTemplate("NE2215", "OnePlus", "1440x3216", 525, "14", 34,
            listOf("UP1A.231005.007"), listOf("Mozilla/5.0 (Linux; Android 14; NE2215) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("IN2023", "OnePlus", "1080x2400", 420, "13", 33,
            listOf("TP1A.220905.001"), listOf("Mozilla/5.0 (Linux; Android 13; IN2023) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")),
        DeviceTemplate("V2254A", "vivo", "1080x2400", 425, "14", 34,
            listOf("UP1A.231005.007"), listOf("Mozilla/5.0 (Linux; Android 14; V2254A) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("moto g84 5G", "Motorola", "1080x2400", 400, "14", 34,
            listOf("U1TDS34.94-12-8"), listOf("Mozilla/5.0 (Linux; Android 14; moto g84 5G) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.6422.165 Mobile Safari/537.36")),
        DeviceTemplate("RMX3686", "Realme", "1080x2400", 410, "13", 33,
            listOf("TP1A.220905.001"), listOf("Mozilla/5.0 (Linux; Android 13; RMX3686) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.6099.230 Mobile Safari/537.36")),
    )

    fun generateProfile(name: String = ""): MobileProfile {
        val dev = devices.random()
        val buildId = dev.buildIds.random()
        val hex = List(16) { "0123456789abcdef".random() }.joinToString("")
        val fp = "${dev.manufacturer.lowercase()}/${dev.model}/${dev.model}:${dev.androidVer}/$buildId/${hex.take(8)}:user/release-keys"

        return MobileProfile(
            name = name.ifEmpty { "Profile ${Random.nextInt(1000, 9999)}" },
            deviceModel = dev.model,
            manufacturer = dev.manufacturer,
            androidVer = dev.androidVer,
            sdk = dev.sdk,
            resolution = dev.resolution,
            dpi = dev.dpi,
            androidId = hex,
            imei = generateIMEI(),
            macAddr = generateMAC(),
            serialNo = generateSerial(),
            buildId = buildId,
            fingerprint = fp,
            userAgent = dev.userAgents.random()
        )
    }

    fun regenerateFingerprint(profile: MobileProfile): MobileProfile {
        val fresh = generateProfile(profile.name)
        return profile.copy(
            deviceModel = fresh.deviceModel,
            manufacturer = fresh.manufacturer,
            androidVer = fresh.androidVer,
            sdk = fresh.sdk,
            resolution = fresh.resolution,
            dpi = fresh.dpi,
            androidId = fresh.androidId,
            imei = fresh.imei,
            macAddr = fresh.macAddr,
            serialNo = fresh.serialNo,
            buildId = fresh.buildId,
            fingerprint = fresh.fingerprint,
            userAgent = fresh.userAgent
        )
    }

    private fun generateIMEI(): String {
        val tacs = listOf("35391109", "35462110", "86770903", "35814909", "35926509")
        var base = tacs.random()
        while (base.length < 14) base += Random.nextInt(10).toString()
        var sum = 0
        for (i in 0 until 14) {
            var d = base[i].digitToInt()
            if (i % 2 == 1) { d *= 2; if (d > 9) d -= 9 }
            sum += d
        }
        val check = (10 - (sum % 10)) % 10
        return base + check.toString()
    }

    private fun generateMAC(): String {
        val bytes = ByteArray(6).also { Random.nextBytes(it) }
        bytes[0] = ((bytes[0].toInt() and 0xFE) or 0x02).toByte()
        return bytes.joinToString(":") { "%02x".format(it) }
    }

    private fun generateSerial(): String {
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..11).map { chars.random() }.joinToString("")
    }
}
