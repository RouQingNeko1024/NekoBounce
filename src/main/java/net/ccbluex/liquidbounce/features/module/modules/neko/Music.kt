//By Neko
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ChatComponentText
import kotlin.concurrent.thread

object Music : Module("Music", Category.NEKO) {

    val mode by choices(
        "Mode",
        arrayOf("crk", "zoomfly"),
        "crk"
    )

    val loop by boolean("Loop", false)

    private var isPlaying = false
    private var playThread: Thread? = null
    private var messageThread: Thread? = null
    private var currentPlayer: javazoom.jl.player.Player? = null
    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    override fun onEnable() {
        val player = mc.thePlayer ?: return
        lastPosX = player.posX
        lastPosY = player.posY
        lastPosZ = player.posZ
        

        playSound()
        

        if (mode == "crk") {
            showCrkMessages()
        }
    }

    override fun onDisable() {
        stopSound()
        stopMessages()
    }

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (loop && !isPlaying) {
            playSound()
        }

        if (mode == "zoomfly") {

            val deltaX = player.posX - lastPosX
            val deltaY = player.posY - lastPosY
            val deltaZ = player.posZ - lastPosZ
            

            if (deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ > 25.0) {
                player.addChatMessage(ChatComponentText("§c没zoomfly不配使用浪子闲话"))
            }
            
            lastPosX = player.posX
            lastPosY = player.posY
            lastPosZ = player.posZ
        }
    }

    private fun playSound() {
        val soundPath = when (mode) {
            "crk" -> "/assets/minecraft/liquidbounce/sounds/crk.mp3"
            "zoomfly" -> "/assets/minecraft/liquidbounce/sounds/zoomfly.mp3"
            else -> return
        }
        
        playThread = thread(start = true) {
            try {
                isPlaying = true
                
                do {

                    val inputStream = Music::class.java.getResourceAsStream(soundPath)
                        ?: throw IllegalArgumentException("音频资源未找到: $soundPath")
                    val bufferedStream = java.io.BufferedInputStream(inputStream)
                    currentPlayer = javazoom.jl.player.Player(bufferedStream)
                    currentPlayer?.play()
                    

                    if (!loop) break
                    

                    if (!state) break
                    
                } while (loop && state)
                
            } catch (e: Exception) {

                if (state) {
                    mc.thePlayer?.addChatMessage(ChatComponentText("§c播放音乐失败: ${e.message}"))
                }
            } finally {
                isPlaying = false
                currentPlayer = null
            }
        }
    }
    
    private fun showCrkMessages() {
        messageThread = thread(start = true) {
            try {
                val messages = listOf("NekoBounce Beta","Hello", "Welcome to using the NekoBounce client", "This client is free.", "https://github.com/beimen-pan/NekoBounce")
                
                for (message in messages) {

                    if (!state) break
                    

                    mc.thePlayer?.addChatMessage(ChatComponentText("§2$message"))
                    

                    Thread.sleep(500)
                }
            } catch (e: InterruptedException) {

            } catch (e: Exception) {

                if (state) {
                    mc.thePlayer?.addChatMessage(ChatComponentText("§c显示消息时出错: ${e.message}"))
                }
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
    
    private fun stopMessages() {

        messageThread?.interrupt()
        messageThread = null
    }

    override val tag
        get() = mode
}