/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.command.commands

import io.qzz.nekobounce.NekoBounce.isStarting
import io.qzz.nekobounce.NekoBounce.moduleManager
import io.qzz.nekobounce.features.command.Command
import io.qzz.nekobounce.features.command.CommandManager
import io.qzz.nekobounce.file.FileManager.accountsConfig
import io.qzz.nekobounce.file.FileManager.clickGuiConfig
import io.qzz.nekobounce.file.FileManager.friendsConfig
import io.qzz.nekobounce.file.FileManager.hudConfig
import io.qzz.nekobounce.file.FileManager.loadConfig
import io.qzz.nekobounce.file.FileManager.modulesConfig
import io.qzz.nekobounce.file.FileManager.valuesConfig
import io.qzz.nekobounce.file.FileManager.xrayConfig
import io.qzz.nekobounce.script.ScriptManager.disableScripts
import io.qzz.nekobounce.script.ScriptManager.reloadScripts
import io.qzz.nekobounce.script.ScriptManager.unloadScripts
import io.qzz.nekobounce.ui.font.Fonts

object ReloadCommand : Command("reload", "configreload") {
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        chat("Reloading...")
        isStarting = true

        chat("§c§lReloading commands...")
        CommandManager.registerCommands()

        disableScripts()
        unloadScripts()

        for (module in moduleManager)
            moduleManager.generateCommand(module)

        chat("§c§lReloading scripts...")
        reloadScripts()

        chat("§c§lReloading fonts...")
        Fonts.loadFonts()

        chat("§c§lReloading modules...")
        loadConfig(modulesConfig)


        chat("§c§lReloading values...")
        loadConfig(valuesConfig)

        chat("§c§lReloading accounts...")
        loadConfig(accountsConfig)

        chat("§c§lReloading friends...")
        loadConfig(friendsConfig)

        chat("§c§lReloading xray...")
        loadConfig(xrayConfig)

        chat("§c§lReloading HUD...")
        loadConfig(hudConfig)

        chat("§c§lReloading ClickGUI...")
        loadConfig(clickGuiConfig)

        isStarting = false
        chat("Reloaded.")
    }
}
