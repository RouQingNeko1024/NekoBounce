/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.misc

import io.qzz.nekobounce.event.Render2DEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.file.FileManager.friendsConfig
import io.qzz.nekobounce.file.FileManager.saveConfig
import io.qzz.nekobounce.utils.client.chat
import io.qzz.nekobounce.utils.render.ColorUtils.stripColor
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.input.Mouse

object MidClick : Module("MidClick", Category.MISC, subjective = true, gameDetecting = false) {
    private var wasDown = false

    val onRender = handler<Render2DEvent> {
        if (mc.currentScreen != null)
            return@handler

        if (!wasDown && Mouse.isButtonDown(2)) {
            val entity = mc.objectMouseOver.entityHit

            if (entity is EntityPlayer) {
                val playerName = stripColor(entity.name)

                if (!friendsConfig.isFriend(playerName)) {
                    friendsConfig.addFriend(playerName)
                    saveConfig(friendsConfig)
                    chat("§a§l$playerName§c was added to your friends.")
                } else {
                    friendsConfig.removeFriend(playerName)
                    saveConfig(friendsConfig)
                    chat("§a§l$playerName§c was removed from your friends.")
                }

            } else
                chat("§c§lError: §aYou need to select a player.")
        }
        wasDown = Mouse.isButtonDown(2)
    }
}