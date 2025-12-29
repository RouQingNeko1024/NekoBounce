/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.block.block
import io.qzz.nekobounce.utils.extensions.isMoving
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos

object IceSpeed : Module("IceSpeed", Category.MOVEMENT) {
    private val mode by choices("Mode", arrayOf("NCP", "AAC", "Spartan"), "NCP")
    override fun onEnable() {
        if (mode == "NCP") {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        }
        super.onEnable()
    }

    val onUpdate = handler<UpdateEvent> {
        val mode = mode
        if (mode == "NCP") {
            Blocks.ice.slipperiness = 0.39f
            Blocks.packed_ice.slipperiness = 0.39f
        } else {
            Blocks.ice.slipperiness = 0.98f
            Blocks.packed_ice.slipperiness = 0.98f
        }

        val thePlayer = mc.thePlayer ?: return@handler

        if (!thePlayer.onGround || thePlayer.isOnLadder || thePlayer.isSneaking || !thePlayer.isSprinting || !thePlayer.isMoving) {
            return@handler
        }

        if (thePlayer.position.down().block.let { it != Blocks.ice && it != Blocks.packed_ice }) {
            return@handler
        }

        when (mode) {
            "AAC" -> {
                thePlayer.motionX *= 1.342
                thePlayer.motionZ *= 1.342
                Blocks.ice.slipperiness = 0.6f
                Blocks.packed_ice.slipperiness = 0.6f
            }

            "Spartan" -> {
                val upBlock = BlockPos(thePlayer).up(2).block

                if (upBlock != Blocks.air) {
                    thePlayer.motionX *= 1.342
                    thePlayer.motionZ *= 1.342
                } else {
                    thePlayer.motionX *= 1.18
                    thePlayer.motionZ *= 1.18
                }

                Blocks.ice.slipperiness = 0.6f
                Blocks.packed_ice.slipperiness = 0.6f
            }
        }
    }

    override fun onDisable() {
        Blocks.ice.slipperiness = 0.98f
        Blocks.packed_ice.slipperiness = 0.98f
        super.onDisable()
    }
}