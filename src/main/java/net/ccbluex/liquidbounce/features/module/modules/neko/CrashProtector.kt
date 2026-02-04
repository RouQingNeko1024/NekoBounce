/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.server.S0FPacketSpawnMob
import net.minecraft.network.play.server.S3EPacketTeams

object CrashProtector : Module("CrashProtector", Category.NEKO) {

    private val blockSpawnMob by boolean("BlockSpawnMob", true)
    private val blockTeams by boolean("BlockTeams", true)
    private val logAction by boolean("LogAction", true)

    private var mobBlocked = 0
    private var teamsBlocked = 0

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        
        when {
            blockSpawnMob && packet is S0FPacketSpawnMob -> {
                mobBlocked++
                event.cancelEvent()
                if (logAction) {
                    println("[CrashProtector] Blocked S0FPacketSpawnMob (#$mobBlocked)")
                }
            }
            
            blockTeams && packet is S3EPacketTeams -> {
                teamsBlocked++
                event.cancelEvent()
                if (logAction) {
                    println("[CrashProtector] Blocked S3EPacketTeams (#$teamsBlocked)")
                }
            }
        }
    }

    override fun onEnable() {
        mobBlocked = 0
        teamsBlocked = 0
        if (logAction) {
            println("[CrashProtector] Enabled - Will block suspicious packets")
        }
    }

    override fun onDisable() {
        if (logAction) {
            println("[CrashProtector] Disabled - Total blocked: S0FPacketSpawnMob=$mobBlocked, S3EPacketTeams=$teamsBlocked")
        }
    }

    override val tag: String
        get() = if ((mobBlocked + teamsBlocked) > 0) {
            "${mobBlocked + teamsBlocked}"
        } else {
            ""
        }
}