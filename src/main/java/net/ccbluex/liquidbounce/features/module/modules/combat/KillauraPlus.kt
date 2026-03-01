/*

LiquidBounce Hacked Client

A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.

https://github.com/CCBlueX/LiquidBounce/
*/
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import java.util.concurrent.ThreadLocalRandom

object KillauraPlus : Module("KillauraPlus", Category.COMBAT) {

private val moduleList = listOf("Killaura", "Killaura2", "Killaura3", "Killaura4", "Killaura5", "Aura")
private var currentIndex = 0
private val timer = MSTimer()
private var delay = 0L

private fun getModule(name: String) = ModuleManager.getModule(name)

private fun disableAllModules() {
    for (moduleName in moduleList) {
        getModule(moduleName)?.state = false
    }
}

private fun enableNextModule() {
    disableAllModules()
    
    if (currentIndex >= moduleList.size) {
        currentIndex = 0
    }
    
    val currentModule = getModule(moduleList[currentIndex])
    currentModule?.state = true
    
    delay = ThreadLocalRandom.current().nextLong(0, 1000)
    timer.reset()
    currentIndex++
}

override fun onEnable() {
    currentIndex = 0
    enableNextModule()
}

override fun onDisable() {
    disableAllModules()
}

val onGameLoop = handler<GameLoopEvent> {
    if (!state) return@handler
    
    if (timer.hasTimePassed(delay)) {
        enableNextModule()
    }
}
}