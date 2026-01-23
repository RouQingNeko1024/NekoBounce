/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

//NekoAi Code
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isLookingOnEntities
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isBlock
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.EnumAction
import net.minecraft.item.ItemBlock
import kotlin.random.Random

object AutoClicker2 : Module("AutoClicker2", Category.COMBAT) {

    // ==================== 全局随机算法设置 ====================
    private val mode by choices(
        "Mode",
        arrayOf(
            "MersenneTwister",
            "Ranlux24",
            "MinStandard",
            "DiscardBlock",
            "IndependentBits",
            "ShuffleOrder",
            "LinearCongruential",
            "SubtractWithCarry",
            "LiquidBounceDefault"
        ),
        "LiquidBounceDefault"
    )
    
    private val useEntropy by boolean("UseEntropy", true)
    private val entropyFactor by float("EntropyFactor", 0.15f, 0.01f..1.0f) { useEntropy }
    private val entropyUpdateRate by int("EntropyRate", 3, 1..10) { useEntropy }
    
    // ==================== 点击分布设置 ====================
    private val distributionMode by choices(
        "Distribution",
        arrayOf("Binomial", "Uniform", "Geometric", "Poisson", "Exponential", "Normal"),
        "Uniform"
    )
    
    // 二项分布参数
    private val binomialTrials by int("Binom-Trials", 10, 1..100) { distributionMode == "Binomial" }
    private val binomialProb by float("Binom-Prob", 0.5f, 0.1f..0.9f) { distributionMode == "Binomial" }
    
    // 几何分布参数
    private val geometricProb by float("Geom-Prob", 0.3f, 0.1f..0.9f) { distributionMode == "Geometric" }
    
    // 泊松分布参数
    private val poissonLambda by float("Poisson-Lambda", 5f, 0.5f..20f) { distributionMode == "Poisson" }
    
    // 指数分布参数
    private val exponentialLambda by float("Exp-Lambda", 0.5f, 0.1f..2f) { distributionMode == "Exponential" }
    
    // 正态分布参数
    private val normalMean by float("Normal-Mean", 8f, 1f..20f) { distributionMode == "Normal" }
    private val normalStdDev by float("Normal-StdDev", 2f, 0.5f..5f) { distributionMode == "Normal" }
    
    // ==================== 最小延迟/最大延迟随机化设置 ====================
    private val minDelayRandomization by boolean("MinDelayRandom", true)
    private val minDelayRandomFactor by float("MinDelayRandomFactor", 0.3f, 0.1f..1.0f) { minDelayRandomization }
    private val minDelayVariation by int("MinDelayVariation", 10, 0..50) { minDelayRandomization }
    
    private val maxDelayRandomization by boolean("MaxDelayRandom", true)
    private val maxDelayRandomFactor by float("MaxDelayRandomFactor", 0.25f, 0.1f..1.0f) { maxDelayRandomization }
    private val maxDelayVariation by int("MaxDelayVariation", 15, 0..50) { maxDelayRandomization }
    
    // ==================== 抖动强度随机化设置 ====================
    private val jitterStrengthRandomization by boolean("JitterStrengthRandom", true)
    private val jitterStrengthRandomFactor by float("JitterStrengthRandomFactor", 0.4f, 0.1f..1.0f) { jitterStrengthRandomization }
    private val jitterStrengthVariation by float("JitterStrengthVariation", 0.5f, 0.1f..2.0f) { jitterStrengthRandomization }
    
    // ==================== 时间变化随机化设置 ====================
    private val timeVariationRandomization by boolean("TimeVariationRandom", true)
    private val timeVariationRandomFactor by float("TimeVariationRandomFactor", 0.35f, 0.1f..1.0f) { timeVariationRandomization }
    private val timeVariationVariation by int("TimeVariationVariation", 3, 1..10) { timeVariationRandomization }
    
    // ==================== 左键独立随机化设置 ====================
    private val left by boolean("Left", true)
    
    // 左键CPS设置
    private val leftCPS by intRange("L-CPS", 8..12, 1..50) { left }
    private val leftCPSRandomization by boolean("L-CPSRandom", true) { left }
    private val leftCPSRandomFactor by float("L-CPSFactor", 0.25f, 0.1f..1.0f) { left && leftCPSRandomization }
    private val leftCPSTimeVariation by int("L-CPSTimeVar", 2, 1..10) { left }
    
    // 左键延迟随机化
    private val leftDelayRandomization by boolean("L-DelayRandom", true) { left }
    private val leftDelayRandomFactor by float("L-DelayFactor", 0.2f, 0.05f..0.5f) { left && leftDelayRandomization }
    private val leftMinDelay by int("L-MinDelay", 10, 0..100) { left }
    private val leftMaxDelay by int("L-MaxDelay", 100, 0..500) { left }
    
    // 左键抖动设置
    private val leftJitter by boolean("L-Jitter", false) { left }
    private val leftJitterStrength by float("L-JitterStr", 1.5f, 0.1f..3.0f) { left && leftJitter }
    private val leftJitterStrengthRandom by boolean("L-JitterStrRandom", true) { left && leftJitter }
    private val leftJitterFrequency by float("L-JitterFreq", 0.5f, 0.1f..1.0f) { left && leftJitter }
    private val leftJitterXRandom by boolean("L-JitterX", true) { left && leftJitter }
    private val leftJitterYRandom by boolean("L-JitterY", true) { left && leftJitter }
    
    // 左键时间变化设置
    private val leftTimeVariation by int("L-TimeVariation", 2, 1..10) { left }
    private val leftTimeVariationRandom by boolean("L-TimeVarRandom", true) { left }
    
    // 左键点击时机随机化
    private val leftTimingRandom by boolean("L-TimingRandom", true) { left }
    private val leftPreClickDelay by int("L-PreDelay", 0, 0..50) { left && leftTimingRandom }
    private val leftPostClickDelay by int("L-PostDelay", 0, 0..50) { left && leftTimingRandom }
    private val leftClickDuration by int("L-ClickDur", 5, 1..20) { left }
    private val leftClickDurationRandom by boolean("L-ClickDurRandom", true) { left }
    
    // 左键算法设置
    private val leftAlgorithmOverride by boolean("L-AlgoOverride", false) { left }
    private val leftAlgorithm by choices(
        "L-Algorithm",
        arrayOf("SameAsGlobal", "MersenneTwister", "Ranlux24", "LinearCongruential"),
        "SameAsGlobal"
    ) { left && leftAlgorithmOverride }
    
    // ==================== 右键独立随机化设置 ====================
    private val right by boolean("Right", true)
    
    // 右键CPS设置
    private val rightCPS by intRange("R-CPS", 6..10, 1..50) { right }
    private val rightCPSRandomization by boolean("R-CPSRandom", true) { right }
    private val rightCPSRandomFactor by float("R-CPSFactor", 0.2f, 0.1f..1.0f) { right && rightCPSRandomization }
    private val rightCPSTimeVariation by int("R-CPSTimeVar", 3, 1..10) { right }
    
    // 右键延迟随机化
    private val rightDelayRandomization by boolean("R-DelayRandom", true) { right }
    private val rightDelayRandomFactor by float("R-DelayFactor", 0.18f, 0.05f..0.5f) { right && rightDelayRandomization }
    private val rightMinDelay by int("R-MinDelay", 15, 0..100) { right }
    private val rightMaxDelay by int("R-MaxDelay", 80, 0..500) { right }
    
    // 右键抖动设置
    private val rightJitter by boolean("R-Jitter", false) { right }
    private val rightJitterStrength by float("R-JitterStr", 1.2f, 0.1f..3.0f) { right && rightJitter }
    private val rightJitterStrengthRandom by boolean("R-JitterStrRandom", true) { right && rightJitter }
    private val rightJitterFrequency by float("R-JitterFreq", 0.4f, 0.1f..1.0f) { right && rightJitter }
    private val rightJitterXRandom by boolean("R-JitterX", true) { right && rightJitter }
    private val rightJitterYRandom by boolean("R-JitterY", true) { right && rightJitter }
    
    // 右键时间变化设置
    private val rightTimeVariation by int("R-TimeVariation", 3, 1..10) { right }
    private val rightTimeVariationRandom by boolean("R-TimeVarRandom", true) { right }
    
    // 右键点击时机随机化
    private val rightTimingRandom by boolean("R-TimingRandom", true) { right }
    private val rightPreClickDelay by int("R-PreDelay", 0, 0..50) { right && rightTimingRandom }
    private val rightPostClickDelay by int("R-PostDelay", 0, 0..50) { right && rightTimingRandom }
    private val rightClickDuration by int("R-ClickDur", 8, 1..20) { right }
    private val rightClickDurationRandom by boolean("R-ClickDurRandom", true) { right }
    
    // 右键算法设置
    private val rightAlgorithmOverride by boolean("R-AlgoOverride", false) { right }
    private val rightAlgorithm by choices(
        "R-Algorithm",
        arrayOf("SameAsGlobal", "MinStandard", "DiscardBlock", "ShuffleOrder"),
        "SameAsGlobal"
    ) { right && rightAlgorithmOverride }
    
    // ==================== 高级随机化设置 ====================
    private val adaptiveRandomization by boolean("AdaptiveRandom", true)
    private val adaptiveFactor by float("AdaptiveFactor", 0.3f, 0.1f..0.8f) { adaptiveRandomization }
    
    private val patternAvoidance by boolean("PatternAvoid", true)
    private val patternHistorySize by int("PatternHistory", 20, 5..50) { patternAvoidance }
    private val patternThreshold by float("PatternThreshold", 0.7f, 0.3f..0.9f) { patternAvoidance }
    
    private val noiseInjection by boolean("NoiseInjection", false)
    private val noiseAmount by float("NoiseAmount", 0.05f, 0.01f..0.2f) { noiseInjection }
    private val noiseFrequency by int("NoiseFreq", 5, 1..20) { noiseInjection }
    
    // ==================== 双击模拟设置 ====================
    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false)
    private val doubleClickProbability by float("DoubleClickProb", 0.1f, 0f..0.5f) { simulateDoubleClicking }
    private val doubleClickDelay by int("DoubleClickDelay", 25, 10..100) { simulateDoubleClicking }
    private val doubleClickDelayRandom by boolean("DoubleClickDelayRandom", true) { simulateDoubleClicking }
    
    private val hurtTime by int("HurtTime", 10, 0..10) { left }
    
    // ==================== 目标检测设置 ====================
    private val requiresNoInput by boolean("RequiresNoInput", false) { left }
    private val maxAngleDifference by float("MaxAngleDifference", 30f, 10f..180f) { left && requiresNoInput }
    private val range by float("Range", 3f, 0.1f..5f) { left && requiresNoInput }
    
    private val onlyBlocks by boolean("OnlyBlocks", true) { right }
    private val block by boolean("AutoBlock", false) { left }
    private val blockDelay by int("BlockDelay", 50, 0..100) { block }
    private val blockDelayRandom by boolean("BlockDelayRandom", true) { block }
    private val blockDelayVariation by int("BlockDelayVariation", 25, 0..50) { block && blockDelayRandom }
    
    // ==================== 私有变量 ====================
    private var rightDelay = 0L
    private var rightLastSwing = 0L
    private var leftDelay = 0L
    private var leftLastSwing = 0L
    private var lastBlocking = 0L
    private var blockDelayCurrent = blockDelay
    
    private var leftCurrentMinDelay = leftMinDelay
    private var leftCurrentMaxDelay = leftMaxDelay
    private var leftCurrentJitterStrength = leftJitterStrength
    private var leftCurrentTimeVariation = leftTimeVariation
    
    private var rightCurrentMinDelay = rightMinDelay
    private var rightCurrentMaxDelay = rightMaxDelay
    private var rightCurrentJitterStrength = rightJitterStrength
    private var rightCurrentTimeVariation = rightTimeVariation
    
    private var shouldJitter = false
    private var target: EntityLivingBase? = null
    
    private var randomSeed = System.currentTimeMillis().toInt()
    private var entropyCounter = 0L
    private var adaptiveCounter = 0
    
    private val clickHistory = mutableListOf<Long>()
    private val patternHistory = mutableListOf<Int>()
    
    private val shouldAutoClick: Boolean
        get() = mc.thePlayer?.let { !it.capabilities.isCreativeMode } ?: false

    // ==================== 随机算法实现 ====================
    private fun getRandomAlgorithm(button: String): Random {
        val effectiveAlgorithm = when (button) {
            "left" -> if (leftAlgorithmOverride && leftAlgorithm != "SameAsGlobal") leftAlgorithm else mode
            "right" -> if (rightAlgorithmOverride && rightAlgorithm != "SameAsGlobal") rightAlgorithm else mode
            else -> mode
        }
        
        var seed = randomSeed + entropyCounter.toInt()
        if (adaptiveRandomization) {
            seed = seed xor (adaptiveCounter * 1000)
        }
        if (noiseInjection && entropyCounter % noiseFrequency == 0L) {
            seed = seed xor (System.nanoTime().toInt() and 0xFF)
        }
        
        return createRandomInstance(effectiveAlgorithm, seed)
    }
    
    private fun createRandomInstance(algorithm: String, seed: Int): Random {
        return when (algorithm) {
            "MersenneTwister" -> Random(seed)
            "Ranlux24" -> Random(seed xor 0x12345678)
            "MinStandard" -> Random(((seed.toLong() * 16807) % 2147483647).toInt())
            "DiscardBlock" -> Random(seed)
            "IndependentBits" -> Random(seed xor 0xABCDEF)
            "ShuffleOrder" -> Random(seed)
            "LinearCongruential" -> Random(((seed.toLong() * 1103515245 + 12345) % 2147483647).toInt())
            "SubtractWithCarry" -> Random(seed xor 0x876543)
            else -> Random(seed)
        }
    }
    
    // ==================== 分布算法实现 ====================
    private fun getDistributedValue(random: Random, button: String): Double {
        val cpsRange = when (button) {
            "left" -> leftCPS
            "right" -> rightCPS
            else -> 8..12
        }
        val center = (cpsRange.first + cpsRange.last) / 2
        
        return when (distributionMode) {
            "Binomial" -> {
                var successes = 0
                repeat(binomialTrials) {
                    if (random.nextDouble() < binomialProb.toDouble()) successes++
                }
                center + (successes - binomialTrials / 2) * 0.1
            }
            "Geometric" -> {
                val geometricValue = Math.log(1.0 - random.nextDouble()) / Math.log(1.0 - geometricProb.toDouble())
                center + geometricValue * 0.05
            }
            "Poisson" -> {
                var k = 0
                var p = 1.0
                val L = Math.exp(-poissonLambda.toDouble())
                
                do {
                    k++
                    p *= random.nextDouble()
                } while (p > L)
                
                center + (k - poissonLambda) * 0.1
            }
            "Exponential" -> {
                val exponentialValue = -Math.log(random.nextDouble()) / exponentialLambda.toDouble()
                center + exponentialValue * 0.05
            }
            "Normal" -> {
                val u1 = random.nextDouble()
                val u2 = random.nextDouble()
                val z = Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2 * Math.PI * u2)
                center + (z * normalStdDev.toDouble())
            }
            else -> center + random.nextDouble() * (cpsRange.last - cpsRange.first)
        }
    }
    
    // ==================== 延迟随机化实现 ====================
    private fun updateDynamicParameters() {
        val globalRandom = getRandomAlgorithm("global")
        
        // 更新最小延迟
        if (minDelayRandomization && globalRandom.nextBoolean()) {
            val random = getRandomAlgorithm("left")
            val leftMinDelayVariation = random.nextInt(minDelayVariation * 2 + 1) - minDelayVariation
            leftCurrentMinDelay = maxOf(0, leftMinDelay + leftMinDelayVariation)
            
            val random2 = getRandomAlgorithm("right")
            val rightMinDelayVariation = random2.nextInt(minDelayVariation * 2 + 1) - minDelayVariation
            rightCurrentMinDelay = maxOf(0, rightMinDelay + rightMinDelayVariation)
        }
        
        // 更新最大延迟
        if (maxDelayRandomization && globalRandom.nextBoolean()) {
            val random = getRandomAlgorithm("left")
            val leftMaxDelayVariation = random.nextInt(maxDelayVariation * 2 + 1) - maxDelayVariation
            leftCurrentMaxDelay = maxOf(leftCurrentMinDelay + 1, leftMaxDelay + leftMaxDelayVariation)
            
            val random2 = getRandomAlgorithm("right")
            val rightMaxDelayVariation = random2.nextInt(maxDelayVariation * 2 + 1) - maxDelayVariation
            rightCurrentMaxDelay = maxOf(rightCurrentMinDelay + 1, rightMaxDelay + rightMaxDelayVariation)
        }
        
        // 更新抖动强度
        if (jitterStrengthRandomization && globalRandom.nextBoolean()) {
            val random = getRandomAlgorithm("left")
            val leftJitterVariation = random.nextDouble(-jitterStrengthVariation.toDouble(), jitterStrengthVariation.toDouble())
            leftCurrentJitterStrength = (leftJitterStrength + leftJitterVariation).toFloat()
            leftCurrentJitterStrength = maxOf(0.1f, minOf(3.0f, leftCurrentJitterStrength))
            
            val random2 = getRandomAlgorithm("right")
            val rightJitterVariation = random2.nextDouble(-jitterStrengthVariation.toDouble(), jitterStrengthVariation.toDouble())
            rightCurrentJitterStrength = (rightJitterStrength + rightJitterVariation).toFloat()
            rightCurrentJitterStrength = maxOf(0.1f, minOf(3.0f, rightCurrentJitterStrength))
        }
        
        // 更新时间变化
        if (timeVariationRandomization && globalRandom.nextBoolean()) {
            val random = getRandomAlgorithm("left")
            leftCurrentTimeVariation = leftTimeVariation + random.nextInt(timeVariationVariation * 2 + 1) - timeVariationVariation
            leftCurrentTimeVariation = maxOf(1, minOf(10, leftCurrentTimeVariation))
            
            val random2 = getRandomAlgorithm("right")
            rightCurrentTimeVariation = rightTimeVariation + random2.nextInt(timeVariationVariation * 2 + 1) - timeVariationVariation
            rightCurrentTimeVariation = maxOf(1, minOf(10, rightCurrentTimeVariation))
        }
    }
    
    private fun getRandomizedDelay(baseDelay: Long, button: String): Long {
        val random = getRandomAlgorithm(button)
        val randomFactor = when (button) {
            "left" -> if (leftDelayRandomization) leftDelayRandomFactor else 0f
            "right" -> if (rightDelayRandomization) rightDelayRandomFactor else 0f
            else -> 0f
        }
        
        if (randomFactor == 0f) return baseDelay
        
        val variation = (baseDelay * randomFactor.toDouble()).toInt()
        val minDelay = when (button) {
            "left" -> leftCurrentMinDelay
            "right" -> rightCurrentMinDelay
            else -> 0
        }
        val maxDelay = when (button) {
            "left" -> leftCurrentMaxDelay
            "right" -> rightCurrentMaxDelay
            else -> Int.MAX_VALUE
        }
        
        // 修复：使用正确的nextInt调用方式
        val randomOffset = if (variation > 0) {
            random.nextInt(variation * 2 + 1) - variation
        } else {
            0
        }
        
        var result = maxOf(minDelay, (baseDelay + randomOffset).toInt())
        result = minOf(maxDelay, result)
        
        // 模式避免
        if (patternAvoidance && isPatternDetected(result.toLong())) {
            result = adjustForPatternAvoidance(result.toLong(), random).toInt()
        }
        
        return result.toLong()
    }
    
    private fun getRandomizedCPS(button: String): IntRange {
        val cpsRange = when (button) {
            "left" -> leftCPS
            "right" -> rightCPS
            else -> 8..12
        }
        
        val randomFactor = when (button) {
            "left" -> if (leftCPSRandomization) leftCPSRandomFactor else 0f
            "right" -> if (rightCPSRandomization) rightCPSRandomFactor else 0f
            else -> 0f
        }
        
        if (randomFactor == 0f) return cpsRange
        
        val random = getRandomAlgorithm(button)
        val center = (cpsRange.first + cpsRange.last) / 2
        val variation = (center * randomFactor.toDouble()).toInt()
        
        val timeVariation = when (button) {
            "left" -> if (leftTimeVariationRandom) leftCurrentTimeVariation else leftTimeVariation
            "right" -> if (rightTimeVariationRandom) rightCurrentTimeVariation else rightTimeVariation
            else -> 2
        }
        
        val dynamicVariation = variation + random.nextInt(timeVariation * 2 + 1) - timeVariation
        val minCPS = maxOf(1, center - dynamicVariation)
        val maxCPS = minOf(50, center + dynamicVariation)
        
        return minCPS..maxCPS
    }
    
    private fun getJitterStrength(button: String): Float {
        val baseStrength = when (button) {
            "left" -> leftJitterStrength
            "right" -> rightJitterStrength
            else -> 1.0f
        }
        
        val useRandom = when (button) {
            "left" -> leftJitterStrengthRandom
            "right" -> rightJitterStrengthRandom
            else -> false
        }
        
        if (!useRandom) return baseStrength
        
        val random = getRandomAlgorithm(button)
        val randomFactor = if (jitterStrengthRandomization) jitterStrengthRandomFactor else 0f
        
        if (randomFactor == 0f) return baseStrength
        
        val variation = baseStrength * randomFactor
        val randomOffset = random.nextDouble(-variation.toDouble(), variation.toDouble()).toFloat()
        val result = baseStrength + randomOffset
        
        return maxOf(0.1f, minOf(3.0f, result))
    }
    
    // ==================== 模式检测与避免 ====================
    private fun isPatternDetected(delay: Long): Boolean {
        clickHistory.add(delay)
        if (clickHistory.size > patternHistorySize) {
            clickHistory.removeAt(0)
        }
        
        if (clickHistory.size < 5) return false
        
        val pattern = clickHistory.takeLast(5).map { it % 10 }
        patternHistory.add(pattern.hashCode())
        
        if (patternHistory.size > 10) {
            patternHistory.removeAt(0)
            val frequency = patternHistory.groupingBy { it }.eachCount()
            val maxFreq = frequency.values.maxOrNull() ?: 0
            val patternRatio = maxFreq.toFloat() / patternHistory.size
            
            return patternRatio > patternThreshold
        }
        
        return false
    }
    
    private fun adjustForPatternAvoidance(delay: Long, random: Random): Long {
        // 修复：使用正确的nextInt调用方式
        val adjustmentRange = (delay / 2).toInt()
        val adjustment = if (adjustmentRange > 0) {
            random.nextInt(adjustmentRange * 2 + 1) - adjustmentRange
        } else {
            0
        }
        return maxOf(10, delay + adjustment)
    }
    
    private fun applyEntropy() {
        if (!useEntropy) return
        
        entropyCounter++
        if (entropyCounter % entropyUpdateRate == 0L) {
            randomSeed = (randomSeed + (entropyFactor * 1000).toInt()) xor entropyCounter.toInt()
            adaptiveCounter = (adaptiveCounter + 1) % 1000
        }
    }
    
    private fun applyTimingDelay(button: String, time: Long): Boolean {
        val preDelay = when (button) {
            "left" -> if (leftTimingRandom) leftPreClickDelay else 0
            "right" -> if (rightTimingRandom) rightPreClickDelay else 0
            else -> 0
        }
        
        val postDelay = when (button) {
            "left" -> if (leftTimingRandom) leftPostClickDelay else 0
            "right" -> if (rightTimingRandom) rightPostClickDelay else 0
            else -> 0
        }
        
        val totalDelay = preDelay + postDelay
        if (totalDelay == 0) return true
        
        val random = getRandomAlgorithm(button)
        return random.nextInt(100) > totalDelay
    }
    
    private fun getClickDuration(button: String): Int {
        val baseDuration = when (button) {
            "left" -> leftClickDuration
            "right" -> rightClickDuration
            else -> 5
        }
        
        val useRandom = when (button) {
            "left" -> leftClickDurationRandom
            "right" -> rightClickDurationRandom
            else -> false
        }
        
        if (!useRandom) return baseDuration
        
        val random = getRandomAlgorithm(button)
        val variation = random.nextInt(5) - 2 // -2到+2的范围
        return maxOf(1, baseDuration + variation)
    }
    
    private fun getDoubleClickDelay(): Long {
        var delay = doubleClickDelay.toLong()
        if (doubleClickDelayRandom) {
            val random = getRandomAlgorithm("global")
            val variation = random.nextInt(11) - 5 // -5到+5的范围
            delay += variation
        }
        return delay
    }

    override fun onEnable() {
        randomSeed = System.currentTimeMillis().toInt()
        entropyCounter = 0L
        adaptiveCounter = 0
        blockDelayCurrent = blockDelay
        
        leftCurrentMinDelay = leftMinDelay
        leftCurrentMaxDelay = leftMaxDelay
        leftCurrentJitterStrength = leftJitterStrength
        leftCurrentTimeVariation = leftTimeVariation
        
        rightCurrentMinDelay = rightMinDelay
        rightCurrentMaxDelay = rightMaxDelay
        rightCurrentJitterStrength = rightJitterStrength
        rightCurrentTimeVariation = rightTimeVariation
        
        clickHistory.clear()
        patternHistory.clear()
        
        // 初始化延迟
        rightDelay = generateNewClickTime("right")
        leftDelay = generateNewClickTime("left")
    }

    override fun onDisable() {
        rightLastSwing = 0L
        leftLastSwing = 0L
        lastBlocking = 0L
        target = null
        clickHistory.clear()
        patternHistory.clear()
    }

    val onAttack = handler<AttackEvent> { event ->
        if (!left) return@handler
        val targetEntity = event.targetEntity as? EntityLivingBase ?: return@handler
        
        target = targetEntity
        applyEntropy()
        updateDynamicParameters()
    }

    val onRender3D = handler<Render3DEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val time = System.currentTimeMillis()
        
        // 动态更新参数
        updateDynamicParameters()
        
        // 检查是否应该模拟双击
        val shouldDoubleClick = if (simulateDoubleClicking) {
            val random = getRandomAlgorithm("global")
            random.nextDouble() < doubleClickProbability.toDouble()
        } else false
        
        if (block && thePlayer.swingProgress > 0 && !mc.gameSettings.keyBindUseItem.isKeyDown) {
            mc.gameSettings.keyBindUseItem.pressTime = 0
        }

        // 右键点击处理
        if (right && mc.gameSettings.keyBindUseItem.isKeyDown && time - rightLastSwing >= rightDelay) {
            if (!onlyBlocks || thePlayer.heldItem?.item is ItemBlock) {
                if (applyTimingDelay("right", time)) {
                    handleRightClick(time, shouldDoubleClick)
                }
            }
        }

        // 左键点击处理
        if (requiresNoInput) {
            val nearbyEntity = getNearestEntityInRange() ?: return@handler
            if (!isLookingOnEntities(nearbyEntity, maxAngleDifference.toDouble())) return@handler

            if (left && shouldAutoClick && time - leftLastSwing >= leftDelay) {
                if (applyTimingDelay("left", time)) {
                    handleLeftClick(time, shouldDoubleClick)
                }
            } else if (block && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && 
                       shouldAutoRightClick() && mc.gameSettings.keyBindAttack.pressTime != 0) {
                handleBlock(time)
            }
        } else {
            if (left && mc.gameSettings.keyBindAttack.isKeyDown && 
                !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && 
                time - leftLastSwing >= leftDelay) {
                if (applyTimingDelay("left", time)) {
                    handleLeftClick(time, shouldDoubleClick)
                }
            } else if (block && mc.gameSettings.keyBindAttack.isKeyDown && 
                       !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick && 
                       shouldAutoRightClick() && mc.gameSettings.keyBindAttack.pressTime != 0) {
                handleBlock(time)
            }
        }
    }

    val onTick = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        shouldJitter = !mc.objectMouseOver.typeOfHit.isBlock &&
                (thePlayer.isSwingInProgress || mc.gameSettings.keyBindAttack.pressTime != 0)

        // 左键抖动
        if (left && leftJitter && shouldJitter && shouldAutoClick) {
            val random = getRandomAlgorithm("left")
            if (random.nextDouble() < leftJitterFrequency.toDouble()) {
                val jitterStr = getJitterStrength("left")
                if (leftJitterXRandom) {
                    thePlayer.fixedSensitivityYaw += random.nextDouble(-jitterStr.toDouble(), jitterStr.toDouble()).toFloat()
                }
                if (leftJitterYRandom) {
                    thePlayer.fixedSensitivityPitch += random.nextDouble(-jitterStr.toDouble(), jitterStr.toDouble()).toFloat()
                }
            }
        }
        
        // 右键抖动
        if (right && rightJitter && !thePlayer.isUsingItem && mc.gameSettings.keyBindUseItem.isKeyDown &&
            ((onlyBlocks && thePlayer.heldItem?.item is ItemBlock) || !onlyBlocks)) {
            val random = getRandomAlgorithm("right")
            if (random.nextDouble() < rightJitterFrequency.toDouble()) {
                val jitterStr = getJitterStrength("right")
                if (rightJitterXRandom) {
                    thePlayer.fixedSensitivityYaw += random.nextDouble(-jitterStr.toDouble(), jitterStr.toDouble()).toFloat()
                }
                if (rightJitterYRandom) {
                    thePlayer.fixedSensitivityPitch += random.nextDouble(-jitterStr.toDouble(), jitterStr.toDouble()).toFloat()
                }
            }
        }
    }

    // 修复：简化实体查找逻辑
    private fun getNearestEntityInRange(): Entity? {
        val player = mc.thePlayer ?: return null
        val world = mc.theWorld ?: return null
        
        return world.loadedEntityList
            .filterIsInstance<EntityLivingBase>()
            .filter { entity ->
                isSelected(entity, true) && player.getDistanceToEntityBox(entity) <= range
            }
            .minByOrNull { player.getDistanceToEntityBox(it) }
    }

    private fun shouldAutoRightClick(): Boolean {
        return mc.thePlayer?.heldItem?.itemUseAction == EnumAction.BLOCK
    }

    private fun handleLeftClick(time: Long, shouldDoubleClick: Boolean) {
        if (target != null && target!!.hurtTime > hurtTime) return
        
        val actualDelay = getRandomizedDelay(leftDelay, "left")
        if (time - leftLastSwing < actualDelay) return
        
        val clickCount = if (shouldDoubleClick) 2 else 1
        val random = getRandomAlgorithm("left")
        
        repeat(clickCount) { clickIndex ->
            if (clickIndex > 0) {
                Thread.sleep(getDoubleClickDelay() + (random.nextInt(11) - 5)) // -5到+5的随机延迟
            }
            
            val duration = getClickDuration("left")
            simulateClickWithDuration(mc.gameSettings.keyBindAttack.keyCode, duration)
            
            if (clickIndex == 0) {
                leftLastSwing = time
                leftDelay = generateNewClickTime("left")
                applyEntropy()
            }
        }
    }

    private fun handleRightClick(time: Long, shouldDoubleClick: Boolean) {
        val actualDelay = getRandomizedDelay(rightDelay, "right")
        if (time - rightLastSwing < actualDelay) return
        
        val clickCount = if (shouldDoubleClick) 2 else 1
        val random = getRandomAlgorithm("right")
        
        repeat(clickCount) { clickIndex ->
            if (clickIndex > 0) {
                Thread.sleep(getDoubleClickDelay() + (random.nextInt(11) - 5)) // -5到+5的随机延迟
            }
            
            val duration = getClickDuration("right")
            simulateClickWithDuration(mc.gameSettings.keyBindUseItem.keyCode, duration)
            
            if (clickIndex == 0) {
                rightLastSwing = time
                rightDelay = generateNewClickTime("right")
                applyEntropy()
            }
        }
    }

    private fun simulateClickWithDuration(keyCode: Int, duration: Int) {
        KeyBinding.onTick(keyCode)
        try {
            Thread.sleep(duration.toLong())
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        KeyBinding.setKeyBindState(keyCode, false)
    }

    private fun handleBlock(time: Long) {
        if (blockDelayRandom && time - lastBlocking >= blockDelayCurrent) {
            val random = getRandomAlgorithm("global")
            val variation = random.nextInt(blockDelayVariation * 2 + 1) - blockDelayVariation
            blockDelayCurrent = maxOf(0, blockDelay + variation)
        }
        
        if (time - lastBlocking >= blockDelayCurrent) {
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            lastBlocking = time
            applyEntropy()
        }
    }

    private fun generateNewClickTime(button: String): Long {
        val cpsRange = getRandomizedCPS(button)
        val random = getRandomAlgorithm(button)
        
        // 修复：使用正确的方式从IntRange中获取随机值
        val cps = when (distributionMode) {
            "Binomial", "Geometric", "Poisson", "Exponential", "Normal" -> getDistributedValue(random, button).toInt()
            else -> cpsRange.first + random.nextInt(cpsRange.last - cpsRange.first + 1)
        }
        
        val clampedCPS = maxOf(1, minOf(50, cps))
        val baseDelay = maxOf(1, (1000.0 / clampedCPS).toLong())
        return getRandomizedDelay(baseDelay, button)
    }
    
    override val tag: String
        get() = "${super.tag} [L:${leftCPS.first}-${leftCPS.last} R:${rightCPS.first}-${rightCPS.last}]"
}