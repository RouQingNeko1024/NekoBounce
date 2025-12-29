/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.event.ClientShutdownEvent
import io.qzz.nekobounce.event.UpdateEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import net.minecraft.potion.Potion
import net.minecraft.potion.PotionEffect

object Fullbright : Module("Fullbright", Category.RENDER, gameDetecting = false) {
    private val mode by choices("Mode", arrayOf("Gamma", "NightVision"), "Gamma")
    private var prevGamma = -1f

    override fun onEnable() {
        prevGamma = mc.gameSettings.gammaSetting
    }

    override fun onDisable() {
        if (prevGamma == -1f)
            return

        mc.gameSettings.gammaSetting = prevGamma
        prevGamma = -1f

        mc.thePlayer?.removePotionEffectClient(Potion.nightVision.id)
    }

    val onUpdate = handler<UpdateEvent>(always = true) {
        if (state || XRay.handleEvents()) {
            when (mode.lowercase()) {
                "gamma" -> when {
                    mc.gameSettings.gammaSetting <= 100f -> mc.gameSettings.gammaSetting++
                }

                "nightvision" -> mc.thePlayer?.addPotionEffect(PotionEffect(Potion.nightVision.id, 1337, 1))
            }
        } else if (prevGamma != -1f) {
            mc.gameSettings.gammaSetting = prevGamma
            prevGamma = -1f
        }
    }

    val onShutdown = handler<ClientShutdownEvent>(always = true) {
        onDisable()
    }
}