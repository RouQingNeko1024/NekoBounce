/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.JumpEvent
import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.block.BlockUtils.collideBlock
import net.minecraft.block.BlockLiquid
import net.minecraft.util.AxisAlignedBB

object ReverseStep : Module("ReverseStep", Category.MOVEMENT) {

    private val motion by float("Motion", 1f, 0.21f..4f)
    private var jumped = false

    val onUpdate = handler<UpdateEvent>(always = true) {
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.onGround)
            jumped = false

        if (thePlayer.motionY > 0)
            jumped = true

        if (!handleEvents())
            return@handler

        if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid } ||
            collideBlock(
                AxisAlignedBB.fromBounds(
                    thePlayer.entityBoundingBox.maxX,
                    thePlayer.entityBoundingBox.maxY,
                    thePlayer.entityBoundingBox.maxZ,
                    thePlayer.entityBoundingBox.minX,
                    thePlayer.entityBoundingBox.minY - 0.01,
                    thePlayer.entityBoundingBox.minZ
                )
            ) {
                it is BlockLiquid
            }) return@handler

        if (!mc.gameSettings.keyBindJump.isKeyDown && !thePlayer.onGround && !thePlayer.movementInput.jump && thePlayer.motionY <= 0.0 && thePlayer.fallDistance <= 1f && !jumped)
            thePlayer.motionY = (-motion).toDouble()
    }

    val onJump = handler<JumpEvent>(always = true) {
        jumped = true
    }

}
