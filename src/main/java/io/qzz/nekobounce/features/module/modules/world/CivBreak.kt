/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.world

import io.qzz.nekobounce.event.*
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.block.BlockUtils.getCenterDistance
import io.qzz.nekobounce.utils.block.block
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.client.PacketUtils.sendPackets
import io.qzz.nekobounce.utils.render.RenderUtils.drawBlockBox
import io.qzz.nekobounce.utils.rotation.RotationSettings
import io.qzz.nekobounce.utils.rotation.RotationUtils.faceBlock
import io.qzz.nekobounce.utils.rotation.RotationUtils.setTargetRotation
import net.minecraft.init.Blocks.air
import net.minecraft.init.Blocks.bedrock
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.START_DESTROY_BLOCK
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.awt.Color

object CivBreak : Module("CivBreak", Category.WORLD) {

    private val range by float("Range", 5F, 1F..6F)
    private val visualSwing by boolean("VisualSwing", true).subjective()

    private val options = RotationSettings(this).withoutKeepRotation()

    private var blockPos: BlockPos? = null
    private var enumFacing: EnumFacing? = null

    val onBlockClick = handler<ClickBlockEvent> { event ->
        blockPos = event.clickedBlock?.takeIf { it.block != bedrock } ?: return@handler
        enumFacing = event.enumFacing ?: return@handler

        // Break
        sendPackets(
            C07PacketPlayerDigging(START_DESTROY_BLOCK, blockPos, enumFacing),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, enumFacing)
        )
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val pos = blockPos ?: return@handler
        val isAirBlock = pos.block == air

        if (isAirBlock || getCenterDistance(pos) > range) {
            blockPos = null
            return@handler
        }

        if (options.rotationsActive) {
            val spot = faceBlock(pos) ?: return@handler

            setTargetRotation(spot.rotation, options = options)
        }
    }

    val onTick = handler<GameTickEvent> {
        blockPos ?: return@handler
        enumFacing ?: return@handler

        if (visualSwing) {
            mc.thePlayer.swingItem()
        } else {
            sendPacket(C0APacketAnimation())
        }

        // Break
        sendPackets(
            C07PacketPlayerDigging(START_DESTROY_BLOCK, blockPos, enumFacing),
            C07PacketPlayerDigging(STOP_DESTROY_BLOCK, blockPos, enumFacing)
        )

        mc.playerController.clickBlock(blockPos, enumFacing)
    }

    val onRender3D = handler<Render3DEvent> {
        drawBlockBox(blockPos ?: return@handler, Color.RED, true)
    }
}