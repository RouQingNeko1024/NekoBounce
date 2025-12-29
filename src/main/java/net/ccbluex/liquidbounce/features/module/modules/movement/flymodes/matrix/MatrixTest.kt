/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code By GoldBounce,Lizz,NightSky,FDP
 * https://github.com/SkidderMC/FDPClient
 * https://github.com/qm123pz/NightSky-Client
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.matrix

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.extras.StuckUtils
import kotlin.math.*

object MatrixTest : FlyMode("MatrixTest") {

    private var moveTicksRemaining = 0
    private var allowMovement = true

    private var burstTicksRemaining = 0

    private var stuckActive = false
    private var stuckStartedByThis = false

    private var timerTemporaryActive = false

    private val period get() = max(1, Fly.matrixPeriod)
    private val motionY1 get() = Fly.matrixMotionY1.toDouble()
    private val motionY2 get() = Fly.matrixMotionY2.toDouble()

    private val timerSpeedConfigured get() = Fly.matrixTimerSpeed
    private val configuredMoveTick get() = Fly.matrixMoveTick
    private val onlyOnDamage get() = Fly.matrixOnlyOnDamage
    private val useStuck get() = Fly.matrixUseStuck

    private var horizontalBoostFactor  = 1.0F // e.g. 1.15 default
    private var burstStrength  = 0.01 // e.g. 0.03
    private var burstTicks  = 2
    private var maxHorizontalSpeed  = 1.1 // cap

    override fun onEnable() {
        allowMovement = !onlyOnDamage
        moveTicksRemaining = 0
        burstTicksRemaining = 0
        stuckActive = false
        stuckStartedByThis = false
        timerTemporaryActive = false

        try { mc.timer.timerSpeed = timerSpeedConfigured } catch (_: Throwable) {}
    }

    override fun onDisable() {
        try { mc.timer.timerSpeed = 1f } catch (_: Throwable) {}

        if (stuckActive && stuckStartedByThis) {
            try { StuckUtils.stopStuck() } catch (_: Throwable) {}
            stuckActive = false
            stuckStartedByThis = false
        }

        allowMovement = true
        moveTicksRemaining = 0
        burstTicksRemaining = 0
        timerTemporaryActive = false
    }

    override fun onTick() {
        val player = mc.thePlayer ?: return

        if (player.hurtTime > 0) {
            if (onlyOnDamage) {
                allowMovement = true
                moveTicksRemaining = configuredMoveTick
                burstTicksRemaining = burstTicks
                if (stuckActive && stuckStartedByThis) {
                    try { StuckUtils.stopStuck() } catch (_: Throwable) {}
                    stuckActive = false
                    stuckStartedByThis = false
                }
                if (timerTemporaryActive) {
                    try { mc.timer.timerSpeed = timerSpeedConfigured } catch (_: Throwable) {}
                    timerTemporaryActive = false
                }
            } else {
            }
        }
    }

    override fun onMove(event: MoveEvent) {
        Fly.handleVanillaKickBypass()
    }
    override fun onUpdate() {
        val player = mc.thePlayer ?: return

        try {
            if (!timerTemporaryActive) mc.timer.timerSpeed = timerSpeedConfigured
        } catch (_: Throwable) {}


        if (moveTicksRemaining > 0) {
            moveTicksRemaining--
            if (moveTicksRemaining <= 0) {
                allowMovement = false

                if (useStuck) {
                    try {
                        StuckUtils.stuck()
                        stuckActive = true
                        stuckStartedByThis = true
                    } catch (_: Throwable) {
                        stuckActive = false
                        stuckStartedByThis = false
                    }
                } else {
                    try {
                        mc.timer.timerSpeed = 0.1f
                        timerTemporaryActive = true
                    } catch (_: Throwable) {}
                }
            }
        }

        if (burstTicksRemaining > 0) {
            burstTicksRemaining--
        }

        if (timerTemporaryActive) {
            if (player.onGround || player.hurtTime > 0) {
                try {
                    mc.timer.timerSpeed = timerSpeedConfigured
                } catch (_: Throwable) {}
                timerTemporaryActive = false
            }
        }

        if (allowMovement) {
            val mod = (player.ticksExisted % period)
            val mY = if (mod % period == 0) motionY1 else motionY2
            try { player.motionY = mY } catch (_: Throwable) {}

            val moving = (player.moveForward != 0f || player.moveStrafing != 0f)
            if (moving) {
                val yawRad = Math.toRadians(player.rotationYaw.toDouble())
                val dirX = -sin(yawRad)
                val dirZ = cos(yawRad)

                if (burstTicksRemaining > 0) {
                    val forwardFactor = max(0.0, player.moveForward.toDouble())
                    val strafeFactor = player.moveStrafing.toDouble()
                    val primaryFactor = if (forwardFactor > 0.0) forwardFactor else (if (abs(strafeFactor) > 0.0) 0.6 else 1.0)
                    val impulse = burstStrength * primaryFactor
                    player.motionX += dirX * impulse
                    player.motionZ += dirZ * impulse
                }

                val extra = (horizontalBoostFactor - 1.0f).coerceAtLeast(0f).toDouble()
                if (extra > 0.0001) {
                    val accel = 0.02 * extra
                    player.motionX += dirX * accel
                    player.motionZ += dirZ * accel
                }

                val hspeed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
                val maxH = maxHorizontalSpeed.coerceAtLeast(0.2)
                if (hspeed > maxH) {
                    val scale = maxH / hspeed
                    player.motionX *= scale
                    player.motionZ *= scale
                }
            }

            if (stuckActive && stuckStartedByThis) {
                try { StuckUtils.stopStuck() } catch (_: Throwable) {}
                stuckActive = false
                stuckStartedByThis = false
            }
        } else {
        }
    }

    fun onWorld(event: WorldEvent) {
        try { mc.timer.timerSpeed = 1f } catch (_: Throwable) {}
        if (stuckActive && stuckStartedByThis) {
            try { StuckUtils.stopStuck() } catch (_: Throwable) {}
        }
        stuckActive = false
        stuckStartedByThis = false
        timerTemporaryActive = false
        allowMovement = !onlyOnDamage
        moveTicksRemaining = 0
        burstTicksRemaining = 0
    }
}
