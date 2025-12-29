/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.world

import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object NoSlowBreak : Module("NoSlowBreak", Category.WORLD, gameDetecting = false) {
    val air by boolean("Air", true)
    val water by boolean("Water", false)
}
