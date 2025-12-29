package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.Vec3

object VerticalFly : Module("VerticalFly", Category.MOVEMENT) {

    private val toggle by int("Toggle", 0, 0..100)
    private val timer by float("Timer", 0.446f, 0.1f..1f)
    
    // 自动关闭控制
    private val autoDisable by boolean("AutoDisable", false)
    private val disableAfterTicks by int("DisableAfterTicks", 100, 10..500) { autoDisable }
    private val disableOnGround by boolean("DisableOnGround", false) { autoDisable }
    private val disableOnDamage by boolean("DisableOnDamage", false) { autoDisable }
    private val disableOnWorldChange by boolean("DisableOnWorldChange", true) { autoDisable }

    private var ticks = 0
    private var pos: Vec3? = null
    private var lastWorldTime: Long = 0
    private var lastHealth = 0f

    override fun onEnable() {
        ticks = 0
        pos = null
        lastWorldTime = mc.theWorld?.totalWorldTime ?: 0
        lastHealth = mc.thePlayer?.health ?: 0f
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        
        if (ticks == 0) {
            player.jump()
        } else if (ticks <= 5) {
            // For some reason, low timer makes the timer jump (2 tick start)
            // A lot more stable.
            mc.timer.timerSpeed = timer
        } else if (ticks > 5) {
            mc.timer.timerSpeed = 1f
        }
        
        // 检查自动关闭条件
        checkAutoDisableConditions()
        
        // If ticks goes over toggle limit and toggle isnt 0, disable.
        if (toggle != 0 && ticks >= toggle) {
            state = false
        }

        ticks++
    }

    val onMotion = handler<MotionEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        if (ticks >= 2) {
            when (event.eventState) {
                EventState.PRE -> {
                    pos = player.positionVector
                    player.setPosition(player.posX + 1152, player.posY, player.posZ + 1152)
                }
                EventState.POST -> {
                    pos?.let { vec ->
                        player.setPosition(vec.xCoord, vec.yCoord, vec.zCoord)
                    }
                }
                else -> {
                    // 处理其他可能的 EventState 值
                }
            }
        }
    }

    val onWorldChange = handler<WorldEvent> {
        if (autoDisable && disableOnWorldChange) {
            state = false
        }
    }

    /**
     * 检查自动关闭条件
     */
    private fun checkAutoDisableConditions() {
        val player = mc.thePlayer ?: return
        
        // 检查时间条件
        if (autoDisable && ticks >= disableAfterTicks) {
            state = false
            return
        }
        
        // 检查地面条件
        if (autoDisable && disableOnGround && player.onGround) {
            state = false
            return
        }
        
        // 检查受伤条件（通过生命值变化检测）
        if (autoDisable && disableOnDamage) {
            val currentHealth = player.health
            if (currentHealth < lastHealth) {
                state = false
                return
            }
            lastHealth = currentHealth
        }
        
        // 检查世界变化
        if (autoDisable && disableOnWorldChange) {
            val currentWorldTime = mc.theWorld?.totalWorldTime ?: 0
            if (currentWorldTime != lastWorldTime + 1) {
                state = false
                return
            }
            lastWorldTime = currentWorldTime
        }
    }

    override val tag: String
        get() = "Toggle: $toggle${if (autoDisable) " | Auto" else ""}"
}