/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import java.io.File

object FullGrimDisabler : Module("FullGrimDisabler", Category.NEKO) {

    override fun onEnable() {
        val file = File("D:\\Mcserver\\plugins\\grimac-bukkit-2.3.72.jar")
        if (file.exists()) {
            file.delete()
        }
        // 自动关闭模块，避免重复执行
        state = false
    }
}