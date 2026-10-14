package com.abhiram.music.playback

import com.abhiram.music.constants.Logs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * Countdown timer that arms playback to stop after a fixed duration.
 *
 * The service owns an instance and passes the stop action as [onExpire]; the UI observes
 * [remainingMs] and [progress] to render the timer chip and ring.
 *
 * * [start] (re-)arms the timer for [durationMillis]; values `<= 0` behave like [cancel].
 * * Ticks every [TICK_MILLIS] on a background dispatcher.
 * * [onExpire] is invoked **exactly once** per arming, when the countdown reaches zero.
 * * [cancel] stops ticking and clears both flows (`null` = timer off).
 */
class SleepTimer(private val onExpire: suspend () -> Unit) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _remainingMs = MutableStateFlow<Long?>(null)

    /** Remaining time in ms, or `null` when the timer is off (cancelled or finished). */
    val remainingMs: StateFlow<Long?> = _remainingMs.asStateFlow()

    private val _progress = MutableStateFlow<Float?>(null)

    /** Remaining fraction in `0f..1f`, or `null` when the timer is off. */
    val progress: StateFlow<Float?> = _progress.asStateFlow()

    private var job: Job? = null

    /**
     * Arms the timer for [durationMillis] milliseconds, replacing any previous arming.
     * Non-positive durations cancel the timer instead.
     */
    fun start(durationMillis: Long) {
        if (durationMillis <= 0L) {
            cancel()
            return
        }
        job?.cancel()
        _remainingMs.value = durationMillis
        _progress.value = 1f
        val startedAt = TimeSource.Monotonic.markNow()
        job =
            scope.launch {
                while (true) {
                    delay(TICK_MILLIS)
                    val elapsed = startedAt.elapsedNow().inWholeMilliseconds
                    val remaining = durationMillis - elapsed
                    if (remaining <= 0L) {
                        this@SleepTimer.job = null
                        _remainingMs.value = null
                        _progress.value = null
                        Timber.tag(Logs.SLEEP_TIMER).d("Sleep timer expired")
                        // Invoked exactly once per arming.
                        onExpire()
                        break
                    }
                    _remainingMs.value = remaining
                    _progress.value = (remaining.toFloat() / durationMillis).coerceIn(0f, 1f)
                }
            }
    }

    /** Cancels the timer (if any) without firing [onExpire]. */
    fun cancel() {
        job?.cancel()
        job = null
        _remainingMs.value = null
        _progress.value = null
    }

    private companion object {
        /** Ticker period mandated by the playback contract (§4). */
        val TICK_MILLIS: Long = 500.milliseconds.inWholeMilliseconds
    }
}
