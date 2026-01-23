/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import kotlin.math.cos
import kotlin.math.sin
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object Fog : Module("Fog", Category.RENDER) {

    private val fogColor by color("FogColor", Color(100, 100, 100, 150))
    private val fogDistance by float("FogDistance", 10.0f, 1.0f..50.0f)
    private val fogSize by float("FogSize", 3.0f, 0.5f..10.0f)
    private val fogDensity by float("FogDensity", 0.8f, 0.1f..1.0f)
    private val fogHeight by float("FogHeight", 0.0f, -10.0f..10.0f)
    private val animate by boolean("Animate", false)
    private val animationSpeed by float("AnimationSpeed", 1.0f, 0.1f..5.0f) { animate }

    private var animationOffset = 0f

    val onRender3D = handler<Render3DEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val renderManager = mc.renderManager ?: return@handler
        val viewerPosX = renderManager.viewerPosX
        val viewerPosY = renderManager.viewerPosY
        val viewerPosZ = renderManager.viewerPosZ
        
        // 计算雾的位置（玩家前方一定距离）
        val yawRad = Math.toRadians(player.rotationYaw.toDouble())
        val fogX = (player.posX - viewerPosX) - sin(yawRad) * fogDistance
        val fogY = (player.posY - viewerPosY) + fogHeight
        val fogZ = (player.posZ - viewerPosZ) + cos(yawRad) * fogDistance
        
        // 更新动画偏移
        if (animate) {
            animationOffset += animationSpeed * 0.01f
            if (animationOffset > Math.PI.toFloat() * 2) {
                animationOffset = 0f
            }
        }
        
        glPushMatrix()
        glTranslated(fogX, fogY.toDouble(), fogZ)
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        glDepthMask(false)
        glDisable(GL_CULL_FACE)
        
        // 设置雾颜色
        glColor4f(
            fogColor.red / 255f,
            fogColor.green / 255f,
            fogColor.blue / 255f,
            fogColor.alpha / 255f * fogDensity
        )
        
        // 绘制球形雾
        drawFogSphere(fogSize)
        
        glEnable(GL_CULL_FACE)
        glDepthMask(true)
        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glPopMatrix()
    }
    
    private fun drawFogSphere(radius: Float) {
        val slices = 16
        val stacks = 16
        
        for (i in 0 until stacks) {
            val lat0 = (Math.PI * (-0.5 + (i.toDouble() / stacks))).toFloat()
            val z0 = sin(lat0.toDouble()).toFloat()
            val zr0 = cos(lat0.toDouble()).toFloat()
            
            val lat1 = (Math.PI * (-0.5 + ((i + 1).toDouble() / stacks))).toFloat()
            val z1 = sin(lat1.toDouble()).toFloat()
            val zr1 = cos(lat1.toDouble()).toFloat()
            
            glBegin(GL_QUAD_STRIP)
            
            for (j in 0..slices) {
                val lng = (2 * Math.PI * (j.toDouble() / slices) + animationOffset.toDouble()).toFloat()
                val x = cos(lng.toDouble()).toFloat()
                val y = sin(lng.toDouble()).toFloat()
                
                // 计算顶点位置，添加一些随机性使雾更自然
                val randomFactor = if (animate) {
                    sin(animationOffset * 2 + j * 0.3f).toDouble().toFloat() * 0.1f
                } else {
                    0f
                }
                
                val adjustedRadius = radius * (1 + randomFactor)
                
                glNormal3f(x * zr0, y * zr0, z0)
                glVertex3f(x * zr0 * adjustedRadius, y * zr0 * adjustedRadius, z0 * adjustedRadius)
                
                glNormal3f(x * zr1, y * zr1, z1)
                glVertex3f(x * zr1 * adjustedRadius, y * zr1 * adjustedRadius, z1 * adjustedRadius)
            }
            
            glEnd()
        }
    }
    
    override fun onEnable() {
        animationOffset = 0f
    }
}