//Code By Neko
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import java.awt.Color

object NekoRainbowControl : Module("NekoRainbowControl", Category.RENDER) {
    
    // 模式选择
    val mode by choices(
        "Mode",
        arrayOf(
            "Standard",      // 标准彩虹
            "TwoColorFade",  // 双色渐变
            "SpeedControl",  // 速度控制
            "RangeControl",  // 范围控制
            "MultiColor",    // 多颜色
            "CustomColor"    // 自定义颜色模式
        ),
        "Standard"
    )
    
    // 基础颜色1
    private val color1Red by int("Color1-R", 255, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color1Green by int("Color1-G", 0, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color1Blue by int("Color1-B", 0, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color1Alpha by int("Color1-A", 255, 0..255) { 
        mode == "CustomColor" 
    }
    
    // 基础颜色2
    private val color2Red by int("Color2-R", 0, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color2Green by int("Color2-G", 0, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color2Blue by int("Color2-B", 255, 0..255) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color2Alpha by int("Color2-A", 255, 0..255) { 
        mode == "CustomColor" 
    }
    
    // 第三个颜色（多颜色模式）
    private val color3Red by int("Color3-R", 0, 0..255) { 
        mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color3Green by int("Color3-G", 255, 0..255) { 
        mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color3Blue by int("Color3-B", 0, 0..255) { 
        mode == "MultiColor" || mode == "CustomColor" 
    }
    private val color3Alpha by int("Color3-A", 255, 0..255) { 
        mode == "CustomColor" 
    }
    
    // 自定义颜色模式专用参数
    private val customColorMode by choices(
        "CustomMode",
        arrayOf(
            "Static",        // 静态颜色
            "Pulse",         // 脉冲
            "Breathing",     // 呼吸
            "Random",        // 随机
            "RainbowFade"    // 彩虹渐变
        ),
        "Static"
    ) { mode == "CustomColor" }
    
    private val customSpeed by float("CustomSpeed", 1.0f, 0.1f..5.0f) { 
        mode == "CustomColor" && customColorMode != "Static" 
    }
    
    private val customPulseWidth by float("PulseWidth", 0.5f, 0.1f..1.0f) { 
        mode == "CustomColor" && customColorMode == "Pulse" 
    }
    
    private val customRandomInterval by int("RandomInterval", 1000, 100..5000) { 
        mode == "CustomColor" && customColorMode == "Random" 
    }
    
    // 速度控制参数
    private val speed by float("Speed", 0.1f, 0.01f..2.0f) { 
        mode == "SpeedControl" || mode == "Standard" || mode == "CustomColor" 
    }
    
    // 色相范围控制（0-1表示整个色轮的范围）
    private val hueRange by float("HueRange", 1.0f, 0.1f..1.0f) { mode == "RangeControl" }
    
    // 色相起始偏移
    private val hueStart by float("HueStart", 0.0f, 0.0f..1.0f) { mode == "RangeControl" }
    
    // 饱和度控制
    private val saturation by float("Saturation", 1.0f, 0.0f..1.0f)
    
    // 亮度控制
    private val brightness by float("Brightness", 1.0f, 0.0f..1.0f)
    
    // 渐变速度
    private val fadeSpeed by float("FadeSpeed", 1.0f, 0.1f..5.0f) { 
        mode == "TwoColorFade" || mode == "MultiColor" || mode == "CustomColor" 
    }
    
    // 预置选项
    val preset by choices(
        "Preset",
        arrayOf(
            "Default",
            "FastRainbow",
            "SlowRainbow",
            "Pastel",
            "Neon",
            "Warm",
            "Cool",
            "RedBlue",
            "GreenPurple",
            "Sunset",
            "Ocean",      // 海洋色
            "Forest",     // 森林色
            "Fire",       // 火焰色
            "Ice",        // 冰蓝色
            "Sunrise",    // 日出色
            "Custom1",    // 自定义预设1
            "Custom2",    // 自定义预设2
            "Custom3"     // 自定义预设3
        ),
        "Default"
    )
    
    // 颜色预览
    private val showPreview by boolean("ShowPreview", true)
    private val previewSize by float("PreviewSize", 50.0f, 10.0f..100.0f) { showPreview }
    
    // 私有变量，避免命名冲突
    private var _currentColor = Color.WHITE
    private var fadeProgress = 0f
    private var customColorIndex = 0
    private var lastRandomTime = System.currentTimeMillis()
    private var randomColor = Color.WHITE
    
    /**
     * 重写的rainbow函数，根据模块设置返回颜色
     */
    fun rainbow(offset: Long = 400000L, alpha: Float = 1f): Color {
        // 应用预设
        applyPreset()
        
        val calculatedColor = when (mode.lowercase()) {
            "twocolorfade" -> twoColorFade(offset, alpha)
            "speedcontrol" -> speedControlRainbow(offset, alpha)
            "rangecontrol" -> rangeControlRainbow(offset, alpha)
            "multicolor" -> multiColorFade(offset, alpha)
            "customcolor" -> customColorMode(offset, alpha)
            else -> standardRainbow(offset, alpha) // 包括Standard模式
        }
        
        // 更新当前颜色
        _currentColor = calculatedColor
        return calculatedColor
    }
    
    /**
     * 标准彩虹（可调节速度）
     */
    private fun standardRainbow(offset: Long, alpha: Float): Color {
        // 调整速度：speed=0.1对应原版，speed=2.0为20倍速
        val adjustedOffset = (offset * (speed / 0.1f)).toLong()
        val hue = (System.nanoTime() + adjustedOffset) / 10000000000F % 1
        val rgb = Color.HSBtoRGB(hue, saturation, brightness)
        
        return Color(
            (rgb shr 16 and 0xFF) / 255f,
            (rgb shr 8 and 0xFF) / 255f,
            (rgb and 0xFF) / 255f,
            alpha
        )
    }
    
    /**
     * 双色渐变（增强版，支持透明度插值）
     */
    private fun twoColorFade(offset: Long, alpha: Float): Color {
        val colorA = Color(color1Red, color1Green, color1Blue, 255)
        val colorB = Color(color2Red, color2Green, color2Blue, 255)
        
        // 计算渐变进度
        val cycleTime = (10000000000L / fadeSpeed).toLong()
        val timeInCycle = (System.nanoTime() + offset) % cycleTime
        val progress = timeInCycle.toFloat() / cycleTime
        
        // 使用正弦波创建更平滑的渐变
        val smoothProgress = (Math.sin(progress * Math.PI * 2 - Math.PI / 2).toFloat() + 1) / 2
        
        // 线性插值
        return interpolateColor(
            colorA,
            colorB,
            smoothProgress,
            alpha
        )
    }
    
    /**
     * 速度控制彩虹
     */
    private fun speedControlRainbow(offset: Long, alpha: Float): Color {
        // 速度调整：速度越快，周期越短
        val adjustedOffset = (offset * speed * 10).toLong()
        val hue = (System.nanoTime() + adjustedOffset) / 10000000000F % 1
        val rgb = Color.HSBtoRGB(hue, saturation, brightness)
        
        return Color(
            (rgb shr 16 and 0xFF) / 255f,
            (rgb shr 8 and 0xFF) / 255f,
            (rgb and 0xFF) / 255f,
            alpha
        )
    }
    
    /**
     * 范围控制彩虹
     */
    private fun rangeControlRainbow(offset: Long, alpha: Float): Color {
        // 限制色相范围
        val rawHue = (System.nanoTime() + offset) / 10000000000F % 1
        val limitedHue = (rawHue * hueRange + hueStart) % 1
        val rgb = Color.HSBtoRGB(limitedHue, saturation, brightness)
        
        return Color(
            (rgb shr 16 and 0xFF) / 255f,
            (rgb shr 8 and 0xFF) / 255f,
            (rgb and 0xFF) / 255f,
            alpha
        )
    }
    
    /**
     * 多颜色渐变（三色）
     */
    private fun multiColorFade(offset: Long, alpha: Float): Color {
        val colorA = Color(color1Red, color1Green, color1Blue, 255)
        val colorB = Color(color2Red, color2Green, color2Blue, 255)
        val colorC = Color(color3Red, color3Green, color3Blue, 255)
        
        // 三色循环
        val cycleTime = (10000000000L / fadeSpeed).toLong()
        val timeInCycle = (System.nanoTime() + offset) % (cycleTime * 3)
        val segment = timeInCycle / cycleTime
        val progress = (timeInCycle % cycleTime).toFloat() / cycleTime
        
        // 使用平滑插值
        val smoothProgress = (Math.sin(progress * Math.PI * 2 - Math.PI / 2).toFloat() + 1) / 2
        
        return when (segment) {
            0L -> interpolateColor(colorA, colorB, smoothProgress, alpha)
            1L -> interpolateColor(colorB, colorC, smoothProgress, alpha)
            else -> interpolateColor(colorC, colorA, smoothProgress, alpha)
        }
    }
    
    /**
     * 自定义颜色模式
     */
    private fun customColorMode(offset: Long, alpha: Float): Color {
        val color1 = Color(color1Red, color1Green, color1Blue, color1Alpha)
        val color2 = Color(color2Red, color2Green, color2Blue, color2Alpha)
        val color3 = Color(color3Red, color3Green, color3Blue, color3Alpha)
        
        return when (customColorMode.lowercase()) {
            "pulse" -> customPulseEffect(offset, alpha, color1, color2)
            "breathing" -> customBreathingEffect(offset, alpha, color1, color2)
            "random" -> customRandomEffect(alpha, color1, color2, color3)
            "rainbowfade" -> customRainbowFade(offset, alpha, color1, color2)
            else -> customStaticColor(alpha, color1) // 静态颜色
        }
    }
    
    /**
     * 自定义脉冲效果
     */
    private fun customPulseEffect(offset: Long, alpha: Float, color1: Color, color2: Color): Color {
        val cycleTime = (10000000000L / customSpeed).toLong()
        val timeInCycle = (System.nanoTime() + offset) % cycleTime
        val progress = timeInCycle.toFloat() / cycleTime
        
        // 使用正弦波创建脉冲效果
        val pulse = Math.sin(progress * Math.PI * 2).toFloat() * 0.5f + 0.5f
        val pulseAmount = pulse * customPulseWidth
        
        return interpolateColor(color1, color2, pulseAmount, alpha)
    }
    
    /**
     * 自定义呼吸效果
     */
    private fun customBreathingEffect(offset: Long, alpha: Float, color1: Color, color2: Color): Color {
        val cycleTime = (10000000000L / customSpeed).toLong()
        val timeInCycle = (System.nanoTime() + offset) % cycleTime
        val progress = timeInCycle.toFloat() / cycleTime
        
        // 使用正弦波创建呼吸效果
        val breathe = (Math.sin(progress * Math.PI * 2 - Math.PI / 2).toFloat() + 1) / 2
        
        return interpolateColor(color1, color2, breathe, alpha)
    }
    
    /**
     * 自定义随机效果
     */
    private fun customRandomEffect(alpha: Float, color1: Color, color2: Color, color3: Color): Color {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRandomTime > customRandomInterval) {
            // 随机选择一个颜色
            val random = Math.random()
            randomColor = when {
                random < 0.33 -> color1
                random < 0.66 -> color2
                else -> color3
            }
            lastRandomTime = currentTime
        }
        
        return Color(
            randomColor.red,
            randomColor.green,
            randomColor.blue,
            (alpha * 255).toInt()
        )
    }
    
    /**
     * 自定义彩虹渐变
     */
    private fun customRainbowFade(offset: Long, alpha: Float, color1: Color, color2: Color): Color {
        // 彩虹色相
        val hue = (System.nanoTime() + offset) / (10000000000L / speed.toLong()) % 1
        val rainbowColor = Color(Color.HSBtoRGB(hue.toFloat(), saturation, brightness))
        
        // 将彩虹色与自定义颜色混合
        return blendColors(rainbowColor, color1, 0.5f, alpha)
    }
    
    /**
     * 自定义静态颜色
     */
    private fun customStaticColor(alpha: Float, color: Color): Color {
        return Color(
            color.red,
            color.green,
            color.blue,
            (alpha * color1Alpha / 255f * 255).toInt()
        )
    }
    
    /**
     * 应用预设配置
     */
    private fun applyPreset() {
        // 这里可以添加代码来自动设置颜色值
        // 例如，当选择"RedBlue"预设时，自动设置color1为红色，color2为蓝色
    }
    
    /**
     * 自定义颜色插值函数
     */
    private fun interpolateColor(start: Color, end: Color, ratio: Float, alpha: Float): Color {
        val t = ratio.coerceIn(0.0f, 1.0f)
        
        val r = (start.red + (end.red - start.red) * t).toInt()
        val g = (start.green + (end.green - start.green) * t).toInt()
        val b = (start.blue + (end.blue - start.blue) * t).toInt()
        
        // 插值Alpha通道
        val a = if (mode == "CustomColor") {
            (start.alpha + (end.alpha - start.alpha) * t).toInt()
        } else {
            (alpha * 255).toInt()
        }
        
        return Color(r, g, b, a)
    }
    
    /**
     * 颜色混合函数
     */
    private fun blendColors(color1: Color, color2: Color, ratio: Float, alpha: Float): Color {
        val t = ratio.coerceIn(0.0f, 1.0f)
        
        val r = (color1.red * (1 - t) + color2.red * t).toInt()
        val g = (color1.green * (1 - t) + color2.green * t).toInt()
        val b = (color1.blue * (1 - t) + color2.blue * t).toInt()
        
        return Color(r, g, b, (alpha * 255).toInt())
    }
    
    /**
     * 获取当前颜色
     */
    val currentColor: Color
        get() = _currentColor
    
    /**
     * 获取当前颜色RGB值
     */
    val currentRGB: Int
        get() = _currentColor.rgb
    
    /**
     * 获取带自定义Alpha的颜色
     */
    fun getColorWithAlpha(alpha: Float): Color {
        return Color(
            _currentColor.red,
            _currentColor.green,
            _currentColor.blue,
            (alpha * 255).toInt()
        )
    }
    
    /**
     * 获取颜色1
     */
    fun getColor1(): Color {
        return Color(color1Red, color1Green, color1Blue, color1Alpha)
    }
    
    /**
     * 获取颜色2
     */
    fun getColor2(): Color {
        return Color(color2Red, color2Green, color2Blue, color2Alpha)
    }
    
    /**
     * 获取颜色3
     */
    fun getColor3(): Color {
        return Color(color3Red, color3Green, color3Blue, color3Alpha)
    }
    
    /**
     * 设置自定义颜色1
     */
    fun setColor1(color: Color) {
        // 注意：这里不能直接修改设置值，但可以更新当前颜色缓存
        _currentColor = color
    }
    
    /**
     * 设置自定义颜色2
     */
    fun setColor2(color: Color) {
        // 注意：这里不能直接修改设置值，但可以更新当前颜色缓存
        // 实际项目中需要通过修改模块设置值来实现
    }
    
    /**
     * 直接调用方法，兼容原ColorUtils.rainbow()调用
     */
    fun getRainbow(offset: Long = 400000L, alpha: Float = 1f): Color {
        return rainbow(offset, alpha)
    }
    
    /**
     * 获取TwoColorFade的当前进度（0-1）
     */
    fun getFadeProgress(): Float {
        return fadeProgress
    }
    
    /**
     * 快速获取双色渐变的中间颜色
     */
    fun getTwoColorBlend(ratio: Float = 0.5f, alpha: Float = 1f): Color {
        val color1 = Color(color1Red, color1Green, color1Blue, color1Alpha)
        val color2 = Color(color2Red, color2Green, color2Blue, color2Alpha)
        return interpolateColor(color1, color2, ratio, alpha)
    }
    
    override val tag: String
        get() = "$mode ($preset)"
}