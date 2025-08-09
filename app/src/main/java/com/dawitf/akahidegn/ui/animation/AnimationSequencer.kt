package com.dawitf.akahidegn.ui.animation

import kotlinx.coroutines.delay

/**
 * Lightweight builder + timeline for sequencing animation and UI steps.
 */
class AnimationSequencer private constructor(
    private val entries: List<SequenceEntry>
) {
    sealed class SequenceEntry {
        data class Step(val name: String?, val block: suspend () -> Unit) : SequenceEntry()
        data class Wait(val durationMs: Long) : SequenceEntry()
    }

    suspend fun run() {
        for (e in entries) when (e) {
            is SequenceEntry.Step -> e.block()
            is SequenceEntry.Wait -> if (e.durationMs > 0) delay(e.durationMs)
        }
    }

    class Builder {
        private val entries = mutableListOf<SequenceEntry>()

        fun step(name: String? = null, block: suspend () -> Unit) = apply {
            entries += SequenceEntry.Step(name, block)
        }

        fun delayMs(durationMs: Long) = apply {
            entries += SequenceEntry.Wait(durationMs)
        }

        fun build(): AnimationSequencer = AnimationSequencer(entries.toList())
    }
}

fun buildAnimationSequence(build: AnimationSequencer.Builder.() -> Unit): AnimationSequencer =
    AnimationSequencer.Builder().apply(build).build()
