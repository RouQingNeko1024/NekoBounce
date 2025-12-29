/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object NoFOV : Module("NoFOV", Category.RENDER, gameDetecting = false) {
    val fov by float("FOV", 1f, 0f..1.5f)
}
