/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.injection.forge.mixins.gui;

import io.qzz.nekobounce.NekoBounce;
import io.qzz.nekobounce.event.EventManager;
import io.qzz.nekobounce.event.Render2DEvent;
import io.qzz.nekobounce.features.module.modules.render.AntiBlind;
import io.qzz.nekobounce.features.module.modules.render.HUD;
import io.qzz.nekobounce.features.module.modules.render.SilentHotbarModule;
import io.qzz.nekobounce.ui.font.AWTFontRenderer;
import io.qzz.nekobounce.utils.client.ClassUtils;
import io.qzz.nekobounce.utils.inventory.SilentHotbar;
import io.qzz.nekobounce.utils.inventory.InventoryUtils;
import io.qzz.nekobounce.utils.render.ColorSettingsKt;
import io.qzz.nekobounce.utils.render.RenderUtils;
import io.qzz.nekobounce.utils.render.shader.shaders.GradientShader;
import io.qzz.nekobounce.utils.render.shader.shaders.RainbowShader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiIngame;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static net.minecraft.client.renderer.GlStateManager.*;
import static org.lwjgl.opengl.GL11.*;

@Mixin(GuiIngame.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiInGame extends MixinGui {

    @Shadow
    protected abstract void renderHotbarItem(int index, int xPos, int yPos, float partialTicks, EntityPlayer player);

    @Shadow
    @Final
    protected Minecraft mc;

    @Shadow
    @Final
    protected static ResourceLocation widgetsTexPath;

    @Inject(method = "renderScoreboard", at = @At("HEAD"), cancellable = true)
    private void renderScoreboard(CallbackInfo callbackInfo) {
        if (HUD.INSTANCE.handleEvents())
            callbackInfo.cancel();
    }

    @Redirect(method = "updateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"))
    private ItemStack hookSilentHotbarHighlightedName(InventoryPlayer instance) {
        SilentHotbarModule module = SilentHotbarModule.INSTANCE;

        int slot = SilentHotbar.INSTANCE.renderSlot(module.handleEvents() && module.getKeepHighlightedName());

        return instance.getStackInSlot(slot);
    }

    @Inject(method = "renderTooltip", at = @At("HEAD"), cancellable = true)
    private void injectCustomHotbar(ScaledResolution sr, float partialTicks, CallbackInfo ci) {
        final HUD hud = HUD.INSTANCE;
        final RenderUtils render = RenderUtils.INSTANCE;

        if(Minecraft.getMinecraft().getRenderViewEntity() instanceof EntityPlayer && hud.getState() && (hud.getBlackHotbarValue() || hud.getAnimHotbarValue())) {
            final Minecraft mc = Minecraft.getMinecraft();
            EntityPlayer entityPlayer = (EntityPlayer) mc.getRenderViewEntity();

            boolean blackHB = hud.getBlackHotbarValue();
            int middleScreen = sr.getScaledWidth() / 2;
            float posInv = hud.getAnimPos(entityPlayer.inventory.currentItem * 20F);

            GlStateManager.resetColor();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            mc.getTextureManager().bindTexture(widgetsTexPath);

            float f = this.zLevel;
            this.zLevel = -90.0F;
            GlStateManager.resetColor();

            if (blackHB) {
                RenderUtils.originalRoundedRect(middleScreen - 91, sr.getScaledHeight() - 2, middleScreen + 91, sr.getScaledHeight() - 22, 3F, Integer.MIN_VALUE);
                RenderUtils.originalRoundedRect(middleScreen - 91 + posInv, sr.getScaledHeight() - 2, middleScreen - 91 + posInv + 22, sr.getScaledHeight() - 22, 3F, Integer.MAX_VALUE);
            } else {
                this.drawTexturedModalRect(middleScreen - 91F, sr.getScaledHeight() - 22, 0, 0, 182, 22);
                this.drawTexturedModalRect(middleScreen - 91F + posInv - 1, sr.getScaledHeight() - 22 - 1, 0, 22, 24, 22);
            }

            this.zLevel = f;
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            RenderHelper.enableGUIStandardItemLighting();

            for (int j = 0; j < 9; ++j) {
                int k = sr.getScaledWidth() / 2 - 90 + j * 20 + 2;
                int l = sr.getScaledHeight() - 19 - (blackHB ? 1 : 0);
                this.renderHotbarItem(j, k, l, partialTicks, entityPlayer);
            }

            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.disableBlend();
            GlStateManager.resetColor();
            ci.cancel();
        }

        liquidBounce$injectRender2DEvent(partialTicks);
    }

    @Inject(method = "renderPumpkinOverlay", at = @At("HEAD"), cancellable = true)
    private void renderPumpkinOverlay(final CallbackInfo callbackInfo) {
        final AntiBlind antiBlind = AntiBlind.INSTANCE;

        if (antiBlind.handleEvents() && antiBlind.getPumpkinEffect())
            callbackInfo.cancel();
    }

    @Inject(method = "renderBossHealth", at = @At("HEAD"), cancellable = true)
    private void renderBossHealth(CallbackInfo callbackInfo) {
        final AntiBlind antiBlind = AntiBlind.INSTANCE;

        if (antiBlind.handleEvents() && antiBlind.getBossHealth())
            callbackInfo.cancel();
    }

    @Unique
    private void liquidBounce$injectRender2DEvent(float delta) {
        if (!ClassUtils.INSTANCE.hasClass("net.labymod.api.LabyModAPI")) {
            EventManager.INSTANCE.call(new Render2DEvent(delta));
        }
    }
}
