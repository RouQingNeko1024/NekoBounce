//Code NekoAi
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MovingObjectPosition
import org.lwjgl.opengl.GL11
import java.awt.Color

object AutoPathFinder : Module("AutoPathFinder", Category.MOVEMENT) {
    
    private var target: EntityLivingBase? = null
    
    val onUpdate = handler<UpdateEvent> {
        if (mc.gameSettings.keyBindPickBlock.isPressed()) {
            val objectMouseOver = mc.objectMouseOver
            if (objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
                val entityHit = objectMouseOver.entityHit
                if (entityHit is EntityLivingBase) {
                    target = entityHit
                }
            }
        }
    }
    
    val onRender = handler<Render3DEvent> { event ->
        val entity = target ?: return@handler
        
        val renderManager = mc.renderManager ?: return@handler
        val partialTicks = event.partialTicks
        
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks - renderManager.renderPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks - renderManager.renderPosY + entity.height + 0.2
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks - renderManager.renderPosZ
        
        GL11.glPushMatrix()
        GL11.glTranslated(x, y, z)
        GL11.glNormal3f(0.0f, 1.0f, 0.0f)
        GL11.glRotatef(-renderManager.playerViewY, 0.0f, 1.0f, 0.0f)
        GL11.glScalef(-0.05f, -0.05f, 0.05f)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glColor4f(1.0f, 0.0f, 0.0f, 1.0f)
        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex2f(0.0f, 0.0f)
        for (i in 0..360 step 10) {
            val rad = Math.toRadians(i.toDouble())
            val cos = Math.cos(rad) * 10.0
            val sin = Math.sin(rad) * 10.0
            GL11.glVertex2f(cos.toFloat(), sin.toFloat())
        }
        GL11.glEnd()
        
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LIGHTING)
        GL11.glPopMatrix()
    }
    
    override fun onDisable() {
        target = null
    }
    
    override val tag: String?
        get() = target?.displayName?.unformattedText
}