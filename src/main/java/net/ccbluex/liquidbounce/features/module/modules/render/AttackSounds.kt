//By NekoBounce
package net.ccbluex.liquidbounce.features.module.modules.render

import javazoom.jl.player.Player
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.io.BufferedInputStream
import kotlin.concurrent.thread

object AttackSounds : Module("AttackSounds", Category.RENDER) {
    private val sound by choices("Sound", arrayOf("Ciallo", "gugugaga"), "Ciallo")
    private val waitForFinish by boolean("WaitForFinish", true)

    private var currentPlayer: Player? = null
    private var isPlaying = false

    val onAttack = handler<AttackEvent> { event ->
        if (event.targetEntity == null) return@handler
        if (waitForFinish && isPlaying) return@handler

        thread(start = true) {
            try {
                isPlaying = true
                val fileName = when (sound) {
                    "Ciallo" -> "Ciallo.mp3"
                    "gugugaga" -> "gugugaga.mp3"
                    else -> throw IllegalArgumentException("未知的声音模式: $sound")
                }
                val resourcePath = "/assets/minecraft/liquidbounce/sounds/AttackEffects/$fileName"
                
                // 使用当前类的类加载器来获取资源
                val inputStream = this::class.java.getResourceAsStream(resourcePath)
                    ?: throw IllegalArgumentException("音频资源未找到: $resourcePath")
                
                val bufferedStream = BufferedInputStream(inputStream)
                currentPlayer = Player(bufferedStream)
                currentPlayer?.play()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isPlaying = false
                currentPlayer = null
            }
        }
    }

    override fun onDisable() {
        currentPlayer?.close()
        isPlaying = false
        currentPlayer = null
    }
}