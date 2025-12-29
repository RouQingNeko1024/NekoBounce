/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.flymodes.aac

import io.qzz.nekobounce.event.Render3DEvent
import io.qzz.nekobounce.features.module.modules.movement.Fly.aacMotion
import io.qzz.nekobounce.features.module.modules.movement.flymodes.FlyMode
import io.qzz.nekobounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color

object AAC3312 : FlyMode("AAC3.3.12") {
    override fun onUpdate() {
        if (mc.thePlayer.posY < -70)
            mc.thePlayer.motionY = aacMotion.toDouble()

        mc.timer.timerSpeed = 1f

        if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
            mc.timer.timerSpeed = 0.2f
            mc.rightClickDelayTimer = 0
        }
    }

    override fun onRender3D(event: Render3DEvent) {
        RenderUtils.drawPlatform(-70.0, Color(0, 0, 255, 90), 1.0)
    }
}
