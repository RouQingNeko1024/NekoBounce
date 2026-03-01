package net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other


import net.ccbluex.liquidbounce.features.module.modules.movement.LongJump
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.LongJumpMode

object KKCraft : LongJumpMode("KKCraft") {
    var started = false
    var ticks = 0
    override fun onUpdate() {
        if (mc.thePlayer.onGround) {
            if (started) {
                LongJump.state = false
                return
            }

            if (!mc.gameSettings.keyBindJump.isKeyDown) {
                mc.thePlayer.jump()
                started = true
            }
        } else {
            if (ticks >= 2 && ticks <= 8) {
                mc.thePlayer.motionY += 0.07
            }
            ticks++
        }
    }

    override fun onEnable() {
        ticks = 0
        started = false
    }
}