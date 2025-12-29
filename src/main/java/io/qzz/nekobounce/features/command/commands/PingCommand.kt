/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.command.commands

import io.qzz.nekobounce.features.command.Command

object PingCommand : Command("ping") {

    override fun execute(args: Array<String>) {
        chat("§3Your ping is §a${mc.netHandler.getPlayerInfo(mc.thePlayer.uniqueID).responseTime}ms§3.")
    }

}