package dev.personal.linkgrab.util

import kotlin.math.max

enum class EtaState { CALCULATING, ESTIMATING, STALLED, DONE }

data class EtaSnapshot(
    val downloadedBytes: Long,
    val totalBytes: Long?,
    val speedBytesPerSecond: Double,
    val remainingBytes: Long?,
    val etaSeconds: Long?,
    val state: EtaState,
    val approximate: Boolean = false
)

class EtaEstimator(private val smoothingWindowSeconds: Double = 5.0) {
    private var lastBytes = 0L
    private var lastTimestamp = 0L
    private var smoothedSpeed = 0.0
    private var firstDataTimestamp = 0L
    private var lastChangeTimestamp = 0L

    fun update(downloadedBytes: Long, totalBytes: Long?, timestampMillis: Long): EtaSnapshot {
        val bytes = downloadedBytes.coerceAtLeast(0)
        if (lastTimestamp == 0L) {
            lastBytes = bytes
            lastTimestamp = timestampMillis
            firstDataTimestamp = timestampMillis
            lastChangeTimestamp = timestampMillis
        } else if (timestampMillis > lastTimestamp) {
            val elapsedSeconds = (timestampMillis - lastTimestamp) / 1000.0
            val delta = bytes - lastBytes
            if (delta > 0) {
                val instantSpeed = delta / elapsedSeconds
                val alpha = (elapsedSeconds / smoothingWindowSeconds).coerceIn(0.05, 1.0)
                smoothedSpeed = if (smoothedSpeed == 0.0) instantSpeed else smoothedSpeed + alpha * (instantSpeed - smoothedSpeed)
                lastChangeTimestamp = timestampMillis
            }
            lastBytes = bytes
            lastTimestamp = timestampMillis
        }
        val done = totalBytes != null && bytes >= totalBytes
        val ageSeconds = (timestampMillis - firstDataTimestamp).coerceAtLeast(0) / 1000
        val stalled = !done && timestampMillis - lastChangeTimestamp >= 10_000
        val state = when {
            done -> EtaState.DONE
            stalled -> EtaState.STALLED
            ageSeconds < 3 || smoothedSpeed <= 0.0 -> EtaState.CALCULATING
            else -> EtaState.ESTIMATING
        }
        val remaining = totalBytes?.let { (it - bytes).coerceAtLeast(0) }
        val eta = if (state == EtaState.ESTIMATING && remaining != null) max(0.0, remaining / smoothedSpeed).toLong() else null
        return EtaSnapshot(bytes, totalBytes, smoothedSpeed, remaining, eta, state, totalBytes == null)
    }
}

object EtaFormatter {
    fun duration(seconds: Long?): String = when {
        seconds == null -> "Calculating…"
        seconds < 60 -> "$seconds s"
        seconds < 3600 -> "${seconds / 60} min ${seconds % 60} s"
        else -> "${seconds / 3600} h ${(seconds % 3600) / 60} min"
    }

    fun speed(bytesPerSecond: Double): String = when {
        bytesPerSecond >= 1_000_000 -> "%.1f MB/s".format(bytesPerSecond / 1_000_000)
        else -> "%.0f KB/s".format(bytesPerSecond / 1_000)
    }

    fun status(snapshot: EtaSnapshot): String = when (snapshot.state) {
        EtaState.CALCULATING -> "Calculating…"
        EtaState.STALLED -> "Stalled — retry"
        EtaState.DONE -> "Complete"
        EtaState.ESTIMATING -> "${duration(snapshot.etaSeconds)} · ${speed(snapshot.speedBytesPerSecond)}"
    }
}
