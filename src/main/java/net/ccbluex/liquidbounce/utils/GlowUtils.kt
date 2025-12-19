package net.ccbluex.liquidbounce.utils

import com.jhlabs.image.GaussianFilter
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.TextureUtil
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.image.BufferedImage
import java.util.HashMap

object GlowUtils {
    private val shadowCache = HashMap<GlowCacheKey, Int>()

    fun drawGlow(x: Float, y: Float, width: Float, height: Float, blurRadius: Int, color: Color) {
        if (width <= 0 || height <= 0 || blurRadius <= 0) return

        val safeRadius = blurRadius.coerceIn(1, 100)

        val contentWidth = width
        val contentHeight = height

        val texWidth = (contentWidth + safeRadius * 2).toInt()
        val texHeight = (contentHeight + safeRadius * 2).toInt()

        val key = GlowCacheKey(texWidth, texHeight, safeRadius)

        GL11.glPushMatrix()
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.01f)

        val texId = if (shadowCache.containsKey(key)) {
            shadowCache[key]!!
        } else {
            val original = BufferedImage(texWidth, texHeight, BufferedImage.TYPE_INT_ARGB_PRE)
            val g = original.graphics

            g.color = Color.WHITE
            g.fillRect(safeRadius, safeRadius, width.toInt(), height.toInt())
            g.dispose()

            val op = GaussianFilter(safeRadius.toFloat())
            val blurred = op.filter(original, null)

            val newTexId = TextureUtil.glGenTextures()
            TextureUtil.uploadTextureImageAllocate(newTexId, blurred, true, false)

            shadowCache[key] = newTexId
            newTexId
        }

        GlStateManager.bindTexture(texId)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glEnable(GL11.GL_BLEND)
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO)

        val red = color.red / 255f
        val green = color.green / 255f
        val blue = color.blue / 255f
        val alpha = color.alpha / 255f
        GlStateManager.color(red, green, blue, alpha)

        val renderX = x - safeRadius
        val renderY = y - safeRadius
        val renderW = width + safeRadius * 2
        val renderH = height + safeRadius * 2

        GL11.glBegin(GL11.GL_QUADS)
        GL11.glTexCoord2f(0f, 0f); GL11.glVertex2f(renderX, renderY)
        GL11.glTexCoord2f(0f, 1f); GL11.glVertex2f(renderX, renderY + renderH)
        GL11.glTexCoord2f(1f, 1f); GL11.glVertex2f(renderX + renderW, renderY + renderH)
        GL11.glTexCoord2f(1f, 0f); GL11.glVertex2f(renderX + renderW, renderY)
        GL11.glEnd()
        GlStateManager.disableBlend()
        GlStateManager.resetColor()
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glPopMatrix()
    }
    fun clearCache() {
        shadowCache.values.forEach {
            GlStateManager.deleteTexture(it)
        }
        shadowCache.clear()
    }
}

data class GlowCacheKey(val width: Int, val height: Int, val radius: Int)