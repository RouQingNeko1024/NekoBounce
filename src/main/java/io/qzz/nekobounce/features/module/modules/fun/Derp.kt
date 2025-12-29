/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.`fun`

import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.kotlin.RandomUtils.nextFloat
import io.qzz.nekobounce.utils.rotation.Rotation
import io.qzz.nekobounce.utils.rotation.RotationUtils.currentRotation
import io.qzz.nekobounce.utils.rotation.RotationUtils.serverRotation
import io.qzz.nekobounce.utils.rotation.RotationUtils.syncSpecialModuleRotations

object Derp : Module("Derp", Category.FUN, subjective = true) {

    private val headless by boolean("Headless", false)
    private val spinny by boolean("Spinny", false)
    private val increment by float("Increment", 1F, 0F..50F) { spinny }

    override fun onDisable() {
        syncSpecialModuleRotations()
    }

    val rotation: Rotation
        get() {
            val rotationToUse = currentRotation ?: serverRotation

            val rot = Rotation(rotationToUse.yaw, nextFloat(-90f, 90f))

            if (headless)
                rot.pitch = 180F

            rot.yaw += if (spinny) increment else nextFloat(-180f, 180f)

            return rot.fixedSensitivity()
        }

}