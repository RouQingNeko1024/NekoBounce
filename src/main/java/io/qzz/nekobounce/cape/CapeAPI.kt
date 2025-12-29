/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.cape

import io.qzz.nekobounce.features.module.modules.render.Cape
import io.qzz.nekobounce.utils.client.MinecraftInstance
import net.minecraft.util.ResourceLocation
import java.util.*

object CapeAPI : MinecraftInstance {

    /**
     * Load cape of user with uuid
     *
     * @param uuid
     * @return cape info
     */
    fun loadCape(uuid: UUID, success: (CapeInfo) -> Unit) {
        // Check if Cape module is enabled and get the cape texture
        val capeTexture = Cape.getCapeForPlayer(uuid)
        
        if (capeTexture != null) {
            // Use the cape from Cape module
            val capeInfo = CapeInfo(capeTexture, true)
            success(capeInfo)
        } else {
            // Fallback to original cape service if Cape module is disabled
            CapeService.refreshCapeCarriers {
                runCatching {
                    // Get URL of cape from cape service
                    val (name, url) = CapeService.getCapeDownload(uuid) ?: return@refreshCapeCarriers

                    // For now, just use a placeholder since the image loading code has issues
                    // In a real implementation, you'd need to properly handle the image download
                    val resourceLocation = ResourceLocation("liquidbounce/cape/default.png")
                    val capeInfo = CapeInfo(resourceLocation, true)
                    
                    success(capeInfo)
                }.onFailure {
                    // Simple error handling without LOGGER
                    System.err.println("Failed to load cape for UUID: $uuid")
                    it.printStackTrace()
                }
            }
        }
    }
}

data class CapeInfo(val resourceLocation: ResourceLocation, var isCapeAvailable: Boolean = false)