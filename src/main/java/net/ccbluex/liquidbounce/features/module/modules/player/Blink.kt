/*
 * FireBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.Breadcrumbs
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.server.S14PacketEntity
import org.lwjgl.opengl.GL11.*

object Blink : Module("Blink", Category.PLAYER, gameDetecting = false) {

    var mode by choices("Mode", arrayOf("Sent", "Received", "Both"), "Sent")
    private val allowS14 by boolean("AllowS14Packet",true) {mode in arrayOf("Received","Both")}

    var pulse by boolean("Pulse", false)
    val pulseDelay by int("PulseDelay", 1000, 50..10000,"ms") { pulse }

    val fakePlayerMenu by boolean("FakePlayer", true)

    val pulseTimer = MSTimer()

    override fun onEnable() {
        pulseTimer.reset()

        if (fakePlayerMenu)
            BlinkUtils.addFakePlayer()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        BlinkUtils.unblink()
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (mc.thePlayer == null || mc.thePlayer.isDead)
            return@handler

        when (mode.lowercase()) {
            "sent" -> {
                BlinkUtils.blink(packet, event, sent = true, receive = false)
            }

            "received" -> {
                if (!allowS14) {
                    BlinkUtils.blink(packet, event, sent = false, receive = true)
                } else {
                    // 如果有S14PacketEntity，根据allowS14设置决定是否处理
                    if (packet is S14PacketEntity) {
                        // 如果允许S14包，则正常处理
                        BlinkUtils.blink(packet, event, sent = false, receive = true)
                    } else {
                        // 其他包正常处理
                        BlinkUtils.blink(packet, event, sent = false, receive = true)
                    }
                }
            }

            "both" -> {
                if (!allowS14) {
                    BlinkUtils.blink(packet, event)
                } else {
                    // 如果有S14PacketEntity，根据allowS14设置决定是否处理
                    if (packet is S14PacketEntity) {
                        // 如果允许S14包，则正常处理
                        BlinkUtils.blink(packet, event)
                    } else {
                        // 其他包正常处理
                        BlinkUtils.blink(packet, event)
                    }
                }
            }
        }
    }

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState == EventState.POST) {
            val thePlayer = mc.thePlayer ?: return@handler

            if (thePlayer.isDead || mc.thePlayer.ticksExisted <= 10) {
                BlinkUtils.unblink()
            }

            when (mode.lowercase()) {
                "sent" -> {
                    BlinkUtils.syncSent()
                }

                "received" -> {
                    BlinkUtils.syncReceived()
                }
            }

            if (pulse && pulseTimer.hasTimePassed(pulseDelay)) {
                BlinkUtils.unblink()
                if (fakePlayerMenu) {
                    BlinkUtils.addFakePlayer()
                }
                pulseTimer.reset()
            }
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val color = Breadcrumbs.colors.color()

        synchronized(BlinkUtils.positions) {
            glPushMatrix()
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)
            glEnable(GL_BLEND)
            glDisable(GL_DEPTH_TEST)
            mc.entityRenderer.disableLightmap()
            glBegin(GL_LINE_STRIP)
            glColor(color)

            val renderPosX = mc.renderManager.viewerPosX
            val renderPosY = mc.renderManager.viewerPosY
            val renderPosZ = mc.renderManager.viewerPosZ

            for (pos in BlinkUtils.positions)
                glVertex3d(pos.xCoord - renderPosX, pos.yCoord - renderPosY, pos.zCoord - renderPosZ)

            glColor4d(1.0, 1.0, 1.0, 1.0)
            glEnd()
            glEnable(GL_DEPTH_TEST)
            glDisable(GL_LINE_SMOOTH)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            glPopMatrix()
        }
    }

    override val tag
        get() = (BlinkUtils.packets.size + BlinkUtils.packetsReceived.size).toString()

    fun blinkingSend() = handleEvents() && (mode == "Sent" || mode == "Both")
    fun blinkingReceive() = handleEvents() && (mode == "Received" || mode == "Both")
}