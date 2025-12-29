/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.utils

import javazoom.jl.player.Player
import java.io.BufferedInputStream
import java.io.InputStream

fun playMP3(resourcePath: String) {
    kotlin.concurrent.thread(start = true) {
        try {
            val inputStream: InputStream = object {}.javaClass.getResourceAsStream(resourcePath)
                ?: throw IllegalArgumentException("音频资源未找到: $resourcePath")
            val bufferedStream = BufferedInputStream(inputStream)
            val player = Player(bufferedStream)
            player.play()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
