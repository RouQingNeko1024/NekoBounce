/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.command.commands

import io.qzz.nekobounce.features.command.Command
import io.qzz.nekobounce.utils.movement.MovementUtils.forward

object HClipCommand : Command("hclip") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            try {
                forward(args[1].toDouble())
                chat("You were teleported.")
            } catch (exception: NumberFormatException) {
                chatSyntaxError()
            }
            return
        }

        chatSyntax("hclip <value>")
    }
}