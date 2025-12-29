/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.command.shortcuts

import io.qzz.nekobounce.features.command.Command

class Shortcut(val name: String, val script: List<Pair<Command, Array<String>>>) : Command(name) {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) = script.forEach { it.first.execute(it.second) }
}
