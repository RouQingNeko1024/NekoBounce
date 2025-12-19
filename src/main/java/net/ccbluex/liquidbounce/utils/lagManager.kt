package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemSword
import net.minecraft.network.Packet
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.login.client.C00PacketLoginStart
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.play.client.*
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.util.Vec3
import java.util.*

object LagManager : Listenable,MinecraftInstance {
    val packetQueue: Deque<LagPacket> = ArrayDeque()
    private var tickDelay = 0
    private var flushing = false
    private var lastPosition = Vec3(0.0, 0.0, 0.0)

    // LagRange 相关属性
    private var tickIndex = -1
    private var delayCounter = 0L
    private var hasTarget = false
    private var currentPosition: Vec3? = null
    private var lagRangeEnabled = false
    private var lagRangeDelay = 150
    private var lagRangeRange = 10.0f
    private var lagRangeWeaponsOnly = true
    private var lagRangeAllowTools = false
    private var lagRangeBotCheck = true
    private var lagRangeTeams = true

    fun setLagRangeSettings(
        enabled: Boolean,
        delay: Int,
        range: Float,
        weaponsOnly: Boolean,
        allowTools: Boolean,
        botCheck: Boolean,
        teams: Boolean
    ) {
        this.lagRangeEnabled = enabled
        this.lagRangeDelay = delay
        this.lagRangeRange = range
        this.lagRangeWeaponsOnly = weaponsOnly
        this.lagRangeAllowTools = allowTools
        this.lagRangeBotCheck = botCheck
        this.lagRangeTeams = teams
    }

    private fun flushQueue() {
        if (mc.netHandler == null) {
            packetQueue.clear()
        } else {
            flushing = true
            while (packetQueue.isNotEmpty()) {
                val lagPacket = packetQueue.peek()
                if (tickDelay > 0 && lagPacket!!.delay <= tickDelay) {
                    break
                }
                packetQueue.poll()
                PacketUtils.sendPacket(lagPacket!!.packet)
                if (lagPacket.packet is C03PacketPlayer) {
                    val c03 = lagPacket.packet
                    if (c03.isMoving) {
                        lastPosition = Vec3(c03.x, c03.y, c03.z)
                    }
                }
            }
            flushing = false
        }
    }

    private fun incrementDelays() {
        packetQueue.forEach { it.delay++ }
    }

    fun handlePacket(packet: Packet<*>): Boolean {
        flushQueue()
        if (packet is C00PacketKeepAlive || packet is C01PacketChatMessage) {
            return false
        } else if (tickDelay > 0) {
            packetQueue.offer(LagPacket(packet))
            return true
        } else {
            if (packet is C03PacketPlayer) {
                val c03 = packet
                if (c03.isMoving) {
                    lastPosition = Vec3(c03.x, c03.y, c03.z)
                }
            }
            return false
        }
    }

    fun setDelay(delay: Int) {
        tickDelay = delay
    }

    fun getLastPosition(): Vec3 {
        return lastPosition
    }

    fun isFlushing(): Boolean {
        return flushing
    }

    private fun isValidTarget(entityPlayer: EntityPlayer): Boolean {
        if (entityPlayer == mc.thePlayer || entityPlayer == mc.thePlayer.ridingEntity) {
            return false
        }

        if (entityPlayer == mc.renderViewEntity || entityPlayer == mc.renderViewEntity?.ridingEntity) {
            return false
        }

        if (entityPlayer.deathTime > 0) {
            return false
        }
        return true
    }

    private fun shouldResetOnPacket(packet: Packet<*>): Boolean {
        return when (packet) {
            is C02PacketUseEntity -> true
            is C07PacketPlayerDigging -> packet.status != C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
            is C08PacketPlayerBlockPlacement -> {
                val item = packet.stack
                item == null || item.item !is ItemSword
            }
            else -> false
        }
    }

    val onTick = handler<PlayerTickEvent> { event ->
        if (event.state == EventState.POST) {
            if (mc.thePlayer.isDead) {
                setDelay(0)
            }
            incrementDelays()
            flushQueue()
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        if (event.packet is C00Handshake
            || event.packet is C00PacketLoginStart
            || event.packet is C00PacketServerQuery
            || event.packet is C01PacketPing
            || event.packet is C01PacketEncryptionResponse) {
            setDelay(0)
        }

        // LagRange packet handling
        if (lagRangeEnabled && shouldResetOnPacket(event.packet)) {
            setDelay(0)
            tickIndex = -1
        }
    }

    fun resetLagRange() {
        setDelay(0)
        tickIndex = -1
        delayCounter = 0L
        hasTarget = false
        currentPosition = null
    }

    class LagPacket(val packet: Packet<*>) {
        var delay = 0
    }
}
