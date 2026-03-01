/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.step
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import kotlin.math.cos
import kotlin.math.sin

object FirePhase : Module("FirePhase", Category.SKID) {
    private val mode by choices(
        "Mode",
        arrayOf("Vanilla", "Skip", "Spartan", "Clip", "AAC3.5.0", "Mineplex", "FullBlock", "BlocksMC", "Intave","OldMatrix", "AAC5.0.14", "H2OMC(Spiter)"),
        "Vanilla"
    )
    private val down by boolean("Down", false) { mode == "H2OMC(Spiter)" }

    private val tickTimer = TickTimer()
    private var mineplexClip = false
    private val mineplexTickTimer = TickTimer()
    private var shouldContinue = false
    private var clipState = 0
    private var yaw = 0.0
    private var phaseValue = 0.0 // TODO: What is this???
    private var spiderTicks = 1
    private var intaveMining = false
    private var fuckedServer = false

    override fun onDisable() {
        super.onDisable()
        mc.timer.timerSpeed = 1f
    }
    val onUpdate = handler<UpdateEvent> {
        if (mode == "FullBlock") {
            return@handler
        }

        val isInsideBlock = collideBlockIntersects(mc.thePlayer.entityBoundingBox) { block: Block? -> block !== air }

        if (isInsideBlock && mode != "Mineplex") {
            mc.thePlayer.noClip = true
            mc.thePlayer.motionY = 0.0
            mc.thePlayer.onGround = false
        }

        when (mode) {
            "H2OMC(Spiter)" -> {
                if(mc.thePlayer.ticksExisted<=10&&!fuckedServer){
                    mc.timer.timerSpeed=0.08f
                    val v = 6 * (if(down) -1 else 1)
                    sendPackets(
                        C06PacketPlayerPosLook(
                            mc.thePlayer.posX,
                            mc.thePlayer.posY +v, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true
                        )
                    )
                    mc.thePlayer.setPosition(mc.thePlayer.posX,mc.thePlayer.posY+v,mc.thePlayer.posZ)
                    fuckedServer = true
                }else if(mc.thePlayer.ticksExisted>20){
                    if(fuckedServer){
                        mc.timer.timerSpeed = 1f
                        Chat.print("Ok, now you can move")
                    }else Chat.print("Rejoin the world to apply the exploit")
                }
            }
            "AAC5.0.14" -> {
                sendPackets(
                    C06PacketPlayerPosLook(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 0.1, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true
                    ),
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 0.2, mc.thePlayer.posZ, true
                    ),
                    C06PacketPlayerPosLook(
                        mc.thePlayer.posX,
                        mc.thePlayer.posY - 0.3, mc.thePlayer.posZ, mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch, true
                    )
                )
            }
            "Vanilla" -> {
                if (mc.thePlayer.onGround && tickTimer.hasTimePassed(2) && mc.thePlayer.isCollidedHorizontally && (!isInsideBlock || mc.thePlayer.isSneaking)) {
                    sendPackets(
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(0.5, 0.0, 0.5, true),
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY + 0.2, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(0.5, 0.0, 0.5, true),
                        C04PacketPlayerPosition(
                            mc.thePlayer.posX + 0.5,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + 0.5,
                            true
                        )
                    )

                    val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
                    val x = -sin(yaw) * 0.04
                    val z = cos(yaw) * 0.04

                    mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z)
                    tickTimer.reset()
                }
            }

            "Skip" -> {
                if (mc.thePlayer.onGround && tickTimer.hasTimePassed(2) && mc.thePlayer.isCollidedHorizontally && (!isInsideBlock || mc.thePlayer.isSneaking)) {
                    val direction = direction
                    val posX = -sin(direction) * 0.3
                    val posZ = cos(direction) * 0.3

                    for (i in 0..2) {
                        sendPackets(
                            C04PacketPlayerPosition(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY + 0.06,
                                mc.thePlayer.posZ,
                                true
                            ),
                            C04PacketPlayerPosition(
                                mc.thePlayer.posX + posX * i,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ + posZ * i,
                                true
                            )
                        )
                    }

                    mc.thePlayer.entityBoundingBox = mc.thePlayer.entityBoundingBox.offset(posX, 0.0, posZ)
                    mc.thePlayer.setPositionAndUpdate(
                        mc.thePlayer.posX + posX,
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ + posZ
                    )
                    tickTimer.reset()
                }
            }

            "Spartan" -> {
                if (mc.thePlayer.onGround && tickTimer.hasTimePassed(2) && mc.thePlayer.isCollidedHorizontally && (!isInsideBlock || mc.thePlayer.isSneaking)) {
                    sendPackets(
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(0.5, 0.0, 0.5, true),
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.2, mc.thePlayer.posZ, true),
                        C04PacketPlayerPosition(0.5, 0.0, 0.5, true),
                        C04PacketPlayerPosition(
                            mc.thePlayer.posX + 0.5,
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + 0.5,
                            true
                        )
                    )

                    val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
                    val x = -sin(yaw) * 0.04
                    val z = cos(yaw) * 0.04

                    mc.thePlayer.setPosition(mc.thePlayer.posX + x, mc.thePlayer.posY, mc.thePlayer.posZ + z)
                    tickTimer.reset()
                }
            }

            "Clip" -> {
                if (tickTimer.hasTimePassed(2) && mc.thePlayer.isCollidedHorizontally && (!isInsideBlock || mc.thePlayer.isSneaking)) {
                    val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
                    val oldX = mc.thePlayer.posX
                    val oldZ = mc.thePlayer.posZ

                    for (i in 1..10) {
                        val x = -sin(yaw) * i
                        val z = cos(yaw) * i

                        if (BlockPos(oldX + x, mc.thePlayer.posY, oldZ + z).block === air && BlockPos(
                                oldX + x,
                                mc.thePlayer.posY + 1,
                                oldZ + z
                            ).block === air
                        ) {
                            mc.thePlayer.setPosition(oldX + x, mc.thePlayer.posY, oldZ + z)
                            break
                        }
                    }
                    tickTimer.reset()
                }
            }

            "Intave" -> {
                val check = mc.gameSettings.keyBindAttack.isKeyDown && mc.thePlayer.rotationPitch > 80f
                val blockPos = mc.thePlayer.position.down()

                if (check) {
                    mc.netHandler.addToSendQueue(
                        net.minecraft.network.play.client.C07PacketPlayerDigging(
                            net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            blockPos,
                            net.minecraft.util.EnumFacing.UP
                        )
                    )
                    intaveMining = true
                } else if (intaveMining) {
                    intaveMining = false
                }

                if (intaveMining) {
                    mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 0.0052, mc.thePlayer.posZ)
                }

                if (mc.thePlayer.isSneaking) {
                    val movementDistance = 0.005f
                    val rotation = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())

                    if (mc.gameSettings.keyBindForward.isKeyDown)
                        intaveMove(rotation, movementDistance, 1, 1)
                    else if (mc.gameSettings.keyBindBack.isKeyDown)
                        intaveMove(rotation, -movementDistance, 1, -1)
                    else if (mc.gameSettings.keyBindLeft.isKeyDown)
                        intaveMove(rotation, movementDistance, -1, 1)
                    else if (mc.gameSettings.keyBindRight.isKeyDown)
                        intaveMove(rotation, -movementDistance, -1, -1)
                }
            }
            "BlocksMC" -> {

                when (spiderTicks) {
                    1 -> {
                        if (mc.gameSettings.keyBindJump.isKeyDown && !mc.theWorld.isAirBlock(mc.thePlayer.position)) {
                            mc.thePlayer.motionY = 0.42
                            spiderTicks++
                        }
                        mc.thePlayer.onGround = true
                    }
                    2 -> {
                        mc.thePlayer.motionY = 0.33
                        spiderTicks++
                    }
                    3 -> {
                        mc.thePlayer.motionY = 0.25
                        spiderTicks++
                    }
                }

                if (spiderTicks > 3 || spiderTicks == 0) {
                    spiderTicks = 1
                }
                mc.thePlayer.noClip = true
                if (mc.thePlayer.isSneaking) {
                    val dir = direction
                    mc.thePlayer.motionX = -sin(dir) * 0.179
                    mc.thePlayer.motionZ = cos(dir) * 0.179
                }
            }


            "AAC3.5.0" -> {
                if (tickTimer.hasTimePassed(2) && mc.thePlayer.isCollidedHorizontally && (!isInsideBlock || mc.thePlayer.isSneaking)) {
                    val yaw = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
                    val oldX = mc.thePlayer.posX
                    val oldZ = mc.thePlayer.posZ
                    val x = -sin(yaw)
                    val z = cos(yaw)

                    mc.thePlayer.setPosition(oldX + x, mc.thePlayer.posY, oldZ + z)
                    tickTimer.reset()
                }
            }
            "OldMatrix" -> {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY - 3, mc.thePlayer.posZ)
                mc.gameSettings.keyBindForward.pressed = true
                MovementUtils.strafe(0.1f)
                mc.gameSettings.keyBindForward.pressed = false
            }
        }

        tickTimer.update()
    }

    val onBlockBB = handler<BlockBBEvent> { event ->
        if (mode == "FullBlock" || mode == "Mineplex") {
            if (mode == "BlocksMC") {
                if (event.block !is net.minecraft.block.BlockLiquid) {
                    if (event.y >= mc.thePlayer.posY) {
                        event.boundingBox = null
                    }
                }
                return@handler
            }

            return@handler
        }

        if (mc.thePlayer != null && collideBlockIntersects(mc.thePlayer.entityBoundingBox) { block: Block? -> block !== air } && event.boundingBox != null && event.boundingBox!!.maxY > mc.thePlayer.entityBoundingBox.minY) {
            val axisAlignedBB = event.boundingBox

            event.boundingBox = AxisAlignedBB.fromBounds(
                axisAlignedBB!!.maxX,
                mc.thePlayer.entityBoundingBox.minY,
                axisAlignedBB.maxZ,
                axisAlignedBB.minX,
                axisAlignedBB.minY,
                axisAlignedBB.minZ
            )
        }
    }

    override fun onEnable() {
        fuckedServer=false
        if (mode == "Intave" && mc.thePlayer != null) {
            intaveMining = false
        }
        if (mode == "BlocksMC" && mc.thePlayer != null) {
            spiderTicks = 1
            mc.thePlayer.motionY = 0.0
        }
        shouldContinue = false
        clipState = 0
        phaseValue = 0.0
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            if (mode == "AAC3.5.0") {
                val yaw = direction.toFloat()

                packet.x -= sin(yaw.toDouble()) * 0.00000001
                packet.z += cos(yaw.toDouble()) * 0.00000001
            }
        }
    }

    val onMove = handler<MoveEvent> { event ->
        when (mode) {
            "Mineplex" -> {
                if (mc.thePlayer.isCollidedHorizontally)
                    mineplexClip = true

                if (!mineplexClip)
                    return@handler

                mineplexTickTimer.update()

                event.x = 0.0
                event.z = 0.0

                if (mineplexTickTimer.hasTimePassed(3)) {
                    mineplexTickTimer.reset()
                    mineplexClip = false
                } else if (mineplexTickTimer.hasTimePassed(1)) {
                    val offset = if (mineplexTickTimer.hasTimePassed(2)) 1.6 else 0.06
                    val direction = direction

                    mc.thePlayer.setPosition(
                        mc.thePlayer.posX + (-sin(direction) * offset),
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ + (cos(direction) * offset)
                    )
                }
            }

            "FullBlock" -> {
                if (mc.thePlayer.isCollidedHorizontally)
                    clipState++

                when (clipState) {
                    1 -> {
                        val direction = direction
                        val cos = cos(direction)
                        val sin = sin(direction)

                        for (i in 0.025..2.0 step 0.025) {
                            shouldContinue = false
                            if (!mc.thePlayer.isMoving || mc.theWorld.getCollidingBoundingBoxes(
                                    mc.thePlayer,
                                    mc.thePlayer.entityBoundingBox
                                        .offset(
                                            -sin * i,
                                            0.0,
                                            cos * i
                                        )
                                ).isNotEmpty()
                            ) continue

                            mc.thePlayer.setPositionAndUpdate(
                                mc.thePlayer.posX - sin * 0.06,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ + cos * 0.06
                            )

                            if (i > 0.06) {
                                phaseValue = i
                                yaw = direction
                                shouldContinue = true
                            }

                            event.zeroXZ()
                            break
                        }

                        clipState++
                        if (!shouldContinue)
                            clipState = 0
                    }

                    2 -> {
                        val value = if (mc.thePlayer.isMoving) phaseValue else -0.06
                        mc.thePlayer.setPositionAndUpdate(
                            mc.thePlayer.posX - (sin(yaw) * value),
                            mc.thePlayer.posY,
                            mc.thePlayer.posZ + (cos(yaw) * value)
                        )
                        clipState = 0
                    }
                }
            }
        }
    }

    val onBlockPush = handler<BlockPushEvent> { event ->
        event.cancelEvent()
    }

    override val tag get() = mode

    private fun intaveMove(rotation: Double, distance: Float, xMultiplier: Int, zMultiplier: Int) {
        val xx = cos(rotation) * distance * xMultiplier
        val zz = sin(rotation) * distance * zMultiplier
        mc.thePlayer.setPosition(mc.thePlayer.posX + xx, mc.thePlayer.posY, mc.thePlayer.posZ + zz)
    }

}