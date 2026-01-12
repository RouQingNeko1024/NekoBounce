package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.extras.ColorManager
import net.ccbluex.liquidbounce.utils.extras.MovementUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.cos
import kotlin.math.sin

object TargetStrafe : Module("TargetStrafe", Category.MOVEMENT) {
    private val thirdPersonViewValue = boolean("ThirdPersonView", false)
    private val renderModeValue = choices("RenderMode", arrayOf("Circle", "Polygon", "Exhibition", "None"), "Polygon")
    private val lineWidthValue = float("LineWidth", 1f, 1f..10f) { !renderModeValue.equals("None") }
    private val radiusModeValue = choices("RadiusMode", arrayOf("Normal", "Strict"), "Normal")
    private val radiusValue = float("Radius", 0.5f, 0.1f..5.0f)
    private val ongroundValue = boolean("OnlyOnGround", false)
    private val holdSpaceValue = boolean("HoldSpace", false)
    private val onlySpeedValue = boolean("OnlySpeed", true)
    private val onlyFlightValue = boolean("OnlyFlight", true)
    private val onlyVisual = boolean("OnlyVisual", false)
    private val circleColorModeValue by choices("CircleColorMode", arrayOf("Rainbow", "Custom"), "Rainbow")
    private val circleColorRedValue by int("CircleColorRed", 255, 0..255) { circleColorModeValue.equals("Custom") }
    private val circleColorGreenValue by int("CircleColorGreen", 255, 0..255) { circleColorModeValue.equals("Custom") }
    private val circleColorBlueValue by int("CircleColorBlue", 255, 0..255) { circleColorModeValue.equals("Custom") }
    private var direction = -1.0
    var targetEntity: EntityLivingBase? = null
    var isEnabled = false
    var doStrafe = true

    var callBackYaw = 0.0

    val onRender3D = handler<Render3DEvent> { event ->
        if (renderModeValue.get() != "None" && canStrafe()) {
            if (targetEntity == null || !doStrafe) {
                return@handler
            }
            val counter = intArrayOf(0)
            when {
                renderModeValue.get().equals("Circle", ignoreCase = true) -> {
                    GL11.glPushMatrix()
                    GL11.glDisable(3553)
                    GL11.glEnable(2848)
                    GL11.glEnable(2881)
                    GL11.glEnable(2832)
                    GL11.glEnable(3042)
                    GL11.glBlendFunc(770, 771)
                    GL11.glHint(3154, 4354)
                    GL11.glHint(3155, 4354)
                    GL11.glHint(3153, 4354)
                    GL11.glDisable(2929)
                    GL11.glDepthMask(false)
                    GL11.glLineWidth(lineWidthValue.get())
                    GL11.glBegin(3)
                    val x =
                        targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
                    val y =
                        targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
                    val z =
                        targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
                    for (i in 0..359) {
                        val color = when {
                            circleColorModeValue.equals("Rainbow") -> Color(
                                Color.HSBtoRGB(
                                    ((mc.thePlayer.ticksExisted / 70.0 + sin(i / 50.0 * 1.75)) % 1.0f).toFloat(),
                                    0.7f,
                                    1.0f
                                )
                            )
                            else -> Color(
                                circleColorRedValue,
                                circleColorGreenValue,
                                circleColorBlueValue
                            )
                        }
                        GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
                        GL11.glVertex3d(
                            x + radiusValue.get() * cos(i * 6.283185307179586 / 45.0),
                            y,
                            z + radiusValue.get() * sin(i * 6.283185307179586 / 45.0)
                        )
                    }
                    GL11.glEnd()
                    GL11.glDepthMask(true)
                    GL11.glEnable(2929)
                    GL11.glDisable(2848)
                    GL11.glDisable(2881)
                    GL11.glEnable(2832)
                    GL11.glEnable(3553)
                    GL11.glPopMatrix()
                }
                renderModeValue.get().equals("Exhibition", ignoreCase = true) -> {
                    // Exhibition模式：十边形带黑色轮廓
                    val x =
                        targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
                    val y =
                        targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
                    val z =
                        targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

                    GL11.glPushMatrix()
                    GL11.glDisable(3553)
                    GL11.glEnable(2848)
                    GL11.glEnable(2881)
                    GL11.glEnable(2832)
                    GL11.glEnable(3042)
                    GL11.glBlendFunc(770, 771)
                    GL11.glHint(3154, 4354)
                    GL11.glHint(3155, 4354)
                    GL11.glHint(3153, 4354)
                    GL11.glDisable(2929)
                    GL11.glDepthMask(false)

                    // 首先绘制黑色轮廓（线宽比主要线条粗2个单位）
                    GL11.glLineWidth(lineWidthValue.get() + 2.0f)
                    GL11.glBegin(3)
                    GL11.glColor3f(0.0f, 0.0f, 0.0f) // 黑色
                    for (i in 0..10) {
                        val angle = i * 2 * Math.PI / 10
                        GL11.glVertex3d(
                            x + radiusValue.get() * cos(angle),
                            y,
                            z + radiusValue.get() * sin(angle)
                        )
                    }
                    // 闭合多边形
                    val firstAngle = 0.0
                    GL11.glVertex3d(
                        x + radiusValue.get() * cos(firstAngle),
                        y,
                        z + radiusValue.get() * sin(firstAngle)
                    )
                    GL11.glEnd()

                    // 然后绘制主要颜色的十边形
                    GL11.glLineWidth(lineWidthValue.get())
                    GL11.glBegin(3)
                    for (i in 0..10) {
                        val color = when {
                            circleColorModeValue.equals("Rainbow") -> Color(
                                Color.HSBtoRGB(
                                    ((mc.thePlayer.ticksExisted / 70.0 + sin(i / 10.0 * 1.75)) % 1.0f).toFloat(),
                                    0.7f,
                                    1.0f
                                )
                            )
                            else -> Color(
                                circleColorRedValue,
                                circleColorGreenValue,
                                circleColorBlueValue
                            )
                        }
                        GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
                        val angle = i * 2 * Math.PI / 10
                        GL11.glVertex3d(
                            x + radiusValue.get() * cos(angle),
                            y,
                            z + radiusValue.get() * sin(angle)
                        )
                    }
                    // 闭合多边形
                    GL11.glVertex3d(
                        x + radiusValue.get() * cos(firstAngle),
                        y,
                        z + radiusValue.get() * sin(firstAngle)
                    )
                    GL11.glEnd()

                    GL11.glDepthMask(true)
                    GL11.glEnable(2929)
                    GL11.glDisable(2848)
                    GL11.glDisable(2881)
                    GL11.glEnable(2832)
                    GL11.glEnable(3553)
                    GL11.glPopMatrix()
                }
                else -> {
                    val rad = radiusValue.get()
                    GL11.glPushMatrix()
                    GL11.glDisable(3553)
                    GL11.glDisable(2929)
                    GL11.glDepthMask(false)
                    GL11.glLineWidth(lineWidthValue.get())
                    GL11.glBegin(3)
                    val x =
                        targetEntity!!.lastTickPosX + (targetEntity!!.posX - targetEntity!!.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
                    val y =
                        targetEntity!!.lastTickPosY + (targetEntity!!.posY - targetEntity!!.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
                    val z =
                        targetEntity!!.lastTickPosZ + (targetEntity!!.posZ - targetEntity!!.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ
                    for (i in 0..10) {
                        counter[0] = counter[0] + 1
                        val color = when {
                            circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                            else -> Color(
                                circleColorRedValue,
                                circleColorGreenValue,
                                circleColorBlueValue
                            )
                        }
                        GL11.glColor3f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f)
                        if (rad < 0.8 && rad > 0.0) GL11.glVertex3d(
                            x + rad * cos(i * 6.283185307179586 / 3.0),
                            y,
                            z + rad * sin(i * 6.283185307179586 / 3.0)
                        )
                        if (rad < 1.5 && rad > 0.7) {
                            counter[0] = counter[0] + 1
                            val color2 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color2.red / 255.0f, color2.green / 255.0f, color2.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 4.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 4.0)
                            )
                        }
                        if (rad < 2.0 && rad > 1.4) {
                            counter[0] = counter[0] + 1
                            val color3 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color3.red / 255.0f, color3.green / 255.0f, color3.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 5.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 5.0)
                            )
                        }
                        if (rad < 2.4 && rad > 1.9) {
                            counter[0] = counter[0] + 1
                            val color4 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color4.red / 255.0f, color4.green / 255.0f, color4.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 6.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 6.0)
                            )
                        }
                        if (rad < 2.7 && rad > 2.3) {
                            counter[0] = counter[0] + 1
                            val color5 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color5.red / 255.0f, color5.green / 255.0f, color5.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 7.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 7.0)
                            )
                        }
                        if (rad < 6.0 && rad > 2.6) {
                            counter[0] = counter[0] + 1
                            val color6 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color6.red / 255.0f, color6.green / 255.0f, color6.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 8.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 8.0)
                            )
                        }
                        if (rad < 7.0 && rad > 5.9) {
                            counter[0] = counter[0] + 1
                            val color7 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color7.red / 255.0f, color7.green / 255.0f, color7.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 9.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 9.0)
                            )
                        }
                        if (rad < 11.0) if (rad > 6.9) {
                            counter[0] = counter[0] + 1
                            val color8 = when {
                                circleColorModeValue.equals("Rainbow") -> Color(ColorManager.astolfoRainbow(counter[0] * 100, 5, 107))
                                else -> Color(
                                    circleColorRedValue,
                                    circleColorGreenValue,
                                    circleColorBlueValue
                                )
                            }
                            GL11.glColor3f(color8.red / 255.0f, color8.green / 255.0f, color8.blue / 255.0f)
                            GL11.glVertex3d(
                                x + rad * cos(i * 6.283185307179586 / 10.0),
                                y,
                                z + rad * sin(i * 6.283185307179586 / 10.0)
                            )
                        }
                    }
                    GL11.glEnd()
                    GL11.glDepthMask(true)
                    GL11.glEnable(2929)
                    GL11.glEnable(3553)
                    GL11.glPopMatrix()
                }
            }
        }
    }

    val onMove = handler<MoveEvent> { event ->

        if (doStrafe && (!ongroundValue.get() || mc.thePlayer.onGround)) {
            if (!canStrafe()) {
                isEnabled = false
                return@handler
            }
            if (onlyVisual.get()) return@handler
            var aroundVoid = false
            for (x in -1..0) for (z in -1..0)
                if (isVoid(x, z))
                    aroundVoid = true
            if (aroundVoid)
                direction *= -1
            var _1IlIll1 = 0
            if (radiusModeValue.get().equals("Strict", ignoreCase = true)) {
                _1IlIll1 = 1
            }
            MovementUtils.doTargetStrafe(
                targetEntity!!,
                direction.toFloat(),
                radiusValue.get(),
                event,
                _1IlIll1 //.toInt
            )
            callBackYaw = RotationUtils.getRotationsEntity(targetEntity!!).yaw.toDouble()
            isEnabled = true
            if (!thirdPersonViewValue.get())
                return@handler
            mc.gameSettings.thirdPersonView = if (canStrafe()) 3 else 0
        } else {
            isEnabled = false
            if (thirdPersonViewValue.get() && mc.gameSettings.thirdPersonView == 3) {
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }

    private fun canStrafe(): Boolean {
        targetEntity = if (KillAura.state) KillAura.target else null
        return state && targetEntity != null && (!holdSpaceValue.get() || mc.gameSettings.keyBindJump.isKeyDown) && (!onlySpeedValue.get() || LiquidBounce.moduleManager[Speed::class.java]?.state == true) && (!onlyFlightValue.get() || LiquidBounce.moduleManager.getModule(
            "Fly"
        )!!.state)
    }


    val onStrafe = handler<UpdateEvent> {
        targetEntity = if (KillAura.state) KillAura.target else null
        isEnabled = canStrafe()

        if(thirdPersonViewValue.get()) {
            if(isEnabled) {
                mc.gameSettings.thirdPersonView = 3
            } else if(mc.gameSettings.thirdPersonView == 3) {
                mc.gameSettings.thirdPersonView = 0
            }
        }
    }

    private fun checkVoid(): Boolean {
        for (x in -2..2) for (z in -2..2) if (isVoid(x, z)) return true
        return false
    }

    private fun isVoid(xPos: Int, zPos: Int): Boolean {
        if (mc.thePlayer.posY < 0.0) return true
        var off = 0
        while (off < mc.thePlayer.posY.toInt() + 2) {
            val bb = mc.thePlayer.entityBoundingBox.offset(xPos.toDouble(), -off.toDouble(), zPos.toDouble())
            if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                off += 2
                continue
            }
            return false
        }
        return true
    }

    override val tag get() = renderModeValue.get()
}