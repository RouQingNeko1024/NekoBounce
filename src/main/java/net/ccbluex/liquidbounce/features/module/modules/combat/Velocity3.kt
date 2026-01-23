//Code By Epilogue-Client
//https://github.com/qm123pz/Epilogue-Client/tree/main
//Skid By NekoAI
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C16PacketClientStatus
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import java.util.Random
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentLinkedDeque

object Velocity3 : Module("Velocity3", Category.COMBAT) {

    override val mc = Minecraft.getMinecraft()
    private val random = Random()
    private val df = DecimalFormat("0.00")
    
    private var chanceCounter = 0
    private var pendingExplosion = false
    private var allowNext = true
    private var jumpFlag = false
    private var delayTicksLeft = 0
    private var airDelayTicksLeft = 0
    private var delayedVelocityActive = false
    
    private var reduceActive = false
    private var reduceVelocityTicks = 0
    private var reduceOffGroundTicks = 0
    private var reduceTicksSinceTeleport = 0
    
    private var jump = false
    private var active = false
    private var receiving = false
    private val delayedPackets = ConcurrentLinkedDeque<net.minecraft.network.Packet<*>>()
    private var ticksSinceTeleport = 0
    private var ticksSinceVelocity = 0
    private var offGroundTicks = 0
    
    private var slot = false
    private var attack = false
    private var swing = false
    private var block = false
    private var inventory = false
    private var dig = false
    
    private var hasReceivedVelocity = false
    private var noattack = true
    
    private var watchdogReduceHitsCount = 0
    private var watchdogReduceTicksCount = 0
    private var watchdogReduceLastHurtTime = 0

    private val mode by choices("Mode", arrayOf("Vanilla", "JumpReset", "Reduce", "Watchdog", "WatchdogReduce"), "Vanilla")

    private val horizontal by int("Horizontal", 100, 0..100) { mode == "Vanilla" }
    private val vertical by int("Vertical", 100, 0..100) { mode == "Vanilla" }
    private val explosionHorizontal by int("Explosion Horizontal", 100, 0..100) { mode == "Vanilla" }
    private val explosionVertical by int("Explosion Vertical", 100, 0..100) { mode == "Vanilla" }
    private val chance by int("Chance", 100, 0..100)
    private val fakeCheck by boolean("Check Fake", true)

    private val airDelay by boolean("Air Delay", false) { mode == "JumpReset" }
    private val airDelayTicks by int("Air Delay Ticks", 3, 1..20) { mode == "JumpReset" && airDelay }

    private val mixDelay by boolean("Delay", true) { mode == "Reduce" }
    private val mixDelayTicks by int("Delay Ticks", 1, 1..20) { mode == "Reduce" && mixDelay }
    private val mixDelayOnlyInGround by boolean("Delay Only In Ground", true) { mode == "Reduce" && mixDelay }
    private val mixReduce by boolean("Reduce", false) { mode == "Reduce" }
    private val mixJumpReset by boolean("Jump Reset", true) { mode == "Reduce" }
    private val reduceOnlySwinging by boolean("Reduce Only Swinging", false) { mode == "Reduce" && mixReduce }
    private val reduceOnlyMoving by boolean("Reduce Only Moving", false) { mode == "Reduce" && mixReduce }

    private val watchdogChance by int("Chance", 100, 0..100) { mode == "Watchdog" }
    private val watchdogLegitTiming by boolean("Legit Timing", false) { mode == "Watchdog" }
    private val onSwing by boolean("On Swing", true) { mode == "Watchdog" }
    private val watchdogJumpReset by boolean("Jumpreset", true) { mode == "Watchdog" }

    private val watchdogReduceChance by int("Chance", 100, 0..100) { mode == "WatchdogReduce" }
    private val watchdogReduceLegitTiming by boolean("Legit Timing", false) { mode == "WatchdogReduce" }
    private val watchdogReduceHitsUntilJump by int("Hits Until Jump", 2, 1..10) { mode == "WatchdogReduce" }
    private val watchdogReduceTicksUntilJump by int("Ticks Until Jump", 2, 1..100) { mode == "WatchdogReduce" }
    private val watchdogReduceDelay by boolean("Delay", false) { mode == "WatchdogReduce" }
    private val watchdogReduceDelayTicks by int("Delay Ticks", 1, 1..20) { mode == "WatchdogReduce" && watchdogReduceDelay }
    private val watchdogReduceJumpReset by boolean("JumpReset", true) { mode == "WatchdogReduce" }

    private fun isInBadPosition(): Boolean {
        val thePlayer = mc.thePlayer ?: return false
        return thePlayer.isInWeb || thePlayer.isOnLadder || thePlayer.isInWater || thePlayer.isInLava
    }

    private fun getNearestPlayerTarget(): EntityPlayer? {
        val world = mc.theWorld ?: return null
        val thePlayer = mc.thePlayer ?: return null
        
        var best: EntityPlayer? = null
        var bestDist = Double.MAX_VALUE
        
        for (entity in world.playerEntities) {
            if (entity !is EntityPlayer || entity == thePlayer || entity.isDead) continue
            
            val dist = thePlayer.getDistanceSqToEntity(entity)
            if (dist < bestDist) {
                bestDist = dist
                best = entity
            }
        }
        
        return best
    }

    private fun resetBadPackets() {
        slot = false
        swing = false
        attack = false
        block = false
        inventory = false
        dig = false
    }

    private fun badPackets(checkSlot: Boolean, checkAttack: Boolean, checkSwing: Boolean,
                          checkBlock: Boolean, checkInventory: Boolean, checkDig: Boolean): Boolean {
        return (slot && checkSlot) ||
               (attack && checkAttack) ||
               (swing && checkSwing) ||
               (block && checkBlock) ||
               (inventory && checkInventory) ||
               (dig && checkDig)
    }

    private fun releaseDelayedPackets() {
        if (delayedPackets.isEmpty() || receiving) return

        receiving = true
        active = false

        val netHandler = mc.netHandler ?: return
        
        while (!delayedPackets.isEmpty()) {
            val packet = delayedPackets.poll()
            try {
                when (packet) {
                    is S12PacketEntityVelocity -> packet.processPacket(netHandler)
                    is S08PacketPlayerPosLook -> packet.processPacket(netHandler)
                    is S19PacketEntityStatus -> packet.processPacket(netHandler)
                    is S32PacketConfirmTransaction -> packet.processPacket(netHandler)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        receiving = false
    }

    val onPacketReceive = handler<PacketEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (event.packet is S12PacketEntityVelocity) {
            val vel = event.packet as S12PacketEntityVelocity
            if (vel.entityID == thePlayer.entityId) {
                hasReceivedVelocity = true
                noattack = false
                
                if (mode == "Vanilla") {
                    chanceCounter = chanceCounter % 100 + chance
                    if (chanceCounter >= 100) {
                        // 获取原始速度
                        val motionX = vel.motionX / 8000.0
                        val motionY = vel.motionY / 8000.0
                        val motionZ = vel.motionZ / 8000.0
                        
                        // 应用减少
                        var newX = motionX
                        var newY = motionY
                        var newZ = motionZ
                        
                        if (horizontal > 0) {
                            newX = motionX * horizontal / 100.0
                            newZ = motionZ * horizontal / 100.0
                        }
                        
                        if (vertical > 0) {
                            newY = motionY * vertical / 100.0
                        }
                        
                        // 设置新的速度
                        try {
                            // 尝试使用MCP映射的字段名
                            val fieldX = S12PacketEntityVelocity::class.java.getDeclaredField("motionX")
                            val fieldY = S12PacketEntityVelocity::class.java.getDeclaredField("motionY")
                            val fieldZ = S12PacketEntityVelocity::class.java.getDeclaredField("motionZ")
                            
                            fieldX.isAccessible = true
                            fieldY.isAccessible = true
                            fieldZ.isAccessible = true
                            
                            fieldX.setInt(vel, (newX * 8000.0).toInt())
                            fieldY.setInt(vel, (newY * 8000.0).toInt())
                            fieldZ.setInt(vel, (newZ * 8000.0).toInt())
                        } catch (e: NoSuchFieldException) {
                            // 如果MCP字段名失败，尝试使用SRG字段名
                            try {
                                val fieldX = S12PacketEntityVelocity::class.java.getDeclaredField("field_149415_b")
                                val fieldY = S12PacketEntityVelocity::class.java.getDeclaredField("field_149416_c")
                                val fieldZ = S12PacketEntityVelocity::class.java.getDeclaredField("field_149414_d")
                                
                                fieldX.isAccessible = true
                                fieldY.isAccessible = true
                                fieldZ.isAccessible = true
                                
                                fieldX.setInt(vel, (newX * 8000.0).toInt())
                                fieldY.setInt(vel, (newY * 8000.0).toInt())
                                fieldZ.setInt(vel, (newZ * 8000.0).toInt())
                            } catch (ex: Exception) {
                                // 如果所有方法都失败，取消包并直接设置玩家速度
                                event.cancelEvent()
                                thePlayer.motionX = newX
                                thePlayer.motionY = newY
                                thePlayer.motionZ = newZ
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        
                        chanceCounter = 0
                    }
                }
                
                if (mode == "WatchdogReduce") {
                    val target = getNearestPlayerTarget()
                    val targetDistance = target?.let { thePlayer.getDistanceSqToEntity(it) }

                    if (receiving || ticksSinceTeleport < 3 || thePlayer.isInWeb || 
                        thePlayer.isSwingInProgress || (target != null && targetDistance != null && targetDistance <= 10.24)) { // 3.2^2 = 10.24
                        // Do nothing
                    } else if (!thePlayer.onGround && watchdogReduceDelay) {
                        delayedPackets.offer(vel)
                        active = true
                        event.cancelEvent()
                        ticksSinceVelocity = 0
                    }
                }
            }
        }

        if (mode == "WatchdogReduce") {
            when (val packet = event.packet) {
                is S32PacketConfirmTransaction -> {
                    if (active && watchdogReduceDelay) {
                        delayedPackets.offer(packet)
                        event.cancelEvent()
                    }
                }
                is S08PacketPlayerPosLook -> {
                    if (active && watchdogReduceDelay) {
                        delayedPackets.offer(packet)
                        event.cancelEvent()
                    }
                }
            }
        }

        if (event.packet is S19PacketEntityStatus) {
            val status = event.packet as S19PacketEntityStatus
            if (status.getEntity(mc.theWorld) == thePlayer && status.opCode.toInt() == 2) {
                ticksSinceVelocity = 0
            }
        }
    }

    val onPacketSend = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is C09PacketHeldItemChange -> slot = true
            is C0APacketAnimation -> swing = true
            is C02PacketUseEntity -> {
                if (packet.action == C02PacketUseEntity.Action.ATTACK) {
                    attack = true
                }
            }
            is C08PacketPlayerBlockPlacement -> block = true
            is C07PacketPlayerDigging -> {
                block = true
                dig = true
            }
            is net.minecraft.network.play.client.C0DPacketCloseWindow -> inventory = true
            is C16PacketClientStatus -> {
                if (packet.status == C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT) {
                    inventory = true
                }
            }
            is C03PacketPlayer -> resetBadPackets()
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        ticksSinceTeleport++
        ticksSinceVelocity++

        if (!thePlayer.onGround) {
            offGroundTicks++
        } else {
            offGroundTicks = 0
        }

        if (mode == "WatchdogReduce" && active) {
            val wdrTarget = getNearestPlayerTarget()
            val wdrDistance = wdrTarget?.let { thePlayer.getDistanceSqToEntity(it) }

            if (thePlayer.onGround || ticksSinceTeleport < 3 || thePlayer.isSwingInProgress || 
                (wdrTarget != null && wdrDistance != null && wdrDistance <= 10.24) || offGroundTicks > 20) {
                releaseDelayedPackets()
            }
        }

        if (mode == "Watchdog") {
            val target = getNearestPlayerTarget()
            val targetDistance = target?.let { thePlayer.getDistanceSqToEntity(it) }

            if (hasReceivedVelocity && !noattack) {
                val canAttack = target != null && target != thePlayer && thePlayer.isSprinting

                if (canAttack) {
                    val motionXStr = df.format(thePlayer.motionX)
                    val motionZStr = df.format(thePlayer.motionZ)
                    thePlayer.addChatMessage(net.minecraft.util.ChatComponentText("§b[Velocity] §bAttack! Motion X: $motionXStr | Motion Z: $motionZStr"))

                    thePlayer.motionX *= 0.6
                    thePlayer.motionZ *= 0.6
                    thePlayer.isSprinting = false

                    hasReceivedVelocity = false
                }
            } else {
                noattack = true
            }

            if (active) {
                if (thePlayer.onGround || ticksSinceTeleport < 3 || thePlayer.isSwingInProgress || 
                    (target != null && targetDistance != null && targetDistance <= 10.24) || offGroundTicks > 20) {
                    releaseDelayedPackets()
                }

                if (watchdogLegitTiming) {
                    try {
                        Thread.sleep((1 + random.nextInt(3)).toLong())
                    } catch (e: InterruptedException) {
                        e.printStackTrace()
                    }
                }
            }
        }

        if (mode == "Reduce") {
            if (!thePlayer.onGround) {
                reduceOffGroundTicks++
            } else {
                reduceOffGroundTicks = 0
            }

            if (reduceActive) {
                reduceVelocityTicks++
            }

            val target = getNearestPlayerTarget()
            val targetDistance = target?.let { thePlayer.getDistanceSqToEntity(it) }

            if (target != null && thePlayer.isSwingInProgress && reduceVelocityTicks < 3 && !thePlayer.onGround) {
                val inBadPos = isInBadPosition()
                var canReduce = !inBadPos && reduceTicksSinceTeleport >= 3

                if (canReduce) {
                    thePlayer.swingItem()
                    mc.playerController.attackEntity(thePlayer, target)
                    thePlayer.motionX *= 0.6
                    thePlayer.motionZ *= 0.6
                    thePlayer.isSprinting = false
                }
            }

            val shouldReset = thePlayer.onGround || thePlayer.isSwingInProgress || 
                             (target != null && targetDistance != null && targetDistance <= 10.24) ||
                             reduceOffGroundTicks > 20 || reduceTicksSinceTeleport < 3

            if (shouldReset && reduceActive) {
                reduceActive = false
            }
            
            reduceTicksSinceTeleport++
        }

        if (mode == "JumpReset" && jumpFlag && thePlayer.onGround) {
            thePlayer.motionY = 0.42
            jumpFlag = false
            thePlayer.addChatMessage(net.minecraft.util.ChatComponentText("§7[Velocity] JumpReset"))
        }

        if (delayedVelocityActive) {
            if (airDelayTicksLeft > 0) {
                airDelayTicksLeft--
                if (airDelayTicksLeft <= 0) {
                    delayedVelocityActive = false
                }
            } else if (delayTicksLeft > 0) {
                delayTicksLeft--
                if (delayTicksLeft <= 0) {
                    delayedVelocityActive = false
                }
            } else {
                delayedVelocityActive = false
            }
        }
    }

    val onStrafe = handler<StrafeEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (mode == "WatchdogReduce" && watchdogReduceJumpReset) {
            watchdogReduceTicksCount++

            if (thePlayer.hurtTime == 9 && watchdogReduceLastHurtTime != 9) {
                if (thePlayer.isSprinting && thePlayer.onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                    watchdogReduceHitsCount++

                    val hitsCondition = watchdogReduceHitsCount >= watchdogReduceHitsUntilJump
                    val ticksCondition = watchdogReduceTicksCount >= watchdogReduceTicksUntilJump

                    if (hitsCondition || ticksCondition) {
                        if (random.nextInt(100) < watchdogReduceChance) {
                            jumpFlag = true
                            thePlayer.addChatMessage(net.minecraft.util.ChatComponentText("§7[WatchdogReduce] Jumpreset (Hits: $watchdogReduceHitsCount, Ticks: $watchdogReduceTicksCount)"))
                            watchdogReduceHitsCount = 0
                            watchdogReduceTicksCount = 0
                        }
                    }
                }
            }

            watchdogReduceLastHurtTime = thePlayer.hurtTime
        }
    }

    val onMove = handler<MoveEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (mode == "Watchdog" && jump && watchdogJumpReset) {
            if (thePlayer.onGround) {
                event.y = 0.42
            }
            jump = false
        } else if (jumpFlag && thePlayer.onGround) {
            event.y = 0.42
            jumpFlag = false
        }
    }

    override fun onEnable() {
        pendingExplosion = false
        allowNext = true
        chanceCounter = 0
        jumpFlag = false
        delayTicksLeft = 0
        airDelayTicksLeft = 0
        delayedVelocityActive = false

        reduceActive = false
        reduceVelocityTicks = 0
        reduceOffGroundTicks = 0
        reduceTicksSinceTeleport = 0

        jump = false
        active = false
        receiving = false
        delayedPackets.clear()
        ticksSinceTeleport = 0
        ticksSinceVelocity = 0
        offGroundTicks = 0

        hasReceivedVelocity = false
        noattack = true

        watchdogReduceHitsCount = 0
        watchdogReduceTicksCount = 0
        watchdogReduceLastHurtTime = 0

        resetBadPackets()
    }

    override fun onDisable() {
        pendingExplosion = false
        allowNext = true
        chanceCounter = 0
        jumpFlag = false
        delayTicksLeft = 0
        airDelayTicksLeft = 0
        delayedVelocityActive = false

        reduceActive = false
        reduceTicksSinceTeleport = 0

        if (!delayedPackets.isEmpty()) {
            releaseDelayedPackets()
        }
        active = false
        receiving = false
        jump = false

        hasReceivedVelocity = false
        noattack = true

        watchdogReduceHitsCount = 0
        watchdogReduceTicksCount = 0
        watchdogReduceLastHurtTime = 0

        resetBadPackets()
    }

    override val tag: String?
        get() = mode
}