package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import kotlin.math.cos
import kotlin.math.sin

object OldFDPPhases : Module("OldFDPPhases", Category.SKID) {

    private val mode by choices(
        "Mode",
        arrayOf("Vanilla", "Skip", "Spartan", "Clip", "AAC3.5.0", "AACv4", "Matrix", "Mineplex", "FastFall", "Automatic"),
        "Vanilla"
    )

    private val automaticOffset by float("AutomaticOffset", 4f, -8f..8f)
    private val automaticPhaseDelay by int("AutomaticPhaseDelay", 1000, 500..5000)
    private val automaticFreezeMotion by boolean("AutomaticFreezeMotion", true)

    private val tickTimer = TickTimer()
    private val mineplexTickTimer = TickTimer()
    private val automaticPhaseTimer = MSTimer()
    private var mineplexClip = false

    private val onUpdate = handler<UpdateEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        val isInsideBlock = BlockUtils.collideBlockIntersects(thePlayer.entityBoundingBox) { block: Block? -> block !is BlockAir }

        if (isInsideBlock && mode in listOf("Vanilla", "Skip", "Spartan", "Clip", "AAC3.5.0")) {
            thePlayer.noClip = true
            thePlayer.motionY = 0.0
            thePlayer.onGround = true
        }

        tickTimer.update()

        when (mode.lowercase()) {
            "aacv4" -> {
                thePlayer.sendQueue.addToSendQueue(C06PacketPlayerPosLook(thePlayer.posX, thePlayer.posY + -0.00000001, thePlayer.posZ, thePlayer.rotationYaw, thePlayer.rotationPitch, false))
                thePlayer.sendQueue.addToSendQueue(C06PacketPlayerPosLook(thePlayer.posX, thePlayer.posY - 1, thePlayer.posZ, thePlayer.rotationYaw, thePlayer.rotationPitch, false))
            }
            "fastfall" -> {
                thePlayer.noClip = true
                thePlayer.motionY -= 10.0
                thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY - 0.5, thePlayer.posZ)
                thePlayer.onGround = BlockUtils.collideBlockIntersects(thePlayer.entityBoundingBox) { block: Block? -> block !is BlockAir }
            }
            "matrix" -> {
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY - 3, thePlayer.posZ)
                mc.gameSettings.keyBindForward.pressed = true
                strafe(0.1f)
                mc.gameSettings.keyBindForward.pressed = false
            }
            "automatic" -> {
                if (automaticPhaseTimer.hasTimePassed(automaticPhaseDelay.toLong())) {
                    if (mineplexClip) {
                        mineplexClip = false
                        thePlayer.setPosition(thePlayer.posX, thePlayer.posY - automaticOffset, thePlayer.posZ)
                    }
                } else if (automaticFreezeMotion) {
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                }
            }
            "vanilla" -> {
                if (!thePlayer.onGround || !tickTimer.hasTimePassed(2) || !thePlayer.isCollidedHorizontally || 
                    !(!isInsideBlock || thePlayer.isSneaking)) return@handler
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                val x = -sin(yaw) * 0.04
                val z = cos(yaw) * 0.04
                thePlayer.setPosition(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z)
                tickTimer.reset()
            }
            "skip" -> {
                if (!thePlayer.onGround || !tickTimer.hasTimePassed(2) || !thePlayer.isCollidedHorizontally || 
                    !(!isInsideBlock || thePlayer.isSneaking)) return@handler
                val direction = getDirection()
                val posX = -sin(direction) * 0.3
                val posZ = cos(direction) * 0.3
                repeat(3) { i ->
                    thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.06, thePlayer.posZ, true))
                    thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX + posX * i, thePlayer.posY, thePlayer.posZ + posZ * i, true))
                }
                thePlayer.entityBoundingBox = thePlayer.entityBoundingBox.offset(posX, 0.0, posZ)
                thePlayer.setPositionAndUpdate(thePlayer.posX + posX, thePlayer.posY, thePlayer.posZ + posZ)
                tickTimer.reset()
            }
            "spartan" -> {
                if (!thePlayer.onGround || !tickTimer.hasTimePassed(2) || !thePlayer.isCollidedHorizontally || 
                    !(!isInsideBlock || thePlayer.isSneaking)) return@handler
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(0.5, 0.0, 0.5, true))
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY - 0.2, thePlayer.posZ, true))
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(0.5, 0.0, 0.5, true))
                thePlayer.sendQueue.addToSendQueue(C04PacketPlayerPosition(thePlayer.posX + 0.5, thePlayer.posY, thePlayer.posZ + 0.5, true))
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                val x = -sin(yaw) * 0.04
                val z = cos(yaw) * 0.04
                thePlayer.setPosition(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z)
                tickTimer.reset()
            }
            "clip" -> {
                if (!tickTimer.hasTimePassed(2) || !thePlayer.isCollidedHorizontally || 
                    !(!isInsideBlock || thePlayer.isSneaking)) return@handler
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                val oldX = thePlayer.posX
                val oldZ = thePlayer.posZ
                for (i in 1..10) {
                    val x = -sin(yaw) * i
                    val z = cos(yaw) * i
                    if (BlockUtils.getBlock(BlockPos(oldX + x, thePlayer.posY, oldZ + z)) is BlockAir &&
                        BlockUtils.getBlock(BlockPos(oldX + x, thePlayer.posY + 1, oldZ + z)) is BlockAir) {
                        thePlayer.setPosition(oldX + x, thePlayer.posY, oldZ + z)
                        break
                    }
                }
                tickTimer.reset()
            }
            "aac3.5.0" -> {
                if (!tickTimer.hasTimePassed(2) || !thePlayer.isCollidedHorizontally || 
                    !(!isInsideBlock || thePlayer.isSneaking)) return@handler
                val yaw = Math.toRadians(thePlayer.rotationYaw.toDouble())
                val oldX = thePlayer.posX
                val oldZ = thePlayer.posZ
                val x = -sin(yaw)
                val z = cos(yaw)
                thePlayer.setPosition(oldX + x, thePlayer.posY, oldZ + z)
                tickTimer.reset()
            }
        }
    }

    private val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val thePlayer = mc.thePlayer ?: return@handler

        if (packet is C03PacketPlayer) {
            when {
                mode == "AAC3.5.0" -> {
                    val yaw = getDirection().toFloat()
                    packet.x = packet.x - MathHelper.sin(yaw) * 0.00000001f
                    packet.z = packet.z + MathHelper.cos(yaw) * 0.00000001f
                }
                mode == "Automatic" -> {
                    if (thePlayer.ticksExisted < 100) {
                        automaticPhaseTimer.reset()
                        mineplexClip = true
                    }
                }
            }
        }
    }

    private val onMove = handler<MoveEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (mode == "Mineplex") {
            if (thePlayer.isCollidedHorizontally) mineplexClip = true
            if (!mineplexClip) return@handler
            mineplexTickTimer.update()
            event.x = 0.0
            event.z = 0.0
            when {
                mineplexTickTimer.hasTimePassed(3) -> {
                    mineplexTickTimer.reset()
                    mineplexClip = false
                }
                mineplexTickTimer.hasTimePassed(1) -> {
                    val offset = if (mineplexTickTimer.hasTimePassed(2)) 1.6 else 0.06
                    val direction = getDirection()
                    thePlayer.setPosition(
                        thePlayer.posX + -sin(direction) * offset,
                        thePlayer.posY,
                        thePlayer.posZ + cos(direction) * offset
                    )
                }
            }
        }
    }

    private fun strafe(speed: Float) {
        val thePlayer = mc.thePlayer ?: return
        val yaw = getDirection().toFloat()
        thePlayer.motionX = -MathHelper.sin(yaw) * speed.toDouble()
        thePlayer.motionZ = MathHelper.cos(yaw) * speed.toDouble()
    }

    private fun getDirection(): Double {
        val thePlayer = mc.thePlayer ?: return 0.0
        var rotationYaw = thePlayer.rotationYaw
        if (thePlayer.moveForward < 0.0f) {
            rotationYaw += 180.0f
        }
        var forward = 1.0f
        if (thePlayer.moveForward < 0.0f) {
            forward = -0.5f
        } else if (thePlayer.moveForward > 0.0f) {
            forward = 0.5f
        }
        if (thePlayer.moveStrafing > 0.0f) {
            rotationYaw -= 90.0f * forward
        }
        if (thePlayer.moveStrafing < 0.0f) {
            rotationYaw += 90.0f * forward
        }
        return Math.toRadians(rotationYaw.toDouble())
    }

    override val tag: String
        get() = mode
}