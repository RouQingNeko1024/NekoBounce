/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.Render3DEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.misc.AntiBot.isBot
import io.qzz.nekobounce.features.module.modules.misc.Teams
import io.qzz.nekobounce.utils.attack.EntityUtils.isLookingOnEntities
import io.qzz.nekobounce.utils.attack.EntityUtils.isSelected
import io.qzz.nekobounce.utils.client.EntityLookup
import io.qzz.nekobounce.utils.extensions.*
import io.qzz.nekobounce.utils.render.ColorSettingsInteger
import io.qzz.nekobounce.utils.render.ColorUtils.withAlpha
import io.qzz.nekobounce.utils.render.RenderUtils.drawCone
import io.qzz.nekobounce.utils.render.RenderUtils.drawConesForEntities
import io.qzz.nekobounce.utils.render.RenderUtils.glStateManagerColor
import io.qzz.nekobounce.utils.rotation.RotationUtils.isEntityHeightVisible
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color

object ChineseHat : Module("ChineseHat", Category.RENDER) {

    private val useChineseHatTexture by boolean("UseChineseHatTexture", false)

    private val colorMode by choices("ColorMode", arrayOf("Custom", "DistanceColor"), "Custom")
    private val colors = ColorSettingsInteger(this) { colorMode == "Custom" }.with(0, 160, 255, 150)

    private val playerHeight by float("PlayerHeight", 0.5f, 0.25f..2f)

    private val coneWidth by float("ConeWidth", 0.5f, 0f..2f)
    private val coneHeight by float("ConeHeight", 0.5f, 0.1f..2f)

    private val renderSelf by boolean("RenderSelf", false)

    private val maxRenderDistance by int("MaxRenderDistance", 100, 1..200)

    private val onLook by boolean("OnLook", false)
    private val maxAngleDifference by float("MaxAngleDifference", 90f, 5.0f..90f) { onLook }

    private val bots by boolean("Bots", true)
    private val teams by boolean("Teams", false)
    private val thruBlocks by boolean("ThruBlocks", true)

    private val entityLookup by EntityLookup<EntityLivingBase>()
        .filter { mc.thePlayer.getDistanceSqToEntity(it) <= maxRenderDistance * maxRenderDistance }
        .filter { bots || !isBot(it) }
        .filter { !onLook || isLookingOnEntities(it, maxAngleDifference.toDouble()) }
        .filter { thruBlocks || isEntityHeightVisible(it) }

    val render = handler<Render3DEvent> {
        drawConesForEntities {
            for (entity in entityLookup) {
                val isRenderingSelf =
                    entity == mc.thePlayer && (mc.gameSettings.thirdPersonView != 0 || FreeCam.handleEvents())

                if (!isRenderingSelf || !renderSelf) {
                    if (!isSelected(entity, false)) continue
                }

                if (isRenderingSelf) {
                    FreeCam.restoreOriginalPosition()
                }

                val (x, y, z) = entity.interpolatedPosition(
                    entity.lastTickPos, entity.eyeHeight + playerHeight
                ) - mc.renderManager.renderPos

                val coneWidth = (mc.renderManager.getEntityRenderObject<Entity>(entity)?.shadowSize ?: 0.5F) + coneWidth

                GlStateManager.pushMatrix()
                GlStateManager.translate(x, y, z)

                glStateManagerColor(figureOutColor(entity))

                drawCone(coneWidth, coneHeight, useChineseHatTexture)

                GlStateManager.popMatrix()

                if (isRenderingSelf) {
                    FreeCam.useModifiedPosition()
                }
            }
        }
    }

    private fun figureOutColor(entity: EntityLivingBase): Color {
        val dist = mc.thePlayer.getDistanceSqToEntity(entity).coerceAtMost(255.0).toInt()

        return when {
            entity is EntityPlayer && entity.isClientFriend() -> Color(0, 0, 255)
            teams && Teams.isInYourTeam(entity) -> Color(0, 162, 232)
            colorMode == "Custom" -> colors.color()
            colorMode == "DistanceColor" -> Color(255 - dist, dist, 0)
            else -> Color.WHITE
        }.withAlpha(colors.color().alpha)
    }
}