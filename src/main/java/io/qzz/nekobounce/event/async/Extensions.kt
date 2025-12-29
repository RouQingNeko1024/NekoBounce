/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.event.async

import kotlinx.coroutines.*
import io.qzz.nekobounce.event.*


/**
 * Start a tick sequence job for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.launchSequence(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    always: Boolean = false,
    body: suspend CoroutineScope.() -> Unit
) {
    val job = EventManager.launch(dispatcher, block = body)

    TickScheduler.schedule {
        if (!always && !this@launchSequence.handleEvents()) {
            job.cancel()
            true
        } else {
            job.isCompleted
        }
    }
}

/**
 * Start a **looped** tick sequence job for given [Listenable]
 * which will be cancelled if [Listenable.handleEvents] of the owner returns false
 */
fun Listenable.loopSequence(
    dispatcher: CoroutineDispatcher = Dispatchers.Unconfined,
    always: Boolean = false,
    priority: Byte = 0,
    body: suspend CoroutineScope.() -> Unit
) {
    var job = EventManager.launch(dispatcher, block = body)

    handler<GameTickEvent>(always = true, priority) {
        if (!always && !this@loopSequence.handleEvents()) {
            job.cancel()
        } else if (!job.isActive) {
            job = EventManager.launch(dispatcher, block = body)
        }
    }
}
