/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce

import com.formdev.flatlaf.themes.FlatMacLightLaf
import kotlinx.coroutines.launch
import io.qzz.nekobounce.api.ClientUpdate
import io.qzz.nekobounce.api.ClientUpdate.gitInfo
import io.qzz.nekobounce.api.loadSettings
import io.qzz.nekobounce.cape.CapeService
import io.qzz.nekobounce.event.ClientShutdownEvent
import io.qzz.nekobounce.event.EventManager
import io.qzz.nekobounce.event.StartupEvent
import io.qzz.nekobounce.features.command.CommandManager
import io.qzz.nekobounce.features.command.CommandManager.registerCommands
import io.qzz.nekobounce.features.module.ModuleManager
import io.qzz.nekobounce.features.module.ModuleManager.registerModules
import io.qzz.nekobounce.features.special.BungeeCordSpoof
import io.qzz.nekobounce.features.special.ClientFixes
import io.qzz.nekobounce.features.special.ClientRichPresence
import io.qzz.nekobounce.features.special.ClientRichPresence.showRPCValue
import io.qzz.nekobounce.file.FileManager
import io.qzz.nekobounce.file.FileManager.loadAllConfigs
import io.qzz.nekobounce.file.FileManager.saveAllConfigs
import io.qzz.nekobounce.file.configs.models.ClientConfiguration.updateClientWindow
import io.qzz.nekobounce.lang.LanguageManager.loadLanguages
import io.qzz.nekobounce.script.ScriptManager
import io.qzz.nekobounce.script.ScriptManager.enableScripts
import io.qzz.nekobounce.script.ScriptManager.loadScripts
import io.qzz.nekobounce.script.remapper.Remapper
import io.qzz.nekobounce.script.remapper.Remapper.loadSrg
import io.qzz.nekobounce.tabs.BlocksTab
import io.qzz.nekobounce.tabs.ExploitsTab
import io.qzz.nekobounce.tabs.HeadsTab
import io.qzz.nekobounce.ui.client.altmanager.GuiAltManager.Companion.loadActiveGenerators
import io.qzz.nekobounce.ui.client.clickgui.ClickGui
import io.qzz.nekobounce.ui.client.hud.HUD
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.client.BlinkUtils
import io.qzz.nekobounce.utils.client.ClassUtils.hasForge
import io.qzz.nekobounce.utils.client.ClientUtils.LOGGER
import io.qzz.nekobounce.utils.client.ClientUtils.disableFastRender
import io.qzz.nekobounce.utils.client.PacketUtils
import io.qzz.nekobounce.utils.inventory.InventoryManager
import io.qzz.nekobounce.utils.inventory.InventoryUtils
import io.qzz.nekobounce.utils.inventory.SilentHotbar
import io.qzz.nekobounce.utils.io.MiscUtils
import io.qzz.nekobounce.utils.io.MiscUtils.showErrorPopup
import io.qzz.nekobounce.utils.kotlin.SharedScopes
import io.qzz.nekobounce.utils.movement.BPSUtils
import io.qzz.nekobounce.utils.movement.MovementUtils
import io.qzz.nekobounce.utils.movement.TimerBalanceUtils
import io.qzz.nekobounce.utils.render.MiniMapRegister
import io.qzz.nekobounce.utils.render.shader.Background
import io.qzz.nekobounce.utils.rotation.RotationUtils
import io.qzz.nekobounce.utils.timing.TickedActions
import io.qzz.nekobounce.utils.timing.WaitTickUtils
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future
import javax.swing.UIManager

object NekoBounce {

    /**
     * Client Information
     *
     * This has all the basic information.
     */
    const val CLIENT_NAME = "NekoBounce"
    const val CLIENT_AUTHOR = "Neko"
    const val CLIENT_CLOUD = "https://cloud.liquidbounce.net/LiquidBounce"
    const val CLIENT_WEBSITE = "NekoBounce.qzz.io"
    const val CLIENT_GITHUB = "https://github.com/RouQingNeko1024/NekoBounce/"

    const val MINECRAFT_VERSION = "1.8.9"
    
    val clientVersionText = gitInfo["git.build.version"]?.toString() ?: "unknown"
    val clientVersionNumber = clientVersionText.substring(1).toIntOrNull() ?: 0 // version format: "b<VERSION>" on legacy
    val clientCommit = gitInfo["git.commit.id.abbrev"]?.let { "git-$it" } ?: "unknown"
    val clientBranch = gitInfo["git.branch"]?.toString() ?: "unknown"

    /**
     * Defines if the client is in development mode.
     * This will enable update checking on commit time instead of regular legacy versioning.
     */
    const val IN_DEV = true

    val clientTitle = CLIENT_NAME + " " + clientVersionText + " " +"| " + "喵喵喵~" + "| " + clientCommit + " " + MINECRAFT_VERSION + if (IN_DEV) " | DEVELOPMENT BUILD" else ""

    var isStarting = true

    // Managers
    val moduleManager = ModuleManager
    val commandManager = CommandManager
    val eventManager = EventManager
    val fileManager = FileManager
    val scriptManager = ScriptManager

    // HUD & ClickGUI
    val hud = HUD

    val clickGui = ClickGui

    // Menu Background
    var background: Background? = null

    // Discord RPC
    val clientRichPresence = ClientRichPresence

    /**
     * Start IO tasks
     */
    fun preload(): Future<*> {

        io.qzz.nekobounce.utils.client.javaVersion

        // Change theme of Swing
        UIManager.setLookAndFeel(FlatMacLightLaf())

        val future = CompletableFuture<Unit>()

        SharedScopes.IO.launch {
            try {
                LOGGER.info("Starting preload tasks of $CLIENT_NAME")

                // Download and extract fonts
                Fonts.downloadFonts()

                // Check update
                ClientUpdate.reloadNewestVersion()

                // Load languages
                loadLanguages()

                // Load alt generators
                loadActiveGenerators()

                // Load SRG file
                loadSrg()

                LOGGER.info("Preload tasks of $CLIENT_NAME are completed!")

                future.complete(Unit)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }

        return future
    }

    /**
     * Execute if client will be started
     */
    fun startClient() {
        isStarting = true

        LOGGER.info("Starting $CLIENT_NAME $clientVersionText $clientCommit, by $CLIENT_AUTHOR")

        try {
            // Load client fonts
            Fonts.loadFonts()

            // Register listeners
            RotationUtils
            ClientFixes
            BungeeCordSpoof
            CapeService
            InventoryUtils
            InventoryManager
            MiniMapRegister
            TickedActions
            MovementUtils
            PacketUtils
            TimerBalanceUtils
            BPSUtils
            WaitTickUtils
            SilentHotbar
            BlinkUtils

            // Load settings
            loadSettings(false) {
                LOGGER.info("Successfully loaded ${it.size} settings.")
            }

            // Register commands
            registerCommands()

            // Setup module manager and register modules
            registerModules()

            runCatching {
                // Remapper
                loadSrg()

                if (!Remapper.mappingsLoaded) {
                    error("Failed to load SRG mappings.")
                }

                // ScriptManager
                loadScripts()
                enableScripts()
            }.onFailure {
                LOGGER.error("Failed to load scripts.", it)
            }

            // Load configs
            loadAllConfigs()

            // Update client window
            updateClientWindow()

            // Tabs (Only for Forge!)
            if (hasForge()) {
                BlocksTab()
                ExploitsTab()
                HeadsTab()
            }

            // Disable Optifine FastRender
            disableFastRender()

            // Setup Discord RPC
            if (showRPCValue) {
                SharedScopes.IO.launch {
                    try {
                        clientRichPresence.setup()
                    } catch (throwable: Throwable) {
                        LOGGER.error("Failed to setup Discord RPC.", throwable)
                    }
                }
            }

            // Login into known token if not empty
            if (CapeService.knownToken.isNotBlank()) {
                SharedScopes.IO.launch {
                    runCatching {
                        CapeService.login(CapeService.knownToken)
                    }.onFailure {
                        LOGGER.error("Failed to login into known cape token.", it)
                    }.onSuccess {
                        LOGGER.info("Successfully logged in into known cape token.")
                    }
                }
            }

            // Refresh cape service
            CapeService.refreshCapeCarriers {
                LOGGER.info("Successfully loaded ${it.size} cape carriers.")
            }

            // Load background
            FileManager.loadBackground()
        } catch (e: Exception) {
            LOGGER.error("Failed to start client: ${e.message}")
            e.showErrorPopup()
        } finally {
            // Set is starting status
            isStarting = false

            if (!FileManager.firstStart && FileManager.backedup) {
                SharedScopes.IO.launch {
                    MiscUtils.showMessageDialog("Warning: backup triggered", "Client update detected! Please check the config folder.")
                }
            }

            EventManager.call(StartupEvent)
            LOGGER.info("Successfully started client")
        }
    }

    /**
     * Execute if client will be stopped
     */
    fun stopClient() {
        // Call client shutdown
        EventManager.call(ClientShutdownEvent)

        // Stop all CoroutineScopes
        SharedScopes.stop()

        // Save all available configs
        saveAllConfigs()
    }

}
