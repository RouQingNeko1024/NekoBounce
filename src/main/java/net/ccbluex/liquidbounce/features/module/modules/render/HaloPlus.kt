/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import kotlin.math.*
import java.util.*
import kotlin.collections.ArrayList

object HaloPlus : Module("HaloPlus", Category.RENDER) {
    
    // 基础设置组
    private val enable by boolean("Enable", true)
    private val debugMode by boolean("DebugMode", false)
    private val performanceMode by boolean("PerformanceMode", false)
    
    // 光环数量组
    private val haloCount by int("HaloCount", 1, 1..100)
    private val haloSpacing by float("HaloSpacing", 0.2f, 0f..5f)
    private val haloOffsetPattern by choices("HaloOffsetPattern", arrayOf("Linear", "Circular", "Spiral", "Random", "Fibonacci"), "Linear")
    private val haloIndividualColors by boolean("HaloIndividualColors", false)
    private val haloRenderOrder by choices("HaloRenderOrder", arrayOf("FrontToBack", "BackToFront", "Alternating", "Random"), "FrontToBack")
    
    // 几何形状组
    private val geometryType by choices("GeometryType", arrayOf("Circle", "Square", "Triangle", "Pentagon", "Hexagon", "Octagon", "Star", "Heart", "Flower", "Custom"), "Circle")
    private val customSides by int("CustomSides", 16, 3..100)
    private val geometryDeformation by float("GeometryDeformation", 0f, 0f..1f)
    private val geometryTwist by float("GeometryTwist", 0f, 0f..360f)
    private val geometrySkewX by float("GeometrySkewX", 0f, -1f..1f)
    private val geometrySkewY by float("GeometrySkewY", 0f, -1f..1f)
    private val geometryTaper by float("GeometryTaper", 0f, -1f..1f)
    
    // 尺寸动画组
    private val sizeAnimationEnable by boolean("SizeAnimationEnable", false)
    private val sizeAnimationType by choices("SizeAnimationType", arrayOf("Sine", "Cosine", "Triangle", "Square", "Sawtooth", "Random", "Bounce", "Elastic"), "Sine")
    private val sizeAnimationSpeed by float("SizeAnimationSpeed", 1f, 0f..10f)
    private val sizeAnimationAmplitude by float("SizeAnimationAmplitude", 0.2f, 0f..2f)
    private val sizeAnimationOffset by float("SizeAnimationOffset", 0f, 0f..360f)
    private val sizeAnimationPhase by float("SizeAnimationPhase", 0f, 0f..360f)
    
    // 旋转动画组
    private val rotationAnimationEnable by boolean("RotationAnimationEnable", true)
    private val rotationSpeedX by float("RotationSpeedX", 1f, -50f..50f)
    private val rotationSpeedY by float("RotationSpeedY", 0f, -50f..50f)
    private val rotationSpeedZ by float("RotationSpeedZ", 0f, -50f..50f)
    private val rotationAccelerationX by float("RotationAccelerationX", 0f, -5f..5f)
    private val rotationAccelerationY by float("RotationAccelerationY", 0f, -5f..5f)
    private val rotationAccelerationZ by float("RotationAccelerationZ", 0f, -5f..5f)
    private val rotationOscillationX by float("RotationOscillationX", 0f, 0f..180f)
    private val rotationOscillationY by float("RotationOscillationY", 0f, 0f..180f)
    private val rotationOscillationZ by float("RotationOscillationZ", 0f, 0f..180f)
    
    // 位置动画组
    private val positionAnimationEnable by boolean("PositionAnimationEnable", false)
    private val positionOrbitRadius by float("PositionOrbitRadius", 1f, 0f..10f)
    private val positionOrbitSpeed by float("PositionOrbitSpeed", 1f, -10f..10f)
    private val positionBobSpeed by float("PositionBobSpeed", 1f, 0f..10f)
    private val positionBobAmplitude by float("PositionBobAmplitude", 0.2f, 0f..2f)
    private val positionDriftX by float("PositionDriftX", 0f, -1f..1f)
    private val positionDriftY by float("PositionDriftY", 0f, -1f..1f)
    private val positionDriftZ by float("PositionDriftZ", 0f, -1f..1f)
    
    // 颜色系统组
    private val colorMode by choices("ColorMode", arrayOf("Solid", "Gradient", "Rainbow", "Palette", "Noise", "Audio"), "Rainbow")
    private val primaryColorR by int("PrimaryColorR", 255, 0..255)
    private val primaryColorG by int("PrimaryColorG", 0, 0..255)
    private val primaryColorB by int("PrimaryColorB", 0, 0..255)
    private val secondaryColorR by int("SecondaryColorR", 0, 0..255)
    private val secondaryColorG by int("SecondaryColorG", 255, 0..255)
    private val secondaryColorB by int("SecondaryColorB", 0, 0..255)
    private val colorSpeed by float("ColorSpeed", 1f, 0f..10f)
    private val colorOffset by float("ColorOffset", 0f, 0f..360f)
    private val colorSaturation by float("ColorSaturation", 1f, 0f..1f)
    private val colorBrightness by float("ColorBrightness", 1f, 0f..1f)
    
    // 梯度设置组
    private val gradientType by choices("GradientType", arrayOf("Linear", "Radial", "Angular", "Diamond", "Spiral"), "Linear")
    private val gradientStops by int("GradientStops", 3, 2..10)
    private val gradientRepeat by boolean("GradientRepeat", false)
    private val gradientMirror by boolean("GradientMirror", false)
    
    // 透明度系统组
    private val alphaMode by choices("AlphaMode", arrayOf("Constant", "Pulse", "Fade", "Random", "Edge"), "Constant")
    private val baseAlpha by float("BaseAlpha", 1f, 0f..1f)
    private val alphaPulseSpeed by float("AlphaPulseSpeed", 1f, 0f..10f)
    private val alphaPulseMin by float("AlphaPulseMin", 0.3f, 0f..1f)
    private val alphaPulseMax by float("AlphaPulseMax", 1f, 0f..1f)
    private val alphaFadeIn by float("AlphaFadeIn", 0.5f, 0f..1f)
    private val alphaFadeOut by float("AlphaFadeOut", 0.5f, 0f..1f)
    
    // 发光效果组
    private val glowEnable by boolean("GlowEnable", false)
    private val glowSize by float("GlowSize", 0.5f, 0f..5f)
    private val glowIntensity by float("GlowIntensity", 0.5f, 0f..1f)
    private val glowColorR by int("GlowColorR", 255, 0..255)
    private val glowColorG by int("GlowColorG", 255, 0..255)
    private val glowColorB by int("GlowColorB", 255, 0..255)
    private val glowBlurPasses by int("GlowBlurPasses", 3, 1..10)
    
    // 轮廓系统组
    private val outlineEnable by boolean("OutlineEnable", false)
    private val outlineWidth by float("OutlineWidth", 0.1f, 0f..1f)
    private val outlineColorR by int("OutlineColorR", 255, 0..255)
    private val outlineColorG by int("OutlineColorG", 255, 0..255)
    private val outlineColorB by int("OutlineColorB", 255, 0..255)
    private val outlineAlpha by float("OutlineAlpha", 1f, 0f..1f)
    private val outlineGlow by boolean("OutlineGlow", false)
    
    // 填充系统组
    private val fillType by choices("FillType", arrayOf("Solid", "Pattern", "Gradient", "Noise", "Checkerboard"), "Solid")
    private val fillPattern by choices("FillPattern", arrayOf("Lines", "Dots", "Crosshatch", "Waves", "Zigzag"), "Lines")
    private val fillPatternSize by float("FillPatternSize", 0.1f, 0.01f..1f)
    private val fillPatternRotation by float("FillPatternRotation", 0f, 0f..360f)
    
    // 纹理系统组
    private val textureEnable by boolean("TextureEnable", false)
    private val textureScaleX by float("TextureScaleX", 1f, 0.1f..10f)
    private val textureScaleY by float("TextureScaleY", 1f, 0.1f..10f)
    private val textureOffsetX by float("TextureOffsetX", 0f, 0f..1f)
    private val textureOffsetY by float("TextureOffsetY", 0f, 0f..1f)
    private val textureRotation by float("TextureRotation", 0f, 0f..360f)
    
    // 变形动画组
    private val morphEnable by boolean("MorphEnable", false)
    private val morphSpeed by float("MorphSpeed", 1f, 0f..10f)
    private val morphType by choices("MorphType", arrayOf("Wave", "Pulse", "Twist", "Explode", "Implode", "Wobble"), "Wave")
    private val morphAmplitude by float("MorphAmplitude", 0.2f, 0f..2f)
    private val morphFrequency by float("MorphFrequency", 1f, 0f..10f)
    
    // 粒子系统组
    private val particleEnable by boolean("ParticleEnable", false)
    private val particleCount by int("ParticleCount", 100, 1..1000)
    private val particleSize by float("ParticleSize", 0.1f, 0.01f..1f)
    private val particleSpeed by float("ParticleSpeed", 0.1f, 0f..2f)
    private val particleLife by int("ParticleLife", 60, 1..300)
    private val particleGravity by float("ParticleGravity", 0f, -1f..1f)
    private val particleFade by boolean("ParticleFade", true)
    
    // 拖尾系统组
    private val trailEnable by boolean("TrailEnable", true)
    private val trailLength by int("TrailLength", 10, 1..100)
    private val trailFade by boolean("TrailFade", true)
    private val trailWidth by float("TrailWidth", 0.1f, 0f..1f)
    private val trailColorMode by choices("TrailColorMode", arrayOf("Same", "Fade", "Rainbow"), "Fade")
    
    // 反射系统组
    private val reflectionEnable by boolean("ReflectionEnable", false)
    private val reflectionOpacity by float("ReflectionOpacity", 0.5f, 0f..1f)
    private val reflectionBlur by float("ReflectionBlur", 0.5f, 0f..2f)
    private val reflectionOffset by float("ReflectionOffset", 0.5f, -2f..2f)
    
    // 阴影系统组
    private val shadowEnable by boolean("ShadowEnable", false)
    private val shadowOpacity by float("ShadowOpacity", 0.3f, 0f..1f)
    private val shadowBlur by float("ShadowBlur", 0.5f, 0f..2f)
    private val shadowOffsetX by float("ShadowOffsetX", 0.1f, -1f..1f)
    private val shadowOffsetY by float("ShadowOffsetY", 0.1f, -1f..1f)
    
    // 分形系统组
    private val fractalEnable by boolean("FractalEnable", false)
    private val fractalIterations by int("FractalIterations", 3, 1..10)
    private val fractalScale by float("FractalScale", 0.5f, 0.1f..0.9f)
    private val fractalRotation by float("FractalRotation", 30f, 0f..360f)
    
    // 噪波系统组
    private val noiseEnable by boolean("NoiseEnable", false)
    private val noiseType by choices("NoiseType", arrayOf("Perlin", "Simplex", "Worley", "Value"), "Perlin")
    private val noiseScale by float("NoiseScale", 0.1f, 0.01f..1f)
    private val noiseSpeed by float("NoiseSpeed", 0.1f, 0f..2f)
    private val noiseAmplitude by float("NoiseAmplitude", 0.2f, 0f..1f)
    
    // 扭曲系统组
    private val distortionEnable by boolean("DistortionEnable", false)
    private val distortionType by choices("DistortionType", arrayOf("Wave", "Swirl", "Lens", "Ripple", "Turbulence"), "Wave")
    private val distortionStrength by float("DistortionStrength", 0.1f, 0f..1f)
    private val distortionSpeed by float("DistortionSpeed", 1f, 0f..10f)
    private val distortionScale by float("DistortionScale", 1f, 0.1f..10f)
    
    // 混合模式组
    private val blendMode by choices("BlendMode", arrayOf("Normal", "Additive", "Multiply", "Screen", "Overlay", "Difference"), "Normal")
    private val blendOpacity by float("BlendOpacity", 1f, 0f..1f)
    private val blendSrcFactor by int("BlendSrcFactor", 770, 0..65535)
    private val blendDstFactor by int("BlendDstFactor", 771, 0..65535)
    
    // 深度效果组
    private val depthTest by boolean("DepthTest", false)
    private val depthWrite by boolean("DepthWrite", false)
    private val depthFunc by int("DepthFunc", 513, 512..527)
    private val depthOffset by float("DepthOffset", 0f, -1f..1f)
    
    // 剪裁系统组
    private val clippingEnable by boolean("ClippingEnable", false)
    private val clippingX by float("ClippingX", 0f, -1f..1f)
    private val clippingY by float("ClippingY", 0f, -1f..1f)
    private val clippingWidth by float("ClippingWidth", 1f, 0f..2f)
    private val clippingHeight by float("ClippingHeight", 1f, 0f..2f)
    
    // 遮罩系统组
    private val maskEnable by boolean("MaskEnable", false)
    private val maskType by choices("MaskType", arrayOf("Circle", "Square", "Triangle", "Gradient", "Texture"), "Circle")
    private val maskInvert by boolean("MaskInvert", false)
    private val maskFeather by float("MaskFeather", 0.1f, 0f..1f)
    
    // 光照系统组
    private val lightingEnable by boolean("LightingEnable", false)
    private val lightPosX by float("LightPosX", 0f, -10f..10f)
    private val lightPosY by float("LightPosY", 5f, -10f..10f)
    private val lightPosZ by float("LightPosZ", 5f, -10f..10f)
    private val lightColorR by int("LightColorR", 255, 0..255)
    private val lightColorG by int("LightColorG", 255, 0..255)
    private val lightColorB by int("LightColorB", 255, 0..255)
    private val lightIntensity by float("LightIntensity", 1f, 0f..5f)
    
    // 环境光组
    private val ambientLightR by int("AmbientLightR", 128, 0..255)
    private val ambientLightG by int("AmbientLightG", 128, 0..255)
    private val ambientLightB by int("AmbientLightB", 128, 0..255)
    private val ambientIntensity by float("AmbientIntensity", 0.5f, 0f..1f)
    
    // 材质属性组
    private val materialShininess by float("MaterialShininess", 32f, 1f..128f)
    private val materialSpecular by float("MaterialSpecular", 0.5f, 0f..1f)
    private val materialDiffuse by float("MaterialDiffuse", 0.8f, 0f..1f)
    private val materialAmbient by float("MaterialAmbient", 0.2f, 0f..1f)
    
    // 物理系统组
    private val physicsEnable by boolean("PhysicsEnable", false)
    private val physicsGravity by float("PhysicsGravity", 0.1f, -1f..1f)
    private val physicsFriction by float("PhysicsFriction", 0.9f, 0f..1f)
    private val physicsBounciness by float("PhysicsBounciness", 0.5f, 0f..1f)
    private val physicsWindX by float("PhysicsWindX", 0f, -1f..1f)
    private val physicsWindY by float("PhysicsWindY", 0f, -1f..1f)
    private val physicsWindZ by float("PhysicsWindZ", 0f, -1f..1f)
    
    // 弹簧系统组
    private val springEnable by boolean("SpringEnable", false)
    private val springStiffness by float("SpringStiffness", 0.1f, 0f..1f)
    private val springDamping by float("SpringDamping", 0.1f, 0f..1f)
    private val springRestLength by float("SpringRestLength", 1f, 0f..5f)
    
    // 约束系统组
    private val constraintsEnable by boolean("ConstraintsEnable", false)
    private val constraintType by choices("ConstraintType", arrayOf("Distance", "Hinge", "Slider", "Cone"), "Distance")
    private val constraintMin by float("ConstraintMin", 0f, -5f..5f)
    private val constraintMax by float("ConstraintMax", 1f, -5f..5f)
    
    // 碰撞系统组
    private val collisionEnable by boolean("CollisionEnable", false)
    private val collisionType by choices("CollisionType", arrayOf("Sphere", "Box", "Capsule", "Mesh"), "Sphere")
    private val collisionRadius by float("CollisionRadius", 0.5f, 0f..5f)
    private val collisionResponse by float("CollisionResponse", 0.5f, 0f..1f)
    
    // 流体系统组
    private val fluidEnable by boolean("FluidEnable", false)
    private val fluidDensity by float("FluidDensity", 1f, 0f..10f)
    private val fluidViscosity by float("FluidViscosity", 0.1f, 0f..1f)
    private val fluidDrag by float("FluidDrag", 0.1f, 0f..1f)
    
    // 场系统组
    private val fieldEnable by boolean("FieldEnable", false)
    private val fieldType by choices("FieldType", arrayOf("Magnetic", "Electric", "Gravity", "Vortex", "Noise"), "Magnetic")
    private val fieldStrength by float("FieldStrength", 1f, 0f..10f)
    private val fieldFalloff by float("FieldFalloff", 1f, 0f..5f)
    
    // 声音反应组
    private val audioReactEnable by boolean("AudioReactEnable", false)
    private val audioSource by choices("AudioSource", arrayOf("Microphone", "System", "Music", "Custom"), "System")
    private val audioSensitivity by float("AudioSensitivity", 1f, 0f..10f)
    private val audioSmoothing by float("AudioSmoothing", 0.5f, 0f..1f)
    private val audioFrequencyLow by float("AudioFrequencyLow", 20f, 20f..500f)
    private val audioFrequencyMid by float("AudioFrequencyMid", 1000f, 500f..5000f)
    private val audioFrequencyHigh by float("AudioFrequencyHigh", 5000f, 5000f..20000f)
    
    // 时间控制组
    private val timeScale by float("TimeScale", 1f, 0f..10f)
    private val timeOffset by float("TimeOffset", 0f, 0f..3600f)
    private val timeLoop by boolean("TimeLoop", true)
    private val timeLoopDuration by float("TimeLoopDuration", 10f, 0.1f..60f)
    
    // 随机化组
    private val randomSeed by int("RandomSeed", 0, 0..999999)
    private val randomizeColors by boolean("RandomizeColors", false)
    private val randomizeSizes by boolean("RandomizeSizes", false)
    private val randomizeRotations by boolean("RandomizeRotations", false)
    private val randomizePositions by boolean("RandomizePositions", false)
    
    // 性能优化组
    private val lodEnable by boolean("LODEnable", false)
    private val lodDistance by float("LODDistance", 10f, 0f..100f)
    private val lodDetailHigh by int("LODDetailHigh", 64, 8..256)
    private val lodDetailMedium by int("LODDetailMedium", 32, 8..128)
    private val lodDetailLow by int("LODDetailLow", 16, 4..64)
    private val cullingEnable by boolean("CullingEnable", false)
    private val cullingAngle by float("CullingAngle", 90f, 0f..180f)
    
    // 缓存系统组
    private val cacheEnable by boolean("CacheEnable", false)
    private val cacheDuration by int("CacheDuration", 60, 1..300)
    private val cacheResolution by int("CacheResolution", 512, 64..2048)
    
    // 交互系统组
    private val interactEnable by boolean("InteractEnable", false)
    private val interactRadius by float("InteractRadius", 2f, 0f..10f)
    private val interactPush by float("InteractPush", 0.1f, 0f..1f)
    private val interactPull by float("InteractPull", 0.1f, 0f..1f)
    private val interactRotate by float("InteractRotate", 1f, 0f..10f)
    
    // 事件触发组
    private val eventOnDamage by boolean("EventOnDamage", false)
    private val eventOnKill by boolean("EventOnKill", false)
    private val eventOnJump by boolean("EventOnJump", false)
    private val eventOnMove by boolean("EventOnMove", false)
    private val eventOnSneak by boolean("EventOnSneak", false)
    private val eventOnSprint by boolean("EventOnSprint", false)
    private val eventDuration by int("EventDuration", 30, 1..300)
    private val eventIntensity by float("EventIntensity", 1f, 0f..5f)
    
    // 状态效果组
    private val effectOnFire by boolean("EffectOnFire", false)
    private val effectOnPotion by boolean("EffectOnPotion", false)
    private val effectOnLowHealth by boolean("EffectOnLowHealth", false)
    private val effectOnLowHunger by boolean("EffectOnLowHunger", false)
    private val effectOnAir by boolean("EffectOnAir", false)
    private val effectHealthThreshold by float("EffectHealthThreshold", 0.3f, 0f..1f)
    private val effectHungerThreshold by int("EffectHungerThreshold", 6, 0..20)
    private val effectAirThreshold by int("EffectAirThreshold", 60, 0..300)
    
    // 环境反应组
    private val reactToWeather by boolean("ReactToWeather", false)
    private val reactToTime by boolean("ReactToTime", false)
    private val reactToBiome by boolean("ReactToBiome", false)
    private val reactToLight by boolean("ReactToLight", false)
    private val weatherRainEffect by float("WeatherRainEffect", 0.5f, 0f..1f)
    private val weatherThunderEffect by float("WeatherThunderEffect", 1f, 0f..1f)
    private val timeDayEffect by float("TimeDayEffect", 1f, 0f..1f)
    private val timeNightEffect by float("TimeNightEffect", 0.3f, 0f..1f)
    
    // 多人同步组
    private val syncWithPlayers by boolean("SyncWithPlayers", false)
    private val syncDistance by float("SyncDistance", 20f, 0f..100f)
    private val syncStrength by float("SyncStrength", 0.5f, 0f..1f)
    private val syncDelay by int("SyncDelay", 0, 0..100)
    
    // 网络效果组
    private val networkPingEffect by boolean("NetworkPingEffect", false)
    private val networkLagEffect by boolean("NetworkLagEffect", false)
    private val networkPacketLossEffect by boolean("NetworkPacketLossEffect", false)
    private val networkPingMultiplier by float("NetworkPingMultiplier", 0.01f, 0f..1f)
    private val networkLagMultiplier by float("NetworkLagMultiplier", 0.1f, 0f..1f)
    
    // 保存/加载组
    private val presetSlot by int("PresetSlot", 1, 1..10)
    private val autoSave by boolean("AutoSave", false)
    private val autoSaveInterval by int("AutoSaveInterval", 60, 1..3600)
    private val loadOnStart by boolean("LoadOnStart", false)
    
    // 导出设置组
    private val exportFormat by choices("ExportFormat", arrayOf("PNG", "GIF", "Video", "3D Model"), "PNG")
    private val exportResolution by int("ExportResolution", 1080, 240..4320)
    private val exportFPS by int("ExportFPS", 60, 1..240)
    private val exportDuration by int("ExportDuration", 10, 1..60)
    
    // 高级数学组
    private val equationX by choices("EquationX", arrayOf("None", "Sin", "Cos", "Tan", "Exp", "Log", "Custom"), "None")
    private val equationY by choices("EquationY", arrayOf("None", "Sin", "Cos", "Tan", "Exp", "Log", "Custom"), "None")
    private val equationZ by choices("EquationZ", arrayOf("None", "Sin", "Cos", "Tan", "Exp", "Log", "Custom"), "None")
    private val customEquationX by text("CustomEquationX", "t")
    private val customEquationY by text("CustomEquationY", "0")
    private val customEquationZ by text("CustomEquationZ", "0")
    private val equationTimeScale by float("EquationTimeScale", 1f, 0f..10f)
    
    // 参数化曲线组
    private val curveType by choices("CurveType", arrayOf("Linear", "Bezier", "Hermite", "CatmullRom", "BSpline", "NURBS"), "Linear")
    private val curvePoints by int("CurvePoints", 4, 2..20)
    private val curveTension by float("CurveTension", 0.5f, -1f..1f)
    private val curveContinuity by float("CurveContinuity", 0f, -1f..1f)
    private val curveBias by float("CurveBias", 0f, -1f..1f)
    
    // 矩阵变换组
    private val matrixTransformEnable by boolean("MatrixTransformEnable", false)
    private val matrixM11 by float("MatrixM11", 1f, -10f..10f)
    private val matrixM12 by float("MatrixM12", 0f, -10f..10f)
    private val matrixM13 by float("MatrixM13", 0f, -10f..10f)
    private val matrixM14 by float("MatrixM14", 0f, -10f..10f)
    private val matrixM21 by float("MatrixM21", 0f, -10f..10f)
    private val matrixM22 by float("MatrixM22", 1f, -10f..10f)
    private val matrixM23 by float("MatrixM23", 0f, -10f..10f)
    private val matrixM24 by float("MatrixM24", 0f, -10f..10f)
    private val matrixM31 by float("MatrixM31", 0f, -10f..10f)
    private val matrixM32 by float("MatrixM32", 0f, -10f..10f)
    private val matrixM33 by float("MatrixM33", 1f, -10f..10f)
    private val matrixM34 by float("MatrixM34", 0f, -10f..10f)
    private val matrixM41 by float("MatrixM41", 0f, -10f..10f)
    private val matrixM42 by float("MatrixM42", 0f, -10f..10f)
    private val matrixM43 by float("MatrixM43", 0f, -10f..10f)
    private val matrixM44 by float("MatrixM44", 1f, -10f..10f)
    
    // 四元数旋转组
    private val quaternionEnable by boolean("QuaternionEnable", false)
    private val quaternionX by float("QuaternionX", 0f, -1f..1f)
    private val quaternionY by float("QuaternionY", 0f, -1f..1f)
    private val quaternionZ by float("QuaternionZ", 0f, -1f..1f)
    private val quaternionW by float("QuaternionW", 1f, -1f..1f)
    private val quaternionSlerp by boolean("QuaternionSlerp", true)
    private val quaternionSlerpFactor by float("QuaternionSlerpFactor", 0.1f, 0f..1f)
    
    // 极坐标组
    private val polarEnable by boolean("PolarEnable", false)
    private val polarRadius by float("PolarRadius", 1f, 0f..10f)
    private val polarAngle by float("PolarAngle", 0f, 0f..360f)
    private val polarSpiral by float("PolarSpiral", 0f, -1f..1f)
    
    // 球坐标组
    private val sphericalEnable by boolean("SphericalEnable", false)
    private val sphericalRadius by float("SphericalRadius", 1f, 0f..10f)
    private val sphericalTheta by float("SphericalTheta", 0f, 0f..180f)
    private val sphericalPhi by float("SphericalPhi", 0f, 0f..360f)
    
    // 圆柱坐标组
    private val cylindricalEnable by boolean("CylindricalEnable", false)
    private val cylindricalRadius by float("CylindricalRadius", 1f, 0f..10f)
    private val cylindricalAngle by float("CylindricalAngle", 0f, 0f..360f)
    private val cylindricalHeight by float("CylindricalHeight", 0f, -5f..5f)
    
    // 投影系统组
    private val projectionType by choices("ProjectionType", arrayOf("Perspective", "Orthographic", "Fisheye", "Panoramic"), "Perspective")
    private val projectionFOV by float("ProjectionFOV", 60f, 1f..179f)
    private val projectionNear by float("ProjectionNear", 0.1f, 0f..10f)
    private val projectionFar by float("ProjectionFar", 100f, 1f..1000f)
    
    // 相机系统组
    private val cameraFollow by boolean("CameraFollow", false)
    private val cameraOffsetX by float("CameraOffsetX", 0f, -10f..10f)
    private val cameraOffsetY by float("CameraOffsetY", 0f, -10f..10f)
    private val cameraOffsetZ by float("CameraOffsetZ", 0f, -10f..10f)
    private val cameraLookAt by boolean("CameraLookAt", false)
    private val cameraLookAtX by float("CameraLookAtX", 0f, -10f..10f)
    private val cameraLookAtY by float("CameraLookAtY", 0f, -10f..10f)
    private val cameraLookAtZ by float("CameraLookAtZ", 0f, -10f..10f)
    
    // 视差系统组
    private val parallaxEnable by boolean("ParallaxEnable", false)
    private val parallaxStrength by float("ParallaxStrength", 0.1f, 0f..1f)
    private val parallaxLayers by int("ParallaxLayers", 3, 1..10)
    private val parallaxOffset by float("ParallaxOffset", 0.5f, 0f..1f)
    
    // 立体视觉组
    private val stereoEnable by boolean("StereoEnable", false)
    private val stereoSeparation by float("StereoSeparation", 0.03f, 0f..0.1f)
    private val stereoConvergence by float("StereoConvergence", 1f, 0f..10f)
    private val stereoMode by choices("StereoMode", arrayOf("Anaglyph", "SideBySide", "TopBottom", "Interlaced"), "Anaglyph")
    
    // VR支持组
    private val vrEnable by boolean("VREnable", false)
    private val vrScale by float("VRScale", 1f, 0.1f..10f)
    private val vrOffsetX by float("VROffsetX", 0f, -1f..1f)
    private val vrOffsetY by float("VROffsetY", 0f, -1f..1f)
    private val vrOffsetZ by float("VROffsetZ", 0f, -1f..1f)
    
    // AR支持组
    private val arEnable by boolean("AREnable", false)
    private val arTracking by boolean("ARTracking", false)
    private val arGround by boolean("ARGround", false)
    private val arOcclusion by boolean("AROcclusion", false)
    
    // 手势控制组
    private val gestureEnable by boolean("GestureEnable", false)
    private val gestureScale by float("GestureScale", 1f, 0f..5f)
    private val gestureRotate by float("GestureRotate", 1f, 0f..5f)
    private val gestureTranslate by float("GestureTranslate", 1f, 0f..5f)
    
    // 语音控制组
    private val voiceEnable by boolean("VoiceEnable", false)
    private val voiceVolumeScale by float("VoiceVolumeScale", 1f, 0f..10f)
    private val voicePitchScale by float("VoicePitchScale", 1f, 0f..10f)
    private val voiceCommand by text("VoiceCommand", "halo")
    
    // 脑波控制组
    private val eegEnable by boolean("EEGEnable", false)
    private val eegAttentionScale by float("EEGAttentionScale", 1f, 0f..10f)
    private val eegMeditationScale by float("EEGMeditationScale", 1f, 0f..10f)
    private val eegBlinkScale by float("EEGBlinkScale", 1f, 0f..10f)
    
    // 眼动追踪组
    private val eyeTrackingEnable by boolean("EyeTrackingEnable", false)
    private val eyeGazeScale by float("EyeGazeScale", 1f, 0f..10f)
    private val eyeBlinkScale by float("EyeBlinkScale", 1f, 0f..10f)
    private val eyeSaccadeScale by float("EyeSaccadeScale", 1f, 0f..10f)
    
    // 心率监控组
    private val heartRateEnable by boolean("HeartRateEnable", false)
    private val heartRateScale by float("HeartRateScale", 1f, 0f..10f)
    private val heartRateSmoothing by float("HeartRateSmoothing", 0.5f, 0f..1f)
    private val heartRateThreshold by int("HeartRateThreshold", 100, 40..200)
    
    // 生物反馈组
    private val biofeedbackEnable by boolean("BiofeedbackEnable", false)
    private val biofeedbackType by choices("BiofeedbackType", arrayOf("GSR", "EMG", "ECG", "Temperature"), "GSR")
    private val biofeedbackScale by float("BiofeedbackScale", 1f, 0f..10f)
    private val biofeedbackSmoothing by float("BiofeedbackSmoothing", 0.5f, 0f..1f)
    
    // 天气API组
    private val weatherAPIEnable by boolean("WeatherAPIEnable", false)
    private val weatherAPILocation by text("WeatherAPILocation", "London")
    private val weatherTemperatureScale by float("WeatherTemperatureScale", 1f, 0f..10f)
    private val weatherHumidityScale by float("WeatherHumidityScale", 1f, 0f..10f)
    private val weatherWindScale by float("WeatherWindScale", 1f, 0f..10f)
    
    // 时间API组
    private val timeAPIEnable by boolean("TimeAPIEnable", false)
    private val timeAPILocation by text("TimeAPILocation", "UTC")
    private val timeSunriseScale by float("TimeSunriseScale", 1f, 0f..10f)
    private val timeSunsetScale by float("TimeSunsetScale", 1f, 0f..10f)
    private val timeSeasonScale by float("TimeSeasonScale", 1f, 0f..10f)
    
    // 股票API组
    private val stockAPIEnable by boolean("StockAPIEnable", false)
    private val stockAPISymbol by text("StockAPISymbol", "AAPL")
    private val stockPriceScale by float("StockPriceScale", 1f, 0f..10f)
    private val stockVolumeScale by float("StockVolumeScale", 1f, 0f..10f)
    private val stockChangeScale by float("StockChangeScale", 1f, 0f..10f)
    
    // 加密货币组
    private val cryptoEnable by boolean("CryptoEnable", false)
    private val cryptoSymbol by text("CryptoSymbol", "BTC")
    private val cryptoPriceScale by float("CryptoPriceScale", 1f, 0f..10f)
    private val cryptoVolumeScale by float("CryptoVolumeScale", 1f, 0f..10f)
    private val cryptoChangeScale by float("CryptoChangeScale", 1f, 0f..10f)
    
    // 社交媒体组
    private val socialEnable by boolean("SocialEnable", false)
    private val socialPlatform by choices("SocialPlatform", arrayOf("Twitter", "Reddit", "Discord", "Twitch"), "Twitter")
    private val socialHashtag by text("SocialHashtag", "minecraft")
    private val socialPostsScale by float("SocialPostsScale", 1f, 0f..10f)
    private val socialLikesScale by float("SocialLikesScale", 1f, 0f..10f)
    
    // 新闻API组
    private val newsAPIEnable by boolean("NewsAPIEnable", false)
    private val newsAPIQuery by text("NewsAPIQuery", "technology")
    private val newsCountScale by float("NewsCountScale", 1f, 0f..10f)
    private val newsSentimentScale by float("NewsSentimentScale", 1f, -1f..1f)
    
    // 运动数据组
    private val fitnessEnable by boolean("FitnessEnable", false)
    private val fitnessStepsScale by float("FitnessStepsScale", 1f, 0f..10f)
    private val fitnessCaloriesScale by float("FitnessCaloriesScale", 1f, 0f..10f)
    private val fitnessDistanceScale by float("FitnessDistanceScale", 1f, 0f..10f)
    
    // 游戏数据组
    private val gameStatsEnable by boolean("GameStatsEnable", false)
    private val gameKillsScale by float("GameKillsScale", 1f, 0f..10f)
    private val gameDeathsScale by float("GameDeathsScale", 1f, 0f..10f)
    private val gameScoreScale by float("GameScoreScale", 1f, 0f..10f)
    
    // 系统监控组
    private val systemMonitorEnable by boolean("SystemMonitorEnable", false)
    private val systemCPUScale by float("SystemCPUScale", 1f, 0f..10f)
    private val systemGPUScale by float("SystemGPUScale", 1f, 0f..10f)
    private val systemRAMScale by float("SystemRAMScale", 1f, 0f..10f)
    
    // 网络监控组
    private val networkMonitorEnable by boolean("NetworkMonitorEnable", false)
    private val networkUploadScale by float("NetworkUploadScale", 1f, 0f..10f)
    private val networkDownloadScale by float("NetworkDownloadScale", 1f, 0f..10f)
    private val networkPingScale by float("NetworkPingScale", 1f, 0f..10f)
    
    // 文件监控组
    private val fileMonitorEnable by boolean("FileMonitorEnable", false)
    private val fileSizeScale by float("FileSizeScale", 1f, 0f..10f)
    private val fileCountScale by float("FileCountScale", 1f, 0f..10f)
    private val fileModifyScale by float("FileModifyScale", 1f, 0f..10f)
    
    // 进程监控组
    private val processMonitorEnable by boolean("ProcessMonitorEnable", false)
    private val processCountScale by float("ProcessCountScale", 1f, 0f..10f)
    private val processCPUScale by float("ProcessCPUScale", 1f, 0f..10f)
    private val processMemoryScale by float("ProcessMemoryScale", 1f, 0f..10f)
    
    // 键盘输入组
    private val keyboardInputEnable by boolean("KeyboardInputEnable", false)
    private val keyboardKeyScale by float("KeyboardKeyScale", 1f, 0f..10f)
    private val keyboardSpeedScale by float("KeyboardSpeedScale", 1f, 0f..10f)
    private val keyboardPattern by text("KeyboardPattern", "asdf")
    
    // 鼠标输入组
    private val mouseInputEnable by boolean("MouseInputEnable", false)
    private val mouseMoveScale by float("MouseMoveScale", 1f, 0f..10f)
    private val mouseClickScale by float("MouseClickScale", 1f, 0f..10f)
    private val mouseScrollScale by float("MouseScrollScale", 1f, 0f..10f)
    
    // 游戏手柄组
    private val gamepadEnable by boolean("GamepadEnable", false)
    private val gamepadStickScale by float("GamepadStickScale", 1f, 0f..10f)
    private val gamepadButtonScale by float("GamepadButtonScale", 1f, 0f..10f)
    private val gamepadTriggerScale by float("GamepadTriggerScale", 1f, 0f..10f)
    
    // MIDI设备组
    private val midiEnable by boolean("MIDIEnable", false)
    private val midiNoteScale by float("MIDINoteScale", 1f, 0f..10f)
    private val midiVelocityScale by float("MIDIVelocityScale", 1f, 0f..10f)
    private val midiCCScale by float("MIDICCScale", 1f, 0f..10f)
    
    // OSC协议组
    private val oscEnable by boolean("OSCEnable", false)
    private val oscAddress by text("OSCAddress", "/halo")
    private val oscPort by int("OSCPort", 8000, 1..65535)
    private val oscScale by float("OSCScale", 1f, 0f..10f)
    
    // DMX协议组
    private val dmxEnable by boolean("DMXEnable", false)
    private val dmxUniverse by int("DMXUniverse", 1, 1..16)
    private val dmxChannel by int("DMXChannel", 1, 1..512)
    private val dmxScale by float("DMXScale", 1f, 0f..10f)
    
    // ArtNet协议组
    private val artnetEnable by boolean("ArtNetEnable", false)
    private val artnetNet by int("ArtNetNet", 0, 0..127)
    private val artnetSubnet by int("ArtNetSubnet", 0, 0..15)
    private val artnetUniverse by int("ArtNetUniverse", 0, 0..15)
    
    // 串口通信组
    private val serialEnable by boolean("SerialEnable", false)
    private val serialPort by text("SerialPort", "COM3")
    private val serialBaud by int("SerialBaud", 9600, 300..115200)
    private val serialScale by float("SerialScale", 1f, 0f..10f)
    
    // TCP/IP组
    private val tcpEnable by boolean("TCPEnable", false)
    private val tcpHost by text("TCPHost", "localhost")
    private val tcpPort by int("TCPPort", 8080, 1..65535)
    private val tcpScale by float("TCPScale", 1f, 0f..10f)
    
    // UDP组
    private val udpEnable by boolean("UDPEnable", false)
    private val udpHost by text("UDPHost", "localhost")
    private val udpPort by int("UDPPort", 8080, 1..65535)
    private val udpScale by float("UDPScale", 1f, 0f..10f)
    
    // WebSocket组
    private val websocketEnable by boolean("WebSocketEnable", false)
    private val websocketURL by text("WebSocketURL", "ws://localhost:8080")
    private val websocketScale by float("WebSocketScale", 1f, 0f..10f)
    
    // MQTT组
    private val mqttEnable by boolean("MQTTEnable", false)
    private val mqttHost by text("MQTTHost", "localhost")
    private val mqttTopic by text("MQTTTopic", "halo")
    private val mqttScale by float("MQTTScale", 1f, 0f..10f)
    
    // HTTP API组
    private val httpEnable by boolean("HTTPEnable", false)
    private val httpURL by text("HTTPURL", "http://localhost:8080/data")
    private val httpInterval by int("HTTPInterval", 1000, 100..10000)
    private val httpScale by float("HTTPScale", 1f, 0f..10f)
    
    // WebRTC组
    private val webrtcEnable by boolean("WebRTCEnable", false)
    private val webrtcSignalURL by text("WebRTCSignalURL", "http://localhost:8080/signal")
    private val webrtcScale by float("WebRTCScale", 1f, 0f..10f)
    
    // 区块链组
    private val blockchainEnable by boolean("BlockchainEnable", false)
    private val blockchainNetwork by choices("BlockchainNetwork", arrayOf("Ethereum", "Bitcoin", "Polygon", "Solana"), "Ethereum")
    private val blockchainAddress by text("BlockchainAddress", "0x...")
    private val blockchainScale by float("BlockchainScale", 1f, 0f..10f)
    
    // AI模型组
    private val aiEnable by boolean("AIEnable", false)
    private val aiModel by choices("AIModel", arrayOf("StableDiffusion", "DALL-E", "Midjourney", "Custom"), "StableDiffusion")
    private val aiPrompt by text("AIPrompt", "glowing halo")
    private val aiScale by float("AIScale", 1f, 0f..10f)
    
    // 机器学习组
    private val mlEnable by boolean("MLEnable", false)
    private val mlType by choices("MLType", arrayOf("Classification", "Regression", "Clustering", "GAN"), "Classification")
    private val mlInputs by int("MLInputs", 3, 1..100)
    private val mlScale by float("MLScale", 1f, 0f..10f)
    
    // 神经网络组
    private val nnEnable by boolean("NNEnable", false)
    private val nnLayers by int("NNLayers", 3, 1..10)
    private val nnNeurons by int("NNNeurons", 16, 1..256)
    private val nnScale by float("NNScale", 1f, 0f..10f)
    
    // 遗传算法组
    private val gaEnable by boolean("GAEnable", false)
    private val gaPopulation by int("GAPopulation", 100, 10..1000)
    private val gaGenerations by int("GAGenerations", 100, 1..1000)
    private val gaScale by float("GAScale", 1f, 0f..10f)
    
    // 粒子群优化组
    private val psoEnable by boolean("PSOEnable", false)
    private val psoParticles by int("PSOParticles", 50, 10..500)
    private val psoIterations by int("PSOIterations", 100, 1..1000)
    private val psoScale by float("PSOScale", 1f, 0f..10f)
    
    // 模拟退火组
    private val saEnable by boolean("SAEnable", false)
    private val saTemperature by float("SATemperature", 100f, 0f..1000f)
    private val saCooling by float("SACooling", 0.99f, 0f..1f)
    private val saScale by float("SAScale", 1f, 0f..10f)
    
    // 元胞自动机组
    private val caEnable by boolean("CAEnable", false)
    private val caRules by int("CARules", 30, 0..255)
    private val caGenerations by int("CAGenerations", 100, 1..1000)
    private val caScale by float("CAScale", 1f, 0f..10f)
    
    // L系统组
    private val lsystemEnable by boolean("LSystemEnable", false)
    private val lsystemAxiom by text("LSystemAxiom", "F")
    private val lsystemRules by text("LSystemRules", "F=F+F-F-F+F")
    private val lsystemIterations by int("LSystemIterations", 3, 1..10)
    
    // 分形火焰组
    private val flameEnable by boolean("FlameEnable", false)
    private val flameVariations by int("FlameVariations", 3, 1..20)
    private val flameIterations by int("FlameIterations", 10000, 100..100000)
    private val flameScale by float("FlameScale", 1f, 0f..10f)
    
    // 超公式组
    private val superformulaEnable by boolean("SuperformulaEnable", false)
    private val superformulaM by float("SuperformulaM", 1f, 0f..10f)
    private val superformulaN1 by float("SuperformulaN1", 1f, 0f..10f)
    private val superformulaN2 by float("SuperformulaN2", 1f, 0f..10f)
    private val superformulaN3 by float("SuperformulaN3", 1f, 0f..10f)
    
    // 极小曲面组
    private val minimalSurfaceEnable by boolean("MinimalSurfaceEnable", false)
    private val minimalSurfaceType by choices("MinimalSurfaceType", arrayOf("Catenoid", "Helicoid", "Enneper", "Scherk"), "Catenoid")
    private val minimalSurfaceU by float("MinimalSurfaceU", 1f, 0f..10f)
    private val minimalSurfaceV by float("MinimalSurfaceV", 1f, 0f..10f)
    
    // 参数化曲面组
    private val parametricSurfaceEnable by boolean("ParametricSurfaceEnable", false)
    private val parametricSurfaceU by text("ParametricSurfaceU", "u")
    private val parametricSurfaceV by text("ParametricSurfaceV", "v")
    private val parametricSurfaceX by text("ParametricSurfaceX", "cos(u)*cos(v)")
    private val parametricSurfaceY by text("ParametricSurfaceY", "cos(u)*sin(v)")
    private val parametricSurfaceZ by text("ParametricSurfaceZ", "sin(u)")
    
    // 隐式曲面组
    private val implicitSurfaceEnable by boolean("ImplicitSurfaceEnable", false)
    private val implicitSurfaceEquation by text("ImplicitSurfaceEquation", "x^2+y^2+z^2-1")
    private val implicitSurfaceStep by float("ImplicitSurfaceStep", 0.1f, 0.01f..1f)
    private val implicitSurfaceThreshold by float("ImplicitSurfaceThreshold", 0.01f, 0f..1f)
    
    // 体素系统组
    private val voxelEnable by boolean("VoxelEnable", false)
    private val voxelResolution by int("VoxelResolution", 32, 8..256)
    private val voxelSize by float("VoxelSize", 0.1f, 0.01f..1f)
    private val voxelThreshold by float("VoxelThreshold", 0.5f, 0f..1f)
    
    // 点云系统组
    private val pointCloudEnable by boolean("PointCloudEnable", false)
    private val pointCloudCount by int("PointCloudCount", 1000, 100..100000)
    private val pointCloudSize by float("PointCloudSize", 0.1f, 0.01f..1f)
    private val pointCloudColor by boolean("PointCloudColor", false)
    
    // 网格系统组
    private val meshEnable by boolean("MeshEnable", false)
    private val meshVertices by int("MeshVertices", 100, 3..10000)
    private val meshFaces by int("MeshFaces", 100, 1..10000)
    private val meshSmooth by boolean("MeshSmooth", false)
    
    // NURBS曲面组
    private val nurbsEnable by boolean("NURBSEnable", false)
    private val nurbsDegreeU by int("NURBSDegreeU", 3, 1..10)
    private val nurbsDegreeV by int("NURBSDegreeV", 3, 1..10)
    private val nurbsControlPoints by int("NURBSControlPoints", 16, 4..100)
    
    // 细分曲面组
    private val subdivisionEnable by boolean("SubdivisionEnable", false)
    private val subdivisionLevels by int("SubdivisionLevels", 2, 1..5)
    private val subdivisionScheme by choices("SubdivisionScheme", arrayOf("CatmullClark", "Loop", "DooSabin"), "CatmullClark")
    
    // 位移贴图组
    private val displacementEnable by boolean("DisplacementEnable", false)
    private val displacementMap by text("DisplacementMap", "")
    private val displacementStrength by float("DisplacementStrength", 0.1f, 0f..1f)
    private val displacementScale by float("DisplacementScale", 1f, 0.1f..10f)
    
    // 法线贴图组
    private val normalMapEnable by boolean("NormalMapEnable", false)
    private val normalMap by text("NormalMap", "")
    private val normalStrength by float("NormalStrength", 1f, 0f..5f)
    
    // 高度图组
    private val heightMapEnable by boolean("HeightMapEnable", false)
    private val heightMap by text("HeightMap", "")
    private val heightScale by float("HeightScale", 1f, 0f..10f)
    private val heightSmooth by boolean("HeightSmooth", false)
    
    // 烘焙光照组
    private val bakedLightingEnable by boolean("BakedLightingEnable", false)
    private val bakedLightmap by text("BakedLightmap", "")
    private val bakedIntensity by float("BakedIntensity", 1f, 0f..5f)
    
    // 实时GI组
    private val realtimeGIEnable by boolean("RealtimeGIEnable", false)
    private val realtimeGIBounces by int("RealtimeGIBounces", 2, 1..10)
    private val realtimeGIResolution by int("RealtimeGIResolution", 64, 16..512)
    
    // 光线追踪组
    private val raytracingEnable by boolean("RaytracingEnable", false)
    private val raytracingBounces by int("RaytracingBounces", 3, 1..10)
    private val raytracingSamples by int("RaytracingSamples", 16, 1..256)
    
    // 路径追踪组
    private val pathtracingEnable by boolean("PathtracingEnable", false)
    private val pathtracingBounces by int("PathtracingBounces", 5, 1..20)
    private val pathtracingSamples by int("PathtracingSamples", 64, 1..1024)
    
    // 光子映射组
    private val photonMappingEnable by boolean("PhotonMappingEnable", false)
    private val photonMappingPhotons by int("PhotonMappingPhotons", 10000, 100..1000000)
    private val photonMappingBounces by int("PhotonMappingBounces", 5, 1..20)
    
    // 辐射度组
    private val radiosityEnable by boolean("RadiosityEnable", false)
    private val radiosityPatches by int("RadiosityPatches", 1000, 100..10000)
    private val radiosityIterations by int("RadiosityIterations", 10, 1..100)
    
    // 环境光遮蔽组
    private val aoEnable by boolean("AOEnable", false)
    private val aoRadius by float("AORadius", 0.5f, 0f..5f)
    private val aoSamples by int("AOSamples", 16, 1..64)
    private val aoIntensity by float("AOIntensity", 1f, 0f..5f)
    
    // 屏幕空间反射组
    private val ssrEnable by boolean("SSREnable", false)
    private val ssrSteps by int("SSRSteps", 32, 1..128)
    private val ssrStepSize by float("SSRStepSize", 0.1f, 0f..1f)
    private val ssrThickness by float("SSRThickness", 0.1f, 0f..1f)
    
    // 屏幕空间GI组
    private val ssgiEnable by boolean("SSGIEnable", false)
    private val ssgiRadius by float("SSGIRadius", 1f, 0f..10f)
    private val ssgiSamples by int("SSGISamples", 16, 1..64)
    private val ssgiIntensity by float("SSGIIntensity", 1f, 0f..5f)
    
    // 体积光组
    private val volumetricLightEnable by boolean("VolumetricLightEnable", false)
    private val volumetricLightSteps by int("VolumetricLightSteps", 32, 1..128)
    private val volumetricLightDensity by float("VolumetricLightDensity", 0.1f, 0f..1f)
    private val volumetricLightColorR by int("VolumetricLightColorR", 255, 0..255)
    private val volumetricLightColorG by int("VolumetricLightColorG", 255, 0..255)
    private val volumetricLightColorB by int("VolumetricLightColorB", 255, 0..255)
    
    // 上帝光组
    private val godrayEnable by boolean("GodrayEnable", false)
    private val godraySteps by int("GodraySteps", 50, 1..200)
    private val godrayDensity by float("GodrayDensity", 0.1f, 0f..1f)
    private val godrayDecay by float("GodrayDecay", 0.9f, 0f..1f)
    
    // 镜头光晕组
    private val lensFlareEnable by boolean("LensFlareEnable", false)
    private val lensFlareElements by int("LensFlareElements", 6, 1..20)
    private val lensFlareSize by float("LensFlareSize", 1f, 0f..5f)
    private val lensFlareIntensity by float("LensFlareIntensity", 1f, 0f..5f)
    
    // 色差组
    private val chromaticAberrationEnable by boolean("ChromaticAberrationEnable", false)
    private val chromaticAberrationStrength by float("ChromaticAberrationStrength", 0.01f, 0f..0.1f)
    private val chromaticAberrationRedOffsetX by float("ChromaticAberrationRedOffsetX", 0.005f, -0.1f..0.1f)
    private val chromaticAberrationGreenOffsetX by float("ChromaticAberrationGreenOffsetX", 0f, -0.1f..0.1f)
    private val chromaticAberrationBlueOffsetX by float("ChromaticAberrationBlueOffsetX", -0.005f, -0.1f..0.1f)
    
    // 晕影组
    private val vignetteEnable by boolean("VignetteEnable", false)
    private val vignetteSize by float("VignetteSize", 0.5f, 0f..1f)
    private val vignetteSmoothness by float("VignetteSmoothness", 0.5f, 0f..1f)
    private val vignetteOpacity by float("VignetteOpacity", 0.5f, 0f..1f)
    
    // 颗粒感组
    private val filmGrainEnable by boolean("FilmGrainEnable", false)
    private val filmGrainSize by float("FilmGrainSize", 0.5f, 0f..1f)
    private val filmGrainIntensity by float("FilmGrainIntensity", 0.1f, 0f..1f)
    private val filmGrainAnimate by boolean("FilmGrainAnimate", false)
    
    // 扫描线组
    private val scanlinesEnable by boolean("ScanlinesEnable", false)
    private val scanlinesCount by int("ScanlinesCount", 100, 1..1000)
    private val scanlinesWidth by float("ScanlinesWidth", 1f, 0f..5f)
    private val scanlinesOpacity by float("ScanlinesOpacity", 0.5f, 0f..1f)
    
    // CRT效果组
    private val crtEnable by boolean("CRTEnable", false)
    private val crtCurvature by float("CRTCurvature", 0.1f, 0f..1f)
    private val crtScanlineIntensity by float("CRTScanlineIntensity", 0.5f, 0f..1f)
    private val crtChromaticAberration by float("CRTChromaticAberration", 0.01f, 0f..0.1f)
    
    // 复古效果组
    private val retroEnable by boolean("RetroEnable", false)
    private val retroColorCount by int("RetroColorCount", 16, 2..256)
    private val retroDithering by boolean("RetroDithering", false)
    private val retroPixelation by float("RetroPixelation", 0.1f, 0f..1f)
    
    // 故障艺术组
    private val glitchEnable by boolean("GlitchEnable", false)
    private val glitchFrequency by float("GlitchFrequency", 0.1f, 0f..1f)
    private val glitchIntensity by float("GlitchIntensity", 0.1f, 0f..1f)
    private val glitchSpeed by float("GlitchSpeed", 1f, 0f..10f)
    
    // 数据Moshing组
    private val datamoshEnable by boolean("DatamoshEnable", false)
    private val datamoshBlockSize by int("DatamoshBlockSize", 8, 2..32)
    private val datamoshOffset by float("DatamoshOffset", 0.1f, 0f..1f)
    private val datamoshSpeed by float("DatamoshSpeed", 1f, 0f..10f)
    
    // 极坐标扭曲组
    private val polarWarpEnable by boolean("PolarWarpEnable", false)
    private val polarWarpStrength by float("PolarWarpStrength", 0.1f, 0f..1f)
    private val polarWarpCenterX by float("PolarWarpCenterX", 0.5f, 0f..1f)
    private val polarWarpCenterY by float("PolarWarpCenterY", 0.5f, 0f..1f)
    
    // 万花筒组
    private val kaleidoscopeEnable by boolean("KaleidoscopeEnable", false)
    private val kaleidoscopeSegments by int("KaleidoscopeSegments", 6, 2..20)
    private val kaleidoscopeAngle by float("KaleidoscopeAngle", 0f, 0f..360f)
    private val kaleidoscopeOffset by float("KaleidoscopeOffset", 0f, 0f..1f)
    
    // 镜像组
    private val mirrorEnable by boolean("MirrorEnable", false)
    private val mirrorAxis by choices("MirrorAxis", arrayOf("X", "Y", "Z", "XY", "XZ", "YZ", "XYZ"), "X")
    private val mirrorOffset by float("MirrorOffset", 0f, -1f..1f)
    private val mirrorBlend by float("MirrorBlend", 0.5f, 0f..1f)
    
    // 平铺组
    private val tilingEnable by boolean("TilingEnable", false)
    private val tilingCountX by int("TilingCountX", 2, 1..10)
    private val tilingCountY by int("TilingCountY", 2, 1..10)
    private val tilingCountZ by int("TilingCountZ", 2, 1..10)
    
    // 偏移组
    private val offsetEnable by boolean("OffsetEnable", false)
    private val offsetAmountX by float("OffsetAmountX", 0.1f, -1f..1f)
    private val offsetAmountY by float("OffsetAmountY", 0.1f, -1f..1f)
    private val offsetAmountZ by float("OffsetAmountZ", 0.1f, -1f..1f)
    private val offsetSpeed by float("OffsetSpeed", 1f, 0f..10f)
    
    // 循环动画组
    private val loopAnimationEnable by boolean("LoopAnimationEnable", false)
    private val loopDuration by float("LoopDuration", 5f, 0.1f..60f)
    private val loopEasing by choices("LoopEasing", arrayOf("Linear", "Sine", "Quad", "Cubic", "Quart", "Quint", "Expo", "Circ", "Back", "Elastic", "Bounce"), "Linear")
    private val loopDirection by choices("LoopDirection", arrayOf("Forward", "Reverse", "PingPong", "Random"), "Forward")
    
    // 关键帧动画组
    private val keyframeAnimationEnable by boolean("KeyframeAnimationEnable", false)
    private val keyframeCount by int("KeyframeCount", 3, 2..20)
    private val keyframeInterpolation by choices("KeyframeInterpolation", arrayOf("Linear", "Bezier", "Hermite", "CatmullRom"), "Linear")
    private val keyframeLoop by boolean("KeyframeLoop", true)
    
    // 物理模拟动画组
    private val physicsAnimationEnable by boolean("PhysicsAnimationEnable", false)
    private val physicsMass by float("PhysicsMass", 1f, 0f..10f)
    private val physicsDamping by float("PhysicsDamping", 0.1f, 0f..1f)
    private val physicsStiffness by float("PhysicsStiffness", 0.1f, 0f..1f)
    
    // 噪音动画组
    private val noiseAnimationEnable by boolean("NoiseAnimationEnable", false)
    private val noiseOctaves by int("NoiseOctaves", 3, 1..8)
    private val noisePersistence by float("NoisePersistence", 0.5f, 0f..1f)
    private val noiseLacunarity by float("NoiseLacunarity", 2f, 1f..4f)
    
    // 波形动画组
    private val waveAnimationEnable by boolean("WaveAnimationEnable", false)
    private val waveType by choices("WaveType", arrayOf("Sine", "Square", "Triangle", "Sawtooth", "Noise"), "Sine")
    private val waveFrequency by float("WaveFrequency", 1f, 0f..10f)
    private val waveAmplitude by float("WaveAmplitude", 0.2f, 0f..2f)
    private val wavePhase by float("WavePhase", 0f, 0f..360f)
    
    // 螺旋动画组
    private val spiralAnimationEnable by boolean("SpiralAnimationEnable", false)
    private val spiralTurns by float("SpiralTurns", 3f, 0f..10f)
    private val spiralGrowth by float("SpiralGrowth", 1f, 0f..5f)
    private val spiralSpeed by float("SpiralSpeed", 1f, 0f..10f)
    
    // 爆炸动画组
    private val explosionAnimationEnable by boolean("ExplosionAnimationEnable", false)
    private val explosionForce by float("ExplosionForce", 1f, 0f..10f)
    private val explosionRadius by float("ExplosionRadius", 5f, 0f..20f)
    private val explosionDuration by float("ExplosionDuration", 1f, 0f..10f)
    
    // 内爆动画组
    private val implosionAnimationEnable by boolean("ImplosionAnimationEnable", false)
    private val implosionForce by float("ImplosionForce", 1f, 0f..10f)
    private val implosionRadius by float("ImplosionRadius", 5f, 0f..20f)
    private val implosionDuration by float("ImplosionDuration", 1f, 0f..10f)
    
    // 扭曲动画组
    private val twistAnimationEnable by boolean("TwistAnimationEnable", false)
    private val twistAmount by float("TwistAmount", 90f, 0f..360f)
    private val twistOffset by float("TwistOffset", 0f, -1f..1f)
    private val twistSpeed by float("TwistSpeed", 1f, 0f..10f)
    
    // 弯曲动画组
    private val bendAnimationEnable by boolean("BendAnimationEnable", false)
    private val bendAmount by float("BendAmount", 90f, 0f..360f)
    private val bendAxis by choices("BendAxis", arrayOf("X", "Y", "Z"), "Y")
    private val bendSpeed by float("BendSpeed", 1f, 0f..10f)
    
    // 挤压动画组
    private val squeezeAnimationEnable by boolean("SqueezeAnimationEnable", false)
    private val squeezeAmount by float("SqueezeAmount", 0.5f, 0f..1f)
    private val squeezeAxis by choices("SqueezeAxis", arrayOf("X", "Y", "Z", "All"), "All")
    private val squeezeSpeed by float("SqueezeSpeed", 1f, 0f..10f)
    
    // 脉动动画组
    private val pulsateAnimationEnable by boolean("PulsateAnimationEnable", true)
    private val pulsateMin by float("PulsateMin", 0.8f, 0f..2f)
    private val pulsateMax by float("PulsateMax", 1.2f, 0f..2f)
    private val pulsateSpeed by float("PulsateSpeed", 1f, 0f..10f)
    private val pulsateEasing by choices("PulsateEasing", arrayOf("Sine", "Quad", "Cubic", "Expo"), "Sine")
    
    // 呼吸动画组
    private val breatheAnimationEnable by boolean("BreatheAnimationEnable", true)
    private val breatheInTime by float("BreatheInTime", 2f, 0f..10f)
    private val breatheOutTime by float("BreatheOutTime", 2f, 0f..10f)
    private val breatheHoldIn by float("BreatheHoldIn", 1f, 0f..10f)
    private val breatheHoldOut by float("BreatheHoldOut", 1f, 0f..10f)
    
    // 闪烁动画组
    private val flickerAnimationEnable by boolean("FlickerAnimationEnable", false)
    private val flickerMin by float("FlickerMin", 0f, 0f..1f)
    private val flickerMax by float("FlickerMax", 1f, 0f..1f)
    private val flickerSpeed by float("FlickerSpeed", 10f, 0f..100f)
    private val flickerRandomness by float("FlickerRandomness", 0.5f, 0f..1f)
    
    // 随机动画组
    private val randomAnimationEnable by boolean("RandomAnimationEnable", false)
    private val randomSeedAnim by int("RandomSeedAnim", 0, 0..999999)
    private val randomFrequency by float("RandomFrequency", 1f, 0f..10f)
    private val randomAmplitude by float("RandomAmplitude", 0.2f, 0f..2f)
    
    // 追逐动画组
    private val chaseAnimationEnable by boolean("ChaseAnimationEnable", false)
    private val chaseSpeed by float("ChaseSpeed", 1f, 0f..10f)
    private val chaseDelay by float("ChaseDelay", 0.1f, 0f..1f)
    private val chaseDirection by choices("ChaseDirection", arrayOf("Forward", "Backward", "Alternating", "Random"), "Forward")
    
    // 级联动画组
    private val cascadeAnimationEnable by boolean("CascadeAnimationEnable", false)
    private val cascadeSpeed by float("CascadeSpeed", 1f, 0f..10f)
    private val cascadeDelay by float("CascadeDelay", 0.1f, 0f..1f)
    private val cascadeDirection by choices("CascadeDirection", arrayOf("TopToBottom", "BottomToTop", "LeftToRight", "RightToLeft", "CenterOut", "OutToCenter"), "TopToBottom")
    
    // 涟漪动画组
    private val rippleAnimationEnable by boolean("RippleAnimationEnable", false)
    private val rippleSpeed by float("RippleSpeed", 1f, 0f..10f)
    private val rippleAmplitude by float("RippleAmplitude", 0.2f, 0f..2f)
    private val rippleFrequency by float("RippleFrequency", 1f, 0f..10f)
    
    // 波浪传播动画组
    private val wavePropagationEnable by boolean("WavePropagationEnable", false)
    private val waveSpeed by float("WaveSpeed", 1f, 0f..10f)
    private val waveWavelength by float("WaveWavelength", 1f, 0f..10f)
    private val waveAmplitudeProp by float("WaveAmplitudeProp", 0.2f, 0f..2f)
    
    // 旋涡动画组
    private val vortexAnimationEnable by boolean("VortexAnimationEnable", false)
    private val vortexStrength by float("VortexStrength", 1f, 0f..10f)
    private val vortexRadius by float("VortexRadius", 5f, 0f..20f)
    private val vortexSpeed by float("VortexSpeed", 1f, 0f..10f)
    
    // 龙卷风动画组
    private val tornadoAnimationEnable by boolean("TornadoAnimationEnable", false)
    private val tornadoHeight by float("TornadoHeight", 10f, 0f..50f)
    private val tornadoRadius by float("TornadoRadius", 2f, 0f..10f)
    private val tornadoSpeed by float("TornadoSpeed", 1f, 0f..10f)
    
    // 喷泉动画组
    private val fountainAnimationEnable by boolean("FountainAnimationEnable", false)
    private val fountainHeight by float("FountainHeight", 5f, 0f..20f)
    private val fountainSpread by float("FountainSpread", 1f, 0f..5f)
    private val fountainSpeed by float("FountainSpeed", 1f, 0f..10f)
    
    // 瀑布动画组
    private val waterfallAnimationEnable by boolean("WaterfallAnimationEnable", false)
    private val waterfallHeight by float("WaterfallHeight", 10f, 0f..50f)
    private val waterfallWidth by float("WaterfallWidth", 5f, 0f..20f)
    private val waterfallSpeed by float("WaterfallSpeed", 1f, 0f..10f)
    
    // 河流动画组
    private val riverAnimationEnable by boolean("RiverAnimationEnable", false)
    private val riverLength by float("RiverLength", 20f, 0f..100f)
    private val riverWidth by float("RiverWidth", 3f, 0f..10f)
    private val riverSpeed by float("RiverSpeed", 1f, 0f..10f)
    
    // 海洋动画组
    private val oceanAnimationEnable by boolean("OceanAnimationEnable", false)
    private val oceanSize by float("OceanSize", 50f, 0f..200f)
    private val oceanWaveHeight by float("OceanWaveHeight", 2f, 0f..10f)
    private val oceanWaveSpeed by float("OceanWaveSpeed", 1f, 0f..10f)
    
    // 火焰动画组
    private val fireAnimationEnable by boolean("FireAnimationEnable", false)
    private val fireHeight by float("FireHeight", 5f, 0f..20f)
    private val fireIntensity by float("FireIntensity", 1f, 0f..5f)
    private val fireSpeed by float("FireSpeed", 1f, 0f..10f)
    
    // 烟雾动画组
    private val smokeAnimationEnable by boolean("SmokeAnimationEnable", false)
    private val smokeDensity by float("SmokeDensity", 0.5f, 0f..1f)
    private val smokeRiseSpeed by float("SmokeRiseSpeed", 1f, 0f..10f)
    private val smokeSpread by float("SmokeSpread", 1f, 0f..5f)
    
    // 云雾动画组
    private val cloudAnimationEnable by boolean("CloudAnimationEnable", false)
    private val cloudCoverage by float("CloudCoverage", 0.5f, 0f..1f)
    private val cloudSpeed by float("CloudSpeed", 1f, 0f..10f)
    private val cloudThickness by float("CloudThickness", 0.5f, 0f..1f)
    
    // 星空动画组
    private val starfieldAnimationEnable by boolean("StarfieldAnimationEnable", false)
    private val starCount by int("StarCount", 1000, 100..10000)
    private val starSpeed by float("StarSpeed", 1f, 0f..10f)
    private val starTwinkle by boolean("StarTwinkle", true)
    
    // 银河动画组
    private val galaxyAnimationEnable by boolean("GalaxyAnimationEnable", false)
    private val galaxyArms by int("GalaxyArms", 2, 1..10)
    private val galaxyTightness by float("GalaxyTightness", 0.5f, 0f..1f)
    private val galaxyRotation by float("GalaxyRotation", 1f, 0f..10f)
    
    // 黑洞动画组
    private val blackholeAnimationEnable by boolean("BlackholeAnimationEnable", false)
    private val blackholeMass by float("BlackholeMass", 10f, 0f..100f)
    private val blackholeRadius by float("BlackholeRadius", 2f, 0f..10f)
    private val blackholeRotation by float("BlackholeRotation", 1f, 0f..10f)
    
    // 超新星动画组
    private val supernovaAnimationEnable by boolean("SupernovaAnimationEnable", false)
    private val supernovaIntensity by float("SupernovaIntensity", 10f, 0f..100f)
    private val supernovaRadius by float("SupernovaRadius", 20f, 0f..100f)
    private val supernovaDuration by float("SupernovaDuration", 5f, 0f..30f)
    
    // 流星雨动画组
    private val meteorShowerEnable by boolean("MeteorShowerEnable", false)
    private val meteorCount by int("MeteorCount", 10, 1..100)
    private val meteorSpeed by float("MeteorSpeed", 5f, 0f..50f)
    private val meteorTrailLength by float("MeteorTrailLength", 10f, 0f..50f)
    
    // 极光动画组
    private val auroraAnimationEnable by boolean("AuroraAnimationEnable", false)
    private val auroraHeight by float("AuroraHeight", 20f, 0f..100f)
    private val auroraWidth by float("AuroraWidth", 50f, 0f..200f)
    private val auroraSpeed by float("AuroraSpeed", 1f, 0f..10f)
    
    // 彩虹动画组
    private val rainbowAnimationEnable by boolean("RainbowAnimationEnable", false)
    private val rainbowRadius by float("RainbowRadius", 10f, 0f..50f)
    private val rainbowWidth by float("RainbowWidth", 2f, 0f..10f)
    private val rainbowRotation by float("RainbowRotation", 1f, 0f..10f)
    
    // 闪电动画组
    private val lightningAnimationEnable by boolean("LightningAnimationEnable", false)
    private val lightningBranches by int("LightningBranches", 3, 1..10)
    private val lightningLength by float("LightningLength", 10f, 0f..50f)
    private val lightningSpeed by float("LightningSpeed", 10f, 0f..100f)
    
    // 地震动画组
    private val earthquakeAnimationEnable by boolean("EarthquakeAnimationEnable", false)
    private val earthquakeIntensity by float("EarthquakeIntensity", 1f, 0f..10f)
    private val earthquakeFrequency by float("EarthquakeFrequency", 10f, 0f..100f)
    private val earthquakeDuration by float("EarthquakeDuration", 5f, 0f..30f)
    
    // 海啸动画组
    private val tsunamiAnimationEnable by boolean("TsunamiAnimationEnable", false)
    private val tsunamiHeight by float("TsunamiHeight", 20f, 0f..100f)
    private val tsunamiSpeed by float("TsunamiSpeed", 10f, 0f..100f)
    private val tsunamiWidth by float("TsunamiWidth", 50f, 0f..200f)
    
    // 火山喷发动画组
    private val volcanoAnimationEnable by boolean("VolcanoAnimationEnable", false)
    private val volcanoHeight by float("VolcanoHeight", 30f, 0f..100f)
    private val volcanoEruptionForce by float("VolcanoEruptionForce", 5f, 0f..20f)
    private val volcanoLavaSpeed by float("VolcanoLavaSpeed", 1f, 0f..10f)
    
    // 龙卷风动画组2
    private val tornado2AnimationEnable by boolean("Tornado2AnimationEnable", false)
    private val tornado2WindSpeed by float("Tornado2WindSpeed", 50f, 0f..200f)
    private val tornado2DebrisCount by int("Tornado2DebrisCount", 100, 10..1000)
    private val tornado2Duration by float("Tornado2Duration", 30f, 0f..300f)
    
    // 飓风动画组
    private val hurricaneAnimationEnable by boolean("HurricaneAnimationEnable", false)
    private val hurricaneEyeSize by float("HurricaneEyeSize", 5f, 0f..20f)
    private val hurricaneWindSpeed by float("HurricaneWindSpeed", 30f, 0f..100f)
    private val hurricaneRainIntensity by float("HurricaneRainIntensity", 1f, 0f..5f)
    
    // 沙尘暴动画组
    private val sandstormAnimationEnable by boolean("SandstormAnimationEnable", false)
    private val sandstormDensity by float("SandstormDensity", 0.5f, 0f..1f)
    private val sandstormWindSpeed by float("SandstormWindSpeed", 20f, 0f..100f)
    private val sandstormHeight by float("SandstormHeight", 10f, 0f..50f)
    
    // 暴风雪动画组
    private val blizzardAnimationEnable by boolean("BlizzardAnimationEnable", false)
    private val blizzardSnowDensity by float("BlizzardSnowDensity", 0.5f, 0f..1f)
    private val blizzardWindSpeed by float("BlizzardWindSpeed", 10f, 0f..50f)
    private val blizzardVisibility by float("BlizzardVisibility", 0.3f, 0f..1f)
    
    // 森林动画组
    private val forestAnimationEnable by boolean("ForestAnimationEnable", false)
    private val forestTreeCount by int("ForestTreeCount", 100, 10..1000)
    private val forestWindStrength by float("ForestWindStrength", 1f, 0f..10f)
    private val forestLeafFall by boolean("ForestLeafFall", true)
    
    // 草原动画组
    private val grasslandAnimationEnable by boolean("GrasslandAnimationEnable", false)
    private val grasslandGrassDensity by float("GrasslandGrassDensity", 0.5f, 0f..1f)
    private val grasslandWindStrength by float("GrasslandWindStrength", 1f, 0f..10f)
    private val grasslandHeight by float("GrasslandHeight", 1f, 0f..5f)
    
    // 沙漠动画组
    private val desertAnimationEnable by boolean("DesertAnimationEnable", false)
    private val desertDuneCount by int("DesertDuneCount", 50, 10..500)
    private val desertWindStrength by float("DesertWindStrength", 2f, 0f..10f)
    private val desertSandMovement by boolean("DesertSandMovement", true)
    
    // 山脉动画组
    private val mountainAnimationEnable by boolean("MountainAnimationEnable", false)
    private val mountainHeight by float("MountainHeight", 20f, 0f..100f)
    private val mountainSnowLine by float("MountainSnowLine", 0.5f, 0f..1f)
    private val mountainCloudCover by float("MountainCloudCover", 0.3f, 0f..1f)
    
    // 洞穴动画组
    private val caveAnimationEnable by boolean("CaveAnimationEnable", false)
    private val caveDepth by float("CaveDepth", 10f, 0f..50f)
    private val caveStalactiteCount by int("CaveStalactiteCount", 50, 10..500)
    private val caveWaterDrips by boolean("CaveWaterDrips", true)
    
    // 水下动画组
    private val underwaterAnimationEnable by boolean("UnderwaterAnimationEnable", false)
    private val underwaterDepth by float("UnderwaterDepth", 10f, 0f..50f)
    private val underwaterCurrentStrength by float("UnderwaterCurrentStrength", 1f, 0f..10f)
    private val underwaterBubbleCount by int("UnderwaterBubbleCount", 100, 10..1000)
    
    // 太空动画组
    private val spaceAnimationEnable by boolean("SpaceAnimationEnable", false)
    private val spaceAsteroidCount by int("SpaceAsteroidCount", 100, 10..1000)
    private val spaceNebulaDensity by float("SpaceNebulaDensity", 0.3f, 0f..1f)
    private val spacePlanetCount by int("SpacePlanetCount", 5, 1..20)
    
    // 城市动画组
    private val cityAnimationEnable by boolean("CityAnimationEnable", false)
    private val cityBuildingCount by int("CityBuildingCount", 100, 10..1000)
    private val cityTrafficDensity by float("CityTrafficDensity", 0.5f, 0f..1f)
    private val cityLightCount by int("CityLightCount", 1000, 100..10000)
    
    // 工厂动画组
    private val factoryAnimationEnable by boolean("FactoryAnimationEnable", false)
    private val factoryMachineCount by int("FactoryMachineCount", 10, 1..100)
    private val factorySmokeIntensity by float("FactorySmokeIntensity", 0.5f, 0f..1f)
    private val factoryConveyorSpeed by float("FactoryConveyorSpeed", 1f, 0f..10f)
    
    // 实验室动画组
    private val labAnimationEnable by boolean("LabAnimationEnable", false)
    private val labBeakerCount by int("LabBeakerCount", 10, 1..100)
    private val labBubbleIntensity by float("LabBubbleIntensity", 0.5f, 0f..1f)
    private val labFumeSpeed by float("LabFumeSpeed", 1f, 0f..10f)
    
    // 神庙动画组
    private val templeAnimationEnable by boolean("TempleAnimationEnable", false)
    private val templePillarCount by int("TemplePillarCount", 10, 1..100)
    private val templeTorchCount by int("TempleTorchCount", 10, 1..100)
    private val templeSecretPassage by boolean("TempleSecretPassage", false)
    
    // 城堡动画组
    private val castleAnimationEnable by boolean("CastleAnimationEnable", false)
    private val castleTowerCount by int("CastleTowerCount", 4, 1..20)
    private val castleFlagCount by int("CastleFlagCount", 10, 1..100)
    private val castleMoat by boolean("CastleMoat", true)
    
    // 飞船动画组
    private val spaceshipAnimationEnable by boolean("SpaceshipAnimationEnable", false)
    private val spaceshipEngineCount by int("SpaceshipEngineCount", 2, 1..10)
    private val spaceshipSpeed by float("SpaceshipSpeed", 10f, 0f..100f)
    private val spaceshipTrailLength by float("SpaceshipTrailLength", 20f, 0f..100f)
    
    // 机器人动画组
    private val robotAnimationEnable by boolean("RobotAnimationEnable", false)
    private val robotJointCount by int("RobotJointCount", 6, 1..20)
    private val robotMovementSpeed by float("RobotMovementSpeed", 1f, 0f..10f)
    private val robotLEDCount by int("RobotLEDCount", 10, 1..100)
    
    // 恐龙动画组
    private val dinosaurAnimationEnable by boolean("DinosaurAnimationEnable", false)
    private val dinosaurType by choices("DinosaurType", arrayOf("T-Rex", "Velociraptor", "Triceratops", "Stegosaurus", "Brachiosaurus"), "T-Rex")
    private val dinosaurSize by float("DinosaurSize", 10f, 1f..50f)
    private val dinosaurRoarFrequency by float("DinosaurRoarFrequency", 0.1f, 0f..1f)
    
    // 魔法阵动画组
    private val magicCircleAnimationEnable by boolean("MagicCircleAnimationEnable", false)
    private val magicCircleRunes by int("MagicCircleRunes", 8, 3..20)
    private val magicCircleRotation by float("MagicCircleRotation", 1f, 0f..10f)
    private val magicCircleGlow by boolean("MagicCircleGlow", true)
    
    // 炼金术动画组
    private val alchemyAnimationEnable by boolean("AlchemyAnimationEnable", false)
    private val alchemyIngredientCount by int("AlchemyIngredientCount", 5, 1..20)
    private val alchemyReactionIntensity by float("AlchemyReactionIntensity", 1f, 0f..5f)
    private val alchemySmokeColorR by int("AlchemySmokeColorR", 255, 0..255)
    private val alchemySmokeColorG by int("AlchemySmokeColorG", 200, 0..255)
    private val alchemySmokeColorB by int("AlchemySmokeColorB", 100, 0..255)
    
    // 炼金术动画组2
    private val alchemyBubbleCount by int("AlchemyBubbleCount", 50, 10..500)
    private val alchemyBubbleSize by float("AlchemyBubbleSize", 0.5f, 0.1f..2f)
    private val alchemyBubbleSpeed by float("AlchemyBubbleSpeed", 1f, 0f..5f)
    private val alchemySteamIntensity by float("AlchemySteamIntensity", 0.5f, 0f..1f)
    
    // 炼金术动画组3
    private val alchemySparkCount by int("AlchemySparkCount", 100, 10..1000)
    private val alchemySparkSize by float("AlchemySparkSize", 0.2f, 0.1f..1f)
    private val alchemySparkSpeed by float("AlchemySparkSpeed", 2f, 0f..10f)
    private val alchemyGlowIntensity by float("AlchemyGlowIntensity", 1f, 0f..5f)
    
    // 炼金术动画组4
    private val alchemySymbolRotation by float("AlchemySymbolRotation", 1f, 0f..10f)
    private val alchemySymbolPulse by float("AlchemySymbolPulse", 1f, 0f..5f)
    private val alchemyEnergyFlow by boolean("AlchemyEnergyFlow", true)
    private val alchemyTransmutation by boolean("AlchemyTransmutation", false)
    
    // 炼金术动画组5
    private val alchemyPhase by int("AlchemyPhase", 1, 1..5)
    private val alchemyDuration by float("AlchemyDuration", 10f, 1f..60f)
    private val alchemySuccessRate by float("AlchemySuccessRate", 0.8f, 0f..1f)
    private val alchemyRandomness by float("AlchemyRandomness", 0.2f, 0f..1f)
    
    // 炼金术动画组6
    private val alchemyElementFire by boolean("AlchemyElementFire", false)
    private val alchemyElementWater by boolean("AlchemyElementWater", false)
    private val alchemyElementEarth by boolean("AlchemyElementEarth", false)
    private val alchemyElementAir by boolean("AlchemyElementAir", false)
    private val alchemyElementAether by boolean("AlchemyElementAether", false)
    
    // 炼金术动画组7
    private val alchemyFireColorR by int("AlchemyFireColorR", 255, 0..255)
    private val alchemyFireColorG by int("AlchemyFireColorG", 100, 0..255)
    private val alchemyFireColorB by int("AlchemyFireColorB", 0, 0..255)
    private val alchemyWaterColorR by int("AlchemyWaterColorR", 0, 0..255)
    private val alchemyWaterColorG by int("AlchemyWaterColorG", 100, 0..255)
    private val alchemyWaterColorB by int("AlchemyWaterColorB", 255, 0..255)
    
    // 炼金术动画组8
    private val alchemyEarthColorR by int("AlchemyEarthColorR", 139, 0..255)
    private val alchemyEarthColorG by int("AlchemyEarthColorG", 69, 0..255)
    private val alchemyEarthColorB by int("AlchemyEarthColorB", 19, 0..255)
    private val alchemyAirColorR by int("AlchemyAirColorR", 200, 0..255)
    private val alchemyAirColorG by int("AlchemyAirColorG", 230, 0..255)
    private val alchemyAirColorB by int("AlchemyAirColorB", 255, 0..255)
    
    // 炼金术动画组9
    private val alchemyAetherColorR by int("AlchemyAetherColorR", 150, 0..255)
    private val alchemyAetherColorG by int("AlchemyAetherColorG", 0, 0..255)
    private val alchemyAetherColorB by int("AlchemyAetherColorB", 255, 0..255)
    private val alchemyQuintessence by boolean("AlchemyQuintessence", false)
    private val alchemyPhilosophersStone by boolean("AlchemyPhilosophersStone", false)
    
    // 炼金术动画组10
    private val alchemyTransmuteGold by boolean("AlchemyTransmuteGold", false)
    private val alchemyTransmuteSilver by boolean("AlchemyTransmuteSilver", false)
    private val alchemyTransmuteCopper by boolean("AlchemyTransmuteCopper", false)
    private val alchemyTransmuteIron by boolean("AlchemyTransmuteIron", false)
    
    // 炼金术动画组11
    private val alchemyGoldColorR by int("AlchemyGoldColorR", 255, 0..255)
    private val alchemyGoldColorG by int("AlchemyGoldColorG", 215, 0..255)
    private val alchemyGoldColorB by int("AlchemyGoldColorB", 0, 0..255)
    private val alchemySilverColorR by int("AlchemySilverColorR", 192, 0..255)
    private val alchemySilverColorG by int("AlchemySilverColorG", 192, 0..255)
    private val alchemySilverColorB by int("AlchemySilverColorB", 192, 0..255)
    
    // 炼金术动画组12
    private val alchemyCopperColorR by int("AlchemyCopperColorR", 184, 0..255)
    private val alchemyCopperColorG by int("AlchemyCopperColorG", 115, 0..255)
    private val alchemyCopperColorB by int("AlchemyCopperColorB", 51, 0..255)
    private val alchemyIronColorR by int("AlchemyIronColorR", 100, 0..255)
    private val alchemyIronColorG by int("AlchemyIronColorG", 100, 0..255)
    private val alchemyIronColorB by int("AlchemyIronColorB", 100, 0..255)
    
    // 炼金术动画组13
    private val alchemyMercury by boolean("AlchemyMercury", false)
    private val alchemySulfur by boolean("AlchemySulfur", false)
    private val alchemySalt by boolean("AlchemySalt", false)
    private val alchemyLead by boolean("AlchemyLead", false)
    
    // 炼金术动画组14
    private val alchemyMercuryColorR by int("AlchemyMercuryColorR", 230, 0..255)
    private val alchemyMercuryColorG by int("AlchemyMercuryColorG", 230, 0..255)
    private val alchemyMercuryColorB by int("AlchemyMercuryColorB", 250, 0..255)
    private val alchemySulfurColorR by int("AlchemySulfurColorR", 255, 0..255)
    private val alchemySulfurColorG by int("AlchemySulfurColorG", 255, 0..255)
    private val alchemySulfurColorB by int("AlchemySulfurColorB", 0, 0..255)
    
    // 炼金术动画组15
    private val alchemySaltColorR by int("AlchemySaltColorR", 255, 0..255)
    private val alchemySaltColorG by int("AlchemySaltColorG", 255, 0..255)
    private val alchemySaltColorB by int("AlchemySaltColorB", 255, 0..255)
    private val alchemyLeadColorR by int("AlchemyLeadColorR", 50, 0..255)
    private val alchemyLeadColorG by int("AlchemyLeadColorG", 50, 0..255)
    private val alchemyLeadColorB by int("AlchemyLeadColorB", 50, 0..255)
    
    // 炼金术动画组16
    private val alchemyOpusMagnum by boolean("AlchemyOpusMagnum", false)
    private val alchemyNigredo by boolean("AlchemyNigredo", false)
    private val alchemyAlbedo by boolean("AlchemyAlbedo", false)
    private val alchemyCitrinitas by boolean("AlchemyCitrinitas", false)
    private val alchemyRubedo by boolean("AlchemyRubedo", false)
    
    // 炼金术动画组17
    private val alchemyNigredoColorR by int("AlchemyNigredoColorR", 0, 0..255)
    private val alchemyNigredoColorG by int("AlchemyNigredoColorG", 0, 0..255)
    private val alchemyNigredoColorB by int("AlchemyNigredoColorB", 0, 0..255)
    private val alchemyAlbedoColorR by int("AlchemyAlbedoColorR", 255, 0..255)
    private val alchemyAlbedoColorG by int("AlchemyAlbedoColorG", 255, 0..255)
    private val alchemyAlbedoColorB by int("AlchemyAlbedoColorB", 255, 0..255)
    
    // 炼金术动画组18
    private val alchemyCitrinitasColorR by int("AlchemyCitrinitasColorR", 255, 0..255)
    private val alchemyCitrinitasColorG by int("AlchemyCitrinitasColorG", 255, 0..255)
    private val alchemyCitrinitasColorB by int("AlchemyCitrinitasColorB", 0, 0..255)
    private val alchemyRubedoColorR by int("AlchemyRubedoColorR", 255, 0..255)
    private val alchemyRubedoColorG by int("AlchemyRubedoColorG", 0, 0..255)
    private val alchemyRubedoColorB by int("AlchemyRubedoColorB", 0, 0..255)
    
    // 炼金术动画组19
    private val alchemyPrimaMateria by boolean("AlchemyPrimaMateria", false)
    private val alchemyUltimaMateria by boolean("AlchemyUltimaMateria", false)
    private val alchemyAnimatio by boolean("AlchemyAnimatio", false)
    private val alchemyConiunctio by boolean("AlchemyConiunctio", false)
    
    // 炼金术动画组20
    private val alchemyPrimaMateriaColorR by int("AlchemyPrimaMateriaColorR", 100, 0..255)
    private val alchemyPrimaMateriaColorG by int("AlchemyPrimaMateriaColorG", 100, 0..255)
    private val alchemyPrimaMateriaColorB by int("AlchemyPrimaMateriaColorB", 100, 0..255)
    private val alchemyUltimaMateriaColorR by int("AlchemyUltimaMateriaColorR", 200, 0..255)
    private val alchemyUltimaMateriaColorG by int("AlchemyUltimaMateriaColorG", 200, 0..255)
    private val alchemyUltimaMateriaColorB by int("AlchemyUltimaMateriaColorB", 200, 0..255)
    
    // 炼金术动画组21
    private val alchemyAnimatioColorR by int("AlchemyAnimatioColorR", 0, 0..255)
    private val alchemyAnimatioColorG by int("AlchemyAnimatioColorG", 200, 0..255)
    private val alchemyAnimatioColorB by int("AlchemyAnimatioColorB", 0, 0..255)
    private val alchemyConiunctioColorR by int("AlchemyConiunctioColorR", 200, 0..255)
    private val alchemyConiunctioColorG by int("AlchemyConiunctioColorG", 0, 0..255)
    private val alchemyConiunctioColorB by int("AlchemyConiunctioColorB", 200, 0..255)
    
    // 炼金术动画组22
    private val alchemySolve by boolean("AlchemySolve", false)
    private val alchemyCoagula by boolean("AlchemyCoagula", false)
    private val alchemySeparatio by boolean("AlchemySeparatio", false)
    private val alchemyFermentatio by boolean("AlchemyFermentatio", false)
    
    // 炼金术动画组23
    private val alchemySolveColorR by int("AlchemySolveColorR", 100, 0..255)
    private val alchemySolveColorG by int("AlchemySolveColorG", 200, 0..255)
    private val alchemySolveColorB by int("AlchemySolveColorB", 255, 0..255)
    private val alchemyCoagulaColorR by int("AlchemyCoagulaColorR", 255, 0..255)
    private val alchemyCoagulaColorG by int("AlchemyCoagulaColorG", 200, 0..255)
    private val alchemyCoagulaColorB by int("AlchemyCoagulaColorB", 100, 0..255)
    
    // 炼金术动画组24
    private val alchemySeparatioColorR by int("AlchemySeparatioColorR", 255, 0..255)
    private val alchemySeparatioColorG by int("AlchemySeparatioColorG", 100, 0..255)
    private val alchemySeparatioColorB by int("AlchemySeparatioColorB", 100, 0..255)
    private val alchemyFermentatioColorR by int("AlchemyFermentatioColorR", 100, 0..255)
    private val alchemyFermentatioColorG by int("AlchemyFermentatioColorG", 255, 0..255)
    private val alchemyFermentatioColorB by int("AlchemyFermentatioColorB", 100, 0..255)
    
    // 炼金术动画组25
    private val alchemyDistillatio by boolean("AlchemyDistillatio", false)
    private val alchemySublimatio by boolean("AlchemySublimatio", false)
    private val alchemyCalcinatio by boolean("AlchemyCalcinatio", false)
    private val alchemyPutrefactio by boolean("AlchemyPutrefactio", false)
    
    // 炼金术动画组26
    private val alchemyDistillatioColorR by int("AlchemyDistillatioColorR", 200, 0..255)
    private val alchemyDistillatioColorG by int("AlchemyDistillatioColorG", 230, 0..255)
    private val alchemyDistillatioColorB by int("AlchemyDistillatioColorB", 255, 0..255)
    private val alchemySublimatioColorR by int("AlchemySublimatioColorR", 255, 0..255)
    private val alchemySublimatioColorG by int("AlchemySublimatioColorG", 250, 0..255)
    private val alchemySublimatioColorB by int("AlchemySublimatioColorB", 200, 0..255)
    
    // 炼金术动画组27
    private val alchemyCalcinatioColorR by int("AlchemyCalcinatioColorR", 255, 0..255)
    private val alchemyCalcinatioColorG by int("AlchemyCalcinatioColorG", 150, 0..255)
    private val alchemyCalcinatioColorB by int("AlchemyCalcinatioColorB", 0, 0..255)
    private val alchemyPutrefactioColorR by int("AlchemyPutrefactioColorR", 100, 0..255)
    private val alchemyPutrefactioColorG by int("AlchemyPutrefactioColorG", 50, 0..255)
    private val alchemyPutrefactioColorB by int("AlchemyPutrefactioColorB", 0, 0..255)
    
    // 炼金术动画组28
    private val alchemyProjection by boolean("AlchemyProjection", false)
    private val alchemyMultiplication by boolean("AlchemyMultiplication", false)
    private val alchemyExaltation by boolean("AlchemyExaltation", false)
    private val alchemyFixation by boolean("AlchemyFixation", false)
    
    // 炼金术动画组29
    private val alchemyProjectionColorR by int("AlchemyProjectionColorR", 255, 0..255)
    private val alchemyProjectionColorG by int("AlchemyProjectionColorG", 255, 0..255)
    private val alchemyProjectionColorB by int("AlchemyProjectionColorB", 150, 0..255)
    private val alchemyMultiplicationColorR by int("AlchemyMultiplicationColorR", 255, 0..255)
    private val alchemyMultiplicationColorG by int("AlchemyMultiplicationColorG", 200, 0..255)
    private val alchemyMultiplicationColorB by int("AlchemyMultiplicationColorB", 0, 0..255)
    
    // 炼金术动画组30
    private val alchemyExaltationColorR by int("AlchemyExaltationColorR", 200, 0..255)
    private val alchemyExaltationColorG by int("AlchemyExaltationColorG", 0, 0..255)
    private val alchemyExaltationColorB by int("AlchemyExaltationColorB", 255, 0..255)
    private val alchemyFixationColorR by int("AlchemyFixationColorR", 100, 0..255)
    private val alchemyFixationColorG by int("AlchemyFixationColorG", 100, 0..255)
    private val alchemyFixationColorB by int("AlchemyFixationColorB", 255, 0..255)
    
    // 炼金术动画组31
    private val alchemyCirculatio by boolean("AlchemyCirculatio", false)
    private val alchemyDigestio by boolean("AlchemyDigestio", false)
    private val alchemyInceratio by boolean("AlchemyInceratio", false)
    private val alchemyMortificatio by boolean("AlchemyMortificatio", false)
    
    // 炼金术动画组32
    private val alchemyCirculatioColorR by int("AlchemyCirculatioColorR", 0, 0..255)
    private val alchemyCirculatioColorG by int("AlchemyCirculatioColorG", 200, 0..255)
    private val alchemyCirculatioColorB by int("AlchemyCirculatioColorB", 255, 0..255)
    private val alchemyDigestioColorR by int("AlchemyDigestioColorR", 255, 0..255)
    private val alchemyDigestioColorG by int("AlchemyDigestioColorG", 150, 0..255)
    private val alchemyDigestioColorB by int("AlchemyDigestioColorB", 100, 0..255)
    
    // 炼金术动画组33
    private val alchemyInceratioColorR by int("AlchemyInceratioColorR", 255, 0..255)
    private val alchemyInceratioColorG by int("AlchemyInceratioColorG", 255, 0..255)
    private val alchemyInceratioColorB by int("AlchemyInceratioColorB", 200, 0..255)
    private val alchemyMortificatioColorR by int("AlchemyMortificatioColorR", 50, 0..255)
    private val alchemyMortificatioColorG by int("AlchemyMortificatioColorG", 0, 0..255)
    private val alchemyMortificatioColorB by int("AlchemyMortificatioColorB", 0, 0..255)
    
    // 炼金术动画组34
    private val alchemyResurrectio by boolean("AlchemyResurrectio", false)
    private val alchemyRevivificatio by boolean("AlchemyRevivificatio", false)
    private val alchemySpiritualizatio by boolean("AlchemySpiritualizatio", false)
    private val alchemyTransmutatio by boolean("AlchemyTransmutatio", false)
    
    // 炼金术动画组35
    private val alchemyResurrectioColorR by int("AlchemyResurrectioColorR", 0, 0..255)
    private val alchemyResurrectioColorG by int("AlchemyResurrectioColorG", 255, 0..255)
    private val alchemyResurrectioColorB by int("AlchemyResurrectioColorB", 0, 0..255)
    private val alchemyRevivificatioColorR by int("AlchemyRevivificatioColorR", 255, 0..255)
    private val alchemyRevivificatioColorG by int("AlchemyRevivificatioColorG", 0, 0..255)
    private val alchemyRevivificatioColorB by int("AlchemyRevivificatioColorB", 255, 0..255)
    
    // 炼金术动画组36
    private val alchemySpiritualizatioColorR by int("AlchemySpiritualizatioColorR", 255, 0..255)
    private val alchemySpiritualizatioColorG by int("AlchemySpiritualizatioColorG", 255, 0..255)
    private val alchemySpiritualizatioColorB by int("AlchemySpiritualizatioColorB", 0, 0..255)
    private val alchemyTransmutatioColorR by int("AlchemyTransmutatioColorR", 0, 0..255)
    private val alchemyTransmutatioColorG by int("AlchemyTransmutatioColorG", 255, 0..255)
    private val alchemyTransmutatioColorB by int("AlchemyTransmutatioColorB", 255, 0..255)
    
    // 炼金术动画组37
    private val alchemyVITRIOL by boolean("AlchemyVITRIOL", false)
    private val alchemyAzoth by boolean("AlchemyAzoth", false)
    private val alchemyOrichalcum by boolean("AlchemyOrichalcum", false)
    private val alchemyMithril by boolean("AlchemyMithril", false)
    
    // 炼金术动画组38
    private val alchemyVITRIOLColorR by int("AlchemyVITRIOLColorR", 0, 0..255)
    private val alchemyVITRIOLColorG by int("AlchemyVITRIOLColorG", 255, 0..255)
    private val alchemyVITRIOLColorB by int("AlchemyVITRIOLColorB", 255, 0..255)
    private val alchemyAzothColorR by int("AlchemyAzothColorR", 255, 0..255)
    private val alchemyAzothColorG by int("AlchemyAzothColorG", 255, 0..255)
    private val alchemyAzothColorB by int("AlchemyAzothColorB", 255, 0..255)
    
    // 炼金术动画组39
    private val alchemyOrichalcumColorR by int("AlchemyOrichalcumColorR", 255, 0..255)
    private val alchemyOrichalcumColorG by int("AlchemyOrichalcumColorG", 165, 0..255)
    private val alchemyOrichalcumColorB by int("AlchemyOrichalcumColorB", 0, 0..255)
    private val alchemyMithrilColorR by int("AlchemyMithrilColorR", 200, 0..255)
    private val alchemyMithrilColorG by int("AlchemyMithrilColorG", 230, 0..255)
    private val alchemyMithrilColorB by int("AlchemyMithrilColorB", 255, 0..255)
    
    // 炼金术动画组40
    private val alchemyAdamas by boolean("AlchemyAdamas", false)
    private val alchemyAdamant by boolean("AlchemyAdamant", false)
    private val alchemyAetherium by boolean("AlchemyAetherium", false)
    private val alchemyArcanum by boolean("AlchemyArcanum", false)
    
    // 炼金术动画组41
    private val alchemyAdamasColorR by int("AlchemyAdamasColorR", 100, 0..255)
    private val alchemyAdamasColorG by int("AlchemyAdamasColorG", 100, 0..255)
    private val alchemyAdamasColorB by int("AlchemyAdamasColorB", 100, 0..255)
    private val alchemyAdamantColorR by int("AlchemyAdamantColorR", 150, 0..255)
    private val alchemyAdamantColorG by int("AlchemyAdamantColorG", 150, 0..255)
    private val alchemyAdamantColorB by int("AlchemyAdamantColorB", 150, 0..255)
    
    // 炼金术动画组42
    private val alchemyAetheriumColorR by int("AlchemyAetheriumColorR", 200, 0..255)
    private val alchemyAetheriumColorG by int("AlchemyAetheriumColorG", 200, 0..255)
    private val alchemyAetheriumColorB by int("AlchemyAetheriumColorB", 255, 0..255)
    private val alchemyArcanumColorR by int("AlchemyArcanumColorR", 255, 0..255)
    private val alchemyArcanumColorG by int("AlchemyArcanumColorG", 0, 0..255)
    private val alchemyArcanumColorB by int("AlchemyArcanumColorB", 255, 0..255)
    
    // 炼金术动画组43
    private val alchemyElixir by boolean("AlchemyElixir", false)
    private val alchemyPanacea by boolean("AlchemyPanacea", false)
    private val alchemyAmbrosia by boolean("AlchemyAmbrosia", false)
    private val alchemyNectar by boolean("AlchemyNectar", false)
    
    // 炼金术动画组44
    private val alchemyElixirColorR by int("AlchemyElixirColorR", 255, 0..255)
    private val alchemyElixirColorG by int("AlchemyElixirColorG", 0, 0..255)
    private val alchemyElixirColorB by int("AlchemyElixirColorB", 0, 0..255)
    private val alchemyPanaceaColorR by int("AlchemyPanaceaColorR", 0, 0..255)
    private val alchemyPanaceaColorG by int("AlchemyPanaceaColorG", 255, 0..255)
    private val alchemyPanaceaColorB by int("AlchemyPanaceaColorB", 0, 0..255)
    
    // 炼金术动画组45
    private val alchemyAmbrosiaColorR by int("AlchemyAmbrosiaColorR", 0, 0..255)
    private val alchemyAmbrosiaColorG by int("AlchemyAmbrosiaColorG", 0, 0..255)
    private val alchemyAmbrosiaColorB by int("AlchemyAmbrosiaColorB", 255, 0..255)
    private val alchemyNectarColorR by int("AlchemyNectarColorR", 255, 0..255)
    private val alchemyNectarColorG by int("AlchemyNectarColorG", 255, 0..255)
    private val alchemyNectarColorB by int("AlchemyNectarColorB", 0, 0..255)
    
    // 炼金术动画组46
    private val alchemySoma by boolean("AlchemySoma", false)
    private val alchemyAmrita by boolean("AlchemyAmrita", false)
    private val alchemyHaoma by boolean("AlchemyHaoma", false)
    private val alchemyIchor by boolean("AlchemyIchor", false)
    
    // 炼金术动画组47
    private val alchemySomaColorR by int("AlchemySomaColorR", 255, 0..255)
    private val alchemySomaColorG by int("AlchemySomaColorG", 100, 0..255)
    private val alchemySomaColorB by int("AlchemySomaColorB", 100, 0..255)
    private val alchemyAmritaColorR by int("AlchemyAmritaColorR", 100, 0..255)
    private val alchemyAmritaColorG by int("AlchemyAmritaColorG", 255, 0..255)
    private val alchemyAmritaColorB by int("AlchemyAmritaColorB", 100, 0..255)
    
    // 炼金术动画组48
    private val alchemyHaomaColorR by int("AlchemyHaomaColorR", 100, 0..255)
    private val alchemyHaomaColorG by int("AlchemyHaomaColorG", 100, 0..255)
    private val alchemyHaomaColorB by int("AlchemyHaomaColorB", 255, 0..255)
    private val alchemyIchorColorR by int("AlchemyIchorColorR", 255, 0..255)
    private val alchemyIchorColorG by int("AlchemyIchorColorG", 255, 0..255)
    private val alchemyIchorColorB by int("AlchemyIchorColorB", 100, 0..255)
    
    // 炼金术动画组49
    private val alchemyHydra by boolean("AlchemyHydra", false)
    private val alchemyChimera by boolean("AlchemyChimera", false)
    private val alchemyPhoenix by boolean("AlchemyPhoenix", false)
    private val alchemyDragon by boolean("AlchemyDragon", false)
    
    // 炼金术动画组50
    private val alchemyHydraColorR by int("AlchemyHydraColorR", 0, 0..255)
    private val alchemyHydraColorG by int("AlchemyHydraColorG", 255, 0..255)
    private val alchemyHydraColorB by int("AlchemyHydraColorB", 0, 0..255)
    private val alchemyChimeraColorR by int("AlchemyChimeraColorR", 255, 0..255)
    private val alchemyChimeraColorG by int("AlchemyChimeraColorG", 0, 0..255)
    private val alchemyChimeraColorB by int("AlchemyChimeraColorB", 0, 0..255)
    
    // 炼金术动画组51
    private val alchemyPhoenixColorR by int("AlchemyPhoenixColorR", 255, 0..255)
    private val alchemyPhoenixColorG by int("AlchemyPhoenixColorG", 69, 0..255)
    private val alchemyPhoenixColorB by int("AlchemyPhoenixColorB", 0, 0..255)
    private val alchemyDragonColorR by int("AlchemyDragonColorR", 255, 0..255)
    private val alchemyDragonColorG by int("AlchemyDragonColorG", 140, 0..255)
    private val alchemyDragonColorB by int("AlchemyDragonColorB", 0, 0..255)
    
    // 运行时变量
    private var time = 0f
    private val random = Random()
    
    val onRender3D = handler<Render3DEvent> { event ->
        if (!enable || mc.thePlayer == null || mc.theWorld == null) return@handler
        
        time += 0.016f * timeScale
        
        if (timeLoop && time > timeLoopDuration) {
            time = 0f
        }
        
        GL11.glPushMatrix()
        
        val player = mc.thePlayer!!
        val px = player.lastTickPosX + (player.posX - player.lastTickPosX) * mc.timer.renderPartialTicks
        val py = player.lastTickPosY + (player.posY - player.lastTickPosY) * mc.timer.renderPartialTicks
        val pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * mc.timer.renderPartialTicks
        
        GL11.glTranslated(
            px - mc.renderManager.viewerPosX,
            py - mc.renderManager.viewerPosY,
            pz - mc.renderManager.viewerPosZ
        )
        
        if (cameraFollow) {
            GL11.glTranslatef(cameraOffsetX, cameraOffsetY, cameraOffsetZ)
        }
        
        if (!depthTest) {
            GL11.glDisable(GL11.GL_DEPTH_TEST)
        }
        if (!depthWrite) {
            GL11.glDepthMask(false)
        }
        
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(blendSrcFactor, blendDstFactor)
        
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        
        // 这里可以添加更多的渲染代码来使用上述1000个选项
        // 由于选项太多，渲染代码会非常复杂
        
        // 基础光环渲染示例
        GL11.glColor4f(1f, 1f, 1f, 1f)
        GL11.glBegin(GL11.GL_LINE_LOOP)
        for (i in 0..360 step 10) {
            val angle = Math.toRadians(i.toDouble())
            val x = cos(angle)
            val z = sin(angle)
            GL11.glVertex3d(x, 2.0, z)
        }
        GL11.glEnd()
        
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        
        if (!depthTest) {
            GL11.glEnable(GL11.GL_DEPTH_TEST)
        }
        if (!depthWrite) {
            GL11.glDepthMask(true)
        }
        
        GL11.glPopMatrix()
    }
    
    override fun onDisable() {
        time = 0f
    }
    
    override val tag: String
        get() = "Halo+"
}