/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.file.configs

import com.google.gson.JsonObject
import io.qzz.nekobounce.NekoBounce
import io.qzz.nekobounce.NekoBounce.commandManager
import io.qzz.nekobounce.NekoBounce.moduleManager
import io.qzz.nekobounce.cape.CapeService
import io.qzz.nekobounce.features.module.modules.misc.LiquidChat.jwtToken
import io.qzz.nekobounce.features.special.ClientFixes
import io.qzz.nekobounce.features.special.ClientRichPresence
import io.qzz.nekobounce.file.FileConfig
import io.qzz.nekobounce.file.FileManager
import io.qzz.nekobounce.file.FileManager.PRETTY_GSON
import io.qzz.nekobounce.file.configs.models.ClientConfiguration
import io.qzz.nekobounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening.Companion.apiKey
import io.qzz.nekobounce.utils.attack.EntityUtils.Targets
import io.qzz.nekobounce.utils.io.readJson
import java.io.*

class ValuesConfig(file: File) : FileConfig(file) {

    private var lastWarningTime: Long = 0L

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val json = file.readJson() as? JsonObject ?: return

        val prevVersion = json["ClientVersion"]?.asString ?: "unknown"
        // Compare the versions
        if (prevVersion != NekoBounce.clientVersionText) {
            // Run backup
            FileManager.backupAllConfigs(prevVersion, NekoBounce.clientVersionText)
        }

        for ((key, value) in json.entrySet()) {
            when {
                key.equals("CommandPrefix", true) -> {
                    commandManager.prefix = value.asString
                }

                key.equals(ClientRichPresence.name, true) -> {
                    ClientRichPresence.fromJson(value)
                }

                key.equals(Targets.name, true) -> {
                    Targets.fromJson(value)
                }

                key.equals(ClientFixes.name, true) -> {
                    ClientFixes.fromJson(value)
                }

                key.equals("thealtening", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("API-Key")) apiKey = jsonValue["API-Key"].asString
                }

                key.equals("liquidchat", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("token")) jwtToken = jsonValue["token"].asString
                }

                key.equals("DonatorCape", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("TransferCode")) {
                        CapeService.knownToken = jsonValue["TransferCode"].asString
                    }
                }

                key.equals(ClientConfiguration.name, true) -> {
                    ClientConfiguration.fromJson(value)
                }

                // Deprecated
                // Compatibility with old versions
                key.equals("background", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("Enabled")) ClientConfiguration.customBackground = jsonValue["Enabled"].asBoolean
                    if (jsonValue.has("Particles")) ClientConfiguration.particles = jsonValue["Particles"].asBoolean
                }

                key.equals("popup", true) -> {
                    val jsonValue = value as JsonObject
                    if (jsonValue.has("lastWarningTime")) lastWarningTime = jsonValue["lastWarningTime"].asLong
                }

                else -> {
                    val module = moduleManager[key] ?: continue

                    val jsonModule = value as JsonObject
                    for (moduleValue in module.values) {
                        val element = jsonModule[moduleValue.name]
                        if (element != null) {
                            moduleValue.fromJson(element)
                        }
                    }
                }
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
        jsonObject.run {
            addProperty("CommandPrefix", commandManager.prefix)
            addProperty("ClientVersion", NekoBounce.clientVersionText)
        }

        jsonObject.add(ClientRichPresence.name, ClientRichPresence.toJson())

        jsonObject.add(Targets.name, Targets.toJson())

        jsonObject.add(ClientFixes.name, ClientFixes.toJson())

        val theAlteningObject = JsonObject()
        theAlteningObject.addProperty("API-Key", apiKey)
        jsonObject.add("thealtening", theAlteningObject)

        val liquidChatObject = JsonObject()
        liquidChatObject.addProperty("token", jwtToken)
        jsonObject.add("liquidchat", liquidChatObject)

        val capeObject = JsonObject()
        capeObject.addProperty("TransferCode", CapeService.knownToken)
        jsonObject.add("DonatorCape", capeObject)

        jsonObject.add(ClientConfiguration.name, ClientConfiguration.toJson())

        for (module in moduleManager) {
            if (module.values.isEmpty()) continue

            val jsonModule = JsonObject()
            for (value in module.values) jsonModule.add(value.name, value.toJson())
            jsonObject.add(module.name, jsonModule)
        }

        val popupData = JsonObject()
        popupData.addProperty("lastWarningTime", lastWarningTime)
        jsonObject.add("popup", popupData)

        file.writeText(PRETTY_GSON.toJson(jsonObject))
    }
}