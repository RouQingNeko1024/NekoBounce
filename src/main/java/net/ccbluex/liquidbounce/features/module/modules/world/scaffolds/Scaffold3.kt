/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code By GoldBounce,Lizz,NightSky,FDP
 * https://github.com/SkidderMC/FDPClient
 * https://github.com/qm123pz/NightSky-Client
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.async.loopSequence
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.block.*
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.hotBarSlot
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.movement.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.rotation.PlaceRotation
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettingsWithRotationModes
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.canUpdateRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getFixedAngleDelta
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.*
import net.minecraft.block.BlockBush
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.util.*
import net.minecraft.world.WorldSettings
import net.minecraftforge.event.ForgeEventFactory
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import kotlin.math.*
import kotlin.collections.ArrayList
import kotlin.collections.ArrayDeque
import kotlin.random.Random

object Scaffold3 : Module("Scaffold3", Category.WORLD) {

    /**
     * TOWER MODES & SETTINGS
     */
    private val towerMode by Tower.towerModeValues

    init {
        addValues(Tower.values)
    }

    /**
     * SCAFFOLD MODES & SETTINGS
     */
    val scaffoldMode by choices(
        "ScaffoldMode", arrayOf("Normal", "Rewinside", "Expand", "Telly", "GodBridge"), "Normal"
    )

    // Expand
    private val omniDirectionalExpand by boolean("OmniDirectionalExpand", false) { scaffoldMode == "Expand" }
    private val expandLength by int("ExpandLength", 1, 1..6) { scaffoldMode == "Expand" }

    // Placeable delay
    private val placeDelayValue = boolean("PlaceDelay", true) { scaffoldMode != "GodBridge" }
    private val delay by intRange("Delay", 0..0, 0..1000) { placeDelayValue.isActive() }

    // Extra clicks
    private val extraClicks by boolean("DoExtraClicks", false)
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { extraClicks }
    private val extraClickCPS by intRange("ExtraClickCPS", 3..7, 0..50) { extraClicks }
    private val placementAttempt by choices(
        "PlacementAttempt", arrayOf("Fail", "Independent"), "Fail"
    ) { extraClicks }

    // Autoblock
    private val autoBlock by choices("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val sortByHighestAmount by boolean("SortByHighestAmount", false) { autoBlock != "Off" }
    private val earlySwitch by boolean("EarlySwitch", false) { autoBlock != "Off" && !sortByHighestAmount }
    private val amountBeforeSwitch by int(
        "SlotAmountBeforeSwitch", 3, 1..10
    ) { earlySwitch && !sortByHighestAmount }

    // Settings
    private val autoF5 by boolean("AutoF5", false).subjective()

    // Basic stuff
    val sprint by boolean("Sprint", false)
    private val swing by boolean("Swing", true).subjective()
    private val down by boolean("Down", true) { !sameY && scaffoldMode !in arrayOf("GodBridge", "Telly") }

    private val ticksUntilRotation by intRange("TicksUntilRotation", 3..3, 1..8) {
        scaffoldMode == "Telly"
    }

    // GodBridge mode sub-values
    private val waitForRots by boolean("WaitForRotations", false) { isGodBridgeEnabled }
    private val useOptimizedPitch by boolean("UseOptimizedPitch", false) { isGodBridgeEnabled }
    private val customGodPitch by float(
        "GodBridgePitch", 73.5f, 0f..90f
    ) { isGodBridgeEnabled && !useOptimizedPitch }

    val jumpAutomatically by boolean("JumpAutomatically", true) { scaffoldMode == "GodBridge" }
    private val blocksToJumpRange by intRange("BlocksToJumpRange", 4..4, 1..8) {  scaffoldMode == "GodBridge" && !jumpAutomatically }

    // Telly mode sub-values
    private val startHorizontally by boolean("StartHorizontally", true) { scaffoldMode == "Telly" }
    private val horizontalPlacementsRange by intRange("HorizontalPlacementsRange", 1..1, 1..10) { scaffoldMode == "Telly" }
    private val verticalPlacementsRange by intRange("VerticalPlacementsRange", 1..1, 1..10) { scaffoldMode == "Telly" }

    private val jumpTicksRange by intRange("JumpTicksRange", 0..0, 0..10) { scaffoldMode == "Telly" }

    private val allowClutching by boolean("AllowClutching", true) { scaffoldMode !in arrayOf("Telly", "Expand") }
    private val horizontalClutchBlocks by int("HorizontalClutchBlocks", 3, 1..5) {
        allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
    }
    private val verticalClutchBlocks by int("VerticalClutchBlocks", 2, 1..3) {
        allowClutching && scaffoldMode !in arrayOf("Telly", "Expand")
    }
    private val blockSafe by boolean("BlockSafe", false) { !isGodBridgeEnabled }

    // Eagle
    private val eagleValue =
        choices("Eagle", arrayOf("Normal", "Silent", "Off"), "Normal") { scaffoldMode != "GodBridge" }
    val eagle by eagleValue
    private val eagleMode by choices("EagleMode", arrayOf("Both", "OnGround", "InAir"), "Both")
    { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val adjustedSneakSpeed by boolean("AdjustedSneakSpeed", true)
    { eagle == "Silent" && scaffoldMode != "GodBridge" }
    private val eagleSpeed by float("EagleSpeed", 0.3f, 0.3f..1.0f) { eagle != "Off" && scaffoldMode != "GodBridge" }
    val eagleSprint by boolean("EagleSprint", false) { eagle == "Normal" && scaffoldMode != "GodBridge" }
    private val blocksToEagle by intRange("BlocksToEagle", 0..0, 0..10) { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val edgeDistance by float("EagleEdgeDistance", 0f, 0f..0.5f)
    { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val useMaxSneakTime by boolean("UseMaxSneakTime", true) { eagle != "Off" && scaffoldMode != "GodBridge" }
    private val maxSneakTicks by intRange("MaxSneakTicks", 3..3, 0..10) { useMaxSneakTime }
    private val blockSneakingAgainUntilOnGround by boolean("BlockSneakingAgainUntilOnGround", true)
    { useMaxSneakTime && eagleMode != "OnGround" }

    // Rotation Options - 扩展以包含新模式
    private val modeList =
        choices("Rotations", arrayOf("Off", "Normal", "Stabilized", "ReverseYaw", "GodBridge", "Hypixel", "Acceleration", "AdvancedSpeed", "Dynamic", "Entropy", "PatternPrediction"), "Normal")
    
    private val hypixelMinPitch by float("HypixelMinPitch", 55f, 50f..90f) { modeList.get() == "Hypixel" }
    private val hypixelMaxPitch by float("HypixelMaxPitch", 75f, 50f..90f) { modeList.get() == "Hypixel" }
    
    private val options = RotationSettingsWithRotationModes(this, modeList).apply {
        strictValue.excludeWithState()
        resetTicksValue.setSupport { it && scaffoldMode != "Telly" }
    }

    // 加速度旋转配置
    private val accelerationEnabled by boolean("Acceleration/Enabled", true) { modeList.get() == "Acceleration" }
    private val accelerationSpeed by floatRange("Acceleration/Speed", 0.5f..1.5f, 0.1f..5f) { accelerationEnabled }
    private val accelerationMaxSpeed by floatRange("Acceleration/MaxSpeed", 1.5f..3f, 0.5f..10f) { accelerationEnabled }
    private val accelerationSmoothFactor by floatRange("Acceleration/SmoothFactor", 0.8f..1.2f, 0.1f..3f) { accelerationEnabled }
    private val accelerationSmartDeceleration by boolean("Acceleration/SmartDeceleration", true) { accelerationEnabled }
    private val accelerationDecelerationRate by floatRange("Acceleration/DecelerationRate", 0.3f..0.7f, 0.1f..1f) { accelerationEnabled }

    // 高级速度旋转配置
    private val advancedSpeedEnabled by boolean("AdvancedSpeed/Enabled", true) { modeList.get() == "AdvancedSpeed" }
    
    // Yaw 速度参数
    private val yawSpeedBase by float("AdvancedSpeed/Yaw/Base", 30f, 1f..180f) { advancedSpeedEnabled }
    private val yawSpeedUpRange by float("AdvancedSpeed/Yaw/UpRange", 10f, 0f..50f) { advancedSpeedEnabled }
    private val yawSpeedDownRange by float("AdvancedSpeed/Yaw/DownRange", 10f, 0f..50f) { advancedSpeedEnabled }
    private val yawSpeedUpChangeMax by float("AdvancedSpeed/Yaw/UpChangeMax", 30f, 0f..100f) { advancedSpeedEnabled }
    private val yawSpeedDownChangeMax by float("AdvancedSpeed/Yaw/DownChangeMax", 30f, 0f..100f) { advancedSpeedEnabled }
    
    // 大旋转时参数
    private val yawSpeedBaseLargeRotation by float("AdvancedSpeed/Yaw/BaseLarge", 60f, 1f..180f) { advancedSpeedEnabled }
    private val yawSpeedUpRangeLargeRotation by float("AdvancedSpeed/Yaw/UpRangeLarge", 20f, 0f..50f) { advancedSpeedEnabled }
    private val yawSpeedDownRangeLargeRotation by float("AdvancedSpeed/Yaw/DownRangeLarge", 20f, 0f..50f) { advancedSpeedEnabled }
    private val yawSpeedUpChangeMaxLargeRotation by float("AdvancedSpeed/Yaw/UpChangeMaxLarge", 50f, 0f..100f) { advancedSpeedEnabled }
    private val yawSpeedDownChangeMaxLargeRotation by float("AdvancedSpeed/Yaw/DownChangeMaxLarge", 50f, 0f..100f) { advancedSpeedEnabled }
    
    // 阈值和过冲
    private val yawDeltaThresholdForLargeRotation by float("AdvancedSpeed/Yaw/DeltaThreshold", 30f, 5f..90f) { advancedSpeedEnabled }
    private val yawOvershotRate by float("AdvancedSpeed/Yaw/OvershotRate", 0.2f, 0f..1f) { advancedSpeedEnabled }

    // Pitch 速度参数
    private val pitchSpeedBase by float("AdvancedSpeed/Pitch/Base", 20f, 1f..90f) { advancedSpeedEnabled }
    private val pitchSpeedUpRange by float("AdvancedSpeed/Pitch/UpRange", 5f, 0f..30f) { advancedSpeedEnabled }
    private val pitchSpeedDownRange by float("AdvancedSpeed/Pitch/DownRange", 5f, 0f..30f) { advancedSpeedEnabled }
    private val pitchSpeedUpChangeMax by float("AdvancedSpeed/Pitch/UpChangeMax", 15f, 0f..50f) { advancedSpeedEnabled }
    private val pitchSpeedDownChangeMax by float("AdvancedSpeed/Pitch/DownChangeMax", 15f, 0f..50f) { advancedSpeedEnabled }
    
    private val pitchSpeedBaseLargeRotation by float("AdvancedSpeed/Pitch/BaseLarge", 40f, 1f..90f) { advancedSpeedEnabled }
    private val pitchSpeedUpRangeLargeRotation by float("AdvancedSpeed/Pitch/UpRangeLarge", 10f, 0f..30f) { advancedSpeedEnabled }
    private val pitchSpeedDownRangeLargeRotation by float("AdvancedSpeed/Pitch/DownRangeLarge", 10f, 0f..30f) { advancedSpeedEnabled }
    private val pitchSpeedUpChangeMaxLargeRotation by float("AdvancedSpeed/Pitch/UpChangeMaxLarge", 25f, 0f..50f) { advancedSpeedEnabled }
    private val pitchSpeedDownChangeMaxLargeRotation by float("AdvancedSpeed/Pitch/DownChangeMaxLarge", 25f, 0f..50f) { advancedSpeedEnabled }
    
    private val pitchDeltaThresholdForLargeRotation by float("AdvancedSpeed/Pitch/DeltaThreshold", 20f, 5f..60f) { advancedSpeedEnabled }
    private val pitchOvershotRate by float("AdvancedSpeed/Pitch/OvershotRate", 0.15f, 0f..1f) { advancedSpeedEnabled }

    // 动态旋转配置
    private val dynamicEnabled by boolean("Dynamic/Enabled", true) { modeList.get() == "Dynamic" }
    
    // 使用多选框的替代方案 - 改为单独的布尔值
    private val dynamicEffectSpeedBoost by boolean("Dynamic/Effect/SpeedBoost", true) { dynamicEnabled }
    private val dynamicEffectSpeedReduction by boolean("Dynamic/Effect/SpeedReduction", false) { dynamicEnabled }
    private val dynamicEffectJitter by boolean("Dynamic/Effect/Jitter", true) { dynamicEnabled }
    private val dynamicEffectSmooth by boolean("Dynamic/Effect/Smooth", false) { dynamicEnabled }
    
    private val dynamicDuration by intRange("Dynamic/Duration", 5..20, 1..100) { dynamicEnabled }
    private val dynamicUpdateInterval by int("Dynamic/UpdateInterval", 2, 1..20) { dynamicEnabled }
    private val dynamicSpeedBoostMultiplier by floatRange("Dynamic/SpeedBoostMultiplier", 1.5f..2.5f, 1f..5f) { dynamicEnabled }
    private val dynamicSpeedReductionMultiplier by floatRange("Dynamic/SpeedReductionMultiplier", 0.3f..0.7f, 0.1f..1f) { dynamicEnabled }
    private val dynamicSmoothMultiplier by floatRange("Dynamic/SmoothMultiplier", 0.5f..0.9f, 0.1f..1f) { dynamicEnabled }
    private val dynamicJitterStrength by floatRange("Dynamic/JitterStrength", 0.5f..1.5f, 0.1f..3f) { dynamicEnabled }
    private val dynamicSmoothFactor by floatRange("Dynamic/SmoothFactor", 0.2f..0.5f, 0.05f..1f) { dynamicEnabled }
    private val dynamicMinMultiplier by float("Dynamic/MinMultiplier", 0.3f, 0.1f..1f) { dynamicEnabled }
    private val dynamicMaxMultiplier by float("Dynamic/MaxMultiplier", 3f, 1f..10f) { dynamicEnabled }

    // 熵旋转配置
    private val entropyEnabled by boolean("Entropy/Enabled", true) { modeList.get() == "Entropy" }
    private val entropyTarget by float("Entropy/Target", 0.5f, 0f..1f) { entropyEnabled }
    private val entropyTolerance by float("Entropy/Tolerance", 0.15f, 0.05f..0.5f) { entropyEnabled }
    private val entropySmoothingStrength by floatRange("Entropy/SmoothingStrength", 0.5f..1.5f, 0.1f..3f) { entropyEnabled }
    private val entropyRandomnessStrength by floatRange("Entropy/RandomnessStrength", 0.3f..0.8f, 0.1f..2f) { entropyEnabled }
    private val entropyAdjustmentSpeed by float("Entropy/AdjustmentSpeed", 0.8f, 0.1f..2f) { entropyEnabled }
    private val entropyHistorySize by int("Entropy/HistorySize", 60, 10..200) { entropyEnabled }

    // 模式预测配置
    private val patternPredictionEnabled by boolean("PatternPrediction/Enabled", true) { modeList.get() == "PatternPrediction" }
    private val mlPredictionWeight by floatRange("PatternPrediction/MLWeight", 0.3f..0.7f, 0f..1f) { patternPredictionEnabled }

    // Search options
    val searchMode by choices("SearchMode", arrayOf("Area", "Center"), "Area") { scaffoldMode != "GodBridge" }
    private val minDist by float("MinDist", 0f, 0f..0.2f) { scaffoldMode !in arrayOf("GodBridge", "Telly") }
    private val autoJump by boolean("Autojump", false)
    private val matrixAutoJump by boolean("MatrixAutoJump", false)
    
    // Zitter
    private val zitterMode by choices("Zitter", arrayOf("Off", "Teleport", "Smooth"), "Off")
    private val zitterSpeed by float("ZitterSpeed", 0.13f, 0.1f..0.3f) { zitterMode == "Teleport" }
    private val zitterStrength by float("ZitterStrength", 0.05f, 0f..0.2f) { zitterMode == "Teleport" }
    private val zitterTicks by intRange("ZitterTicks", 2..3, 0..6) { zitterMode == "Smooth" }

    private val useSneakMidAir by boolean("UseSneakMidAir", false) { zitterMode == "Smooth" }

    // Game
    val timer by float("Timer", 1f, 0.1f..10f)
    private val speedModifier by float("SpeedModifier", 1f, 0f..2f)
    private val speedLimiter by boolean("SpeedLimiter", false) { !slow }
    private val speedLimit by float("SpeedLimit", 0.11f, 0.01f..0.12f) { !slow && speedLimiter }
    private val slow by boolean("Slow", false)
    private val slowGround by boolean("SlowOnlyGround", false) { slow }
    private val slowSpeed by float("SlowSpeed", 0.6f, 0.2f..0.8f) { slow }

    // Jump Strafe
    private val jumpStrafe by boolean("JumpStrafe", false)
    private val jumpStraightStrafe by floatRange("JumpStraightStrafe", 0.4f..0.45f, 0.1f..1f) { jumpStrafe }
    private val jumpDiagonalStrafe by floatRange("JumpDiagonalStrafe", 0.4f..0.45f, 0.1f..1f) { jumpStrafe }

    // Safety
    private val sameY by boolean("SameY", false) { scaffoldMode != "GodBridge" }
    private val jumpOnUserInput by boolean("JumpOnUserInput", true) { sameY && scaffoldMode != "GodBridge" }

    private val safeWalkValue = boolean("SafeWalk", true) { scaffoldMode != "GodBridge" }
    private val airSafe by boolean("AirSafe", false) { safeWalkValue.isActive() }

    // Visuals
    private val mark by boolean("Mark", false).subjective()
    private val markColor by color("MarkColor", Color(68, 117, 255, 100)) { mark }
    private val trackCPS by boolean("TrackCPS", false).subjective()

    // Target placement
    var placeRotation: PlaceRotation? = null

    // Launch position
    private var launchY = -999

    // 高级旋转系统状态变量
    private var accelerationCurrentSpeedYaw = 0f
    private var accelerationCurrentSpeedPitch = 0f
    private var accelerationLastUpdateTime = 0L
    
    // 动态旋转状态变量
    private var currentDynamicMultiplier = 1.0f
    private var dynamicTicksRemaining = 0
    private var lastUpdateTick = 0L
    private var currentDynamicEffect = "SpeedBoost"
    
    // 熵旋转状态变量
    private val entropyHistory = ArrayDeque<Float>()
    private var lastEntropyUpdate = 0L
    private var currentEntropy = 0.5f
    
    // 模式预测状态
    private val rotationHistory = mutableListOf<RotationData>()
    private var lastPrediction: Rotation? = null
    private var lastPredictionTime = 0L

    val shouldJumpOnInput
        get() = !jumpOnUserInput || !mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.posY >= launchY && !mc.thePlayer.onGround

    private val shouldKeepLaunchPosition
        get() = sameY && shouldJumpOnInput && scaffoldMode != "GodBridge"

    // Zitter
    private var zitterDirection = false

    // Delay
    private val delayTimer = object : DelayTimer(delay.first, delay.last, MSTimer()) {
        override fun hasTimePassed() = !placeDelayValue.isActive() || super.hasTimePassed()
    }

    private val zitterTickTimer = TickDelayTimer(zitterTicks.first, zitterTicks.last)

    // Eagle
    private var placedBlocksWithoutEagle = 0
    var eagleSneaking = false
    private var requestedStopSneak = false

    private val isEagleEnabled
        get() = eagle != "Off" && !shouldGoDown && scaffoldMode != "GodBridge"

    // Downwards
    val shouldGoDown
        get() = down && !sameY && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak) && scaffoldMode !in arrayOf(
            "GodBridge", "Telly"
        ) && blocksAmount() > 1

    // Current rotation
    private val currRotation
        get() = RotationUtils.currentRotation ?: mc.thePlayer.rotation

    // Extra clicks
    private var extraClick = ExtraClickInfo(TimeUtils.randomClickDelay(extraClickCPS.first, extraClickCPS.last), 0L, 0)

    // GodBridge
    private var blocksPlacedUntilJump = 0
    private val isManualJumpOptionActive
        get() = scaffoldMode == "GodBridge" && !jumpAutomatically
    private var blocksToJump = blocksToJumpRange.random()
    private val isGodBridgeEnabled
        get() = scaffoldMode == "GodBridge" || scaffoldMode == "Normal" && options.rotationMode == "GodBridge"
    private var godBridgeTargetRotation: Rotation? = null

    private val isLookingDiagonally: Boolean
        get() {
            val player = mc.thePlayer ?: return false
            val directionDegree = MovementUtils.direction.toDegreesF()
            val yaw = round(abs(MathHelper.wrapAngleTo180_float(directionDegree)) / 45f) * 45f
            val isYawDiagonal = yaw % 90 != 0f
            val isMovingDiagonal = player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
            val isStrafing = mc.gameSettings.keyBindRight.isKeyDown || mc.gameSettings.keyBindLeft.isKeyDown
            return isYawDiagonal && (isMovingDiagonal || isStrafing)
        }

    // Telly
    private var ticksUntilJump = 0
    private var blocksUntilAxisChange = 0
    private var jumpTicks = jumpTicksRange.random()
    private var horizontalPlacements = horizontalPlacementsRange.random()
    private var verticalPlacements = verticalPlacementsRange.random()
    private val shouldPlaceHorizontally
        get() = scaffoldMode == "Telly" && mc.thePlayer.isMoving && (startHorizontally && blocksUntilAxisChange <= horizontalPlacements || !startHorizontally && blocksUntilAxisChange > verticalPlacements)

    // Enabling module
    override fun onEnable() {
        val player = mc.thePlayer ?: return
        launchY = player.posY.roundToInt()
        blocksUntilAxisChange = 0
        accelerationLastUpdateTime = System.currentTimeMillis()
        currentDynamicMultiplier = 1.0f
        dynamicTicksRemaining = 0
        entropyHistory.clear()
        currentEntropy = 0.5f
        rotationHistory.clear()
    }

    // Events
    val onUpdate = loopSequence {
        val player = mc.thePlayer ?: return@loopSequence

        if (mc.playerController.currentGameType == WorldSettings.GameType.SPECTATOR) return@loopSequence

        mc.timer.timerSpeed = timer

        // 添加旋转历史用于模式预测
        rotationHistory.add(RotationData(player.rotationYaw, player.rotationPitch, System.currentTimeMillis()))
        if (rotationHistory.size > 50) {
            rotationHistory.removeAt(0)
        }

        // 更新动态旋转效果
        if (dynamicEnabled) {
            val currentTick = System.currentTimeMillis() / 50
            if (dynamicTicksRemaining <= 0 || currentTick - lastUpdateTick >= dynamicUpdateInterval) {
                updateDynamicEffect()
                lastUpdateTick = currentTick
            }
            dynamicTicksRemaining--
        }

        // Telly
        if (player.onGround) ticksUntilJump++

        if (shouldGoDown) {
            mc.gameSettings.keyBindSneak.pressed = false
        }

        if (slow) {
            if (!slowGround || slowGround && player.onGround) {
                player.motionX *= slowSpeed
                player.motionZ *= slowSpeed
            }
        }

        // Eagle
        if (isEagleEnabled) {
            var dif = 0.5
            val blockPos = BlockPos(player).down()

            for (side in EnumFacing.entries) {
                if (side.axis == EnumFacing.Axis.Y) {
                    continue
                }

                val neighbor = blockPos.offset(side)

                if (neighbor.isReplaceable) {
                    val calcDif = (if (side.axis == EnumFacing.Axis.Z) {
                        abs(neighbor.z + 0.5 - player.posZ)
                    } else {
                        abs(neighbor.x + 0.5 - player.posX)
                    }) - 0.5

                    if (calcDif < dif) {
                        dif = calcDif
                    }
                }
            }

            val blockSneaking = WaitTickUtils.hasScheduled("block")
            val alreadySneaking = WaitTickUtils.hasScheduled("sneak")

            val options = mc.gameSettings

            run {
                if (placedBlocksWithoutEagle < blocksToEagle.random() && !alreadySneaking && !blockSneaking && !eagleSneaking && !requestedStopSneak) {
                    return@run
                }

                val eagleCondition = when (eagleMode) {
                    "OnGround" -> player.onGround
                    "InAir" -> !player.onGround
                    else -> true
                }

                val pressedOnKeyboard = Keyboard.isKeyDown(options.keyBindSneak.keyCode)

                var shouldEagle =
                    eagleCondition && (blockPos.isReplaceable || dif < edgeDistance) || pressedOnKeyboard

                val shouldSchedule = !requestedStopSneak

                if (requestedStopSneak) {
                    requestedStopSneak = false

                    if (!player.onGround) {
                        shouldEagle = pressedOnKeyboard
                    }
                } else if (blockSneaking || alreadySneaking) {
                    return@run
                }

                if (eagle == "Silent") {
                    if (eagleSneaking != shouldEagle) {
                        sendPacket(
                            C0BPacketEntityAction(
                                player, if (shouldEagle) {
                                    C0BPacketEntityAction.Action.START_SNEAKING
                                } else {
                                    C0BPacketEntityAction.Action.STOP_SNEAKING
                                }
                            )
                        )

                        if (adjustedSneakSpeed && shouldEagle) {
                            player.motionX *= eagleSpeed
                            player.motionZ *= eagleSpeed
                        }
                    }

                    eagleSneaking = shouldEagle
                } else {
                    options.keyBindSneak.pressed = shouldEagle
                    eagleSneaking = shouldEagle
                }

                if (eagleSneaking && shouldSchedule) {
                    if (useMaxSneakTime) {
                        WaitTickUtils.conditionalSchedule("sneak") { elapsed ->
                            (elapsed >= maxSneakTicks.random() + 1).also { requestedStopSneak = it }
                        }
                    }

                    if (blockSneakingAgainUntilOnGround && !player.onGround) {
                        WaitTickUtils.conditionalSchedule("block") {
                            mc.thePlayer?.onGround.also { if (it != false) requestedStopSneak = true } ?: true
                        }
                    }
                }

                placedBlocksWithoutEagle = 0
            }
        }

        if (player.onGround) {
            if (scaffoldMode == "Rewinside") {
                strafe(0.2F)
                player.motionY = 0.0
            }
        }
    }
    
    val onStrafe = handler<StrafeEvent> {
        val player = mc.thePlayer ?: return@handler

        if (scaffoldMode == "Telly" && player.onGround && player.isMoving && currRotation == player.rotation && ticksUntilJump >= jumpTicks) {
            player.tryJump()
            ticksUntilJump = 0
            jumpTicks = jumpTicksRange.random()
            return@handler
        }

        if (matrixAutoJump && player.onGround && player.isMoving) {
            if (player.isInWater || player.isInLava || player.isOnLadder || player.isInWeb) return@handler

            if (Speed.matrixLowHop) {
                try {
                    player.jumpMovementFactor = 0.026f
                } catch (_: Throwable) {  }
            }

            player.tryJump()

            val lowHopAdjust = if (Speed.matrixLowHop) 0.00348 else 0.0
            try {
                player.motionY = 0.42 - lowHopAdjust
            } catch (_: Throwable) {}

            try {
                val groundSpeed = if (!handleEvents()) speed + Speed.extraGroundBoost else speed
                strafe(groundSpeed)
            } catch (_: Throwable) {}

            try {
                player.speedInAir = if (player.fallDistance <= 0.4 && player.moveStrafing == 0f) {
                    0.02035f
                } else {
                    0.02f
                }
            } catch (_: Throwable) {}

            return@handler
        }

        if (autoJump && player.onGround && player.isMoving) {
            player.tryJump()
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        val player = mc.thePlayer ?: return@handler

        if (player.ticksExisted == 1) {
            launchY = player.posY.roundToInt()
        }

        val rotation = RotationUtils.currentRotation

        update()

        val ticks = if (options.keepRotation) {
            if (scaffoldMode == "Telly") 1 else options.resetTicks
        } else {
            if (isGodBridgeEnabled) options.resetTicks else RotationUtils.resetTicks
        }

        if (!Tower.isTowering && isGodBridgeEnabled && options.rotationsActive) {
            generateGodBridgeRotations(ticks)
            return@handler
        }
        
        if (modeList.get() == "Hypixel" && options.rotationsActive) {
            val targetBlock = placeRotation?.placeInfo?.blockPos ?: return@handler
            val hypixelRotation = calculateHypixelRotation(targetBlock)
            setTargetRotation(hypixelRotation, options, ticks)
            return@handler
        }

        if (options.rotationsActive && rotation != null) {
            val placeRotation = this.placeRotation?.rotation ?: rotation
            val targetRotation = placeRotation.copy()
            
            // 根据选择的旋转模式应用不同的旋转算法
            val finalRotation = when (modeList.get()) {
                "Acceleration" -> applyAccelerationRotation(rotation, targetRotation)
                "AdvancedSpeed" -> applyAdvancedSpeedRotation(rotation, targetRotation)
                "Dynamic" -> applyDynamicRotation(rotation, targetRotation)
                "Entropy" -> applyEntropyRotation(rotation, targetRotation)
                "PatternPrediction" -> applyPatternPredictionRotation(rotation, targetRotation)
                else -> targetRotation // 默认使用标准旋转
            }
            
            setRotation(finalRotation, ticks)
        }
    }

    /**
     * 应用加速度旋转模式
     */
    private fun applyAccelerationRotation(current: Rotation, target: Rotation): Rotation {
        val now = System.currentTimeMillis()
        val yawDiff = angleDifference(target.yaw, current.yaw)
        val pitchDiff = target.pitch - current.pitch

        val baseHSpeed = 30f
        val baseVSpeed = 20f

        val accel = accelerationSpeed.random()
        val maxSpeedMult = accelerationMaxSpeed.random()
        val smoothFactor = accelerationSmoothFactor.random()

        val timeDiff = (now - accelerationLastUpdateTime).coerceAtLeast(1L)
        accelerationLastUpdateTime = now

        val ticksPassed = (timeDiff / 50f).coerceAtLeast(0.5f).coerceAtMost(3f)

        if (accelerationCurrentSpeedYaw == 0f && yawDiff != 0f) {
            accelerationCurrentSpeedYaw = baseHSpeed * sign(yawDiff) * 0.5f
        }
        if (accelerationCurrentSpeedPitch == 0f && pitchDiff != 0f) {
            accelerationCurrentSpeedPitch = baseVSpeed * sign(pitchDiff) * 0.5f
        }

        val yawDirection = sign(yawDiff)
        val pitchDirection = sign(pitchDiff)

        val maxAllowedYawSpeed = baseHSpeed * maxSpeedMult
        val maxAllowedPitchSpeed = baseVSpeed * maxSpeedMult

        if (abs(yawDiff) > 1f) {
            val accelThisTick = accel * ticksPassed * yawDirection
            accelerationCurrentSpeedYaw += accelThisTick

            if (abs(accelerationCurrentSpeedYaw) > maxAllowedYawSpeed) {
                accelerationCurrentSpeedYaw = maxAllowedYawSpeed * yawDirection
            }

            if (sign(accelerationCurrentSpeedYaw) != yawDirection && yawDirection != 0f) {
                accelerationCurrentSpeedYaw = baseHSpeed * yawDirection
            }
        } else {
            if (accelerationSmartDeceleration) {
                val decelRate = accelerationDecelerationRate.random()
                accelerationCurrentSpeedYaw *= (1f - (1f - decelRate) * ticksPassed)
                if (abs(accelerationCurrentSpeedYaw) < baseHSpeed * 0.05f) {
                    accelerationCurrentSpeedYaw = 0f
                }
            } else {
                accelerationCurrentSpeedYaw = 0f
            }
        }

        if (abs(pitchDiff) > 1f) {
            val accelThisTick = accel * ticksPassed * pitchDirection * 0.8f
            accelerationCurrentSpeedPitch += accelThisTick

            if (abs(accelerationCurrentSpeedPitch) > maxAllowedPitchSpeed) {
                accelerationCurrentSpeedPitch = maxAllowedPitchSpeed * pitchDirection
            }

            if (sign(accelerationCurrentSpeedPitch) != pitchDirection && pitchDirection != 0f) {
                accelerationCurrentSpeedPitch = baseVSpeed * pitchDirection
            }
        } else {
            if (accelerationSmartDeceleration) {
                val decelRate = accelerationDecelerationRate.random()
                accelerationCurrentSpeedPitch *= (1f - (1f - decelRate) * ticksPassed)
                if (abs(accelerationCurrentSpeedPitch) < baseVSpeed * 0.05f) {
                    accelerationCurrentSpeedPitch = 0f
                }
            } else {
                accelerationCurrentSpeedPitch = 0f
            }
        }

        val smoothedYawSpeed = accelerationCurrentSpeedYaw * smoothFactor
        val smoothedPitchSpeed = accelerationCurrentSpeedPitch * smoothFactor

        val yawMoveThisTick = smoothedYawSpeed * ticksPassed
        val pitchMoveThisTick = smoothedPitchSpeed * ticksPassed

        val maxYawMove = abs(yawDiff).coerceAtLeast(1f)
        val maxPitchMove = abs(pitchDiff).coerceAtLeast(1f)

        val actualYawMove = if (abs(yawMoveThisTick) > maxYawMove) {
            yawDiff
        } else {
            yawMoveThisTick
        }

        val actualPitchMove = if (abs(pitchMoveThisTick) > maxPitchMove) {
            pitchDiff
        } else {
            pitchMoveThisTick
        }

        return Rotation(current.yaw + actualYawMove, current.pitch + actualPitchMove)
    }

    /**
     * 应用高级速度旋转模式
     */
    private fun applyAdvancedSpeedRotation(current: Rotation, target: Rotation): Rotation {
        val yawDiff = angleDifference(target.yaw, current.yaw)
        val pitchDiff = target.pitch - current.pitch
        
        val yawSpeed = if (abs(yawDiff) > yawDeltaThresholdForLargeRotation) {
            yawSpeedBaseLargeRotation + (Random.nextFloat() * 2 - 1) * yawSpeedUpRangeLargeRotation
        } else {
            yawSpeedBase + (Random.nextFloat() * 2 - 1) * yawSpeedUpRange
        }
        
        val pitchSpeed = if (abs(pitchDiff) > pitchDeltaThresholdForLargeRotation) {
            pitchSpeedBaseLargeRotation + (Random.nextFloat() * 2 - 1) * pitchSpeedUpRangeLargeRotation
        } else {
            pitchSpeedBase + (Random.nextFloat() * 2 - 1) * pitchSpeedUpRange
        }
        
        // 手动实现角度限制
        val limitedYaw = limitAngleChange(current.yaw, target.yaw, abs(yawSpeed))
        val limitedPitch = limitAngleChange(current.pitch, target.pitch, abs(pitchSpeed))
        
        return Rotation(limitedYaw, limitedPitch).fixedSensitivity()
    }

    /**
     * 手动实现角度变化限制（替代缺失的 updateRotation 函数）
     */
    private fun limitAngleChange(current: Float, target: Float, maxChange: Float): Float {
        var diff = ((target - current) % 360 + 540) % 360 - 180
        diff = diff.coerceIn(-maxChange, maxChange)
        return current + diff
    }

    /**
     * 更新动态效果
     */
    private fun updateDynamicEffect() {
        // 从启用的效果中随机选择一个
        val availableEffects = mutableListOf<String>()
        if (dynamicEffectSpeedBoost) availableEffects.add("SpeedBoost")
        if (dynamicEffectSpeedReduction) availableEffects.add("SpeedReduction")
        if (dynamicEffectJitter) availableEffects.add("Jitter")
        if (dynamicEffectSmooth) availableEffects.add("Smooth")
        
        currentDynamicEffect = if (availableEffects.isNotEmpty()) {
            availableEffects.random()
        } else {
            "SpeedBoost" // 默认
        }

        dynamicTicksRemaining = dynamicDuration.random()

        currentDynamicMultiplier = when (currentDynamicEffect) {
            "SpeedBoost" -> dynamicSpeedBoostMultiplier.random()
            "SpeedReduction" -> dynamicSpeedReductionMultiplier.random()
            "Jitter" -> 1.0f
            "Smooth" -> dynamicSmoothMultiplier.random()
            else -> 1.0f
        }

        currentDynamicMultiplier = currentDynamicMultiplier.coerceIn(dynamicMinMultiplier, dynamicMaxMultiplier)
    }

    /**
     * 应用动态旋转模式
     */
    private fun applyDynamicRotation(current: Rotation, target: Rotation): Rotation {
        val baseHSpeed = 30f
        val baseVSpeed = 20f

        val dynamicHSpeed = baseHSpeed * currentDynamicMultiplier
        val dynamicVSpeed = baseVSpeed * currentDynamicMultiplier

        val yawDiff = angleDifference(target.yaw, current.yaw)
        val pitchDiff = target.pitch - current.pitch

        return when (currentDynamicEffect) {
            "Jitter" -> {
                val maxJitterYaw = dynamicHSpeed.coerceAtMost(abs(yawDiff))
                val maxJitterPitch = dynamicVSpeed.coerceAtMost(abs(pitchDiff))

                val jitterStrength = dynamicJitterStrength.random()
                val randomYaw = if (yawDiff != 0f) {
                    maxJitterYaw * (1 + Random.nextFloat() * jitterStrength - jitterStrength / 2)
                } else 0f

                val randomPitch = if (pitchDiff != 0f) {
                    maxJitterPitch * (1 + Random.nextFloat() * jitterStrength - jitterStrength / 2)
                } else 0f

                val finalYaw = if (yawDiff > 0) current.yaw + abs(randomYaw) else current.yaw - abs(randomYaw)
                val finalPitch = if (pitchDiff > 0) current.pitch + abs(randomPitch) else current.pitch - abs(randomPitch)

                Rotation(finalYaw, finalPitch)
            }
            "Smooth" -> {
                val smoothFactor = dynamicSmoothFactor.random()
                Rotation(
                    current.yaw + yawDiff * smoothFactor,
                    current.pitch + pitchDiff * smoothFactor
                )
            }
            else -> {
                // SpeedBoost 或 SpeedReduction
                val limitedYaw = limitAngleChange(current.yaw, target.yaw, dynamicHSpeed)
                val limitedPitch = limitAngleChange(current.pitch, target.pitch, dynamicVSpeed)
                Rotation(limitedYaw, limitedPitch)
            }
        }
    }

    /**
     * 计算当前旋转行为的熵值
     */
    private fun calculateCurrentEntropy(): Float {
        val now = System.currentTimeMillis()
        
        // 简化版熵计算
        if (rotationHistory.size < 5) return 0.5f
        
        val recentRotations = rotationHistory.takeLast(5)
        var totalDelta = 0f
        
        for (i in 1 until recentRotations.size) {
            val deltaYaw = abs(recentRotations[i].yaw - recentRotations[i-1].yaw)
            val deltaPitch = abs(recentRotations[i].pitch - recentRotations[i-1].pitch)
            totalDelta += (deltaYaw + deltaPitch) / 2
        }
        
        val avgDelta = totalDelta / (recentRotations.size - 1)
        val entropy = (avgDelta / 30f).coerceIn(0f, 1f)
        
        if (now - lastEntropyUpdate > 50) {
            entropyHistory.addLast(entropy)
            if (entropyHistory.size > entropyHistorySize) {
                entropyHistory.removeFirst()
            }
            lastEntropyUpdate = now
        }
        
        currentEntropy = if (entropyHistory.isNotEmpty()) {
            entropyHistory.average().toFloat()
        } else entropy
        
        return currentEntropy
    }

    /**
     * 应用熵旋转模式
     */
    private fun applyEntropyRotation(current: Rotation, target: Rotation): Rotation {
        val currentEntropy = calculateCurrentEntropy()
        val entropyDeviation = currentEntropy - entropyTarget
        
        return when {
            abs(entropyDeviation) > entropyTolerance -> {
                if (entropyDeviation > 0) {
                    val strength = (abs(entropyDeviation) / entropyTolerance).coerceIn(0f, 1f)
                    val smoothFactor = entropySmoothingStrength.random() * strength
                    
                    Rotation(
                        current.yaw + angleDifference(target.yaw, current.yaw) * (1f - smoothFactor * 0.3f),
                        current.pitch + (target.pitch - current.pitch) * (1f - smoothFactor * 0.3f)
                    )
                } else {
                    val strength = (abs(entropyDeviation) / entropyTolerance).coerceIn(0f, 1f)
                    val randomness = entropyRandomnessStrength.random() * strength
                    
                    val baseYaw = current.yaw + angleDifference(target.yaw, current.yaw)
                    val basePitch = current.pitch + (target.pitch - current.pitch)
                    
                    val jitterYaw = (Random.nextFloat() * 2 - 1) * randomness * 1.5f
                    val jitterPitch = (Random.nextFloat() * 2 - 1) * randomness * 1.0f
                    
                    Rotation(
                        baseYaw + jitterYaw,
                        (basePitch + jitterPitch).coerceIn(-90f, 90f)
                    )
                }
            }
            else -> {
                // 标准旋转
                Rotation(
                    current.yaw + angleDifference(target.yaw, current.yaw),
                    current.pitch + (target.pitch - current.pitch)
                )
            }
        }.fixedSensitivity()
    }

    /**
     * 应用模式预测旋转
     */
    private fun applyPatternPredictionRotation(current: Rotation, target: Rotation): Rotation {
        val prediction = predictNextRotation(current)
        val predictionWeight = mlPredictionWeight.random().coerceIn(0f, 1f)
        
        return if (prediction != null && predictionWeight > 0) {
            Rotation(
                current.yaw * 0.1f + prediction.yaw * predictionWeight + target.yaw * (1f - predictionWeight - 0.1f),
                current.pitch * 0.1f + prediction.pitch * predictionWeight + target.pitch * (1f - predictionWeight - 0.1f)
            )
        } else {
            target
        }
    }

    /**
     * 预测下一个旋转
     */
    private fun predictNextRotation(current: Rotation): Rotation? {
        val now = System.currentTimeMillis()
        if (lastPrediction != null && now - lastPredictionTime < 50) {
            return lastPrediction
        }
        
        if (rotationHistory.size < 3) return null
        
        val lastIndex = rotationHistory.size - 1
        val last = rotationHistory[lastIndex]
        val prev = rotationHistory[lastIndex - 1]
        val prevPrev = rotationHistory[lastIndex - 2]
        
        val deltaYaw1 = angleDifference(last.yaw, prev.yaw)
        val deltaYaw2 = angleDifference(prev.yaw, prevPrev.yaw)
        val deltaPitch1 = last.pitch - prev.pitch
        val deltaPitch2 = prev.pitch - prevPrev.pitch
        
        val predictedDeltaYaw = deltaYaw1 * 0.7f + deltaYaw2 * 0.3f
        val predictedDeltaPitch = deltaPitch1 * 0.7f + deltaPitch2 * 0.3f
        
        val maxDelta = 30f
        val limitedDeltaYaw = predictedDeltaYaw.coerceIn(-maxDelta, maxDelta)
        val limitedDeltaPitch = predictedDeltaPitch.coerceIn(-maxDelta, maxDelta)
        
        val prediction = Rotation(
            current.yaw + limitedDeltaYaw,
            current.pitch + limitedDeltaPitch
        ).fixedSensitivity()
        
        lastPrediction = prediction
        lastPredictionTime = now
        
        return prediction
    }

    private fun calculateHypixelRotation(targetBlock: BlockPos): Rotation {
        val player = mc.thePlayer ?: return currRotation
        val eyes = player.eyes
        val blockVec = Vec3(targetBlock.x + 0.5, targetBlock.y + 0.5, targetBlock.z + 0.5)
        val rawYaw = MovementUtils.direction.toDegreesF()
        var adjustedYaw = rawYaw
        val isMovingStraight = player.movementInput.moveForward != 0f && player.movementInput.moveStrafe == 0f
        val baseRotation = toRotation(blockVec, false)

        adjustedYaw = when {
            isMovingStraight -> {
                val yawOption1 = rawYaw + 118f
                val yawOption2 = rawYaw - 118f
                val diff1 = MathHelper.wrapAngleTo180_float(yawOption1 - baseRotation.yaw)
                val diff2 = MathHelper.wrapAngleTo180_float(yawOption2 - baseRotation.yaw)
                if (abs(diff1) < abs(diff2)) yawOption1 else yawOption2
            }
            else -> rawYaw + 132f
        }
        val placeFace = placeRotation?.placeInfo?.enumFacing ?: EnumFacing.UP
        val faceVec = placeFace.directionVec
        val (minX, maxX) = when (faceVec.x) {
            1 -> 1.0f to 1.0f
            -1 -> 0.0f to 0.0f
            else -> 0.1f to 0.9f
        }

        val (minY, maxY) = when (faceVec.y) {
            1 -> 1.0f to 1.0f
            -1 -> 0.0f to 0.0f
            else -> 0.1f to 0.9f
        }

        val (minZ, maxZ) = when (faceVec.z) {
            1 -> 1.0f to 1.0f
            -1 -> 0.0f to 0.0f
            else -> 0.1f to 0.9f
        }

        val step = 0.1f
        var bestPitch = currRotation.pitch
        var minDiff = Float.MAX_VALUE

        val stepsX = ((maxX - minX) * 10).toInt()
        val stepsY = ((maxY - minY) * 10).toInt()
        val stepsZ = ((maxZ - minZ) * 10).toInt()

        for (i in 0..stepsX) {
            val x = minX + i * step
            for (j in 0..stepsY) {
                val y = minY + j * step
                for (k in 0..stepsZ) {
                    val z = minZ + k * step
                    val candidate = Vec3(
                        targetBlock.x + x.toDouble(),
                        targetBlock.y + y.toDouble(),
                        targetBlock.z + z.toDouble()
                    )
                    val candidateRotation = toRotation(candidate, false)
                    val diff = rotationDifference(candidateRotation, currRotation)

                    if (diff < minDiff) {
                        minDiff = diff
                        bestPitch = candidateRotation.pitch
                    }
                }
            }
        }

        return Rotation(
            adjustedYaw,
            bestPitch.coerceIn(hypixelMinPitch, hypixelMaxPitch)
        ).fixedSensitivity()
    }

    val onTick = handler<GameTickEvent> {
        val target = placeRotation?.placeInfo

        val raycastProperly = !(scaffoldMode == "Expand" && expandLength > 1 || shouldGoDown) && options.rotationsActive

        val raycast = performBlockRaytrace(currRotation, mc.playerController.blockReachDistance)

        var alreadyPlaced = false

        if (extraClicks) {
            val doubleClick = if (simulateDoubleClicking) RandomUtils.nextInt(-1, 1) else 0

            val clicks = extraClick.clicks + doubleClick

            repeat(clicks) {
                extraClick.clicks--

                doPlaceAttempt(raycast, it + 1 == clicks) { alreadyPlaced = true }
            }
        }

        if (target == null) {
            if (placeDelayValue.isActive()) {
                delayTimer.reset()
            }
            return@handler
        }

        if (alreadyPlaced || SilentHotbar.modifiedThisTick) {
            return@handler
        }

        raycast.let {
            if (!options.rotationsActive || it != null && it.blockPos == target.blockPos && (!raycastProperly || it.sideHit == target.enumFacing)) {
                val result = if (raycastProperly && it != null) {
                    PlaceInfo(it.blockPos, it.sideHit, it.hitVec)
                } else {
                    target
                }

                place(result)
            }
        }
    }

    val onSneakSlowDown = handler<SneakSlowDownEvent> { event ->
        if (!isEagleEnabled || eagle != "Normal") {
            return@handler
        }

        event.forward *= eagleSpeed / 0.3f
        event.strafe *= eagleSpeed / 0.3f
    }

    val onMovementInput = handler<MovementInputEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (!isGodBridgeEnabled || !player.onGround) return@handler

        if (waitForRots) {
            godBridgeTargetRotation?.run {
                event.originalInput.sneak =
                    event.originalInput.sneak || rotationDifference(this, currRotation) > getFixedAngleDelta()
            }
        }

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = currRotation.yaw

        simPlayer.tick()

        if (!simPlayer.onGround && !isManualJumpOptionActive || blocksPlacedUntilJump > blocksToJump) {
            event.originalInput.jump = true

            blocksPlacedUntilJump = 0

            blocksToJump = blocksToJumpRange.random()
        }
    }

    fun update() {
        val player = mc.thePlayer ?: return
        val holdingItem = player.heldItem?.item is ItemBlock

        if (!holdingItem && (autoBlock == "Off" || InventoryUtils.findBlockInHotbar() == null)) {
            return
        }

        findBlock(scaffoldMode == "Expand" && expandLength > 1, searchMode == "Area")
    }

    private fun setRotation(rotation: Rotation, ticks: Int) {
        val player = mc.thePlayer ?: return

        if (scaffoldMode == "Telly" && player.isMoving) {
            if (player.airTicks < ticksUntilRotation.random() && ticksUntilJump >= jumpTicks) {
                return
            }
        }

        setTargetRotation(rotation, options, ticks)
    }

    private fun findBlock(expand: Boolean, area: Boolean) {
        val player = mc.thePlayer ?: return

        if (!shouldKeepLaunchPosition) launchY = player.posY.roundToInt()

        val blockPosition = if (shouldGoDown) {
            if (player.posY == player.posY.roundToInt() + 0.5) {
                BlockPos(player.posX, player.posY - 0.6, player.posZ)
            } else {
                BlockPos(player.posX, player.posY - 0.6, player.posZ).down()
            }
        } else if (shouldKeepLaunchPosition && launchY <= player.posY) {
            BlockPos(player.posX, launchY - 1.0, player.posZ)
        } else if (player.posY == player.posY.roundToInt() + 0.5) {
            BlockPos(player)
        } else {
            BlockPos(player).down()
        }

        if (!expand && (!blockPosition.isReplaceable || search(
                blockPosition, !shouldGoDown, area, shouldPlaceHorizontally
            ))
        ) {
            return
        }

        if (expand) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z

            repeat(expandLength) {
                if (search(blockPosition.add(x * it, 0, z * it), false, area)) return
            }
            return
        }

        val (horizontal, vertical) = if (scaffoldMode == "Telly") {
            5 to 3
        } else if (allowClutching) {
            horizontalClutchBlocks to verticalClutchBlocks
        } else {
            1 to 1
        }

        BlockPos.getAllInBox(
            blockPosition.add(-horizontal, 0, -horizontal), blockPosition.add(horizontal, -vertical, horizontal)
        ).sortedBy {
            BlockUtils.getCenterDistance(it)
        }.forEach {
            if (it.canBeClicked() || search(it, !shouldGoDown, area, shouldPlaceHorizontally)) {
                return
            }
        }
    }

    private fun place(placeInfo: PlaceInfo) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (!delayTimer.hasTimePassed() || shouldKeepLaunchPosition && launchY - 1 != placeInfo.vec3.yCoord.toInt() && scaffoldMode != "Expand") return

        val currentSlot = SilentHotbar.currentSlot

        var stack = player.hotBarSlot(currentSlot).stack

        if (stack == null || stack.item !is ItemBlock || (stack.item as ItemBlock).block is BlockBush || stack.stackSize <= 0 || sortByHighestAmount || earlySwitch) {
            val blockSlot = if (sortByHighestAmount) {
                InventoryUtils.findLargestBlockStackInHotbar() ?: return
            } else if (earlySwitch) {
                InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch)
                    ?: InventoryUtils.findBlockInHotbar() ?: return
            } else {
                InventoryUtils.findBlockInHotbar() ?: return
            }

            stack = player.hotBarSlot(blockSlot).stack

            if ((stack.item as? ItemBlock)?.canPlaceBlockOnSide(
                    world, placeInfo.blockPos, placeInfo.enumFacing, player, stack
                ) == false
            ) {
                return
            }

            if (autoBlock != "Off") {
                SilentHotbar.selectSlotSilently(this, blockSlot, render = autoBlock == "Pick", resetManually = true)
            }
        }

        tryToPlaceBlock(stack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)

        if (autoBlock == "Switch") SilentHotbar.resetSlot(this, true)

        findBlockToSwitchNextTick(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun doPlaceAttempt(raytrace: MovingObjectPosition?, lastClick: Boolean, onSuccess: () -> Unit = { }) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val stack = player.hotBarSlot(SilentHotbar.currentSlot).stack ?: return

        if (stack.item !is ItemBlock || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)) {
            return
        }

        raytrace ?: return

        val block = stack.item as ItemBlock

        val canPlaceOnUpperFace = block.canPlaceBlockOnSide(
            world, raytrace.blockPos, EnumFacing.UP, player, stack
        )

        val shouldPlace = if (placementAttempt == "Fail") {
            !block.canPlaceBlockOnSide(world, raytrace.blockPos, raytrace.sideHit, player, stack)
        } else {
            if (shouldKeepLaunchPosition) {
                raytrace.blockPos.y == launchY - 1 && !canPlaceOnUpperFace
            } else if (shouldPlaceHorizontally) {
                !canPlaceOnUpperFace
            } else {
                raytrace.blockPos.y <= player.posY.toInt() - 1 && !(raytrace.blockPos.y == player.posY.toInt() - 1 && canPlaceOnUpperFace && raytrace.sideHit == EnumFacing.UP)
            }
        }

        if (!raytrace.typeOfHit.isBlock || !shouldPlace) {
            return
        }

        tryToPlaceBlock(stack, raytrace.blockPos, raytrace.sideHit, raytrace.hitVec, attempt = true) { onSuccess() }

        if (lastClick) {
            findBlockToSwitchNextTick(stack)
        }

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking && player.isSneaking) {
                player.isSneaking = false
            }
        }

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindRight)) {
            mc.gameSettings.keyBindRight.pressed = false
        }
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindLeft)) {
            mc.gameSettings.keyBindLeft.pressed = false
        }

        if (autoF5) {
            mc.gameSettings.thirdPersonView = 0
        }

        placeRotation = null
        mc.timer.timerSpeed = 1f

        SilentHotbar.resetSlot(this)

        options.instant = false
    }

    val onMove = handler<MoveEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (!safeWalkValue.isActive() || shouldGoDown) {
            return@handler
        }

        if (airSafe || player.onGround) {
            event.isSafeWalk = true
        }
    }

    val jumpHandler = handler<JumpEvent> { event ->
        if (!jumpStrafe) return@handler

        if (event.eventState == EventState.POST) {
            strafe(
                (if (!isLookingDiagonally) jumpStraightStrafe else jumpDiagonalStrafe).random()
            )
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val player = mc.thePlayer ?: return@handler

        val shouldBother =
            !(shouldGoDown || scaffoldMode == "Expand" && expandLength > 1) && extraClicks && (player.isMoving || speed > 0.03)

        if (shouldBother) {
            currRotation.let {
                performBlockRaytrace(it, mc.playerController.blockReachDistance)?.let { raytrace ->
                    val timePassed = System.currentTimeMillis() - extraClick.lastClick >= extraClick.delay

                    if (raytrace.typeOfHit.isBlock && timePassed) {
                        extraClick = ExtraClickInfo(
                            TimeUtils.randomClickDelay(extraClickCPS.first, extraClickCPS.last),
                            System.currentTimeMillis(),
                            extraClick.clicks + 1
                        )
                    }
                }
            }
        }

        if (!mark) {
            return@handler
        }

        repeat(if (scaffoldMode == "Expand") expandLength + 1 else 2) {
            val yaw = player.rotationYaw.toRadiansD()
            val x = if (omniDirectionalExpand) -sin(yaw).roundToInt() else player.horizontalFacing.directionVec.x
            val z = if (omniDirectionalExpand) cos(yaw).roundToInt() else player.horizontalFacing.directionVec.z
            val blockPos = BlockPos(
                player.posX + x * it,
                if (shouldKeepLaunchPosition && launchY <= player.posY) launchY - 1.0 else player.posY - (if (player.posY == player.posY + 0.5) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0,
                player.posZ + z * it
            )
            val placeInfo = PlaceInfo.get(blockPos)

            if (blockPos.isReplaceable && placeInfo != null) {
                RenderUtils.drawBlockBox(blockPos, markColor, false)
                return@handler
            }
        }
    }

    fun search(
        blockPosition: BlockPos,
        raycast: Boolean,
        area: Boolean,
        horizontalOnly: Boolean = false,
    ): Boolean {
        val player = mc.thePlayer ?: return false

        options.instant = false

        if (!blockPosition.isReplaceable) {
            if (autoF5) mc.gameSettings.thirdPersonView = 0
            return false
        } else {
            if (autoF5 && mc.gameSettings.thirdPersonView != 1) mc.gameSettings.thirdPersonView = 1
        }

        val maxReach = mc.playerController.blockReachDistance

        val eyes = player.eyes
        var placeRotation: PlaceRotation? = null

        var currPlaceRotation: PlaceRotation?

        for (side in EnumFacing.entries) {
            if (horizontalOnly && side.axis == EnumFacing.Axis.Y) {
                continue
            }

            val neighbor = blockPosition.offset(side)

            if (!neighbor.canBeClicked()) {
                continue
            }

            if (!area || isGodBridgeEnabled) {
                currPlaceRotation =
                    findTargetPlace(blockPosition, neighbor, Vec3(0.5, 0.5, 0.5), side, eyes, maxReach, raycast)
                        ?: continue

                placeRotation = compareDifferences(currPlaceRotation, placeRotation)
            } else {
                for (x in 0.1..0.9 step 0.1) {
                    for (y in 0.1..0.9 step 0.1) {
                        for (z in 0.1..0.9 step 0.1) {
                            currPlaceRotation =
                                findTargetPlace(blockPosition, neighbor, Vec3(x, y, z), side, eyes, maxReach, raycast)
                                    ?: continue

                            placeRotation = compareDifferences(currPlaceRotation, placeRotation)
                        }
                    }
                }
            }
        }

        placeRotation ?: return false

        if (options.rotationsActive && !isGodBridgeEnabled) {
            val rotDiff = rotationDifference(placeRotation.rotation, currRotation)
            val rotDiff2 = rotationDifference(placeRotation.rotation / 90F, currRotation / 90F)

            val simPlayer = SimulatedPlayer.fromClientPlayer(player.movementInput)
            simPlayer.tick()

            options.instant =
                blockSafe && simPlayer.fallDistance > player.fallDistance + 0.05 && rotDiff > rotDiff2 / 2

            val targetRotation = placeRotation.rotation.copy()
            
            // 根据选择的旋转模式应用不同的旋转算法
            val finalRotation = when (modeList.get()) {
                "Acceleration" -> applyAccelerationRotation(currRotation, targetRotation)
                "AdvancedSpeed" -> applyAdvancedSpeedRotation(currRotation, targetRotation)
                "Dynamic" -> applyDynamicRotation(currRotation, targetRotation)
                "Entropy" -> applyEntropyRotation(currRotation, targetRotation)
                "PatternPrediction" -> applyPatternPredictionRotation(currRotation, targetRotation)
                else -> targetRotation
            }
            
            setRotation(finalRotation, if (scaffoldMode == "Telly") 1 else options.resetTicks)
        }

        this.placeRotation = placeRotation
        return true
    }

    private fun modifyVec(original: Vec3, direction: EnumFacing, pos: Vec3, shouldModify: Boolean): Vec3 {
        if (!shouldModify) {
            return original
        }

        val x = original.xCoord
        val y = original.yCoord
        val z = original.zCoord

        val side = direction.opposite

        return when (side.axis ?: return original) {
            EnumFacing.Axis.Y -> Vec3(x, pos.yCoord + side.directionVec.y.coerceAtLeast(0), z)
            EnumFacing.Axis.X -> Vec3(pos.xCoord + side.directionVec.x.coerceAtLeast(0), y, z)
            EnumFacing.Axis.Z -> Vec3(x, y, pos.zCoord + side.directionVec.z.coerceAtLeast(0))
        }

    }

    private fun findTargetPlace(
        pos: BlockPos, offsetPos: BlockPos, vec3: Vec3, side: EnumFacing, eyes: Vec3, maxReach: Float, raycast: Boolean,
    ): PlaceRotation? {
        val world = mc.theWorld ?: return null

        val vec = (Vec3(pos) + vec3).addVector(
            side.directionVec.x * vec3.xCoord, side.directionVec.y * vec3.yCoord, side.directionVec.z * vec3.zCoord
        )

        val distance = eyes.distanceTo(vec)

        if (raycast && (distance > maxReach || world.rayTraceBlocks(eyes, vec, false, true, false) != null)) {
            return null
        }

        val diff = vec - eyes

        if (side.axis != EnumFacing.Axis.Y) {
            val dist = abs(if (side.axis == EnumFacing.Axis.Z) diff.zCoord else diff.xCoord)

            if (dist < minDist && scaffoldMode != "Telly") {
                return null
            }
        }

        var rotation = toRotation(vec, false)

        val roundYaw90 = round(rotation.yaw / 90f) * 90f
        val roundYaw45 = round(rotation.yaw / 45f) * 45f

        rotation = when (options.rotationMode) {
            "Stabilized" -> Rotation(roundYaw45, rotation.pitch)
            "ReverseYaw" -> Rotation(if (!isLookingDiagonally) roundYaw90 else roundYaw45, rotation.pitch)
            else -> rotation
        }.fixedSensitivity()

        performBlockRaytrace(currRotation, maxReach)?.let { raytrace ->
            if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite)) {
                return PlaceRotation(
                    PlaceInfo(
                        raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                    ), currRotation
                )
            }
        }

        val raytrace = performBlockRaytrace(rotation, maxReach) ?: return null

        val multiplier = if (options.legitimize) 3 else 1

        if (raytrace.blockPos == offsetPos && (!raycast || raytrace.sideHit == side.opposite) && canUpdateRotation(
                currRotation, rotation, multiplier
            )
        ) {
            return PlaceRotation(
                PlaceInfo(
                    raytrace.blockPos, side.opposite, modifyVec(raytrace.hitVec, side, Vec3(offsetPos), !raycast)
                ), rotation
            )
        }

        return null
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): MovingObjectPosition? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTraceBlocks(eyes, reach, false, false, true)
    }

    private fun compareDifferences(
        new: PlaceRotation, old: PlaceRotation?, rotation: Rotation = currRotation,
    ): PlaceRotation {
        if (old == null || rotationDifference(new.rotation, rotation) < rotationDifference(old.rotation, rotation)) {
            return new
        }

        return old
    }

    private fun findBlockToSwitchNextTick(stack: ItemStack) {
        if (autoBlock !in arrayOf("Off", "Switch")) return

        val switchAmount = if (earlySwitch) amountBeforeSwitch else 0

        if (stack.stackSize > switchAmount) return

        val switchSlot = if (earlySwitch) {
            InventoryUtils.findBlockStackInHotbarGreaterThan(amountBeforeSwitch) ?: InventoryUtils.findBlockInHotbar()
            ?: return
        } else {
            InventoryUtils.findBlockInHotbar()
        } ?: return

        SilentHotbar.selectSlotSilently(this, switchSlot, render = autoBlock == "Pick", resetManually = true)
    }

    private fun updatePlacedBlocksForTelly() {
        if (blocksUntilAxisChange > horizontalPlacements + verticalPlacements) {
            blocksUntilAxisChange = 0

            horizontalPlacements = horizontalPlacementsRange.random()
            verticalPlacements = verticalPlacementsRange.random()
            return
        }

        blocksUntilAxisChange++
    }

    private fun tryToPlaceBlock(
        stack: ItemStack, clickPos: BlockPos, side: EnumFacing, hitVec: Vec3, attempt: Boolean = false,
        onSuccess: () -> Unit = { }
    ): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        val prevSize = stack.stackSize

        val clickedSuccessfully = thePlayer.onPlayerRightClick(clickPos, side, hitVec, stack)

        if (clickedSuccessfully) {
            if (!attempt) {
                delayTimer.reset()

                if (thePlayer.onGround) {
                    thePlayer.motionX *= speedModifier
                    thePlayer.motionZ *= speedModifier
                }
            }

            if (swing) thePlayer.swingItem()
            else sendPacket(C0APacketAnimation())

            if (isManualJumpOptionActive) blocksPlacedUntilJump++

            updatePlacedBlocksForTelly()

            if (stack.stackSize <= 0) {
                thePlayer.inventory.mainInventory[SilentHotbar.currentSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(thePlayer, stack)
            } else if (stack.stackSize != prevSize || mc.playerController.isInCreativeMode) mc.entityRenderer.itemRenderer.resetEquippedProgress()

            placeRotation = null

            placedBlocksWithoutEagle++

            onSuccess()
        } else {
            if (thePlayer.sendUseItem(stack)) mc.entityRenderer.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    fun handleMovementOptions(input: MovementInput) {
        val player = mc.thePlayer ?: return

        if (!state) {
            return
        }

        if (!slow && speedLimiter && speed > speedLimit) {
            input.moveStrafe = 0f
            input.moveForward = 0f
            return
        }

        when (zitterMode.lowercase()) {
            "off" -> {
                return
            }

            "smooth" -> {
                val notOnGround = !player.onGround || !player.isCollidedVertically

                if (player.onGround) {
                    input.sneak = eagleSneaking || GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
                }

                if (input.jump || mc.gameSettings.keyBindJump.isKeyDown || notOnGround) {
                    zitterTickTimer.reset()

                    if (useSneakMidAir) {
                        input.sneak = true
                    }

                    if (!notOnGround && !input.jump) {
                        input.moveStrafe = if (zitterDirection) 1f else -1f
                    } else {
                        input.moveStrafe = 0f
                    }

                    zitterDirection = !zitterDirection

                    if (mc.gameSettings.keyBindLeft.isKeyDown) {
                        input.moveStrafe++
                    }

                    if (mc.gameSettings.keyBindRight.isKeyDown) {
                        input.moveStrafe--
                    }
                    return
                }

                if (zitterTickTimer.hasTimePassed()) {
                    zitterDirection = !zitterDirection
                    zitterTickTimer.reset()
                } else {
                    zitterTickTimer.update()
                }

                if (zitterDirection) {
                    input.moveStrafe = -1f
                } else {
                    input.moveStrafe = 1f
                }
            }

            "teleport" -> {
                strafe(zitterSpeed)
                val yaw = (player.rotationYaw + if (zitterDirection) 90.0 else -90.0).toRadians()
                player.motionX -= sin(yaw) * zitterStrength
                player.motionZ += cos(yaw) * zitterStrength
                zitterDirection = !zitterDirection
            }
        }
    }

    private var isOnRightSide = false

    private fun generateGodBridgeRotations(ticks: Int) {
        val player = mc.thePlayer ?: return

        val direction = if (options.applyServerSide) {
            MovementUtils.direction.toDegreesF() + 180f
        } else MathHelper.wrapAngleTo180_float(player.rotationYaw)

        val movingYaw = round(direction / 45) * 45

        val steps45 = arrayListOf(-135f, -45f, 45f, 135f)

        val isMovingStraight = if (options.applyServerSide) {
            movingYaw % 90 == 0f
        } else movingYaw in steps45 && player.movementInput.isSideways

        if (!player.isNearEdge(2.5f)) return

        if (!player.isMoving) {
            placeRotation?.run {
                val axisMovement = floor(this.rotation.yaw / 90) * 90

                val yaw = axisMovement + 45f
                val pitch = 75f

                setRotation(Rotation(yaw, pitch), ticks)
                return
            }

            if (!options.keepRotation) return
        }

        val rotation = if (isMovingStraight) {
            if (player.onGround) {
                isOnRightSide = floor(player.posX + cos(movingYaw.toRadians()) * 0.5) != floor(player.posX) || floor(
                    player.posZ + sin(movingYaw.toRadians()) * 0.5
                ) != floor(player.posZ)

                val posInDirection =
                    BlockPos(player.positionVector.offset(EnumFacing.fromAngle(movingYaw.toDouble()), 0.6))

                val isLeaningOffBlock = player.position.down().block == air
                val nextBlockIsAir = posInDirection.down().block == air

                if (isLeaningOffBlock && nextBlockIsAir) {
                    isOnRightSide = !isOnRightSide
                }
            }

            val side = if (options.applyServerSide) {
                if (isOnRightSide) 45f else -45f
            } else 0f

            Rotation(movingYaw + side, if (useOptimizedPitch) 73.5f else customGodPitch)
        } else {
            Rotation(movingYaw, 75.6f)
        }.fixedSensitivity()

        godBridgeTargetRotation = rotation

        setRotation(rotation, ticks)
    }

    override val tag
        get() = if (towerMode != "None") ("$scaffoldMode | $towerMode") else scaffoldMode

    private fun sign(value: Float): Float = if (value > 0) 1f else if (value < 0) -1f else 0f

    // ==================== 内部类定义 ====================

    data class ExtraClickInfo(val delay: Int, val lastClick: Long, var clicks: Int)
    
    data class RotationData(val yaw: Float, val pitch: Float, val timestamp: Long)
}