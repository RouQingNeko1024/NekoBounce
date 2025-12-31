/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.NekoBounce.CLIENT_NAME
import io.qzz.nekobounce.NekoBounce.hud
import io.qzz.nekobounce.event.*
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.ui.client.hud.designer.GuiHudDesigner
import io.qzz.nekobounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import io.qzz.nekobounce.utils.AnimationUtils_LiquidBouncePlusPlus
import io.qzz.nekobounce.utils.render.ColorSettingsFloat
import io.qzz.nekobounce.utils.render.ColorSettingsInteger
import io.qzz.nekobounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiChat
import net.minecraft.util.ResourceLocation

object HUD : Module("HUD", Category.RENDER, gameDetecting = false, defaultState = true, defaultHidden = true) {
    val animHotbarValue by boolean("AnimatedHotbar", true)
    val blackHotbarValue by boolean("BlackHotbar", true)

    private var hotBarX = 0F

    val inventoryParticle by boolean("InventoryParticle", false)
    private val blur by boolean("Blur", false)
    private val fontChat by boolean("FontChat", false)

    val onRender2D = handler<Render2DEvent> {
        if (mc.currentScreen is GuiHudDesigner)
            return@handler

        hud.render(false)
    }

    val onUpdate = handler<UpdateEvent> {
        hud.update()
    }

    val onKey = handler<KeyEvent> { event ->
        hud.handleKey('a', event.key)
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
        if (state && blur && !mc.entityRenderer.isShaderActive && event.guiScreen != null &&
            !(event.guiScreen is GuiChat || event.guiScreen is GuiHudDesigner)
        ) mc.entityRenderer.loadShader(
            ResourceLocation(CLIENT_NAME.lowercase() + "/blur.json")
        ) else if (mc.entityRenderer.shaderGroup != null &&
            "liquidbounce/blur.json" in mc.entityRenderer.shaderGroup.shaderGroupName
        ) mc.entityRenderer.stopUseShader()
    }

    fun shouldModifyChatFont() = handleEvents() && fontChat

    fun getAnimPos(pos: Float): Float {
        if (state && animHotbarValue) hotBarX = AnimationUtils_LiquidBouncePlusPlus.animate(pos, hotBarX, 0.02F * RenderUtils.deltaTime.toFloat())
        else hotBarX = pos

        return hotBarX
    }
}