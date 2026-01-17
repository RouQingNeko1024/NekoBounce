/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.client.renderer.culling.Frustum
import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color

object GlowESP : Module("GlowESP", Category.RENDER) {

    private val radius by float("Radius", 2f, 1f..30f)
    private val exposure by float("Exposure", 2.2f, 1f..3.5f)
    private val seperate by boolean("Seperate Texture", false)

    private val colorRed by int("Color-Red", 255, 0..255)
    private val colorGreen by int("Color-Green", 255, 0..255)
    private val colorBlue by int("Color-Blue", 255, 0..255)
    private val players by boolean("Players", false)
    private val animals by boolean("Animals", false)
    private val mobs by boolean("Mobs", false)

    private val frustum = Frustum()
    private val entities = mutableListOf<Entity>()

    private fun isInView(ent: Entity): Boolean {
        val renderViewEntity = mc.renderViewEntity ?: return false
        frustum.setPosition(renderViewEntity.posX, renderViewEntity.posY, renderViewEntity.posZ)
        return frustum.isBoundingBoxInFrustum(ent.entityBoundingBox) || ent.ignoreFrustumCheck
    }

    private fun collectEntities() {
        entities.clear()
        val world = mc.theWorld ?: return
        
        for (entity in world.loadedEntityList) {
            if (!isInView(entity)) continue
            if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue
            
            when {
                entity is EntityAnimal && animals -> entities.add(entity)
                entity is EntityPlayer && players -> entities.add(entity)
                entity is EntityMob && mobs -> entities.add(entity)
            }
        }
    }

    private fun renderEntityGlow(entity: Entity, partialTicks: Float) {
        val posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks

        val x = posX - mc.renderManager.viewerPosX
        val y = posY - mc.renderManager.viewerPosY
        val posZ = z - mc.renderManager.viewerPosZ

        // 绘制发光效果
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, posZ)
        
        // 根据实体类型选择颜色
        val baseColor = when {
            entity is EntityPlayer -> Color(colorRed, colorGreen, colorBlue, (200 * exposure).toInt().coerceAtMost(255))
            entity is EntityAnimal -> Color(0, 255, 0, (180 * exposure).toInt().coerceAtMost(255))
            entity is EntityMob -> Color(255, 0, 0, (180 * exposure).toInt().coerceAtMost(255))
            else -> Color(colorRed, colorGreen, colorBlue, (150 * exposure).toInt().coerceAtMost(255))
        }
        
        // 绘制多层轮廓实现发光效果
        for (i in 1..radius.toInt()) {
            val scale = 1.0f + i * 0.02f
            val alpha = (100 - i * 5).coerceAtLeast(10)
            val layerColor = Color(baseColor.red, baseColor.green, baseColor.blue, alpha)
            
            GlStateManager.pushMatrix()
            GlStateManager.scale(scale, scale, scale)
            RenderUtils.drawEntityBox(entity, layerColor, false)
            GlStateManager.popMatrix()
        }
        
        // 绘制实体本身（较暗的轮廓）
        val entityColor = Color(baseColor.red, baseColor.green, baseColor.blue, 100)
        RenderUtils.drawEntityBox(entity, entityColor, true)
        
        GlStateManager.popMatrix()
    }

    val onRender3D = handler<Render3DEvent> { event ->
        collectEntities()
        
        if (entities.isEmpty()) return@handler
        
        // 保存当前状态
        GlStateManager.pushMatrix()
        GlStateManager.disableTexture2D()
        GlStateManager.enableBlend()
        GlStateManager.disableAlpha()
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)
        GlStateManager.shadeModel(GL11.GL_SMOOTH)
        GlStateManager.disableDepth()
        GlStateManager.enableCull()
        GlStateManager.depthMask(false)
        
        // 渲染所有实体的发光效果
        for (entity in entities) {
            renderEntityGlow(entity, event.partialTicks)
        }
        
        // 恢复状态
        GlStateManager.depthMask(true)
        GlStateManager.enableDepth()
        GlStateManager.shadeModel(GL11.GL_FLAT)
        GlStateManager.disableBlend()
        GlStateManager.enableAlpha()
        GlStateManager.enableTexture2D()
        GlStateManager.popMatrix()
    }

    val onRender2D = handler<Render2DEvent> {
        // 在2D中不需要做特殊处理，所有发光效果都在3D中渲染
    }
    
    override val tag: String?
        get() {
            val count = mutableListOf<String>()
            if (players) count.add("P")
            if (animals) count.add("A")
            if (mobs) count.add("M")
            return if (count.isNotEmpty()) count.joinToString(",") else null
        }
}