/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.flymodes.blocksmc

import io.qzz.nekobounce.event.Listenable
import io.qzz.nekobounce.event.WorldEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.modules.movement.Fly
import io.qzz.nekobounce.features.module.modules.movement.Fly.boostSpeed
import io.qzz.nekobounce.features.module.modules.movement.Fly.debugFly
import io.qzz.nekobounce.features.module.modules.movement.Fly.extraBoost
import io.qzz.nekobounce.features.module.modules.movement.Fly.stable
import io.qzz.nekobounce.features.module.modules.movement.Fly.stopOnLanding
import io.qzz.nekobounce.features.module.modules.movement.Fly.stopOnNoMove
import io.qzz.nekobounce.features.module.modules.movement.Fly.timerSlowed
import io.qzz.nekobounce.features.module.modules.movement.flymodes.FlyMode
import io.qzz.nekobounce.utils.client.PacketUtils.sendPackets
import io.qzz.nekobounce.utils.client.chat
import io.qzz.nekobounce.utils.extensions.airTicks
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump
import io.qzz.nekobounce.utils.movement.MovementUtils.strafe
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.world.World


object BlocksMC : FlyMode("BlocksMC"), Listenable {

    private var isFlying = false
    private var isNotUnder = false
    private var isTeleported = false
    private var jumped = false

    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (isFlying) {
            if (player.onGround && stopOnLanding) {
                if (debugFly)
                    chat("Ground Detected.. Stopping Fly")
                Fly.state = false
            }

            if (!player.isMoving && stopOnNoMove) {
                if (debugFly)
                    chat("No Movement Detected.. Stopping Fly. (Could be flagged)")
                Fly.state = false
            }
        }

        if (shouldFly(player, world)) {
            if (isTeleported) {

                if (stable)
                    player.motionY = 0.0

                handleTimerSlow(player)
                handlePlayerFlying(player)
            } else {
                if (debugFly)
                    chat("Waiting to be Teleported.. Please ensure you're below a block.")
            }
        } else {
            handleTeleport(player)
        }

        strafe()
    }

    override fun onDisable() {
        isNotUnder = false
        isFlying = false
        isTeleported = false
        jumped = false
    }

    val onWorld = handler<WorldEvent> {
        Fly.state = false
    }

    private fun handleTimerSlow(player: EntityPlayerSP) {
        if (!player.onGround && timerSlowed) {
            if (player.ticksExisted % 7 == 0) {
                mc.timer.timerSpeed = 0.415f
            } else {
                mc.timer.timerSpeed = 0.35f
            }
        } else {
            mc.timer.timerSpeed = 1.0f
        }
    }

    private fun shouldFly(player: EntityPlayerSP, world: World): Boolean {
        return world.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, 1.0, 0.0))
            .isEmpty() || isFlying
    }

    private fun handlePlayerFlying(player: EntityPlayerSP) {
        when (player.airTicks) {
            0 -> {
                if (isNotUnder && isTeleported) {
                    strafe(boostSpeed + extraBoost)
                    player.tryJump()
                    isFlying = true
                    isNotUnder = false
                }
            }

            1 -> {
                if (isFlying) {
                    strafe(boostSpeed)
                }
            }
        }
    }

    private fun handleTeleport(player: EntityPlayerSP) {
        isNotUnder = true
        if (!isTeleported) {
            sendPackets(
                C04PacketPlayerPosition(
                    player.posX,
                    // Clipping is now patch in BlocksMC
                    player.posY - 0.05,
                    player.posZ,
                    false
                )
            )
            sendPackets(
                C04PacketPlayerPosition(
                    player.posX,
                    player.posY,
                    player.posZ,
                    false
                )
            )

            isTeleported = true
            if (debugFly)
                chat("Teleported.. Fly Now!")
        }
    }
}