/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.draw2D
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.WorldToScreen.worldToScreen
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemArmor
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Matrix4f
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import java.nio.FloatBuffer
import java.text.DecimalFormat

object FDPESP : Module("FDPESP", Category.SKID) {

    // 模式选择
    val mode by choices("Mode", arrayOf("Box", "OtherBox", "2D", "Real2D", "CSGO", "CSGO-Old", "Outline"), "Outline")

    // Outline 宽度
    val outlineWidth by float("Outline-Width", 1.36f, 0.5f..5f)

    // CSGO 模式选项
    val csgoDirectLine by boolean("CSGO-DirectLine", false)
    val csgoShowHealth by boolean("CSGO-ShowHealth", true)
    val csgoShowHeldItem by boolean("CSGO-ShowHeldItem", true)
    val csgoShowName by boolean("CSGO-ShowName", true)
    val csgoOldWidth by float("CSGOOld-Width", 2f, 0.5f..5f)

    // 颜色模式
    val colorMode by choices("ColorMode", arrayOf("Name", "Armor", "OFF"), "Name")

    // 静态颜色
    val colorRed by int("R", 255, 0..255)
    val colorGreen by int("G", 255, 0..255)
    val colorBlue by int("B", 255, 0..255)
    val colorClient by boolean("ClientColor", false)

    // 受伤颜色
    val damageColor by boolean("ColorOnDamage", true)
    val damageRed by int("DamageR", 255, 0..255)
    val damageGreen by int("DamageG", 0, 0..255)
    val damageBlue by int("DamageB", 0, 0..255)

    private val decimalFormat = DecimalFormat("0.0")

    // 预定义颜色表
    private val hexColors = intArrayOf(
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA,
        0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF,
        0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    )

    // ========== 事件处理器 ==========

    val onRender3D = handler<Render3DEvent> { event ->
        val currentMode = mode.lowercase()

        // 获取投影矩阵和模型视图矩阵
        val mvFloat = FloatBuffer.allocate(16)
        val projFloat = FloatBuffer.allocate(16)
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, mvFloat)
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projFloat)
        val mvMatrix = Matrix4f()
        val projMatrix = Matrix4f()
        mvFloat.rewind()
        projFloat.rewind()
        mvMatrix.load(mvFloat)
        projMatrix.load(projFloat)

        // 2D 模式需要切换渲染环境
        val need2dTranslate = currentMode in setOf("csgo", "real2d", "csgo-old")
        if (need2dTranslate) {
            GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPushMatrix()
            GL11.glLoadIdentity()
            GL11.glOrtho(0.0, mc.displayWidth.toDouble(), mc.displayHeight.toDouble(), 0.0, -1.0, 1.0)
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPushMatrix()
            GL11.glLoadIdentity()
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GlStateManager.enableTexture2D()
            GlStateManager.depthMask(true)
            GL11.glLineWidth(1.0f)
        }

        // 遍历实体
        for (entity in mc.theWorld.loadedEntityList) {
            if (isSelected(entity, true)) {
                val entityLiving = entity as EntityLivingBase
                val color = getColor(entityLiving)

                when (currentMode) {
                    "box", "otherbox" -> {
                        drawEntityBox(entity, color, currentMode != "otherbox")
                    }

                    "outline" -> {
                        GL11.glLineWidth(outlineWidth)
                        drawEntityBox(entity, color, true)
                    }

                    "2d" -> {
                        val renderManager = mc.renderManager
                        val timer = mc.timer
                        val posX = entityLiving.lastTickPosX + (entityLiving.posX - entityLiving.lastTickPosX) * timer.renderPartialTicks - renderManager.renderPosX
                        val posY = entityLiving.lastTickPosY + (entityLiving.posY - entityLiving.lastTickPosY) * timer.renderPartialTicks - renderManager.renderPosY
                        val posZ = entityLiving.lastTickPosZ + (entityLiving.posZ - entityLiving.lastTickPosZ) * timer.renderPartialTicks - renderManager.renderPosZ
                        draw2D(entityLiving, posX, posY, posZ, color.rgb, Color.BLACK.rgb)
                    }

                    "csgo", "real2d", "csgo-old" -> {
                        val renderManager = mc.renderManager
                        val timer = mc.timer
                        val bb = entityLiving.entityBoundingBox
                            .offset(-entityLiving.posX, -entityLiving.posY, -entityLiving.posZ)
                            .offset(
                                entityLiving.lastTickPosX + (entityLiving.posX - entityLiving.lastTickPosX) * timer.renderPartialTicks,
                                entityLiving.lastTickPosY + (entityLiving.posY - entityLiving.lastTickPosY) * timer.renderPartialTicks,
                                entityLiving.lastTickPosZ + (entityLiving.posZ - entityLiving.lastTickPosZ) * timer.renderPartialTicks
                            )
                            .offset(-renderManager.renderPosX, -renderManager.renderPosY, -renderManager.renderPosZ)

                        val boxVertices = arrayOf(
                            doubleArrayOf(bb.minX, bb.minY, bb.minZ),
                            doubleArrayOf(bb.minX, bb.maxY, bb.minZ),
                            doubleArrayOf(bb.maxX, bb.maxY, bb.minZ),
                            doubleArrayOf(bb.maxX, bb.minY, bb.minZ),
                            doubleArrayOf(bb.minX, bb.minY, bb.maxZ),
                            doubleArrayOf(bb.minX, bb.maxY, bb.maxZ),
                            doubleArrayOf(bb.maxX, bb.maxY, bb.maxZ),
                            doubleArrayOf(bb.maxX, bb.minY, bb.maxZ)
                        )

                        var minX = mc.displayWidth.toFloat()
                        var minY = mc.displayHeight.toFloat()
                        var maxX = 0f
                        var maxY = 0f

                        for (boxVertex in boxVertices) {
                            val screenPos = worldToScreen(
                                Vector3f(
                                    boxVertex[0].toFloat(), boxVertex[1].toFloat(), boxVertex[2].toFloat()
                                ), mvMatrix, projMatrix, mc.displayWidth, mc.displayHeight
                            ) ?: continue
                            minX = minOf(screenPos.x, minX)
                            minY = minOf(screenPos.y, minY)
                            maxX = maxOf(screenPos.x, maxX)
                            maxY = maxOf(screenPos.y, maxY)
                        }

                        // 如果完全在屏幕外则跳过
                        if (minX == mc.displayWidth.toFloat() || minY == mc.displayHeight.toFloat() || maxX == 0f || maxY == 0f)
                            continue

                        when (currentMode) {
                            "csgo" -> {
                                RenderUtils.glColor(color)
                                if (!csgoDirectLine) {
                                    val distX = (maxX - minX) / 3.0f
                                    val distY = (maxY - minY) / 3.0f
                                    GL11.glBegin(GL11.GL_LINE_STRIP)
                                    GL11.glVertex2f(minX, minY + distY)
                                    GL11.glVertex2f(minX, minY)
                                    GL11.glVertex2f(minX + distX, minY)
                                    GL11.glEnd()
                                    GL11.glBegin(GL11.GL_LINE_STRIP)
                                    GL11.glVertex2f(minX, maxY - distY)
                                    GL11.glVertex2f(minX, maxY)
                                    GL11.glVertex2f(minX + distX, maxY)
                                    GL11.glEnd()
                                    GL11.glBegin(GL11.GL_LINE_STRIP)
                                    GL11.glVertex2f(maxX - distX, minY)
                                    GL11.glVertex2f(maxX, minY)
                                    GL11.glVertex2f(maxX, minY + distY)
                                    GL11.glEnd()
                                    GL11.glBegin(GL11.GL_LINE_STRIP)
                                    GL11.glVertex2f(maxX - distX, maxY)
                                    GL11.glVertex2f(maxX, maxY)
                                    GL11.glVertex2f(maxX, maxY - distY)
                                    GL11.glEnd()
                                } else {
                                    GL11.glBegin(GL11.GL_LINE_LOOP)
                                    GL11.glVertex2f(minX, minY)
                                    GL11.glVertex2f(minX, maxY)
                                    GL11.glVertex2f(maxX, maxY)
                                    GL11.glVertex2f(maxX, minY)
                                    GL11.glEnd()
                                }

                                // 血量条
                                if (csgoShowHealth) {
                                    val barHeight = (maxY - minY) * (1.0f - entityLiving.health / entityLiving.maxHealth)
                                    GL11.glColor4f(0.1f, 1.0f, 0.1f, 1.0f)
                                    GL11.glBegin(GL11.GL_QUADS)
                                    GL11.glVertex2f(maxX + 2.0f, minY + barHeight)
                                    GL11.glVertex2f(maxX + 2.0f, maxY)
                                    GL11.glVertex2f(maxX + 3.0f, maxY)
                                    GL11.glVertex2f(maxX + 3.0f, minY + barHeight)
                                    GL11.glEnd()
                                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
                                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                                    GL11.glEnable(GL11.GL_DEPTH_TEST)

                                    // 修复类型不匹配：将 Float 转换为 Int
                                    val textX = (maxX + 4.0f).toInt()
                                    val textY = (minY + barHeight).toInt()
                                    mc.fontRendererObj.drawString(decimalFormat.format(entityLiving.health) + "§c❤", textX, textY, getHealthColor(entityLiving.health, entityLiving.maxHealth).rgb)

                                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
                                }

                                // 手持物品
                                if (csgoShowHeldItem && entityLiving.heldItem?.displayName != null) {
                                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                                    GL11.glEnable(GL11.GL_DEPTH_TEST)
                                    val itemName = entityLiving.heldItem.displayName
                                    val x = (minX + (maxX - minX) / 2.0f - mc.fontRendererObj.getStringWidth(itemName) / 2.0f).toInt()
                                    mc.fontRendererObj.drawString(itemName, x, (maxY + 2.0f).toInt(), -1)
                                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                                }

                                // 名字
                                if (csgoShowName) {
                                    GL11.glEnable(GL11.GL_TEXTURE_2D)
                                    GL11.glEnable(GL11.GL_DEPTH_TEST)
                                    val name = entityLiving.displayName.formattedText
                                    val x = (minX + (maxX - minX) / 2.0f - mc.fontRendererObj.getStringWidth(name) / 2.0f).toInt()
                                    mc.fontRendererObj.drawString(name, x, (minY - 12.0f).toInt(), -1)
                                    GL11.glDisable(GL11.GL_TEXTURE_2D)
                                    GL11.glDisable(GL11.GL_DEPTH_TEST)
                                }
                            }

                            "real2d" -> {
                                drawRect(minX - 1, minY - 1, minX, maxY, color)
                                drawRect(maxX, minY - 1, maxX + 1, maxY + 1, color)
                                drawRect(minX - 1, maxY, maxX, maxY + 1, color)
                                drawRect(minX - 1, minY - 1, maxX, minY, color)
                            }

                            "csgo-old" -> {
                                val width = csgoOldWidth * ((maxY - minY) / 50)
                                drawRect(minX - width, minY - width, minX, maxY, color)
                                drawRect(maxX, minY - width, maxX + width, maxY + width, color)
                                drawRect(minX - width, maxY, maxX, maxY + width, color)
                                drawRect(minX - width, minY - width, maxX, minY, color)

                                // 血量条
                                val hpSize = (maxY + width - minY) * (entityLiving.health / entityLiving.maxHealth)
                                drawRect(minX - width * 3, minY - width, minX - width * 2, maxY + width, Color.GRAY)
                                drawRect(minX - width * 3, maxY - hpSize, minX - width * 2, maxY + width, getHealthColor(entityLiving.health, entityLiving.maxHealth))
                            }
                        }
                    }
                }
            }
        }

        if (need2dTranslate) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glMatrixMode(GL11.GL_PROJECTION)
            GL11.glPopMatrix()
            GL11.glMatrixMode(GL11.GL_MODELVIEW)
            GL11.glPopMatrix()
            GL11.glPopAttrib()
        }
    }

    // 2D 渲染事件（本模块无需处理，保留空实现）
    val onRender2D = handler<Render2DEvent> {}

    override val tag: String
        get() = mode

    // ========== 颜色逻辑 ==========

    private fun getColor(entity: EntityLivingBase): Color {
        if (damageColor && entity.hurtTime > 0) {
            return Color(damageRed, damageGreen, damageBlue)
        }

        return when (colorMode) {
            "Name" -> extractColorFromName(entity) ?: fallbackColor()
            "Armor" -> extractColorFromArmor(entity) ?: fallbackColor()
            else -> fallbackColor()
        }
    }

    private fun extractColorFromName(entity: EntityLivingBase): Color? {
        val formatted = entity.displayName.formattedText
        val matcher = "§[0-9a-f]".toRegex().find(formatted)
        val code = matcher?.value?.get(1)
        val index = code?.let { "0123456789abcdef".indexOf(it) }
        return if (index != null && index in 0..15) {
            Color(hexColors[index])
        } else null
    }

    private fun extractColorFromArmor(entity: EntityLivingBase): Color? {
        if (entity is EntityPlayer) {
            val helmet = entity.inventory.armorInventory[3]
            if (helmet?.item is ItemArmor) {
                val armor = helmet.item as ItemArmor
                return Color(armor.getColor(helmet))
            }
        }
        return null
    }

    private fun fallbackColor(): Color {
        return if (colorClient) {
            Color.WHITE
        } else {
            Color(colorRed, colorGreen, colorBlue)
        }
    }

    private fun getHealthColor(health: Float, maxHealth: Float): Color {
        val percent = health / maxHealth
        val r = (255 * (1 - percent)).toInt().coerceIn(0, 255)
        val g = (255 * percent).toInt().coerceIn(0, 255)
        return Color(r, g, 0)
    }
}