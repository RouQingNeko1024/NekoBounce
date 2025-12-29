/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.render

import io.qzz.nekobounce.NekoBounce.clickGui
import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.ui.client.clickgui.ClickGui
import io.qzz.nekobounce.ui.client.clickgui.style.styles.BlackStyle
import io.qzz.nekobounce.ui.client.clickgui.style.styles.LiquidBounceStyle
import io.qzz.nekobounce.ui.client.clickgui.style.styles.NullStyle
import io.qzz.nekobounce.ui.client.clickgui.style.styles.SlowlyStyle
import io.qzz.nekobounce.ui.client.clickgui.style.styles.AugustusStyle
import net.minecraft.network.play.server.S2EPacketCloseWindow
import org.lwjgl.input.Keyboard
import java.awt.Color

object ClickGUI : Module("ClickGUI", Category.RENDER, Keyboard.KEY_RSHIFT, canBeEnabled = false) {
    private val style by choices(
        "Style",
        arrayOf("NekoBounce", "Null", "Slowly", "Black","Augustus"),
        "NekoBounce"
    ).onChanged {
        updateStyle()
    }
    var scale by float("Scale", 0.8f, 0.5f..1.5f)
    val maxElements by int("MaxElements", 15, 1..30)
    val fadeSpeed by float("FadeSpeed", 1f, 0.5f..4f)
    val scrolls by boolean("Scrolls", true)
    val spacedModules by boolean("SpacedModules", false)
    val panelsForcedInBoundaries by boolean("PanelsForcedInBoundaries", false)

    private val color by color("Color", Color(0, 160, 255)) { style !in arrayOf("Slowly", "Black") }

    val guiColor
        get() = color.rgb

    override fun onEnable() {
        updateStyle()
        mc.displayGuiScreen(clickGui)
        Keyboard.enableRepeatEvents(true)
    }

    private fun updateStyle() {
        clickGui.style = when (style) {
            "NekoBounce" -> LiquidBounceStyle
            "Null" -> NullStyle
            "Slowly" -> SlowlyStyle
            "Black" -> BlackStyle 
            "Augustus" -> AugustusStyle
            else -> return
        }
    }

    val onPacket = handler<PacketEvent>(always = true) { event ->
        if (event.packet is S2EPacketCloseWindow && mc.currentScreen is ClickGui) {
            event.cancelEvent()
        }
    }
}
