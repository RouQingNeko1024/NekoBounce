/**
 * @author FireFly_Legit
 */
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.network.play.server.*

object PingSpoofUtils {

    // 存储数据包和对应的延迟时间（毫秒）
    private data class DelayedPacket(val packet: Packet<*>, val timestamp: Long, val delay: Int)

    private val packetQueue = mutableListOf<DelayedPacket>()

    /**
     * 延迟发送数据包实现ping欺骗
     *
     * @param packet 接收到的数据包
     * @param pingOnly 是否只欺骗ping相关数据包
     * @param onlyKillAura 是否只在KillAura启用时生效
     * @param spoofDelay 欺骗延迟(毫秒)
     * @param delayMode Velocity是否为延迟模式
     * @return 是否成功拦截并延迟发送数据包
     */
    fun spoof(
        packet: Packet<*>,
        pingOnly: Boolean = true,
        onlyKillAura: Boolean = false,
        spoofDelay: Int = 500,
        delayMode: Boolean = false,
        killAuraState: Boolean = KillAura.state
    ): Boolean {
        // 如果设置了只在KillAura启用时生效，且KillAura未启用，则直接返回
        if (onlyKillAura && !killAuraState) {
            return false
        }

        // 判断数据包是否需要延迟发送
        val shouldDelay = if (pingOnly) {
            packet is S32PacketConfirmTransaction || packet is S00PacketKeepAlive
        } else {
            packet is S32PacketConfirmTransaction || packet is S00PacketKeepAlive
                    || packet is S19PacketEntityStatus || (packet is S12PacketEntityVelocity && !delayMode)
                    || packet is S08PacketPlayerPosLook || packet is C0CPacketInput
        }

        if (shouldDelay) {
            synchronized(packetQueue) {
                packetQueue.add(DelayedPacket(packet, System.currentTimeMillis(), spoofDelay))
            }
            return true
        }

        return false
    }

    /**
     * 处理延迟队列中的数据包
     * 这个方法应该在游戏循环中定期调用
     *
     * @param all 是否发送所有延迟的数据包（忽略延迟时间）
     */
    fun processQueue(all: Boolean = false) {
        synchronized(packetQueue) {
            val iterator = packetQueue.iterator()
            while (iterator.hasNext()) {
                val delayedPacket = iterator.next()
                // 如果设置了发送所有数据包，或者数据包已经达到了延迟时间
                if (all || System.currentTimeMillis() >= (delayedPacket.timestamp + delayedPacket.delay)) {
                    // 发送数据包
                    PacketUtils.schedulePacketProcess(delayedPacket.packet)
                    iterator.remove()
                }
            }
        }
    }

    /**
     * 立即停止欺骗并发送所有延迟的数据包
     */
    fun stopSpoof() {
        processQueue(all = true)
        clearQueue()
    }

    /**
     * 清理所有延迟的数据包
     */
    fun reset() {
        stopSpoof()
    }

    /**
     * 获取当前队列中的包数量
     */
    val queueSize: Int
        get() = synchronized(packetQueue) { packetQueue.size }

    /**
     * 清理队列（不发送数据包）
     */
    fun clearQueue() {
        synchronized(packetQueue) {
            packetQueue.clear()
        }
    }

    /**
     * 检查是否有数据包在队列中
     */
    val hasPackets: Boolean
        get() = queueSize > 0

    /**
     * 处理队列并返回是否还有剩余的数据包
     */
    fun tick(): Boolean {
        processQueue()
        return hasPackets
    }
}