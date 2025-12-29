/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.misc

import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module

object OverrideRaycast : Module("OverrideRaycast", Category.MISC, gameDetecting = false) {
    private val alwaysActive by boolean("AlwaysActive", true)

    fun shouldOverride() = handleEvents() || alwaysActive
}