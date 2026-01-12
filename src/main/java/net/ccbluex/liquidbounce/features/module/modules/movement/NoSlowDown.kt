/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import io.netty.buffer.Unpooled
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Gapple
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.hasMotion
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.item.*
import net.minecraft.network.Packet
import net.minecraft.network.PacketBuffer
import net.minecraft.network.handshake.client.C00Handshake
import net.minecraft.network.play.client.*
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.DROP_ITEM
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S2FPacketSetSlot
import net.minecraft.network.status.client.C00PacketServerQuery
import net.minecraft.network.status.client.C01PacketPing
import net.minecraft.network.status.server.S01PacketPong
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3


object NoSlowDown : Module("NoSlowDown", Category.MOVEMENT, gameDetecting = false) {

    override val tag: String
        get() = "$swordMode $consumeMode $bowPacket"

    private val swordMode by choices(
        "SwordMode",
        arrayOf("None", "NCP", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Blink","PostPlace", "GrimAC", "BlocksMC", "Matrix1", "Matrix2", "Matrix3", "Matrix4", "Matrix5", "Matrix6", "Matrix7"),
        "None"
    )

    private val bmcTicks by int("BMC Ticks", 1, 1..20) { swordMode == "BlocksMC" }
    private val bmcOldOffset by boolean("BMC OldOffset", false) { swordMode == "BlocksMC" }
    private val reblinkTicks by int("ReblinkTicks", 10, 1..20) { swordMode == "Blink" }

    private val blockForwardMultiplier by float("BlockForwardMultiplier", 1f, 0.2F..1f)
    private val blockStrafeMultiplier by float("BlockStrafeMultiplier", 1f, 0.2F..1f)

    private val consumeMode by choices(
        "ConsumeMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Intave", "Drop", "BlocksMC", "Matrix1", "Matrix2", "Matrix3", "Matrix4", "Matrix5", "Matrix6", "Matrix7"),
        "None"
    )

    private val consumeForwardMultiplier by float("ConsumeForwardMultiplier", 1f, 0.2F..1f)
    private val consumeStrafeMultiplier by float("ConsumeStrafeMultiplier", 1f, 0.2F..1f)
    private val consumeFoodOnly by boolean(
        "ConsumeFood",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }
    private val consumeDrinkOnly by boolean(
        "ConsumeDrink",
        true
    ) { consumeForwardMultiplier > 0.2F || consumeStrafeMultiplier > 0.2F }
    
    // Matrix mode settings
    private val matrixDamage by boolean("MatrixDamage", true) { consumeMode.contains("Matrix") || swordMode.contains("Matrix") || bowPacket.contains("Matrix") }
    private val matrixSlowDiag by boolean("MatrixSlowDiag", false) { consumeMode.contains("Matrix") || swordMode.contains("Matrix") || bowPacket.contains("Matrix") }

    private val bowPacket by choices(
        "BowMode",
        arrayOf("None", "UpdatedNCP", "AAC5", "SwitchItem", "InvalidC08", "Matrix1", "Matrix2", "Matrix3", "Matrix4", "Matrix5", "Matrix6", "Matrix7"),
        "None"
    )

    private val bowForwardMultiplier by float("BowForwardMultiplier", 1f, 0.2F..1f)
    private val bowStrafeMultiplier by float("BowStrafeMultiplier", 1f, 0.2F..1f)

    // Blocks
    val soulSand by boolean("SoulSand", true)
    val liquidPush by boolean("LiquidPush", true)

    private var shouldSwap = false
    private var shouldBlink = true
    private var shouldNoSlow = false

    private var hasDropped = false

    private val BlinkTimer = TickTimer()
    // blocksmc add
    private var randomFactor = 0f
    private var sent = false

    // consume BlocksMC tick counter (移植自你给的 BlocksMCNoSlow)
    private var consumeTickCycle = 0
    
    // Matrix mode variables
    private var index1 = 0
    private var index2 = 0
    private var index3 = 0
    private var index4 = 0
    private var index5 = 0
    private var index6 = 0
    private var prevslotid = 0
    private var nextspoof = false
    private var wasitemused = false
    private var lastVeltimer = 0L
    
    // Additional Matrix mode variables for new modes
    private var matrix3Counter = 0
    private var matrix4Counter = 0
    private var matrix5Counter = 0
    private var matrix5GroundTicks = 0
    private var matrix5AirTicks = 0
    private var matrix5LastGroundState = false
    
    // Matrix6 mode variables
    private var matrix6Counter = 0
    private var matrix6AirTicks = 0
    private var matrix6GroundTicks = 0
    private var matrix6LastOnGround = false
    private var matrix6MotionY = 0.0
    private var matrix6Speed = 0.0
    
    // Matrix7 mode variables
    private var nextTemp = false
    private var lastBlockingStat = false
    private var waitC03 = false
    private var matrix7Counter = 0
    private var matrix7LastPacketTime = 0L
    private val packetBuf = mutableListOf<Packet<*>>()

    override fun onDisable() {
        shouldSwap = false
        shouldBlink = true
        BlinkTimer.reset()
        BlinkUtils.unblink()
        
        // Reset Matrix variables
        index2 = 0
        index1 = 0
        index5 = 0
        index6 = 0
        index4 = 0
        nextspoof = false
        wasitemused = false
        
        // Reset additional Matrix variables
        matrix3Counter = 0
        matrix4Counter = 0
        matrix5Counter = 0
        matrix5GroundTicks = 0
        matrix5AirTicks = 0
        matrix5LastGroundState = false
        
        // Reset Matrix6 variables
        matrix6Counter = 0
        matrix6AirTicks = 0
        matrix6GroundTicks = 0
        matrix6LastOnGround = false
        matrix6MotionY = 0.0
        matrix6Speed = 0.0
        
        // Reset Matrix7 variables
        nextTemp = false
        lastBlockingStat = false
        waitC03 = false
        matrix7Counter = 0
        matrix7LastPacketTime = 0L
        packetBuf.clear()
    }

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val heldItem = player.heldItem ?: return@handler
        val isUsingItem = usingItemFunc()

        if (!hasMotion && !shouldSwap)
            return@handler

        // Matrix mode specific logic
        if (consumeMode == "Matrix1" || swordMode == "Matrix1" || bowPacket == "Matrix1" ||
            consumeMode == "Matrix2" || swordMode == "Matrix2" || bowPacket == "Matrix2" ||
            consumeMode == "Matrix3" || swordMode == "Matrix3" || bowPacket == "Matrix3" ||
            consumeMode == "Matrix4" || swordMode == "Matrix4" || bowPacket == "Matrix4" ||
            consumeMode == "Matrix5" || swordMode == "Matrix5" || bowPacket == "Matrix5") {
            // Update Matrix mode variables
            if (event.eventState == EventState.PRE) {
                index3 = index2
                index2 = index1
                index1 = if (player.isUsingItem) 1 else 0
            }
        }
        
        // Additional Matrix mode logic for new modes
        if (consumeMode == "Matrix3" || swordMode == "Matrix3" || bowPacket == "Matrix3") {
            if (event.eventState == EventState.PRE) {
                matrix3Counter++
            }
        }
        
        if (consumeMode == "Matrix4" || swordMode == "Matrix4" || bowPacket == "Matrix4") {
            if (event.eventState == EventState.PRE) {
                matrix4Counter++
            }
        }
        
        if (consumeMode == "Matrix5" || swordMode == "Matrix5" || bowPacket == "Matrix5") {
            if (event.eventState == EventState.PRE) {
                matrix5Counter++
                
                // Track ground state changes for Matrix5
                if (player.onGround != matrix5LastGroundState) {
                    if (player.onGround) {
                        matrix5GroundTicks = 0
                    } else {
                        matrix5AirTicks = 0
                    }
                    matrix5LastGroundState = player.onGround
                }
                
                if (player.onGround) {
                    matrix5GroundTicks++
                } else {
                    matrix5AirTicks++
                }
            }
        }
        
        if (consumeMode == "Matrix6" || swordMode == "Matrix6" || bowPacket == "Matrix6") {
            if (event.eventState == EventState.PRE) {
                matrix6Counter++
                
                // Track ground state changes for Matrix6
                if (player.onGround != matrix6LastOnGround) {
                    if (player.onGround) {
                        matrix6GroundTicks = 0
                    } else {
                        matrix6AirTicks = 0
                    }
                    matrix6LastOnGround = player.onGround
                }
                
                if (player.onGround) {
                    matrix6GroundTicks++
                } else {
                    matrix6AirTicks++
                }
                
                // Calculate motion values for Matrix6
                matrix6MotionY = player.motionY
                matrix6Speed = Math.sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
            }
        }
        
        if (consumeMode == "Matrix7" || swordMode == "Matrix7" || bowPacket == "Matrix7") {
            val isUsingItem = usingItemFunc()
            
            if (event.eventState == EventState.PRE) {
                matrix7Counter++
                
                // Matrix7 mode processing
                if (lastBlockingStat || isUsingItem) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - matrix7LastPacketTime >= 230 && nextTemp) { // 230ms delay, similar to the original code
                        nextTemp = false
                        // Send release item use packet
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos(-1, -1, -1), EnumFacing.DOWN))
                        
                        // Send cached packets from buffer
                        if (packetBuf.isNotEmpty()) {
                            var canAttack = false
                            for (packet in packetBuf) {
                                if (packet is C03PacketPlayer) {
                                    canAttack = true
                                }
                                // Ensure attack-related packets are sent after C03
                                if (!((packet is C02PacketUseEntity || packet is C0APacketAnimation) && !canAttack)) {
                                    sendPacket(packet)
                                }
                            }
                            packetBuf.clear()
                        }
                        matrix7LastPacketTime = currentTime
                    }
                }
                
                if (!nextTemp && mc.thePlayer.heldItem?.item is ItemSword && isUsingItem) {
                    lastBlockingStat = isUsingItem
                    // Send fake blocking packet
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, mc.thePlayer.inventory.getCurrentItem(), 0f, 0f, 0f))
                    nextTemp = true
                    waitC03 = false
                }
            }
        }

        if (isUsingItem || shouldSwap) {
            if (heldItem.item !is ItemSword && heldItem.item !is ItemBow && (consumeFoodOnly && heldItem.item is ItemFood ||
                        consumeDrinkOnly && (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk))
            ) {
                when (consumeMode.lowercase()) {
                    "aac5" ->
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))

                    "switchitem" ->
                        if (event.eventState == EventState.PRE) {
                            updateSlot()
                        }

                    "updatedncp" ->
                        if (event.eventState == EventState.PRE && shouldSwap) {
                            updateSlot()
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                            shouldSwap = false
                        }

                    "invalidc08" -> {
                        if (event.eventState == EventState.PRE) {
                            if (InventoryUtils.hasSpaceInInventory()) {
                                if (player.ticksExisted % 3 == 0)
                                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                            }
                        }
                    }

                    "intave" -> {
                        if (event.eventState == EventState.PRE) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.UP))
                        }
                    }

                    // ---- BlocksMC for consumeMode (port from BlocksMCNoSlow.java) ----
                    "blocksmc" -> {
                        // 只在 PRE 阶段计时并发送包（与原 Java 类行为相匹配）
                        if (event.eventState == EventState.PRE) {
                            consumeTickCycle++
                            if (consumeTickCycle == 1) {
                                mc.netHandler.addToSendQueue(C09PacketHeldItemChange((mc.thePlayer.inventory.currentItem + 1) % 9))
                                mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 0, heldItem, 0f, 0f, 0f))
                            }
                        }
                    }
                                    
                    "matrix3" -> {
                        // Matrix3 consume mode - send release and placement packets
                        if (event.eventState == EventState.PRE) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        } else {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }
                                    
                    "matrix4" -> {
                        // Matrix4 consume mode - alternating packet sequence
                        if (event.eventState == EventState.PRE) {
                            if (matrix4Counter % 2 == 0) {
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                            } else {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            }
                        }
                    }
                                    
                    "matrix5" -> {
                        // Matrix5 consume mode - complex packet timing
                        if (event.eventState == EventState.PRE) {
                            if (matrix5Counter % 3 == 0) {
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                            }
                                            
                            // Apply additional logic based on ground state
                            if (player.onGround && matrix5GroundTicks < 5) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            }
                        }
                    }
                                    
                    "matrix6" -> {
                        // Matrix6 consume mode - dynamic packet timing
                        if (event.eventState == EventState.PRE) {
                            if (matrix6Counter % 4 == 0) {
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                            }
                                            
                            // Apply additional logic based on ground state and movement
                            if (player.onGround && matrix6GroundTicks < 3) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            }
                                            
                            // Adjust based on movement speed
                            if (matrix6Speed > 0.28) {
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                            }
                        }
                    }
                                    
                    "matrix7" -> {
                        // Matrix7 consume mode - advanced packet buffering
                        if (event.eventState == EventState.PRE) {
                            if (matrix7Counter % 5 == 0) {
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                            }
                                            
                            // Apply additional logic based on ground state
                            if (player.onGround && matrix7Counter % 3 == 0) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            }
                        }
                    }
                }
            }
        } else {
            consumeTickCycle = 0
        }

        if (heldItem.item is ItemBow && (isUsingItem || shouldSwap)) {
            when (bowPacket.lowercase()) {
                "aac5" ->
                    sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.PRE && shouldSwap) {
                        updateSlot()
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                        shouldSwap = false
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
                
                "matrix6" -> {
                    // Matrix6 bow mode - dynamic packet timing
                    if (event.eventState == EventState.PRE) {
                        if (matrix6Counter % 4 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                        
                        // Apply additional logic based on ground state and movement
                        if (player.onGround && matrix6GroundTicks < 3) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                        
                        // Adjust based on movement speed
                        if (matrix6Speed > 0.28) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }
                }
                
                "matrix7" -> {
                    // Matrix7 bow mode - advanced packet buffering
                    if (event.eventState == EventState.PRE) {
                        if (matrix7Counter % 5 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                        
                        // Apply additional logic based on ground state
                        if (player.onGround && matrix7Counter % 3 == 0) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
            }
        }

        if (heldItem.item is ItemSword && isUsingItem) {
            when (swordMode.lowercase()) {
                "postplace" ->
                    if (event.eventState == EventState.PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    } else {
                        repeat(5) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }

                "grimac" ->
                    if (event.eventState == EventState.PRE) {
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem % 8 + 1))
                        mc.netHandler.addToSendQueue(C17PacketCustomPayload("许锦良", PacketBuffer(Unpooled.buffer())))
                        mc.netHandler.addToSendQueue(C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem))
                    } else {
                        repeat(5) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }


                "ncp" ->
                    when (event.eventState) {
                        EventState.PRE -> sendPacket(
                            C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN)
                        )

                        EventState.POST -> sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f
                            )
                        )

                        else -> return@handler
                    }

                "updatedncp" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos.ORIGIN, 255, heldItem, 0f, 0f, 0f))
                    }

                "aac5" ->
                    if (event.eventState == EventState.POST) {
                        sendPacket(
                            C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, player.heldItem, 0f, 0f, 0f)
                        )
                    }

                "switchitem" ->
                    if (event.eventState == EventState.PRE) {
                        updateSlot()
                    }

                "invalidc08" -> {
                    if (event.eventState == EventState.PRE) {
                        if (InventoryUtils.hasSpaceInInventory()) {
                            if (player.ticksExisted % 3 == 0)
                                sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 1, null, 0f, 0f, 0f))
                        }
                    }
                }
                "BlocksMC" -> {
                    if (!sent) {
                        sent = true
                        if (player.onGround) {
                            player.jump()
                        }
                    }

                    val slot = player.inventory.currentItem
                    val item = player.inventory.getStackInSlot(slot)

                    if (item != null && (item.unlocalizedName.contains(
                            "apple",
                            true
                        ) || item.unlocalizedName.contains("bow", true) || item.unlocalizedName.contains(
                            "potion",
                            true
                        ))
                    ) {
                        randomFactor = if (bmcOldOffset) {
                            0.5f + (Math.random() * 0.44).toFloat()
                        } else {
                            (Math.random() * 0.96).toFloat()
                        }

                        val playerPosition = player.position
                        val adjustedY = if (playerPosition.y > 0) playerPosition.y - 255 else playerPosition.y + 255
                        val inter = Vec3(playerPosition.x.toDouble(), adjustedY.toDouble(), playerPosition.z.toDouble())

                        sendPacket(
                            C08PacketPlayerBlockPlacement(
                                BlockPos(
                                    inter.xCoord.toInt(),
                                    inter.yCoord.toInt(),
                                    inter.zCoord.toInt()
                                ), 0, item, 0f, randomFactor, 0f
                            )
                        )

                        if (player.ticksExisted % bmcTicks == 0) {
                            // Additional logic if needed
                        }
                    }
                    return@handler
                }
                "grimtick" -> {
                    val currentTick = player.ticksExisted % 32
                    if (event.eventState == EventState.PRE) {
                        if (currentTick % 4 < 2) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
                
                "matrix3" -> {
                    // Matrix3 sword mode - release and placement sequence
                    if (event.eventState == EventState.PRE) {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    } else {
                        sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                    }
                }
                
                "matrix4" -> {
                    // Matrix4 sword mode - alternating packet sequence
                    if (event.eventState == EventState.PRE) {
                        if (matrix4Counter % 2 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        } else {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
                
                "matrix5" -> {
                    // Matrix5 sword mode - complex packet timing
                    if (event.eventState == EventState.PRE) {
                        if (matrix5Counter % 3 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                        
                        // Apply additional logic based on ground state
                        if (player.onGround && matrix5GroundTicks < 5) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
                
                "matrix6" -> {
                    // Matrix6 sword mode - dynamic packet timing
                    if (event.eventState == EventState.PRE) {
                        if (matrix6Counter % 4 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                        
                        // Apply additional logic based on ground state and movement
                        if (player.onGround && matrix6GroundTicks < 3) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                        
                        // Adjust based on movement speed
                        if (matrix6Speed > 0.28) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                    }
                }
                
                "matrix7" -> {
                    // Matrix7 sword mode - advanced packet buffering
                    if (event.eventState == EventState.PRE) {
                        if (matrix7Counter % 5 == 0) {
                            sendPacket(C08PacketPlayerBlockPlacement(BlockPos(-1, -1, -1), 255, heldItem, 0f, 0f, 0f))
                        }
                        
                        // Apply additional logic based on ground state
                        if (player.onGround && matrix7Counter % 3 == 0) {
                            sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                        }
                    }
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.thePlayer ?: return@handler

        if (event.isCancelled || shouldSwap)
            return@handler

        // Credit: @ManInMyVan
        // TODO: Not sure how to fix random grim simulation flag. (Seem to only happen in Loyisa).
        if (consumeMode == "Drop") {
            if (player.heldItem?.item !is ItemFood || !player.isMoving) {
                shouldNoSlow = false
                return@handler
            }

            val isUsingItem = packet is C08PacketPlayerBlockPlacement && packet.placedBlockDirection == 255

            if (!player.isUsingItem) {
                shouldNoSlow = false
                hasDropped = false
            }

            if (isUsingItem && !hasDropped) {
                sendPacket(C07PacketPlayerDigging(DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                shouldNoSlow = false
                hasDropped = true
            } else if (packet is S2FPacketSetSlot && player.isUsingItem) {
                if (packet.func_149175_c() != 0 || packet.func_149173_d() != SilentHotbar.currentSlot + 36) return@handler

                event.cancelEvent()
                shouldNoSlow = true

                player.itemInUse = packet.func_149174_e()
                if (!player.isUsingItem) player.itemInUseCount = 0
                player.inventory.mainInventory[SilentHotbar.currentSlot] = packet.func_149174_e()
            }
        }

        if (swordMode == "Blink") {
            when (packet) {
                is C00Handshake, is C00PacketServerQuery, is C01PacketPing, is C01PacketChatMessage, is S01PacketPong -> return@handler

                is C07PacketPlayerDigging, is C02PacketUseEntity, is C12PacketUpdateSign, is C19PacketResourcePackStatus -> {
                    BlinkTimer.update()
                    if (shouldBlink && BlinkTimer.hasTimePassed(reblinkTicks) && (BlinkUtils.packetsReceived.isNotEmpty() || BlinkUtils.packets.isNotEmpty())) {
                        BlinkUtils.unblink()
                        BlinkTimer.reset()
                        shouldBlink = false
                    } else if (!BlinkTimer.hasTimePassed(reblinkTicks)) {
                        shouldBlink = true
                    }
                    return@handler
                }

                // Flush on kb
                is S12PacketEntityVelocity -> {
                    if (mc.thePlayer.entityId == packet.entityID) {
                        BlinkUtils.unblink()
                        // Update last velocity timer for Matrix mode
                        lastVeltimer = System.currentTimeMillis()
                        return@handler
                    }
                }

                // Flush on explosion
                is S27PacketExplosion -> {
                    if (packet.field_149153_g != 0f || packet.field_149152_f != 0f || packet.field_149159_h != 0f) {
                        BlinkUtils.unblink()
                        return@handler
                    }
                }

                is C03PacketPlayer -> {
                    if (swordMode == "Blink") {
                        if (player.isMoving) {
                            if (player.heldItem?.item is ItemSword && usingItemFunc()) {
                                if (shouldBlink)
                                    BlinkUtils.blink(packet, event)
                            } else {
                                shouldBlink = true
                                BlinkUtils.unblink()
                            }
                        }
                    }
                }
            }
        }
        
        // Update last velocity timer for Matrix mode
        if (packet is S12PacketEntityVelocity && mc.thePlayer.entityId == packet.entityID) {
            lastVeltimer = System.currentTimeMillis()
        }
        
        // Matrix7 mode packet handling
        if (consumeMode == "Matrix7" || swordMode == "Matrix7" || bowPacket == "Matrix7") {
            // Matrix7 mode packet processing
            if (nextTemp) {
                // Cancel actual blocking and digging packets
                if ((packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) && usingItemFunc()) {
                    event.cancelEvent()
                }
                
                // Buffer movement, attack and other packets
                else if (packet is C03PacketPlayer || packet is C0APacketAnimation || packet is C0BPacketEntityAction ||
                    packet is C02PacketUseEntity || packet is C07PacketPlayerDigging || packet is C08PacketPlayerBlockPlacement) {
                    packetBuf.add(packet)
                    event.cancelEvent()
                }
            }
        }

        when (packet) {
            is C08PacketPlayerBlockPlacement -> {
                if (packet.stack?.item != null && player.heldItem?.item != null && packet.stack.item == mc.thePlayer.heldItem?.item) {
                    if ((consumeMode == "UpdatedNCP" && (
                                packet.stack.item is ItemFood ||
                                        packet.stack.item is ItemPotion ||
                                        packet.stack.item is ItemBucketMilk)) ||
                        (bowPacket == "UpdatedNCP" && packet.stack.item is ItemBow)
                    ) {
                        shouldSwap = true
                    }
                }
            }
        }
    }

    val onSlowDown = handler<SlowDownEvent> { event ->
        val heldItem = mc.thePlayer.heldItem?.item

        if (heldItem !is ItemSword) {
            if (!consumeFoodOnly && heldItem is ItemFood ||
                !consumeDrinkOnly && (heldItem is ItemPotion || heldItem is ItemBucketMilk)
            ) {
                return@handler
            }

            if (consumeMode == "Drop" && !shouldNoSlow)
                return@handler
        }
        
        // Matrix mode handling
        when {
            consumeMode == "Matrix1" || swordMode == "Matrix1" || bowPacket == "Matrix1" -> {
                if (usingItemFunc()) {
                    if (!mc.thePlayer.onGround && (event.forward > 0.9f || event.strafe > 0.9f)) {
                        event.forward = 0.2f
                        event.strafe = 0.2f
                        return@handler
                    }
                    
                    if (!matrixDamage || System.currentTimeMillis() - lastVeltimer < 1300L || nextspoof || mc.thePlayer.isSneaking() || mc.thePlayer.isSprinting()) {
                        event.forward = 1.0f
                        event.strafe = 1.0f
                        index1 = 0
                    } else {
                        if (mc.thePlayer.onGround) {
                            val mult = if (mc.thePlayer.motionY > 0) 1.0f else 0.54f
                            event.forward = mult
                            event.strafe = mult
                            index2 = 0
                        } else {
                            event.forward = 0.694f
                            event.strafe = 0.694f
                        }
                        
                        if (index1 % 3 == 0 && index1 % 12 != 0) {
                            event.forward = 1.0f
                            event.strafe = 1.0f
                        }
                        
                        if (mc.thePlayer.moveForward != 0.0f && mc.thePlayer.moveStrafing != 0.0f && matrixSlowDiag) { // Diagonal movement
                            event.forward = event.forward * 0.7f
                            event.strafe = event.strafe * 0.7f
                        }
                        
                        event.forward = event.forward.coerceAtMost(1.0f)
                        event.strafe = event.strafe.coerceAtMost(1.0f)
                    }
                    nextspoof = false
                    index1++
                    index2++
                } else {
                    index6 = 0
                    index5 = 0
                    index1 = 0
                    index2 = 0
                }
                index6 = index4
                index4 = if (mc.thePlayer.isUsingItem) 1 else 0
            }
            
            consumeMode == "Matrix2" || swordMode == "Matrix2" || bowPacket == "Matrix2" -> {
                if (!usingItemFunc()) {
                    return@handler
                }
                
                if (!mc.thePlayer.onGround && (event.forward > 0.9f || event.strafe > 0.9f)) {
                    event.forward = 0.2f
                    event.strafe = 0.2f
                    return@handler
                }
                
                if (mc.thePlayer.onGround) {
                    val mult = if (mc.thePlayer.motionY > 0) 1.0f else 0.54f
                    event.forward = mult
                    event.strafe = mult
                } else {
                    event.forward = 0.694f
                    event.strafe = 0.694f
                }
                
                if (mc.thePlayer.moveForward != 0.0f && mc.thePlayer.moveStrafing != 0.0f && matrixSlowDiag) { // Diagonal movement
                    event.forward = event.forward * 0.7f
                    event.strafe = event.strafe * 0.7f
                }
                
                event.forward = event.forward.coerceAtMost(1.0f)
                event.strafe = event.strafe.coerceAtMost(1.0f)
            }
            
            consumeMode == "Matrix3" || swordMode == "Matrix3" || bowPacket == "Matrix3" -> {
                // Matrix3 mode - Advanced food/bow/sword no-slow with motion adjustments
                if (usingItemFunc()) {
                    if (mc.thePlayer.onGround) {
                        // On ground: reduce slowdown significantly
                        event.forward = 0.95f
                        event.strafe = 0.95f
                    } else {
                        // In air: moderate slowdown
                        event.forward = 0.7f
                        event.strafe = 0.7f
                        
                        // Apply motion modifications based on counter
                        if (matrix3Counter % 4 == 0) {
                            mc.thePlayer.motionX *= 0.98
                            mc.thePlayer.motionZ *= 0.98
                        }
                    }
                }
            }
            
            consumeMode == "Matrix4" || swordMode == "Matrix4" || bowPacket == "Matrix4" -> {
                // Matrix4 mode - Complex timing based slowdown
                if (usingItemFunc()) {
                    if (mc.thePlayer.onGround) {
                        if (mc.thePlayer.isSprinting) {
                            event.forward = 0.8f
                            event.strafe = 0.8f
                        } else {
                            event.forward = 0.9f
                            event.strafe = 0.9f
                        }
                    } else {
                        // In air with complex timing
                        val adjustedForward = if (matrix4Counter % 3 == 0) 0.8f else 0.6f
                        val adjustedStrafe = if (matrix4Counter % 3 == 0) 0.8f else 0.6f
                        event.forward = adjustedForward
                        event.strafe = adjustedStrafe
                    }
                }
            }
            
            consumeMode == "Matrix5" || swordMode == "Matrix5" || bowPacket == "Matrix5" -> {
                // Matrix5 mode - Advanced ground/air state tracking
                if (usingItemFunc()) {
                    if (mc.thePlayer.onGround) {
                        if (matrix5GroundTicks <= 2) {
                            // Just landed - minimal slowdown
                            event.forward = 0.98f
                            event.strafe = 0.98f
                        } else {
                            // On ground for a while
                            event.forward = 0.85f
                            event.strafe = 0.85f
                        }
                    } else {
                        if (matrix5AirTicks <= 3) {
                            // Just jumped - moderate slowdown
                            event.forward = 0.75f
                            event.strafe = 0.75f
                        } else {
                            // Extended air time - higher slowdown
                            event.forward = 0.6f
                            event.strafe = 0.6f
                        }
                    }
                    
                    // Apply diagonal movement adjustment if enabled
                    if (matrixSlowDiag && mc.thePlayer.moveForward != 0.0f && mc.thePlayer.moveStrafing != 0.0f) {
                        event.forward *= 0.95f
                        event.strafe *= 0.95f
                    }
                }
            }
            
            consumeMode == "Matrix6" || swordMode == "Matrix6" || bowPacket == "Matrix6" -> {
                // Matrix6 mode - Variable slowdown with dynamic adjustment
                if (usingItemFunc()) {
                    // Calculate dynamic multiplier based on player state
                    var forwardMultiplier = 1.0f
                    var strafeMultiplier = 1.0f
                    
                    if (mc.thePlayer.onGround) {
                        // On ground: use ground ticks to adjust slowdown
                        if (matrix6GroundTicks <= 2) {
                            // Just landed - minimal slowdown
                            forwardMultiplier = 0.98f
                            strafeMultiplier = 0.98f
                        } else {
                            // On ground for a while
                            forwardMultiplier = 0.85f
                            strafeMultiplier = 0.85f
                        }
                    } else {
                        // In air: use air ticks and motion to adjust slowdown
                        if (matrix6AirTicks <= 3) {
                            // Just jumped - moderate slowdown
                            forwardMultiplier = 0.75f
                            strafeMultiplier = 0.75f
                        } else {
                            // Extended air time - higher slowdown
                            forwardMultiplier = 0.6f
                            strafeMultiplier = 0.6f
                        }
                        
                        // Adjust based on motion
                        if (matrix6MotionY > 0) { // Going up
                            forwardMultiplier *= 1.1f
                            strafeMultiplier *= 1.1f
                        } else if (matrix6MotionY < 0) { // Going down
                            forwardMultiplier *= 0.95f
                            strafeMultiplier *= 0.95f
                        }
                    }
                    
                    // Apply diagonal movement adjustment if enabled
                    if (matrixSlowDiag && mc.thePlayer.moveForward != 0.0f && mc.thePlayer.moveStrafing != 0.0f) {
                        forwardMultiplier *= 0.9f
                        strafeMultiplier *= 0.9f
                    }
                    
                    // Apply speed-based adjustment
                    if (matrix6Speed > 0.2) {
                        forwardMultiplier += (0.1f * (matrix6Speed / 0.5f).toFloat())
                        strafeMultiplier += (0.1f * (matrix6Speed / 0.5f).toFloat())
                    }
                    
                    // Clamp values to reasonable range
                    event.forward = forwardMultiplier.coerceIn(0.2f, 1.0f)
                    event.strafe = strafeMultiplier.coerceIn(0.2f, 1.0f)
                }
            }
            
            consumeMode == "Matrix7" || swordMode == "Matrix7" || bowPacket == "Matrix7" -> {
                // Matrix7 mode - Advanced packet buffering approach
                if (usingItemFunc()) {
                    // Check if we're in the temporary state where packets are buffered
                    if (nextTemp && mc.thePlayer.onGround) {
                        // When buffering, reduce slowdown significantly
                        event.forward = 0.98f
                        event.strafe = 0.98f
                    } else {
                        // Standard slowdown when not buffering
                        if (mc.thePlayer.onGround) {
                            event.forward = 0.9f
                            event.strafe = 0.9f
                        } else {
                            event.forward = 0.7f
                            event.strafe = 0.7f
                        }
                    }
                }
            }
            
            else -> {
                event.forward = getMultiplier(heldItem, true)
                event.strafe = getMultiplier(heldItem, false)
            }
        }
    }

    private fun getMultiplier(item: Item?, isForward: Boolean) = when (item) {
        is ItemFood, is ItemPotion, is ItemBucketMilk -> if (isForward) consumeForwardMultiplier else consumeStrafeMultiplier

        is ItemSword -> if (isForward) blockForwardMultiplier else blockStrafeMultiplier

        is ItemBow -> if (isForward) bowForwardMultiplier else bowStrafeMultiplier

        else -> 0.2F
    }

    fun isUNCPBlocking() =
        swordMode == "UpdatedNCP" && mc.gameSettings.keyBindUseItem.isKeyDown && (mc.thePlayer.heldItem?.item is ItemSword)

    fun usingItemFunc() =
        mc.thePlayer?.heldItem != null && (mc.thePlayer.isUsingItem || (mc.thePlayer.heldItem?.item is ItemSword && KillAura.blockStatus) || isUNCPBlocking())

    private fun updateSlot() {
        SilentHotbar.selectSlotSilently(this, (SilentHotbar.currentSlot + 1) % 9, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }
    
    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        
        // Handle Matrix mode specific logic
        if (consumeMode.contains("Matrix") || swordMode.contains("Matrix") || bowPacket.contains("Matrix")) {
            // Update Matrix variables
            index3 = index2
            index2 = index1
            index1 = if (player.isUsingItem) 1 else 0
            
            // Additional Matrix logic can go here
            
            // Update additional Matrix variables for new modes
            if (consumeMode.contains("Matrix3") || swordMode.contains("Matrix3") || bowPacket.contains("Matrix3")) {
                // Additional Matrix3 logic
            }
            
            if (consumeMode.contains("Matrix4") || swordMode.contains("Matrix4") || bowPacket.contains("Matrix4")) {
                // Additional Matrix4 logic
            }
            
            if (consumeMode.contains("Matrix5") || swordMode.contains("Matrix5") || bowPacket.contains("Matrix5")) {
                // Additional Matrix5 logic
            }
            
            if (consumeMode.contains("Matrix6") || swordMode.contains("Matrix6") || bowPacket.contains("Matrix6")) {
                // Additional Matrix6 logic
            }
        }
    }
}
