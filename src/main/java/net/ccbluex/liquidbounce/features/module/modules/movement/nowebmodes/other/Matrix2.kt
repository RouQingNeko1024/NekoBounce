package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.client.settings.GameSettings

object Matrix2 : NoWebMode("Matrix2") {
    private val tickTimer = TickTimer()

    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isInWeb) return

        thePlayer.jumpMovementFactor = 0.12425f
        thePlayer.motionY = -0.0125

        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            thePlayer.motionY = -0.1625
        }

        tickTimer.update()
        if (tickTimer.hasTimePassed(40)) {
            mc.timer.timerSpeed = 3.0f
            tickTimer.reset()
        }
    }
}
