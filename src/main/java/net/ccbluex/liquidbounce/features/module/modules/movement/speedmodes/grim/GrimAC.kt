/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityBoat
import net.minecraft.entity.item.EntityMinecart
import net.minecraft.entity.projectile.EntityFishHook
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB

/*
* GrimAC Speed mode
* Based on entity collision detection
*/
object GrimAC : SpeedMode("GrimAC") {
    
    private var entityCount = 0
    
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        
        if (!player.isMoving) return
        
        val playerBox = player.entityBoundingBox.expand(1.0, 1.0, 1.0)
        entityCount = 0
        
        for (entity in mc.theWorld.loadedEntityList) {
            if (!isValidEntity(entity) || 
                entity.entityId == player.entityId || 
                !playerBox.intersectsWith(entity.entityBoundingBox) || 
                entity.entityId == -8 || 
                entity.entityId == -1337) {
                continue
            }
            entityCount++
        }
        
        if (entityCount > 0) {
            val strafeOffset = minOf(entityCount, Speed.grimacAmount).toFloat() * 0.06f
            MovementUtils.strafe(strafeOffset)
        }
        
        // 自动跳跃逻辑
        if (player.isMoving && Speed.grimacAutoJump && player.onGround) {
            player.motionY = 0.42
        }
    }
    
    private fun isValidEntity(entity: Entity): Boolean {
        return when (entity) {
            is EntityLivingBase, is EntityBoat, is EntityMinecart, is EntityFishHook -> true
            else -> false
        } && entity !is EntityArmorStand
    }
}