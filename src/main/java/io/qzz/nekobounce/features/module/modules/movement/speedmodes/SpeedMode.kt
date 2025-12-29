/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.movement.speedmodes

import io.qzz.nekobounce.event.JumpEvent
import io.qzz.nekobounce.event.MoveEvent
import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.utils.client.MinecraftInstance

open class SpeedMode(val modeName: String) : MinecraftInstance {
    open fun onMotion() {}
    open fun onUpdate() {}
    open fun onMove(event: MoveEvent) {}
    open fun onTick() {}
    open fun onStrafe() {}
    open fun onJump(event: JumpEvent) {}
    open fun onPacket(event: PacketEvent) {}
    open fun onEnable() {}
    open fun onDisable() {}
}