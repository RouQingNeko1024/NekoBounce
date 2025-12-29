/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.special

object AutoReconnect {
    const val MAX = 60000
    const val MIN = 1000

    var isEnabled = true
        internal set

    var delay by ClientFixes.autoReconnectDelayValue
}