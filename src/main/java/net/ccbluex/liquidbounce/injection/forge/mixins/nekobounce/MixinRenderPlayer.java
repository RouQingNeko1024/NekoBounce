package net.ccbluex.liquidbounce.injection.forge.mixins.nekobounce;

import net.ccbluex.liquidbounce.features.module.modules.render.MMDModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderPlayer.class)
public class MixinRenderPlayer {
    
    @Inject(method = "doRender(Lnet/minecraft/client/entity/AbstractClientPlayer;DDDFF)V", at = @At("HEAD"), cancellable = true)
    public void onDoRender(AbstractClientPlayer entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        // 当MMDModel模块启用且渲染的是当前玩家时，取消原版渲染
        if (MMDModel.INSTANCE.getState() && entity == Minecraft.getMinecraft().thePlayer) {
            ci.cancel();
        }
    }
}