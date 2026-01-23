package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import kotlin.concurrent.thread

object Disco : Module("Disco", Category.NEKO) {

    val mode by choices(
        "Mode",
        arrayOf("disco1", "disco2", "disco3"),
        "disco1"
    )

    val loop by boolean("Loop", true)
    val jumpDelay by int("JumpDelay", 500, 100..2000)

    private var isPlaying = false
    private var playThread: Thread? = null
    private var currentPlayer: javazoom.jl.player.Player? = null
    private val jumpTimer = MSTimer()

    override fun onEnable() {
        jumpTimer.reset()
        playSound()
    }

    override fun onDisable() {
        stopSound()
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (jumpTimer.hasTimePassed(jumpDelay)) {
            player.jump()
            jumpTimer.reset()
        }

        if (loop && !isPlaying) {
            playSound()
        }
    }

    private fun playSound() {
        val soundPath = when (mode) {
            "disco1" -> "/assets/minecraft/liquidbounce/sounds/disco1.mp3"
            "disco2" -> "/assets/minecraft/liquidbounce/sounds/disco2.mp3"
            "disco3" -> "/assets/minecraft/liquidbounce/sounds/disco3.mp3"
            else -> return
        }
        
        playThread = thread(start = true) {
            try {
                isPlaying = true
                
                do {
                    val inputStream = Disco::class.java.getResourceAsStream(soundPath)
                        ?: throw IllegalArgumentException("音频资源未找到: $soundPath")
                    val bufferedStream = java.io.BufferedInputStream(inputStream)
                    currentPlayer = javazoom.jl.player.Player(bufferedStream)
                    currentPlayer?.play()
                    
                    if (!loop) break
                    
                    if (!state) break
                    
                } while (loop && state)
                
            } catch (e: Exception) {
                if (state) {
                    mc.thePlayer?.addChatMessage(
                        net.minecraft.util.ChatComponentText("§c播放音乐失败: ${e.message}")
                    )
                }
            } finally {
                isPlaying = false
                currentPlayer = null
            }
        }
    }

    private fun stopSound() {
        currentPlayer?.close()
        currentPlayer = null
        
        playThread?.interrupt()
        playThread = null
        
        isPlaying = false
    }

    override val tag
        get() = mode
}