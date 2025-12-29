/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement

import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.aac.AAC
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.aac.LAAC
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.intave.IntaveNew
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.intave.IntaveOld
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other.None
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other.Matrix
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other.Matrix2
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other.OldGrim
import io.qzz.nekobounce.features.module.modules.movement.nowebmodes.other.Rewi

object NoWeb : Module("NoWeb", Category.MOVEMENT) {

    private val noWebModes = arrayOf(
        // Vanilla
        None,

        // AAC
        AAC, LAAC,

        // Intave
        IntaveOld,
        IntaveNew,

        // Other
        Rewi,
        OldGrim,
        Matrix,
        Matrix2
    )

    private val modes = noWebModes.map { it.modeName }.toTypedArray()

    val mode by choices(
        "Mode", modes, "None"
    )

    val onUpdate = handler<UpdateEvent> {
        modeModule.onUpdate()
    }

    override val tag
        get() = mode

    private val modeModule
        get() = noWebModes.find { it.modeName == mode }!!
}
