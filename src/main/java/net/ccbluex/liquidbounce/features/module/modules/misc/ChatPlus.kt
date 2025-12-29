//By Neko(全ai写的)
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.client.C01PacketChatMessage

object ChatPlus : Module("ChatPlus", Category.MISC) {

    private val suffixValue by text("Suffix", "喵")
    private val ignoreCommands by boolean("IgnoreCommands", true)
    private val ignoreSlash by boolean("IgnoreSlash", true)
    private val spaceBeforeSuffix by boolean("SpaceBeforeSuffix", true)
    private val excludePackets by boolean("ExcludePackets", false)

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C01PacketChatMessage) {
            var message = packet.message

            // 检查是否需要忽略
            if (shouldIgnore(message)) {
                return@handler
            }

            // 添加后缀
            val modifiedMessage = modifyMessage(message)
            
            // 更新数据包
            packet.message = modifiedMessage
        }
    }

    private fun shouldIgnore(message: String): Boolean {
        // 忽略空消息
        if (message.trim().isEmpty()) {
            return true
        }

        // 忽略命令
        if (ignoreCommands && message.startsWith("/")) {
            return true
        }

        // 忽略斜杠开头的消息
        if (ignoreSlash && message.startsWith("/")) {
            return true
        }

        // 排除数据包（某些特殊数据包）
        if (excludePackets && message.contains("\u0000")) {
            return true
        }

        return false
    }

    private fun modifyMessage(original: String): String {
        val trimmed = original.trim()
        
        return if (spaceBeforeSuffix) {
            "$trimmed $suffixValue"
        } else {
            "$trimmed$suffixValue"
        }
    }

    override val tag: String
        get() = suffixValue
}