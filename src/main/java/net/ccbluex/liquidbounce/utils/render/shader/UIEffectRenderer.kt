/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11.*

object UIEffectRenderer {


    @JvmStatic
    fun drawTexturedRectWithCustomAlpha(x: Float, y: Float, width: Float, height: Float, image: String, alpha: Float) {
        glPushMatrix()
        val enableBlend = glIsEnabled(GL_BLEND)
        val disableAlpha = !glIsEnabled(GL_ALPHA_TEST)
        if (!enableBlend) glEnable(GL_BLEND)
        if (!disableAlpha) glDisable(GL_ALPHA_TEST)
        GlStateManager.color(1f, 1f, 1f, alpha)
        Minecraft.getMinecraft().textureManager.bindTexture(ResourceLocation("shaders/$image.png"))
        drawModalRectWithCustomSizedTexture(
            x,
            y,
            0F,
            0F,
            width,
            height,
            width,
            height
        )
        if (!enableBlend) glDisable(GL_BLEND)
        if (!disableAlpha) glEnable(GL_ALPHA_TEST)
        GlStateManager.resetColor()
        glPopMatrix()
    }

    @JvmStatic
    fun drawTexturedRect(x: Float, y: Float, width: Float, height: Float, image: String) {
        glPushMatrix()
        val enableBlend = glIsEnabled(GL_BLEND)
        val disableAlpha = !glIsEnabled(GL_ALPHA_TEST)
        if (!enableBlend) glEnable(GL_BLEND)
        if (!disableAlpha) glDisable(GL_ALPHA_TEST)
        Minecraft.getMinecraft().textureManager.bindTexture(ResourceLocation("shaders/$image.png"))
        GlStateManager.color(1f, 1f, 1f, 1f)
        drawModalRectWithCustomSizedTexture(
            x,
            y,
            0F,
            0F,
            width,
            height,
            width,
            height
        )
        if (!enableBlend) glDisable(GL_BLEND)
        if (!disableAlpha) glEnable(GL_ALPHA_TEST)
        glPopMatrix()
    }

    @JvmStatic
    fun drawModalRectWithCustomSizedTexture(
        x: Float,
        y: Float,
        u: Float,
        v: Float,
        width: Float,
        height: Float,
        textureWidth: Float,
        textureHeight: Float
    ) {
        val f = 1.0f / textureWidth
        val f1 = 1.0f / textureHeight
        val tessellator = Tessellator.getInstance()
        val worldrenderer = tessellator.worldRenderer
        worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX)
        worldrenderer.pos(x.toDouble(), (y + height).toDouble(), 0.0).tex(
            (u * f).toDouble(),
            ((v + height.toFloat()) * f1).toDouble()
        ).endVertex()
        worldrenderer.pos((x + width).toDouble(), (y + height).toDouble(), 0.0).tex(
            ((u + width) * f).toDouble(),
            ((v + height) * f1).toDouble()
        ).endVertex()
        worldrenderer.pos((x + width).toDouble(), y.toDouble(), 0.0).tex(
            ((u + width) * f).toDouble(),
            (v * f1).toDouble()
        ).endVertex()
        worldrenderer.pos(x.toDouble(), y.toDouble(), 0.0).tex((u * f).toDouble(), (v * f1).toDouble()).endVertex()
        tessellator.draw()
    }
}
