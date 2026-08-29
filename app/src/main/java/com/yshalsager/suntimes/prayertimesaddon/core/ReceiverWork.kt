package com.yshalsager.suntimes.prayertimesaddon.core

import android.content.BroadcastReceiver
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

internal object ReceiverWork {
    class State {
        var running = false
        var requested = false
    }

    private val executor = Executors.newSingleThreadExecutor()

    fun submit(receiver: BroadcastReceiver, state: State, work: () -> Unit) {
        synchronized(state) {
            state.requested = true
            if (state.running) return
            state.running = true
        }

        val pending_result = receiver.goAsync()
        executor.execute {
            var released = false
            try {
                while (true) {
                    synchronized(state) { state.requested = false }
                    work()
                    val done = synchronized(state) {
                        if (!state.requested) state.running = false
                        !state.requested
                    }
                    if (done) {
                        released = true
                        break
                    }
                }
            } finally {
                if (!released) synchronized(state) { state.running = false }
                pending_result?.finish()
            }
        }
    }

    fun await_idle() = executor.submit {}.get(30, TimeUnit.SECONDS)
}
