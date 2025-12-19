
package net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.nowebmodes.NoWebMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer

object Matrix : NoWebMode("Matrix") {
    private val timer = MSTimer()

    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isInWeb) {
            return
        }

        thePlayer.jumpMovementFactor = 0.12425f
        thePlayer.motionY = -0.0125

        if (mc.gameSettings.keyBindSneak.isKeyDown) {
            thePlayer.motionY = -0.1625
        }

        if (timer.hasTimePassed(1000)) {
            mc.timer.timerSpeed = 3.0f
            timer.reset()
        } else {
            mc.timer.timerSpeed = 1.0f
        }
    }
}