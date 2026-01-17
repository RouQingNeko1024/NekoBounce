/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

object KillESP : Module("KillESP", Category.RENDER) {

    private val defaultMode by boolean("Default", false)
    private val fdpMode by boolean("FDP", false)
    private val jelloMode by boolean("Jello", false)
    private val sigmaMode by boolean("Sigma", false)
    private val tracersMode by boolean("Tracers", false)
    private val zavzMode by boolean("Zavz", true)
    private val zavz2Mode by boolean("Zywl", false)
    private val liesMode by boolean("Lies", false)
    private val simsMode by boolean("Sims", false)
    private val blockMode by boolean("Block", false)
    private val otherBlockMode by boolean("OtherBlock", false)
    private val colorMode by choices("Color", arrayOf("Custom", "Rainbow", "Sky", "Slowly", "Fade", "Mixer", "Health"), "Custom")
    private val colorRed by int("Red", 255, 0..255)
    private val colorGreen by int("Green", 255, 0..255)
    private val colorBlue by int("Blue", 255, 0..255)
    private val colorAlpha by int("Alpha", 255, 0..255)
    private val circle by boolean("Circle", false)
    private val circleRed by int("CircleRed", 255, 0..255)
    private val circleGreen by int("CircleGreen", 255, 0..255)
    private val circleBlue by int("CircleBlue", 255, 0..255)
    private val circleAlpha by int("CircleAlpha", 255, 0..255)
    private val circleThickness by float("CircleThickness", 2f, 1f..5f)
    private val speed by float("Zavz-Speed", 0.1f, 0f..10f)
    private val dual by boolean("Zavz-Dual", true)
    private val jelloAlpha by float("JelloEndAlphaPercent", 0.4f, 0f..1f)
    private val jelloWidth by float("JelloCircleWidth", 3f, 0.01f..5f)
    private val jelloGradientHeight by float("JelloGradientHeight", 3f, 1f..8f)
    private val jelloFadeSpeed by float("JelloFadeSpeed", 0.1f, 0.01f..0.5f)
    private val saturation by float("Saturation", 1f, 0f..1f)
    private val brightness by float("Brightness", 1f, 0f..1f)
    private val moveMark by float("MoveMarkY", 0.6f, 0f..2f)
    private val thickness by float("Thickness", 1f, 0.1f..5f)
    private val colorTeam by boolean("Team", false)
    private val blockMarkExpand by float("BlockExpandValue", 0.2f, -0.5f..1f)

    private val markTimer = MSTimer()
    private var entity: EntityLivingBase? = null
    private var markEntity: EntityLivingBase? = null
    private var direction = 1.0
    private var yPos = 0.0
    private var progress = 0.0
    private var start = 0.0
    private var al = 0f
    private var bb: AxisAlignedBB? = null
    private var lastMS = System.currentTimeMillis()
    private var lastDeltaMS = 0L

    override fun onEnable() {
        start = 0.0
    }

    val onTick = handler<UpdateEvent> {
        if (jelloMode) {
            val target = KillAura.target
            al = animate(al, if (target != null) jelloFadeSpeed else -jelloFadeSpeed, 0f, colorAlpha / 255.0f)
        }
    }

    val onRender3D = handler<Render3DEvent> { event ->
        if (jelloMode) renderJelloMode(event)
        if (defaultMode) renderDefaultMode(event)
        if (simsMode) renderSimsMode(event)
        if (liesMode) renderLiesMode(event)
        if (blockMode) renderBlockMode(event)
        if (otherBlockMode) renderOtherBlockMode(event)
        if (zavzMode) renderZavzMode(event)
        if (tracersMode) renderTracersMode(event)
        if (zavz2Mode) renderZavz2Mode(event)
        if (sigmaMode) renderSigmaMode(event)
        if (fdpMode) renderFDPMode(event)
        if (circle) renderCircleMode(event)
    }

    private fun renderCircleMode(event: Render3DEvent) {
        GL11.glPushMatrix()

        val player = mc.thePlayer ?: return
        val interpolatedX = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosX
        val interpolatedY = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosY
        val interpolatedZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.viewerPosZ
        GL11.glTranslated(interpolatedX, interpolatedY, interpolatedZ)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        GL11.glLineWidth(circleThickness)
        GL11.glColor4f(circleRed / 255.0f, circleGreen / 255.0f, circleBlue / 255.0f, circleAlpha / 255.0f)

        GL11.glRotatef(90f, 1f, 0f, 0f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        for (i in 0..360 step 5) {
            val angleRadians = Math.toRadians(i.toDouble())
            GL11.glVertex2f(
                cos(angleRadians).toFloat() * 4.5f,
                sin(angleRadians).toFloat() * 4.5f
            )
        }

        GL11.glEnd()

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)

        GL11.glPopMatrix()
    }

    private fun renderFDPMode(event: Render3DEvent) {
        val target = markEntity ?: return
        val drawTime = (System.currentTimeMillis() % 1500).toInt()
        val drawMode = drawTime > 750
        var drawPercent = drawTime / 750.0

        drawPercent = if (!drawMode) {
            1 - drawPercent
        } else {
            drawPercent - 1
        }

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)

        val bb = target.entityBoundingBox
        val radius = ((bb.maxX - bb.minX + (bb.maxZ - bb.minZ)) * 0.5).toFloat()

        val x = target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        mc.entityRenderer.disableLightmap()
        GL11.glLineWidth(radius * 8f)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        
        for (i in 0..360 step 10) {
            val hue = if (i < 180) {
                i / 180f
            } else {
                (-(i - 360)) / 180f
            }
            val color = Color.getHSBColor(hue, 0.7f, 1f)
            GlStateManager.color(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
            GL11.glVertex3d(x - sin(i * Math.PI / 180.0) * radius, y, z + cos(i * Math.PI / 180.0) * radius)
        }

        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderSigmaMode(event: Render3DEvent) {
        val target = markEntity ?: return
        val drawTime = System.currentTimeMillis() % 2000
        val drawMode = drawTime > 1000
        var drawPercent = drawTime / 1000.0
        
        drawPercent = if (!drawMode) {
            1 - drawPercent
        } else {
            drawPercent - 1
        }
        drawPercent = easeInOutQuad(drawPercent)

        val points = mutableListOf<Vec3>()
        val bb = target.entityBoundingBox
        val radius = bb.maxX - bb.minX
        val height = bb.maxY - bb.minY
        val posX = target.lastTickPosX + (target.posX - target.lastTickPosX) * mc.timer.renderPartialTicks
        var posY = target.lastTickPosY + (target.posY - target.lastTickPosY) * mc.timer.renderPartialTicks
        
        if (drawMode) {
            posY -= 0.5
        } else {
            posY += 0.5
        }
        
        val posZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * mc.timer.renderPartialTicks
        
        for (i in 0..360 step 7) {
            points.add(Vec3(
                posX - sin(i * Math.PI / 180.0) * radius,
                posY + height * drawPercent,
                posZ + cos(i * Math.PI / 180.0) * radius
            ))
        }
        points.add(points[0])

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBegin(GL11.GL_LINE_STRIP)
        
        val baseMove = if (drawPercent > 0.5) (1 - drawPercent) else drawPercent
        val min = (height / 60) * 20 * (1 - baseMove) * (if (drawMode) -1 else 1)
        
        for (i in 0..20) {
            var moveFace = (height / 60.0) * i * baseMove
            if (drawMode) moveFace = -moveFace
            
            val firstPoint = points[0]
            GL11.glVertex3d(
                firstPoint.xCoord - mc.renderManager.viewerPosX,
                firstPoint.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                firstPoint.zCoord - mc.renderManager.viewerPosZ
            )
            GL11.glColor4f(1f, 1f, 1f, 0.7f * (i / 20f))
            
            for (vec3 in points) {
                GL11.glVertex3d(
                    vec3.xCoord - mc.renderManager.viewerPosX,
                    vec3.yCoord - moveFace - min - mc.renderManager.viewerPosY,
                    vec3.zCoord - mc.renderManager.viewerPosZ
                )
            }
            GL11.glColor4f(0f, 0f, 0f, 0f)
        }
        
        GL11.glEnd()
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderJelloMode(event: Render3DEvent) {
        val lastY = yPos

        if (al > 0f) {
            if (System.currentTimeMillis() - lastMS >= 1000L) {
                direction = -direction
                lastMS = System.currentTimeMillis()
            }
            val weird = if (direction > 0) System.currentTimeMillis() - lastMS else 1000L - (System.currentTimeMillis() - lastMS)
            progress = weird / 1000.0
            lastDeltaMS = System.currentTimeMillis() - lastMS
        } else {
            lastMS = System.currentTimeMillis() - lastDeltaMS
        }

        val target = KillAura.target
        if (target != null) {
            entity = target
            bb = target.entityBoundingBox
        }

        if (bb == null || entity == null) return

        val radius = bb!!.maxX - bb!!.minX
        val height = bb!!.maxY - bb!!.minY
        val posX = entity!!.lastTickPosX + (entity!!.posX - entity!!.lastTickPosX) * mc.timer.renderPartialTicks
        val posY = entity!!.lastTickPosY + (entity!!.posY - entity!!.lastTickPosY) * mc.timer.renderPartialTicks
        val posZ = entity!!.lastTickPosZ + (entity!!.posZ - entity!!.lastTickPosZ) * mc.timer.renderPartialTicks

        yPos = easeInOutQuart(progress) * height
        val deltaY = (if (direction > 0) yPos - lastY else lastY - yPos) * -direction * jelloGradientHeight

        if (al <= 0f && entity != null) {
            entity = null
            return
        }

        val colour = getColor(entity!!)
        val r = colour.red / 255.0f
        val g = colour.green / 255.0f
        val b = colour.blue / 255.0f

        pre3D()
        GL11.glTranslated(-mc.renderManager.viewerPosX, -mc.renderManager.viewerPosY, -mc.renderManager.viewerPosZ)

        GL11.glBegin(GL11.GL_QUAD_STRIP)

        for (i in 0..360) {
            val calc = i * Math.PI / 180
            val posX2 = posX - sin(calc) * radius
            val posZ2 = posZ + cos(calc) * radius

            GL11.glColor4f(r, g, b, 0f)
            GL11.glVertex3d(posX2, posY + yPos + deltaY, posZ2)

            GL11.glColor4f(r, g, b, al * jelloAlpha)
            GL11.glVertex3d(posX2, posY + yPos, posZ2)
        }

        GL11.glEnd()

        drawCircle(posX, posY + yPos, posZ, jelloWidth, radius, r, g, b, al)

        post3D()
    }

    private fun renderDefaultMode(event: Render3DEvent) {
        val target = KillAura.target ?: return
        val color = if (target.hurtTime > 0) {
            Color(colorRed, colorGreen, colorBlue, colorAlpha)
        } else {
            Color(235, 40, 40, colorAlpha)
        }
        RenderUtils.drawEntityBox(target, color, true)
    }

    private fun renderSimsMode(event: Render3DEvent) {
        val target = KillAura.target ?: return
        renderESP()
        val colorRGB = if (target.hurtTime <= 0) {
            Color(80, 255, 80, 200).rgb
        } else {
            Color(255, 0, 0, 200).rgb
        }
        drawESP(target, colorRGB, event)
    }

    private fun renderLiesMode(event: Render3DEvent) {
        val target = KillAura.target ?: return
        val ticks = event.partialTicks

        val interval = 3000
        val drawTime = System.currentTimeMillis() % interval
        val drawMode = drawTime > (interval / 2)
        var drawPercent = drawTime / (interval / 2.0)

        if (!drawMode) {
            drawPercent = 1 - drawPercent
        } else {
            drawPercent -= 1
        }

        drawPercent = easeInOutQuad(drawPercent)

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_CULL_FACE)
        GL11.glShadeModel(GL11.GL_FLAT)
        mc.entityRenderer.disableLightmap()

        val bb = target.entityBoundingBox
        val radius = ((bb.maxX - bb.minX) + (bb.maxZ - bb.minZ)) * 0.5
        val height = (bb.maxY - bb.minY).toFloat()
        val x = target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = (target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY) + height * drawPercent
        val z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        val eased = ((height / 3) * (if (drawPercent > 0.5) (1 - drawPercent) else drawPercent) * (if (drawMode) -1 else 1)).toFloat()

        for (i in 5..360 step 5) {
            val color = Color.getHSBColor(
                if (i < 180) i / 180f else (-(i - 360)) / 180f,
                0.7f,
                1f
            )
            val x1 = x - sin(i * Math.PI / 180.0) * radius
            val z1 = z + cos(i * Math.PI / 180.0) * radius
            val x2 = x - sin((i - 5) * Math.PI / 180.0) * radius
            val z2 = z + cos((i - 5) * Math.PI / 180.0) * radius
            
            GL11.glBegin(GL11.GL_QUADS)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 0f)
            GL11.glVertex3d(x1, y + eased, z1)
            GL11.glVertex3d(x2, y + eased, z2)
            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 150f / 255f)
            GL11.glVertex3d(x2, y, z2)
            GL11.glVertex3d(x1, y, z1)
            GL11.glEnd()
        }

        GL11.glEnable(GL11.GL_CULL_FACE)
        GL11.glShadeModel(GL11.GL_FLAT)
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderBlockMode(event: Render3DEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || entity == mc.thePlayer) continue

            val originalBB = entity.entityBoundingBox
            val expandedBB = originalBB.expand(blockMarkExpand.toDouble(), blockMarkExpand.toDouble(), blockMarkExpand.toDouble())
            entity.entityBoundingBox = expandedBB

            val boxColor = if (entity.hurtTime <= 0) {
                if (entity == KillAura.target) {
                    Color(25, 230, 0, 170)
                } else {
                    Color(10, 250, 10, 170)
                }
            } else {
                Color(255, 0, 0, 170)
            }

            RenderUtils.drawEntityBox(entity, boxColor, true)
            entity.entityBoundingBox = originalBB
        }
    }

    private fun renderOtherBlockMode(event: Render3DEvent) {
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || entity == mc.thePlayer) continue

            val originalBB = entity.entityBoundingBox
            val expandedBB = originalBB.expand(blockMarkExpand.toDouble(), blockMarkExpand.toDouble(), blockMarkExpand.toDouble())
            entity.entityBoundingBox = expandedBB

            val boxColor = if (entity.hurtTime <= 0) {
                if (entity == KillAura.target) {
                    Color(25, 230, 0, 170)
                } else {
                    Color(10, 250, 10, 170)
                }
            } else {
                Color(255, 0, 0, 170)
            }

            RenderUtils.drawEntityBox(entity, boxColor, true)
            entity.entityBoundingBox = originalBB
        }
    }

    private fun renderZavzMode(event: Render3DEvent) {
        val target = KillAura.target ?: return
        val ticks = event.partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        val x = target.lastTickPosX + (target.posX - target.lastTickPosX) * ticks - mc.renderManager.viewerPosX
        val z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * ticks - mc.renderManager.viewerPosZ
        var y = target.lastTickPosY + (target.posY - target.lastTickPosY) * ticks - mc.renderManager.viewerPosY

        val radius = 0.65
        val precision = 360
        var startPos = start % 360
        start += speed

        for (i in 0..precision) {
            val posX = x + radius * cos(startPos + i * Math.PI * 2 / (precision / 2.0))
            val posZ = z + radius * sin(startPos + i * Math.PI * 2 / (precision / 2.0))

            GL11.glColor4f(1f, 1f, 1f, 1f)
            GL11.glVertex3d(posX, y, posZ)
            y += target.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)

        if (dual) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)
            GL11.glLineWidth(2f)
            GL11.glBegin(GL11.GL_LINE_STRIP)

            startPos = start % 360
            start += speed
            y = target.lastTickPosY + (target.posY - target.lastTickPosY) * ticks - mc.renderManager.viewerPosY + target.height

            for (i in 0..precision) {
                val posX = x + radius * cos(-(startPos + i * Math.PI * 2 / (precision / 2.0)))
                val posZ = z + radius * sin(-(startPos + i * Math.PI * 2 / (precision / 2.0)))

                GL11.glColor4f(1f, 1f, 1f, 1f)
                GL11.glVertex3d(posX, y, posZ)
                y -= target.height / precision
            }

            GL11.glEnd()
            GL11.glDepthMask(true)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
        }

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderZavz2Mode(event: Render3DEvent) {
        val target = KillAura.target ?: return
        val ticks = event.partialTicks

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)

        renderRing(target, ticks, false)
        if (dual) renderRing(target, ticks, true)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun renderRing(target: EntityLivingBase, ticks: Float, dualRing: Boolean) {
        val x = target.lastTickPosX + (target.posX - target.lastTickPosX) * ticks - mc.renderManager.viewerPosX
        val z = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * ticks - mc.renderManager.viewerPosZ
        var y = target.lastTickPosY + (target.posY - target.lastTickPosY) * ticks - mc.renderManager.viewerPosY

        val radius = 0.65
        val precision = 360
        var startPos = start % 360
        start += speed

        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        GL11.glBegin(GL11.GL_LINE_STRIP)

        for (i in 0..precision) {
            val angle = startPos + i * Math.PI * 2.0 / precision
            val posX = x + radius * cos(angle)
            val posZ = z + radius * sin(angle)

            val offset = Math.abs(System.currentTimeMillis() / 10L) / 100.0 + y
            val alpha = if (dualRing) 0 else 170
            val color = ColorUtils.interpolateColor(Color.WHITE, Color.BLACK, offset.toFloat())

            GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, alpha / 255f)
            GL11.glVertex3d(posX, y, posZ)

            y += if (dualRing) -target.height / precision else target.height / precision
        }

        GL11.glEnd()
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glPopMatrix()
    }

    private fun renderTracersMode(event: Render3DEvent) {
        val target = KillAura.target ?: return

        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(thickness)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)

        GL11.glBegin(GL11.GL_LINES)
        val color = getColor(target)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        
        val player = mc.thePlayer ?: return
        GL11.glVertex3d(
            player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX,
            player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY + player.eyeHeight.toDouble(),
            player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        )
        
        GL11.glVertex3d(
            target.lastTickPosX + (target.posX - target.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX,
            target.lastTickPosY + (target.posY - target.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY + target.height / 2,
            target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        )
        
        GL11.glEnd()

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(true)
        GL11.glDisable(GL11.GL_BLEND)
        GlStateManager.resetColor()
    }

    private fun renderESP() {
        val entity = markEntity ?: return
        
        if (markTimer.hasTimePassed(500) || entity.isDead) {
            markEntity = null
            return
        }

        val drawTime = System.currentTimeMillis() % 2000
        val drawMode = drawTime > 1000
        var drawPercent = drawTime / 1000.0

        if (!drawMode) {
            drawPercent = 1 - drawPercent
        } else {
            drawPercent -= 1
        }

        val points = mutableListOf<Vec3>()
        val bb = entity.entityBoundingBox
        val radius = bb.maxX - bb.minX
        val height = bb.maxY - bb.minY
        val posX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * mc.timer.renderPartialTicks
        var posY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * mc.timer.renderPartialTicks
        
        if (drawMode) {
            posY -= 0.5
        } else {
            posY += 0.5
        }
        
        val posZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * mc.timer.renderPartialTicks
        
        for (i in 0..360 step 7) {
            points.add(Vec3(
                posX - sin(i * Math.PI / 180.0) * radius,
                posY + height * drawPercent,
                posZ + cos(i * Math.PI / 180.0) * radius
            ))
        }
        points.add(points[0])

        mc.entityRenderer.disableLightmap()
        GL11.glPushMatrix()
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        
        for (i in 0..20) {
            var moveFace = (height / 60.0) * i
            if (drawMode) moveFace = -moveFace
            
            val firstPoint = points[0]
            GL11.glBegin(GL11.GL_LINE_STRIP)
            GL11.glVertex3d(
                firstPoint.xCoord - mc.renderManager.viewerPosX,
                firstPoint.yCoord - moveFace - mc.renderManager.viewerPosY,
                firstPoint.zCoord - mc.renderManager.viewerPosZ
            )
            GL11.glColor4f(1f, 1f, 1f, 0.7f * (i / 20f))
            
            for (vec3 in points) {
                GL11.glVertex3d(
                    vec3.xCoord - mc.renderManager.viewerPosX,
                    vec3.yCoord - moveFace - mc.renderManager.viewerPosY,
                    vec3.zCoord - mc.renderManager.viewerPosZ
                )
            }
            GL11.glColor4f(0f, 0f, 0f, 0f)
            GL11.glEnd()
        }
        
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }

    private fun drawESP(entity: EntityLivingBase, color: Int, event: Render3DEvent) {
        val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
        val radius = 0.15f

        GL11.glPushMatrix()
        GL11.glTranslated(x, y + 2, z)
        GL11.glRotatef(-entity.width, 0f, 1f, 0f)

        GL11.glColor4f(Color(color).red / 255f, Color(color).green / 255f, Color(color).blue / 255f, Color(color).alpha / 255f)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glLineWidth(1.5f)

        GL11.glBegin(GL11.GL_LINE_STRIP)
        GL11.glColor4f(if (entity.hurtTime <= 0) 80/255f else 255/255f, 
                      if (entity.hurtTime <= 0) 255/255f else 0f, 
                      if (entity.hurtTime <= 0) 80/255f else 0f, 
                      200/255f)
        
        // 绘制圆柱体轮廓
        for (i in 0..360 step 10) {
            val angle = Math.toRadians(i.toDouble())
            GL11.glVertex3d(cos(angle).toDouble() * radius, 0.0, sin(angle).toDouble() * radius)
            GL11.glVertex3d(cos(angle).toDouble() * radius, 0.3, sin(angle).toDouble() * radius)
        }
        
        GL11.glEnd()

        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glPopMatrix()
    }

    private fun getColor(entity: Entity): Color {
        if (entity is EntityLivingBase) {
            if (colorMode.equals("Health", ignoreCase = true)) {
                val health = entity.health / entity.maxHealth
                return Color(
                    (1.0 - health).toFloat().coerceIn(0f, 1f),
                    health.toFloat().coerceIn(0f, 1f),
                    0f
                )
            }

            if (colorTeam) {
                val chars = entity.displayName.unformattedText.toCharArray()
                var color = Int.MAX_VALUE

                for (i in chars.indices) {
                    if (chars[i] != '§' || i + 1 >= chars.size) continue
                    
                    val index = "0123456789abcdef".indexOf(chars[i + 1])
                    if (index < 0 || index > 15) continue
                    
                    color = ColorUtils.hexColors[index]
                    break
                }
                
                return Color(color)
            }
        }

        return when (colorMode.lowercase()) {
            "custom" -> Color(colorRed, colorGreen, colorBlue)
            "slowly" -> ColorUtils.rainbow()
            else -> fade(Color(colorRed, colorGreen, colorBlue), 0, 100)
        }
    }

    private fun pre3D() {
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glShadeModel(GL11.GL_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LIGHTING)
        GL11.glDepthMask(false)
        GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST)
        GL11.glDisable(GL11.GL_CULL_FACE)
    }

    private fun post3D() {
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
        GL11.glColor4f(1f, 1f, 1f, 1f)
    }

    private fun drawCircle(x: Double, y: Double, z: Double, width: Float, radius: Double, red: Float, green: Float, blue: Float, alp: Float) {
        GL11.glLineWidth(width)
        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glColor4f(red, green, blue, alp)

        for (i in 0..360) {
            val posX = x - sin(i * Math.PI / 180.0) * radius
            val posZ = z + cos(i * Math.PI / 180.0) * radius
            GL11.glVertex3d(posX, y, posZ)
        }

        GL11.glEnd()
    }

    private fun easeInOutQuart(x: Double): Double {
        return if (x < 0.5) {
            8 * x * x * x * x
        } else {
            1 - (-2 * x + 2).pow(4) / 2
        }
    }

    private fun easeInOutQuad(t: Double): Double {
        return if (t < 0.5) {
            2 * t * t
        } else {
            -1 + (4 - 2 * t) * t
        }
    }

    private fun animate(value: Float, target: Float, speed: Float, max: Float): Float {
        val diff = target - value
        val delta = diff.coerceIn(-speed, speed)
        return (value + delta).coerceIn(0f, max)
    }

    private fun fade(color: Color, index: Int, count: Int): Color {
        val hue = (System.currentTimeMillis() % 2000L / 2000f + index * (1f / count)) % 1f
        return Color.getHSBColor(hue, saturation, brightness)
    }
}