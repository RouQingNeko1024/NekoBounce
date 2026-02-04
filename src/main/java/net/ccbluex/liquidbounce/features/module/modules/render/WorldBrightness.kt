/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.entity.RenderManager
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*

object WorldBrightness : Module("WorldBrightness", Category.RENDER) {
    
    private val brightness by float("Brightness", 1.0f, 0.0f..2.0f)
    private val timeLock by boolean("TimeLock", true)
    private val lockedTime by int("LockedTime", 6000, 0..24000) { timeLock }
    
    private var originalWorldTime: Long = 0
    private var worldTimeModified = false
    
    // 保存原始亮度值以便恢复
    private var originalBrightness: Float = 0f
    
    override fun onEnable() {
        // 保存原始世界时间
        originalWorldTime = mc.theWorld?.worldTime ?: 0
        worldTimeModified = false
        
        // 保存原始亮度
        originalBrightness = mc.gameSettings.gammaSetting
    }
    
    override fun onDisable() {
        // 恢复原始亮度
        mc.gameSettings.gammaSetting = originalBrightness
        
        // 恢复原始时间
        if (worldTimeModified) {
            mc.theWorld?.worldTime = originalWorldTime
            worldTimeModified = false
        }
    }
    
    // 锁定游戏时间
    val onWorldTick = handler<PacketEvent> { event ->
        if (!state) return@handler
        
        if (timeLock && event.eventType == EventState.RECEIVE) {
            // 锁定世界时间
            mc.theWorld?.worldTime = lockedTime.toLong()
            worldTimeModified = true
        }
    }
    
    // 在渲染时应用自定义光照
    val onRender3D = handler<Render3DEvent> {
        if (!state) return@handler
        
        // 设置亮度（gamma调整）
        mc.gameSettings.gammaSetting = brightness
        
        // 锁定时间
        if (timeLock) {
            mc.theWorld?.worldTime = lockedTime.toLong()
            worldTimeModified = true
        }
        
        // 应用自定义光照设置
        applyCustomLighting()
    }
    
    // 应用自定义光照
    private fun applyCustomLighting() {
        // 设置环境光强度
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
        GlStateManager.enableTexture2D()
        GlStateManager.shadeModel(GL_SMOOTH)
        
        // 使用自定义的亮度值来影响渲染
        // 这里我们通过修改OpenGL的状态来影响光照计算
        val lightValue = brightness * 0.5f + 0.5f // 将0-2范围映射到0.5-1.5
        
        // 修改环境光
        GlStateManager.color(lightValue, lightValue, lightValue, 1.0f)
        
        // 设置材质环境光
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit)
        val lightMapTextureCoords = OpenGlHelper.lastBrightnessX.toFloat() / 240.0f
        val skyLightTextureCoords = OpenGlHelper.lastBrightnessY.toFloat() / 240.0f
        
        // 应用自定义光照映射
        val customLightMap = lightValue * 240.0f
        val customSkyLight = lightValue * 240.0f
        
        GlStateManager.color(customLightMap / 240.0f, customSkyLight / 240.0f, 0.0f, 1.0f)
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit)
    }
    
    // 实体渲染处理
    val onEntityRender = handler<Render3DEvent> {
        if (!state) return@handler
        
        // 对所有实体应用相同的亮度
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityPlayer && entity != mc.thePlayer) {
                // 设置实体的光照值
                val renderManager = mc.renderManager as? RenderManager
                renderManager?.renderEntitySimple(entity, it.partialTicks)?.let {
                    // 应用自定义光照到实体
                    GlStateManager.pushMatrix()
                    GlStateManager.color(brightness, brightness, brightness, 1.0f)
                    GlStateManager.popMatrix()
                }
            }
        }
    }
    
    override val tag: String
        get() = "%.1f".format(brightness)
}