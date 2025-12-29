/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.MoveEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.block.block
import io.qzz.nekobounce.utils.movement.FallingPlayer
import net.minecraft.block.BlockAir
import net.minecraft.util.BlockPos

object SafeWalk : Module("SafeWalk", Category.MOVEMENT) {

    private val airSafe by boolean("AirSafe", false)
    private val maxFallDistanceValue = int("MaxFallDistance", 5, 0..100)

    private var lastGroundY: Double? = null
    private var lastCollisionY: Int? = null

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        if (player.capabilities.allowFlying || player.capabilities.isFlying
            || !mc.playerController.gameIsSurvivalOrAdventure()
        ) return@handler

        if (!maxFallDistanceValue.isMinimal() && player.onGround && BlockPos(player).down().block !is BlockAir) {
            lastGroundY = player.posY
            lastCollisionY = FallingPlayer(player, true).findCollision(60)?.pos?.y
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = maxFallDistanceValue.isMinimal()
                    || (lastGroundY != null && lastCollisionY != null
                    && lastGroundY!! - lastCollisionY!! > maxFallDistanceValue.get() + 1)
        }
    }
}
