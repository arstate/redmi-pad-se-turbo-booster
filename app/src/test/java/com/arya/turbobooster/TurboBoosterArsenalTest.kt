package com.arya.turbobooster

import com.arya.turbobooster.booster.BoosterProfile
import com.arya.turbobooster.booster.TurboBoosterManager
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class TurboBoosterArsenalTest {

    @Test
    fun testRedListGuards() {
        val mandatoryRedList = listOf(
            "com.miui.securitycenter",
            "com.miui.securitycore",
            "com.miui.securityadd",
            "com.xiaomi.finddevice",
            "com.lbe.security.miui",
            "com.miui.home",
            "com.android.systemui",
            "com.android.settings",
            "com.miui.powerkeeper",
            "com.google.android.gms",
            "com.android.vending",
            "com.android.webview",
            "com.google.android.webview"
        )

        for (pkg in mandatoryRedList) {
            assertTrue("Red List must contain critical package: $pkg", TurboBoosterManager.RED_LIST.contains(pkg))
        }
    }

    @Test
    fun testGreenListDisjointFromRedList() {
        for (pkg in TurboBoosterManager.ALL_GREEN_LIST) {
            assertFalse(
                "Green List package $pkg MUST NEVER intersect with Red List!",
                TurboBoosterManager.RED_LIST.contains(pkg)
            )
        }
    }

    @Test
    fun testProfileDefinitions() {
        val profiles = BoosterProfile.values()
        assertEquals(4, profiles.size)
        assertTrue(profiles.contains(BoosterProfile.STOCK))
        assertTrue(profiles.contains(BoosterProfile.FLAGSHIP_BALANCED))
        assertTrue(profiles.contains(BoosterProfile.EXTREME_GAMING_RENDER))
        assertTrue(profiles.contains(BoosterProfile.DEBLOAT_RESTORE))
    }

    @Test
    fun testNativeDaemonArtifactExistsAndIsELF() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val rootDir = if (projectDir.name == "app") projectDir.parentFile else projectDir
        val libFile = File(rootDir, "app/src/main/jniLibs/arm64-v8a/libturbodaemon.so")

        assertTrue("libturbodaemon.so must exist in jniLibs/arm64-v8a/", libFile.exists())
        assertTrue("libturbodaemon.so must not be empty", libFile.length() > 50000)

        val header = ByteArray(4)
        libFile.inputStream().use { it.read(header) }
        assertEquals(0x7F.toByte(), header[0])
        assertEquals('E'.code.toByte(), header[1])
        assertEquals('L'.code.toByte(), header[2])
        assertEquals('F'.code.toByte(), header[3])
    }

    @Test
    fun testNoKuntilanakAudioInAssets() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val rootDir = if (projectDir.name == "app") projectDir.parentFile else projectDir
        val audioDir = File(rootDir, "app/src/main/assets/audio")
        if (audioDir.exists()) {
            val badFile = File(audioDir, "kuntilanak_seram.mp3")
            assertFalse("kuntilanak_seram.mp3 must be purged from repo!", badFile.exists())
        }
    }

    @Test
    fun testNoContradictionsInShellScripts() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val rootDir = if (projectDir.name == "app") projectDir.parentFile else projectDir

        val scriptsDirs = listOf(
            File(rootDir, "app/src/main/assets/scripts"),
            rootDir
        )

        val forbiddenSnippets = listOf(
            "cmd power set-fixed-performance-mode-enabled true",
            "debug.sqlite.journalmode MEMORY",
            "debug.sqlite.wal.syncmode OFF",
            "deviceLevelList \"v:1,c:3,g:3\"",
            "computility.gpulevel 6",
            "extm.bdsize 8192",
            "zram_size 8192",
            "ctl.restart surfaceflinger",
            "kuntilanak_seram",
            "debug.sf.disable_backpressure",
            "debug.sf.latch_unsignaled",
            "debug.sf.vulkan_renderengine"
        )

        for (dir in scriptsDirs) {
            val scripts = dir.listFiles { f -> f.extension == "sh" } ?: emptyArray()
            for (script in scripts) {
                val content = script.readText()
                for (forbidden in forbiddenSnippets) {
                    assertFalse(
                        "Script ${script.name} in ${dir.name} contains forbidden placebo/contradiction: '$forbidden'",
                        content.contains(forbidden)
                    )
                }
            }
        }
    }

    @Test
    fun testApkSizeConstraint() {
        val projectDir = File(System.getProperty("user.dir") ?: ".")
        val rootDir = if (projectDir.name == "app") projectDir.parentFile else projectDir
        val releaseApk = File(rootDir, "release/TurboBooster.apk")
        if (releaseApk.exists()) {
            val sizeMb = releaseApk.length() / (1024.0 * 1024.0)
            assertTrue(
                "Release APK size must strictly be < 3.5 MB, actual: ${String.format("%.2f", sizeMb)} MB",
                releaseApk.length() < 3.5 * 1024 * 1024
            )
        }
    }

    @Test
    fun testTargetGamesList() {
        val pkgs = TurboBoosterManager.TARGET_GAMES.map { it.first }
        assertTrue("PUBG Mobile must be in target games", pkgs.contains("com.tencent.ig"))
        assertTrue("Mobile Legends must be in target games", pkgs.contains("com.mobile.legends"))
        assertTrue("Roblox must be in target games", pkgs.contains("com.roblox.client"))
        assertTrue("CapCut Global must be in target games", pkgs.contains("com.lemon.lvoverseas"))
        assertTrue("CapCut Pad Pro must be in target games", pkgs.contains("com.lemon.ccpadpro"))
    }

    @Test
    fun testProtectedAppsDefault() {
        val protected = TurboBoosterManager.DEFAULT_PROTECTED_APPS
        assertTrue("WhatsApp must be in default protected apps", protected.contains("com.whatsapp"))
        assertTrue("Telegram must be in default protected apps", protected.contains("org.telegram.messenger"))
        assertTrue("Discord must be in default protected apps", protected.contains("com.discord"))
    }
}
