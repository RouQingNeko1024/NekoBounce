/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui

import net.ccbluex.liquidbounce.features.module.modules.render.CustomCrosshair
import net.minecraft.client.gui.GuiIngame
import net.minecraft.client.gui.ScaledResolution
import org.spongepowered.asm.mixin.Mixin
import org.spongepowered.asm.mixin.injection.At
import org.spongepowered.asm.mixin.injection.Inject
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo

@Mixin(GuiIngame::class)
class MixinGuiIngame {
    
    @Inject(method = ["renderCrosshair"], at = [At("HEAD")], cancellable = true)
    private fun hideVanillaCrosshair(scaledResolution: ScaledResolution, callbackInfo: CallbackInfo) {
        if (CustomCrosshair.state && CustomCrosshair.hideVanillaCrosshair) {
            callbackInfo.cancel()
        }
    }
}