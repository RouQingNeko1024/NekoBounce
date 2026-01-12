package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MoveUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook
// copy from Loftily LoL
object MatrixLongjump : Module("MatrixLongjump", Category.MOVEMENT) {

    private val bypassMethod by choices("MatrixNew-BypassMethod", arrayOf("Fall", "NoGround"), "Fall")
    private val boostSpeed by float("MatrixNew-BoostSpeed", 2.1f, -3f..8f) { true }

    private var flagTicks = 0
    private var lastFlagTime = 0L

    private var canBoost = false
    private var boosted = false
    private var touchGround = false

    override fun onEnable() {
        flagTicks = 0
        lastFlagTime = 0L
        canBoost = false
        boosted = false
        touchGround = false

        if (bypassMethod.equals("NoGround", ignoreCase = true)) {
            try {
                if (mc.thePlayer?.onGround == true) mc.thePlayer.tryJump()
            } catch (_: Throwable) {}
            touchGround = true
        }
    }

    override fun onDisable() {
        flagTicks = 0
        lastFlagTime = 0L
        canBoost = false
        boosted = false
        touchGround = false
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        try {
            if (packet is S08PacketPlayerPosLook) {
                val now = System.currentTimeMillis()
                if (now - lastFlagTime > 50L) {
                    flagTicks++
                    lastFlagTime = now
                }
            }
        } catch (_: Throwable) { /* tolerant */ }
    }

    val onMove = handler<MoveEvent> { _ ->
        try {
            if (bypassMethod.equals("NoGround", ignoreCase = true) && !canBoost) {
                mc.thePlayer?.onGround = false
            }
        } catch (_: Throwable) {}
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (!player.onGround && touchGround) {
            touchGround = false
        }

        if (player.onGround && !touchGround) {
            try { player.tryJump() } catch (_: Throwable) {}
            boosted = false
            if (bypassMethod.equals("NoGround", ignoreCase = true) && !boosted) {
                canBoost = true
            }
        }

        try {
            if (player.fallDistance >= 0.25 && !boosted && bypassMethod.equals("Fall", ignoreCase = true)) {
                canBoost = true
            }
        } catch (_: Throwable) {}

        if (canBoost) {
            val bs = try { boostSpeed.toDouble() } catch (_: Throwable) { 2.1 }
            try { MoveUtils.setSpeed(bs, false) } catch (_: Throwable) {}
            try { player.motionY = 0.42 } catch (_: Throwable) {}
            boosted = true
        }

        if (flagTicks >= 1 && boosted) {
            try { this.state = false } catch (_: Throwable) {}
            canBoost = false
            flagTicks = 0
            lastFlagTime = 0L
        }
    }
}
