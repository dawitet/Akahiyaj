package com.dawitf.akahidegn.benchmark

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.benchmark.macro.macrobenchmark
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StartupBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @OptIn(ExperimentalBaselineProfilesApi::class)
    @Test
    fun startupCold() = benchmarkRule.macrobenchmark(
        uniqueName = "startupCold",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(StartupTimingMetric()),
        iterations = 3,
        startupMode = StartupMode.COLD
    ) {
        pressHome()
        startActivityAndWait()
    }

    @OptIn(ExperimentalBaselineProfilesApi::class)
    @Test
    fun startupWarm() = benchmarkRule.macrobenchmark(
        uniqueName = "startupWarm",
        packageName = "com.dawitf.akahidegn",
        metrics = listOf(StartupTimingMetric()),
        iterations = 5,
        startupMode = StartupMode.WARM
    ) {
        pressHome()
        startActivityAndWait()
    }

    private fun MacrobenchmarkScope.startActivityAndWait() {
        val intent = context.packageManager.getLaunchIntentForPackage("com.dawitf.akahidegn")
        requireNotNull(intent)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        context.startActivity(intent)
        device.waitForIdle()
    }
}
