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
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot.isBot
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.EntityLookup
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawTexturedModalRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawBorderedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.quickDrawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.potion.Potion
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.pow
import kotlin.math.roundToInt

object NameTags3 : Module("NameTags3", Category.RENDER) {
    private val renderSelf by boolean("RenderSelf", false)
    private val health by boolean("Health", true)
    private val healthFromScoreboard by boolean("HealthFromScoreboard", false) { health }
    private val absorption by boolean("Absorption", false) { health || healthBar }
    private val roundedHealth by boolean("RoundedHealth", true) { health }

    private val healthPrefix by boolean("HealthPrefix", false) { health }
    private val healthPrefixText by text("HealthPrefixText", "") { health && healthPrefix }

    private val healthSuffix by boolean("HealthSuffix", true) { health }
    private val healthSuffixText by text("HealthSuffixText", " HP") { health && healthSuffix }

    private val ping by boolean("Ping", false)
    private val healthBar by boolean("Bar", true)
    private val distance by boolean("Distance", false)
    private val armor by boolean("Armor", true)
    private val bot by boolean("Bots", true)
    private val potion by boolean("Potions", true)
    private val clearNames by boolean("ClearNames", false)
    private val fontShadow by boolean("Shadow", true)

    private val scale by float("Scale", 1F, 1F..4F)

    private val maxRenderDistance by int("MaxRenderDistance", 50, 1..200).onChanged { value ->
        maxRenderDistanceSq = value.toDouble().pow(2)
    }

    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val thruBlocks by boolean("ThruBlocks", true)

    private var maxRenderDistanceSq = 0.0
        set(value) {
            field = if (value <= 0.0) maxRenderDistance.toDouble().pow(2.0) else value
        }

    private val inventoryBackground = ResourceLocation("textures/gui/container/inventory.png")
    private val decimalFormat = DecimalFormat("##0.00", DecimalFormatSymbols(Locale.ENGLISH))

    private val entities by EntityLookup<EntityLivingBase>()
        .filter { bot || !isBot(it) }
        .filter { !onLook || isLookingOnEntities(it, maxAngleDifference.toDouble()) }
        .filter { thruBlocks || isEntityHeightVisible(it) }

    val onRender3D = handler<Render3DEvent> {
        if (mc.theWorld == null || mc.thePlayer == null) return@handler

        glPushAttrib(GL_ENABLE_BIT)
        glPushMatrix()

        // Disable lightning and depth test
        glDisable(GL_LIGHTING)
        glDisable(GL_DEPTH_TEST)

        glEnable(GL_LINE_SMOOTH)

        // Enable blend
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        for (entity in entities) {
            val isThirdPersonView = mc.gameSettings.thirdPersonView != 0
            val isRenderingSelf = entity is EntityPlayerSP && (isThirdPersonView || FreeCam.handleEvents())

            // 第一人称不显示自己的Tags，第二/三人称显示
            if (isRenderingSelf && !renderSelf) {
                if (mc.gameSettings.thirdPersonView == 0) continue
            }

            if (!isRenderingSelf) {
                if (!isSelected(entity, false)) continue
            }

            val name = entity.displayName.unformattedText ?: continue

            val distanceSquared = mc.thePlayer.getDistanceSqToEntity(entity)

            // In case user has FreeCam enabled, we restore the position back to normal,
            // so it renders the name-tag at the player's body position instead of the FreeCam position.
            if (isRenderingSelf) {
                FreeCam.restoreOriginalPosition()
            }

            if (distanceSquared <= maxRenderDistanceSq) {
                renderNewNameTag(entity, isRenderingSelf, if (clearNames) ColorUtils.stripColor(name) else name)
            }

            if (isRenderingSelf) {
                FreeCam.useModifiedPosition()
            }
        }

        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)

        glPopMatrix()
        glPopAttrib()

        // Reset color
        glColor4f(1F, 1F, 1F, 1F)
    }

    private fun getTeamColorCode(entity: EntityLivingBase): String {
        if (entity !is EntityPlayer) return "§f"
        
        val world = mc.theWorld ?: return "§f"
        val scoreboard = world.scoreboard ?: return "§f"
        val team = scoreboard.getPlayersTeam(entity.name) ?: return "§f"
        
        // 使用getChatFormat()方法来获取队伍颜色
        val chatFormat = team.chatFormat
        return if (chatFormat != null) {
            chatFormat.toString()
        } else {
            "§f"
        }
    }

    private fun getTeamColorChar(colorCode: String): String {
        // 根据颜色代码返回对应的字母
        return when (colorCode) {
            "§0" -> "B"  // 黑色
            "§1" -> "B"  // 深蓝
            "§2" -> "G"  // 深绿
            "§3" -> "C"  // 青色
            "§4" -> "R"  // 红色
            "§5" -> "P"  // 紫色
            "§6" -> "O"  // 橙色
            "§7" -> "L"  // 灰色
            "§8" -> "G"  // 深灰
            "§9" -> "B"  // 蓝色
            "§a" -> "G"  // 绿色
            "§b" -> "A"  // 天蓝
            "§c" -> "R"  // 亮红
            "§d" -> "P"  // 粉红
            "§e" -> "Y"  // 黄色
            "§f" -> "W"  // 白色
            else -> "T"  // 默认
        }
    }

    private fun renderNewNameTag(entity: EntityLivingBase, isRenderingSelf: Boolean, name: String) {
        val thePlayer = mc.thePlayer ?: return

        // 使用原版字体
        val fontRenderer = Fonts.minecraftFont

        // Push
        glPushMatrix()

        // Translate to player position
        val renderManager = mc.renderManager
        val rotateX = if (mc.gameSettings.thirdPersonView == 2) -1.0f else 1.0f

        val (x, y, z) = entity.interpolatedPosition(entity.lastTickPos) - renderManager.renderPos

        glTranslated(x, y + entity.eyeHeight.toDouble() + 0.55, z)

        glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
        glRotatef(renderManager.playerViewX * rotateX, 1F, 0F, 0F)

        // Disable lightning and depth test
        disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)

        // Enable blend
        enableGlCap(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        // 获取队伍颜色和字母
        val teamColorCode = getTeamColorCode(entity)
        val teamChar = getTeamColorChar(teamColorCode)
        
        // 获取血量
        val healthValue = getHealth(entity, healthFromScoreboard, absorption)
        val healthString = if (roundedHealth) healthValue.roundToInt().toString() else decimalFormat.format(healthValue)
        
        // 获取延迟
        val playerPing = if (entity is EntityPlayer) entity.getPing() else 0
        
        // 构建新格式的文本：
        // 自己显示：[队伍§r][§9User] §f玩家名称 §r[§c血量♥§r][§6延迟§r]
        // 其他人显示：[队伍§r] §f玩家名称 §r[§c血量♥§r][§6延迟§r]
        
        // 修复：使用直接的颜色字符而不是转义序列
        // Minecraft颜色代码：§ = \u00A7
        val sectionChar = '\u00A7'
        
        val teamPart = "[${sectionChar}7$teamChar${sectionChar}r]"  // 使用灰色显示队伍字母
        val userPart = if (isRenderingSelf) "[${sectionChar}9User${sectionChar}r] " else ""
        val namePart = "${sectionChar}f$name${sectionChar}r "
        val healthPart = if (health) "[${sectionChar}c${healthString}♥${sectionChar}r]" else ""
        val pingPart = if (ping && entity is EntityPlayer) "[${sectionChar}6${playerPing}ms${sectionChar}r]" else ""
        
        val text = "$teamPart$userPart$namePart$healthPart$pingPart"

        // Scale
        val playerDistance = thePlayer.getDistanceToEntity(entity)
        val scale = ((playerDistance / 4F).coerceAtLeast(1F) / 150F) * scale

        glScalef(-scale, -scale, scale)

        val width = fontRenderer.getStringWidth(text) * 0.5f
        
        // 先绘制背景（透明）
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)

        val bgColor = Color(0, 0, 0, 0)
        quickDrawRect(
            -width - 2F, -2F, width + 4F, fontRenderer.FONT_HEIGHT + 2F + if (healthBar) 2F else 0F, bgColor.rgb
        )

        if (healthBar) {
            val dist = width + 4F - (-width - 2F)
            quickDrawRect(
                -width - 2F,
                fontRenderer.FONT_HEIGHT + 3F,
                -width - 2F + dist,
                fontRenderer.FONT_HEIGHT + 4F,
                Color(50, 50, 50).rgb
            )
            quickDrawRect(
                -width - 2F,
                fontRenderer.FONT_HEIGHT + 3F,
                -width - 2F + (dist * (getHealth(entity, healthFromScoreboard) / entity.maxHealth).coerceIn(0F, 1F)),
                fontRenderer.FONT_HEIGHT + 4F,
                Color(255, 0, 0).rgb  // 固定红色血条
            )
        }

        glEnable(GL_TEXTURE_2D)
        
        // 绘制文本 - 使用字体渲染器的drawString方法
        fontRenderer.drawString(
            text, -width, 1F, -1, fontShadow
        )

        var foundPotion = false

        if (potion && entity is EntityPlayer) {
            val potions =
                entity.activePotionEffects.map { Potion.potionTypes[it.potionID] }.filter { it.hasStatusIcon() }
            if (potions.isNotEmpty()) {
                foundPotion = true

                color(1.0F, 1.0F, 1.0F, 1.0F)
                disableLighting()
                enableTexture2D()

                val minX = (potions.size * -20) / 2

                glPushMatrix()
                enableRescaleNormal()
                for ((index, potion) in potions.withIndex()) {
                    color(1.0F, 1.0F, 1.0F, 1.0F)
                    mc.textureManager.bindTexture(inventoryBackground)
                    val i1 = potion.statusIconIndex
                    drawTexturedModalRect(minX + index * 20, -22, 0 + i1 % 8 * 18, 198 + i1 / 8 * 18, 18, 18, 0F)
                }
                disableRescaleNormal()
                glPopMatrix()

                enableAlpha()
                disableBlend()
                enableTexture2D()
            }
        }

        if (armor && entity is EntityPlayer) {
            RenderHelper.enableGUIStandardItemLighting()
            for (index in 0..4) {
                val itemStack = entity.getEquipmentInSlot(index) ?: continue

                mc.renderItem.zLevel = -147F
                mc.renderItem.renderItemAndEffectIntoGUI(
                    itemStack, -50 + index * 20, if (potion && foundPotion) -42 else -22
                )
            }
            RenderHelper.disableStandardItemLighting()

            enableAlpha()
            disableBlend()
            enableTexture2D()
        }

        // Reset caps
        resetCaps()

        // Reset color
        resetColor()
        glColor4f(1F, 1F, 1F, 1F)

        // Pop
        glPopMatrix()
    }

    fun shouldRenderNameTags(entity: Entity) =
        handleEvents() && entity is EntityLivingBase && (ESP.handleEvents() && ESP.renderNameTags || isSelected(
            entity,
            false
        ) && (bot || !isBot(entity)))
}