/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code AiNeko
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithSimulatedPosition
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.features.module.modules.world.Fucker
import net.ccbluex.liquidbounce.features.module.modules.world.Nuker
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.*
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.getAttackCooldownProgress
import net.ccbluex.liquidbounce.utils.attack.CooldownHelper.resetLastAttackedTicks
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils.runTimeTicks
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverOpenInventory
import net.ccbluex.liquidbounce.utils.inventory.ItemUtils.isConsumingItem
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawCircle
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawEntityBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.rotation.RandomizationSettings
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationSettings
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isRotationFaced
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.isVisible
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.rotationDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.searchCenter
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.simulation.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TickedActions.nextTick
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemAxe
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.*
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.RELEASE_USE_ITEM
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.potion.Potion
import net.minecraft.util.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import kotlin.math.max
import kotlin.math.roundToInt

object Aura : Module("Aura", Category.COMBAT) {
    /**
     * OPTIONS
     */

    private val simulateCooldown by boolean("SimulateCooldown", false)
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false) { !simulateCooldown }

    // 模拟抖动系统
    private val simulateJitter by boolean("SimulateJitter", false)
    private val jitterAmplitudeYaw by float("JitterAmplitudeYaw", 2.5f, 0f..10f) { simulateJitter }
    private val jitterAmplitudePitch by float("JitterAmplitudePitch", 1.5f, 0f..10f) { simulateJitter }
    private val jitterFrequencyYaw by float("JitterFrequencyYaw", 3.7f, 0.1f..20f) { simulateJitter }
    private val jitterFrequencyPitch by float("JitterFrequencyPitch", 2.3f, 0.1f..20f) { simulateJitter }
    private val jitterPhaseOffsetYaw by float("JitterPhaseOffsetYaw", 0.5f, 0f..6.283f) { simulateJitter }
    private val jitterPhaseOffsetPitch by float("JitterPhaseOffsetPitch", 1.2f, 0f..6.283f) { simulateJitter }
    private val jitterWaveTypeYaw by choices("JitterWaveTypeYaw", arrayOf("Sine", "Cosine", "Triangle", "Sawtooth", "Square", "Noise"), "Sine") { simulateJitter }
    private val jitterWaveTypePitch by choices("JitterWaveTypePitch", arrayOf("Sine", "Cosine", "Triangle", "Sawtooth", "Square", "Noise"), "Sine") { simulateJitter }
    private val jitterDampingFactorYaw by float("JitterDampingFactorYaw", 0.95f, 0.5f..1f) { simulateJitter }
    private val jitterDampingFactorPitch by float("JitterDampingFactorPitch", 0.97f, 0.5f..1f) { simulateJitter }
    private val jitterRandomSeed by int("JitterRandomSeed", 12345, 0..100000) { simulateJitter }
    private val jitterApplyOnlyWhenMoving by boolean("JitterApplyOnlyWhenMoving", true) { simulateJitter }
    private val jitterMaxVelocityThreshold by float("JitterMaxVelocityThreshold", 0.2f, 0f..1f) { simulateJitter && jitterApplyOnlyWhenMoving }
    private val jitterSmoothTransitions by boolean("JitterSmoothTransitions", true) { simulateJitter }
    private val jitterTransitionTime by float("JitterTransitionTime", 0.3f, 0.1f..2f) { simulateJitter && jitterSmoothTransitions }
    private val jitterSyncWithAttack by boolean("JitterSyncWithAttack", false) { simulateJitter }
    private val jitterAttackMultiplier by float("JitterAttackMultiplier", 1.5f, 1f..3f) { simulateJitter && jitterSyncWithAttack }
    private val jitterNoiseOctaves by int("JitterNoiseOctaves", 3, 1..8) { simulateJitter }
    private val jitterNoisePersistence by float("JitterNoisePersistence", 0.5f, 0.1f..1f) { simulateJitter }
    private val jitterNoiseLacunarity by float("JitterNoiseLacunarity", 2f, 1f..4f) { simulateJitter }

    // 模拟滞后系统
    private val simulateLag by boolean("SimulateLag", false)
    private val lagLatencyMs by int("LagLatencyMs", 50, 0..500) { simulateLag }
    private val lagJitterMs by int("LagJitterMs", 10, 0..100) { simulateLag }
    private val lagPacketLossPercent by float("LagPacketLossPercent", 2f, 0f..20f) { simulateLag }
    private val lagBurstDurationMs by int("LagBurstDurationMs", 200, 0..2000) { simulateLag }
    private val lagBurstIntervalMs by int("LagBurstIntervalMs", 5000, 1000..30000) { simulateLag }
    private val lagSimulatePingSpikes by boolean("LagSimulatePingSpikes", false) { simulateLag }
    private val lagSpikeFrequency by float("LagSpikeFrequency", 0.1f, 0.01f..1f) { simulateLag && lagSimulatePingSpikes }
    private val lagSpikeAmplitudeMs by int("LagSpikeAmplitudeMs", 200, 50..1000) { simulateLag && lagSimulatePingSpikes }
    private val lagSmoothLatency by boolean("LagSmoothLatency", true) { simulateLag }
    private val lagSmoothingFactor by float("LagSmoothingFactor", 0.3f, 0.1f..0.9f) { simulateLag && lagSmoothLatency }
    private val lagApplyToRotations by boolean("LagApplyToRotations", true) { simulateLag }
    private val lagApplyToAttacks by boolean("LagApplyToAttacks", false) { simulateLag }
    private val lagApplyToMovement by boolean("LagApplyToMovement", false) { simulateLag }
    private val lagRandomSeed by int("LagRandomSeed", 54321, 0..100000) { simulateLag }

    // 模拟超前系统
    private val simulateLead by boolean("SimulateLead", false)
    private val leadPredictionTicks by int("LeadPredictionTicks", 2, 0..10) { simulateLead }
    private val leadVelocityMultiplier by float("LeadVelocityMultiplier", 1.2f, 0.5f..3f) { simulateLead }
    private val leadAccelerationFactor by float("LeadAccelerationFactor", 0.5f, 0f..2f) { simulateLead }
    private val leadJerkFactor by float("LeadJerkFactor", 0.2f, 0f..1f) { simulateLead }
    private val leadAdaptivePrediction by boolean("LeadAdaptivePrediction", true) { simulateLead }
    private val leadMinPredictionTicks by int("LeadMinPredictionTicks", 1, 0..5) { simulateLead && leadAdaptivePrediction }
    private val leadMaxPredictionTicks by int("LeadMaxPredictionTicks", 5, 1..15) { simulateLead && leadAdaptivePrediction }
    private val leadDistanceThreshold by float("LeadDistanceThreshold", 4f, 0f..10f) { simulateLead && leadAdaptivePrediction }
    private val leadVelocityThreshold by float("LeadVelocityThreshold", 0.5f, 0f..2f) { simulateLead && leadAdaptivePrediction }
    private val leadSmoothPrediction by boolean("LeadSmoothPrediction", true) { simulateLead }
    private val leadSmoothingFactor by float("LeadSmoothingFactor", 0.7f, 0.1f..0.9f) { simulateLead && leadSmoothPrediction }
    private val leadSystemApplyToYaw by boolean("LeadSystemApplyToYaw", true) { simulateLead }
    private val leadSystemApplyToPitch by boolean("LeadSystemApplyToPitch", true) { simulateLead }
    private val leadNoiseReduction by boolean("LeadNoiseReduction", false) { simulateLead }
    private val leadNoiseThreshold by float("LeadNoiseThreshold", 0.1f, 0f..1f) { simulateLead && leadNoiseReduction }
    private val leadRandomSeed by int("LeadRandomSeed", 98765, 0..100000) { simulateLead }

    // CPS - Attack speed
    private val cps by intRange("CPS", 5..8, 1..50) { !simulateCooldown }.onChanged {
        attackDelay = randomClickDelay(it.first, it.last)
    }

    // 攻击范围动态调整
    private val attackRangeSprintReduceMaxValue by float("AttackRangeSprintReduceMaxValue", 0.4f, 0f..6f)
    private val attackRangeSprintReduceMinValue by float("AttackRangeSprintReduceMinValue", 0f, 0f..6f)
    private val attackRangeAirReduceMaxValue by float("AttackRangeAirReduceMaxValue", 0.6f, 0f..6f)
    private val attackRangeAirReduceMinValue by float("AttackRangeAirReduceMinValue", 0.2f, 0f..6f)
    private val minAttackHurtTime by int("MinAttackHurtTime", 0, 0..10)
    private val maxAttackHurtTime by int("MaxAttackHurtTime", 10, 0..10)

    private val hurtTime by int("HurtTime", 10, 0..10) { !simulateCooldown }

    private val activationSlot by boolean("ActivationSlot", false)
    private val preferredSlot by int("PreferredSlot", 1, 1..9) { activationSlot }

    private val clickOnly by boolean("ClickOnly", false)

    // Range
    private val range: Float by float("Range", 3.7f, 1f..8f).onChanged {
        blockRange = blockRange.coerceAtMost(it)
    }
    private val scanRange by float("ScanRange", 2f, 0f..10f)
    private val throughWallsRange by float("ThroughWallsRange", 3f, 0f..8f)
    private val rangeSprintReduction by float("RangeSprintReduction", 0f, 0f..0.4f)

    // Modes
    private val priority by choices(
        "Priority", arrayOf(
            "Health",
            "Distance",
            "Direction",
            "LivingTime",
            "Armor",
            "HurtResistance",
            "HurtTime",
            "HealthAbsorption",
            "RegenAmplifier",
            "OnLadder",
            "InLiquid",
            "InWeb"
        ), "Distance"
    )
    private val targetMode by choices("TargetMode", arrayOf("Single", "Switch", "Multi"), "Switch")
    private val limitedMultiTargets by int("LimitedMultiTargets", 0, 0..50) { targetMode == "Multi" }
    private val maxSwitchFOV by float("MaxSwitchFOV", 90f, 30f..180f) { targetMode == "Switch" }

    // Delay
    private val switchDelay by int("SwitchDelay", 15, 1..1000) { targetMode == "Switch" }

    // Bypass
    private val swing by boolean("Swing", true)
    private val keepSprint by boolean("KeepSprint", true)

    // Settings
    private val autoF5 by boolean("AutoF5", false)
    private val onScaffold by boolean("OnScaffold", false)
    private val onDestroyBlock by boolean("OnDestroyBlock", false)

    // AutoBlock
    val autoBlock by choices("AutoBlock", arrayOf("Off", "Packet", "Fake"), "Packet")
    private val blockMaxRange by float("BlockMaxRange", 3f, 0f..8f) { autoBlock == "Packet" }
    private val unblockMode by choices(
        "UnblockMode", arrayOf("Stop", "Switch", "Empty"), "Stop"
    ) { autoBlock == "Packet" }
    private val releaseAutoBlock by boolean("ReleaseAutoBlock", true) { autoBlock !in arrayOf("Off", "Fake") }
    val forceBlockRender by boolean("ForceBlockRender", true) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && releaseAutoBlock
    }
    private val ignoreTickRule by boolean("IgnoreTickRule", false) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && releaseAutoBlock
    }
    private val blockRate by int("BlockRate", 100, 1..100) { autoBlock !in arrayOf("Off", "Fake") && releaseAutoBlock }

    private val uncpAutoBlock by boolean("UpdatedNCPAutoBlock", false) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && !releaseAutoBlock
    }

    private val switchStartBlock by boolean("SwitchStartBlock", false) { autoBlock !in arrayOf("Off", "Fake") }

    private val interactAutoBlock by boolean("InteractAutoBlock", true) { autoBlock !in arrayOf("Off", "Fake") }

    val blinkAutoBlock by boolean("BlinkAutoBlock", false) { autoBlock !in arrayOf("Off", "Fake") }

    private val blinkBlockTicks by int("BlinkBlockTicks", 3, 2..5) {
        autoBlock !in arrayOf(
            "Off", "Fake"
        ) && blinkAutoBlock
    }

    // AutoBlock conditions
    private val smartAutoBlock by boolean("SmartAutoBlock", false) { autoBlock == "Packet" }

    // Ignore all blocking conditions, except for block rate, when standing still
    private val forceBlock by boolean("ForceBlockWhenStill", true) { smartAutoBlock }

    // Don't block if target isn't holding a sword or an axe
    private val checkWeapon by boolean("CheckEnemyWeapon", true) { smartAutoBlock }

    private var blockRange: Float by float("BlockRange", range, 1f..8f) {
        smartAutoBlock
    }.onChange { _, new ->
        new.coerceAtMost(this@Aura.range)
    }

    // Don't block when you can't get damaged
    private val maxOwnHurtTime by int("MaxOwnHurtTime", 3, 0..10) { smartAutoBlock }

    // Don't block if target isn't looking at you
    private val maxDirectionDiff by float("MaxOpponentDirectionDiff", 60f, 30f..180f) { smartAutoBlock }

    // Don't block if target is swinging an item and therefore cannot attack
    private val maxSwingProgress by int("MaxOpponentSwingProgress", 1, 0..5) { smartAutoBlock }

    // Rotations
    private val options = RotationSettings(this).withoutKeepRotation()

    // Raycast
    private val raycastValue = boolean("RayCast", true) { options.rotationsActive }
    private val raycast by raycastValue
    private val raycastIgnored by boolean(
        "RayCastIgnored", false
    ) { raycastValue.isActive() && options.rotationsActive }
    private val livingRaycast by boolean("LivingRayCast", true) { raycastValue.isActive() && options.rotationsActive }

    // Hit delay
    private val useHitDelay by boolean("UseHitDelay", false)
    private val hitDelayTicks by int("HitDelayTicks", 1, 1..5) { useHitDelay }

    private val generateClicksBasedOnDist by boolean("GenerateClicksBasedOnDistance", false)
    private val cpsMultiplier by intRange("CPS-Multiplier", 1..2, 1..10) { generateClicksBasedOnDist }
    private val distanceFactor by floatRange("DistanceFactor", 5F..10F, 1F..10F) { generateClicksBasedOnDist }

    private val generateSpotBasedOnDistance by boolean("GenerateSpotBasedOnDistance", false) { options.rotationsActive }

    // 平滑算法
    private val smoothingAlgorithm by choices(
        "SmoothingAlgorithm",
        arrayOf(
            "None",
            "MovingAverage",
            "ExponentialSmoothing",
            "GaussianSmoothing",
            "NGramSmoothing",
            "FixedPointA",
            "FixedPointB",
            "OptimalFixedPointA"
        ),
        "None"
    ) { options.rotationsActive }

    // 移动平均参数
    private val movingAverageWindow by int("MovingAverageWindow", 5, 2..20) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageWeightType by choices(
        "MovingAverageWeightType",
        arrayOf("Equal", "LinearDecay", "ExponentialDecay", "CentralPeak", "CustomWeights"),
        "Equal"
    ) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageCustomWeightsText by text("MovingAverageCustomWeights", "1.0,0.8,0.6,0.4,0.2") { smoothingAlgorithm == "MovingAverage" && movingAverageWeightType == "CustomWeights" && options.rotationsActive }
    private val movingAverageWeightDecayFactor by float("MovingAverageWeightDecayFactor", 0.5f, 0.1f..0.9f) { smoothingAlgorithm == "MovingAverage" && movingAverageWeightType == "ExponentialDecay" && options.rotationsActive }
    private val movingAveragePeakPosition by float("MovingAveragePeakPosition", 0.5f, 0f..1f) { smoothingAlgorithm == "MovingAverage" && movingAverageWeightType == "CentralPeak" && options.rotationsActive }
    private val movingAveragePeakWidth by float("MovingAveragePeakWidth", 0.3f, 0.1f..1f) { smoothingAlgorithm == "MovingAverage" && movingAverageWeightType == "CentralPeak" && options.rotationsActive }
    private val movingAverageLinearSlope by float("MovingAverageLinearSlope", -0.2f, -1f..0f) { smoothingAlgorithm == "MovingAverage" && movingAverageWeightType == "LinearDecay" && options.rotationsActive }
    private val movingAverageApplyToYaw by boolean("MovingAverageApplyToYaw", true) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageApplyToPitch by boolean("MovingAverageApplyToPitch", true) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageAdaptiveWindow by boolean("MovingAverageAdaptiveWindow", false) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageMaxWindow by int("MovingAverageMaxWindow", 10, 2..30) { smoothingAlgorithm == "MovingAverage" && movingAverageAdaptiveWindow && options.rotationsActive }
    private val movingAverageVelocityThreshold by float("MovingAverageVelocityThreshold", 5f, 0f..30f) { smoothingAlgorithm == "MovingAverage" && movingAverageAdaptiveWindow && options.rotationsActive }
    private val movingAverageResetOnTargetChange by boolean("MovingAverageResetOnTargetChange", true) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }
    private val movingAverageSmoothFactor by float("MovingAverageSmoothFactor", 1f, 0.1f..2f) { smoothingAlgorithm == "MovingAverage" && options.rotationsActive }

    // 指数平滑参数
    private val exponentialSmoothingAlpha by float("ExponentialAlpha", 0.3f, 0.01f..1f) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingBeta by float("ExponentialBeta", 0.2f, 0f..1f) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingGamma by float("ExponentialGamma", 0.1f, 0f..1f) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingUseTrend by boolean("ExponentialUseTrend", true) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingUseSeasonality by boolean("ExponentialUseSeasonality", false) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingSeasonalPeriod by int("ExponentialSeasonalPeriod", 4, 2..20) { smoothingAlgorithm == "ExponentialSmoothing" && exponentialSmoothingUseSeasonality && options.rotationsActive }
    private val exponentialSmoothingDampingFactor by float("ExponentialDampingFactor", 0.98f, 0.5f..1f) { smoothingAlgorithm == "ExponentialSmoothing" && exponentialSmoothingUseTrend && options.rotationsActive }
    private val exponentialSmoothingAdaptiveAlpha by boolean("ExponentialAdaptiveAlpha", false) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingAlphaMin by float("ExponentialAlphaMin", 0.1f, 0f..0.5f) { smoothingAlgorithm == "ExponentialSmoothing" && exponentialSmoothingAdaptiveAlpha && options.rotationsActive }
    private val exponentialSmoothingAlphaMax by float("ExponentialAlphaMax", 0.9f, 0.5f..1f) { smoothingAlgorithm == "ExponentialSmoothing" && exponentialSmoothingAdaptiveAlpha && options.rotationsActive }
    private val exponentialSmoothingErrorThreshold by float("ExponentialErrorThreshold", 10f, 0f..45f) { smoothingAlgorithm == "ExponentialSmoothing" && exponentialSmoothingAdaptiveAlpha && options.rotationsActive }
    private val exponentialSmoothingResetOnTargetChange by boolean("ExponentialResetOnTargetChange", true) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingApplyToYaw by boolean("ExponentialApplyToYaw", true) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }
    private val exponentialSmoothingApplyToPitch by boolean("ExponentialApplyToPitch", true) { smoothingAlgorithm == "ExponentialSmoothing" && options.rotationsActive }

    // 高斯平滑参数
    private val gaussianSmoothingSigma by float("GaussianSigma", 1.5f, 0.1f..5f) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingWindowSize by int("GaussianWindowSize", 7, 3..21) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingKernelType by choices(
        "GaussianKernelType",
        arrayOf("Standard", "Truncated", "Modified", "Adaptive", "Bilateral"),
        "Standard"
    ) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingTruncationFactor by float("GaussianTruncationFactor", 2.5f, 1f..5f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingKernelType == "Truncated" && options.rotationsActive }
    private val gaussianSmoothingModificationFactor by float("GaussianModificationFactor", 0.5f, 0f..2f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingKernelType == "Modified" && options.rotationsActive }
    private val gaussianSmoothingAdaptiveSigma by boolean("GaussianAdaptiveSigma", false) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingSigmaMin by float("GaussianSigmaMin", 0.5f, 0.1f..3f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingAdaptiveSigma && options.rotationsActive }
    private val gaussianSmoothingSigmaMax by float("GaussianSigmaMax", 3f, 1f..10f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingAdaptiveSigma && options.rotationsActive }
    private val gaussianSmoothingVelocityFactor by float("GaussianVelocityFactor", 0.1f, 0f..1f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingAdaptiveSigma && options.rotationsActive }
    private val gaussianSmoothingBilateralRangeSigma by float("GaussianRangeSigma", 10f, 1f..30f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingKernelType == "Bilateral" && options.rotationsActive }
    private val gaussianSmoothingBilateralDomainSigma by float("GaussianDomainSigma", 15f, 1f..45f) { smoothingAlgorithm == "GaussianSmoothing" && gaussianSmoothingKernelType == "Bilateral" && options.rotationsActive }
    private val gaussianSmoothingApplyToYaw by boolean("GaussianApplyToYaw", true) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingApplyToPitch by boolean("GaussianApplyToPitch", true) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }
    private val gaussianSmoothingResetOnTargetChange by boolean("GaussianResetOnTargetChange", true) { smoothingAlgorithm == "GaussianSmoothing" && options.rotationsActive }

    // N-gram平滑参数
    private val nGramSmoothingN by int("NGramN", 3, 2..10) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingLambda by float("NGramLambda", 0.5f, 0f..1f) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingBackoffFactor by float("NGramBackoffFactor", 0.3f, 0f..1f) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingUseInterpolation by boolean("NGramUseInterpolation", true) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingInterpolationWeightsText by text("NGramInterpolationWeights", "0.4,0.3,0.2,0.1") { smoothingAlgorithm == "NGramSmoothing" && nGramSmoothingUseInterpolation && options.rotationsActive }
    private val nGramSmoothingSmoothingMethod by choices(
        "NGramSmoothingMethod",
        arrayOf("Additive", "GoodTuring", "KneserNey", "WittenBell"),
        "Additive"
    ) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingDelta by float("NGramDelta", 0.75f, 0f..1f) { smoothingAlgorithm == "NGramSmoothing" && nGramSmoothingSmoothingMethod == "KneserNey" && options.rotationsActive }
    private val nGramSmoothingAdditiveDelta by float("NGramAdditiveDelta", 1f, 0f..10f) { smoothingAlgorithm == "NGramSmoothing" && nGramSmoothingSmoothingMethod == "Additive" && options.rotationsActive }
    private val nGramSmoothingApplyToYaw by boolean("NGramApplyToYaw", true) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingApplyToPitch by boolean("NGramApplyToPitch", true) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingResetOnTargetChange by boolean("NGramResetOnTargetChange", true) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }
    private val nGramSmoothingHistorySize by int("NGramHistorySize", 100, 10..1000) { smoothingAlgorithm == "NGramSmoothing" && options.rotationsActive }

    // 固定点平滑算法A参数
    private val fixedPointAIterations by int("FixedPointAIterations", 5, 1..20) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAContractionFactor by float("FixedPointAContractionFactor", 0.8f, 0.1f..1f) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointATolerance by float("FixedPointATolerance", 0.01f, 0.001f..0.1f) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAUseAcceleration by boolean("FixedPointAUseAcceleration", true) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAAccelerationFactor by float("FixedPointAAccelerationFactor", 1.5f, 1f..3f) { smoothingAlgorithm == "FixedPointA" && fixedPointAUseAcceleration && options.rotationsActive }
    private val fixedPointAMaxAccelerationSteps by int("FixedPointAMaxAccelerationSteps", 3, 1..10) { smoothingAlgorithm == "FixedPointA" && fixedPointAUseAcceleration && options.rotationsActive }
    private val fixedPointAApplyToYaw by boolean("FixedPointAApplyToYaw", true) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAApplyToPitch by boolean("FixedPointAApplyToPitch", true) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAResetOnTargetChange by boolean("FixedPointAResetOnTargetChange", true) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointASmoothnessWeight by float("FixedPointASmoothnessWeight", 0.7f, 0f..1f) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }
    private val fixedPointAFidelityWeight by float("FixedPointAFidelityWeight", 0.3f, 0f..1f) { smoothingAlgorithm == "FixedPointA" && options.rotationsActive }

    // 固定点平滑算法B参数
    private val fixedPointBIterations by int("FixedPointBIterations", 7, 1..20) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBRelaxationFactor by float("FixedPointBRelaxationFactor", 0.6f, 0.1f..1f) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBOverRelaxationFactor by float("FixedPointBOverRelaxationFactor", 1.2f, 1f..2f) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBUseOverRelaxation by boolean("FixedPointBUseOverRelaxation", false) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBTolerance by float("FixedPointBTolerance", 0.005f, 0.001f..0.1f) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBApplyToYaw by boolean("FixedPointBApplyToYaw", true) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBApplyToPitch by boolean("FixedPointBApplyToPitch", true) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBResetOnTargetChange by boolean("FixedPointBResetOnTargetChange", true) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBSmoothnessFactor by float("FixedPointBSmoothnessFactor", 0.5f, 0f..1f) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBConvergenceCheck by boolean("FixedPointBConvergenceCheck", true) { smoothingAlgorithm == "FixedPointB" && options.rotationsActive }
    private val fixedPointBMaxDivergenceSteps by int("FixedPointBMaxDivergenceSteps", 5, 1..20) { smoothingAlgorithm == "FixedPointB" && fixedPointBConvergenceCheck && options.rotationsActive }

    // 最优固定点平滑A参数
    private val optimalFixedPointAIterations by int("OptimalFixedPointAIterations", 10, 1..30) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointALearningRate by float("OptimalFixedPointALearningRate", 0.1f, 0.01f..1f) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointAMomentumFactor by float("OptimalFixedPointAMomentumFactor", 0.9f, 0f..1f) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointAAdaptiveLearningRate by boolean("OptimalFixedPointAAdaptiveLearningRate", true) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointALearningRateDecay by float("OptimalFixedPointALearningRateDecay", 0.99f, 0.9f..1f) { smoothingAlgorithm == "OptimalFixedPointA" && optimalFixedPointAAdaptiveLearningRate && options.rotationsActive }
    private val optimalFixedPointALearningRateMin by float("OptimalFixedPointALearningRateMin", 0.001f, 0f..0.1f) { smoothingAlgorithm == "OptimalFixedPointA" && optimalFixedPointAAdaptiveLearningRate && options.rotationsActive }
    private val optimalFixedPointAApplyToYaw by boolean("OptimalFixedPointAApplyToYaw", true) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointAApplyToPitch by boolean("OptimalFixedPointAApplyToPitch", true) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointAResetOnTargetChange by boolean("OptimalFixedPointAResetOnTargetChange", true) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointALossFunction by choices(
        "OptimalFixedPointALossFunction",
        arrayOf("MSE", "MAE", "Huber", "Custom"),
        "MSE"
    ) { smoothingAlgorithm == "OptimalFixedPointA" && options.rotationsActive }
    private val optimalFixedPointAHuberDelta by float("OptimalFixedPointAHuberDelta", 1f, 0.1f..5f) { smoothingAlgorithm == "OptimalFixedPointA" && optimalFixedPointALossFunction == "Huber" && options.rotationsActive }
    private val optimalFixedPointACustomLossWeight by float("OptimalFixedPointACustomLossWeight", 0.5f, 0f..1f) { smoothingAlgorithm == "OptimalFixedPointA" && optimalFixedPointALossFunction == "Custom" && options.rotationsActive }

    // 随机化功能
    private val randomizationAlgorithm by choices(
        "RandomizationAlgorithm",
        arrayOf(
            "MersenneTwister",
            "Ranlux24",
            "MinimalStandard",
            "DiscardBlock",
            "IndependentBits",
            "ShuffleOrder",
            "LinearCongruential",
            "SubtractWithCarry",
            "LiquidBounce"
        ),
        "LiquidBounce"
    ) { options.rotationsActive }

    // 梅森旋转算法参数
    private val mersenneTwisterSeed by int("MersenneTwisterSeed", 5489, 0..100000) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterUseTimeSeed by boolean("MersenneTwisterUseTimeSeed", true) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingU by int("MersenneTwisterTemperingU", 11, 1..32) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingD by int("MersenneTwisterTemperingD", 0x7FFFFFFF, 0..0x7FFFFFFF) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingS by int("MersenneTwisterTemperingS", 7, 1..32) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingB by int("MersenneTwisterTemperingB", 0x7D2C5680, 0..0x7FFFFFFF) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingT by int("MersenneTwisterTemperingT", 15, 1..32) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingC by int("MersenneTwisterTemperingC", 0x6FC60000, 0..0x7FFFFFFF) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterTemperingL by int("MersenneTwisterTemperingL", 18, 1..32) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterApplyToYaw by boolean("MersenneTwisterApplyToYaw", true) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterApplyToPitch by boolean("MersenneTwisterApplyToPitch", true) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterYawRange by float("MersenneTwisterYawRange", 3f, 0f..10f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterApplyToYaw && options.rotationsActive }
    private val mersenneTwisterPitchRange by float("MersenneTwisterPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterApplyToPitch && options.rotationsActive }
    private val mersenneTwisterDistributionType by choices(
        "MersenneTwisterDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "MersenneTwister" && options.rotationsActive }
    private val mersenneTwisterNormalMean by float("MersenneTwisterNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterDistributionType == "Normal" && options.rotationsActive }
    private val mersenneTwisterNormalStdDev by float("MersenneTwisterNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterDistributionType == "Normal" && options.rotationsActive }
    private val mersenneTwisterTriangularMode by float("MersenneTwisterTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterDistributionType == "Triangular" && options.rotationsActive }
    private val mersenneTwisterExponentialLambda by float("MersenneTwisterExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "MersenneTwister" && mersenneTwisterDistributionType == "Exponential" && options.rotationsActive }

    // Ranlux24基础算法参数
    private val ranlux24Seed by int("Ranlux24Seed", 19780503, 0..100000000) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24UseTimeSeed by boolean("Ranlux24UseTimeSeed", true) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24LuxuryLevel by int("Ranlux24LuxuryLevel", 24, 1..48) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24CarryBitCount by int("Ranlux24CarryBitCount", 24, 1..48) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24ApplyToYaw by boolean("Ranlux24ApplyToYaw", true) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24ApplyToPitch by boolean("Ranlux24ApplyToPitch", true) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24YawRange by float("Ranlux24YawRange", 3f, 0f..10f) { randomizationAlgorithm == "Ranlux24" && ranlux24ApplyToYaw && options.rotationsActive }
    private val ranlux24PitchRange by float("Ranlux24PitchRange", 2f, 0f..10f) { randomizationAlgorithm == "Ranlux24" && ranlux24ApplyToPitch && options.rotationsActive }
    private val ranlux24DistributionType by choices(
        "Ranlux24DistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "Ranlux24" && options.rotationsActive }
    private val ranlux24NormalMean by float("Ranlux24NormalMean", 0f, -10f..10f) { randomizationAlgorithm == "Ranlux24" && ranlux24DistributionType == "Normal" && options.rotationsActive }
    private val ranlux24NormalStdDev by float("Ranlux24NormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "Ranlux24" && ranlux24DistributionType == "Normal" && options.rotationsActive }
    private val ranlux24TriangularMode by float("Ranlux24TriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "Ranlux24" && ranlux24DistributionType == "Triangular" && options.rotationsActive }
    private val ranlux24ExponentialLambda by float("Ranlux24ExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "Ranlux24" && ranlux24DistributionType == "Exponential" && options.rotationsActive }

    // 最小标准随机算法参数
    private val minimalStandardSeed by int("MinimalStandardSeed", 1, 1..2147483646) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardMultiplier by int("MinimalStandardMultiplier", 16807, 1..2147483646) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardModulus by int("MinimalStandardModulus", 2147483647, 1000000..2147483647) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardApplyToYaw by boolean("MinimalStandardApplyToYaw", true) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardApplyToPitch by boolean("MinimalStandardApplyToPitch", true) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardYawRange by float("MinimalStandardYawRange", 3f, 0f..10f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardApplyToYaw && options.rotationsActive }
    private val minimalStandardPitchRange by float("MinimalStandardPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardApplyToPitch && options.rotationsActive }
    private val minimalStandardDistributionType by choices(
        "MinimalStandardDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "MinimalStandard" && options.rotationsActive }
    private val minimalStandardNormalMean by float("MinimalStandardNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardDistributionType == "Normal" && options.rotationsActive }
    private val minimalStandardNormalStdDev by float("MinimalStandardNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardDistributionType == "Normal" && options.rotationsActive }
    private val minimalStandardTriangularMode by float("MinimalStandardTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardDistributionType == "Triangular" && options.rotationsActive }
    private val minimalStandardExponentialLambda by float("MinimalStandardExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "MinimalStandard" && minimalStandardDistributionType == "Exponential" && options.rotationsActive }

    // 丢弃块算法参数
    private val discardBlockSeed by int("DiscardBlockSeed", 123456789, 0..1000000000) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockBlockSize by int("DiscardBlockBlockSize", 10, 2..100) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockDiscardSize by int("DiscardBlockDiscardSize", 5, 1..50) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockUseTimeSeed by boolean("DiscardBlockUseTimeSeed", true) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockApplyToYaw by boolean("DiscardBlockApplyToYaw", true) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockApplyToPitch by boolean("DiscardBlockApplyToPitch", true) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockYawRange by float("DiscardBlockYawRange", 3f, 0f..10f) { randomizationAlgorithm == "DiscardBlock" && discardBlockApplyToYaw && options.rotationsActive }
    private val discardBlockPitchRange by float("DiscardBlockPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "DiscardBlock" && discardBlockApplyToPitch && options.rotationsActive }
    private val discardBlockDistributionType by choices(
        "DiscardBlockDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "DiscardBlock" && options.rotationsActive }
    private val discardBlockNormalMean by float("DiscardBlockNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "DiscardBlock" && discardBlockDistributionType == "Normal" && options.rotationsActive }
    private val discardBlockNormalStdDev by float("DiscardBlockNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "DiscardBlock" && discardBlockDistributionType == "Normal" && options.rotationsActive }
    private val discardBlockTriangularMode by float("DiscardBlockTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "DiscardBlock" && discardBlockDistributionType == "Triangular" && options.rotationsActive }
    private val discardBlockExponentialLambda by float("DiscardBlockExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "DiscardBlock" && discardBlockDistributionType == "Exponential" && options.rotationsActive }

    // 独立位算法参数
    private val independentBitsSeed by int("IndependentBitsSeed", 987654321, 0..1000000000) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsBitCount by int("IndependentBitsBitCount", 32, 1..64) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsUseXorShift by boolean("IndependentBitsUseXorShift", true) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsXorShiftA by int("IndependentBitsXorShiftA", 13, 1..32) { randomizationAlgorithm == "IndependentBits" && independentBitsUseXorShift && options.rotationsActive }
    private val independentBitsXorShiftB by int("IndependentBitsXorShiftB", 17, 1..32) { randomizationAlgorithm == "IndependentBits" && independentBitsUseXorShift && options.rotationsActive }
    private val independentBitsXorShiftC by int("IndependentBitsXorShiftC", 5, 1..32) { randomizationAlgorithm == "IndependentBits" && independentBitsUseXorShift && options.rotationsActive }
    private val independentBitsApplyToYaw by boolean("IndependentBitsApplyToYaw", true) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsApplyToPitch by boolean("IndependentBitsApplyToPitch", true) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsYawRange by float("IndependentBitsYawRange", 3f, 0f..10f) { randomizationAlgorithm == "IndependentBits" && independentBitsApplyToYaw && options.rotationsActive }
    private val independentBitsPitchRange by float("IndependentBitsPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "IndependentBits" && independentBitsApplyToPitch && options.rotationsActive }
    private val independentBitsDistributionType by choices(
        "IndependentBitsDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "IndependentBits" && options.rotationsActive }
    private val independentBitsNormalMean by float("IndependentBitsNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "IndependentBits" && independentBitsDistributionType == "Normal" && options.rotationsActive }
    private val independentBitsNormalStdDev by float("IndependentBitsNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "IndependentBits" && independentBitsDistributionType == "Normal" && options.rotationsActive }
    private val independentBitsTriangularMode by float("IndependentBitsTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "IndependentBits" && independentBitsDistributionType == "Triangular" && options.rotationsActive }
    private val independentBitsExponentialLambda by float("IndependentBitsExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "IndependentBits" && independentBitsDistributionType == "Exponential" && options.rotationsActive }

    // 洗牌顺序算法参数
    private val shuffleOrderSeed by int("ShuffleOrderSeed", 135792468, 0..1000000000) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderTableSize by int("ShuffleOrderTableSize", 256, 16..1024) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderShuffleInterval by int("ShuffleOrderShuffleInterval", 1, 1..100) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderUseTimeSeed by boolean("ShuffleOrderUseTimeSeed", true) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderApplyToYaw by boolean("ShuffleOrderApplyToYaw", true) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderApplyToPitch by boolean("ShuffleOrderApplyToPitch", true) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderYawRange by float("ShuffleOrderYawRange", 3f, 0f..10f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderApplyToYaw && options.rotationsActive }
    private val shuffleOrderPitchRange by float("ShuffleOrderPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderApplyToPitch && options.rotationsActive }
    private val shuffleOrderDistributionType by choices(
        "ShuffleOrderDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "ShuffleOrder" && options.rotationsActive }
    private val shuffleOrderNormalMean by float("ShuffleOrderNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderDistributionType == "Normal" && options.rotationsActive }
    private val shuffleOrderNormalStdDev by float("ShuffleOrderNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderDistributionType == "Normal" && options.rotationsActive }
    private val shuffleOrderTriangularMode by float("ShuffleOrderTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderDistributionType == "Triangular" && options.rotationsActive }
    private val shuffleOrderExponentialLambda by float("ShuffleOrderExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "ShuffleOrder" && shuffleOrderDistributionType == "Exponential" && options.rotationsActive }

    // 线性同余算法参数
    private val linearCongruentialSeed by int("LinearCongruentialSeed", 12345, 0..100000) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialMultiplier by int("LinearCongruentialMultiplier", 1103515245, 0..2147483647) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialIncrement by int("LinearCongruentialIncrement", 12345, 0..2147483647) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialModulus by int("LinearCongruentialModulus", 2147483647, 1000000..2147483647) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialApplyToYaw by boolean("LinearCongruentialApplyToYaw", true) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialApplyToPitch by boolean("LinearCongruentialApplyToPitch", true) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialYawRange by float("LinearCongruentialYawRange", 3f, 0f..10f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialApplyToYaw && options.rotationsActive }
    private val linearCongruentialPitchRange by float("LinearCongruentialPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialApplyToPitch && options.rotationsActive }
    private val linearCongruentialDistributionType by choices(
        "LinearCongruentialDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "LinearCongruential" && options.rotationsActive }
    private val linearCongruentialNormalMean by float("LinearCongruentialNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialDistributionType == "Normal" && options.rotationsActive }
    private val linearCongruentialNormalStdDev by float("LinearCongruentialNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialDistributionType == "Normal" && options.rotationsActive }
    private val linearCongruentialTriangularMode by float("LinearCongruentialTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialDistributionType == "Triangular" && options.rotationsActive }
    private val linearCongruentialExponentialLambda by float("LinearCongruentialExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "LinearCongruential" && linearCongruentialDistributionType == "Exponential" && options.rotationsActive }

    // 带进位减法算法参数
    private val subtractWithCarrySeed by int("SubtractWithCarrySeed", 123456789, 0..1000000000) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryLagR by int("SubtractWithCarryLagR", 24, 1..48) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryLagS by int("SubtractWithCarryLagS", 10, 1..24) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryModulus by int("SubtractWithCarryModulus", 2147483647, 1000000..2147483647) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryApplyToYaw by boolean("SubtractWithCarryApplyToYaw", true) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryApplyToPitch by boolean("SubtractWithCarryApplyToPitch", true) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryYawRange by float("SubtractWithCarryYawRange", 3f, 0f..10f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryApplyToYaw && options.rotationsActive }
    private val subtractWithCarryPitchRange by float("SubtractWithCarryPitchRange", 2f, 0f..10f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryApplyToPitch && options.rotationsActive }
    private val subtractWithCarryDistributionType by choices(
        "SubtractWithCarryDistributionType",
        arrayOf("Uniform", "Normal", "Triangular", "Exponential"),
        "Uniform"
    ) { randomizationAlgorithm == "SubtractWithCarry" && options.rotationsActive }
    private val subtractWithCarryNormalMean by float("SubtractWithCarryNormalMean", 0f, -10f..10f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryDistributionType == "Normal" && options.rotationsActive }
    private val subtractWithCarryNormalStdDev by float("SubtractWithCarryNormalStdDev", 1f, 0.1f..5f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryDistributionType == "Normal" && options.rotationsActive }
    private val subtractWithCarryTriangularMode by float("SubtractWithCarryTriangularMode", 0f, -1f..1f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryDistributionType == "Triangular" && options.rotationsActive }
    private val subtractWithCarryExponentialLambda by float("SubtractWithCarryExponentialLambda", 1f, 0.1f..10f) { randomizationAlgorithm == "SubtractWithCarry" && subtractWithCarryDistributionType == "Exponential" && options.rotationsActive }

    // 随机生成分布
    private val randomizationDistribution by choices(
        "RandomizationDistribution",
        arrayOf("Binomial", "UniformInteger"),
        "UniformInteger"
    ) { options.rotationsActive && randomizationAlgorithm != "LiquidBounce" }

    // 二项分布参数
    private val binomialTrials by int("BinomialTrials", 10, 1..100) { randomizationDistribution == "Binomial" && options.rotationsActive }
    private val binomialSuccessProbability by float("BinomialSuccessProbability", 0.5f, 0f..1f) { randomizationDistribution == "Binomial" && options.rotationsActive }
    private val binomialApplyToYaw by boolean("BinomialApplyToYaw", true) { randomizationDistribution == "Binomial" && options.rotationsActive }
    private val binomialApplyToPitch by boolean("BinomialApplyToPitch", true) { randomizationDistribution == "Binomial" && options.rotationsActive }
    private val binomialYawScale by float("BinomialYawScale", 1f, 0.1f..10f) { randomizationDistribution == "Binomial" && binomialApplyToYaw && options.rotationsActive }
    private val binomialPitchScale by float("BinomialPitchScale", 1f, 0.1f..10f) { randomizationDistribution == "Binomial" && binomialApplyToPitch && options.rotationsActive }
    private val binomialOffsetYaw by float("BinomialOffsetYaw", 0f, -10f..10f) { randomizationDistribution == "Binomial" && binomialApplyToYaw && options.rotationsActive }
    private val binomialOffsetPitch by float("BinomialOffsetPitch", 0f, -10f..10f) { randomizationDistribution == "Binomial" && binomialApplyToPitch && options.rotationsActive }

    // 均匀整数分布参数
    private val uniformIntegerMin by int("UniformIntegerMin", -5, -100..100) { randomizationDistribution == "UniformInteger" && options.rotationsActive }
    private val uniformIntegerMax by int("UniformIntegerMax", 5, -100..100) { randomizationDistribution == "UniformInteger" && options.rotationsActive }
    private val uniformIntegerApplyToYaw by boolean("UniformIntegerApplyToYaw", true) { randomizationDistribution == "UniformInteger" && options.rotationsActive }
    private val uniformIntegerApplyToPitch by boolean("UniformIntegerApplyToPitch", true) { randomizationDistribution == "UniformInteger" && options.rotationsActive }
    private val uniformIntegerYawScale by float("UniformIntegerYawScale", 0.1f, 0.01f..1f) { randomizationDistribution == "UniformInteger" && uniformIntegerApplyToYaw && options.rotationsActive }
    private val uniformIntegerPitchScale by float("UniformIntegerPitchScale", 0.1f, 0.01f..1f) { randomizationDistribution == "UniformInteger" && uniformIntegerApplyToPitch && options.rotationsActive }
    private val uniformIntegerOffsetYaw by float("UniformIntegerOffsetYaw", 0f, -10f..10f) { randomizationDistribution == "UniformInteger" && uniformIntegerApplyToYaw && options.rotationsActive }
    private val uniformIntegerOffsetPitch by float("UniformIntegerOffsetPitch", 0f, -10f..10f) { randomizationDistribution == "UniformInteger" && uniformIntegerApplyToPitch && options.rotationsActive }

    // 噪声系统 - NL1
    private val nl1Enabled by boolean("NL1-Enabled", false) { options.rotationsActive }
    private val nl1Frequency by float("NL1-Frequency", 1.0f, 0.01f..10f) { nl1Enabled && options.rotationsActive }
    private val nl1Amplitude by float("NL1-Amplitude", 0.5f, 0f..5f) { nl1Enabled && options.rotationsActive }
    private val nl1Octaves by int("NL1-Octaves", 3, 1..8) { nl1Enabled && options.rotationsActive }
    private val nl1Persistence by float("NL1-Persistence", 0.5f, 0.1f..1f) { nl1Enabled && options.rotationsActive }
    private val nl1Lacunarity by float("NL1-Lacunarity", 2.0f, 1f..4f) { nl1Enabled && options.rotationsActive }
    private val nl1ApplyToYaw by boolean("NL1-ApplyToYaw", true) { nl1Enabled && options.rotationsActive }
    private val nl1ApplyToPitch by boolean("NL1-ApplyToPitch", true) { nl1Enabled && options.rotationsActive }
    private val nl1PhaseOffset by float("NL1-PhaseOffset", 0f, 0f..6.283f) { nl1Enabled && options.rotationsActive }
    private val nl1Seed by int("NL1-Seed", 1111, 0..1000000) { nl1Enabled && options.rotationsActive }
    private val nl1NoiseType by choices("NL1-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Perlin") { nl1Enabled && options.rotationsActive }
    private val nl1Interpolation by choices("NL1-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cosine") { nl1Enabled && options.rotationsActive }
    private val nl1AmplitudeModulation by float("NL1-AmplitudeModulation", 0f, 0f..1f) { nl1Enabled && options.rotationsActive }
    private val nl1FrequencyModulation by float("NL1-FrequencyModulation", 0f, 0f..1f) { nl1Enabled && options.rotationsActive }
    private val nl1TimeFactor by float("NL1-TimeFactor", 1f, 0f..10f) { nl1Enabled && options.rotationsActive }
    private val nl1Smoothness by float("NL1-Smoothness", 0.5f, 0f..1f) { nl1Enabled && options.rotationsActive }
    private val nl1ClippingMin by float("NL1-ClippingMin", -1f, -5f..5f) { nl1Enabled && options.rotationsActive }
    private val nl1ClippingMax by float("NL1-ClippingMax", 1f, -5f..5f) { nl1Enabled && options.rotationsActive }
    private val nl1Normalization by boolean("NL1-Normalization", true) { nl1Enabled && options.rotationsActive }
    private val nl1Weight by float("NL1-Weight", 1f, 0f..2f) { nl1Enabled && options.rotationsActive }

    // 噪声系统 - NL2
    private val nl2Enabled by boolean("NL2-Enabled", false) { options.rotationsActive }
    private val nl2Frequency by float("NL2-Frequency", 2.0f, 0.01f..10f) { nl2Enabled && options.rotationsActive }
    private val nl2Amplitude by float("NL2-Amplitude", 0.3f, 0f..5f) { nl2Enabled && options.rotationsActive }
    private val nl2Octaves by int("NL2-Octaves", 2, 1..8) { nl2Enabled && options.rotationsActive }
    private val nl2Persistence by float("NL2-Persistence", 0.6f, 0.1f..1f) { nl2Enabled && options.rotationsActive }
    private val nl2Lacunarity by float("NL2-Lacunarity", 2.2f, 1f..4f) { nl2Enabled && options.rotationsActive }
    private val nl2ApplyToYaw by boolean("NL2-ApplyToYaw", true) { nl2Enabled && options.rotationsActive }
    private val nl2ApplyToPitch by boolean("NL2-ApplyToPitch", true) { nl2Enabled && options.rotationsActive }
    private val nl2PhaseOffset by float("NL2-PhaseOffset", 1.5f, 0f..6.283f) { nl2Enabled && options.rotationsActive }
    private val nl2Seed by int("NL2-Seed", 2222, 0..1000000) { nl2Enabled && options.rotationsActive }
    private val nl2NoiseType by choices("NL2-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Simplex") { nl2Enabled && options.rotationsActive }
    private val nl2Interpolation by choices("NL2-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cubic") { nl2Enabled && options.rotationsActive }
    private val nl2AmplitudeModulation by float("NL2-AmplitudeModulation", 0.2f, 0f..1f) { nl2Enabled && options.rotationsActive }
    private val nl2FrequencyModulation by float("NL2-FrequencyModulation", 0.1f, 0f..1f) { nl2Enabled && options.rotationsActive }
    private val nl2TimeFactor by float("NL2-TimeFactor", 1.5f, 0f..10f) { nl2Enabled && options.rotationsActive }
    private val nl2Smoothness by float("NL2-Smoothness", 0.6f, 0f..1f) { nl2Enabled && options.rotationsActive }
    private val nl2ClippingMin by float("NL2-ClippingMin", -0.8f, -5f..5f) { nl2Enabled && options.rotationsActive }
    private val nl2ClippingMax by float("NL2-ClippingMax", 0.8f, -5f..5f) { nl2Enabled && options.rotationsActive }
    private val nl2Normalization by boolean("NL2-Normalization", true) { nl2Enabled && options.rotationsActive }
    private val nl2Weight by float("NL2-Weight", 0.8f, 0f..2f) { nl2Enabled && options.rotationsActive }

    // 噪声系统 - NL3
    private val nl3Enabled by boolean("NL3-Enabled", false) { options.rotationsActive }
    private val nl3Frequency by float("NL3-Frequency", 3.0f, 0.01f..10f) { nl3Enabled && options.rotationsActive }
    private val nl3Amplitude by float("NL3-Amplitude", 0.2f, 0f..5f) { nl3Enabled && options.rotationsActive }
    private val nl3Octaves by int("NL3-Octaves", 4, 1..8) { nl3Enabled && options.rotationsActive }
    private val nl3Persistence by float("NL3-Persistence", 0.4f, 0.1f..1f) { nl3Enabled && options.rotationsActive }
    private val nl3Lacunarity by float("NL3-Lacunarity", 1.8f, 1f..4f) { nl3Enabled && options.rotationsActive }
    private val nl3ApplyToYaw by boolean("NL3-ApplyToYaw", true) { nl3Enabled && options.rotationsActive }
    private val nl3ApplyToPitch by boolean("NL3-ApplyToPitch", true) { nl3Enabled && options.rotationsActive }
    private val nl3PhaseOffset by float("NL3-PhaseOffset", 3.0f, 0f..6.283f) { nl3Enabled && options.rotationsActive }
    private val nl3Seed by int("NL3-Seed", 3333, 0..1000000) { nl3Enabled && options.rotationsActive }
    private val nl3NoiseType by choices("NL3-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Value") { nl3Enabled && options.rotationsActive }
    private val nl3Interpolation by choices("NL3-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Hermite") { nl3Enabled && options.rotationsActive }
    private val nl3AmplitudeModulation by float("NL3-AmplitudeModulation", 0.3f, 0f..1f) { nl3Enabled && options.rotationsActive }
    private val nl3FrequencyModulation by float("NL3-FrequencyModulation", 0.2f, 0f..1f) { nl3Enabled && options.rotationsActive }
    private val nl3TimeFactor by float("NL3-TimeFactor", 2.0f, 0f..10f) { nl3Enabled && options.rotationsActive }
    private val nl3Smoothness by float("NL3-Smoothness", 0.7f, 0f..1f) { nl3Enabled && options.rotationsActive }
    private val nl3ClippingMin by float("NL3-ClippingMin", -0.5f, -5f..5f) { nl3Enabled && options.rotationsActive }
    private val nl3ClippingMax by float("NL3-ClippingMax", 0.5f, -5f..5f) { nl3Enabled && options.rotationsActive }
    private val nl3Normalization by boolean("NL3-Normalization", true) { nl3Enabled && options.rotationsActive }
    private val nl3Weight by float("NL3-Weight", 0.6f, 0f..2f) { nl3Enabled && options.rotationsActive }

    // 噪声系统 - NL4
    private val nl4Enabled by boolean("NL4-Enabled", false) { options.rotationsActive }
    private val nl4Frequency by float("NL4-Frequency", 0.5f, 0.01f..10f) { nl4Enabled && options.rotationsActive }
    private val nl4Amplitude by float("NL4-Amplitude", 0.4f, 0f..5f) { nl4Enabled && options.rotationsActive }
    private val nl4Octaves by int("NL4-Octaves", 5, 1..8) { nl4Enabled && options.rotationsActive }
    private val nl4Persistence by float("NL4-Persistence", 0.7f, 0.1f..1f) { nl4Enabled && options.rotationsActive }
    private val nl4Lacunarity by float("NL4-Lacunarity", 2.5f, 1f..4f) { nl4Enabled && options.rotationsActive }
    private val nl4ApplyToYaw by boolean("NL4-ApplyToYaw", true) { nl4Enabled && options.rotationsActive }
    private val nl4ApplyToPitch by boolean("NL4-ApplyToPitch", true) { nl4Enabled && options.rotationsActive }
    private val nl4PhaseOffset by float("NL4-PhaseOffset", 2.5f, 0f..6.283f) { nl4Enabled && options.rotationsActive }
    private val nl4Seed by int("NL4-Seed", 4444, 0..1000000) { nl4Enabled && options.rotationsActive }
    private val nl4NoiseType by choices("NL4-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Perlin") { nl4Enabled && options.rotationsActive }
    private val nl4Interpolation by choices("NL4-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Linear") { nl4Enabled && options.rotationsActive }
    private val nl4AmplitudeModulation by float("NL4-AmplitudeModulation", 0.4f, 0f..1f) { nl4Enabled && options.rotationsActive }
    private val nl4FrequencyModulation by float("NL4-FrequencyModulation", 0.3f, 0f..1f) { nl4Enabled && options.rotationsActive }
    private val nl4TimeFactor by float("NL4-TimeFactor", 0.8f, 0f..10f) { nl4Enabled && options.rotationsActive }
    private val nl4Smoothness by float("NL4-Smoothness", 0.4f, 0f..1f) { nl4Enabled && options.rotationsActive }
    private val nl4ClippingMin by float("NL4-ClippingMin", -1.2f, -5f..5f) { nl4Enabled && options.rotationsActive }
    private val nl4ClippingMax by float("NL4-ClippingMax", 1.2f, -5f..5f) { nl4Enabled && options.rotationsActive }
    private val nl4Normalization by boolean("NL4-Normalization", true) { nl4Enabled && options.rotationsActive }
    private val nl4Weight by float("NL4-Weight", 0.9f, 0f..2f) { nl4Enabled && options.rotationsActive }

    // 噪声系统 - NL5
    private val nl5Enabled by boolean("NL5-Enabled", false) { options.rotationsActive }
    private val nl5Frequency by float("NL5-Frequency", 4.0f, 0.01f..10f) { nl5Enabled && options.rotationsActive }
    private val nl5Amplitude by float("NL5-Amplitude", 0.1f, 0f..5f) { nl5Enabled && options.rotationsActive }
    private val nl5Octaves by int("NL5-Octaves", 6, 1..8) { nl5Enabled && options.rotationsActive }
    private val nl5Persistence by float("NL5-Persistence", 0.3f, 0.1f..1f) { nl5Enabled && options.rotationsActive }
    private val nl5Lacunarity by float("NL5-Lacunarity", 1.5f, 1f..4f) { nl5Enabled && options.rotationsActive }
    private val nl5ApplyToYaw by boolean("NL5-ApplyToYaw", true) { nl5Enabled && options.rotationsActive }
    private val nl5ApplyToPitch by boolean("NL5-ApplyToPitch", true) { nl5Enabled && options.rotationsActive }
    private val nl5PhaseOffset by float("NL5-PhaseOffset", 4.5f, 0f..6.283f) { nl5Enabled && options.rotationsActive }
    private val nl5Seed by int("NL5-Seed", 5555, 0..1000000) { nl5Enabled && options.rotationsActive }
    private val nl5NoiseType by choices("NL5-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "White") { nl5Enabled && options.rotationsActive }
    private val nl5Interpolation by choices("NL5-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cosine") { nl5Enabled && options.rotationsActive }
    private val nl5AmplitudeModulation by float("NL5-AmplitudeModulation", 0.5f, 0f..1f) { nl5Enabled && options.rotationsActive }
    private val nl5FrequencyModulation by float("NL5-FrequencyModulation", 0.4f, 0f..1f) { nl5Enabled && options.rotationsActive }
    private val nl5TimeFactor by float("NL5-TimeFactor", 3.0f, 0f..10f) { nl5Enabled && options.rotationsActive }
    private val nl5Smoothness by float("NL5-Smoothness", 0.3f, 0f..1f) { nl5Enabled && options.rotationsActive }
    private val nl5ClippingMin by float("NL5-ClippingMin", -0.3f, -5f..5f) { nl5Enabled && options.rotationsActive }
    private val nl5ClippingMax by float("NL5-ClippingMax", 0.3f, -5f..5f) { nl5Enabled && options.rotationsActive }
    private val nl5Normalization by boolean("NL5-Normalization", true) { nl5Enabled && options.rotationsActive }
    private val nl5Weight by float("NL5-Weight", 0.4f, 0f..2f) { nl5Enabled && options.rotationsActive }

    // 噪声系统 - NL6
    private val nl6Enabled by boolean("NL6-Enabled", false) { options.rotationsActive }
    private val nl6Frequency by float("NL6-Frequency", 1.5f, 0.01f..10f) { nl6Enabled && options.rotationsActive }
    private val nl6Amplitude by float("NL6-Amplitude", 0.25f, 0f..5f) { nl6Enabled && options.rotationsActive }
    private val nl6Octaves by int("NL6-Octaves", 3, 1..8) { nl6Enabled && options.rotationsActive }
    private val nl6Persistence by float("NL6-Persistence", 0.55f, 0.1f..1f) { nl6Enabled && options.rotationsActive }
    private val nl6Lacunarity by float("NL6-Lacunarity", 2.1f, 1f..4f) { nl6Enabled && options.rotationsActive }
    private val nl6ApplyToYaw by boolean("NL6-ApplyToYaw", true) { nl6Enabled && options.rotationsActive }
    private val nl6ApplyToPitch by boolean("NL6-ApplyToPitch", true) { nl6Enabled && options.rotationsActive }
    private val nl6PhaseOffset by float("NL6-PhaseOffset", 0.8f, 0f..6.283f) { nl6Enabled && options.rotationsActive }
    private val nl6Seed by int("NL6-Seed", 6666, 0..1000000) { nl6Enabled && options.rotationsActive }
    private val nl6NoiseType by choices("NL6-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Simplex") { nl6Enabled && options.rotationsActive }
    private val nl6Interpolation by choices("NL6-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cubic") { nl6Enabled && options.rotationsActive }
    private val nl6AmplitudeModulation by float("NL6-AmplitudeModulation", 0.1f, 0f..1f) { nl6Enabled && options.rotationsActive }
    private val nl6FrequencyModulation by float("NL6-FrequencyModulation", 0.05f, 0f..1f) { nl6Enabled && options.rotationsActive }
    private val nl6TimeFactor by float("NL6-TimeFactor", 1.2f, 0f..10f) { nl6Enabled && options.rotationsActive }
    private val nl6Smoothness by float("NL6-Smoothness", 0.65f, 0f..1f) { nl6Enabled && options.rotationsActive }
    private val nl6ClippingMin by float("NL6-ClippingMin", -0.7f, -5f..5f) { nl6Enabled && options.rotationsActive }
    private val nl6ClippingMax by float("NL6-ClippingMax", 0.7f, -5f..5f) { nl6Enabled && options.rotationsActive }
    private val nl6Normalization by boolean("NL6-Normalization", true) { nl6Enabled && options.rotationsActive }
    private val nl6Weight by float("NL6-Weight", 0.7f, 0f..2f) { nl6Enabled && options.rotationsActive }

    // 噪声系统 - NL7
    private val nl7Enabled by boolean("NL7-Enabled", false) { options.rotationsActive }
    private val nl7Frequency by float("NL7-Frequency", 2.5f, 0.01f..10f) { nl7Enabled && options.rotationsActive }
    private val nl7Amplitude by float("NL7-Amplitude", 0.15f, 0f..5f) { nl7Enabled && options.rotationsActive }
    private val nl7Octaves by int("NL7-Octaves", 4, 1..8) { nl7Enabled && options.rotationsActive }
    private val nl7Persistence by float("NL7-Persistence", 0.45f, 0.1f..1f) { nl7Enabled && options.rotationsActive }
    private val nl7Lacunarity by float("NL7-Lacunarity", 1.9f, 1f..4f) { nl7Enabled && options.rotationsActive }
    private val nl7ApplyToYaw by boolean("NL7-ApplyToYaw", true) { nl7Enabled && options.rotationsActive }
    private val nl7ApplyToPitch by boolean("NL7-ApplyToPitch", true) { nl7Enabled && options.rotationsActive }
    private val nl7PhaseOffset by float("NL7-PhaseOffset", 2.0f, 0f..6.283f) { nl7Enabled && options.rotationsActive }
    private val nl7Seed by int("NL7-Seed", 7777, 0..1000000) { nl7Enabled && options.rotationsActive }
    private val nl7NoiseType by choices("NL7-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Value") { nl7Enabled && options.rotationsActive }
    private val nl7Interpolation by choices("NL7-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Hermite") { nl7Enabled && options.rotationsActive }
    private val nl7AmplitudeModulation by float("NL7-AmplitudeModulation", 0.25f, 0f..1f) { nl7Enabled && options.rotationsActive }
    private val nl7FrequencyModulation by float("NL7-FrequencyModulation", 0.15f, 0f..1f) { nl7Enabled && options.rotationsActive }
    private val nl7TimeFactor by float("NL7-TimeFactor", 1.8f, 0f..10f) { nl7Enabled && options.rotationsActive }
    private val nl7Smoothness by float("NL7-Smoothness", 0.55f, 0f..1f) { nl7Enabled && options.rotationsActive }
    private val nl7ClippingMin by float("NL7-ClippingMin", -0.4f, -5f..5f) { nl7Enabled && options.rotationsActive }
    private val nl7ClippingMax by float("NL7-ClippingMax", 0.4f, -5f..5f) { nl7Enabled && options.rotationsActive }
    private val nl7Normalization by boolean("NL7-Normalization", true) { nl7Enabled && options.rotationsActive }
    private val nl7Weight by float("NL7-Weight", 0.5f, 0f..2f) { nl7Enabled && options.rotationsActive }

    // 噪声系统 - NL8
    private val nl8Enabled by boolean("NL8-Enabled", false) { options.rotationsActive }
    private val nl8Frequency by float("NL8-Frequency", 0.8f, 0.01f..10f) { nl8Enabled && options.rotationsActive }
    private val nl8Amplitude by float("NL8-Amplitude", 0.35f, 0f..5f) { nl8Enabled && options.rotationsActive }
    private val nl8Octaves by int("NL8-Octaves", 5, 1..8) { nl8Enabled && options.rotationsActive }
    private val nl8Persistence by float("NL8-Persistence", 0.65f, 0.1f..1f) { nl8Enabled && options.rotationsActive }
    private val nl8Lacunarity by float("NL8-Lacunarity", 2.3f, 1f..4f) { nl8Enabled && options.rotationsActive }
    private val nl8ApplyToYaw by boolean("NL8-ApplyToYaw", true) { nl8Enabled && options.rotationsActive }
    private val nl8ApplyToPitch by boolean("NL8-ApplyToPitch", true) { nl8Enabled && options.rotationsActive }
    private val nl8PhaseOffset by float("NL8-PhaseOffset", 1.2f, 0f..6.283f) { nl8Enabled && options.rotationsActive }
    private val nl8Seed by int("NL8-Seed", 8888, 0..1000000) { nl8Enabled && options.rotationsActive }
    private val nl8NoiseType by choices("NL8-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Perlin") { nl8Enabled && options.rotationsActive }
    private val nl8Interpolation by choices("NL8-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Linear") { nl8Enabled && options.rotationsActive }
    private val nl8AmplitudeModulation by float("NL8-AmplitudeModulation", 0.35f, 0f..1f) { nl8Enabled && options.rotationsActive }
    private val nl8FrequencyModulation by float("NL8-FrequencyModulation", 0.25f, 0f..1f) { nl8Enabled && options.rotationsActive }
    private val nl8TimeFactor by float("NL8-TimeFactor", 0.9f, 0f..10f) { nl8Enabled && options.rotationsActive }
    private val nl8Smoothness by float("NL8-Smoothness", 0.45f, 0f..1f) { nl8Enabled && options.rotationsActive }
    private val nl8ClippingMin by float("NL8-ClippingMin", -0.9f, -5f..5f) { nl8Enabled && options.rotationsActive }
    private val nl8ClippingMax by float("NL8-ClippingMax", 0.9f, -5f..5f) { nl8Enabled && options.rotationsActive }
    private val nl8Normalization by boolean("NL8-Normalization", true) { nl8Enabled && options.rotationsActive }
    private val nl8Weight by float("NL8-Weight", 0.85f, 0f..2f) { nl8Enabled && options.rotationsActive }

    // 噪声系统 - NL9
    private val nl9Enabled by boolean("NL9-Enabled", false) { options.rotationsActive }
    private val nl9Frequency by float("NL9-Frequency", 3.5f, 0.01f..10f) { nl9Enabled && options.rotationsActive }
    private val nl9Amplitude by float("NL9-Amplitude", 0.08f, 0f..5f) { nl9Enabled && options.rotationsActive }
    private val nl9Octaves by int("NL9-Octaves", 7, 1..8) { nl9Enabled && options.rotationsActive }
    private val nl9Persistence by float("NL9-Persistence", 0.35f, 0.1f..1f) { nl9Enabled && options.rotationsActive }
    private val nl9Lacunarity by float("NL9-Lacunarity", 1.7f, 1f..4f) { nl9Enabled && options.rotationsActive }
    private val nl9ApplyToYaw by boolean("NL9-ApplyToYaw", true) { nl9Enabled && options.rotationsActive }
    private val nl9ApplyToPitch by boolean("NL9-ApplyToPitch", true) { nl9Enabled && options.rotationsActive }
    private val nl9PhaseOffset by float("NL9-PhaseOffset", 3.8f, 0f..6.283f) { nl9Enabled && options.rotationsActive }
    private val nl9Seed by int("NL9-Seed", 9999, 0..1000000) { nl9Enabled && options.rotationsActive }
    private val nl9NoiseType by choices("NL9-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "White") { nl9Enabled && options.rotationsActive }
    private val nl9Interpolation by choices("NL9-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cosine") { nl9Enabled && options.rotationsActive }
    private val nl9AmplitudeModulation by float("NL9-AmplitudeModulation", 0.45f, 0f..1f) { nl9Enabled && options.rotationsActive }
    private val nl9FrequencyModulation by float("NL9-FrequencyModulation", 0.35f, 0f..1f) { nl9Enabled && options.rotationsActive }
    private val nl9TimeFactor by float("NL9-TimeFactor", 2.5f, 0f..10f) { nl9Enabled && options.rotationsActive }
    private val nl9Smoothness by float("NL9-Smoothness", 0.25f, 0f..1f) { nl9Enabled && options.rotationsActive }
    private val nl9ClippingMin by float("NL9-ClippingMin", -0.2f, -5f..5f) { nl9Enabled && options.rotationsActive }
    private val nl9ClippingMax by float("NL9-ClippingMax", 0.2f, -5f..5f) { nl9Enabled && options.rotationsActive }
    private val nl9Normalization by boolean("NL9-Normalization", true) { nl9Enabled && options.rotationsActive }
    private val nl9Weight by float("NL9-Weight", 0.3f, 0f..2f) { nl9Enabled && options.rotationsActive }

    // 噪声系统 - NL10
    private val nl10Enabled by boolean("NL10-Enabled", false) { options.rotationsActive }
    private val nl10Frequency by float("NL10-Frequency", 1.2f, 0.01f..10f) { nl10Enabled && options.rotationsActive }
    private val nl10Amplitude by float("NL10-Amplitude", 0.18f, 0f..5f) { nl10Enabled && options.rotationsActive }
    private val nl10Octaves by int("NL10-Octaves", 3, 1..8) { nl10Enabled && options.rotationsActive }
    private val nl10Persistence by float("NL10-Persistence", 0.5f, 0.1f..1f) { nl10Enabled && options.rotationsActive }
    private val nl10Lacunarity by float("NL10-Lacunarity", 2.0f, 1f..4f) { nl10Enabled && options.rotationsActive }
    private val nl10ApplyToYaw by boolean("NL10-ApplyToYaw", true) { nl10Enabled && options.rotationsActive }
    private val nl10ApplyToPitch by boolean("NL10-ApplyToPitch", true) { nl10Enabled && options.rotationsActive }
    private val nl10PhaseOffset by float("NL10-PhaseOffset", 0.5f, 0f..6.283f) { nl10Enabled && options.rotationsActive }
    private val nl10Seed by int("NL10-Seed", 1010, 0..1000000) { nl10Enabled && options.rotationsActive }
    private val nl10NoiseType by choices("NL10-NoiseType", arrayOf("Perlin", "Simplex", "Value", "White"), "Simplex") { nl10Enabled && options.rotationsActive }
    private val nl10Interpolation by choices("NL10-Interpolation", arrayOf("Linear", "Cosine", "Cubic", "Hermite"), "Cubic") { nl10Enabled && options.rotationsActive }
    private val nl10AmplitudeModulation by float("NL10-AmplitudeModulation", 0.15f, 0f..1f) { nl10Enabled && options.rotationsActive }
    private val nl10FrequencyModulation by float("NL10-FrequencyModulation", 0.1f, 0f..1f) { nl10Enabled && options.rotationsActive }
    private val nl10TimeFactor by float("NL10-TimeFactor", 1.1f, 0f..10f) { nl10Enabled && options.rotationsActive }
    private val nl10Smoothness by float("NL10-Smoothness", 0.6f, 0f..1f) { nl10Enabled && options.rotationsActive }
    private val nl10ClippingMin by float("NL10-ClippingMin", -0.6f, -5f..5f) { nl10Enabled && options.rotationsActive }
    private val nl10ClippingMax by float("NL10-ClippingMax", 0.6f, -5f..5f) { nl10Enabled && options.rotationsActive }
    private val nl10Normalization by boolean("NL10-Normalization", true) { nl10Enabled && options.rotationsActive }
    private val nl10Weight by float("NL10-Weight", 0.65f, 0f..2f) { nl10Enabled && options.rotationsActive }

    // PID控制器参数
    private val usePidController by boolean("UsePIDController", false) { options.rotationsActive }
    private val pidProportionalGain by float("PID-ProportionalGain", 1.0f, 0f..10f) { usePidController && options.rotationsActive }
    private val pidIntegralGain by float("PID-IntegralGain", 0.1f, 0f..5f) { usePidController && options.rotationsActive }
    private val pidDerivativeGain by float("PID-DerivativeGain", 0.05f, 0f..5f) { usePidController && options.rotationsActive }
    private val pidErrorSensitivity by float("PID-ErrorSensitivity", 1.0f, 0f..5f) { usePidController && options.rotationsActive }
    private val pidBaseValue by float("PID-BaseValue", 0f, -10f..10f) { usePidController && options.rotationsActive }
    private val pidDecayFactor by float("PID-DecayFactor", 0.95f, 0.5f..1f) { usePidController && options.rotationsActive }
    private val pidDynamicGain by boolean("PID-DynamicGain", false) { usePidController && options.rotationsActive }
    private val pidMinGain by float("PID-MinGain", 0.1f, 0f..5f) { usePidController && pidDynamicGain && options.rotationsActive }
    private val pidMaxGain by float("PID-MaxGain", 3.0f, 0f..10f) { usePidController && pidDynamicGain && options.rotationsActive }
    private val pidNonlinearityFactor by float("PID-NonlinearityFactor", 1.0f, 0f..5f) { usePidController && options.rotationsActive }
    private val pidResponseCurve by choices("PID-ResponseCurve", arrayOf("Linear", "Exponential", "Logarithmic", "Sigmoid", "Tanh"), "Linear") { usePidController && options.rotationsActive }
    private val pidSmoothness by float("PID-Smoothness", 0.5f, 0f..1f) { usePidController && options.rotationsActive }
    private val pidNonlinearEnhancement by float("PID-NonlinearEnhancement", 0f, 0f..2f) { usePidController && options.rotationsActive }
    private val pidErrorThreshold by float("PID-ErrorThreshold", 5f, 0f..45f) { usePidController && options.rotationsActive }
    private val pidIntegralWindupLimit by float("PID-IntegralWindupLimit", 10f, 0f..50f) { usePidController && options.rotationsActive }
    private val pidDerivativeFilter by float("PID-DerivativeFilter", 0.1f, 0f..1f) { usePidController && options.rotationsActive }
    private val pidApplyToYaw by boolean("PID-ApplyToYaw", true) { usePidController && options.rotationsActive }
    private val pidApplyToPitch by boolean("PID-ApplyToPitch", true) { usePidController && options.rotationsActive }
    private val pidResetOnTargetChange by boolean("PID-ResetOnTargetChange", true) { usePidController && options.rotationsActive }
    private val pidAdaptiveTuning by boolean("PID-AdaptiveTuning", false) { usePidController && options.rotationsActive }
    private val pidTuningRate by float("PID-TuningRate", 0.01f, 0f..0.1f) { usePidController && pidAdaptiveTuning && options.rotationsActive }
    private val pidSetpointWeight by float("PID-SetpointWeight", 1.0f, 0f..2f) { usePidController && options.rotationsActive }
    private val pidOutputLimit by float("PID-OutputLimit", 10f, 0f..50f) { usePidController && options.rotationsActive }
    private val pidDeadZone by float("PID-DeadZone", 0.1f, 0f..5f) { usePidController && options.rotationsActive }
    private val pidOvershootReduction by float("PID-OvershootReduction", 0f, 0f..1f) { usePidController && options.rotationsActive }
    private val pidVelocityLimit by float("PID-VelocityLimit", 50f, 0f..180f) { usePidController && options.rotationsActive }
    private val pidAccelerationLimit by float("PID-AccelerationLimit", 100f, 0f..360f) { usePidController && options.rotationsActive }
    private val pidJerkLimit by float("PID-JerkLimit", 500f, 0f..1000f) { usePidController && options.rotationsActive }
    private val pidFeedForward by float("PID-FeedForward", 0f, -10f..10f) { usePidController && options.rotationsActive }
    private val pidIntegralSeparation by boolean("PID-IntegralSeparation", false) { usePidController && options.rotationsActive }
    private val pidIntegralSeparationThreshold by float("PID-IntegralSeparationThreshold", 10f, 0f..45f) { usePidController && pidIntegralSeparation && options.rotationsActive }
    private val pidDerivativeOnMeasurement by boolean("PID-DerivativeOnMeasurement", false) { usePidController && options.rotationsActive }
    private val pidSetpointRamping by boolean("PID-SetpointRamping", true) { usePidController && options.rotationsActive }
    private val pidRampRate by float("PID-RampRate", 10f, 0f..90f) { usePidController && pidSetpointRamping && options.rotationsActive }
    private val pidAntiWindup by boolean("PID-AntiWindup", true) { usePidController && options.rotationsActive }
    private val pidAntiWindupGain by float("PID-AntiWindupGain", 0.1f, 0f..1f) { usePidController && pidAntiWindup && options.rotationsActive }
    private val pidClamping by boolean("PID-Clamping", false) { usePidController && options.rotationsActive }
    private val pidClampMin by float("PID-ClampMin", -10f, -50f..50f) { usePidController && pidClamping && options.rotationsActive }
    private val pidClampMax by float("PID-ClampMax", 10f, -50f..50f) { usePidController && pidClamping && options.rotationsActive }
    private val pidResetIntegralOnSetpointChange by boolean("PID-ResetIntegralOnSetpointChange", true) { usePidController && options.rotationsActive }
    private val pidUseFilteredDerivative by boolean("PID-UseFilteredDerivative", true) { usePidController && options.rotationsActive }
    private val pidFilterCoefficient by float("PID-FilterCoefficient", 0.1f, 0f..1f) { usePidController && pidUseFilteredDerivative && options.rotationsActive }
    private val pidDerivativeKickReduction by boolean("PID-DerivativeKickReduction", true) { usePidController && options.rotationsActive }
    private val pidProportionalOnMeasurement by boolean("PID-ProportionalOnMeasurement", false) { usePidController && options.rotationsActive }
    private val pidBumplessTransfer by boolean("PID-BumplessTransfer", true) { usePidController && options.rotationsActive }
    private val pidTrackingTimeConstant by float("PID-TrackingTimeConstant", 1f, 0f..10f) { usePidController && pidBumplessTransfer && options.rotationsActive }
    private val pidManualMode by boolean("PID-ManualMode", false) { usePidController && options.rotationsActive }
    private val pidManualOutput by float("PID-ManualOutput", 0f, -10f..10f) { usePidController && pidManualMode && options.rotationsActive }

    // 超前系统参数
    private val leadSystemType by choices("LeadSystemType", arrayOf("None", "Predictive", "Adaptive", "Hybrid"), "None") { options.rotationsActive }
    private val leadPredictionHorizon by int("Lead-PredictionHorizon", 3, 1..10) { leadSystemType != "None" && options.rotationsActive }
    private val leadLearningRate by float("Lead-LearningRate", 0.1f, 0f..1f) { leadSystemType != "None" && options.rotationsActive }
    private val leadForgettingFactor by float("Lead-ForgettingFactor", 0.95f, 0.5f..1f) { leadSystemType != "None" && options.rotationsActive }
    private val leadNoiseRejection by float("Lead-NoiseRejection", 0.5f, 0f..1f) { leadSystemType != "None" && options.rotationsActive }
    private val leadAdaptiveGain by float("Lead-AdaptiveGain", 0.1f, 0f..1f) { leadSystemType == "Adaptive" && options.rotationsActive }
    private val leadModelOrder by int("Lead-ModelOrder", 2, 1..5) { leadSystemType != "None" && options.rotationsActive }
    private val leadSystemApplyToYaw2 by boolean("LeadSystemApplyToYaw2", true) { leadSystemType != "None" && options.rotationsActive }
    private val leadSystemApplyToPitch2 by boolean("LeadSystemApplyToPitch2", true) { leadSystemType != "None" && options.rotationsActive }
    private val leadResetOnTargetChange by boolean("Lead-ResetOnTargetChange", true) { leadSystemType != "None" && options.rotationsActive }

    private val randomization = RandomizationSettings(this) { options.rotationsActive }
    private val outBorder by boolean("OutBorder", false) { options.rotationsActive }

    private val highestBodyPointToTargetValue = choices(
        "HighestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Head"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val lowestPoint = RotationUtils.BodyPoint.fromString(lowestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, lowestPoint, RotationUtils.BodyPoint.HEAD)
        coercedPoint.displayName
    }
    private val highestBodyPointToTarget: String by highestBodyPointToTargetValue

    private val lowestBodyPointToTargetValue = choices(
        "LowestBodyPointToTarget", arrayOf("Head", "Body", "Feet"), "Feet"
    ) {
        options.rotationsActive
    }.onChange { _, new ->
        val newPoint = RotationUtils.BodyPoint.fromString(new)
        val highestPoint = RotationUtils.BodyPoint.fromString(highestBodyPointToTarget)
        val coercedPoint = RotationUtils.coerceBodyPoint(newPoint, RotationUtils.BodyPoint.FEET, highestPoint)
        coercedPoint.displayName
    }

    private val lowestBodyPointToTarget: String by lowestBodyPointToTargetValue

    private val horizontalBodySearchRange by floatRange(
        "HorizontalBodySearchRange", 0f..1f, 0f..1f
    ) { options.rotationsActive }

    private val fov by float("FOV", 180f, 0f..180f)

    // Prediction
    private val predictClientMovement by int("PredictClientMovement", 2, 0..5)
    private val predictOnlyWhenOutOfRange by boolean(
        "PredictOnlyWhenOutOfRange", false
    ) { predictClientMovement != 0 }
    private val predictEnemyPosition by float("PredictEnemyPosition", 1.5f, -1f..2f)

    private val forceFirstHit by boolean("ForceFirstHit", false) { !respectMissCooldown && !useHitDelay }

    // Extra swing
    private val failSwing by boolean("FailSwing", true) { swing && options.rotationsActive }
    private val respectMissCooldown by boolean(
        "RespectMissCooldown", false
    ) { swing && failSwing && options.rotationsActive }
    private val swingOnlyInAir by boolean("SwingOnlyInAir", true) { swing && failSwing && options.rotationsActive }
    private val maxRotationDifferenceToSwing by float(
        "MaxRotationDifferenceToSwing", 180f, 0f..180f
    ) { swing && failSwing && options.rotationsActive }
    private val swingWhenTicksLate = boolean("SwingWhenTicksLate", false) {
        swing && failSwing && maxRotationDifferenceToSwing != 180f && options.rotationsActive
    }
    private val ticksLateToSwing by int(
        "TicksLateToSwing", 4, 0..20
    ) { swing && failSwing && swingWhenTicksLate.isActive() && options.rotationsActive }
    private val renderBoxOnSwingFail by boolean("RenderBoxOnSwingFail", false) { failSwing }
    private val renderBoxColor = ColorSettingsInteger(this, "RenderBoxColor") { renderBoxOnSwingFail }.with(Color.CYAN)
    private val renderBoxFadeSeconds by float("RenderBoxFadeSeconds", 1f, 0f..5f) { renderBoxOnSwingFail }

    // Inventory
    private val simulateClosingInventory by boolean("SimulateClosingInventory", false) { !noInventoryAttack }
    private val noInventoryAttack by boolean("NoInvAttack", false)
    private val noInventoryDelay by int("NoInvDelay", 200, 0..500) { noInventoryAttack }
    private val noConsumeAttack by choices(
        "NoConsumeAttack", arrayOf("Off", "NoHits", "NoRotation"), "Off"
    ).subjective()

    // Visuals
    private val mark by choices("Mark", arrayOf("None", "Platform", "Box", "Circle"), "Circle").subjective()
    private val fakeSharp by boolean("FakeSharp", true).subjective()
    private val renderAimPointBox by boolean("RenderAimPointBox", false).subjective()
    private val aimPointBoxColor by color("AimPointBoxColor", Color.CYAN) { renderAimPointBox }.subjective()
    private val aimPointBoxSize by float("AimPointBoxSize", 0.1f, 0f..0.2F) { renderAimPointBox }.subjective()

    // Circle options
    private val circleStartColor by color("CircleStartColor", Color.BLUE) { mark == "Circle" }.subjective()
    private val circleEndColor by color("CircleEndColor", Color.CYAN.withAlpha(0)) { mark == "Circle" }.subjective()
    private val fillInnerCircle by boolean("FillInnerCircle", false) { mark == "Circle" }.subjective()
    private val withHeight by boolean("WithHeight", true) { mark == "Circle" }.subjective()
    private val animateHeight by boolean("AnimateHeight", false) { withHeight }.subjective()
    private val heightRange by floatRange("HeightRange", 0.0f..0.4f, -2f..2f) { withHeight }.subjective()
    private val extraWidth by float("ExtraWidth", 0F, 0F..2F) { mark == "Circle" }.subjective()
    private val animateCircleY by boolean("AnimateCircleY", true) { fillInnerCircle || withHeight }.subjective()
    private val circleYRange by floatRange("CircleYRange", 0F..0.5F, 0F..2F) { animateCircleY }.subjective()
    private val duration by float(
        "Duration", 1.5F, 0.5F..3F, suffix = "Seconds"
    ) { animateCircleY || animateHeight }.subjective()

    // Box option
    private val boxOutline by boolean("Outline", true) { mark == "Box" }.subjective()

    /**
     * MODULE
     */

    // Target
    var target: EntityLivingBase? = null
    private var hittable = false
    private val prevTargetEntities = mutableListOf<Int>()

    // Attack delay
    private val attackTimer = MSTimer()
    private var attackDelay = 0
    private var clicks = 0
    private var attackTickTimes = mutableListOf<Pair<MovingObjectPosition, Int>>()

    // Container Delay
    private var containerOpen = -1L

    // Block status
    var renderBlocking = false
    var blockStatus = false
    private var blockStopInDead = false

    // Switch Delay
    private val switchTimer = MSTimer()

    // Blink AutoBlock
    private var blinked = false

    // Swing fails
    private val swingFails = mutableListOf<SwingFailData>()

    /**
     * Disable kill aura module
     */
    override fun onToggle(state: Boolean) {
        target = null
        hittable = false
        prevTargetEntities.clear()
        attackTickTimes.clear()
        attackTimer.reset()
        clicks = 0

        if (blinkAutoBlock) {
            BlinkUtils.unblink()
            blinked = false
        }

        if (autoF5) mc.gameSettings.thirdPersonView = 0

        stopBlocking(true)

        synchronized(swingFails) {
            swingFails.clear()
        }
    }

    val onRotationUpdate = handler<RotationUpdateEvent> {
        update()
    }

    fun update() {
        if (cancelRun || (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay))) return

        // Update target
        updateTarget()

        if (autoF5) {
            if (mc.gameSettings.thirdPersonView != 1 && target != null) {
                mc.gameSettings.thirdPersonView = 1
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        attackTickTimes.clear()

        if (blinkAutoBlock && BlinkUtils.isBlinking) BlinkUtils.unblink()

        synchronized(swingFails) {
            swingFails.clear()
        }
    }

    /**
     * Tick event
     */
    val onTick = handler<GameTickEvent>(priority = 2) {
        val player = mc.thePlayer ?: return@handler

        if (blockStatus && player.heldItem?.item !is ItemSword) {
            blockStatus = false
            renderBlocking = false
            return@handler
        }

        if (shouldPrioritize()) {
            target = null
            renderBlocking = false
            return@handler
        }

        if (clickOnly && !mc.gameSettings.keyBindAttack.isKeyDown) {
            clicks = 0
            return@handler
        }

        if (blockStatus && autoBlock == "Packet" && releaseAutoBlock && !ignoreTickRule) {
            clicks = 0
            stopBlocking()
            return@handler
        }

        if (cancelRun) {
            target = null
            hittable = false
            stopBlocking()
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        if (simulateCooldown && getAttackCooldownProgress() < 1f) {
            return@handler
        }

        if (target == null && !blockStopInDead) {
            blockStopInDead = true
            stopBlocking()
            return@handler
        }

        if (blinkAutoBlock) {
            when (player.ticksExisted % (blinkBlockTicks + 1)) {
                0 -> {
                    if (blockStatus && !blinked && !BlinkUtils.isBlinking) {
                        blinked = true
                    }
                }

                1 -> {
                    if (blockStatus && blinked && BlinkUtils.isBlinking) {
                        stopBlocking()
                    }
                }

                blinkBlockTicks -> {
                    if (!blockStatus && blinked && BlinkUtils.isBlinking) {
                        BlinkUtils.unblink()
                        blinked = false

                        startBlocking(target!!, interactAutoBlock, autoBlock == "Fake") // block again
                    }
                }
            }
        }

        if (target != null) {
            if (player.getDistanceToEntityBox(target!!) > blockMaxRange && blockStatus) {
                stopBlocking(true)
                return@handler
            } else {
                if (autoBlock != "Off" && !releaseAutoBlock) {
                    renderBlocking = true
                }
            }

            // Usually when you butterfly click, you end up clicking two (and possibly more) times in a single tick.
            // Sometimes you also do not click. The positives outweigh the negatives, however.
            val extraClicks = if (simulateDoubleClicking && !simulateCooldown) nextInt(-1, 1) else 0

            // Generate clicks based on distance from us to target.
            val generatedClicks = if (generateClicksBasedOnDist) {
                val distance = player.getDistanceToEntityBox(target!!)
                ((distance / distanceFactor.random()) * cpsMultiplier.random()).roundToInt()
            } else 0

            var maxClicks = clicks + extraClicks + generatedClicks

            val prevHittable = hittable

            updateHittable()

            if (!prevHittable && hittable && maxClicks == 0 && forceFirstHit) {
                maxClicks++
            }

            repeat(maxClicks) {
                val wasBlocking = blockStatus

                runAttack(it == 0, it + 1 == maxClicks)
                clicks--

                if (wasBlocking && !blockStatus && (releaseAutoBlock && !ignoreTickRule || autoBlock == "Off")) {
                    return@handler
                }
            }
        } else {
            renderBlocking = false
        }
    }

    /**
     * Render event
     */
    val onRender3D = handler<Render3DEvent> {
        handleFailedSwings()

        drawAimPointBox()

        if (cancelRun) {
            target = null
            hittable = false
            return@handler
        }

        if (noInventoryAttack && (mc.currentScreen is GuiContainer || System.currentTimeMillis() - containerOpen < noInventoryDelay)) {
            target = null
            hittable = false
            if (mc.currentScreen is GuiContainer) containerOpen = System.currentTimeMillis()
            return@handler
        }

        target ?: return@handler

        if (attackTimer.hasTimePassed(attackDelay)) {
            if (cps.last > 0) clicks++
            attackTimer.reset()

            attackDelay = randomClickDelay(cps.first, cps.last)
        }

        val hittableColor = if (hittable) Color(37, 126, 255, 70) else Color(255, 0, 0, 70)

        if (targetMode != "Multi") {
            when (mark.lowercase()) {
                "none" -> return@handler
                "platform" -> drawPlatform(target!!, hittableColor)
                "box" -> drawEntityBox(target!!, hittableColor, boxOutline)
                "circle" -> drawCircle(
                    target!!,
                    duration * 1000F,
                    heightRange.takeIf { animateHeight } ?: heightRange.endInclusive..heightRange.endInclusive,
                    extraWidth,
                    fillInnerCircle,
                    withHeight,
                    circleYRange.takeIf { animateCircleY },
                    circleStartColor.rgb,
                    circleEndColor.rgb
                )
            }
        }
    }

    /**
     * Attack enemy
     */
    private fun runAttack(isFirstClick: Boolean, isLastClick: Boolean) {
        val currentTarget = this.target ?: return

        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (noConsumeAttack == "NoHits" && isConsumingItem()) {
            return
        }

        // Settings
        val multi = targetMode == "Multi"
        val manipulateInventory = simulateClosingInventory && !noInventoryAttack && serverOpenInventory

        // 动态调整hurtTime
        val adjustedHurtTime = if (hittable && currentTarget.hurtTime > minAttackHurtTime && currentTarget.hurtTime < maxAttackHurtTime) {
            currentTarget.hurtTime
        } else if (hittable && currentTarget.hurtTime > hurtTime) {
            return
        } else {
            hurtTime
        }

        if (hittable && currentTarget.hurtTime > adjustedHurtTime) {
            return
        }

        // Check if enemy is not hittable
        if (!hittable && options.rotationsActive) {
            if (swing && failSwing) {
                val rotation = currentRotation ?: player.rotation

                // Can humans keep click consistency when performing massive rotation changes?
                // (10-30 rotation difference/doing large mouse movements for example)
                // Maybe apply to attacks too?
                if (rotationDifference(rotation) > maxRotationDifferenceToSwing) {
                    // At the same time there is also a chance of the user clicking at least once in a while
                    // when the consistency has dropped a lot.
                    val shouldIgnore = swingWhenTicksLate.isActive() && ticksSinceClick() >= ticksLateToSwing

                    if (!shouldIgnore) {
                        return
                    }
                }

                runWithModifiedRaycastResult(rotation, range.toDouble(), throughWallsRange.toDouble()) {
                    if (swingOnlyInAir && !it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    // Left click miss cool-down logic:
                    // When you click and miss, you receive a 10 tick cool down.
                    // It decreases gradually (tick by tick) when you hold the button.
                    // If you click and then release the button, the cool down drops from where it was immediately to 0.
                    // Most humans will release the button 1-2 ticks max after clicking, leaving them with an average of 10 CPS.
                    // The maximum CPS allowed when you miss a hit is 20 CPS, if you click and release immediately, which is highly unlikely.
                    // With that being said, we force an average of 10 CPS by doing this below, since 10 CPS when missing is possible.
                    if (respectMissCooldown && ticksSinceClick() <= 1 && it.typeOfHit.isMiss) {
                        return@runWithModifiedRaycastResult
                    }

                    val shouldEnterBlockBreakProgress =
                        !shouldDelayClick(it.typeOfHit) || attackTickTimes.lastOrNull()?.first?.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK

                    if (shouldEnterBlockBreakProgress) {
                        // Close inventory when open
                        if (manipulateInventory && isFirstClick) serverOpenInventory = false
                    }

                    val prevCooldown = mc.leftClickCounter

                    // Is any GUI coming from our client?
                    val isAnyClientGuiActive = mc.currentScreen?.javaClass?.`package`?.name?.contains(
                        LiquidBounce.CLIENT_NAME, ignoreCase = true
                    ) == true

                    if (isAnyClientGuiActive) {
                        mc.leftClickCounter = 0
                    }

                    if (!shouldDelayClick(it.typeOfHit)) {
                        attackTickTimes += it to runTimeTicks

                        if (it.typeOfHit.isEntity) {
                            val entity = it.entityHit

                            // Use own function instead of clickMouse() to maintain keep sprint, auto block, etc
                            if (entity is EntityLivingBase && isSelected(entity, true)) {
                                attackEntity(entity, isLastClick)
                            } else attackTickTimes -= it to runTimeTicks
                        } else {
                            // Imitate game click
                            mc.clickMouse()

                            if (renderBoxOnSwingFail) {
                                synchronized(swingFails) {
                                    val centerDistance = (currentTarget.hitBox.center - player.eyes).lengthVector()
                                    val spot = player.eyes + getVectorForRotation(rotation) * centerDistance

                                    swingFails += SwingFailData(spot, System.currentTimeMillis())
                                }
                            }
                        }
                    }

                    if (shouldEnterBlockBreakProgress && isLastClick) {
                        /**
                         * This is used to update the block breaking progress, resulting in sending an animation packet.
                         *
                         * Setting this function's parameter to [false] would still obey vanilla clicking logic,
                         * but only if you were releasing the click button immediately after pressing. Does not seem legit
                         * in the long term, right? This is why we are going to set it to [true], so it can send the animation packet.
                         */
                        mc.sendClickBlockToController(true)
                        /**
                         * Since we want to simulate proper clicking behavior, we schedule the block break progress stop
                         * in the next tick, since that is a doable action by the average player.
                         */
                        nextTick {
                            mc.sendClickBlockToController(false)

                            // Swings are sent a tick after stopping the block break progress.
                            clicks = 0

                            // [manipulateInventory] could have been changed at that point, but it is okay because
                            // serverOpenInventory's backing fields check for same values.
                            if (manipulateInventory) serverOpenInventory = true
                        }
                    }

                    if (isAnyClientGuiActive) {
                        mc.leftClickCounter = prevCooldown
                    }
                }
            }

            return
        }

        // Close inventory when open
        if (manipulateInventory && isFirstClick) serverOpenInventory = false

        blockStopInDead = false

        if (!multi) {
            attackEntity(currentTarget, isLastClick)
        } else {
            var targets = 0

            for (entity in world.loadedEntityList) {
                val distance = player.getDistanceToEntityBox(entity)

                if (entity is EntityLivingBase && isSelected(entity, true) && distance <= getRange(entity)) {
                    attackEntity(entity, isLastClick)

                    targets += 1

                    if (limitedMultiTargets != 0 && limitedMultiTargets <= targets) break
                }
            }
        }

        if (!isLastClick) return

        val switchMode = targetMode == "Switch"

        if (!switchMode || switchTimer.hasTimePassed(switchDelay)) {
            prevTargetEntities += currentTarget.entityId

            if (switchMode) {
                switchTimer.reset()
            }
        }

        // Open inventory
        if (manipulateInventory) serverOpenInventory = true
    }

    /**
     * Update current target
     */
    private fun updateTarget() {
        if (shouldPrioritize()) return

        // Reset fixed target to null
        target = null

        val switchMode = targetMode == "Switch"

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        var bestTarget: EntityLivingBase? = null
        var bestValue: Double? = null

        for (entity in theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || !isSelected(
                    entity, true
                ) || switchMode && entity.entityId in prevTargetEntities
            ) continue

            val distance = Backtrack.runWithNearestTrackedDistance(entity) { thePlayer.getDistanceToEntityBox(entity) }

            if (switchMode && distance > range && prevTargetEntities.isNotEmpty()) continue

            val entityFov = rotationDifference(entity)

            if (distance > maxRange || fov != 180F && entityFov > fov) continue

            if (switchMode && !isLookingOnEntities(entity, maxSwitchFOV.toDouble())) continue

            val currentValue = when (priority.lowercase()) {
                "distance" -> distance
                "direction" -> entityFov.toDouble()
                "health" -> entity.health.toDouble()
                "livingtime" -> -entity.ticksExisted.toDouble()
                "armor" -> entity.totalArmorValue.toDouble()
                "hurtresistance" -> entity.hurtResistantTime.toDouble()
                "hurttime" -> entity.hurtTime.toDouble()
                "healthabsorption" -> (entity.health + entity.absorptionAmount).toDouble()
                "regenamplifier" -> if (entity.isPotionActive(Potion.regeneration)) {
                    entity.getActivePotionEffect(Potion.regeneration).amplifier.toDouble()
                } else -1.0

                "inweb" -> if (entity.isInWeb) -1.0 else Double.MAX_VALUE
                "onladder" -> if (entity.isOnLadder) -1.0 else Double.MAX_VALUE
                "inliquid" -> if (entity.isInWater || entity.isInLava) -1.0 else Double.MAX_VALUE
                else -> null
            } ?: continue

            if (bestValue == null || currentValue < bestValue) {
                bestValue = currentValue
                bestTarget = entity
            }
        }

        if (bestTarget != null) {
            if (Backtrack.runWithNearestTrackedDistance(bestTarget) { updateRotations(bestTarget) }) {
                target = bestTarget
                return
            }
        }

        if (prevTargetEntities.isNotEmpty()) {
            prevTargetEntities.clear()
            updateTarget()
        }
    }

    /**
     * Attack [entity]
     */
    private fun attackEntity(entity: EntityLivingBase, isLastClick: Boolean) {
        val thePlayer = mc.thePlayer

        if (shouldPrioritize()) return

        if (thePlayer.isBlocking && (autoBlock == "Off" && blockStatus || autoBlock == "Packet" && releaseAutoBlock)) {
            stopBlocking()

            if (!ignoreTickRule || autoBlock == "Off") {
                return
            }
        }

        // The function is only called when we are facing an entity
        if (shouldDelayClick(MovingObjectPosition.MovingObjectType.ENTITY)) {
            return
        }

        if (!blinkAutoBlock || !BlinkUtils.isBlinking) {
            val affectSprint = false.takeIf { KeepSprint.handleEvents() || keepSprint }

            thePlayer.attackEntityWithModifiedSprint(entity, affectSprint) { if (swing) thePlayer.swingItem() }

            // Apply enchantment critical effect if FakeSharp is enabled
            if (EnchantmentHelper.getModifierForCreature(
                    thePlayer.heldItem, entity.creatureAttribute
                ) <= 0F && fakeSharp
            ) {
                thePlayer.onEnchantmentCritical(entity)
            }
        }

        // Start blocking after attack
        if (autoBlock != "Off" && (thePlayer.isBlocking || canBlock) && (!blinkAutoBlock && isLastClick || blinkAutoBlock && (!blinked || !BlinkUtils.isBlinking))) {
            startBlocking(entity, interactAutoBlock, autoBlock == "Fake")
        }

        resetLastAttackedTicks()
    }

    /**
     * Update rotations to enemy
     */
    private fun updateRotations(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false

        if (shouldPrioritize()) return false

        if (!options.rotationsActive) {
            return player.getDistanceToEntityBox(entity) <= range
        }

        val prediction = entity.currPos.subtract(entity.prevPos).times(2 + predictEnemyPosition.toDouble())

        val boundingBox = entity.hitBox.offset(prediction)
        val (currPos, oldPos) = player.currPos to player.prevPos

        val simPlayer = SimulatedPlayer.fromClientPlayer(RotationUtils.modifiedInput)

        simPlayer.rotationYaw = (currentRotation ?: player.rotation).yaw

        var pos = currPos

        repeat(predictClientMovement) {
            val previousPos = simPlayer.pos

            simPlayer.tick()

            if (predictOnlyWhenOutOfRange) {
                player.setPosAndPrevPos(simPlayer.pos)

                val currDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(previousPos)

                val prevDist = player.getDistanceToEntityBox(entity)

                player.setPosAndPrevPos(currPos, oldPos)
                pos = simPlayer.pos

                if (currDist <= range && currDist <= prevDist) {
                    return@repeat
                }
            }

            pos = previousPos
        }

        player.setPosAndPrevPos(pos)

        val rotation = searchCenter(
            boundingBox,
            generateSpotBasedOnDistance,
            outBorder && !attackTimer.hasTimePassed(attackDelay / 2),
            randomization,
            predict = false,
            lookRange = range + scanRange,
            attackRange = range,
            throughWallsRange = throughWallsRange,
            bodyPoints = listOf(highestBodyPointToTarget, lowestBodyPointToTarget),
            horizontalSearch = horizontalBodySearchRange
        )

        if (rotation == null) {
            player.setPosAndPrevPos(currPos, oldPos)

            return false
        }

        setTargetRotation(rotation, options = options)

        player.setPosAndPrevPos(currPos, oldPos)

        return true
    }

    private fun ticksSinceClick() = runTimeTicks - (attackTickTimes.lastOrNull()?.second ?: 0)

    /**
     * Check if enemy is hittable with current rotations
     */
    private fun updateHittable() {
        val eyes = mc.thePlayer.eyes

        val currentRotation = currentRotation ?: mc.thePlayer.rotation
        val target = this.target ?: return

        if (shouldPrioritize()) return

        if (!options.rotationsActive) {
            hittable = mc.thePlayer.getDistanceToEntityBox(target) <= range
            return
        }

        var chosenEntity: Entity? = null

        if (raycast) {
            chosenEntity = raycastEntity(
                range.toDouble(), currentRotation.yaw, currentRotation.pitch
            ) { entity -> !livingRaycast || entity is EntityLivingBase && entity !is EntityArmorStand }

            if (chosenEntity != null && chosenEntity is EntityLivingBase && (NoFriends.handleEvents() || !(chosenEntity is EntityPlayer && chosenEntity.isClientFriend()))) {
                if (raycastIgnored && target != chosenEntity) {
                    this.target = chosenEntity
                }
            }

            hittable = this.target == chosenEntity
        } else {
            hittable = isRotationFaced(target, range.toDouble(), currentRotation)
        }

        var shouldExcept = false

        chosenEntity ?: this.target?.run {
            if (ForwardTrack.handleEvents()) {
                ForwardTrack.includeEntityTruePos(this) {
                    checkIfAimingAtBox(this, currentRotation, eyes, onSuccess = {
                        hittable = true

                        shouldExcept = true
                    })
                }
            }
        }

        if (!hittable || shouldExcept) {
            return
        }

        val targetToCheck = chosenEntity ?: this.target ?: return

        // If player is inside entity, automatic yes because the intercept below cannot check for that
        // Minecraft does the same, see #EntityRenderer line 353
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            return
        }

        var checkNormally = true

        if (Backtrack.handleEvents()) {
            Backtrack.loopThroughBacktrackData(targetToCheck) {
                var result = false

                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = {
                    checkNormally = false

                    result = true
                }, onFail = {
                    result = false
                })

                return@loopThroughBacktrackData result
            }
        } else if (ForwardTrack.handleEvents()) {
            ForwardTrack.includeEntityTruePos(targetToCheck) {
                checkIfAimingAtBox(targetToCheck, currentRotation, eyes, onSuccess = { checkNormally = false })
            }
        }

        if (!checkNormally) {
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        // Is the entity box raycast vector visible? If not, check through-wall range
        hittable =
            isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange
    }

    /**
     * Start blocking
     */
    private fun startBlocking(interactEntity: Entity, interact: Boolean, fake: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (blockStatus && (!uncpAutoBlock || !blinkAutoBlock) || shouldPrioritize()) return

        if (mc.thePlayer.isBlocking) {
            blockStatus = true
            renderBlocking = true
            return
        }

        if (unblockMode == "Empty" && player.inventory.firstEmptyStack !in 0..8) {
            return
        }

        if (!fake) {
            if (!(blockRate > 0 && nextInt(100) <= blockRate)) return

            if (interact) {
                val positionEye = player.eyes

                val boundingBox = interactEntity.hitBox

                val (yaw, pitch) = currentRotation ?: player.rotation

                val vec = getVectorForRotation(Rotation(yaw, pitch))

                val lookAt = positionEye.add(vec * maxRange.toDouble())

                val movingObject = boundingBox.calculateIntercept(positionEye, lookAt) ?: return
                val hitVec = movingObject.hitVec

                sendPackets(
                    C02PacketUseEntity(interactEntity, hitVec - interactEntity.positionVector),
                    C02PacketUseEntity(interactEntity, INTERACT)
                )

            }

            if (switchStartBlock) {
                switchToSlot((SilentHotbar.currentSlot + 1) % 9)
            }

            sendPacket(C08PacketPlayerBlockPlacement(player.heldItem))
            blockStatus = true
        }

        renderBlocking = true

        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
    }

    /**
     * Stop blocking
     */
    private fun stopBlocking(forceStop: Boolean = false) {
        val player = mc.thePlayer ?: return

        if (!forceStop) {
            if (blockStatus && !mc.thePlayer.isBlocking) {

                when (unblockMode.lowercase()) {
                    "stop" -> {
                        sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    }

                    "switch" -> {
                        switchToSlot((SilentHotbar.currentSlot + 1) % 9)
                    }

                    "empty" -> {
                        player.inventory.firstEmptyStack.takeIf { it in 0..8 }.let {
                            if (it == null) {
                                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                                return@let
                            }

                            switchToSlot(it)
                        }
                    }
                }

                blockStatus = false
            }
        } else {
            if (blockStatus) {
                sendPacket(C07PacketPlayerDigging(RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            }

            blockStatus = false
        }

        renderBlocking = false
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        if (autoBlock == "Off" || !blinkAutoBlock || !blinked) return@handler

        if (player.isDead || player.ticksExisted < 20) {
            BlinkUtils.unblink()
            return@handler
        }

        if (Blink.blinkingSend() || Blink.blinkingReceive()) {
            BlinkUtils.unblink()
            return@handler
        }

        BlinkUtils.blink(packet, event)
    }

    /**
     * Checks if raycast landed on a different object
     *
     * The game requires at least 1 tick of cool-down on raycast object type change (miss, block, entity)
     * We are doing the same thing here but allow more cool-down.
     */
    private fun shouldDelayClick(currentType: MovingObjectPosition.MovingObjectType): Boolean {
        if (!useHitDelay) {
            return false
        }

        val lastAttack = attackTickTimes.lastOrNull()

        return lastAttack != null && lastAttack.first.typeOfHit != currentType && runTimeTicks - lastAttack.second <= hitDelayTicks
    }

    private fun checkIfAimingAtBox(
        targetToCheck: Entity, currentRotation: Rotation, eyes: Vec3, onSuccess: () -> Unit,
        onFail: () -> Unit = { },
    ) {
        if (targetToCheck.hitBox.isVecInside(eyes)) {
            onSuccess()
            return
        }

        // Recreate raycast logic
        val intercept = targetToCheck.hitBox.calculateIntercept(
            eyes, eyes + getVectorForRotation(currentRotation) * range.toDouble()
        )

        if (intercept != null) {
            // Is the entity box raycast vector visible? If not, check through-wall range
            hittable =
                isVisible(intercept.hitVec) || mc.thePlayer.getDistanceToEntityBox(targetToCheck) <= throughWallsRange

            if (hittable) {
                onSuccess()
                return
            }
        }

        onFail()
    }

    private fun switchToSlot(slot: Int) {
        SilentHotbar.selectSlotSilently(this, slot, immediate = true)
        SilentHotbar.resetSlot(this, true)
    }

    private fun shouldPrioritize(): Boolean = when {
        !onScaffold && (Scaffold.handleEvents() && (Scaffold.placeRotation != null || currentRotation != null) || Tower.handleEvents() && Tower.isTowering) -> true

        !onDestroyBlock && (Fucker.handleEvents() && !Fucker.noHit && Fucker.pos != null && !Fucker.isOwnBed || Nuker.handleEvents()) -> true

        activationSlot && SilentHotbar.currentSlot != preferredSlot - 1 -> true

        else -> false
    }

    private fun handleFailedSwings() {
        if (!renderBoxOnSwingFail) return

        val box = AxisAlignedBB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        synchronized(swingFails) {
            val fadeSeconds = renderBoxFadeSeconds * 1000L
            val colorSettings = renderBoxColor

            val renderManager = mc.renderManager

            swingFails.removeAll {
                val timestamp = System.currentTimeMillis() - it.startTime
                val transparency = (0f..255f).lerpWith(1 - (timestamp / fadeSeconds).coerceAtMost(1.0F))

                val offsetBox = box.offset(it.vec3 - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offsetBox, colorSettings.color(a = transparency.roundToInt()))

                timestamp > fadeSeconds
            }
        }
    }

    private fun drawAimPointBox() {
        val player = mc.thePlayer ?: return
        val target = this.target ?: return

        if (!renderAimPointBox) {
            return
        }

        val f = aimPointBoxSize.toDouble()

        val box = AxisAlignedBB(0.0, 0.0, 0.0, f, f, f)

        val renderManager = mc.renderManager

        runWithSimulatedPosition(player, player.interpolatedPosition(player.prevPos)) {
            runWithSimulatedPosition(target, target.interpolatedPosition(target.prevPos)) {
                val rotationVec = player.eyes + getVectorForRotation(
                    serverRotation.lerpWith(currentRotation ?: player.rotation, mc.timer.renderPartialTicks)
                ) * player.getDistanceToEntityBox(target).coerceAtMost(range.toDouble())

                val offSetBox = box.offset(rotationVec - renderManager.renderPos)

                RenderUtils.drawAxisAlignedBB(offSetBox, aimPointBoxColor)
            }
        }
    }

    /**
     * Check if run should be cancelled
     */
    private val cancelRun
        inline get() = mc.thePlayer.isSpectator || !isAlive(mc.thePlayer) || noConsumeAttack == "NoRotation" && isConsumingItem()

    /**
     * Check if [entity] is alive
     */
    private fun isAlive(entity: EntityLivingBase) = entity.isEntityAlive && entity.health > 0

    /**
     * Check if player is able to block
     */
    private val canBlock: Boolean
        get() {
            val player = mc.thePlayer ?: return false

            if (target != null && player.heldItem?.item is ItemSword) {
                if (smartAutoBlock) {
                    if (player.isMoving && forceBlock) return false

                    if (checkWeapon && target?.heldItem?.item !is ItemSword && target?.heldItem?.item !is ItemAxe) return false

                    if (player.hurtTime > maxOwnHurtTime) return false

                    val rotationToPlayer = toRotation(player.hitBox.center, true, target!!)

                    if (rotationDifference(rotationToPlayer, target!!.rotation) > maxDirectionDiff) return false

                    if (target!!.swingProgressInt > maxSwingProgress) return false

                    if (target!!.getDistanceToEntityBox(player) > blockRange) return false
                }

                if (player.getDistanceToEntityBox(target!!) > blockMaxRange) return false

                return true
            }

            return false
        }

    /**
     * Range
     */
    private val maxRange
        get() = max(range + scanRange, throughWallsRange)

    private fun getRange(entity: Entity): Float {
        val baseRange = if (mc.thePlayer.getDistanceToEntityBox(entity) >= throughWallsRange) range + scanRange else throughWallsRange
        var reduction = 0f
        
        // 冲刺范围减少
        if (mc.thePlayer.isSprinting) {
            reduction += attackRangeSprintReduceMinValue + (attackRangeSprintReduceMaxValue - attackRangeSprintReduceMinValue) * nextFloat()
        }
        
        // 空中范围减少
        if (!mc.thePlayer.onGround) {
            reduction += attackRangeAirReduceMinValue + (attackRangeAirReduceMaxValue - attackRangeAirReduceMinValue) * nextFloat()
        }
        
        return baseRange - reduction - if (mc.thePlayer.isSprinting) rangeSprintReduction else 0F
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = targetMode

    val isBlockingChestAura
        get() = handleEvents() && target != null
}