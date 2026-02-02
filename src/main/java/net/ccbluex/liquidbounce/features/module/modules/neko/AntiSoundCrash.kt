/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.server.S29PacketSoundEffect

object AntiSoundCrash : Module("AntiSoundCrash", Category.NEKO) {

    /**
     * 最大允许的声音名称长度
     * Minecraft 1.8.9 协议限制为 16 个字符
     */
    private val maxSoundNameLength by int("MaxLength", 16, 1..32)

    /**
     * 是否取消过长的声音包
     */
    private val cancelPacket by boolean("CancelPacket", true)

    /**
     * 是否记录被阻止的声音包
     */
    private val logBlocked by boolean("LogBlocked", false)

    /**
     * 处理的包数量统计
     */
    private var blockedCount = 0
    private var processedCount = 0

    /**
     * 处理接收到的数据包
     */
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        
        // 只处理服务器发送的声音效果包
        if (packet !is S29PacketSoundEffect) return@handler
        
        processedCount++
        
        // 检查声音名称长度
        val soundName = packet.soundName
        if (soundName.length > maxSoundNameLength) {
            blockedCount++
            
            // 记录被阻止的声音包
            if (logBlocked) {
                println("[AntiSoundCrash] Blocked sound packet: ${soundName.substring(0, minOf(20, soundName.length))}... (length: ${soundName.length})")
            }
            
            // 取消事件以防止进一步处理
            if (cancelPacket) {
                event.cancelEvent()
            }
        }
    }

    /**
     * 模块启用时重置统计
     */
    override fun onEnable() {
        blockedCount = 0
        processedCount = 0
    }

    /**
     * 显示模块标签（统计信息）
     */
    override val tag: String
        get() = if (processedCount > 0) {
            "$blockedCount/$processedCount"
        } else {
            ""
        }

    /**
     * 获取当前模块状态描述
     */
    fun getStatus(): String {
        return if (state) {
            "Blocked $blockedCount of $processedCount packets"
        } else {
            "Disabled"
        }
    }
}