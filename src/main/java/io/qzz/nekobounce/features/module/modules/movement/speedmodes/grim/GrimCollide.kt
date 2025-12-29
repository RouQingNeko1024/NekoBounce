/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes.grim

import io.qzz.nekobounce.features.module.modules.movement.Speed
import io.qzz.nekobounce.features.module.modules.movement.speedmodes.SpeedMode
import io.qzz.nekobounce.utils.extensions.isMoving
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import kotlin.math.cos
import kotlin.math.sin

/*
* GrimCollide Speed mode
* Based on entity collision detection with velocity boost
*/
object GrimCollide : SpeedMode("GrimCollide") {
    
 
    override fun onUpdate() {
        val player = mc.thePlayer ?: return
        
        if (!player.isMoving) return
        
        var collisions = 0
        val playerBox = player.entityBoundingBox.expand(1.0, 1.0, 1.0)
        
        for (entity in mc.theWorld.loadedEntityList) {
            if (!isValidEntity(entity) || 
                entity.entityId == player.entityId || 
                !playerBox.intersectsWith(entity.entityBoundingBox)) {
                continue
            }
            collisions++
        }
        
        if (collisions > 0) {
            val yaw = Math.toRadians(player.rotationYaw.toDouble())
            val boost = Speed.grimCollideSpeed.toDouble() * collisions.toDouble()
            
            player.motionX += -sin(yaw) * boost
            player.motionZ += cos(yaw) * boost
        }
    }
    
    private fun isValidEntity(entity: Entity): Boolean {
        return entity is EntityLivingBase && entity !is EntityArmorStand
    }
}