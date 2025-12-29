package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module


object Camera : Module("Camera", Category.RENDER, gameDetecting = false) {

    val motionCamera = boolean("MotionCamera", true)
    val interpolation = float("MotionInterpolation", 0.05f, 0.01f..0.5f) { motionCamera.get()}
    override fun onEnable() {
        LiquidBounce.moduleManager.getModule(Camera::class.java)?.state = false
    }
    override val tag
        get() = interpolation.get().toString()
}