//Full Ai Code
//这是石别用
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.rotation.RaycastUtils.runWithModifiedRaycastResult
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0APacketAnimation
import kotlin.math.*

object VelocityClick : Module("VelocityClick", Category.COMBAT) {

    // 基础选项
    private val mode by choices("Mode", arrayOf("Normal", "Burst", "Continuous", "Smart", "Predictive", "Pattern", "Adaptive"), "Normal")
    private val clicks by intRange("Clicks", 3..5, 1..50)
    private val hurtTimeToClick by int("HurtTimeToClick", 10, 0..10)
    private val whenFacingEnemyOnly by boolean("WhenFacingEnemyOnly", true)
    private val ignoreBlocking by boolean("IgnoreBlocking", false)
    private val clickRange by float("ClickRange", 3f, 0.5f..6f)
    private val swingMode by choices("SwingMode", arrayOf("Off", "Normal", "Packet", "Legit", "ServerSide", "Random", "Conditional"), "Normal")
    private val chance by int("Chance", 100, 0..100)
    private val delayBetweenClicks by int("DelayBetweenClicks", 0, 0..500)

    // 噪声随机化选项 - 基础噪声
    private val baseNoise by boolean("BaseNoise", true)
    private val noiseIntensity by float("NoiseIntensity", 0.3f, 0f..1f) { baseNoise }
    private val frequency by float("Frequency", 1f, 0.1f..5f) { baseNoise }
    private val amplitude by float("Amplitude", 0.5f, 0f..2f) { baseNoise }
    private val noiseType by choices("NoiseType", arrayOf("White", "Perlin", "Simplex", "Fractal", "Gaussian", "Random"), "Perlin") { baseNoise }
    private val noiseSeed by int("NoiseSeed", 12345, 0..100000) { baseNoise && noiseType != "Random" }
    private val temporalNoise by boolean("TemporalNoise", true) { baseNoise }
    private val timeScale by float("TimeScale", 0.01f, 0f..0.1f) { temporalNoise && baseNoise }
    private val spatialNoise by boolean("SpatialNoise", false) { baseNoise }
    private val spatialScale by float("SpatialScale", 0.1f, 0f..1f) { spatialNoise && baseNoise }
    private val noiseOctaves by int("NoiseOctaves", 4, 1..8) { baseNoise && noiseType in arrayOf("Perlin", "Simplex", "Fractal") }

    // 攻击噪声选项
    private val clickNoise by boolean("ClickNoise", true)
    private val clickCountNoise by boolean("ClickCountNoise", false) { clickNoise }
    private val clickCountMean by float("ClickCountMean", 4f, 1f..20f) { clickCountNoise && clickNoise }
    private val clickCountStdDev by float("ClickCountStdDev", 1f, 0f..5f) { clickCountNoise && clickNoise }
    private val clickIntervalNoise by boolean("ClickIntervalNoise", true) { clickNoise }
    private val clickIntervalBase by int("ClickIntervalBase", 50, 0..500) { clickIntervalNoise && clickNoise }
    private val clickIntervalVariation by int("ClickIntervalVariation", 20, 0..200) { clickIntervalNoise && clickNoise }
    private val clickPatternNoise by boolean("ClickPatternNoise", false) { clickNoise }
    private val patternLength by int("PatternLength", 10, 1..50) { clickPatternNoise && clickNoise }
    private val patternRandomness by float("PatternRandomness", 0.3f, 0f..1f) { clickPatternNoise && clickNoise }
    private val doubleClickChance by int("DoubleClickChance", 5, 0..50) { clickNoise }
    private val missClickChance by int("MissClickChance", 2, 0..20) { clickNoise }
    private val prematureClickChance by int("PrematureClickChance", 3, 0..30) { clickNoise }
    private val delayedClickChance by int("DelayedClickChance", 3, 0..30) { clickNoise }

    // 目标选择噪声选项
    private val targetSelectionNoise by boolean("TargetSelectionNoise", true)
    private val targetSwitchNoise by boolean("TargetSwitchNoise", false) { targetSelectionNoise }
    private val switchProbability by float("SwitchProbability", 0.1f, 0f..1f) { targetSwitchNoise && targetSelectionNoise }
    private val targetPriorityNoise by boolean("TargetPriorityNoise", true) { targetSelectionNoise }
    private val priorityWeights by boolean("PriorityWeights", false) { targetPriorityNoise && targetSelectionNoise }
    private val distanceWeight by float("DistanceWeight", 0.6f, 0f..1f) { priorityWeights && targetPriorityNoise && targetSelectionNoise }
    private val healthWeight by float("HealthWeight", 0.3f, 0f..1f) { priorityWeights && targetPriorityNoise && targetSelectionNoise }
    private val angleWeight by float("AngleWeight", 0.1f, 0f..1f) { priorityWeights && targetPriorityNoise && targetSelectionNoise }
    private val targetJitter by boolean("TargetJitter", true) { targetSelectionNoise }
    private val jitterAmount by float("JitterAmount", 0.2f, 0f..1f) { targetJitter && targetSelectionNoise }
    private val targetDrift by boolean("TargetDrift", false) { targetSelectionNoise }
    private val driftSpeed by float("DriftSpeed", 0.05f, 0f..0.2f) { targetDrift && targetSelectionNoise }
    private val ghostTargets by boolean("GhostTargets", false) { targetSelectionNoise }
    private val ghostProbability by float("GhostProbability", 0.05f, 0f..0.2f) { ghostTargets && targetSelectionNoise }
    private val targetMemory by boolean("TargetMemory", true) { targetSelectionNoise }
    private val memoryDecay by float("MemoryDecay", 0.9f, 0f..1f) { targetMemory && targetSelectionNoise }

    // 距离噪声选项
    private val distanceNoise by boolean("DistanceNoise", true)
    private val rangeVariation by boolean("RangeVariation", true) { distanceNoise }
    private val rangeMean by float("RangeMean", 3f, 1f..6f) { rangeVariation && distanceNoise }
    private val rangeStdDev by float("RangeStdDev", 0.3f, 0f..1f) { rangeVariation && distanceNoise }
    private val dynamicRangeNoise by boolean("DynamicRangeNoise", false) { distanceNoise }
    private val rangeOscillation by boolean("RangeOscillation", false) { dynamicRangeNoise && distanceNoise }
    private val oscillationAmplitude by float("OscillationAmplitude", 0.5f, 0f..2f) { rangeOscillation && dynamicRangeNoise && distanceNoise }
    private val oscillationFrequency by float("OscillationFrequency", 0.5f, 0f..2f) { rangeOscillation && dynamicRangeNoise && distanceNoise }
    private val distanceHysteresis by boolean("DistanceHysteresis", true) { distanceNoise }
    private val hysteresisThreshold by float("HysteresisThreshold", 0.2f, 0f..1f) { distanceHysteresis && distanceNoise }
    private val proximityBoost by boolean("ProximityBoost", false) { distanceNoise }
    private val boostThreshold by float("BoostThreshold", 2f, 1f..4f) { proximityBoost && distanceNoise }
    private val boostMultiplier by float("BoostMultiplier", 1.5f, 1f..3f) { proximityBoost && distanceNoise }

    // 角度噪声选项
    private val angleNoise by boolean("AngleNoise", true)
    private val angleJitter by boolean("AngleJitter", true) { angleNoise }
    private val jitterX by float("JitterX", 2f, 0f..10f) { angleJitter && angleNoise }
    private val jitterY by float("JitterY", 1f, 0f..5f) { angleJitter && angleNoise }
    private val angleDrift by boolean("AngleDrift", false) { angleNoise }
    private val driftX by float("DriftX", 0.5f, 0f..2f) { angleDrift && angleNoise }
    private val driftY by float("DriftY", 0.3f, 0f..1f) { angleDrift && angleNoise }
    private val angleTremor by boolean("AngleTremor", false) { angleNoise }
    private val tremorFrequency by float("TremorFrequency", 10f, 1f..50f) { angleTremor && angleNoise }
    private val tremorAmplitude by float("TremorAmplitude", 0.5f, 0f..2f) { angleTremor && angleNoise }
    private val smoothSaccades by boolean("SmoothSaccades", true) { angleNoise }
    private val saccadeSpeed by float("SaccadeSpeed", 5f, 1f..20f) { smoothSaccades && angleNoise }
    private val microCorrections by boolean("MicroCorrections", false) { angleNoise }
    private val correctionFrequency by int("CorrectionFrequency", 10, 1..100) { microCorrections && angleNoise }

    // 时机噪声选项
    private val timingNoise by boolean("TimingNoise", true)
    private val delayNoise by boolean("DelayNoise", true) { timingNoise }
    private val delayDistribution by choices("DelayDistribution", arrayOf("Uniform", "Normal", "Exponential", "Gamma", "LogNormal", "Weibull"), "Normal") { delayNoise && timingNoise }
    private val delayMean by int("DelayMean", 50, 0..500) { delayNoise && timingNoise }
    private val delayStdDev by int("DelayStdDev", 20, 0..200) { delayNoise && timingNoise }
    private val delaySkew by float("DelaySkew", 0f, -1f..1f) { delayNoise && timingNoise && delayDistribution in arrayOf("LogNormal", "Gamma") }
    private val reactionNoise by boolean("ReactionNoise", false) { timingNoise }
    private val reactionMean by int("ReactionMean", 200, 50..1000) { reactionNoise && timingNoise }
    private val reactionStdDev by int("ReactionStdDev", 50, 0..200) { reactionNoise && timingNoise }
    private val anticipation by boolean("Anticipation", false) { timingNoise }
    private val anticipationTime by int("AnticipationTime", 100, 0..500) { anticipation && timingNoise }
    private val anticipationAccuracy by float("AnticipationAccuracy", 0.7f, 0f..1f) { anticipation && timingNoise }
    private val rhythmNoise by boolean("RhythmNoise", false) { timingNoise }
    private val rhythmPattern by boolean("RhythmPattern", false) { rhythmNoise && timingNoise }
    private val patternType by choices("PatternType", arrayOf("Even", "Swing", "Shuffle", "Random"), "Even") { rhythmPattern && rhythmNoise && timingNoise }
    private val tempo by float("Tempo", 120f, 60f..240f) { rhythmNoise && timingNoise }

    // 移动噪声选项
    private val movementNoise by boolean("MovementNoise", true)
    private val strafeNoise by boolean("StrafeNoise", false) { movementNoise }
    private val strafePattern by choices("StrafePattern", arrayOf("Circle", "Zigzag", "Random", "Sinusoidal", "Lissajous"), "Circle") { strafeNoise && movementNoise }
    private val strafeAmplitude by float("StrafeAmplitude", 0.2f, 0f..1f) { strafeNoise && movementNoise }
    private val strafeFrequency by float("StrafeFrequency", 1f, 0.1f..5f) { strafeNoise && movementNoise }
    private val walkNoise by boolean("WalkNoise", false) { movementNoise }
    private val walkSpeedVariation by float("WalkSpeedVariation", 0.1f, 0f..0.5f) { walkNoise && movementNoise }
    private val stepNoise by boolean("StepNoise", false) { movementNoise }
    private val stepPattern by choices("StepPattern", arrayOf("Regular", "Irregular", "Stutter", "Skip"), "Regular") { stepNoise && movementNoise }
    private val jumpNoise by boolean("JumpNoise", false) { movementNoise }
    private val jumpTimingNoise by float("JumpTimingNoise", 0.2f, 0f..1f) { jumpNoise && movementNoise }
    private val jumpHeightNoise by float("JumpHeightNoise", 0.1f, 0f..0.5f) { jumpNoise && movementNoise }
    private val sprintNoise by boolean("SprintNoise", false) { movementNoise }
    private val sprintPattern by choices("SprintPattern", arrayOf("Continuous", "Burst", "Interval"), "Continuous") { sprintNoise && movementNoise }

    // 视觉噪声选项
    private val visualNoise by boolean("VisualNoise", false)
    private val fovNoise by boolean("FOVNoise", false) { visualNoise }
    private val fovVariation by float("FOVVariation", 5f, 0f..20f) { fovNoise && visualNoise }
    private val cameraShake by boolean("CameraShake", false) { visualNoise }
    private val shakeIntensity by float("ShakeIntensity", 0.1f, 0f..1f) { cameraShake && visualNoise }
    private val shakeFrequency by float("ShakeFrequency", 10f, 1f..50f) { cameraShake && visualNoise }
    private val bobNoise by boolean("BobNoise", false) { visualNoise }
    private val bobAmount by float("BobAmount", 0.1f, 0f..0.5f) { bobNoise && visualNoise }
    private val bobSpeed by float("BobSpeed", 1f, 0.1f..3f) { bobNoise && visualNoise }
    private val blurNoise by boolean("BlurNoise", false) { visualNoise }
    private val blurAmount by float("BlurAmount", 0.1f, 0f..1f) { blurNoise && visualNoise }
    private val chromaticAberration by boolean("ChromaticAberration", false) { visualNoise }
    private val aberrationAmount by float("AberrationAmount", 0.02f, 0f..0.1f) { chromaticAberration && visualNoise }

    // 音频噪声选项
    private val audioNoise by boolean("AudioNoise", false)
    private val pitchNoise by boolean("PitchNoise", false) { audioNoise }
    private val pitchVariation by float("PitchVariation", 0.1f, 0f..0.5f) { pitchNoise && audioNoise }
    private val volumeNoise by boolean("VolumeNoise", false) { audioNoise }
    private val volumeVariation by float("VolumeVariation", 0.2f, 0f..1f) { volumeNoise && audioNoise }
    private val spatialAudio by boolean("SpatialAudio", false) { audioNoise }
    private val audioDelay by boolean("AudioDelay", false) { audioNoise }
    private val delayTime by int("DelayTime", 50, 0..500) { audioDelay && audioNoise }
    private val echoNoise by boolean("EchoNoise", false) { audioNoise }
    private val echoDecay by float("EchoDecay", 0.5f, 0f..1f) { echoNoise && audioNoise }

    // 网络噪声选项
    private val networkNoise by boolean("NetworkNoise", false)
    private val packetLoss by boolean("PacketLoss", false) { networkNoise }
    private val lossProbability by float("LossProbability", 0.01f, 0f..0.1f) { packetLoss && networkNoise }
    private val packetDelay by boolean("PacketDelay", false) { networkNoise }
    private val delayDistributionType by choices("DelayDistributionType", arrayOf("Constant", "Normal", "Exponential"), "Normal") { packetDelay && networkNoise }
    private val packetDelayMean by int("PacketDelayMean", 50, 0..500) { packetDelay && networkNoise }
    private val packetJitter by boolean("PacketJitter", false) { networkNoise }
    private val jitterAmountMs by int("JitterAmountMs", 10, 0..100) { packetJitter && networkNoise }
    private val packetReorder by boolean("PacketReorder", false) { networkNoise }
    private val reorderProbability by float("ReorderProbability", 0.005f, 0f..0.02f) { packetReorder && networkNoise }
    private val packetDuplication by boolean("PacketDuplication", false) { networkNoise }
    private val duplicationProbability by float("DuplicationProbability", 0.002f, 0f..0.01f) { packetDuplication && networkNoise }

    // 环境噪声选项
    private val environmentalNoise by boolean("EnvironmentalNoise", false)
    private val weatherEffects by boolean("WeatherEffects", false) { environmentalNoise }
    private val rainIntensity by float("RainIntensity", 0.5f, 0f..1f) { weatherEffects && environmentalNoise }
    private val windEffects by boolean("WindEffects", false) { environmentalNoise }
    private val windStrength by float("WindStrength", 0.3f, 0f..1f) { windEffects && environmentalNoise }
    private val timeOfDayEffects by boolean("TimeOfDayEffects", false) { environmentalNoise }
    private val dayNightCycle by boolean("DayNightCycle", false) { timeOfDayEffects && environmentalNoise }
    private val lightingNoise by boolean("LightingNoise", false) { environmentalNoise }
    private val lightVariation by float("LightVariation", 0.2f, 0f..1f) { lightingNoise && environmentalNoise }
    private val fogNoise by boolean("FogNoise", false) { environmentalNoise }
    private val fogDensity by float("FogDensity", 0.1f, 0f..1f) { fogNoise && environmentalNoise }

    // 生理噪声选项
    private val physiologicalNoise by boolean("PhysiologicalNoise", false)
    private val fatigue by boolean("Fatigue", false) { physiologicalNoise }
    private val fatigueRate by float("FatigueRate", 0.001f, 0f..0.01f) { fatigue && physiologicalNoise }
    private val recoveryRate by float("RecoveryRate", 0.0005f, 0f..0.005f) { fatigue && physiologicalNoise }
    private val adrenaline by boolean("Adrenaline", false) { physiologicalNoise }
    private val adrenalineBoost by float("AdrenalineBoost", 1.2f, 1f..2f) { adrenaline && physiologicalNoise }
    private val adrenalineDecay by float("AdrenalineDecay", 0.99f, 0.9f..1f) { adrenaline && physiologicalNoise }
    private val handTremor by boolean("HandTremor", false) { physiologicalNoise }
    private val tremorIntensity by float("TremorIntensity", 0.1f, 0f..0.5f) { handTremor && physiologicalNoise }
    private val eyeStrain by boolean("EyeStrain", false) { physiologicalNoise }
    private val strainRate by float("StrainRate", 0.001f, 0f..0.01f) { eyeStrain && physiologicalNoise }
    private val blinkEffect by boolean("BlinkEffect", false) { physiologicalNoise }
    private val blinkRate by float("BlinkRate", 0.1f, 0f..0.5f) { blinkEffect && physiologicalNoise }

    // 行为噪声选项
    private val behavioralNoise by boolean("BehavioralNoise", true)
    private val habitPatterns by boolean("HabitPatterns", false) { behavioralNoise }
    private val habitStrength by float("HabitStrength", 0.3f, 0f..1f) { habitPatterns && behavioralNoise }
    private val learningEffect by boolean("LearningEffect", false) { behavioralNoise }
    private val learningRate by float("LearningRate", 0.01f, 0f..0.1f) { learningEffect && behavioralNoise }
    private val forgettingRate by float("ForgettingRate", 0.001f, 0f..0.01f) { learningEffect && behavioralNoise }
    private val moodSwings by boolean("MoodSwings", false) { behavioralNoise }
    private val moodVolatility by float("MoodVolatility", 0.1f, 0f..0.5f) { moodSwings && behavioralNoise }
    private val decisionLatency by boolean("DecisionLatency", false) { behavioralNoise }
    private val latencyMean by int("LatencyMean", 100, 0..500) { decisionLatency && behavioralNoise }
    private val confidenceLevel by boolean("ConfidenceLevel", false) { behavioralNoise }
    private val confidenceThreshold by float("ConfidenceThreshold", 0.7f, 0f..1f) { confidenceLevel && behavioralNoise }

    // 模式特定噪声选项
    private val smartModeNoise by boolean("SmartModeNoise", true) { mode == "Smart" }
    private val predictionNoise by boolean("PredictionNoise", false) { mode == "Predictive" }
    private val patternModeNoise by boolean("PatternModeNoise", false) { mode == "Pattern" }
    private val adaptiveNoise by boolean("AdaptiveNoise", true) { mode == "Adaptive" }

    // 高级噪声控制
    private val noiseCorrelation by boolean("NoiseCorrelation", false)
    private val correlationMatrix by boolean("CorrelationMatrix", false) { noiseCorrelation }
    private val crossCorrelation by boolean("CrossCorrelation", false) { noiseCorrelation }
    private val noiseFiltering by boolean("NoiseFiltering", false)
    private val filterType by choices("FilterType", arrayOf("LowPass", "HighPass", "BandPass", "Kalman", "MovingAverage"), "LowPass") { noiseFiltering }
    private val filterCutoff by float("FilterCutoff", 0.5f, 0f..1f) { noiseFiltering }
    private val noiseModulation by boolean("NoiseModulation", false)
    private val modulatorType by choices("ModulatorType", arrayOf("AM", "FM", "PM", "PWM"), "AM") { noiseModulation }
    private val modulationDepth by float("ModulationDepth", 0.5f, 0f..1f) { noiseModulation }

    // 额外选项 - 修复缺失的变量
    private val randomHurtTime by boolean("RandomHurtTime", false)
    private val minHurtTime by int("MinHurtTime", 8, 0..10) { randomHurtTime }
    private val maxHurtTime by int("MaxHurtTime", 10, 0..10) { randomHurtTime }
    private val requireGround by boolean("RequireGround", true)
    private val requireNotMoving by boolean("RequireNotMoving", false)
    private val movingThreshold by float("MovingThreshold", 0.1f, 0f..1f) { requireNotMoving }
    private val attackDelay by int("AttackDelay", 0, 0..500)
    private val postAttackDelay by int("PostAttackDelay", 0, 0..500)
    private val reduceMotion by boolean("ReduceMotion", false)
    private val motionReduction by float("MotionReduction", 0.5f, 0f..1f) { reduceMotion }
    private val randomMotionReduction by boolean("RandomMotionReduction", false) { reduceMotion }
    private val minMotionReduction by float("MinMotionReduction", 0.3f, 0f..0.8f) { randomMotionReduction && reduceMotion }
    private val maxMotionReduction by float("MaxMotionReduction", 0.7f, 0.2f..1f) { randomMotionReduction && reduceMotion }
    private val stopSprint by boolean("StopSprint", false)
    private val requireSprint by boolean("RequireSprint", false)
    private val ignoreInvisible by boolean("IgnoreInvisible", true)
    private val randomAngle by boolean("RandomAngle", false)
    private val minAngle by float("MinAngle", 90f, 0f..150f) { randomAngle }
    private val maxAngleRandom by float("MaxAngleRandom", 150f, 30f..180f) { randomAngle }
    private val maxAngle by float("MaxAngle", 180f, 0f..180f)

    // 状态变量
    private val msTimer = MSTimer()
    private val attackTimer = MSTimer()
    private var attackCountValue = 0
    private var clickPatternIndex = 0
    private var currentTarget: Entity? = null
    private var lastTarget: Entity? = null
    private var targetMemoryMap = mutableMapOf<Int, Float>()
    private var noiseState = 0f
    private var angleDriftX = 0f
    private var angleDriftY = 0f
    private var fatigueLevel = 0f
    private var adrenalineLevel = 0f
    private var moodLevel = 0.5f
    private var confidence = 1f
    private var learningState = mutableMapOf<String, Float>()
    private var habitState = mutableMapOf<String, Float>()
    private var clickPattern = mutableListOf<Int>()
    private var rhythmState = 0f
    private var strafePhase = 0f
    private var cameraShakePhase = 0f
    private var bobPhase = 0f
    private var jumpTimer = 0L
    private var sprintState = false
    private var lastDecisionTime = 0L
    private var fovOffset = 0f
    private var cameraOffsetX = 0f
    private var cameraOffsetY = 0f
    private var audioState = 0f
    private var networkState = 0
    private var weatherState = 0f
    private var windState = 0f
    private var timeState = 0f
    private var lightState = 0f
    private var fogState = 0f
    private var tremorState = 0f
    private var eyeStrainLevel = 0f
    private var blinkState = false
    private var blinkTimer = 0L
    private var filterState = 0f
    private var modulationState = 0f

    // 初始化
    override fun onEnable() {
        resetNoiseState()
        generateClickPattern()
    }

    private fun resetNoiseState() {
        noiseState = 0f
        angleDriftX = 0f
        angleDriftY = 0f
        fatigueLevel = 0f
        adrenalineLevel = 0f
        moodLevel = 0.5f
        confidence = 1f
        learningState.clear()
        habitState.clear()
        clickPattern.clear()
        rhythmState = 0f
        strafePhase = 0f
        cameraShakePhase = 0f
        bobPhase = 0f
        jumpTimer = System.currentTimeMillis()
        sprintState = false
        lastDecisionTime = System.currentTimeMillis()
        fovOffset = 0f
        cameraOffsetX = 0f
        cameraOffsetY = 0f
        audioState = 0f
        networkState = 0
        weatherState = 0f
        windState = 0f
        timeState = 0f
        lightState = 0f
        fogState = 0f
        tremorState = 0f
        eyeStrainLevel = 0f
        blinkState = false
        blinkTimer = System.currentTimeMillis()
        filterState = 0f
        modulationState = 0f
        targetMemoryMap.clear()
        currentTarget = null
        lastTarget = null
        attackCountValue = 0
        clickPatternIndex = 0
        msTimer.reset()
        attackTimer.reset()
    }

    private fun generateClickPattern() {
        clickPattern.clear()
        val patternSize = if (clickPatternNoise) patternLength else 10
        for (i in 0 until patternSize) {
            val baseValue = when (patternType) {
                "Even" -> clicks.random()
                "Swing" -> (abs(i - patternSize / 2) + 1)
                "Shuffle" -> RandomUtils.nextInt(1, clicks.endInclusive + 1)
                "Random" -> if (RandomUtils.nextFloat() < patternRandomness) RandomUtils.nextInt(1, clicks.endInclusive + 1) else clicks.random()
                else -> clicks.random()
            }
            clickPattern.add(baseValue)
        }
    }

    // 噪声生成函数
    private fun generateNoise(x: Float = 0f, y: Float = 0f, z: Float = 0f, t: Float = System.currentTimeMillis() * timeScale): Float {
        return when (noiseType) {
            "White" -> RandomUtils.nextFloat(-1f, 1f)
            "Perlin" -> perlinNoise(x, y, z, t)
            "Simplex" -> simplexNoise(x, y, z, t)
            "Fractal" -> fractalNoise(x, y, z, t)
            "Gaussian" -> gaussianNoise()
            "Random" -> RandomUtils.nextFloat(-1f, 1f) * if (RandomUtils.nextBoolean()) 1f else -1f
            else -> 0f
        }
    }

    private fun perlinNoise(x: Float, y: Float, z: Float, t: Float): Float {
        // 简化版柏林噪声
        val frequency = frequency
        val amplitude = amplitude
        var total = 0f
        var maxValue = 0f
        var currentAmplitude = 1f
        
        for (i in 0 until noiseOctaves) {
            val nx = x * frequency * currentAmplitude + noiseSeed
            val ny = y * frequency * currentAmplitude + noiseSeed
            val nz = z * frequency * currentAmplitude + noiseSeed
            val nt = t * frequency * currentAmplitude
            
            val value = (sin(nx) * cos(ny) * sin(nz) * cos(nt)).toFloat()
            total += value * currentAmplitude
            maxValue += currentAmplitude
            currentAmplitude *= amplitude
        }
        
        return total / maxValue
    }

    private fun simplexNoise(x: Float, y: Float, z: Float, t: Float): Float {
        // 简化版单纯形噪声
        val frequency = frequency
        val nx = x * frequency + noiseSeed
        val ny = y * frequency + noiseSeed
        val nz = z * frequency + noiseSeed
        val nt = t * frequency
        
        return (sin(nx * 0.5f) * cos(ny * 0.5f) * sin(nz * 0.5f) * cos(nt * 0.5f)).toFloat()
    }

    private fun fractalNoise(x: Float, y: Float, z: Float, t: Float): Float {
        // 分形噪声
        var total = 0f
        var frequency = frequency
        var amplitude = amplitude
        var maxAmplitude = 0f
        
        for (i in 0 until noiseOctaves) {
            total += perlinNoise(x * frequency, y * frequency, z * frequency, t * frequency) * amplitude
            maxAmplitude += amplitude
            frequency *= 2f
            amplitude *= 0.5f
        }
        
        return total / maxAmplitude
    }

    private fun gaussianNoise(): Float {
        // 高斯噪声
        val u1 = RandomUtils.nextFloat()
        val u2 = RandomUtils.nextFloat()
        val z0 = sqrt(-2.0 * ln(u1.toDouble())) * cos(2.0 * Math.PI * u2.toDouble())
        return z0.toFloat() * 0.3f // 缩放
    }

    private fun filterNoise(value: Float, prevValue: Float): Float {
        return when (filterType) {
            "LowPass" -> filterState * (1 - filterCutoff) + value * filterCutoff
            "HighPass" -> value - filterState
            "MovingAverage" -> (filterState * 0.9f + value * 0.1f)
            else -> value
        }.also { filterState = it }
    }

    // 游戏事件处理
    val onGameTick = handler<GameTickEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler
        
        // 更新噪声状态
        updateNoiseState(thePlayer)
        
        // 检查基础条件
        if (!checkBasicConditions(thePlayer)) return@handler
        
        // 获取目标
        val target = getTarget(thePlayer, theWorld) ?: return@handler
        
        // 更新目标记忆
        updateTargetMemory(target)
        
        // 应用目标噪声（这里调用应用目标选择噪声的函数）
        val noisyTarget = applyTargetSelectionNoise(target, thePlayer, theWorld)
        currentTarget = noisyTarget ?: return@handler
        
        // 执行攻击
        executeAttack(thePlayer, currentTarget!!)
        
        // 应用移动噪声
        applyMovementNoise(thePlayer)
        
        // 应用视觉噪声
        applyVisualNoise(thePlayer)
        
        // 应用音频噪声
        applyAudioNoise(thePlayer)
        
        // 应用网络噪声
        applyNetworkNoise()
        
        // 应用环境噪声
        applyEnvironmentalNoise(theWorld)
        
        // 应用生理噪声
        applyPhysiologicalNoise(thePlayer)
        
        // 应用行为噪声
        applyBehavioralNoise(thePlayer)
    }

    val onPacket = handler<PacketEvent> { event ->
        // 应用网络噪声到数据包
        if (networkNoise) {
            applyPacketNoise(event)
        }
    }

    val onRender3D = handler<Render3DEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        
        // 渲染视觉噪声效果
        if (visualNoise) {
            renderVisualNoise(thePlayer)
        }
    }

    private fun updateNoiseState(player: EntityPlayerSP) {
        val time = System.currentTimeMillis()
        val deltaTime = (time - lastDecisionTime).coerceAtMost(100L).toFloat() / 1000f
        lastDecisionTime = time
        
        // 更新基础噪声
        if (baseNoise) {
            val x = player.posX.toFloat()
            val y = player.posY.toFloat()
            val z = player.posZ.toFloat()
            val t = time * timeScale
            
            noiseState = generateNoise(x, y, z, t)
            if (noiseFiltering) {
                noiseState = filterNoise(noiseState, filterState)
            }
        }
        
        // 更新角度漂移
        if (angleDrift && angleNoise) {
            angleDriftX += (RandomUtils.nextFloat() - 0.5f) * driftX * deltaTime
            angleDriftY += (RandomUtils.nextFloat() - 0.5f) * driftY * deltaTime
            angleDriftX = angleDriftX.coerceIn(-10f, 10f)
            angleDriftY = angleDriftY.coerceIn(-5f, 5f)
        }
        
        // 更新疲劳
        if (fatigue && physiologicalNoise) {
            fatigueLevel += fatigueRate * deltaTime
            fatigueLevel -= recoveryRate * deltaTime
            fatigueLevel = fatigueLevel.coerceIn(0f, 1f)
        }
        
        // 更新肾上腺素
        if (adrenaline && physiologicalNoise) {
            adrenalineLevel *= adrenalineDecay
            adrenalineLevel = adrenalineLevel.coerceIn(0f, 1f)
        }
        
        // 更新情绪
        if (moodSwings && behavioralNoise) {
            moodLevel += (RandomUtils.nextFloat() - 0.5f) * moodVolatility * deltaTime
            moodLevel = moodLevel.coerceIn(0f, 1f)
        }
        
        // 更新信心
        if (confidenceLevel && behavioralNoise) {
            confidence = 0.9f * confidence + 0.1f * (if (attackCountValue > 3) 1f else 0.8f)
            confidence = confidence.coerceIn(0f, 1f)
        }
        
        // 更新节奏状态
        if (rhythmNoise && timingNoise) {
            rhythmState += deltaTime * tempo / 60f
        }
        
        // 更新相机抖动
        if (cameraShake && visualNoise) {
            cameraShakePhase += deltaTime * shakeFrequency
        }
        
        // 更新头部摆动
        if (bobNoise && visualNoise) {
            bobPhase += deltaTime * bobSpeed
        }
        
        // 更新眨眼状态
        if (blinkEffect && physiologicalNoise && time - blinkTimer > 3000) {
            blinkState = !blinkState
            blinkTimer = time
        }
    }

    private fun checkBasicConditions(player: EntityPlayerSP): Boolean {
        // 检查冷却时间
        if (!msTimer.hasTimePassed(delayBetweenClicks)) return false
        
        // 检查随机几率
        if (RandomUtils.nextInt(0, 100) >= applyNoiseToValue(chance.toFloat(), 0.1f).toInt()) return false
        
        // 检查受伤时间
        val hurtTimeCheck = if (randomHurtTime) {
            player.hurtTime in minHurtTime..maxHurtTime
        } else {
            player.hurtTime == hurtTimeToClick
        }
        if (!hurtTimeCheck) return false
        
        // 检查格挡
        if (!ignoreBlocking && player.isBlocking) return false
        
        // 检查地面
        if (requireGround && !player.onGround) return false
        
        // 检查移动
        if (requireNotMoving && player.horizontalMoveSpeed() > movingThreshold) return false
        
        // 检查冲刺
        if (requireSprint && !player.isSprinting) return false
        
        return true
    }

    private fun getTarget(player: EntityPlayerSP, world: net.minecraft.world.World): Entity? {
        // 获取鼠标指向的实体
        var entity: Entity? = mc.objectMouseOver?.entityHit
        
        if (entity == null || !isValidTarget(entity, player)) {
            entity = if (whenFacingEnemyOnly) {
                getTargetByRaycast(player)
            } else {
                getTargetByDistance(player, world)
            }
        }
        
        // 应用目标选择噪声
        if (targetSelectionNoise) {
            entity = applyTargetSelectionNoise(entity, player, world)
        }
        
        return entity?.takeIf { isValidTarget(it, player) }
    }

    private fun getTargetByRaycast(player: EntityPlayerSP): Entity? {
        val rotation = currentRotation ?: return null
        
        // 应用角度噪声
        val noisyRotation = applyAngleNoise(rotation)
        
        var result: Entity? = null
        val range = getRange()
        
        runWithModifiedRaycastResult(
            noisyRotation,
            range.toDouble(),
            0.0
        ) { raycastResult ->
            result = raycastResult.entityHit?.takeIf { 
                isValidTarget(it, player) && checkAngleCondition(player, it)
            }
        }
        
        return result
    }

    private fun getTargetByDistance(player: EntityPlayerSP, world: net.minecraft.world.World): Entity? {
        val range = getRange()
        
        val targets = world.loadedEntityList
            .filter { isValidTarget(it, player) && player.getDistanceToEntity(it) <= range }
            .takeIf { it.isNotEmpty() } ?: return null
        
        // 应用目标优先级噪声
        return applyTargetPriorityNoise(targets, player)
    }

    private fun applyTargetSelectionNoise(currentTarget: Entity?, player: EntityPlayerSP, world: net.minecraft.world.World): Entity? {
        var target = currentTarget
        
        // 目标切换噪声
        if (targetSwitchNoise && RandomUtils.nextFloat() < switchProbability) {
            val otherTargets = world.loadedEntityList
                .filter { it != target && isValidTarget(it, player) }
                .takeIf { it.isNotEmpty() }
            
            target = otherTargets?.randomOrNull() ?: target
        }
        
        // 目标抖动
        if (targetJitter && target != null) {
            val jitter = generateNoise() * jitterAmount
            if (jitter > 0.5f) {
                val nearbyTargets = world.loadedEntityList
                    .filter { 
                        it != target && isValidTarget(it, player) && 
                        player.getDistanceToEntity(it) <= player.getDistanceToEntity(target!!) + 2f
                    }
                if (nearbyTargets.isNotEmpty()) {
                    target = nearbyTargets.randomOrNull()
                }
            }
        }
        
        // 幽灵目标
        if (ghostTargets && RandomUtils.nextFloat() < ghostProbability) {
            return null // 模拟误判目标
        }
        
        return target
    }

    private fun applyTargetPriorityNoise(targets: List<Entity>, player: EntityPlayerSP): Entity? {
        if (!targetPriorityNoise) {
            return targets.minByOrNull { player.getDistanceToEntity(it) }
        }
        
        val scoredTargets = targets.map { target ->
            var score = 0f
            
            if (priorityWeights) {
                // 距离分数
                val distance = player.getDistanceToEntity(target)
                val distanceScore = (1f - distance / clickRange) * distanceWeight
                
                // 生命值分数（如果目标有生命值）
                val healthScore = if (target is EntityLivingBase) {
                    (1f - target.health / 20f) * healthWeight
                } else 0f
                
                // 角度分数
                val angle = player.getAngleToEntity(target)
                val angleScore = (1f - angle / 180f) * angleWeight
                
                score = distanceScore + healthScore + angleScore
            } else {
                // 随机权重
                score = RandomUtils.nextFloat()
            }
            
            // 应用记忆分数
            if (targetMemory) {
                val memoryScore = targetMemoryMap[target.entityId] ?: 0f
                score += memoryScore * memoryDecay
            }
            
            Pair(target, score)
        }
        
        return scoredTargets.maxByOrNull { it.second }?.first
    }

    private fun updateTargetMemory(target: Entity) {
        if (targetMemory) {
            val currentScore = targetMemoryMap[target.entityId] ?: 0f
            targetMemoryMap[target.entityId] = currentScore + 0.1f
            
            // 记忆衰减
            targetMemoryMap.entries.forEach { entry ->
                entry.setValue(entry.value * 0.99f)
            }
        }
    }

    private fun applyAngleNoise(rotation: net.ccbluex.liquidbounce.utils.rotation.Rotation): net.ccbluex.liquidbounce.utils.rotation.Rotation {
        if (!angleNoise) return rotation
        
        var yaw = rotation.yaw
        var pitch = rotation.pitch
        
        // 角度抖动
        if (angleJitter) {
            yaw += generateNoise() * jitterX
            pitch += generateNoise() * jitterY
        }
        
        // 角度漂移
        if (angleDrift) {
            yaw += angleDriftX
            pitch += angleDriftY
        }
        
        // 角度震颤
        if (angleTremor) {
            val tremor = sin(System.currentTimeMillis() * 0.001 * tremorFrequency.toDouble()).toFloat() * tremorAmplitude
            yaw += tremor
            pitch += tremor * 0.5f
        }
        
        return net.ccbluex.liquidbounce.utils.rotation.Rotation(yaw, pitch)
    }

    private fun getRange(): Float {
        var range = clickRange
        
        if (distanceNoise) {
            if (rangeVariation) {
                val noise = generateNoise() * rangeStdDev
                range = (rangeMean + noise).coerceIn(1f, 6f)
            }
            
            if (dynamicRangeNoise && rangeOscillation) {
                val oscillation = sin(System.currentTimeMillis() * 0.001 * oscillationFrequency.toDouble()).toFloat() * oscillationAmplitude
                range += oscillation
            }
            
            if (proximityBoost) {
                // 根据目标接近程度提升范围
                currentTarget?.let { target ->
                    val distance = mc.thePlayer?.getDistanceToEntity(target) ?: 0f
                    if (distance < boostThreshold) {
                        range *= boostMultiplier
                    }
                }
            }
        }
        
        return range.coerceIn(0.5f, 6f)
    }

    private fun executeAttack(player: EntityPlayerSP, target: Entity) {
        // 计算攻击次数
        val attackCount = calculateAttackCount()
        
        // 攻击前延迟
        val preDelay = calculateDelay(attackDelay)
        if (preDelay > 0) Thread.sleep(preDelay.toLong())
        
        // 执行攻击
        for (i in 1..attackCount) {
            // 双击机会
            if (doubleClickChance > 0 && RandomUtils.nextInt(100) < doubleClickChance && i < attackCount) {
                performSingleAttack(player, target)
                Thread.sleep(20) // 短暂延迟模拟双击
                performSingleAttack(player, target)
                continue
            }
            
            // 失误点击机会
            if (missClickChance > 0 && RandomUtils.nextInt(100) < missClickChance) {
                // 模拟失误，不攻击
                Thread.sleep(50)
                continue
            }
            
            // 过早点击机会
            if (prematureClickChance > 0 && RandomUtils.nextInt(100) < prematureClickChance) {
                // 提前点击
                performSingleAttack(player, target)
                Thread.sleep(RandomUtils.nextInt(20, 100).toLong())
                continue
            }
            
            // 延迟点击机会
            if (delayedClickChance > 0 && RandomUtils.nextInt(100) < delayedClickChance) {
                Thread.sleep(RandomUtils.nextInt(50, 200).toLong())
            }
            
            // 正常攻击
            performSingleAttack(player, target)
            
            // 点击间隔
            if (i < attackCount) {
                val interval = calculateClickInterval()
                Thread.sleep(interval.toLong())
            }
        }
        
        // 更新状态
        msTimer.reset()
        attackCountValue++
        
        // 攻击后延迟
        val postDelay = calculateDelay(postAttackDelay)
        if (postDelay > 0) Thread.sleep(postDelay.toLong())
    }

    private fun calculateAttackCount(): Int {
        var count = clicks.random()
        
        if (clickNoise) {
            if (clickCountNoise) {
                // 高斯分布
                val noise = gaussianNoise() * clickCountStdDev
                count = (clickCountMean + noise).toInt().coerceIn(clicks.start, clicks.endInclusive)
            }
            
            if (clickPatternNoise && clickPattern.isNotEmpty()) {
                count = clickPattern[clickPatternIndex % clickPattern.size]
                clickPatternIndex++
            }
        }
        
        return count.coerceIn(1, 50)
    }

    private fun calculateDelay(baseDelay: Int): Int {
        if (!timingNoise || !delayNoise) return baseDelay
        
        return when (delayDistribution) {
            "Uniform" -> RandomUtils.nextInt(baseDelay - delayStdDev, baseDelay + delayStdDev + 1).coerceAtLeast(0)
            "Normal" -> {
                val noise = gaussianNoise() * delayStdDev
                (baseDelay + noise).toInt().coerceAtLeast(0)
            }
            "Exponential" -> {
                val lambda = 1.0 / baseDelay
                (-ln(1 - RandomUtils.nextFloat()) / lambda).toInt()
            }
            else -> baseDelay
        }
    }

    private fun calculateClickInterval(): Int {
        var interval = clickIntervalBase
        
        if (clickNoise && clickIntervalNoise) {
            interval += RandomUtils.nextInt(-clickIntervalVariation, clickIntervalVariation + 1)
            
            if (rhythmNoise) {
                // 根据节奏调整间隔
                val rhythmFactor = sin(rhythmState * 2 * Math.PI.toFloat())
                interval += (rhythmFactor * 20).toInt()
            }
        }
        
        return interval.coerceAtLeast(0)
    }

    private fun performSingleAttack(player: EntityPlayerSP, target: Entity) {
        // 处理摆动模式
        when (swingMode.lowercase()) {
            "normal" -> player.swingItem()
            "packet" -> sendPacket(C0APacketAnimation())
            "legit" -> {
                // Legit模式模拟真实左键
                player.swingItem()
                // 添加小的随机延迟模拟人类点击
                Thread.sleep(RandomUtils.nextInt(10, 30).toLong())
            }
            "serverside" -> {
                // 服务器端摆动
                sendPacket(C0APacketAnimation())
            }
            "random" -> {
                if (RandomUtils.nextBoolean()) player.swingItem() else sendPacket(C0APacketAnimation())
            }
            "conditional" -> {
                // 根据条件决定摆动方式
                if (player.getDistanceToEntity(target) < 2f) {
                    player.swingItem()
                } else {
                    sendPacket(C0APacketAnimation())
                }
            }
        }
        
        // 攻击实体
        player.attackEntityWithModifiedSprint(target, true) {
            // 攻击后回调
            afterAttackCallback(player, target)
        }
        
        // 更新肾上腺素
        if (adrenaline && physiologicalNoise) {
            adrenalineLevel += 0.1f
            adrenalineLevel = adrenalineLevel.coerceIn(0f, 1f)
        }
    }

    private fun afterAttackCallback(player: EntityPlayerSP, target: Entity) {
        // 应用攻击后效果
        if (reduceMotion) {
            val reduction = if (randomMotionReduction) 
                RandomUtils.nextFloat(minMotionReduction, maxMotionReduction) 
            else motionReduction
            
            player.motionX *= reduction.toDouble()
            player.motionZ *= reduction.toDouble()
        }
        
        if (stopSprint) {
            player.isSprinting = false
        }
    }

    private fun applyMovementNoise(player: EntityPlayerSP) {
        if (!movementNoise) return
        
        // 侧移噪声
        if (strafeNoise) {
            val strafeValue = when (strafePattern) {
                "Circle" -> {
                    val angle = strafePhase * 2 * Math.PI.toFloat()
                    (strafeAmplitude * sin(angle)).toFloat()
                }
                "Zigzag" -> {
                    val sawtooth = 2 * (strafePhase % 1f) - 1
                    (strafeAmplitude * sawtooth).toFloat()
                }
                "Sinusoidal" -> {
                    (strafeAmplitude * sin(strafePhase * 2 * Math.PI.toFloat())).toFloat()
                }
                "Lissajous" -> {
                    val a = 3f
                    val b = 2f
                    val delta = Math.PI.toFloat() / 2
                    (strafeAmplitude * sin(a * strafePhase * 2 * Math.PI.toFloat() + delta)).toFloat()
                }
                else -> (generateNoise() * strafeAmplitude).toFloat()
            }
            
            player.moveStrafing = strafeValue
            strafePhase += 0.1f * strafeFrequency
        }
        
        // 行走噪声
        if (walkNoise) {
            val speedVariation = generateNoise() * walkSpeedVariation
            player.moveForward = (player.moveForward * (1 + speedVariation)).toFloat()
        }
        
        // 跳跃噪声
        if (jumpNoise && player.onGround && System.currentTimeMillis() - jumpTimer > 1000) {
            if (RandomUtils.nextFloat() < jumpTimingNoise) {
                player.jump()
                player.motionY *= (1 + (generateNoise() * jumpHeightNoise)).toDouble()
                jumpTimer = System.currentTimeMillis()
            }
        }
        
        // 冲刺噪声
        if (sprintNoise) {
            when (sprintPattern) {
                "Burst" -> {
                    if (System.currentTimeMillis() % 3000 < 1000) {
                        player.isSprinting = true
                    } else {
                        player.isSprinting = false
                    }
                }
                "Interval" -> {
                    sprintState = !sprintState
                    player.isSprinting = sprintState
                }
                else -> player.isSprinting = true
            }
        }
    }

    private fun applyVisualNoise(player: EntityPlayerSP) {
        if (!visualNoise) return
        
        // FOV噪声
        if (fovNoise) {
            fovOffset = generateNoise() * fovVariation
            mc.gameSettings.fovSetting += fovOffset
        }
        
        // 相机抖动
        if (cameraShake) {
            cameraOffsetX = sin(cameraShakePhase.toDouble()).toFloat() * shakeIntensity
            cameraOffsetY = cos(cameraShakePhase * 1.3).toFloat() * shakeIntensity
        }
        
        // 头部摆动
        if (bobNoise) {
            val bobOffset = sin(bobPhase.toDouble()).toFloat() * bobAmount
            // 应用到玩家视图
        }
    }

    private fun applyAudioNoise(player: EntityPlayerSP) {
        if (!audioNoise) return
        
        // 音高噪声
        if (pitchNoise) {
            val pitchVariationValue = generateNoise() * pitchVariation
            // 应用到音频引擎
        }
        
        // 音量噪声
        if (volumeNoise) {
            val volumeVariationValue = generateNoise() * volumeVariation
            // 应用到音频引擎
        }
    }

    private fun applyNetworkNoise() {
        if (!networkNoise) return
        
        // 这里网络噪声主要通过修改数据包实现
    }

    private fun applyEnvironmentalNoise(world: net.minecraft.world.World) {
        if (!environmentalNoise) return
        
        // 天气效果
        if (weatherEffects) {
            // 模拟雨的效果
        }
        
        // 风力效果
        if (windEffects) {
            // 模拟风的效果
        }
    }

    private fun applyPhysiologicalNoise(player: EntityPlayerSP) {
        if (!physiologicalNoise) return
        
        // 手部震颤
        if (handTremor) {
            tremorState = generateNoise() * tremorIntensity
            // 应用到鼠标输入
        }
        
        // 眼疲劳
        if (eyeStrain) {
            eyeStrainLevel += strainRate
            eyeStrainLevel = eyeStrainLevel.coerceIn(0f, 1f)
        }
        
        // 眨眼效果
        if (blinkEffect && blinkState) {
            // 模拟眨眼，暂时降低可见性
        }
        
        // 应用疲劳
        if (fatigue && fatigueLevel > 0.5f) {
            // 降低反应速度
        }
        
        // 应用肾上腺素
        if (adrenaline && adrenalineLevel > 0) {
            // 提升性能
        }
    }

    private fun applyBehavioralNoise(player: EntityPlayerSP) {
        if (!behavioralNoise) return
        
        // 习惯模式
        if (habitPatterns) {
            // 基于习惯调整行为
        }
        
        // 学习效果
        if (learningEffect) {
            // 基于学习调整行为
        }
        
        // 决策延迟
        if (decisionLatency) {
            val latency = if (delayDistribution == "Normal") {
                (gaussianNoise() * reactionStdDev + reactionMean).toInt()
            } else {
                RandomUtils.nextInt(0, latencyMean * 2)
            }
            
            if (latency > 0) {
                Thread.sleep(latency.toLong())
            }
        }
        
        // 信心水平
        if (confidenceLevel && confidence < confidenceThreshold) {
            // 降低攻击频率
        }
    }

    private fun applyPacketNoise(event: PacketEvent) {
        if (!networkNoise) return
        
        val packet = event.packet
        
        // 数据包丢失
        if (packetLoss && RandomUtils.nextFloat() < lossProbability) {
            event.cancelEvent()
            return
        }
        
        // 数据包延迟
        if (packetDelay) {
            val delay = when (delayDistributionType) {
                "Constant" -> packetDelayMean
                "Normal" -> (gaussianNoise() * 20 + packetDelayMean).toInt()
                "Exponential" -> (-ln(1 - RandomUtils.nextFloat()) * packetDelayMean).toInt()
                else -> 0
            }.coerceAtLeast(0)
            
            if (delay > 0) {
                Thread.sleep(delay.toLong())
            }
        }
        
        // 数据包抖动
        if (packetJitter) {
            val jitter = RandomUtils.nextInt(-jitterAmountMs, jitterAmountMs + 1)
            Thread.sleep(jitter.toLong())
        }
        
        // 数据包重排序
        if (packetReorder && RandomUtils.nextFloat() < reorderProbability) {
            // 模拟重排序，延迟发送
            Thread.sleep(50)
        }
        
        // 数据包重复
        if (packetDuplication && RandomUtils.nextFloat() < duplicationProbability) {
            // 发送重复数据包
            sendPacket(packet)
        }
    }

    private fun renderVisualNoise(player: EntityPlayerSP) {
        // 渲染视觉噪声效果
        if (cameraShake) {
            // 应用相机偏移
        }
        
        if (blurNoise) {
            // 应用模糊效果
        }
        
        if (chromaticAberration) {
            // 应用色差效果
        }
        
        if (blinkEffect && blinkState) {
            // 应用眨眼效果
        }
    }

    private fun applyNoiseToValue(value: Float, intensity: Float = noiseIntensity): Float {
        if (!baseNoise) return value
        
        val noise = generateNoise() * intensity
        return value * (1 + noise)
    }

    private fun isValidTarget(entity: Entity, player: EntityPlayerSP): Boolean {
        if (entity === player) return false
        if (!isSelected(entity, true)) return false
        if (ignoreInvisible && entity.isInvisible) return false
        
        return true
    }

    private fun checkAngleCondition(player: EntityPlayerSP, entity: Entity): Boolean {
        val angle = player.getAngleToEntity(entity)
        val maxAngleValue = if (randomAngle) RandomUtils.nextFloat(minAngle, maxAngleRandom) else maxAngle
        return angle <= maxAngleValue
    }

    private fun EntityPlayerSP.getAngleToEntity(entity: Entity): Float {
        val diffX = entity.posX - this.posX
        val diffZ = entity.posZ - this.posZ
        val yaw = Math.toDegrees(Math.atan2(diffZ, diffX).toDouble()) - 90.0
        var angleDiff = (this.rotationYaw - yaw).toFloat() % 360.0f
        if (angleDiff < -180f) angleDiff += 360f
        if (angleDiff > 180f) angleDiff -= 360f
        return abs(angleDiff)
    }

    private fun EntityPlayerSP.horizontalMoveSpeed(): Float {
        return sqrt((motionX * motionX + motionZ * motionZ).toDouble()).toFloat()
    }

    override fun onDisable() {
        // 重置所有噪声状态
        resetNoiseState()
    }
}