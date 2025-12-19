//By Neko
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.network.play.client.*

object NekoDisabler : Module("NekoDisabler", Category.NEKO) {

    // C00PacketKeepAlive
    private val c00 by boolean("C00-KeepAlive", false)
    private val c00Interval by int("C00-Interval", 0, 0..100) { c00 }
    private val c00Invert by boolean("C00-Invert", false) { c00 }
    private val c00Condition by choices("C00-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c00 }

    // C01PacketChatMessage
    private val c01 by boolean("C01-ChatMessage", false)
    private val c01Interval by int("C01-Interval", 0, 0..100) { c01 }
    private val c01Invert by boolean("C01-Invert", false) { c01 }
    private val c01Condition by choices("C01-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c01 }

    // C02PacketUseEntity
    private val c02 by boolean("C02-UseEntity", false)
    private val c02Interval by int("C02-Interval", 0, 0..100) { c02 }
    private val c02Invert by boolean("C02-Invert", false) { c02 }
    private val c02Condition by choices("C02-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c02 }

    // C03PacketPlayer
    private val c03 by boolean("C03-Player", false)
    private val c03Interval by int("C03-Interval", 0, 0..100) { c03 }
    private val c03Invert by boolean("C03-Invert", false) { c03 }
    private val c03Condition by choices("C03-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c03 }

    // C04PacketPlayerPosition
    private val c04 by boolean("C04-PlayerPosition", false)
    private val c04Interval by int("C04-Interval", 0, 0..100) { c04 }
    private val c04Invert by boolean("C04-Invert", false) { c04 }
    private val c04Condition by choices("C04-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c04 }

    // C05PacketPlayerLook
    private val c05 by boolean("C05-PlayerLook", false)
    private val c05Interval by int("C05-Interval", 0, 0..100) { c05 }
    private val c05Invert by boolean("C05-Invert", false) { c05 }
    private val c05Condition by choices("C05-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c05 }

    // C06PacketPlayerPosLook
    private val c06 by boolean("C06-PlayerPosLook", false)
    private val c06Interval by int("C06-Interval", 0, 0..100) { c06 }
    private val c06Invert by boolean("C06-Invert", false) { c06 }
    private val c06Condition by choices("C06-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c06 }

    // C07PacketPlayerDigging
    private val c07 by boolean("C07-PlayerDigging", false)
    private val c07Interval by int("C07-Interval", 0, 0..100) { c07 }
    private val c07Invert by boolean("C07-Invert", false) { c07 }
    private val c07Condition by choices("C07-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c07 }

    // C08PacketPlayerBlockPlacement
    private val c08 by boolean("C08-BlockPlacement", false)
    private val c08Interval by int("C08-Interval", 0, 0..100) { c08 }
    private val c08Invert by boolean("C08-Invert", false) { c08 }
    private val c08Condition by choices("C08-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c08 }

    // C09PacketHeldItemChange
    private val c09 by boolean("C09-HeldItemChange", false)
    private val c09Interval by int("C09-Interval", 0, 0..100) { c09 }
    private val c09Invert by boolean("C09-Invert", false) { c09 }
    private val c09Condition by choices("C09-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c09 }

    // C0APacketAnimation
    private val c0a by boolean("C0A-Animation", false)
    private val c0aInterval by int("C0A-Interval", 0, 0..100) { c0a }
    private val c0aInvert by boolean("C0A-Invert", false) { c0a }
    private val c0aCondition by choices("C0A-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0a }

    // C0BPacketEntityAction
    private val c0b by boolean("C0B-EntityAction", false)
    private val c0bInterval by int("C0B-Interval", 0, 0..100) { c0b }
    private val c0bInvert by boolean("C0B-Invert", false) { c0b }
    private val c0bCondition by choices("C0B-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0b }

    // C0CPacketInput
    private val c0c by boolean("C0C-Input", false)
    private val c0cInterval by int("C0C-Interval", 0, 0..100) { c0c }
    private val c0cInvert by boolean("C0C-Invert", false) { c0c }
    private val c0cCondition by choices("C0C-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0c }

    // C0DPacketCloseWindow
    private val c0d by boolean("C0D-CloseWindow", false)
    private val c0dInterval by int("C0D-Interval", 0, 0..100) { c0d }
    private val c0dInvert by boolean("C0D-Invert", false) { c0d }
    private val c0dCondition by choices("C0D-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0d }

    // C0EPacketClickWindow
    private val c0e by boolean("C0E-ClickWindow", false)
    private val c0eInterval by int("C0E-Interval", 0, 0..100) { c0e }
    private val c0eInvert by boolean("C0E-Invert", false) { c0e }
    private val c0eCondition by choices("C0E-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0e }

    // C0FPacketConfirmTransaction
    private val c0f by boolean("C0F-ConfirmTransaction", false)
    private val c0fInterval by int("C0F-Interval", 0, 0..100) { c0f }
    private val c0fInvert by boolean("C0F-Invert", false) { c0f }
    private val c0fCondition by choices("C0F-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c0f }

    // C10PacketCreativeInventoryAction
    private val c10 by boolean("C10-CreativeInventory", false)
    private val c10Interval by int("C10-Interval", 0, 0..100) { c10 }
    private val c10Invert by boolean("C10-Invert", false) { c10 }
    private val c10Condition by choices("C10-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c10 }

    // C11PacketEnchantItem
    private val c11 by boolean("C11-EnchantItem", false)
    private val c11Interval by int("C11-Interval", 0, 0..100) { c11 }
    private val c11Invert by boolean("C11-Invert", false) { c11 }
    private val c11Condition by choices("C11-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c11 }

    // C12PacketUpdateSign
    private val c12 by boolean("C12-UpdateSign", false)
    private val c12Interval by int("C12-Interval", 0, 0..100) { c12 }
    private val c12Invert by boolean("C12-Invert", false) { c12 }
    private val c12Condition by choices("C12-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c12 }

    // C13PacketPlayerAbilities
    private val c13 by boolean("C13-PlayerAbilities", false)
    private val c13Interval by int("C13-Interval", 0, 0..100) { c13 }
    private val c13Invert by boolean("C13-Invert", false) { c13 }
    private val c13Condition by choices("C13-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c13 }

    // C14PacketTabComplete
    private val c14 by boolean("C14-TabComplete", false)
    private val c14Interval by int("C14-Interval", 0, 0..100) { c14 }
    private val c14Invert by boolean("C14-Invert", false) { c14 }
    private val c14Condition by choices("C14-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c14 }

    // C15PacketClientSettings
    private val c15 by boolean("C15-ClientSettings", false)
    private val c15Interval by int("C15-Interval", 0, 0..100) { c15 }
    private val c15Invert by boolean("C15-Invert", false) { c15 }
    private val c15Condition by choices("C15-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c15 }

    // C16PacketClientStatus
    private val c16 by boolean("C16-ClientStatus", false)
    private val c16Interval by int("C16-Interval", 0, 0..100) { c16 }
    private val c16Invert by boolean("C16-Invert", false) { c16 }
    private val c16Condition by choices("C16-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c16 }

    // C17PacketCustomPayload
    private val c17 by boolean("C17-CustomPayload", false)
    private val c17Interval by int("C17-Interval", 0, 0..100) { c17 }
    private val c17Invert by boolean("C17-Invert", false) { c17 }
    private val c17Condition by choices("C17-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c17 }

    // C18PacketSpectate
    private val c18 by boolean("C18-Spectate", false)
    private val c18Interval by int("C18-Interval", 0, 0..100) { c18 }
    private val c18Invert by boolean("C18-Invert", false) { c18 }
    private val c18Condition by choices("C18-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c18 }

    // C19PacketResourcePackStatus
    private val c19 by boolean("C19-ResourcePack", false)
    private val c19Interval by int("C19-Interval", 0, 0..100) { c19 }
    private val c19Invert by boolean("C19-Invert", false) { c19 }
    private val c19Condition by choices("C19-Condition", arrayOf("Always", "OnGround", "InAir"), "Always") { c19 }

    // 为所有包添加计时器
    private val packetTimers = mutableMapOf<String, MSTimer>()

    private val onPacketSend = handler<PacketEvent> { event ->
        val packet = event.packet
        val packetName = when (packet) {
            is C00PacketKeepAlive -> "C00"
            is C01PacketChatMessage -> "C01"
            is C02PacketUseEntity -> "C02"
            is C03PacketPlayer -> "C03"
            is C03PacketPlayer.C04PacketPlayerPosition -> "C04"
            is C03PacketPlayer.C05PacketPlayerLook -> "C05"
            is C03PacketPlayer.C06PacketPlayerPosLook -> "C06"
            is C07PacketPlayerDigging -> "C07"
            is C08PacketPlayerBlockPlacement -> "C08"
            is C09PacketHeldItemChange -> "C09"
            is C0APacketAnimation -> "C0A"
            is C0BPacketEntityAction -> "C0B"
            is C0CPacketInput -> "C0C"
            is C0DPacketCloseWindow -> "C0D"
            is C0EPacketClickWindow -> "C0E"
            is C0FPacketConfirmTransaction -> "C0F"
            is C10PacketCreativeInventoryAction -> "C10"
            is C11PacketEnchantItem -> "C11"
            is C12PacketUpdateSign -> "C12"
            is C13PacketPlayerAbilities -> "C13"
            is C14PacketTabComplete -> "C14"
            is C15PacketClientSettings -> "C15"
            is C16PacketClientStatus -> "C16"
            is C17PacketCustomPayload -> "C17"
            is C18PacketSpectate -> "C18"
            is C19PacketResourcePackStatus -> "C19"
            else -> return@handler
        }

        handlePacketCancellation(event, packetName)
    }

    private fun handlePacketCancellation(event: PacketEvent, packetName: String) {
        val enabled = getSettingValue<Boolean>(packetName) ?: return

        if (!enabled) return

        val interval = getSettingValue<Int>("${packetName}-Interval") ?: 0
        val invert = getSettingValue<Boolean>("${packetName}-Invert") ?: false
        val condition = getSettingValue<String>("${packetName}-Condition") ?: "Always"

        // 检查地面条件
        val thePlayer = mc.thePlayer ?: return
        val onGroundConditionMet = when (condition) {
            "OnGround" -> thePlayer.onGround
            "InAir" -> !thePlayer.onGround
            else -> true
        }

        if (!onGroundConditionMet) return

        // 检查间隔条件
        val timerKey = "SEND_$packetName"
        val timer = packetTimers.getOrPut(timerKey) { MSTimer() }

        val shouldCancel = if (interval > 0) {
            val timePassed = timer.hasTimePassed(interval.toLong())
            if (invert) !timePassed else timePassed
        } else {
            true
        }

        if (shouldCancel) {
            event.cancelEvent()
            if (interval > 0) {
                timer.reset()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> getSettingValue(name: String): T? {
        return when (name) {
            // C00PacketKeepAlive
            "C00" -> c00 as T
            "C00-Interval" -> c00Interval as T
            "C00-Invert" -> c00Invert as T
            "C00-Condition" -> c00Condition as T
            
            // C01PacketChatMessage
            "C01" -> c01 as T
            "C01-Interval" -> c01Interval as T
            "C01-Invert" -> c01Invert as T
            "C01-Condition" -> c01Condition as T
            
            // C02PacketUseEntity
            "C02" -> c02 as T
            "C02-Interval" -> c02Interval as T
            "C02-Invert" -> c02Invert as T
            "C02-Condition" -> c02Condition as T
            
            // C03PacketPlayer
            "C03" -> c03 as T
            "C03-Interval" -> c03Interval as T
            "C03-Invert" -> c03Invert as T
            "C03-Condition" -> c03Condition as T
            
            // C04PacketPlayerPosition
            "C04" -> c04 as T
            "C04-Interval" -> c04Interval as T
            "C04-Invert" -> c04Invert as T
            "C04-Condition" -> c04Condition as T
            
            // C05PacketPlayerLook
            "C05" -> c05 as T
            "C05-Interval" -> c05Interval as T
            "C05-Invert" -> c05Invert as T
            "C05-Condition" -> c05Condition as T
            
            // C06PacketPlayerPosLook
            "C06" -> c06 as T
            "C06-Interval" -> c06Interval as T
            "C06-Invert" -> c06Invert as T
            "C06-Condition" -> c06Condition as T
            
            // C07PacketPlayerDigging
            "C07" -> c07 as T
            "C07-Interval" -> c07Interval as T
            "C07-Invert" -> c07Invert as T
            "C07-Condition" -> c07Condition as T
            
            // C08PacketPlayerBlockPlacement
            "C08" -> c08 as T
            "C08-Interval" -> c08Interval as T
            "C08-Invert" -> c08Invert as T
            "C08-Condition" -> c08Condition as T
            
            // C09PacketHeldItemChange
            "C09" -> c09 as T
            "C09-Interval" -> c09Interval as T
            "C09-Invert" -> c09Invert as T
            "C09-Condition" -> c09Condition as T
            
            // C0APacketAnimation
            "C0A" -> c0a as T
            "C0A-Interval" -> c0aInterval as T
            "C0A-Invert" -> c0aInvert as T
            "C0A-Condition" -> c0aCondition as T
            
            // C0BPacketEntityAction
            "C0B" -> c0b as T
            "C0B-Interval" -> c0bInterval as T
            "C0B-Invert" -> c0bInvert as T
            "C0B-Condition" -> c0bCondition as T
            
            // C0CPacketInput
            "C0C" -> c0c as T
            "C0C-Interval" -> c0cInterval as T
            "C0C-Invert" -> c0cInvert as T
            "C0C-Condition" -> c0cCondition as T
            
            // C0DPacketCloseWindow
            "C0D" -> c0d as T
            "C0D-Interval" -> c0dInterval as T
            "C0D-Invert" -> c0dInvert as T
            "C0D-Condition" -> c0dCondition as T
            
            // C0EPacketClickWindow
            "C0E" -> c0e as T
            "C0E-Interval" -> c0eInterval as T
            "C0E-Invert" -> c0eInvert as T
            "C0E-Condition" -> c0eCondition as T
            
            // C0FPacketConfirmTransaction
            "C0F" -> c0f as T
            "C0F-Interval" -> c0fInterval as T
            "C0F-Invert" -> c0fInvert as T
            "C0F-Condition" -> c0fCondition as T
            
            // C10PacketCreativeInventoryAction
            "C10" -> c10 as T
            "C10-Interval" -> c10Interval as T
            "C10-Invert" -> c10Invert as T
            "C10-Condition" -> c10Condition as T
            
            // C11PacketEnchantItem
            "C11" -> c11 as T
            "C11-Interval" -> c11Interval as T
            "C11-Invert" -> c11Invert as T
            "C11-Condition" -> c11Condition as T
            
            // C12PacketUpdateSign
            "C12" -> c12 as T
            "C12-Interval" -> c12Interval as T
            "C12-Invert" -> c12Invert as T
            "C12-Condition" -> c12Condition as T
            
            // C13PacketPlayerAbilities
            "C13" -> c13 as T
            "C13-Interval" -> c13Interval as T
            "C13-Invert" -> c13Invert as T
            "C13-Condition" -> c13Condition as T
            
            // C14PacketTabComplete
            "C14" -> c14 as T
            "C14-Interval" -> c14Interval as T
            "C14-Invert" -> c14Invert as T
            "C14-Condition" -> c14Condition as T
            
            // C15PacketClientSettings
            "C15" -> c15 as T
            "C15-Interval" -> c15Interval as T
            "C15-Invert" -> c15Invert as T
            "C15-Condition" -> c15Condition as T
            
            // C16PacketClientStatus
            "C16" -> c16 as T
            "C16-Interval" -> c16Interval as T
            "C16-Invert" -> c16Invert as T
            "C16-Condition" -> c16Condition as T
            
            // C17PacketCustomPayload
            "C17" -> c17 as T
            "C17-Interval" -> c17Interval as T
            "C17-Invert" -> c17Invert as T
            "C17-Condition" -> c17Condition as T
            
            // C18PacketSpectate
            "C18" -> c18 as T
            "C18-Interval" -> c18Interval as T
            "C18-Invert" -> c18Invert as T
            "C18-Condition" -> c18Condition as T
            
            // C19PacketResourcePackStatus
            "C19" -> c19 as T
            "C19-Interval" -> c19Interval as T
            "C19-Invert" -> c19Invert as T
            "C19-Condition" -> c19Condition as T
            
            else -> null
        }
    }

    override fun onDisable() {
        packetTimers.clear()
    }

    override val tag: String?
        get() = "Client Packets"
}