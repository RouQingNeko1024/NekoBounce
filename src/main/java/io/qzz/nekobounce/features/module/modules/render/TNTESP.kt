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
import io.qzz.nekobounce.utils.extensions.interpolatedPosition
import io.qzz.nekobounce.utils.extensions.prevPos
import io.qzz.nekobounce.utils.render.ColorSettingsInteger
import io.qzz.nekobounce.utils.render.RenderUtils.drawDome
import io.qzz.nekobounce.utils.render.RenderUtils.drawEntityBox
import net.minecraft.entity.item.EntityTNTPrimed
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object TNTESP : Module("TNTESP", Category.RENDER, spacedName = "TNT ESP") {

    private val dangerZoneDome by boolean("DangerZoneDome", false)
    private val mode by choices("Mode", arrayOf("Lines", "Triangles", "Filled"), "Lines") { dangerZoneDome }
    private val lineWidth by float("LineWidth", 1F, 0.5F..5F) { mode == "Lines" }
    private val colors = ColorSettingsInteger(this, "Dome") { dangerZoneDome }

    private val renderModes = mapOf("Lines" to GL_LINES, "Triangles" to GL_TRIANGLES, "Filled" to GL_QUADS)

    val onRender3D = handler<Render3DEvent> {
        val renderMode = renderModes[mode] ?: return@handler
        val color = colors.color()

        val width = lineWidth.takeIf { mode == "Lines" }

        mc.theWorld.loadedEntityList.forEach {
            if (it !is EntityTNTPrimed) return@forEach

            if (dangerZoneDome) {
                drawDome(it.interpolatedPosition(it.prevPos), 8.0, 8.0, width, color, renderMode)
            }

            drawEntityBox(it, Color.RED, false)
        }
    }
}