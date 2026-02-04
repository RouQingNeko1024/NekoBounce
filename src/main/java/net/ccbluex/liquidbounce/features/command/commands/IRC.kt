package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.IRC

class IRCCommand : Command("irc") {
    override fun execute(args: Array<String>) {
        IRC.handleCommand(args)
    }
}