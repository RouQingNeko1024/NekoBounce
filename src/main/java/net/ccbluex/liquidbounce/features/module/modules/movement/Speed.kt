/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop3313
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop350
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop4
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop5
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.grim.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.WatchdogSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave.IntaveHop14
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.MatrixHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.MatrixSlowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.OldMatrixHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusFHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHopNew
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanGround288
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanLowHop
import net.ccbluex.liquidbounce.utils.extensions.isMoving

object Speed : Module("Speed", Category.MOVEMENT) {

    private val speedModes = arrayOf<SpeedMode>(
        // NCP
        NCPBHop,
        NCPFHop,
        SNCPBHop,
        NCPHop,
        NCPYPort,
        UNCPHop,
        UNCPHopNew,

        // AAC
        AACHop3313,
        AACHop350,
        AACHop4,
        AACHop5,

        // Spartan
        SpartanYPort,

        // Spectre
        SpectreLowHop,
        SpectreBHop,
        SpectreOnGround,

        // Verus
        VerusHop,
        VerusFHop,
        VerusLowHop,
        VerusLowHopNew,

        // Vulcan
        VulcanHop,
        VulcanLowHop,
        VulcanGround288,

        // Matrix
        OldMatrixHop,
        MatrixHop,
        MatrixSlowHop,

        // Intave
        IntaveHop14,

        // GrimAC
        GrimAC,
        GrimCollide,
        Grim273FastFall,

        // Server specific
        TeleportCubeCraft,
        HypixelHop,
        HypixelLowHop,
        WatchdogSpeed,
        BlocksMCHop,

        // Other
        Boost,
        Frame,
        MiJump,
        OnGround,
        SlowHop,
        Legit,
        CustomSpeed,
        PredictionTimer,
        PredictionTimer2
    )

    /**
     * Old/Deprecated Modes
     */
    private val deprecatedMode = arrayOf<SpeedMode>(
        TeleportCubeCraft,
        OldMatrixHop,
        VerusLowHop,
        SpectreLowHop, SpectreBHop, SpectreOnGround,
        AACHop3313, AACHop350, AACHop4,
        NCPBHop, NCPFHop, SNCPBHop, NCPHop, NCPYPort,
        MiJump, Frame
    )

    // 修复：移除不存在的changeValue和updateValues方法
    private val showDeprecated by boolean("DeprecatedMode", true).onChanged { value ->
        // 当显示废弃模式时，不需要做任何特殊处理
        // 模式选择器会自动更新可选项
    }

    private var modesList = speedModes

    // 修复：使用字符串数组而不是从对象获取
    private val modeNames = speedModes.map { it.modeName }.toTypedArray()
    val mode by choices("Mode", modeNames, "NCPBHop")

    // 修复：将私有变量改为public，以便其他模块可以访问
    // Grim Speed
    val grimCollideSpeed by float("GrimCollide-Speed", 0.08f, 0.01f..0.08f) { mode == "GrimCollide" }
    val grimacAmount by int("GrimAC-Amount", 3, 0..10) { mode == "GrimAC" }
    val grimacAutoJump by boolean("GrimAC-AutoJump", true) { mode == "GrimAC" }

    // Grim273FastFall Speed
    val fastFallStrength by float("FastFallStrength", 0.15f, 0.05f..0.5f) { mode == "Grim2.3.73 1.9+ FastFall" }
    val constantJump by boolean("ConstantJump", true) { mode == "Grim2.3.73 1.9+ FastFall" }
    val postPlacePackets by boolean("PostPlacePackets", true) { mode == "Grim2.3.73 1.9+ FastFall" }

    // Custom Speed
    val customY by float("CustomY", 0.42f, 0f..4f) { mode == "CustomSpeed" }
    val customGroundStrafe by float("CustomGroundStrafe", 1.6f, 0f..2f) { mode == "CustomSpeed" }
    val customAirStrafe by float("CustomAirStrafe", 0f, 0f..2f) { mode == "CustomSpeed" }
    val customGroundTimer by float("CustomGroundTimer", 1f, 0.1f..2f) { mode == "CustomSpeed" }
    val customAirTimerTick by int("CustomAirTimerTick", 5, 1..20) { mode == "CustomSpeed" }
    val customAirTimer by float("CustomAirTimer", 1f, 0.1f..2f) { mode == "CustomSpeed" }

    // Extra options
    val resetXZ by boolean("ResetXZ", false) { mode == "CustomSpeed" }
    val resetY by boolean("ResetY", false) { mode == "CustomSpeed" }
    val notOnConsuming by boolean("NotOnConsuming", false) { mode == "CustomSpeed" }
    val notOnFalling by boolean("NotOnFalling", false) { mode == "CustomSpeed" }
    val notOnVoid by boolean("NotOnVoid", true) { mode == "CustomSpeed" }

    // TeleportCubecraft Speed
    val cubecraftPortLength by float("CubeCraft-PortLength", 1f, 0.1f..2f) { mode == "TeleportCubeCraft" }

    // IntaveHop14 Speed
    val boost by boolean("Boost", true) { mode == "IntaveHop14" }
    val initialBoostMultiplier by float("InitialBoostMultiplier", 1f, 0.01f..10f) { boost && mode == "IntaveHop14" }
    val intaveLowHop by boolean("LowHop", true) { mode == "IntaveHop14" }
    val strafeStrength by float("StrafeStrength", 0.29f, 0.1f..0.29f) { mode == "IntaveHop14" }
    val groundTimer by float("GroundTimer", 0.5f, 0.1f..5f) { mode == "IntaveHop14" }
    val airTimer by float("AirTimer", 1.09f, 0.1f..5f) { mode == "IntaveHop14" }

    // UNCPHopNew Speed
    val pullDown by boolean("PullDown", true) { mode == "UNCPHopNew" }
    val uncpOnTick by int("OnTick", 5, 5..9) { pullDown && mode == "UNCPHopNew" } // 修复：重命名变量避免冲突
    val onHurt by boolean("OnHurt", true) { pullDown && mode == "UNCPHopNew" }
    val shouldBoost by boolean("ShouldBoost", true) { mode == "UNCPHopNew" }
    val timerBoost by boolean("TimerBoost", true) { mode == "UNCPHopNew" }
    val damageBoost by boolean("DamageBoost", true) { mode == "UNCPHopNew" }
    val lowHop by boolean("LowHop", true) { mode == "UNCPHopNew" }
    val airStrafe by boolean("AirStrafe", true) { mode == "UNCPHopNew" }

    // MatrixHop Speed
    val matrixLowHop by boolean("LowHop", true) { mode == "MatrixHop" || mode == "MatrixSlowHop" }
    val extraGroundBoost by float("ExtraGroundBoost", 0.2f, 0f..0.5f) { mode == "MatrixHop" || mode == "MatrixSlowHop" }

    // HypixelLowHop Speed
    val glide by boolean("Glide", true) { mode == "HypixelLowHop" }

    // BlocksMCHop Speed
    val fullStrafe by boolean("FullStrafe", true) { mode == "BlocksMCHop" }
    val bmcLowHop by boolean("LowHop", true) { mode == "BlocksMCHop" }
    val bmcDamageBoost by boolean("DamageBoost", true) { mode == "BlocksMCHop" }
    val damageLowHop by boolean("DamageLowHop", false) { mode == "BlocksMCHop" }
    val safeY by boolean("SafeY", true) { mode == "BlocksMCHop" }

    // PredictionTimer
    val predictionGroundTimer by float("PredictionGroundTimer", 1.5f, 1f..3.0f) { mode == "PredictionTimer" }

    // PredictionTimer2 配置
    val prediction2TimerSpeed by float("Prediction2TimerSpeed", 1.5f, 1f..3.0f) { mode == "PredictionTimer2" }
    val prediction2CycleLength by int("Prediction2CycleLength", 8, 5..60) { mode == "PredictionTimer2" }
    val prediction2BoostDuration by int("Prediction2BoostDuration", 2, 1..5) { mode == "PredictionTimer2" }

    // WatchdogSpeed 配置
    val watchdogTimer by float("Watchdog-Timer", 1f, 1f..2f) { mode == "WatchdogSpeed" }
    val watchdogType by choices("Watchdog-Type", arrayOf("Full Strafe", "Ground Strafe", "Damage Strafe"), "Full Strafe") { mode == "WatchdogSpeed" }
    val watchdogDamageBoost by boolean("Watchdog-DamageBoost", false) { mode == "WatchdogSpeed" }

    val onUpdateHandler = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.isSneaking)
            return@handler

        if (thePlayer.isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onUpdate()
    }

    val onMotionHandler = handler<MotionEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler

        if (thePlayer.isSneaking || event.eventState != EventState.PRE)
            return@handler

        if (thePlayer.isMoving && !sprintManually)
            thePlayer.isSprinting = true

        modeModule.onMotion()
    }

    val onMoveHandler = handler<MoveEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onMove(event)
    }

    val onTickHandler = handler<GameTickEvent> {
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onTick()
    }

    val onStrafeHandler = handler<StrafeEvent> {
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onStrafe()
    }

    val onJumpHandler = handler<JumpEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onJump(event)
    }

    val onPacketHandler = handler<PacketEvent> { event ->
        if (mc.thePlayer?.isSneaking == true)
            return@handler

        modeModule.onPacket(event)
    }

    override fun onEnable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        modeModule.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f
        modeModule.onDisable()
    }

    override val tag: String
        get() = mode

    private val modeModule: SpeedMode
        get() = speedModes.find { it.modeName == mode } ?: speedModes.first()

    private val sprintManually: Boolean
        get() = modeModule in arrayOf(Legit)
}