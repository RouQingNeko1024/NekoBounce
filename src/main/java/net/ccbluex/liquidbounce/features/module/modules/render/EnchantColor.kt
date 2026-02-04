/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import java.awt.Color

object EnchantColor : Module("EnchantColor", Category.RENDER) {

    private val color = ColorSettingsInteger(this, "Color").with(255, 20, 147)

    val enchantRGB: Color
        get() = color.color()
}