package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer

object TimerPlus : Module("TimerPlus", Category.NEKO) {

    private val lowTimer by float("LowTimer", 0.5f, 0.1f..10.0f)
    private val highTimer by float("HighTimer", 2.0f, 0.1f..10.0f)
    private val lowTime by int("LowTime", 1000, 100..5000)
    private val highTime by int("HighTime", 500, 100..5000)
    private val autoDisable by boolean("AutoDisable", true)

    private val phaseTimer = MSTimer()
    private var isHighPhase = false
    private var hasSwitched = false

    override fun onEnable() {
        phaseTimer.reset()
        isHighPhase = false
        hasSwitched = false
        mc.timer.timerSpeed = lowTimer
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
    }

    val onUpdate = handler<UpdateEvent> {
        if (!phaseTimer.hasTimePassed(if (isHighPhase) highTime else lowTime)) {
            return@handler
        }

        if (!isHighPhase) {
            // 切换到高Timer阶段
            isHighPhase = true
            hasSwitched = true
            mc.timer.timerSpeed = highTimer
            phaseTimer.reset()
        } else {
            // 高Timer阶段结束，根据设置决定是否关闭
            if (autoDisable && hasSwitched) {
                state = false
            } else {
                // 重新开始循环
                isHighPhase = false
                hasSwitched = false
                mc.timer.timerSpeed = lowTimer
                phaseTimer.reset()
            }
        }
    }
}