/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.MotionEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object NoBob : Module("NoBob", Category.RENDER, gameDetecting = false) {

    val onMotion = handler<MotionEvent> {
        mc.thePlayer?.distanceWalkedModified = -1f
    }
}
