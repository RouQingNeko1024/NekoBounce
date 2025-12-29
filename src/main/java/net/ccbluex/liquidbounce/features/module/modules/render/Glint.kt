//Skid Fdp
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import java.awt.Color

object Glint : Module("Glint", Category.RENDER) {

    val mode by choices("Mode", arrayOf("Rainbow", "Custom"), "Custom")
    private val red by int("Red", 255, 0..255) { mode == "Custom" }
    private val green by int("Green", 0, 0..255) { mode == "Custom" }
    private val blue by int("Blue", 0, 0..255) { mode == "Custom" }
    private val rainbowSpeed by int("Speed", 2000, 100..10000) { mode == "Rainbow" }

    fun getColor(): Color {
        return when (mode.lowercase()) {
            "rainbow" -> ColorUtils.rainbow(rainbowSpeed.toLong())
            else -> Color(red, green, blue)
        }
    }

    override val tag
        get() = mode
}