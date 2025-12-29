/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.file.configs

import com.google.gson.JsonObject
import io.qzz.nekobounce.NekoBounce.moduleManager
import io.qzz.nekobounce.file.FileConfig
import io.qzz.nekobounce.file.FileManager.PRETTY_GSON
import io.qzz.nekobounce.utils.io.json
import io.qzz.nekobounce.utils.io.readJson
import java.io.*

class ModulesConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val json = file.readJson() as? JsonObject ?: return

        for ((key, value) in json.entrySet()) {
            val module = moduleManager[key] ?: continue

            val jsonModule = value as JsonObject
            module.state = jsonModule["State"].asBoolean
            module.keyBind = jsonModule["KeyBind"].asInt
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        val jsonObject = json {
            for (module in moduleManager) {
                module.name to json {
                    "State" to module.state
                    "KeyBind" to module.keyBind
                }
            }
        }
        file.writeText(PRETTY_GSON.toJson(jsonObject))
    }
}