/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.JumpEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.block.block
import net.minecraft.block.BlockSlime

object SlimeJump : Module("SlimeJump", Category.MOVEMENT) {

    private val motion by float("Motion", 0.42f, 0.2f..1f)
    private val mode by choices("Mode", arrayOf("Set", "Add"), "Add")

    val onJump = handler<JumpEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (mc.thePlayer != null && mc.theWorld != null && thePlayer.position.down().block is BlockSlime) {
            event.cancelEvent()

            when (mode.lowercase()) {
                "set" -> thePlayer.motionY = motion.toDouble()
                "add" -> thePlayer.motionY += motion
            }
        }
    }
}