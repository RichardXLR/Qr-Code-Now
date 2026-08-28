package com.richardittou.qrcodenow.benchmark

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun criticalUserJourneys() = baselineProfileRule.collect(
        packageName = PACKAGE_NAME,
        includeInStartupProfile = true
    ) {
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.text("Criar")), 5_000)
        device.findObject(By.text("Criar"))?.click()
        device.waitForIdle()
        device.findObject(By.text("Histórico"))?.click()
        device.waitForIdle()
        device.findObject(By.text("Configurações"))?.click()
        device.waitForIdle()
    }

    private companion object {
        const val PACKAGE_NAME = "com.richardittou.qrcodenow"
    }
}
