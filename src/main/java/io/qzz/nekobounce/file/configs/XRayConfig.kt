/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.file.configs

import com.google.gson.JsonArray
import io.qzz.nekobounce.features.module.modules.render.XRay
import io.qzz.nekobounce.file.FileConfig
import io.qzz.nekobounce.utils.block.blockById
import io.qzz.nekobounce.utils.block.id
import io.qzz.nekobounce.utils.io.readJson
import io.qzz.nekobounce.utils.io.writeJson
import net.minecraft.init.Blocks
import java.io.*

class XRayConfig(file: File) : FileConfig(file) {

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig() {
        val json = file.readJson() as? JsonArray ?: return

        XRay.xrayBlocks.clear()

        json.mapNotNullTo(XRay.xrayBlocks) {
            it.asInt.blockById.takeIf { b -> b != Blocks.air }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig() {
        file.writeJson(XRay.xrayBlocks.map { it.id }.sorted())
    }
}