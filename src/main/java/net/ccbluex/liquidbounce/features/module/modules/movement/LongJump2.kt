//By Neko,Deepseek
//Skid Rise
//Skid LiquidBounce-nextgen
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object LongJump2 : Module("LongJump2", Category.MOVEMENT) {

    private val mode by choices(
        "Mode", 
        arrayOf("Matrix", "Matrix7145Flag", "MatrixBypass"), 
        "Matrix"
    )
    
    private val autoDisable by boolean("AutoDisable", true)
    private val enableBlink by boolean("EnableBlink", true)
    
    // 自动触发选项
    private val autoTrigger by boolean("AutoTrigger", false)
    private val triggerHeight by float("TriggerHeight", 1.5f, 0.5f..2f) { autoTrigger }
    
    // 下落后延迟触发选项
    private val afterFallTrigger by boolean("AfterFallTrigger", false)
    private val afterFallDelay by int("AfterFallDelay", 3, 0..10) { afterFallTrigger }

    // Matrix 7145 Flag 专用参数
    private val boostSpeed by float("BoostSpeed", 1.97f, 0.1f..5f) { mode == "Matrix7145Flag" || mode == "MatrixBypass" }
    private val motionY by float("MotionY", 0.42f, 0.0f..5.0f) { mode == "Matrix7145Flag" || mode == "MatrixBypass" }
    private val delay by int("Delay", 0, 0..3) { mode == "Matrix7145Flag" || mode == "MatrixBypass" }
    private val fastFall by boolean("FastFall", false) { mode == "Matrix7145Flag" || mode == "MatrixBypass" }
    private val fallMotion by float("FallMotion", -0.3f, -2.0f..0f) { (mode == "Matrix7145Flag" || mode == "MatrixBypass") && fastFall }
    
    // MatrixBypass 专用参数
    private val initialJumpHeight by float("InitialJumpHeight", 0.42f, 0.1f..1f) { mode == "MatrixBypass" }
    private val bypassDelay by int("BypassDelay", 2, 0..10) { mode == "MatrixBypass" }
    
    // Matrix7145Flag 空中起跳参数
    private val airJump by boolean("AirJump", false) { mode == "Matrix7145Flag" }
    private val airJumpHeight by float("AirJumpHeight", 0.3f, 0.1f..1f) { mode == "Matrix7145Flag" && airJump }

    private var ticks = 0
    private var lastMotion = 0.0
    private var originalBlinkState = false
    
    // Matrix 7145 Flag 专用变量
    private var flagTicks = 0
    private var airTicks = 0
    private const val ACCEPTED_AIR_TIME = 5
    private var isFalling = false
    private var matrixState = 0 // 0=等待, 1=跳跃中, 2=完成
    
    // 自动触发专用变量
    private var lastGroundY = 0.0
    private var hasTriggered = false
    
    // 下落后延迟触发专用变量
    private var afterFallTicks = 0
    private var isAfterFall = false
    
    // MatrixBypass 专用变量
    private var bypassState = 0 // 0=初始跳跃, 1=等待回弹, 2=执行长跳, 3=完成
    private var bypassTicks = 0
    
    // 空中起跳专用变量
    private var hasAirJumped = false

    val onUpdate = handler<UpdateEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        // 更新空中ticks
        if (!player.onGround) {
            airTicks++
        } else {
            airTicks = 0
            lastGroundY = player.posY
            hasTriggered = false
            hasAirJumped = false
            
            // 重置下落后状态
            isAfterFall = false
            afterFallTicks = 0
        }

        // 下落后延迟触发检测
        if (afterFallTrigger && !isAfterFall && player.onGround && airTicks == 0) {
            // 检测是否刚刚落地
            if (player.fallDistance > 0.5) {
                isAfterFall = true
                afterFallTicks = 0
            }
        }
        
        // 下落后延迟触发执行
        if (afterFallTrigger && isAfterFall && afterFallTicks >= afterFallDelay) {
            // 触发 LongJump
            when (mode.lowercase()) {
                "matrix" -> {
                    player.tryJump()
                    ticks = 0
                    lastMotion = 0.42 // 跳跃高度
                }
                "matrix7145flag", "matrixbypass" -> {
                    matrixState = 1
                    airTicks = 0
                }
            }
            isAfterFall = false
            afterFallTicks = 0
        }
        
        // 增加下落后延迟触发的tick计数
        if (isAfterFall) {
            afterFallTicks++
        }

        // 自动触发检测（高度触发）
        if (autoTrigger && !hasTriggered && !player.onGround && player.motionY < 0) {
            val fallDistance = lastGroundY - player.posY
            if (fallDistance >= triggerHeight) {
                // 触发 LongJump
                when (mode.lowercase()) {
                    "matrix" -> {
                        player.tryJump()
                        ticks = 0
                        lastMotion = 0.42 // 跳跃高度
                    }
                    "matrix7145flag", "matrixbypass" -> {
                        matrixState = 1
                        airTicks = 0
                    }
                }
                hasTriggered = true
            }
        }

        when (mode.lowercase()) {
            "matrix" -> {
                if (!player.onGround) ticks++
                if (player.onGround && !autoTrigger && !afterFallTrigger) player.tryJump() // 非自动触发时才在地面跳跃
                
                if (ticks % 12 == 0 || player.isCollidedVertically) 
                    lastMotion = player.motionY
                
                player.motionY = lastMotion
                
                if (autoDisable && player.motionY < 0.1 && player.onGround) 
                    state = false
            }
            "matrix7145flag" -> {
                // 检测下坠状态
                isFalling = player.motionY < 0 && !player.onGround
                
                // 在下坠过程中应用快速下坠
                if (isFalling && fastFall) {
                    player.motionY = fallMotion.toDouble()
                }

                // 空中起跳检测
                if (airJump && !player.onGround && !hasAirJumped && airTicks >= delay) {
                    // 执行空中起跳
                    player.motionY = airJumpHeight.toDouble()
                    hasAirJumped = true
                }

                // Matrix 7145 Flag 状态机
                when (matrixState) {
                    0 -> { // 等待状态
                        if (player.onGround && !autoTrigger && !afterFallTrigger) {
                            matrixState = 1
                            airTicks = 0
                            hasAirJumped = false
                        }
                    }
                    1 -> { // 跳跃状态
                        if (!player.onGround && airTicks >= delay) {
                            // 执行跳跃逻辑
                            val yaw = Math.toRadians(player.rotationYaw.toDouble())
                            player.motionX = -Math.sin(yaw) * boostSpeed
                            player.motionZ = Math.cos(yaw) * boostSpeed
                            player.motionY = motionY.toDouble()
                            
                            matrixState = 2
                        }
                    }
                    2 -> { // 完成状态
                        if (player.onGround || airTicks >= ACCEPTED_AIR_TIME || flagTicks >= 2) {
                            if (autoDisable) {
                                state = false
                            }
                            matrixState = 0
                            flagTicks = 0
                        }
                    }
                }
            }
            "matrixbypass" -> {
                // MatrixBypass 状态机
                when (bypassState) {
                    0 -> { // 初始跳跃状态
                        if (player.onGround) {
                            // 执行初始跳跃
                            player.motionY = initialJumpHeight.toDouble()
                            bypassState = 1
                            bypassTicks = 0
                        }
                    }
                    1 -> { // 等待回弹状态
                        bypassTicks++
                        
                        // 检测是否回弹（开始下落）
                        if (player.motionY < 0 || bypassTicks >= bypassDelay) {
                            bypassState = 2
                            airTicks = 0
                        }
                    }
                    2 -> { // 执行长跳状态
                        if (!player.onGround && airTicks >= delay) {
                            // 执行长跳逻辑
                            val yaw = Math.toRadians(player.rotationYaw.toDouble())
                            player.motionX = -Math.sin(yaw) * boostSpeed
                            player.motionZ = Math.cos(yaw) * boostSpeed
                            player.motionY = motionY.toDouble()
                            
                            bypassState = 3
                        }
                    }
                    3 -> { // 完成状态
                        if (player.onGround || airTicks >= ACCEPTED_AIR_TIME || flagTicks >= 2) {
                            if (autoDisable) {
                                state = false
                            }
                            bypassState = 0
                            flagTicks = 0
                        }
                    }
                }
                
                // 检测下坠状态
                isFalling = player.motionY < 0 && !player.onGround
                
                // 在下坠过程中应用快速下坠
                if (isFalling && fastFall) {
                    player.motionY = fallMotion.toDouble()
                }
            }
        }
    }

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        
        // Matrix7145Flag 和 MatrixBypass 模式下的移动调整
        if ((mode == "Matrix7145Flag" || mode == "MatrixBypass") && isFalling && fastFall) {
            event.y = fallMotion.toDouble()
        }
    }

    // Matrix 7145 Flag 和 MatrixBypass 专用包处理器
    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (mode != "Matrix7145Flag" && mode != "MatrixBypass") return@handler
        
        if (event.packet is S08PacketPlayerPosLook) {
            flagTicks++
        }
    }

    override fun onEnable() {
        ticks = 0
        lastMotion = 0.0
        flagTicks = 0
        airTicks = 0
        isFalling = false
        matrixState = 0
        lastGroundY = mc.thePlayer?.posY ?: 0.0
        hasTriggered = false
        isAfterFall = false
        afterFallTicks = 0
        bypassState = 0
        bypassTicks = 0
        hasAirJumped = false
        
        // MatrixBypass 模式启用时立即开始初始跳跃
        if (mode == "MatrixBypass") {
            bypassState = 0
        }
        
        // Blink控制
        if (enableBlink) {
            val blinkModule = ModuleManager.getModule("Blink")
            if (blinkModule != null) {
                originalBlinkState = blinkModule.state
                blinkModule.state = true
            }
        }
    }

    override fun onDisable() {
        flagTicks = 0
        airTicks = 0
        isFalling = false
        matrixState = 0
        hasTriggered = false
        isAfterFall = false
        afterFallTicks = 0
        bypassState = 0
        bypassTicks = 0
        hasAirJumped = false

        // 恢复Blink状态
        if (enableBlink) {
            val blinkModule = ModuleManager.getModule("Blink")
            if (blinkModule != null && blinkModule.state != originalBlinkState) {
                blinkModule.state = originalBlinkState
            }
        }
    }

    override val tag
        get() = mode
}