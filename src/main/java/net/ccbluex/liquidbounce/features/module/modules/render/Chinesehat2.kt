package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object Chinesehat2 : Module("Chinesehat2", Category.RENDER) {

    private val colorMode by choices("Color", arrayOf("Rainbow", "Purple", "Astolfo", "Cherry", "Matrix", "Custom", "Blend"), "Rainbow")
    private val renderInFirstPerson by boolean("FirstPerson", false)
    private val target by boolean("Target", false)
    private val onlySelf by boolean("OnlySelf", false)
    private val size by float("Size", 0.5f, 0.0f..2.0f)
    private val points by int("Points", 30, 3..180)
    private val offSet by int("Offset", 2000, 0..5000)
    private val color by color("Color", Color(255, 255, 255))
    private val secondColor by color("SecondColor", Color(0, 0, 0))
    private val thirdColor by color("ThirdColor", Color(0, 0, 0))

    private val pointsCache = Array(181) { DoubleArray(2) }
    private var lastPoints = 0
    private var lastSize = 0.0
    private var yaw = 0f
    private var prevYaw = 0f
    private var pitch = 0f
    private var prevPitch = 0f

    private val gradient = arrayOf(
        Color(255, 150, 255), Color(255, 132, 199), Color(211, 101, 187),
        Color(160, 80, 158), Color(120, 63, 160), Color(123, 65, 168),
        Color(104, 52, 152), Color(142, 74, 175), Color(160, 83, 179),
        Color(255, 110, 189), Color(255, 150, 255)
    )

    private val cherry = arrayOf(
        Color(35, 255, 145), Color(35, 255, 145), Color(35, 255, 145),
        Color(35, 255, 145), Color(35, 255, 145), Color(155, 155, 155),
        Color(255, 50, 130), Color(255, 50, 130), Color(255, 50, 130),
        Color(255, 50, 130), Color(255, 50, 130), Color(200, 200, 200)
    )

    private val rainbow = arrayOf(
        Color(30, 250, 215), Color(0, 200, 255), Color(50, 100, 255),
        Color(100, 50, 255), Color(255, 50, 240), Color(255, 0, 0),
        Color(255, 150, 0), Color(255, 255, 0), Color(0, 255, 0),
        Color(80, 240, 155)
    )

    private val astolfo = arrayOf(
        Color(252, 106, 140), Color(252, 106, 213), Color(218, 106, 252),
        Color(145, 106, 252), Color(106, 140, 252), Color(106, 213, 252),
        Color(106, 213, 252), Color(106, 140, 252), Color(145, 106, 252),
        Color(218, 106, 252), Color(252, 106, 213), Color(252, 106, 140)
    )

    private val onMotion = handler<MotionEvent> { event ->
        if (event.eventState == EventState.PRE) {
            prevYaw = yaw
            prevPitch = pitch
            yaw = mc.thePlayer.rotationYaw
            pitch = mc.thePlayer.rotationPitch
        }
    }

    private val onRender3D = handler<Render3DEvent> { event ->
        if (lastSize != size.toDouble() || lastPoints != points) {
            lastSize = size.toDouble()
            lastPoints = points
            genPoints(lastPoints, lastSize)
        }

        if (onlySelf) {
            // 仅在自己身上显示
            val thePlayer = mc.thePlayer
            if (thePlayer != null) {
                drawHat(event, thePlayer)
            }
        } else {
            // 原有逻辑
            for (entity in mc.theWorld.playerEntities) {
                if (entity == mc.thePlayer && mc.gameSettings.thirdPersonView != 0) {
                    drawHat(event, entity)
                }
                if (target && entity == KillAura.target) {
                    drawHat(event, entity)
                }
            }
        }
    }

    private fun drawHat(event: Render3DEvent, entity: EntityLivingBase) {
        val isPlayerSP = entity == mc.thePlayer
        if (mc.gameSettings.thirdPersonView == 0 && isPlayerSP && !renderInFirstPerson) {
            return
        }

        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glDepthMask(false)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0)

        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - mc.renderManager.renderPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - mc.renderManager.renderPosY
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - mc.renderManager.renderPosZ

        val colorModeArray = when (colorMode) {
            "Purple" -> gradient
            "Astolfo" -> astolfo
            "Cherry" -> cherry
            "Matrix" -> Array(12) { Color((0..255).random(), (0..255).random(), (0..255).random()) }
            "Custom" -> Array(12) { 
                if (it % 3 == 0) color else if (it % 3 == 1) darkenColor(color, 0.7f) else darkenColor(color, 0.5f)
            }
            "Blend" -> arrayOf(
                darkenColor(color, 0.5f), color, color, color, darkenColor(color, 0.5f),
                darkenColor(secondColor, 0.5f), secondColor, secondColor, secondColor, darkenColor(secondColor, 0.5f),
                darkenColor(thirdColor, 0.5f), thirdColor, thirdColor, thirdColor, darkenColor(thirdColor, 0.5f)
            )
            else -> rainbow
        }

        val colors = Array(181) { i ->
            if (colorMode == "Rainbow") {
                fadeBetween(colorModeArray, 6000.0, i * (6000.0 / points))
            } else {
                fadeBetween(colorModeArray, offSet.toDouble(), i * (offSet.toDouble() / points))
            }
        }

        GL11.glPushMatrix()
        GL11.glTranslated(x, y + 1.9, z)

        if (entity.isSneaking) {
            GL11.glTranslated(0.0, -0.2, 0.0)
        }

        val interpolatedYaw = prevYaw + (yaw - prevYaw) * event.partialTicks
        val interpolatedPitch = prevPitch + (pitch - prevPitch) * event.partialTicks
        
        GL11.glRotatef(interpolatedYaw, 0f, -1f, 0f)
        GL11.glRotatef(interpolatedPitch / 3f, 1f, 0f, 0f)
        GL11.glTranslated(0.0, 0.0, interpolatedPitch / 270.0)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glLineWidth(2f)

        GL11.glBegin(GL11.GL_LINE_LOOP)
        drawCircle(points, colors, 255)
        GL11.glEnd()

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_DONT_CARE)

        GL11.glBegin(GL11.GL_TRIANGLE_FAN)
        GL11.glVertex3d(0.0, size.toDouble() / 2.0, 0.0)
        drawCircle(points, colors, 85)
        GL11.glEnd()

        GL11.glPopMatrix()

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDepthMask(true)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
    }

    private fun drawCircle(points: Int, colors: Array<Color>, alpha: Int) {
        for (i in 0 until points) {
            val point = pointsCache[i]
            val clr = colors[i]
            GL11.glColor4f(
                clr.red / 255f,
                clr.green / 255f,
                clr.blue / 255f,
                alpha / 255f
            )
            GL11.glVertex3d(point[0], 0.0, point[1])
        }
    }

    private fun genPoints(points: Int, size: Double) {
        for (i in 0 until points) {
            val angle = i * Math.PI * 2.0 / points
            val cos = size * cos(angle)
            val sin = size * sin(angle)
            pointsCache[i][0] = cos
            pointsCache[i][1] = sin
        }
    }

    private fun fadeBetween(table: Array<Color>, speed: Double, offset: Double): Color {
        val progress = ((System.currentTimeMillis() + offset) % speed) / speed
        return fadeBetween(table, progress)
    }

    private fun fadeBetween(table: Array<Color>, progress: Double): Color {
        val i = table.size
        if (progress == 1.0) return table[0]
        if (progress == 0.0) return table[i - 1]

        val max = Math.max(0.0, (1.0 - progress) * (i - 1))
        val min = max.toInt()
        return fadeBetween(table[min], table[(min + 1) % i], max - min)
    }

    private fun fadeBetween(start: Color, end: Color, progress: Double): Color {
        val actualProgress = if (progress > 1.0) 1.0 - progress % 1.0 else progress
        return gradient(start, end, actualProgress)
    }

    private fun gradient(start: Color, end: Color, progress: Double): Color {
        val invert = 1.0 - progress
        return Color(
            (start.red * invert + end.red * progress).toInt(),
            (start.green * invert + end.green * progress).toInt(),
            (start.blue * invert + end.blue * progress).toInt()
        )
    }

    private fun darkenColor(color: Color, factor: Float): Color {
        return Color(
            (color.red * factor).toInt(),
            (color.green * factor).toInt(),
            (color.blue * factor).toInt()
        )
    }

    override val tag: String
        get() = colorMode
}