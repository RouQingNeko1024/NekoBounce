/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
//Full Ai Code
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TargetESP : Module("TargetESP", Category.RENDER) {

    private val pointCount by int("PointCount", 12, 4..32)
    private val circleRadius by float("CircleRadius", 0.5f, 0.1f..2.0f)
    private val circleColor by color("CircleColor", Color(255, 0, 0, 200))
    private val yOffset by float("YOffset", 0.0f, -1.0f..1.0f)
    private val lineWidth by float("LineWidth", 2.0f, 0.5f..5.0f)
    private val drawPoints by boolean("DrawPoints", true)
    private val pointColor by color("PointColor", Color(0, 255, 0, 255))
    private val pointSize by float("PointSize", 4.0f, 1.0f..10.0f) { drawPoints }

    private val auraModules = arrayOf(
        net.ccbluex.liquidbounce.features.module.modules.combat.KillAura,
        net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2,
        net.ccbluex.liquidbounce.features.module.modules.combat.KillAura3,
        net.ccbluex.liquidbounce.features.module.modules.combat.Aura
    )

    private fun getTarget(): EntityLivingBase? {
        for (module in auraModules) {
            when (module) {
                is KillAura -> {
                    if (module.state && module.target != null) {
                        return module.target
                    }
                }
                is net.ccbluex.liquidbounce.features.module.modules.combat.KillAura2 -> {
                    if (module.state && module.target != null) {
                        return module.target
                    }
                }
                is net.ccbluex.liquidbounce.features.module.modules.combat.KillAura3 -> {
                    if (module.state && module.target != null) {
                        return module.target
                    }
                }
                is net.ccbluex.liquidbounce.features.module.modules.combat.Aura -> {
                    if (module.state && module.target != null) {
                        return module.target
                    }
                }
            }
        }
        return null
    }

    val onRender3D = handler<Render3DEvent> { event ->
        val target = getTarget() ?: return@handler
        val renderManager = mc.renderManager ?: return@handler
        val viewerPosX = renderManager.viewerPosX
        val viewerPosY = renderManager.viewerPosY
        val viewerPosZ = renderManager.viewerPosZ

        val x = (target.prevPosX + (target.posX - target.prevPosX) * event.partialTicks) - viewerPosX
        val y = (target.prevPosY + (target.posY - target.prevPosY) * event.partialTicks) + yOffset - viewerPosY
        val z = (target.prevPosZ + (target.posZ - target.prevPosZ) * event.partialTicks) - viewerPosZ

        glPushMatrix()
        glTranslated(x, y, z)
        glNormal3f(0.0f, 1.0f, 0.0f)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glLineWidth(lineWidth)
        
        // 绘制圆圈
        glBegin(GL_LINE_LOOP)
        glColor4f(
            circleColor.red / 255f,
            circleColor.green / 255f,
            circleColor.blue / 255f,
            circleColor.alpha / 255f
        )

        for (i in 0 until pointCount) {
            val angle = (2 * Math.PI * i / pointCount).toFloat()
            val pointX = Math.cos(angle.toDouble()).toFloat() * circleRadius
            val pointZ = Math.sin(angle.toDouble()).toFloat() * circleRadius
            glVertex3f(pointX, 0.0f, pointZ)
        }

        glEnd()
        
        // 绘制点
        if (drawPoints) {
            glPointSize(pointSize)
            glBegin(GL_POINTS)
            glColor4f(
                pointColor.red / 255f,
                pointColor.green / 255f,
                pointColor.blue / 255f,
                pointColor.alpha / 255f
            )
            
            for (i in 0 until pointCount) {
                val angle = (2 * Math.PI * i / pointCount).toFloat()
                val pointX = Math.cos(angle.toDouble()).toFloat() * circleRadius
                val pointZ = Math.sin(angle.toDouble()).toFloat() * circleRadius
                glVertex3f(pointX, 0.0f, pointZ)
            }
            
            glEnd()
            glPointSize(1.0f)
        }
        
        glDepthMask(true)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glPopMatrix()
    }
}