/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.ui.client.altmanager.menus

import me.liuli.elixir.account.MinecraftAccount
import io.qzz.nekobounce.lang.translationText
import io.qzz.nekobounce.ui.client.altmanager.GuiAltManager.Companion.login
import io.qzz.nekobounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import io.qzz.nekobounce.utils.render.RenderUtils.drawLoadingCircle
import io.qzz.nekobounce.utils.ui.AbstractScreen

class GuiLoginProgress(
    minecraftAccount: MinecraftAccount,
    success: () -> Unit,
    error: (Exception) -> Unit,
    done: () -> Unit
) : AbstractScreen() {

    init {
        login(minecraftAccount, success, error, done)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        assumeNonVolatile {
            drawDefaultBackground()
            drawLoadingCircle(width / 2f, height / 4f + 70)
            drawCenteredString(fontRendererObj, translationText(
                "Loggingintoaccount"), width / 2, height / 2 - 60, 16777215)
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

}