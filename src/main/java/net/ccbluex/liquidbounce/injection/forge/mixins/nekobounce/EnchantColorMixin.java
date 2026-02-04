// EnchantColorMixin.java
package net.ccbluex.liquidbounce.injection.forge.mixins.nekobounce;

import net.ccbluex.liquidbounce.features.module.modules.render.EnchantColor;
import net.minecraft.client.renderer.entity.RenderItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;
import java.awt.Color;

@Mixin(RenderItem.class)
public class EnchantColorMixin {
    
    @ModifyArgs(
        method = "renderEffect",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"
        )
    )
    private void modifyEnchantColor(Args args) {
        if (EnchantColor.INSTANCE.getState()) {
            Color color = EnchantColor.INSTANCE.getEnchantRGB();
            args.set(0, color.getRed() / 255.0f);
            args.set(1, color.getGreen() / 255.0f);
            args.set(2, color.getBlue() / 255.0f);
        }
    }
}