//Code By NekoAi
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.block.*
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.Packet
import net.minecraft.network.play.client.*
import net.minecraft.network.play.server.*
import net.minecraft.util.*
import java.util.*
import kotlin.math.*
import kotlin.random.Random

object UltimateVelocity : Module("UltimateVelocity", Category.COMBAT) {

    // ==================== 合并的基础控制参数 (200个) ====================
    val enableHorizontalReduction by boolean("HorizontalReduction", true)
    val enableVerticalReduction by boolean("VerticalReduction", true)
    val enableMotionModification by boolean("MotionModification", true)
    val enablePacketCancellation by boolean("PacketCancellation", false)
    val enableExplosionHandling by boolean("ExplosionHandling", true)
    val enableHurtTimeCheck by boolean("HurtTimeCheck", true)
    val enableGroundCheck by boolean("GroundCheck", false)
    val enableAirCheck by boolean("AirCheck", false)
    val enableSprintCheck by boolean("SprintCheck", false)
    val enableSneakCheck by boolean("SneakCheck", false)
    val enableJumpCheck by boolean("JumpCheck", false)
    val enableFallDistanceCheck by boolean("FallDistanceCheck", false)
    val enableLiquidCheck by boolean("LiquidCheck", true)
    val enableWebCheck by boolean("WebCheck", true)
    val enableLadderCheck by boolean("LadderCheck", true)
    val enableVehicleCheck by boolean("VehicleCheck", true)
    val enableBlockCheck by boolean("BlockCheck", false)
    val enableEntityCheck by boolean("EntityCheck", false)
    val enableDistanceCheck by boolean("DistanceCheck", false)
    val enableAngleCheck by boolean("AngleCheck", false)
    val enableTimerModification by boolean("TimerModification", false)
    val enableStrafeModification by boolean("StrafeModification", false)
    val enablePositionModification by boolean("PositionModification", false)
    val enableRotationModification by boolean("RotationModification", false)
    val enableAttackModification by boolean("AttackModification", false)
    val enableSprintModification by boolean("SprintModification", false)
    val enableSneakModification by boolean("SneakModification", false)
    val enableJumpModification by boolean("JumpModification", false)
    val enableItemUseModification by boolean("ItemUseModification", false)
    val enableBlockingModification by boolean("BlockingModification", false)
    val enableElytraModification by boolean("ElytraModification", false)
    val enableFireCheck by boolean("FireCheck", false)
    val enableWaterCheck by boolean("WaterCheck", false)
    val enableLavaCheck by boolean("LavaCheck", false)
    val enablePortalCheck by boolean("PortalCheck", false)
    val enableBedCheck by boolean("BedCheck", false)
    val enableAnvilCheck by boolean("AnvilCheck", false)
    val enableCactusCheck by boolean("CactusCheck", false)
    val enableCobwebCheck by boolean("CobwebCheck", false)
    val enableIceCheck by boolean("IceCheck", false)
    val enableSlimeCheck by boolean("SlimeCheck", false)
    val enableSoulSandCheck by boolean("SoulSandCheck", false)
    val enableHoneyCheck by boolean("HoneyCheck", false)
    val enableBerryCheck by boolean("BerryCheck", false)
    val enablePowderSnowCheck by boolean("PowderSnowCheck", false)
    val enableScaffoldCheck by boolean("ScaffoldCheck", false)
    val enableFlyCheck by boolean("FlyCheck", false)
    val enableJesusCheck by boolean("JesusCheck", false)
    val enablePhaseCheck by boolean("PhaseCheck", false)
    val enableNoFallCheck by boolean("NoFallCheck", false)

    // 数据包延迟控制参数
    val enablePacketDelay by boolean("EnablePacketDelay", false)
    val delayS12Packet by boolean("DelayS12Packet", false)
    val delayS27Packet by boolean("DelayS27Packet", false)
    val delayS32Packet by boolean("DelayS32Packet", false)
    val delayS08Packet by boolean("DelayS08Packet", false)
    val delayS0FPacket by boolean("DelayS0FPacket", false)
    val delayS18Packet by boolean("DelayS18Packet", false)
    val delayS19Packet by boolean("DelayS19Packet", false)
    val delayS1FPacket by boolean("DelayS1FPacket", false)
    val delayS29Packet by boolean("DelayS29Packet", false)
    val delayS2CPacket by boolean("DelayS2CPacket", false)
    val delayS2EPacket by boolean("DelayS2EPacket", false)
    val delayS30Packet by boolean("DelayS30Packet", false)
    val delayS3APacket by boolean("DelayS3APacket", false)
    val delayS3BPacket by boolean("DelayS3BPacket", false)
    val delayS3FPacket by boolean("DelayS3FPacket", false)
    val delayC00Packet by boolean("DelayC00Packet", false)
    val delayC01Packet by boolean("DelayC01Packet", false)
    val delayC02Packet by boolean("DelayC02Packet", false)
    val delayC03Packet by boolean("DelayC03Packet", false)
    val delayC04Packet by boolean("DelayC04Packet", false)
    val delayC05Packet by boolean("DelayC05Packet", false)
    val delayC06Packet by boolean("DelayC06Packet", false)
    val delayC07Packet by boolean("DelayC07Packet", false)
    val delayC08Packet by boolean("DelayC08Packet", false)
    val delayC09Packet by boolean("DelayC09Packet", false)
    val delayC0APacket by boolean("DelayC0APacket", false)
    val delayC0BPacket by boolean("DelayC0BPacket", false)
    val delayC0CPacket by boolean("DelayC0CPacket", false)
    val delayC0DPacket by boolean("DelayC0DPacket", false)
    val delayC0EPacket by boolean("DelayC0EPacket", false)
    val delayC0FPacket by boolean("DelayC0FPacket", false)
    val enablePacketReceiveDelay by boolean("EnablePacketReceiveDelay", false)
    val enablePacketSendDelay by boolean("EnablePacketSendDelay", false)
    
    // 数据包拦截控制参数
    val enablePacketInterception by boolean("EnablePacketInterception", false)
    val interceptS12Packet by boolean("InterceptS12Packet", false)
    val interceptS27Packet by boolean("InterceptS27Packet", false)
    val interceptS32Packet by boolean("InterceptS32Packet", false)
    val interceptS08Packet by boolean("InterceptS08Packet", false)
    val interceptS0FPacket by boolean("InterceptS0FPacket", false)
    val interceptS18Packet by boolean("InterceptS18Packet", false)
    val interceptS19Packet by boolean("InterceptS19Packet", false)
    val interceptS1FPacket by boolean("InterceptS1FPacket", false)
    val interceptS29Packet by boolean("InterceptS29Packet", false)
    val interceptS2CPacket by boolean("InterceptS2CPacket", false)
    val interceptS2EPacket by boolean("InterceptS2EPacket", false)
    val interceptS30Packet by boolean("InterceptS30Packet", false)
    val interceptS3APacket by boolean("InterceptS3APacket", false)
    val interceptS3BPacket by boolean("InterceptS3BPacket", false)
    val interceptS3FPacket by boolean("InterceptS3FPacket", false)
    
    // 数据包修改控制参数
    val enablePacketModification by boolean("EnablePacketModification", false)
    val modifyS12Packet by boolean("ModifyS12Packet", false)
    val modifyS27Packet by boolean("ModifyS27Packet", false)
    val modifyS32Packet by boolean("ModifyS32Packet", false)
    val modifyS08Packet by boolean("ModifyS08Packet", false)
    val modifyS0FPacket by boolean("ModifyS0FPacket", false)
    val modifyS18Packet by boolean("ModifyS18Packet", false)
    val modifyS19Packet by boolean("ModifyS19Packet", false)
    val modifyS1FPacket by boolean("ModifyS1FPacket", false)
    val modifyS29Packet by boolean("ModifyS29Packet", false)
    val modifyS2CPacket by boolean("ModifyS2CPacket", false)
    val modifyS2EPacket by boolean("ModifyS2EPacket", false)
    val modifyS30Packet by boolean("ModifyS30Packet", false)
    val modifyS3APacket by boolean("ModifyS3APacket", false)
    val modifyS3BPacket by boolean("ModifyS3BPacket", false)
    val modifyS3FPacket by boolean("ModifyS3FPacket", false)
    
    // 数据包重发控制参数
    val enablePacketResend by boolean("EnablePacketResend", false)
    val resendS12Packet by boolean("ResendS12Packet", false)
    val resendS27Packet by boolean("ResendS27Packet", false)
    val resendS32Packet by boolean("ResendS32Packet", false)
    val resendCount by int("ResendCount", 1, 1..10)
    val resendDelay by int("ResendDelay", 0, 0..1000)
    val resendRandomDelay by boolean("ResendRandomDelay", false)
    val resendRandomCount by boolean("ResendRandomCount", false)
    
    // 网络流量控制参数
    val enableTrafficControl by boolean("EnableTrafficControl", false)
    val limitPacketRate by boolean("LimitPacketRate", false)
    val maxPacketsPerSecond by int("MaxPacketsPerSecond", 1000, 1..10000)
    val limitPacketSize by boolean("LimitPacketSize", false)
    val maxPacketSize by int("MaxPacketSize", 65536, 1..65536)
    val simulatePacketLoss by boolean("SimulatePacketLoss", false)
    val packetLossRate by float("PacketLossRate", 0.0f, 0.0f..1.0f)
    val simulateNetworkLag by boolean("SimulateNetworkLag", false)
    val networkLagMin by int("NetworkLagMin", 0, 0..1000)
    val networkLagMax by int("NetworkLagMax", 100, 0..1000)
    val simulateJitter by boolean("SimulateJitter", false)
    val jitterAmount by int("JitterAmount", 10, 0..100)
    val throttleUpload by boolean("ThrottleUpload", false)
    val uploadLimit by int("UploadLimit", 1024, 1..65536)
    val throttleDownload by boolean("ThrottleDownload", false)
    val downloadLimit by int("DownloadLimit", 1024, 1..65536)
    val compressPackets by boolean("CompressPackets", false)
    val encryptionEnabled by boolean("EncryptionEnabled", false)

    // ==================== 合并的数值调节参数 (300个) ====================
    val horizontalMultiplier by float("HorizontalMultiplier", 0.0f, -5.0f..5.0f)
    val verticalMultiplier by float("VerticalMultiplier", 0.0f, -5.0f..5.0f)
    val motionXMultiplier by float("MotionXMultiplier", 1.0f, -5.0f..5.0f)
    val motionYMultiplier by float("MotionYMultiplier", 1.0f, -5.0f..5.0f)
    val motionZMultiplier by float("MotionZMultiplier", 1.0f, -5.0f..5.0f)
    val motionXAddition by float("MotionXAddition", 0.0f, -2.0f..2.0f)
    val motionYAddition by float("MotionYAddition", 0.0f, -2.0f..2.0f)
    val motionZAddition by float("MotionZAddition", 0.0f, -2.0f..2.0f)
    val motionXExponent by float("MotionXExponent", 1.0f, 0.0f..3.0f)
    val motionYExponent by float("MotionYExponent", 1.0f, 0.0f..3.0f)
    val motionZExponent by float("MotionZExponent", 1.0f, 0.0f..3.0f)
    val speedMultiplier by float("SpeedMultiplier", 1.0f, 0.0f..5.0f)
    val speedAddition by float("SpeedAddition", 0.0f, -2.0f..2.0f)
    val jumpMultiplier by float("JumpMultiplier", 1.0f, 0.0f..5.0f)
    val jumpAddition by float("JumpAddition", 0.0f, -2.0f..2.0f)
    val fallMultiplier by float("FallMultiplier", 1.0f, 0.0f..5.0f)
    val fallAddition by float("FallAddition", 0.0f, -2.0f..2.0f)
    val gravityMultiplier by float("GravityMultiplier", 1.0f, 0.0f..5.0f)
    val gravityAddition by float("GravityAddition", 0.0f, -0.2f..0.2f)
    val airFrictionMultiplier by float("AirFrictionMultiplier", 1.0f, 0.0f..5.0f)
    
    // 新增的疾跑和潜行参数
    val sprintSpeedMultiplier by float("SprintMultiplier", 1.0f, 0.0f..5.0f)
    val sneakSpeedMultiplier by float("SneakMultiplier", 1.0f, 0.0f..5.0f)
    
    val reverseStrength by float("ReverseStrength", 0.0f, -2.0f..2.0f)
    val reverseXStrength by float("ReverseXStrength", 0.0f, -2.0f..2.0f)
    val reverseYStrength by float("ReverseYStrength", 0.0f, -2.0f..2.0f)
    val reverseZStrength by float("ReverseZStrength", 0.0f, -2.0f..2.0f)
    val smoothAmount by float("SmoothAmount", 0.5f, 0.0f..1.0f)
    val smoothXAmount by float("SmoothXAmount", 0.5f, 0.0f..1.0f)
    val smoothYAmount by float("SmoothYAmount", 0.5f, 0.0f..1.0f)
    val smoothZAmount by float("SmoothZAmount", 0.5f, 0.0f..1.0f)
    val delayTime by float("DelayTime", 0.0f, 0.0f..1000.0f)
    val delayXTime by float("DelayXTime", 0.0f, 0.0f..1000.0f)
    val delayYTime by float("DelayYTime", 0.0f, 0.0f..1000.0f)
    val delayZTime by float("DelayZTime", 0.0f, 0.0f..1000.0f)
    val randomMultiplier by float("RandomMultiplier", 0.0f, 0.0f..1.0f)
    val randomXMultiplier by float("RandomXMultiplier", 0.0f, 0.0f..1.0f)
    val randomYMultiplier by float("RandomYMultiplier", 0.0f, 0.0f..1.0f)
    val randomZMultiplier by float("RandomZMultiplier", 0.0f, 0.0f..1.0f)
    val sinMultiplier by float("SinMultiplier", 0.0f, 0.0f..5.0f)
    val cosMultiplier by float("CosMultiplier", 0.0f, 0.0f..5.0f)
    val tanMultiplier by float("TanMultiplier", 0.0f, 0.0f..5.0f)
    val logMultiplier by float("LogMultiplier", 0.0f, 0.0f..5.0f)
    val expMultiplier by float("ExpMultiplier", 0.0f, 0.0f..5.0f)
    val sqrtMultiplier by float("SqrtMultiplier", 0.0f, 0.0f..5.0f)
    val powMultiplier by float("PowMultiplier", 1.0f, 0.0f..5.0f)
    val absMultiplier by float("AbsMultiplier", 1.0f, 0.0f..5.0f)
    val floorMultiplier by float("FloorMultiplier", 1.0f, 0.0f..5.0f)
    val ceilMultiplier by float("CeilMultiplier", 1.0f, 0.0f..5.0f)
    val roundMultiplier by float("RoundMultiplier", 1.0f, 0.0f..5.0f)
    
    val minHorizontal by float("MinHorizontal", -2.0f, -5.0f..5.0f)
    val maxHorizontal by float("MaxHorizontal", 2.0f, -5.0f..5.0f)
    val minVertical by float("MinVertical", -2.0f, -5.0f..5.0f)
    val maxVertical by float("MaxVertical", 2.0f, -5.0f..5.0f)
    val minMotionX by float("MinMotionX", -2.0f, -5.0f..5.0f)
    val maxMotionX by float("MaxMotionX", 2.0f, -5.0f..5.0f)
    val minMotionY by float("MinMotionY", -2.0f, -5.0f..5.0f)
    val maxMotionY by float("MaxMotionY", 2.0f, -5.0f..5.0f)
    val minMotionZ by float("MinMotionZ", -2.0f, -5.0f..5.0f)
    val maxMotionZ by float("MaxMotionZ", 2.0f, -5.0f..5.0f)
    val minSpeed by float("MinSpeed", 0.0f, 0.0f..2.0f)
    val maxSpeed by float("MaxSpeed", 2.0f, 0.0f..5.0f)
    val minJump by float("MinJump", 0.0f, -1.0f..2.0f)
    val maxJump by float("MaxJump", 2.0f, 0.0f..5.0f)
    val minFall by float("MinFall", 0.0f, -2.0f..2.0f)
    val maxFall by float("MaxFall", 2.0f, 0.0f..5.0f)
    
    val explosionHorizontal by float("ExplosionHorizontal", 0.0f, -5.0f..5.0f)
    val explosionVertical by float("ExplosionVertical", 0.0f, -5.0f..5.0f)
    val explosionMotionX by float("ExplosionMotionX", 1.0f, -5.0f..5.0f)
    val explosionMotionY by float("ExplosionMotionY", 1.0f, -5.0f..5.0f)
    val explosionMotionZ by float("ExplosionMotionZ", 1.0f, -5.0f..5.0f)
    val explosionSpeed by float("ExplosionSpeed", 1.0f, 0.0f..5.0f)
    val explosionJump by float("ExplosionJump", 1.0f, 0.0f..5.0f)
    val explosionFall by float("ExplosionFall", 1.0f, 0.0f..5.0f)
    
    val timerSpeed by float("TimerSpeed", 1.0f, 0.1f..10.0f)
    val timerXSpeed by float("TimerXSpeed", 1.0f, 0.1f..10.0f)
    val timerYSpeed by float("TimerYSpeed", 1.0f, 0.1f..10.0f)
    val timerZSpeed by float("TimerZSpeed", 1.0f, 0.1f..10.0f)
    val strafeSpeed by float("StrafeSpeed", 1.0f, 0.0f..5.0f)
    val strafeXSpeed by float("StrafeXSpeed", 1.0f, 0.0f..5.0f)
    val strafeZSpeed by float("StrafeZSpeed", 1.0f, 0.0f..5.0f)
    
    // 数据包延迟数值参数
    val s12PacketDelay by int("S12PacketDelay", 0, 0..10000)
    val s27PacketDelay by int("S27PacketDelay", 0, 0..10000)
    val s32PacketDelay by int("S32PacketDelay", 0, 0..10000)
    val c03PacketDelay by int("C03PacketDelay", 0, 0..10000)
    val receiveDelayBase by int("ReceiveDelayBase", 0, 0..10000)
    val sendDelayBase by int("SendDelayBase", 0, 0..10000)
    val delayRandomness by float("DelayRandomness", 0.0f, 0.0f..1.0f)
    
    // 数据包修改数值参数
    val s12MotionXMultiplier by float("S12MotionXMultiplier", 1.0f, 0.0f..10.0f)
    val s12MotionYMultiplier by float("S12MotionYMultiplier", 1.0f, 0.0f..10.0f)
    val s12MotionZMultiplier by float("S12MotionZMultiplier", 1.0f, 0.0f..10.0f)
    val s12EntityIdOffset by int("S12EntityIdOffset", 0, -1000..1000)
    val s27ExplosionXMultiplier by float("S27ExplosionXMultiplier", 1.0f, 0.0f..10.0f)
    val s27ExplosionYMultiplier by float("S27ExplosionYMultiplier", 1.0f, 0.0f..10.0f)
    val s27ExplosionZMultiplier by float("S27ExplosionZMultiplier", 1.0f, 0.0f..10.0f)
    
    // 网络流量控制数值参数
    val packetLossRateRandomMin by float("PacketLossRateRandomMin", 0.0f, 0.0f..1.0f)
    val packetLossRateRandomMax by float("PacketLossRateRandomMax", 0.1f, 0.0f..1.0f)
    val networkLagRandomMin by int("NetworkLagRandomMin", 0, 0..1000)
    val networkLagRandomMax by int("NetworkLagRandomMax", 100, 0..1000)
    val jitterRandomMin by int("JitterRandomMin", 0, 0..100)
    val jitterRandomMax by int("JitterRandomMax", 10, 0..100)
    
    // 随机化控制数值参数
    val randomizeHorizontalMultiplier by boolean("RandomizeHorizontalMultiplier", false)
    val horizontalMultiplierRandomMin by float("HorizontalMultiplierRandomMin", 0.0f, -5.0f..5.0f)
    val horizontalMultiplierRandomMax by float("HorizontalMultiplierRandomMax", 1.0f, -5.0f..5.0f)
    val randomizeVerticalMultiplier by boolean("RandomizeVerticalMultiplier", false)
    val verticalMultiplierRandomMin by float("VerticalMultiplierRandomMin", 0.0f, -5.0f..5.0f)
    val verticalMultiplierRandomMax by float("VerticalMultiplierRandomMax", 1.0f, -5.0f..5.0f)
    val randomizeMotionXMultiplier by boolean("RandomizeMotionXMultiplier", false)
    val motionXMultiplierRandomMin by float("MotionXMultiplierRandomMin", 0.5f, -5.0f..5.0f)
    val motionXMultiplierRandomMax by float("MotionXMultiplierRandomMax", 1.5f, -5.0f..5.0f)
    val randomizeMotionYMultiplier by boolean("RandomizeMotionYMultiplier", false)
    val motionYMultiplierRandomMin by float("MotionYMultiplierRandomMin", 0.5f, -5.0f..5.0f)
    val motionYMultiplierRandomMax by float("MotionYMultiplierRandomMax", 1.5f, -5.0f..5.0f)
    val randomizeMotionZMultiplier by boolean("RandomizeMotionZMultiplier", false)
    val motionZMultiplierRandomMin by float("MotionZMultiplierRandomMin", 0.5f, -5.0f..5.0f)
    val motionZMultiplierRandomMax by float("MotionZMultiplierRandomMax", 1.5f, -5.0f..5.0f)
    val randomizeDelayTime by boolean("RandomizeDelayTime", false)
    val delayTimeRandomMin by float("DelayTimeRandomMin", 0.0f, 0.0f..1000.0f)
    val delayTimeRandomMax by float("DelayTimeRandomMax", 100.0f, 0.0f..1000.0f)
    val randomizeReverseStrength by boolean("RandomizeReverseStrength", false)
    val reverseStrengthRandomMin by float("ReverseStrengthRandomMin", 0.0f, -2.0f..2.0f)
    val reverseStrengthRandomMax by float("ReverseStrengthRandomMax", 1.0f, -2.0f..2.0f)
    val randomizeSmoothAmount by boolean("RandomizeSmoothAmount", false)
    val smoothAmountRandomMin by float("SmoothAmountRandomMin", 0.1f, 0.0f..1.0f)
    val smoothAmountRandomMax by float("SmoothAmountRandomMax", 0.9f, 0.0f..1.0f)
    val randomizeTimerSpeed by boolean("RandomizeTimerSpeed", false)
    val timerSpeedRandomMin by float("TimerSpeedRandomMin", 0.5f, 0.1f..10.0f)
    val timerSpeedRandomMax by float("TimerSpeedRandomMax", 2.0f, 0.1f..10.0f)
    val randomizeChance by boolean("RandomizeChance", false)
    val chanceRandomMin by int("ChanceRandomMin", 50, 0..100)
    val chanceRandomMax by int("ChanceRandomMax", 100, 0..100)
    val randomizeAllDelays by boolean("RandomizeAllDelays", false)
    val randomizeAllMultipliers by boolean("RandomizeAllMultipliers", false)
    val randomizeAllOffsets by boolean("RandomizeAllOffsets", false)
    val randomizeAllScales by boolean("RandomizeAllScales", false)
    val randomizeAllLimits by boolean("RandomizeAllLimits", false)
    val randomUpdateInterval by int("RandomUpdateInterval", 100, 1..1000)
    val enableDynamicRandomization by boolean("EnableDynamicRandomization", false)
    val dynamicRandomizationRate by float("DynamicRandomizationRate", 0.1f, 0.0f..1.0f)

    // ==================== 合并的整数参数 (200个) ====================
    val hurtTimeMin by int("HurtTimeMin", 0, 0..10)
    val hurtTimeMax by int("HurtTimeMax", 10, 0..10)
    val hurtResistantTimeMin by int("HurtResistantTimeMin", 0, 0..20)
    val hurtResistantTimeMax by int("HurtResistantTimeMax", 20, 0..20)
    val ticksExistedMin by int("TicksExistedMin", 0, 0..1000)
    val ticksExistedMax by int("TicksExistedMax", 1000, 0..1000)
    val ageMin by int("AgeMin", 0, 0..1000)
    val ageMax by int("AgeMax", 1000, 0..1000)
    val fireMin by int("FireMin", 0, 0..1000)
    val fireMax by int("FireMax", 1000, 0..1000)
    val airMin by int("AirMin", 0, 0..300)
    val airMax by int("AirMax", 300, 0..300)
    
    val delayTicks by int("DelayTicks", 0, 0..100)
    val delayXTicks by int("DelayXTicks", 0, 0..100)
    val delayYTicks by int("DelayYTicks", 0, 0..100)
    val delayZTicks by int("DelayZTicks", 0, 0..100)
    val smoothTicks by int("SmoothTicks", 10, 1..100)
    val smoothXTicks by int("SmoothXTicks", 10, 1..100)
    val smoothYTicks by int("SmoothYTicks", 10, 1..100)
    val smoothZTicks by int("SmoothZTicks", 10, 1..100)
    val randomTicks by int("RandomTicks", 1, 1..100)
    val randomXTicks by int("RandomXTicks", 1, 1..100)
    val randomYTicks by int("RandomYTicks", 1, 1..100)
    val randomZTicks by int("RandomZTicks", 1, 1..100)
    val sinTicks by int("SinTicks", 20, 1..100)
    val cosTicks by int("CosTicks", 20, 1..100)
    val tanTicks by int("TanTicks", 20, 1..100)
    
    val timerTicks by int("TimerTicks", 0, 0..100)
    val timerXTicks by int("TimerXTicks", 0, 0..100)
    val timerYTicks by int("TimerYTicks", 0, 0..100)
    val timerZTicks by int("TimerZTicks", 0, 0..100)
    val strafeTicks by int("StrafeTicks", 0, 0..100)
    val strafeXTicks by int("StrafeXTicks", 0, 0..100)
    val strafeZTicks by int("StrafeZTicks", 0, 0..100)
    val positionTicks by int("PositionTicks", 0, 0..100)
    val rotationTicks by int("RotationTicks", 0, 0..100)
    val attackTicks by int("AttackTicks", 0, 0..100)
    val sprintTicks by int("SprintTicks", 0, 0..100)
    val sneakTicks by int("SneakTicks", 0, 0..100)
    val jumpTicks by int("JumpTicks", 0, 0..100)
    
    val distanceMin by int("DistanceMin", 0, 0..100)
    val distanceMax by int("DistanceMax", 100, 0..100)
    val angleMin by int("AngleMin", 0, 0..360)
    val angleMax by int("AngleMax", 360, 0..360)
    val yawMin by int("YawMin", -180, -180..180)
    val yawMax by int("YawMax", 180, -180..180)
    val pitchMin by int("PitchMin", -90, -90..90)
    val pitchMax by int("PitchMax", 90, -90..90)
    
    val packetDelay by int("PacketDelay", 0, 0..1000)
    val packetXDelay by int("PacketXDelay", 0, 0..1000)
    val packetYDelay by int("PacketYDelay", 0, 0..1000)
    val packetZDelay by int("PacketZDelay", 0, 0..1000)
    val explosionDelay by int("ExplosionDelay", 0, 0..1000)
    
    val chance by int("Chance", 100, 0..100)
    val xChance by int("XChance", 100, 0..100)
    val yChance by int("YChance", 100, 0..100)
    val zChance by int("ZChance", 100, 0..100)
    val speedChance by int("SpeedChance", 100, 0..100)
    val jumpChance by int("JumpChance", 100, 0..100)
    val fallChance by int("FallChance", 100, 0..100)

    // ==================== 合并的布尔参数 (250个) ====================
    val randomEnabled by boolean("RandomEnabled", false)
    val sinEnabled by boolean("SinEnabled", false)
    val cosEnabled by boolean("CosEnabled", false)
    val tanEnabled by boolean("TanEnabled", false)
    val logEnabled by boolean("LogEnabled", false)
    val expEnabled by boolean("ExpEnabled", false)
    val sqrtEnabled by boolean("SqrtEnabled", false)
    val powEnabled by boolean("PowEnabled", false)
    val absEnabled by boolean("AbsEnabled", false)
    val floorEnabled by boolean("FloorEnabled", false)
    val ceilEnabled by boolean("CeilEnabled", false)
    val roundEnabled by boolean("RoundEnabled", false)
    val clampEnabled by boolean("ClampEnabled", false)
    val lerpEnabled by boolean("LerpEnabled", false)
    val slerpEnabled by boolean("SlerpEnabled", false)
    val normalizeEnabled by boolean("NormalizeEnabled", false)
    
    val timerXEnabled by boolean("TimerXEnabled", false)
    val timerYEnabled by boolean("TimerYEnabled", false)
    val timerZEnabled by boolean("TimerZEnabled", false)
    val strafeXEnabled by boolean("StrafeXEnabled", false)
    val strafeZEnabled by boolean("StrafeZEnabled", false)
    val positionXEnabled by boolean("PositionXEnabled", false)
    val positionYEnabled by boolean("PositionYEnabled", false)
    val positionZEnabled by boolean("PositionZEnabled", false)
    val rotationYawEnabled by boolean("RotationYawEnabled", false)
    val rotationPitchEnabled by boolean("RotationPitchEnabled", false)
    val attackEnabled by boolean("AttackEnabled", false)
    val sprintEnabled by boolean("SprintEnabled", false)
    val sneakEnabled by boolean("SneakEnabled", false)
    val jumpEnabled by boolean("JumpEnabled", false)
    val itemUseEnabled by boolean("ItemUseEnabled", false)
    val blockingEnabled by boolean("BlockingEnabled", false)
    val elytraEnabled by boolean("ElytraEnabled", false)
    
    val packetXEnabled by boolean("PacketXEnabled", false)
    val packetYEnabled by boolean("PacketYEnabled", false)
    val packetZEnabled by boolean("PacketZEnabled", false)
    val explosionXEnabled by boolean("ExplosionXEnabled", false)
    val explosionYEnabled by boolean("ExplosionYEnabled", false)
    val explosionZEnabled by boolean("ExplosionZEnabled", false)
    val motionXEnabled by boolean("MotionXEnabled", true)
    val motionYEnabled by boolean("MotionYEnabled", true)
    val motionZEnabled by boolean("MotionZEnabled", true)
    val speedEnabled by boolean("SpeedEnabled", false)
    val jumpPowerEnabled by boolean("JumpPowerEnabled", false)
    val fallDistanceEnabled by boolean("FallDistanceEnabled", false)
    val gravityEnabled by boolean("GravityEnabled", false)
    val airFrictionEnabled by boolean("AirFrictionEnabled", false)
    
    val reverseXEnabled by boolean("ReverseXEnabled", false)
    val reverseYEnabled by boolean("ReverseYEnabled", false)
    val reverseZEnabled by boolean("ReverseZEnabled", false)
    val smoothXEnabled by boolean("SmoothXEnabled", false)
    val smoothYEnabled by boolean("SmoothYEnabled", false)
    val smoothZEnabled by boolean("SmoothZEnabled", false)
    val delayXEnabled by boolean("DelayXEnabled", false)
    val delayYEnabled by boolean("DelayYEnabled", false)
    val delayZEnabled by boolean("DelayZEnabled", false)
    val randomXEnabled by boolean("RandomXEnabled", false)
    val randomYEnabled by boolean("RandomYEnabled", false)
    val randomZEnabled by boolean("RandomZEnabled", false)
    
    val minMaxXEnabled by boolean("MinMaxXEnabled", false)
    val minMaxYEnabled by boolean("MinMaxYEnabled", false)
    val minMaxZEnabled by boolean("MinMaxZEnabled", false)
    val minMaxSpeedEnabled by boolean("MinMaxSpeedEnabled", false)
    val minMaxJumpEnabled by boolean("MinMaxJumpEnabled", false)
    val minMaxFallEnabled by boolean("MinMaxFallEnabled", false)
    
    val explosionMotionXEnabled by boolean("ExplosionMotionXEnabled", false)
    val explosionMotionYEnabled by boolean("ExplosionMotionYEnabled", false)
    val explosionMotionZEnabled by boolean("ExplosionMotionZEnabled", false)
    val explosionSpeedEnabled by boolean("ExplosionSpeedEnabled", false)
    val explosionJumpEnabled by boolean("ExplosionJumpEnabled", false)
    val explosionFallEnabled by boolean("ExplosionFallEnabled", false)
    
    val timerXSpeedEnabled by boolean("TimerXSpeedEnabled", false)
    val timerYSpeedEnabled by boolean("TimerYSpeedEnabled", false)
    val timerZSpeedEnabled by boolean("TimerZSpeedEnabled", false)
    val strafeXSpeedEnabled by boolean("StrafeXSpeedEnabled", false)
    val strafeZSpeedEnabled by boolean("StrafeZSpeedEnabled", false)
    
    val hurtTimeCheckEnabled by boolean("HurtTimeCheckEnabled", true)
    val hurtResistantTimeCheckEnabled by boolean("HurtResistantTimeCheckEnabled", false)
    val ticksExistedCheckEnabled by boolean("TicksExistedCheckEnabled", false)
    val ageCheckEnabled by boolean("AgeCheckEnabled", false)
    val fireCheckEnabled by boolean("FireCheckEnabled", false)
    val airCheckEnabled by boolean("AirCheckEnabled", false)
    
    val delayTicksEnabled by boolean("DelayTicksEnabled", false)
    val smoothTicksEnabled by boolean("SmoothTicksEnabled", false)
    val randomTicksEnabled by boolean("RandomTicksEnabled", false)
    val sinTicksEnabled by boolean("SinTicksEnabled", false)
    val cosTicksEnabled by boolean("CosTicksEnabled", false)
    val tanTicksEnabled by boolean("TanTicksEnabled", false)
    
    // 随机化布尔参数扩展
    val randomizeHorizontalReduction by boolean("RandomizeHorizontalReduction", false)
    val randomizeVerticalReduction by boolean("RandomizeVerticalReduction", false)
    val randomizeMotionModification by boolean("RandomizeMotionModification", false)
    val randomizePacketCancellation by boolean("RandomizePacketCancellation", false)
    val randomizeExplosionHandling by boolean("RandomizeExplosionHandling", false)
    val randomizeHurtTimeCheck by boolean("RandomizeHurtTimeCheck", false)
    val randomizeGroundCheck by boolean("RandomizeGroundCheck", false)
    val randomizeAirCheck by boolean("RandomizeAirCheck", false)
    val randomizeSprintCheck by boolean("RandomizeSprintCheck", false)
    val randomizeSneakCheck by boolean("RandomizeSneakCheck", false)
    val randomizeJumpCheck by boolean("RandomizeJumpCheck", false)
    val randomizeFallDistanceCheck by boolean("RandomizeFallDistanceCheck", false)
    val randomizeLiquidCheck by boolean("RandomizeLiquidCheck", false)
    val randomizeWebCheck by boolean("RandomizeWebCheck", false)
    val randomizeLadderCheck by boolean("RandomizeLadderCheck", false)
    val randomizeVehicleCheck by boolean("RandomizeVehicleCheck", false)
    val randomizeFireCheck by boolean("RandomizeFireCheck", false)
    val randomizeWaterCheck by boolean("RandomizeWaterCheck", false)
    val randomizeLavaCheck by boolean("RandomizeLavaCheck", false)
    
    val randomizeTimerModification by boolean("RandomizeTimerModification", false)
    val randomizeStrafeModification by boolean("RandomizeStrafeModification", false)
    val randomizeSprintModification by boolean("RandomizeSprintModification", false)
    val randomizeSneakModification by boolean("RandomizeSneakModification", false)
    val randomizeJumpModification by boolean("RandomizeJumpModification", false)
    
    val randomizePacketDelayEnabled by boolean("RandomizePacketDelayEnabled", false)
    val randomizePacketInterceptionEnabled by boolean("RandomizePacketInterceptionEnabled", false)
    val randomizePacketModificationEnabled by boolean("RandomizePacketModificationEnabled", false)
    val randomizePacketResendEnabled by boolean("RandomizePacketResendEnabled", false)
    val randomizeTrafficControlEnabled by boolean("RandomizeTrafficControlEnabled", false)
    val randomizeLimitPacketRate by boolean("RandomizeLimitPacketRate", false)
    val randomizeLimitPacketSize by boolean("RandomizeLimitPacketSize", false)
    val randomizeSimulatePacketLoss by boolean("RandomizeSimulatePacketLoss", false)
    val randomizeSimulateNetworkLag by boolean("RandomizeSimulateNetworkLag", false)
    val randomizeSimulateJitter by boolean("RandomizeSimulateJitter", false)
    val randomizeThrottleUpload by boolean("RandomizeThrottleUpload", false)
    val randomizeThrottleDownload by boolean("RandomizeThrottleDownload", false)
    
    val randomizeRandomEnabled by boolean("RandomizeRandomEnabled", false)
    val randomizeSinEnabled by boolean("RandomizeSinEnabled", false)
    val randomizeCosEnabled by boolean("RandomizeCosEnabled", false)
    val randomizeTanEnabled by boolean("RandomizeTanEnabled", false)
    val randomizeLogEnabled by boolean("RandomizeLogEnabled", false)
    val randomizeExpEnabled by boolean("RandomizeExpEnabled", false)
    val randomizeSqrtEnabled by boolean("RandomizeSqrtEnabled", false)
    val randomizePowEnabled by boolean("RandomizePowEnabled", false)
    val randomizeAbsEnabled by boolean("RandomizeAbsEnabled", false)
    val randomizeFloorEnabled by boolean("RandomizeFloorEnabled", false)
    val randomizeCeilEnabled by boolean("RandomizeCeilEnabled", false)
    val randomizeRoundEnabled by boolean("RandomizeRoundEnabled", false)

    // ==================== 合并的高级参数 (150个) ====================
    val advancedMathEnabled by boolean("AdvancedMathEnabled", false)
    val matrixTransformEnabled by boolean("MatrixTransformEnabled", false)
    val quaternionRotationEnabled by boolean("QuaternionRotationEnabled", false)
    val vectorProjectionEnabled by boolean("VectorProjectionEnabled", false)
    val physicsSimulationEnabled by boolean("PhysicsSimulationEnabled", false)
    val collisionDetectionEnabled by boolean("CollisionDetectionEnabled", false)
    val pathPredictionEnabled by boolean("PathPredictionEnabled", false)
    val machineLearningEnabled by boolean("MachineLearningEnabled", false)
    val neuralNetworkEnabled by boolean("NeuralNetworkEnabled", false)
    val geneticAlgorithmEnabled by boolean("GeneticAlgorithmEnabled", false)
    
    val adaptiveLearningRate by float("AdaptiveLearningRate", 0.01f, 0.0f..1.0f)
    val momentumFactor by float("MomentumFactor", 0.9f, 0.0f..1.0f)
    val decayRate by float("DecayRate", 0.001f, 0.0f..0.1f)
    val regularizationFactor by float("RegularizationFactor", 0.0001f, 0.0f..0.01f)
    val dropoutRate by float("DropoutRate", 0.5f, 0.0f..1.0f)
    val batchSize by int("BatchSize", 32, 1..256)
    val epochs by int("Epochs", 100, 1..1000)
    val hiddenLayers by int("HiddenLayers", 3, 1..10)
    val neuronsPerLayer by int("NeuronsPerLayer", 64, 1..512)
    val activationFunction by choices("ActivationFunction", arrayOf("ReLU", "Sigmoid", "Tanh", "LeakyReLU"), "ReLU")
    
    val populationSize by int("PopulationSize", 100, 10..1000)
    val generations by int("Generations", 50, 1..500)
    val mutationRate by float("MutationRate", 0.01f, 0.0f..0.1f)
    val crossoverRate by float("CrossoverRate", 0.7f, 0.0f..1.0f)
    val selectionPressure by float("SelectionPressure", 2.0f, 1.0f..10.0f)
    val elitismCount by int("ElitismCount", 2, 0..20)
    
    val frictionCoefficient by float("FrictionCoefficient", 0.6f, 0.0f..1.0f)
    val restitutionCoefficient by float("RestitutionCoefficient", 0.8f, 0.0f..1.0f)
    val dragCoefficient by float("DragCoefficient", 0.1f, 0.0f..1.0f)
    val airDensity by float("AirDensity", 1.2f, 0.0f..10.0f)
    val gravityConstant by float("GravityConstant", 9.8f, 0.0f..100.0f)
    val timeStep by float("TimeStep", 0.05f, 0.001f..0.1f)
    
    val kalmanGain by float("KalmanGain", 0.8f, 0.0f..1.0f)
    val processNoise by float("ProcessNoise", 0.1f, 0.0f..1.0f)
    val measurementNoise by float("MeasurementNoise", 0.1f, 0.0f..1.0f)
    val predictionHorizon by int("PredictionHorizon", 10, 1..100)
    val smoothingWindow by int("SmoothingWindow", 5, 1..50)
    
    val fourierSeriesTerms by int("FourierSeriesTerms", 10, 1..100)
    val waveletScale by float("WaveletScale", 1.0f, 0.1f..10.0f)
    val waveletShift by float("WaveletShift", 0.0f, -10.0f..10.0f)
    val convolutionKernelSize by int("ConvolutionKernelSize", 3, 1..11)
    val poolingSize by int("PoolingSize", 2, 1..5)
    
    val quantumSuperposition by boolean("QuantumSuperposition", false)
    val quantumEntanglement by boolean("QuantumEntanglement", false)
    val quantumTunneling by boolean("QuantumTunneling", false)
    val quantumInterference by boolean("QuantumInterference", false)
    val quantumMeasurement by boolean("QuantumMeasurement", false)
    val qubitCount by int("QubitCount", 2, 1..10)
    val quantumGateType by choices("QuantumGateType", arrayOf("Hadamard", "PauliX", "PauliY", "PauliZ", "CNOT"), "Hadamard")
    val quantumCircuitDepth by int("QuantumCircuitDepth", 3, 1..20)
    
    // 数据包处理高级参数
    val packetProcessingAlgorithm by choices("PacketProcessingAlgorithm", arrayOf("FIFO", "LIFO", "Priority", "RoundRobin", "WeightedFair"), "FIFO")
    val packetCompressionAlgorithm by choices("PacketCompressionAlgorithm", arrayOf("None", "Zlib", "Gzip", "LZ4", "Snappy", "Brotli"), "Zlib")
    val packetEncryptionAlgorithm by choices("PacketEncryptionAlgorithm", arrayOf("None", "AES", "RSA", "ECC", "ChaCha20", "Twofish"), "AES")
    val packetAuthenticationMethod by choices("PacketAuthenticationMethod", arrayOf("None", "HMAC", "DigitalSignature", "ZeroKnowledge", "Biometric"), "HMAC")
    val packetIntegrityCheck by choices("PacketIntegrityCheck", arrayOf("None", "CRC32", "MD5", "SHA1", "SHA256", "SHA512"), "CRC32")
    
    // 随机化高级参数
    val randomizationAlgorithm by choices("RandomizationAlgorithm", arrayOf("Uniform", "Normal", "Exponential", "Gamma", "Beta", "Poisson"), "Uniform")
    val randomDistributionType by choices("RandomDistributionType", arrayOf("Continuous", "Discrete", "Mixed", "Multivariate", "TimeSeries"), "Continuous")
    val randomNumberGenerator by choices("RandomNumberGenerator", arrayOf("LinearCongruential", "MersenneTwister", "Xorshift", "PCG", "Cryptographic"), "MersenneTwister")
    val randomSeedGeneration by choices("RandomSeedGeneration", arrayOf("Time", "Entropy", "Hardware", "Hybrid", "Deterministic"), "Time")
    val randomQualityMetric by choices("RandomQualityMetric", arrayOf("None", "Entropy", "ChiSquare", "Autocorrelation", "Spectral", "Complexity"), "None")
    val randomProcessType by choices("RandomProcessType", arrayOf("Stationary", "NonStationary", "Cyclostationary", "Markov", "Martingale"), "Stationary")
    val randomTimeSeriesModel by choices("RandomTimeSeriesModel", arrayOf("None", "AR", "MA", "ARMA", "ARIMA", "GARCH"), "None")
    val randomFilterDesign by choices("RandomFilterDesign", arrayOf("None", "FIR", "IIR", "Kalman", "Particle", "Adaptive"), "None")
    val randomSmoothingMethod by choices("RandomSmoothingMethod", arrayOf("None", "MovingAverage", "Exponential", "SavitzkyGolay", "Kalman", "LOESS"), "None")
    val randomInterpolationMethod by choices("RandomInterpolationMethod", arrayOf("None", "Linear", "Polynomial", "Spline", "RadialBasis", "Kriging"), "None")
    val randomPredictionMethod by choices("RandomPredictionMethod", arrayOf("None", "LinearRegression", "DecisionTree", "NeuralNetwork", "SVM", "Ensemble"), "None")
    val randomClassificationMethod by choices("RandomClassificationMethod", arrayOf("None", "LogisticRegression", "RandomForest", "NeuralNetwork", "SVM", "NaiveBayes"), "None")
    val randomOptimizationAlgorithm by choices("RandomOptimizationAlgorithm", arrayOf("None", "GradientDescent", "Genetic", "ParticleSwarm", "SimulatedAnnealing", "Bayesian"), "None")
    val randomSearchStrategy by choices("RandomSearchStrategy", arrayOf("None", "Grid", "Random", "Bayesian", "Hyperband", "BOHB"), "None")

    // ==================== 状态变量 ====================
    private var velocityReceived = false
    private val velocityTimer = MSTimer()
    private val motionHistory = mutableListOf<Triple<Double, Double, Double>>()
    private var tickCounter = 0
    private val randomState = Random(System.currentTimeMillis())
    private val neuralNetworkWeights = mutableListOf<MutableList<Float>>()
    private val geneticPopulation = mutableListOf<MutableList<Float>>()
    private val physicsState = PhysicsState()
    private val kalmanFilter = KalmanFilter()
    private val quantumState = QuantumState()
    
    // 数据包处理状态变量
    private val packetQueue = mutableListOf<Packet<*>>()
    private val packetDelayMap = mutableMapOf<Class<*>, Long>()
    private val packetStatistics = mutableMapOf<String, PacketStats>()
    private val randomGenerators = mutableMapOf<String, Random>()
    private val dynamicParameters = mutableMapOf<String, Float>()
    private val parameterHistory = mutableMapOf<String, MutableList<Float>>()
    
    // 计时器
    private val updateTimer = MSTimer()
    private val randomUpdateTimer = MSTimer()
    private val statisticsTimer = MSTimer()
    
    // 计数器
    private var totalPacketsProcessed = 0L
    private var totalPacketsDelayed = 0L
    private var totalPacketsModified = 0L
    private var totalPacketsResent = 0L
    private var totalPacketsLost = 0L
    private var randomizationCycle = 0

    // ==================== 事件处理器 ====================
    val onPacket = handler<PacketEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        val packet = event.packet
        
        // 更新数据包统计
        updatePacketStatistics(packet)
        
        // 应用数据包处理
        applyPacketProcessing(event, packet)
        
        // 击退处理逻辑
        if (!shouldProcess(thePlayer)) return@handler
        
        when {
            packet is S12PacketEntityVelocity && packet.entityID == thePlayer.entityId -> {
                handleVelocityPacket(event, packet)
            }
            
            packet is S27PacketExplosion && enableExplosionHandling -> {
                handleExplosionPacket(event, packet)
            }
            
            else -> handleOtherPackets(event, packet)
        }
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (!shouldProcess(thePlayer)) return@handler
        
        tickCounter++
        
        // 更新动态随机化
        if (enableDynamicRandomization) {
            updateDynamicRandomization()
        }
        
        // 更新参数历史
        updateParameterHistory()
        
        // 处理数据包队列
        processPacketQueue()
        
        // 原有更新逻辑
        updateMotionHistory(thePlayer)
        applyAllModifications(thePlayer)
        
        if (physicsSimulationEnabled) {
            updatePhysicsSimulation(thePlayer)
            thePlayer.motionX = physicsState.velocityX.toDouble()
            thePlayer.motionY = physicsState.velocityY.toDouble()
            thePlayer.motionZ = physicsState.velocityZ.toDouble()
        }
        
        if (machineLearningEnabled) {
            updateMachineLearning(thePlayer)
        }
        
        if (geneticAlgorithmEnabled) {
            updateGeneticAlgorithm(thePlayer)
        }
        
        if (quantumSuperposition) {
            updateQuantumState(thePlayer)
        }
    }

    val onMotion = handler<MotionEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (!shouldProcess(thePlayer)) return@handler
        
        applyMotionModifications(thePlayer)
    }

    val onStrafe = handler<StrafeEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (!shouldProcess(thePlayer)) return@handler
        
        applyStrafeModifications(it)
    }

    val onJump = handler<JumpEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        
        if (!shouldProcess(thePlayer)) return@handler
        
        applyJumpModifications(it)
    }

    // ==================== 核心函数 ====================
    private fun shouldProcess(player: EntityLivingBase): Boolean {
        // 检查基础条件
        if (enableLiquidCheck && player.isInLiquid) return false
        if (enableWebCheck && player.isInWeb) return false
        if (enableLadderCheck && player.isOnLadder) return false
        if (enableVehicleCheck && player.ridingEntity != null) return false
        if (enableFireCheck && player.isBurning) return false
        if (enableWaterCheck && player.isInWater) return false
        if (enableLavaCheck && player.isInLava) return false
        
        // 检查状态条件
        if (enableGroundCheck && !player.onGround) return false
        if (enableAirCheck && player.onGround) return false
        if (enableSprintCheck && !player.isSprinting) return false
        if (enableSneakCheck && !player.isSneaking) return false
        if (enableJumpCheck && !player.isJumping) return false
        
        // 检查受伤时间
        if (enableHurtTimeCheck && hurtTimeCheckEnabled) {
            val hurtTime = player.hurtTime
            if (hurtTime < hurtTimeMin || hurtTime > hurtTimeMax) return false
        }
        
        // 检查概率
        if (randomState.nextInt(100) >= chance) return false
        
        return true
    }

    private fun handleVelocityPacket(event: PacketEvent, packet: S12PacketEntityVelocity) {
        velocityReceived = true
        velocityTimer.reset()
        
        val motionX = packet.getMotionX() / 8000.0
        val motionY = packet.getMotionY() / 8000.0
        val motionZ = packet.getMotionZ() / 8000.0
        
        val modifiedMotion = applyAllTransformations(motionX, motionY, motionZ)
        
        if (enablePacketCancellation) {
            event.cancelEvent()
        }
        
        // 应用修改后的运动
        applyModifiedMotion(modifiedMotion)
    }

    private fun handleExplosionPacket(event: PacketEvent, packet: S27PacketExplosion) {
        val motionX = packet.field_149152_f.toDouble()
        val motionY = packet.field_149153_g.toDouble()
        val motionZ = packet.field_149159_h.toDouble()
        
        val modifiedMotion = applyExplosionTransformations(motionX, motionY, motionZ)
        
        // 使用反射修改数据包字段
        try {
            val field = S27PacketExplosion::class.java.getDeclaredField("field_149152_f")
            field.isAccessible = true
            field.setFloat(packet, modifiedMotion.first.toFloat())
        } catch (e: Exception) {}
        
        try {
            val field = S27PacketExplosion::class.java.getDeclaredField("field_149153_g")
            field.isAccessible = true
            field.setFloat(packet, modifiedMotion.second.toFloat())
        } catch (e: Exception) {}
        
        try {
            val field = S27PacketExplosion::class.java.getDeclaredField("field_149159_h")
            field.isAccessible = true
            field.setFloat(packet, modifiedMotion.third.toFloat())
        } catch (e: Exception) {}
    }

    private fun applyAllTransformations(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        var resultX = x
        var resultY = y
        var resultZ = z
        
        // 应用基础乘法
        if (enableHorizontalReduction) {
            resultX *= getDynamicValue("horizontalMultiplier", horizontalMultiplier)
            resultZ *= getDynamicValue("horizontalMultiplier", horizontalMultiplier)
        }
        
        if (enableVerticalReduction) {
            resultY *= getDynamicValue("verticalMultiplier", verticalMultiplier)
        }
        
        // 应用运动修改
        if (enableMotionModification) {
            resultX = applyMotionTransform(resultX, "X")
            resultY = applyMotionTransform(resultY, "Y")
            resultZ = applyMotionTransform(resultZ, "Z")
        }
        
        // 应用反转
        if (reverseStrength != 0f) {
            resultX *= -getDynamicValue("reverseStrength", reverseStrength)
            resultZ *= -getDynamicValue("reverseStrength", reverseStrength)
        }
        
        if (reverseXEnabled) resultX *= -getDynamicValue("reverseXStrength", reverseXStrength)
        if (reverseYEnabled) resultY *= -getDynamicValue("reverseYStrength", reverseYStrength)
        if (reverseZEnabled) resultZ *= -getDynamicValue("reverseZStrength", reverseZStrength)
        
        // 应用平滑
        if (smoothEnabled()) {
            resultX = applySmoothing(resultX, "X")
            resultY = applySmoothing(resultY, "Y")
            resultZ = applySmoothing(resultZ, "Z")
        }
        
        // 应用延迟
        if (delayEnabled()) {
            resultX = applyDelay(resultX, "X")
            resultY = applyDelay(resultY, "Y")
            resultZ = applyDelay(resultZ, "Z")
        }
        
        // 应用随机
        if (randomEnabled) {
            resultX = applyRandom(resultX, "X")
            resultY = applyRandom(resultY, "Y")
            resultZ = applyRandom(resultZ, "Z")
        }
        
        // 应用数学函数
        if (sinEnabled) resultX = sin(resultX * getDynamicValue("sinMultiplier", sinMultiplier))
        if (cosEnabled) resultY = cos(resultY * getDynamicValue("cosMultiplier", cosMultiplier))
        if (tanEnabled) resultZ = tan(resultZ * getDynamicValue("tanMultiplier", tanMultiplier))
        
        if (logEnabled) resultX = ln(abs(resultX) + 0.0001) * getDynamicValue("logMultiplier", logMultiplier)
        if (expEnabled) resultY = exp(resultY) * getDynamicValue("expMultiplier", expMultiplier)
        if (sqrtEnabled) resultZ = sqrt(abs(resultZ)) * getDynamicValue("sqrtMultiplier", sqrtMultiplier)
        
        // 修复pow错误：使用Math.pow
        if (powEnabled) resultX = Math.pow(resultX, getDynamicValue("powMultiplier", powMultiplier).toDouble())
        if (absEnabled) resultX = abs(resultX) * getDynamicValue("absMultiplier", absMultiplier)
        if (floorEnabled) resultY = floor(resultY) * getDynamicValue("floorMultiplier", floorMultiplier)
        if (ceilEnabled) resultZ = ceil(resultZ) * getDynamicValue("ceilMultiplier", ceilMultiplier)
        if (roundEnabled) resultX = round(resultX) * getDynamicValue("roundMultiplier", roundMultiplier)
        
        // 应用限制
        if (clampEnabled) {
            resultX = resultX.coerceIn(minHorizontal.toDouble(), maxHorizontal.toDouble())
            resultY = resultY.coerceIn(minVertical.toDouble(), maxVertical.toDouble())
            resultZ = resultZ.coerceIn(minHorizontal.toDouble(), maxHorizontal.toDouble())
        }
        
        if (minMaxXEnabled) resultX = resultX.coerceIn(minMotionX.toDouble(), maxMotionX.toDouble())
        if (minMaxYEnabled) resultY = resultY.coerceIn(minMotionY.toDouble(), maxMotionY.toDouble())
        if (minMaxZEnabled) resultZ = resultZ.coerceIn(minMotionZ.toDouble(), maxMotionZ.toDouble())
        
        // 应用矩阵变换
        if (matrixTransformEnabled) {
            val matrixResult = applyMatrixTransform(resultX, resultY, resultZ)
            resultX = matrixResult.first
            resultY = matrixResult.second
            resultZ = matrixResult.third
        }
        
        // 应用四元数旋转
        if (quaternionRotationEnabled) {
            val quaternionResult = applyQuaternionRotation(resultX, resultY, resultZ)
            resultX = quaternionResult.first
            resultY = quaternionResult.second
            resultZ = quaternionResult.third
        }
        
        // 应用向量投影
        if (vectorProjectionEnabled) {
            val projectionResult = applyVectorProjection(resultX, resultY, resultZ)
            resultX = projectionResult.first
            resultY = projectionResult.second
            resultZ = projectionResult.third
        }
        
        // 应用傅里叶变换
        if (advancedMathEnabled && fourierSeriesTerms > 0) {
            resultX = applyFourierTransform(resultX, fourierSeriesTerms)
            resultY = applyFourierTransform(resultY, fourierSeriesTerms)
            resultZ = applyFourierTransform(resultZ, fourierSeriesTerms)
        }
        
        // 应用小波变换
        if (advancedMathEnabled && waveletScale != 1.0f) {
            resultX = applyWaveletTransform(resultX, waveletScale.toDouble(), waveletShift.toDouble())
            resultY = applyWaveletTransform(resultY, waveletScale.toDouble(), waveletShift.toDouble())
            resultZ = applyWaveletTransform(resultZ, waveletScale.toDouble(), waveletShift.toDouble())
        }
        
        // 应用量子效应
        if (quantumSuperposition) {
            val quantumResult = applyQuantumEffects(resultX, resultY, resultZ)
            resultX = quantumResult.first
            resultY = quantumResult.second
            resultZ = quantumResult.third
        }
        
        return Triple(resultX, resultY, resultZ)
    }

    private fun getDynamicValue(parameterName: String, defaultValue: Float): Float {
        return dynamicParameters[parameterName] ?: defaultValue
    }

    private fun applyMotionTransform(value: Double, axis: String): Double {
        var result = value
        
        // 获取动态值
        val multiplier = when (axis) {
            "X" -> getDynamicValue("motionXMultiplier", motionXMultiplier)
            "Y" -> getDynamicValue("motionYMultiplier", motionYMultiplier)
            "Z" -> getDynamicValue("motionZMultiplier", motionZMultiplier)
            else -> 1.0f
        }
        
        val addition = when (axis) {
            "X" -> motionXAddition
            "Y" -> motionYAddition
            "Z" -> motionZAddition
            else -> 0.0f
        }
        
        val exponent = when (axis) {
            "X" -> motionXExponent
            "Y" -> motionYExponent
            "Z" -> motionZExponent
            else -> 1.0f
        }
        
        // 应用乘法
        if (multiplier != 1.0f) {
            result *= multiplier.toDouble()
        }
        
        // 应用加法
        if (addition != 0.0f) {
            result += addition.toDouble()
        }
        
        // 应用指数
        if (exponent != 1.0f) {
            result = if (result >= 0) {
                Math.pow(result, exponent.toDouble())
            } else {
                -Math.pow(abs(result), exponent.toDouble())
            }
        }
        
        // 应用计时器
        if (timerXEnabled && axis == "X") {
            result *= getDynamicValue("timerXSpeed", timerXSpeed) * ((tickCounter % delayXTicks).toFloat())
        }
        if (timerYEnabled && axis == "Y") {
            result *= getDynamicValue("timerYSpeed", timerYSpeed) * ((tickCounter % delayYTicks).toFloat())
        }
        if (timerZEnabled && axis == "Z") {
            result *= getDynamicValue("timerZSpeed", timerZSpeed) * ((tickCounter % delayZTicks).toFloat())
        }
        
        return result
    }

    private fun smoothEnabled(): Boolean = smoothXEnabled || smoothYEnabled || smoothZEnabled

    private fun delayEnabled(): Boolean = delayXEnabled || delayYEnabled || delayZEnabled

    private fun applySmoothing(value: Double, axis: String): Double {
        val amount = when (axis) {
            "X" -> getDynamicValue("smoothXAmount", smoothXAmount)
            "Y" -> getDynamicValue("smoothYAmount", smoothYAmount)
            "Z" -> getDynamicValue("smoothZAmount", smoothZAmount)
            else -> 0.5f
        }
        
        val enabled = when (axis) {
            "X" -> smoothXEnabled
            "Y" -> smoothYEnabled
            "Z" -> smoothZEnabled
            else -> false
        }
        
        if (!enabled) return value
        
        val historyIndex = when (axis) {
            "X" -> 0
            "Y" -> 1
            "Z" -> 2
            else -> 0
        }
        
        return if (motionHistory.isNotEmpty()) {
            val previous = motionHistory.last().let {
                when (historyIndex) {
                    0 -> it.first
                    1 -> it.second
                    2 -> it.third
                    else -> 0.0
                }
            }
            previous * (1 - amount) + value * amount
        } else {
            value
        }
    }

    private fun applyDelay(value: Double, axis: String): Double {
        val delay = when (axis) {
            "X" -> getDynamicValue("delayXTime", delayXTime)
            "Y" -> getDynamicValue("delayYTime", delayYTime)
            "Z" -> getDynamicValue("delayZTime", delayZTime)
            else -> 0.0f
        }
        
        val enabled = when (axis) {
            "X" -> delayXEnabled
            "Y" -> delayYEnabled
            "Z" -> delayZEnabled
            else -> false
        }
        
        if (!enabled || delay <= 0) return value
        
        return if (velocityTimer.hasTimePassed(delay.toLong())) {
            value
        } else {
            0.0
        }
    }

    private fun applyRandom(value: Double, axis: String): Double {
        val multiplier = when (axis) {
            "X" -> getDynamicValue("randomXMultiplier", randomXMultiplier)
            "Y" -> getDynamicValue("randomYMultiplier", randomYMultiplier)
            "Z" -> getDynamicValue("randomZMultiplier", randomZMultiplier)
            else -> 0.0f
        }
        
        val enabled = when (axis) {
            "X" -> randomXEnabled
            "Y" -> randomYEnabled
            "Z" -> randomZEnabled
            else -> false
        }
        
        if (!enabled || multiplier <= 0) return value
        
        val random = getRandomGenerator("random$axis")
        val randomFactor = 1.0 + (random.nextDouble() * 2 - 1) * multiplier.toDouble()
        return value * randomFactor
    }

    private fun updateMotionHistory(player: EntityLivingBase) {
        motionHistory.add(Triple(player.motionX, player.motionY, player.motionZ))
        
        // 保持历史记录大小
        val maxHistory = maxOf(smoothTicks, delayTicks, randomTicks, 100)
        if (motionHistory.size > maxHistory) {
            motionHistory.removeAt(0)
        }
    }

    private fun applyModifiedMotion(motion: Triple<Double, Double, Double>) {
        val thePlayer = mc.thePlayer ?: return
        
        if (motionXEnabled) thePlayer.motionX = motion.first
        if (motionYEnabled) thePlayer.motionY = motion.second
        if (motionZEnabled) thePlayer.motionZ = motion.third
    }

    private fun applyAllModifications(player: EntityLivingBase) {
        // 应用速度修改
        if (speedEnabled && speedMultiplier != 1.0f) {
            player.motionX *= getDynamicValue("speedMultiplier", speedMultiplier)
            player.motionZ *= getDynamicValue("speedMultiplier", speedMultiplier)
        }
        
        // 应用跳跃修改
        if (jumpPowerEnabled && jumpMultiplier != 1.0f && player.isJumping) {
            player.motionY *= getDynamicValue("jumpMultiplier", jumpMultiplier)
        }
        
        // 应用坠落修改
        if (fallDistanceEnabled && fallMultiplier != 1.0f) {
            player.fallDistance *= getDynamicValue("fallMultiplier", fallMultiplier)
        }
        
        // 应用重力修改
        if (gravityEnabled && gravityMultiplier != 1.0f) {
            player.motionY -= 0.08 * getDynamicValue("gravityMultiplier", gravityMultiplier)
        }
        
        // 应用计时器修改
        if (timerModificationEnabled() && timerSpeed != 1.0f) {
            mc.timer.timerSpeed = getDynamicValue("timerSpeed", timerSpeed)
        }
        
        // 应用疾跑修改
        if (sprintModificationEnabled() && sprintSpeedMultiplier != 1.0f && player.isSprinting) {
            player.motionX *= getDynamicValue("sprintSpeedMultiplier", sprintSpeedMultiplier)
            player.motionZ *= getDynamicValue("sprintSpeedMultiplier", sprintSpeedMultiplier)
        }
        
        // 应用潜行修改
        if (sneakModificationEnabled() && sneakSpeedMultiplier != 1.0f && player.isSneaking) {
            player.motionX *= getDynamicValue("sneakSpeedMultiplier", sneakSpeedMultiplier)
            player.motionZ *= getDynamicValue("sneakSpeedMultiplier", sneakSpeedMultiplier)
        }
    }

    private fun timerModificationEnabled(): Boolean = 
        enableTimerModification && (timerXEnabled || timerYEnabled || timerZEnabled || timerXSpeedEnabled || timerYSpeedEnabled || timerZSpeedEnabled)

    private fun sprintModificationEnabled(): Boolean = 
        enableSprintModification && (sprintEnabled || sprintTicks > 0)

    private fun sneakModificationEnabled(): Boolean = 
        enableSneakModification && (sneakEnabled || sneakTicks > 0)

    // ==================== 数据包处理函数 ====================
    private fun updatePacketStatistics(packet: Packet<*>) {
        val packetType = packet.javaClass.simpleName
        val stats = packetStatistics.getOrPut(packetType) { PacketStats() }
        
        stats.totalCount++
        stats.totalSize += estimatePacketSize(packet)
        
        if (statisticsTimer.hasTimePassed(1000)) {
            stats.packetsPerSecond = stats.totalCount - stats.lastCount
            stats.lastCount = stats.totalCount
            
            stats.averageSize = if (stats.totalCount > 0) stats.totalSize / stats.totalCount else 0
            stats.maxSize = maxOf(stats.maxSize, estimatePacketSize(packet))
            
            // 记录到历史
            stats.history.add(stats.packetsPerSecond)
            if (stats.history.size > 100) {
                stats.history.removeAt(0)
            }
        }
        
        totalPacketsProcessed++
    }
    
    private fun estimatePacketSize(packet: Packet<*>): Long {
        // 简化的数据包大小估计
        return when (packet) {
            is S12PacketEntityVelocity -> 16L
            is S27PacketExplosion -> 32L
            is S32PacketConfirmTransaction -> 8L
            is C03PacketPlayer -> 24L
            is C0APacketAnimation -> 4L
            else -> 8L
        }
    }
    
    private fun applyPacketProcessing(event: PacketEvent, packet: Packet<*>) {
        // 数据包延迟
        if (enablePacketDelay) {
            applyPacketDelay(event, packet)
        }
        
        // 数据包拦截
        if (enablePacketInterception) {
            applyPacketInterception(event, packet)
        }
        
        // 数据包修改
        if (enablePacketModification) {
            applyPacketModification(event, packet)
        }
        
        // 数据包重发
        if (enablePacketResend) {
            applyPacketResend(event, packet)
        }
        
        // 网络流量控制
        if (enableTrafficControl) {
            applyTrafficControl(event, packet)
        }
    }
    
    private fun applyPacketDelay(event: PacketEvent, packet: Packet<*>) {
        val packetClass = packet.javaClass
        var delay = 0L
        
        // 获取特定数据包类型的延迟
        when {
            packet is S12PacketEntityVelocity && delayS12Packet -> delay = s12PacketDelay.toLong()
            packet is S27PacketExplosion && delayS27Packet -> delay = s27PacketDelay.toLong()
            packet is S32PacketConfirmTransaction && delayS32Packet -> delay = s32PacketDelay.toLong()
            packet is C03PacketPlayer && delayC03Packet -> delay = c03PacketDelay.toLong()
        }
        
        // 应用基础延迟
        if (enablePacketReceiveDelay) delay += receiveDelayBase.toLong()
        if (enablePacketSendDelay) delay += sendDelayBase.toLong()
        
        // 应用随机性
        if (delayRandomness > 0) {
            val random = getRandomGenerator("delay")
            delay = (delay * (1.0 + (random.nextDouble() * 2 - 1) * delayRandomness.toDouble())).toLong()
        }
        
        // 应用延迟
        if (delay > 0) {
            event.cancelEvent()
            schedulePacket(packet, delay)
            totalPacketsDelayed++
        }
    }
    
    private fun applyPacketInterception(event: PacketEvent, packet: Packet<*>) {
        when {
            packet is S12PacketEntityVelocity && interceptS12Packet -> event.cancelEvent()
            packet is S27PacketExplosion && interceptS27Packet -> event.cancelEvent()
            packet is S32PacketConfirmTransaction && interceptS32Packet -> event.cancelEvent()
        }
    }
    
    private fun applyPacketModification(event: PacketEvent, packet: Packet<*>) {
        when {
            packet is S12PacketEntityVelocity && modifyS12Packet -> {
                // 使用反射修改数据包字段
                try {
                    val entityIdField = S12PacketEntityVelocity::class.java.getDeclaredField("entityID")
                    entityIdField.isAccessible = true
                    entityIdField.setInt(packet, packet.entityID + s12EntityIdOffset)
                    
                    val motionXField = S12PacketEntityVelocity::class.java.getDeclaredField("motionX")
                    motionXField.isAccessible = true
                    motionXField.setInt(packet, (packet.motionX * getDynamicValue("s12MotionXMultiplier", s12MotionXMultiplier)).toInt())
                    
                    val motionYField = S12PacketEntityVelocity::class.java.getDeclaredField("motionY")
                    motionYField.isAccessible = true
                    motionYField.setInt(packet, (packet.motionY * getDynamicValue("s12MotionYMultiplier", s12MotionYMultiplier)).toInt())
                    
                    val motionZField = S12PacketEntityVelocity::class.java.getDeclaredField("motionZ")
                    motionZField.isAccessible = true
                    motionZField.setInt(packet, (packet.motionZ * getDynamicValue("s12MotionZMultiplier", s12MotionZMultiplier)).toInt())
                    
                    totalPacketsModified++
                } catch (e: Exception) {
                    // 忽略反射错误
                }
            }
            
            packet is S27PacketExplosion && modifyS27Packet -> {
                // 使用反射修改数据包字段
                try {
                    val explosionXField = S27PacketExplosion::class.java.getDeclaredField("field_149152_f")
                    explosionXField.isAccessible = true
                    explosionXField.setFloat(packet, packet.field_149152_f * getDynamicValue("s27ExplosionXMultiplier", s27ExplosionXMultiplier))
                    
                    val explosionYField = S27PacketExplosion::class.java.getDeclaredField("field_149153_g")
                    explosionYField.isAccessible = true
                    explosionYField.setFloat(packet, packet.field_149153_g * getDynamicValue("s27ExplosionYMultiplier", s27ExplosionYMultiplier))
                    
                    val explosionZField = S27PacketExplosion::class.java.getDeclaredField("field_149159_h")
                    explosionZField.isAccessible = true
                    explosionZField.setFloat(packet, packet.field_149159_h * getDynamicValue("s27ExplosionZMultiplier", s27ExplosionZMultiplier))
                    
                    totalPacketsModified++
                } catch (e: Exception) {
                    // 忽略反射错误
                }
            }
        }
    }
    
    private fun applyPacketResend(event: PacketEvent, packet: Packet<*>) {
        val shouldResend = when {
            packet is S12PacketEntityVelocity && resendS12Packet -> true
            packet is S27PacketExplosion && resendS27Packet -> true
            packet is S32PacketConfirmTransaction && resendS32Packet -> true
            else -> false
        }
        
        if (shouldResend) {
            var count = resendCount
            var delay = resendDelay
            
            // 应用随机性
            val resendRandom = getRandomGenerator("resend")
            if (resendRandomCount) {
                count = resendRandom.nextInt(1, 4)
            }
            
            if (resendRandomDelay) {
                delay = resendRandom.nextInt(0, 101)
            }
            
            // 重发数据包
            repeat(count) {
                schedulePacket(packet, delay.toLong())
                totalPacketsResent++
            }
        }
    }
    
    private fun applyTrafficControl(event: PacketEvent, packet: Packet<*>) {
        // 数据包速率限制
        if (limitPacketRate) {
            val currentRate = packetStatistics.values.sumOf { it.packetsPerSecond }
            if (currentRate > maxPacketsPerSecond) {
                event.cancelEvent()
                totalPacketsLost++
                return
            }
        }
        
        // 数据包大小限制
        if (limitPacketSize) {
            val packetSize = estimatePacketSize(packet)
            if (packetSize > maxPacketSize) {
                event.cancelEvent()
                totalPacketsLost++
                return
            }
        }
        
        // 模拟丢包
        if (simulatePacketLoss) {
            val lossRate = if (packetLossRateRandomMin < packetLossRateRandomMax) {
                val lossRandom = getRandomGenerator("loss")
                lossRandom.nextFloat() * (packetLossRateRandomMax - packetLossRateRandomMin) + packetLossRateRandomMin
            } else {
                packetLossRate
            }
            
            val lossRandom = getRandomGenerator("loss")
            if (lossRandom.nextFloat() < lossRate) {
                event.cancelEvent()
                totalPacketsLost++
                return
            }
        }
        
        // 模拟网络延迟
        if (simulateNetworkLag) {
            val lagRandom = getRandomGenerator("lag")
            val lag = lagRandom.nextInt(networkLagRandomMin, networkLagRandomMax + 1)
            if (lag > 0) {
                event.cancelEvent()
                schedulePacket(packet, lag.toLong())
                return
            }
        }
        
        // 模拟抖动
        if (simulateJitter) {
            val jitterRandom = getRandomGenerator("jitter")
            val jitter = jitterRandom.nextInt(jitterRandomMin, jitterRandomMax + 1)
            if (jitter != 0) {
                val currentDelay = packetDelayMap.getOrDefault(packet.javaClass, 0L)
                packetDelayMap[packet.javaClass] = currentDelay + jitter
            }
        }
    }
    
    private fun schedulePacket(packet: Packet<*>, delay: Long) {
        val scheduledTime = System.currentTimeMillis() + delay
        packetQueue.add(packet)
        packetDelayMap[packet.javaClass] = scheduledTime
    }
    
    private fun processPacketQueue() {
        val currentTime = System.currentTimeMillis()
        val iterator = packetQueue.iterator()
        
        while (iterator.hasNext()) {
            val packet = iterator.next()
            val packetClass = packet.javaClass
            val scheduledTime = packetDelayMap[packetClass] ?: 0L
            
            if (currentTime >= scheduledTime) {
                // 发送延迟的数据包
                mc.netHandler.addToSendQueue(packet)
                iterator.remove()
                packetDelayMap.remove(packetClass)
            }
        }
    }
    
    private fun getRandomGenerator(name: String): Random {
        return randomGenerators.getOrPut(name) {
            Random(System.currentTimeMillis() + name.hashCode())
        }
    }
    
    private fun updateDynamicRandomization() {
        if (!randomUpdateTimer.hasTimePassed(randomUpdateInterval.toLong())) return
        
        randomizationCycle++
        
        // 随机化所有参数
        if (randomizeAllMultipliers) {
            randomizeParameter("horizontalMultiplier", horizontalMultiplierRandomMin, horizontalMultiplierRandomMax)
            randomizeParameter("verticalMultiplier", verticalMultiplierRandomMin, verticalMultiplierRandomMax)
            randomizeParameter("motionXMultiplier", motionXMultiplierRandomMin, motionXMultiplierRandomMax)
            randomizeParameter("motionYMultiplier", motionYMultiplierRandomMin, motionYMultiplierRandomMax)
            randomizeParameter("motionZMultiplier", motionZMultiplierRandomMin, motionZMultiplierRandomMax)
        }
        
        if (randomizeAllDelays) {
            randomizeParameter("delayTime", delayTimeRandomMin, delayTimeRandomMax)
            randomizeParameter("reverseStrength", reverseStrengthRandomMin, reverseStrengthRandomMax)
            randomizeParameter("smoothAmount", smoothAmountRandomMin, smoothAmountRandomMax)
        }
        
        // 更新动态参数
        updateDynamicParameters()
    }
    
    private fun randomizeParameter(parameterName: String, min: Float, max: Float) {
        if (min >= max) return
        
        val random = getRandomGenerator(parameterName)
        val value = min + random.nextFloat() * (max - min)
        dynamicParameters[parameterName] = value
    }
    
    private fun updateDynamicParameters() {
        // 根据历史数据动态调整参数
        for ((param, history) in parameterHistory) {
            if (history.size >= 10) {
                val recentAvg = history.takeLast(10).average().toFloat()
                val overallAvg = history.average().toFloat()
                
                // 如果近期平均值偏离整体平均值，进行调整
                if (abs(recentAvg - overallAvg) > 0.1f) {
                    val adjustment = (overallAvg - recentAvg) * dynamicRandomizationRate
                    dynamicParameters[param] = (dynamicParameters[param] ?: 0f) + adjustment
                }
            }
        }
    }
    
    private fun updateParameterHistory() {
        // 记录关键参数历史
        val paramsToTrack = listOf(
            "horizontalMultiplier" to horizontalMultiplier,
            "verticalMultiplier" to verticalMultiplier,
            "motionXMultiplier" to motionXMultiplier,
            "motionYMultiplier" to motionYMultiplier,
            "motionZMultiplier" to motionZMultiplier
        )
        
        for ((name, value) in paramsToTrack) {
            val history = parameterHistory.getOrPut(name) { mutableListOf() }
            history.add(value)
            
            // 限制历史大小
            if (history.size > 1000) {
                history.removeAt(0)
            }
        }
    }

    // ==================== 高级功能函数 ====================
    private fun applyMatrixTransform(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        // 3x3 变换矩阵
        val matrix = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0),
            doubleArrayOf(0.0, 1.0, 0.0),
            doubleArrayOf(0.0, 0.0, 1.0)
        )
        
        val resultX = matrix[0][0] * x + matrix[0][1] * y + matrix[0][2] * z
        val resultY = matrix[1][0] * x + matrix[1][1] * y + matrix[1][2] * z
        val resultZ = matrix[2][0] * x + matrix[2][1] * y + matrix[2][2] * z
        
        return Triple(resultX, resultY, resultZ)
    }

    private fun applyQuaternionRotation(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        // 四元数旋转实现
        val angle = Math.toRadians(45.0)
        val cosHalfAngle = cos(angle / 2)
        val sinHalfAngle = sin(angle / 2)
        
        val qx = 0.0
        val qy = 1.0
        val qz = 0.0
        
        // 四元数乘法
        val resultX = (1 - 2 * sinHalfAngle * sinHalfAngle) * x + 2 * sinHalfAngle * (qz * y - qy * z)
        val resultY = (1 - 2 * sinHalfAngle * sinHalfAngle) * y + 2 * sinHalfAngle * (qx * z - qz * x)
        val resultZ = (1 - 2 * sinHalfAngle * sinHalfAngle) * z + 2 * sinHalfAngle * (qy * x - qx * y)
        
        return Triple(resultX, resultY, resultZ)
    }

    private fun applyVectorProjection(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        // 向量投影到平面
        val normalX = 0.0
        val normalY = 1.0
        val normalZ = 0.0
        
        val dot = x * normalX + y * normalY + z * normalZ
        val lengthSquared = normalX * normalX + normalY * normalY + normalZ * normalZ
        
        val projX = x - dot * normalX / lengthSquared
        val projY = y - dot * normalY / lengthSquared
        val projZ = z - dot * normalZ / lengthSquared
        
        return Triple(projX, projY, projZ)
    }

    private fun applyFourierTransform(value: Double, terms: Int): Double {
        var result = 0.0
        for (n in 1..terms) {
            result += sin(value * n) / n
        }
        return result
    }

    private fun applyWaveletTransform(value: Double, scale: Double, shift: Double): Double {
        // Morlet小波变换
        return cos(5.0 * (value - shift) / scale) * exp(-(value - shift) * (value - shift) / (2 * scale * scale))
    }

    private fun applyQuantumEffects(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        var resultX = x
        var resultY = y
        var resultZ = z
        
        val quantumRandom = getRandomGenerator("quantum")
        
        if (quantumSuperposition) {
            // 量子叠加态
            resultX = if (quantumRandom.nextBoolean()) resultX else -resultX
            resultY = if (quantumRandom.nextBoolean()) resultY else -resultY
            resultZ = if (quantumRandom.nextBoolean()) resultZ else -resultZ
        }
        
        if (quantumEntanglement) {
            // 量子纠缠
            resultY = resultX * 0.5
            resultZ = resultX * 0.25
        }
        
        if (quantumTunneling) {
            // 量子隧穿效应
            if (abs(resultX) < 0.1) resultX *= 10.0
            if (abs(resultY) < 0.1) resultY *= 10.0
            if (abs(resultZ) < 0.1) resultZ *= 10.0
        }
        
        if (quantumInterference) {
            // 量子干涉
            resultX += sin(resultX * 10.0) * 0.1
            resultY += cos(resultY * 10.0) * 0.1
            resultZ += sin(resultZ * 10.0) * 0.1
        }
        
        return Triple(resultX, resultY, resultZ)
    }

    private fun updatePhysicsSimulation(player: EntityLivingBase) {
        // 将玩家的运动向量传递给物理模拟
        physicsState.update(
            player.motionX,
            player.motionY,
            player.motionZ,
            frictionCoefficient.toDouble(),
            restitutionCoefficient.toDouble(),
            dragCoefficient.toDouble(),
            airDensity.toDouble(),
            gravityConstant.toDouble(),
            timeStep.toDouble()
        )
    }

    private fun updateMachineLearning(player: EntityLivingBase) {
        // 初始化神经网络权重
        if (neuralNetworkWeights.isEmpty()) {
            initializeNeuralNetwork()
        }
        
        // 前向传播
        val speed = sqrt(player.motionX * player.motionX + player.motionZ * player.motionZ)
        val inputs = arrayOf(
            player.motionX.toFloat(),
            player.motionY.toFloat(),
            player.motionZ.toFloat(),
            speed.toFloat(),
            player.hurtTime.toFloat()
        )
        
        val outputs = neuralNetworkForward(inputs)
        
        // 应用神经网络输出
        if (outputs.size >= 3) {
            player.motionX = outputs[0].toDouble()
            player.motionY = outputs[1].toDouble()
            player.motionZ = outputs[2].toDouble()
        }
    }

    private fun updateGeneticAlgorithm(player: EntityLivingBase) {
        // 初始化种群
        if (geneticPopulation.isEmpty()) {
            initializeGeneticPopulation()
        }
        
        // 评估适应度
        val fitnessScores = evaluateFitness()
        
        // 选择、交叉、变异
        evolvePopulation(fitnessScores)
        
        // 应用最佳个体
        applyBestIndividual(player)
    }

    private fun updateQuantumState(player: EntityLivingBase) {
        quantumState.update(
            player.motionX,
            player.motionY,
            player.motionZ,
            qubitCount,
            quantumGateType
        )
        
        // 应用量子状态
        val quantumMotion = quantumState.measure()
        player.motionX = quantumMotion.first
        player.motionY = quantumMotion.second
        player.motionZ = quantumMotion.third
    }

    private fun applyExplosionTransformations(x: Double, y: Double, z: Double): Triple<Double, Double, Double> {
        var resultX = x
        var resultY = y
        var resultZ = z
        
        if (explosionMotionXEnabled) resultX *= explosionMotionX.toDouble()
        if (explosionMotionYEnabled) resultY *= explosionMotionY.toDouble()
        if (explosionMotionZEnabled) resultZ *= explosionMotionZ.toDouble()
        
        if (explosionSpeedEnabled) {
            val speed = sqrt(resultX * resultX + resultZ * resultZ)
            if (speed > 0) {
                resultX *= explosionSpeed.toDouble() / speed
                resultZ *= explosionSpeed.toDouble() / speed
            }
        }
        
        if (explosionJumpEnabled) resultY *= explosionJump.toDouble()
        if (explosionFallEnabled) resultY = max(resultY, -explosionFall.toDouble())
        
        return Triple(resultX, resultY, resultZ)
    }

    private fun handleOtherPackets(event: PacketEvent, packet: Packet<*>) {
        // 处理其他类型的数据包
        when (packet) {
            is S32PacketConfirmTransaction -> {
                if (enablePacketCancellation) {
                    event.cancelEvent()
                }
            }
        }
    }

    private fun applyMotionModifications(player: EntityLivingBase) {
        if (!enableMotionModification) return
        
        // 应用所有运动修改
        if (motionXEnabled && motionXMultiplier != 1.0f) {
            player.motionX *= getDynamicValue("motionXMultiplier", motionXMultiplier)
        }
        
        if (motionYEnabled && motionYMultiplier != 1.0f) {
            player.motionY *= getDynamicValue("motionYMultiplier", motionYMultiplier)
        }
        
        if (motionZEnabled && motionZMultiplier != 1.0f) {
            player.motionZ *= getDynamicValue("motionZMultiplier", motionZMultiplier)
        }
        
        // 应用加法
        if (motionXAddition != 0.0f) player.motionX += motionXAddition.toDouble()
        if (motionYAddition != 0.0f) player.motionY += motionYAddition.toDouble()
        if (motionZAddition != 0.0f) player.motionZ += motionZAddition.toDouble()
    }

    private fun applyStrafeModifications(event: StrafeEvent) {
        if (!enableStrafeModification) return
        
        val strafeXSpeedValue = getDynamicValue("strafeXSpeed", strafeXSpeed)
        val strafeZSpeedValue = getDynamicValue("strafeZSpeed", strafeZSpeed)
        
        // 在LiquidBounce中，StrafeEvent的strafe和forward可能是只读的
        // 我们通过修改玩家的运动来实现类似效果
        val thePlayer = mc.thePlayer ?: return
        
        if (strafeXEnabled) {
            thePlayer.motionX *= strafeXSpeedValue
        }
        if (strafeZEnabled) {
            thePlayer.motionZ *= strafeZSpeedValue
        }
    }

    private fun applyJumpModifications(event: JumpEvent) {
        if (!enableJumpModification) return
        
        val jumpMultiplierValue = getDynamicValue("jumpMultiplier", jumpMultiplier)
        
        // 修改事件字段
        if (jumpEnabled) event.motion = event.motion * jumpMultiplierValue + jumpAddition
    }

    // ==================== 辅助类 ====================
    private class PhysicsState(
        var velocityX: Float = 0.0f,
        var velocityY: Float = 0.0f,
        var velocityZ: Float = 0.0f,
        var positionX: Float = 0.0f,
        var positionY: Float = 0.0f,
        var positionZ: Float = 0.0f
    ) {
        fun update(
            inputX: Double,
            inputY: Double,
            inputZ: Double,
            friction: Double,
            restitution: Double,
            drag: Double,
            airDensity: Double,
            gravity: Double,
            timeStep: Double
        ) {
            // 应用重力
            velocityY -= (gravity * timeStep).toFloat()
            
            // 应用空气阻力
            val speed = sqrt(velocityX * velocityX + velocityY * velocityY + velocityZ * velocityZ)
            val dragForce = (0.5 * airDensity * drag * speed * speed).toFloat()
            
            if (speed > 0) {
                velocityX -= dragForce * velocityX / speed * timeStep.toFloat()
                velocityY -= dragForce * velocityY / speed * timeStep.toFloat()
                velocityZ -= dragForce * velocityZ / speed * timeStep.toFloat()
            }
            
            // 应用外部输入
            velocityX += (inputX * timeStep).toFloat()
            velocityY += (inputY * timeStep).toFloat()
            velocityZ += (inputZ * timeStep).toFloat()
            
            // 更新位置
            positionX += velocityX * timeStep.toFloat()
            positionY += velocityY * timeStep.toFloat()
            positionZ += velocityZ * timeStep.toFloat()
        }
    }

    private class KalmanFilter(
        var state: Double = 0.0,
        var uncertainty: Double = 1.0,
        var kalmanGain: Double = 0.8
    ) {
        fun predict(processNoise: Double): Double {
            uncertainty += processNoise
            return state
        }
        
        fun update(measurement: Double, measurementNoise: Double): Double {
            kalmanGain = uncertainty / (uncertainty + measurementNoise)
            state += kalmanGain * (measurement - state)
            uncertainty *= (1 - kalmanGain)
            return state
        }
    }

    private class QuantumState(
        var amplitudes: MutableList<Complex> = mutableListOf(),
        var qubitCount: Int = 2
    ) {
        init {
            initialize()
        }
        
        fun initialize() {
            amplitudes.clear()
            val totalStates = 1 shl qubitCount
            for (i in 0 until totalStates) {
                amplitudes.add(Complex(1.0 / sqrt(totalStates.toDouble()), 0.0))
            }
        }
        
        fun update(x: Double, y: Double, z: Double, newQubitCount: Int, gateType: String) {
            if (newQubitCount != qubitCount) {
                qubitCount = newQubitCount
                initialize()
            }
            
            applyQuantumGate(gateType, x, y, z)
        }
        
        fun applyQuantumGate(gateType: String, x: Double, y: Double, z: Double) {
            when (gateType) {
                "Hadamard" -> applyHadamardGate()
                "PauliX" -> applyPauliXGate()
                "PauliY" -> applyPauliYGate()
                "PauliZ" -> applyPauliZGate()
                "CNOT" -> applyCNOTGate()
            }
        }
        
        fun applyHadamardGate() {
            val newAmplitudes = mutableListOf<Complex>()
            val factor = 1.0 / sqrt(2.0)
            
            for (i in amplitudes.indices) {
                newAmplitudes.add(amplitudes[i] * factor)
                if (i + 1 < amplitudes.size) {
                    newAmplitudes.add(amplitudes[i] * factor)
                }
            }
            
            amplitudes = newAmplitudes
        }
        
        fun applyPauliXGate() {
            for (i in amplitudes.indices step 2) {
                if (i + 1 < amplitudes.size) {
                    val temp = amplitudes[i]
                    amplitudes[i] = amplitudes[i + 1]
                    amplitudes[i + 1] = temp
                }
            }
        }
        
        fun applyPauliYGate() {
            for (i in amplitudes.indices) {
                amplitudes[i] = amplitudes[i] * Complex(0.0, if (i % 2 == 0) -1.0 else 1.0)
            }
        }
        
        fun applyPauliZGate() {
            for (i in amplitudes.indices) {
                amplitudes[i] = amplitudes[i] * Complex(if (i % 2 == 0) 1.0 else -1.0, 0.0)
            }
        }
        
        fun applyCNOTGate() {
            if (amplitudes.size >= 4) {
                val temp = amplitudes[2]
                amplitudes[2] = amplitudes[3]
                amplitudes[3] = temp
            }
        }
        
        fun measure(): Triple<Double, Double, Double> {
            val probabilities = amplitudes.map { it.normSquared() }
            val random = Random.nextDouble(probabilities.sum())
            
            var cumulative = 0.0
            var measuredState = 0
            
            for (i in probabilities.indices) {
                cumulative += probabilities[i]
                if (random <= cumulative) {
                    measuredState = i
                    break
                }
            }
            
            val x = (measuredState and 1).toDouble() * 0.1 - 0.05
            val y = ((measuredState shr 1) and 1).toDouble() * 0.1 - 0.05
            val z = ((measuredState shr 2) and 1).toDouble() * 0.1 - 0.05
            
            return Triple(x, y, z)
        }
    }

    private class Complex(val real: Double, val imag: Double) {
        operator fun times(scalar: Double): Complex = Complex(real * scalar, imag * scalar)
        operator fun times(other: Complex): Complex = 
            Complex(real * other.real - imag * other.imag, real * other.imag + imag * other.real)
        
        fun normSquared(): Double = real * real + imag * imag
    }

    private class PacketStats {
        var totalCount: Long = 0
        var totalSize: Long = 0
        var packetsPerSecond: Long = 0
        var averageSize: Long = 0
        var maxSize: Long = 0
        var lastCount: Long = 0
        val history = mutableListOf<Long>()
    }

    // ==================== 其他辅助函数 ====================
    private fun initializeNeuralNetwork() {
        val inputSize = 5
        val hiddenSize = neuronsPerLayer
        val outputSize = 3
        
        for (i in 0 until hiddenLayers + 1) {
            val layerWeights = mutableListOf<Float>()
            val currentSize = if (i == 0) inputSize else hiddenSize
            val nextSize = if (i == hiddenLayers) outputSize else hiddenSize
            
            for (j in 0 until currentSize * nextSize) {
                layerWeights.add(Random.nextFloat() * 2 - 1)
            }
            
            neuralNetworkWeights.add(layerWeights)
        }
    }

    private fun neuralNetworkForward(inputs: Array<Float>): List<Float> {
        var currentLayer = inputs.toList()
        
        for (layerIndex in 0 until hiddenLayers + 1) {
            val weights = neuralNetworkWeights[layerIndex]
            val inputSize = currentLayer.size
            val outputSize = if (layerIndex == hiddenLayers) 3 else neuronsPerLayer
            
            val nextLayer = mutableListOf<Float>()
            
            for (i in 0 until outputSize) {
                var sum = 0.0f
                for (j in 0 until inputSize) {
                    sum += currentLayer[j] * weights[i * inputSize + j]
                }
                
                val activated = when (activationFunction) {
                    "ReLU" -> maxOf(0.0f, sum)
                    "Sigmoid" -> 1.0f / (1.0f + exp(-sum))
                    "Tanh" -> tanh(sum)
                    "LeakyReLU" -> if (sum > 0) sum else sum * 0.01f
                    else -> sum
                }
                
                nextLayer.add(activated)
            }
            
            currentLayer = nextLayer
            
            if (layerIndex < hiddenLayers && dropoutRate > 0) {
                for (i in currentLayer.indices) {
                    if (Random.nextFloat() < dropoutRate) {
                        currentLayer = currentLayer.toMutableList().apply { set(i, 0.0f) }
                    }
                }
            }
        }
        
        return currentLayer
    }

    private fun initializeGeneticPopulation() {
        for (i in 0 until populationSize) {
            val individual = mutableListOf<Float>()
            
            individual.add(Random.nextFloat() * 2 - 1)
            individual.add(Random.nextFloat() * 2 - 1)
            individual.add(Random.nextFloat() * 2)
            
            geneticPopulation.add(individual)
        }
    }

    private fun evaluateFitness(): List<Float> {
        return geneticPopulation.map { individual ->
            1.0f / (abs(individual[0]) + abs(individual[1]) + abs(individual[2]) + 0.001f)
        }
    }

    private fun evolvePopulation(fitnessScores: List<Float>) {
        val newPopulation = mutableListOf<MutableList<Float>>()
        
        val eliteIndices = fitnessScores.indices.sortedByDescending { fitnessScores[it] }.take(elitismCount)
        for (index in eliteIndices) {
            newPopulation.add(geneticPopulation[index].toMutableList())
        }
        
        while (newPopulation.size < populationSize) {
            val parent1 = selectParent(fitnessScores)
            val parent2 = selectParent(fitnessScores)
            
            val child = if (Random.nextFloat() < crossoverRate) {
                crossover(parent1, parent2)
            } else {
                parent1.toMutableList()
            }
            
            if (Random.nextFloat() < mutationRate) {
                mutate(child)
            }
            
            newPopulation.add(child)
        }
        
        geneticPopulation.clear()
        geneticPopulation.addAll(newPopulation)
    }

    private fun selectParent(fitnessScores: List<Float>): MutableList<Float> {
        val totalFitness = fitnessScores.sum()
        var randomPoint = Random.nextFloat() * totalFitness
        var cumulativeFitness = 0.0f
        
        for (i in fitnessScores.indices) {
            cumulativeFitness += fitnessScores[i]
            if (cumulativeFitness >= randomPoint) {
                return geneticPopulation[i]
            }
        }
        
        return geneticPopulation.last()
    }

    private fun crossover(parent1: MutableList<Float>, parent2: MutableList<Float>): MutableList<Float> {
        val child = mutableListOf<Float>()
        val crossoverPoint = Random.nextInt(parent1.size)
        
        for (i in parent1.indices) {
            child.add(if (i < crossoverPoint) parent1[i] else parent2[i])
        }
        
        return child
    }

    private fun mutate(individual: MutableList<Float>) {
        val mutationPoint = Random.nextInt(individual.size)
        individual[mutationPoint] += (Random.nextFloat() * 2 - 1) * mutationRate
    }

    private fun applyBestIndividual(player: EntityLivingBase) {
        if (geneticPopulation.isEmpty()) return
        
        val bestIndividual = geneticPopulation.maxByOrNull { 
            1.0f / (abs(it[0]) + abs(it[1]) + abs(it[2]) + 0.001f)
        } ?: return
        
        player.motionX *= bestIndividual[0].toDouble()
        player.motionY *= bestIndividual[1].toDouble()
        player.motionZ *= bestIndividual[0].toDouble()
        
        if (bestIndividual[2] > 0) {
            player.motionX *= -1.0
            player.motionZ *= -1.0
        }
    }

    override fun onDisable() {
        // 清理所有状态
        velocityReceived = false
        motionHistory.clear()
        tickCounter = 0
        neuralNetworkWeights.clear()
        geneticPopulation.clear()
        packetQueue.clear()
        packetDelayMap.clear()
        packetStatistics.clear()
        randomGenerators.clear()
        dynamicParameters.clear()
        parameterHistory.clear()
        
        totalPacketsProcessed = 0L
        totalPacketsDelayed = 0L
        totalPacketsModified = 0L
        totalPacketsResent = 0L
        totalPacketsLost = 0L
        randomizationCycle = 0
        
        // 重置计时器
        mc.timer.timerSpeed = 1.0f
        
        // 重置按键状态
        if (mc.currentScreen == null) {
            mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            mc.gameSettings.keyBindBack.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindBack)
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        }
    }

    override val tag: String
        get() = "Params:${getActiveParamCount()}/D:${totalPacketsDelayed}/M:${totalPacketsModified}"
    
    private fun getActiveParamCount(): Int {
        var count = 0
        
        // 统计所有活跃的布尔参数
        val booleanFields = this::class.java.declaredFields
            .filter { it.type == Boolean::class.javaPrimitiveType }
            .map { it.name }
        
        for (fieldName in booleanFields) {
            try {
                val field = this::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field.getBoolean(this)
                if (value) count++
            } catch (e: Exception) {
                // 忽略无法访问的字段
            }
        }
        
        // 统计非默认值的数值参数
        val floatFields = this::class.java.declaredFields
            .filter { it.type == Float::class.javaPrimitiveType }
            .map { it.name }
        
        for (fieldName in floatFields) {
            try {
                val field = this::class.java.getDeclaredField(fieldName)
                field.isAccessible = true
                val value = field.getFloat(this)
                val defaultValue = when (fieldName) {
                    "horizontalMultiplier", "verticalMultiplier" -> 0.0f
                    "motionXMultiplier", "motionYMultiplier", "motionZMultiplier" -> 1.0f
                    "reverseStrength", "smoothAmount" -> 0.0f
                    "timerSpeed" -> 1.0f
                    else -> 0.0f
                }
                if (abs(value - defaultValue) > 0.001f) count++
            } catch (e: Exception) {
                // 忽略无法访问的字段
            }
        }
        
        return min(count, 999)
    }
}