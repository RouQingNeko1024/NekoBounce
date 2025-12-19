package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object NoHitSlow : Module("NoHitSlow", Category.MOVEMENT) {

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState == EventState.PRE) {
            val thePlayer = mc.thePlayer ?: return@handler
            
            if (thePlayer.hurtTime > 0) {
                thePlayer.speedInAir = 0.02f
            }
        }
    }
}