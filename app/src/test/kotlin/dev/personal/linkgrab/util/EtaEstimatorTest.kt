package dev.personal.linkgrab.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EtaEstimatorTest {
    @Test fun steadySpeedEstimates() {
        val estimator = EtaEstimator()
        estimator.update(0, 1_000, 0)
        estimator.update(300, 1_000, 3_000)
        val result = estimator.update(600, 1_000, 6_000)
        assertEquals(EtaState.ESTIMATING, result.state)
        assertTrue(result.etaSeconds != null && result.etaSeconds in 3..5)
    }

    @Test fun spikeThenDropIsSmoothed() {
        val estimator = EtaEstimator()
        estimator.update(0, 10_000, 0)
        estimator.update(5_000, 10_000, 5_000)
        val result = estimator.update(5_100, 10_000, 6_000)
        assertTrue(result.speedBytesPerSecond < 2_000)
    }

    @Test fun stallsAfterTenSeconds() {
        val estimator = EtaEstimator()
        estimator.update(0, 10_000, 0)
        estimator.update(500, 10_000, 3_000)
        assertEquals(EtaState.STALLED, estimator.update(500, 10_000, 13_000).state)
    }

    @Test fun unknownTotalIsApproximate() {
        val estimator = EtaEstimator()
        estimator.update(0, null, 0)
        val result = estimator.update(500, null, 3_000)
        assertTrue(result.approximate)
        assertEquals(null, result.etaSeconds)
    }

    @Test fun mergedProgressUsesCombinedTotal() {
        val estimator = EtaEstimator()
        estimator.update(0, 1_000, 0)
        val result = estimator.update(500, 1_000, 5_000)
        assertEquals(500, result.remainingBytes)
    }

    @Test fun retryCanRestartEstimator() {
        val estimator = EtaEstimator()
        estimator.update(0, 1_000, 0)
        estimator.update(500, 1_000, 5_000)
        val result = estimator.update(0, 1_000, 20_000)
        assertEquals(0, result.downloadedBytes)
        assertEquals(EtaState.STALLED, result.state)
    }

    @Test fun formatsDurations() {
        assertEquals("45 s", EtaFormatter.duration(45))
        assertEquals("3 min 20 s", EtaFormatter.duration(200))
        assertEquals("1 h 5 min", EtaFormatter.duration(3900))
    }
}
