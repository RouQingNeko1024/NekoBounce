package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.movement.MoveUtils
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.DamageSource
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
// copy from Loftily LoL
object MatrixLongjump2 : Module("MatrixLongjump2", Category.MOVEMENT) {

    private val bypassMethod by choices("MatrixNew-BypassMethod", arrayOf("Fall", "NoGround"), "Fall")
    private val boostSpeed by float("MatrixNew-BoostSpeed", 2.1f, -3f..8f) { true }
    private val fakeDamage by boolean("FakeDamage", false) // 新增的假受伤选项

    private var flagTicks = 0
    private var lastFlagTime = 0L

    private var canBoost = false
    private var boosted = false
    private var touchGround = false
    private var fakeDamageCooldown = 0

    override fun onEnable() {
        flagTicks = 0
        lastFlagTime = 0L
        canBoost = false
        boosted = false
        touchGround = false
        fakeDamageCooldown = 0

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
        fakeDamageCooldown = 0
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

        // 假受伤冷却时间减少
        if (fakeDamageCooldown > 0) {
            fakeDamageCooldown--
        }

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
            // 触发假受伤效果
            if (fakeDamage && fakeDamageCooldown == 0) {
                triggerFakeDamage(player)
            }

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

    /**
     * 触发客户端假受伤效果
     * 只会在客户端显示动画和声音，不会向服务器发送任何数据包
     */
    private fun triggerFakeDamage(player: EntityLivingBase) {
        try {
            // 设置受伤时间，这会触发红色屏幕闪烁动画
            player.hurtTime = 10
            player.maxHurtTime = 10
            player.attackedAtYaw = 0f

            // 播放受伤声音
            val hurtSound = PositionedSoundRecord.create(
                ResourceLocation("game.player.hurt"),
                1.0f
            )
            mc.soundHandler.playSound(hurtSound)

            // 设置冷却时间，防止短时间内重复触发
            fakeDamageCooldown = 20

        } catch (e: Exception) {
            // 静默处理异常，避免崩溃
        }
    }

    /**
     * 更高级的假受伤效果（可选）
     * 包含屏幕抖动和粒子效果（如果可用）
     */
    private fun triggerAdvancedFakeDamage(player: EntityLivingBase) {
        try {
            // 基础受伤效果
            triggerFakeDamage(player)

            // 尝试添加屏幕抖动效果
            try {
                player.hurtResistantTime = 10
            } catch (_: Throwable) {}

            // 尝试播放更明显的受伤声音
            try {
                val hurtSound = PositionedSoundRecord.create(
                    ResourceLocation("game.player.hurt.fall.big"),
                    1.2f
                )
                mc.soundHandler.playSound(hurtSound)
            } catch (_: Throwable) {
                // 如果上述声音不存在，使用默认受伤声音
                val defaultHurtSound = PositionedSoundRecord.create(
                    ResourceLocation("game.player.hurt"),
                    1.0f
                )
                mc.soundHandler.playSound(defaultHurtSound)
            }

        } catch (e: Exception) {
            // 静默处理异常
        }
    }
}