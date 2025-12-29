/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.JumpEvent
import io.qzz.nekobounce.event.MoveEvent
import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.aac.AACv1
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.aac.AACv2
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.aac.AACv3
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.ncp.NCP
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other.Buzz
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other.Hycraft
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other.Redesky
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other.VerusDamage
import io.qzz.nekobounce.features.module.modules.movement.longjumpmodes.other.VerusDamage.damaged
import io.qzz.nekobounce.utils.extensions.isMoving
import io.qzz.nekobounce.utils.extensions.tryJump

object LongJump : Module("LongJump", Category.MOVEMENT) {

    private val longJumpModes = arrayOf(
        // NCP
        NCP,

        // AAC
        AACv1, AACv2, AACv3,

        // Other
        Redesky, Hycraft, Buzz, VerusDamage
    )

    private val modes = longJumpModes.map { it.modeName }.toTypedArray()

    val mode by choices("Mode", modes, "NCP")
    val ncpBoost by float("NCPBoost", 4.25f, 1f..10f) { mode == "NCP" }

    private val autoJump by boolean("AutoJump", true)

    val autoDisable by boolean("AutoDisable", true) { mode == "VerusDamage" }

    var jumped = false
    var canBoost = false
    var teleported = false

    val onUpdate = handler<UpdateEvent> {
        if (jumped) {
            val mode = mode

            if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
                jumped = false

                if (mode == "NCP") {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                }
                return@handler
            }

            modeModule.onUpdate()
        }
        if (autoJump && mc.thePlayer.onGround && mc.thePlayer.isMoving) {
            if (autoDisable && !damaged) {
                return@handler
            }

            jumped = true
            mc.thePlayer.tryJump()
        }
    }

    val onMove = handler<MoveEvent> { event ->
        modeModule.onMove(event)
    }

    override fun onEnable() {
        modeModule.onEnable()
    }

    override fun onDisable() {
        modeModule.onDisable()
    }

    val onJump = handler<JumpEvent>(always = true) { event ->
        jumped = true
        canBoost = true
        teleported = false

        if (handleEvents()) {
            modeModule.onJump(event)
        }
    }

    override val tag
        get() = mode

    private val modeModule
        get() = longJumpModes.find { it.modeName == mode }!!
}
