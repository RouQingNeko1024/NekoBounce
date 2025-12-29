/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.file.configs

import com.google.gson.JsonObject
import io.qzz.nekobounce.NekoBounce.clickGui
import io.qzz.nekobounce.file.FileConfig
import io.qzz.nekobounce.file.FileManager.PRETTY_GSON
import io.qzz.nekobounce.ui.client.clickgui.ClickGui
import io.qzz.nekobounce.ui.client.clickgui.elements.ModuleElement
import io.qzz.nekobounce.utils.client.ClientUtils.LOGGER
import io.qzz.nekobounce.utils.io.readJson
import java.io.*

class ClickGuiConfig(file: File) : FileConfig(file) {

    override fun loadDefault() = ClickGui.setDefault()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        // Regenerate panels and elements in case a script got loaded or removed.
        loadDefault()

        val json = file.readJson() as? JsonObject ?: return
        for (panel in clickGui.panels) {
            if (!json.has(panel.name)) continue
            try {
                val panelObject = json.getAsJsonObject(panel.name)
                panel.open = panelObject["open"].asBoolean
                panel.isVisible = panelObject["visible"].asBoolean
                panel.x = panelObject["posX"].asInt
                panel.y = panelObject["posY"].asInt

                for (element in panel.elements) {
                    if (element !is ModuleElement) continue
                    if (!panelObject.has(element.module.name)) continue
                    try {
                        val elementObject = panelObject.getAsJsonObject(element.module.name)
                        element.showSettings = elementObject["Settings"].asBoolean
                    } catch (e: Exception) {
                        LOGGER.error(
                            "Error while loading clickgui module element with the name '" + element.module.getName() + "' (Panel Name: " + panel.name + ").",
                            e
                        )
                    }
                }
            } catch (e: Exception) {
                LOGGER.error("Error while loading clickgui panel with the name '" + panel.name + "'.", e)
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonObject = JsonObject()

        for (panel in clickGui.panels) {
            val panelObject = JsonObject()
            panelObject.run {
                addProperty("open", panel.open)
                addProperty("visible", panel.isVisible)
                addProperty("posX", panel.x)
                addProperty("posY", panel.y)
            }
            for (element in panel.elements) {
                if (element !is ModuleElement) continue
                val elementObject = JsonObject()
                elementObject.addProperty("Settings", element.showSettings)
                panelObject.add(element.module.name, elementObject)
            }
            jsonObject.add(panel.name, panelObject)
        }

        file.writeText(PRETTY_GSON.toJson(jsonObject))
    }
}