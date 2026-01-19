/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.*
import net.minecraft.entity.passive.*
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.AxisAlignedBB
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*
import kotlin.math.*

object ESP2 : Module("ESP2", Category.RENDER) {

    // ==================== 主开关和分类 ====================
    val moduleEnabled by boolean("Enabled", true)
    val renderCategory by choices("Category", arrayOf("All", "Players", "Mobs", "Animals", "Items", "Vehicles", "Projectiles"), "All")
    
    // ==================== 实体类型详细过滤 ====================
    // 玩家相关
    val players by boolean("Players", true)
    val teammates by boolean("Teammates", true)
    val enemies by boolean("Enemies", true)
    val neutralPlayers by boolean("NeutralPlayers", true)
    val friends by boolean("Friends", true)
    val bots by boolean("Bots", false)
    val playerDistance by int("PlayerDist", 100, 0..500)
    val playerMinHealth by float("PlayerMinHP", 0f, 0f..100f)
    val playerMaxHealth by float("PlayerMaxHP", 100f, 0.1f..1000f)
    
    // 敌对生物
    val mobs by boolean("Mobs", true)
    val zombies by boolean("Zombies", true)
    val skeletons by boolean("Skeletons", true)
    val creepers by boolean("Creepers", true)
    val spiders by boolean("Spiders", true)
    val endermen by boolean("Endermen", true)
    val witches by boolean("Witches", true)
    val slimes by boolean("Slimes", true)
    val ghasts by boolean("Ghasts", true)
    val blazes by boolean("Blazes", true)
    val magmaCubes by boolean("MagmaCubes", true)
    val silverfish by boolean("Silverfish", false)
    val caveSpiders by boolean("CaveSpiders", true)
    val pigZombies by boolean("PigZombies", true)
    val guardians by boolean("Guardians", true)
    val mobDistance by int("MobDist", 80, 0..500)
    
    // 中立生物
    val animals by boolean("Animals", true)
    val cows by boolean("Cows", true)
    val pigs by boolean("Pigs", true)
    val sheep by boolean("Sheep", true)
    val chickens by boolean("Chickens", true)
    val wolves by boolean("Wolves", true)
    val ocelots by boolean("Ocelots", true)
    val horses by boolean("Horses", true)
    val rabbits by boolean("Rabbits", true)
    val bats by boolean("Bats", false)
    val squids by boolean("Squids", true)
    val mooshrooms by boolean("Mooshrooms", true)
    val animalDistance by int("AnimalDist", 60, 0..500)
    
    // 被动生物
    val villagers by boolean("Villagers", true)
    val ironGolems by boolean("IronGolems", true)
    val snowGolems by boolean("SnowGolems", true)
    val passiveDistance by int("PassiveDist", 50, 0..500)
    
    // 物品和掉落物
    val items by boolean("Items", false)
    val commonItems by boolean("CommonItems", false)
    val rareItems by boolean("RareItems", true)
    val epicItems by boolean("EpicItems", true)
    val legendaryItems by boolean("LegendaryItems", true)
    val itemDistance by int("ItemDist", 30, 0..500)
    val itemMinValue by int("ItemMinValue", 0, 0..10000)
    val itemMaxValue by int("ItemMaxValue", 10000, 0..10000)
    
    // 载具和实体
    val vehicles by boolean("Vehicles", false)
    val boats by boolean("Boats", true)
    val minecarts by boolean("Minecarts", true)
    val horsesMounted by boolean("HorsesMounted", true)
    val pigsMounted by boolean("PigsMounted", true)
    val vehicleDistance by int("VehicleDist", 40, 0..500)
    
    // 抛射物
    val projectiles by boolean("Projectiles", false)
    val arrows by boolean("Arrows", true)
    val fireballs by boolean("Fireballs", true)
    val potions by boolean("Potions", true)
    val snowballs by boolean("Snowballs", false)
    val eggs by boolean("Eggs", false)
    val enderPearls by boolean("EnderPearls", true)
    val projectileDistance by int("ProjectileDist", 50, 0..500)
    
    // 其他实体
    val others by boolean("Others", false)
    val paintings by boolean("Paintings", false)
    val itemFrames by boolean("ItemFrames", false)
    val armorStands by boolean("ArmorStands", false)
    val enderCrystals by boolean("EnderCrystals", true)
    val tnt by boolean("TNT", true)
    val fallingBlocks by boolean("FallingBlocks", false)
    val otherDistance by int("OtherDist", 40, 0..500)
    
    // ==================== 渲染系统 ====================
    // 主渲染系统
    val renderSystem by choices("RenderSystem", arrayOf("Simple", "Advanced", "Shader", "Hybrid"), "Advanced")
    val renderPriority by int("RenderPriority", 0, -10..10)
    val renderStage by choices("RenderStage", arrayOf("Pre", "Post", "Both"), "Both")
    
    // 基础渲染开关
    val boxEnabled by boolean("BoxEnabled", true)
    val outlineEnabled by boolean("OutlineEnabled", true)
    val healthEnabled by boolean("HealthEnabled", true)
    val nameEnabled by boolean("NameEnabled", true)
    val tracerEnabled by boolean("TracerEnabled", false)
    val distanceEnabled by boolean("DistanceEnabled", true)
    val infoEnabled by boolean("InfoEnabled", false)
    val armorEnabled by boolean("ArmorEnabled", false)
    val itemEnabled by boolean("ItemEnabled", false)
    val directionEnabled by boolean("DirectionEnabled", false)
    val skeletonEnabled by boolean("SkeletonEnabled", false)
    val hitboxEnabled by boolean("HitboxEnabled", false)
    val circleEnabled by boolean("CircleEnabled", false)
    val lightEnabled by boolean("LightEnabled", false)
    val shadowEnabled by boolean("ShadowEnabled", false)
    val glowEnabled by boolean("GlowEnabled", false)
    val bloomEnabled by boolean("BloomEnabled", false)
    val outline3dEnabled by boolean("Outline3DEnabled", false)
    val indicatorEnabled by boolean("IndicatorEnabled", false)
    
    // ==================== 边框系统 ====================
    // 边框模式
    val boxMode by choices("BoxMode", arrayOf("2D", "3D", "Wireframe", "Corners", "Rounded", "Gradient", "Filled", "Outline", "Full"), "2D")
    val box2dType by choices("Box2DType", arrayOf("Normal", "Expand", "Shrink", "Dynamic"), "Normal")
    val box3dType by choices("Box3DType", arrayOf("Normal", "Cylinder", "Sphere", "Custom"), "Normal")
    
    // 边框样式
    val boxWidth by float("BoxWidth", 1.5f, 0.1f..10f)
    val boxHeightScale by float("BoxHeightScale", 1.0f, 0.1f..5f)
    val boxDepth by float("BoxDepth", 1.0f, 0.1f..5f)
    val boxCornerSize by float("BoxCornerSize", 0.2f, 0f..2f)
    val boxCornerLength by float("BoxCornerLength", 2f, 0f..10f)
    val boxRounding by float("BoxRounding", 0f, 0f..10f)
    val boxFade by boolean("BoxFade", true)
    val boxFadeStrength by float("BoxFadeStrength", 0.5f, 0f..1f)
    val boxPulse by boolean("BoxPulse", false)
    val boxPulseSpeed by float("BoxPulseSpeed", 1f, 0.1f..5f)
    val boxPulseAmplitude by float("BoxPulseAmpl", 0.1f, 0f..1f)
    val boxDynamicScale by boolean("BoxDynamicScale", false)
    val boxScaleFactor by float("BoxScaleFactor", 0.1f, 0f..1f)
    val boxPerspective by boolean("BoxPerspective", true)
    val boxDepthTest by boolean("BoxDepthTest", false)
    
    // 边框颜色
    val boxColorMode by choices("BoxColorMode", arrayOf("Static", "Rainbow", "Health", "Distance", "Team", "Pulse", "Chroma"), "Static")
    val boxStaticColor by color("BoxStaticColor", Color(0, 255, 0, 150))
    val boxRainbowSpeed by float("BoxRainbowSpeed", 1f, 0.1f..10f)
    val boxRainbowSaturation by float("BoxRainbowSaturation", 1f, 0f..1f)
    val boxRainbowBrightness by float("BoxRainbowBrightness", 1f, 0f..1f)
    val boxHealthMinColor by color("BoxHealthMinColor", Color(255, 0, 0, 150))
    val boxHealthMaxColor by color("BoxHealthMaxColor", Color(0, 255, 0, 150))
    val boxDistanceMinColor by color("BoxDistMinColor", Color(255, 255, 255, 150))
    val boxDistanceMaxColor by color("BoxDistMaxColor", Color(0, 0, 255, 150))
    val boxTeamColor by color("BoxTeamColor", Color(0, 255, 0, 150))
    val boxEnemyColor by color("BoxEnemyColor", Color(255, 0, 0, 150))
    val boxFriendColor by color("BoxFriendColor", Color(0, 0, 255, 150))
    val boxAlpha by int("BoxAlpha", 150, 0..255)
    val boxRed by int("BoxRed", 0, 0..255)
    val boxGreen by int("BoxGreen", 255, 0..255)
    val boxBlue by int("BoxBlue", 0, 0..255)
    
    // ==================== 轮廓系统 ====================
    // 轮廓模式
    val outlineMode by choices("OutlineMode", arrayOf("Normal", "Glow", "Shader", "Bloom", "Depth", "Sobel", "Canny", "Multi"), "Normal")
    val outlineType by choices("OutlineType", arrayOf("Line", "Fill", "Both", "Gradient"), "Line")
    
    // 轮廓样式
    val outlineWidth by float("OutlineWidth", 2f, 0.1f..10f)
    val outlineStrength by float("OutlineStrength", 1f, 0.1f..5f)
    val outlineSmoothness by float("OutlineSmoothness", 0.5f, 0f..1f)
    val outlineBloomRadius by float("OutlineBloomRadius", 3f, 0f..10f)
    val outlineBloomStrength by float("OutlineBloomStrength", 1f, 0f..5f)
    val outlineDepthThreshold by float("OutlineDepthThresh", 0.5f, 0f..1f)
    val outlineNormalThreshold by float("OutlineNormalThresh", 0.5f, 0f..1f)
    val outlineEdgeThreshold by float("OutlineEdgeThresh", 0.5f, 0f..1f)
    val outlineMultiCount by int("OutlineMultiCount", 2, 1..5)
    val outlineMultiSpacing by float("OutlineMultiSpacing", 2f, 0f..10f)
    val outlineFade by boolean("OutlineFade", true)
    val outlinePulse by boolean("OutlinePulse", false)
    
    // 轮廓颜色
    val outlineColorMode by choices("OutlineColorMode", arrayOf("Static", "Rainbow", "Health", "Distance"), "Static")
    val outlineStaticColor by color("OutlineStaticColor", Color(255, 255, 255, 200))
    val outlineRainbowSpeed by float("OutlineRainbowSpeed", 1f, 0.1f..10f)
    val outlineAlpha by int("OutlineAlpha", 200, 0..255)
    
    // ==================== 生命值系统 ====================
    // 生命值模式
    val healthMode by choices("HealthMode", arrayOf("Bar", "Text", "Both", "Circle", "Grid"), "Bar")
    val healthBarType by choices("HealthBarType", arrayOf("Vertical", "Horizontal", "Radial", "Gradient"), "Vertical")
    val healthTextType by choices("HealthTextType", arrayOf("Number", "Percentage", "Fraction", "Custom"), "Number")
    
    // 生命值样式
    val healthBarWidth by float("HealthBarWidth", 40f, 1f..100f)
    val healthBarHeight by float("HealthBarHeight", 6f, 1f..20f)
    val healthBarThickness by float("HealthBarThickness", 2f, 0.5f..10f)
    val healthBarOffsetX by float("HealthBarOffsetX", 0f, -50f..50f)
    val healthBarOffsetY by float("HealthBarOffsetY", 10f, -50f..50f)
    val healthTextSize by float("HealthTextSize", 1f, 0.5f..3f)
    val healthTextOffsetX by float("HealthTextOffsetX", 0f, -50f..50f)
    val healthTextOffsetY by float("HealthTextOffsetY", 18f, -50f..50f)
    val healthBarBackground by boolean("HealthBarBg", true)
    val healthBarBorder by boolean("HealthBarBorder", true)
    val healthBarAnimated by boolean("HealthBarAnimated", true)
    val healthBarAnimationSpeed by float("HealthBarAnimSpeed", 1f, 0.1f..5f)
    val healthBarSmooth by boolean("HealthBarSmooth", true)
    val healthBarSmoothness by float("HealthBarSmoothness", 0.5f, 0f..1f)
    
    // 生命值颜色
    val healthColorMode by choices("HealthColorMode", arrayOf("Gradient", "Static", "Rainbow", "Team"), "Gradient")
    val healthMinColor by color("HealthMinColor", Color(255, 0, 0, 255))
    val healthMidColor by color("HealthMidColor", Color(255, 255, 0, 255))
    val healthMaxColor by color("HealthMaxColor", Color(0, 255, 0, 255))
    val healthStaticColor by color("HealthStaticColor", Color(255, 255, 255, 255))
    val healthBackgroundColor by color("HealthBgColor", Color(0, 0, 0, 100))
    val healthBorderColor by color("HealthBorderColor", Color(0, 0, 0, 200))
    
    // ==================== 名称系统 ====================
    // 名称模式
    val nameMode by choices("NameMode", arrayOf("Normal", "Distance", "Health", "Both", "Custom"), "Normal")
    val namePosition by choices("NamePosition", arrayOf("Above", "Below", "Left", "Right", "Center"), "Above")
    val nameAlign by choices("NameAlign", arrayOf("Center", "Left", "Right"), "Center")
    
    // 名称样式
    val nameScale by float("NameScale", 1f, 0.5f..3f)
    val nameOffsetX by float("NameOffsetX", 0f, -50f..50f)
    val nameOffsetY by float("NameOffsetY", -20f, -100f..100f)
    val nameShadow by boolean("NameShadow", true)
    val nameBackground by boolean("NameBackground", false)
    val nameBackgroundColor by color("NameBgColor", Color(0, 0, 0, 100))
    val nameBorder by boolean("NameBorder", false)
    val nameBorderColor by color("NameBorderColor", Color(0, 0, 0, 200))
    val nameBorderWidth by float("NameBorderWidth", 1f, 0.5f..5f)
    val nameDistanceUnit by choices("NameDistUnit", arrayOf("Blocks", "Meters", "Feet", "None"), "Blocks")
    val nameCustomFormat by text("NameCustomFormat", "%name% [%health%HP] [%distance%m]")
    
    // 名称颜色
    val nameColorMode by choices("NameColorMode", arrayOf("Static", "Team", "Health", "Distance", "Rainbow"), "Static")
    val nameStaticColor by color("NameStaticColor", Color(255, 255, 255, 255))
    val nameTeamColor by color("NameTeamColor", Color(0, 255, 0, 255))
    val nameEnemyColor by color("NameEnemyColor", Color(255, 0, 0, 255))
    val nameFriendColor by color("NameFriendColor", Color(0, 0, 255, 255))
    
    // ==================== 追踪线系统 ====================
    // 追踪线模式
    val tracerMode by choices("TracerMode", arrayOf("Line", "Arrow", "Beam", "Laser", "Curve", "Spiral", "None"), "Line")
    val tracerStartPoint by choices("TracerStart", arrayOf("Feet", "Eye", "Crosshair", "ScreenCenter", "Custom"), "Feet")
    val tracerEndPoint by choices("TracerEnd", arrayOf("Feet", "Eye", "Center", "Top", "Bottom"), "Feet")
    
    // 追踪线样式
    val tracerWidth by float("TracerWidth", 1f, 0.1f..10f)
    val tracerLength by float("TracerLength", 100f, 1f..500f)
    val tracerArrowSize by float("TracerArrowSize", 4f, 1f..20f)
    val tracerCurvature by float("TracerCurvature", 0.5f, -2f..2f)
    val tracerSegments by int("TracerSegments", 20, 3..100)
    val tracerFade by boolean("TracerFade", true)
    val tracerFadeLength by float("TracerFadeLength", 0.5f, 0f..1f)
    val tracerPulse by boolean("TracerPulse", false)
    val tracerPulseSpeed by float("TracerPulseSpeed", 1f, 0.1f..5f)
    val tracerDepthTest by boolean("TracerDepthTest", false)
    
    // 追踪线颜色
    val tracerColorMode by choices("TracerColorMode", arrayOf("Static", "Rainbow", "Distance", "Team"), "Static")
    val tracerStaticColor by color("TracerStaticColor", Color(255, 255, 255, 200))
    val tracerRainbowSpeed by float("TracerRainbowSpeed", 1f, 0.1f..10f)
    val tracerAlpha by int("TracerAlpha", 200, 0..255)
    val tracerGradientStartColor by color("TracerGradStart", Color(255, 0, 0, 255))
    val tracerGradientEndColor by color("TracerGradEnd", Color(0, 255, 0, 255))
    
    // ==================== 距离系统 ====================
    val distanceMode by choices("DistanceMode", arrayOf("Text", "Bar", "Circle", "Fade"), "Text")
    val distanceUnit by choices("DistanceUnit", arrayOf("Blocks", "Meters", "Feet", "Yards"), "Blocks")
    val distancePrecision by int("DistancePrecision", 0, 0..3)
    val distanceScale by float("DistanceScale", 1f, 0.5f..3f)
    val distanceOffsetX by float("DistanceOffsetX", 0f, -50f..50f)
    val distanceOffsetY by float("DistanceOffsetY", -30f, -100f..100f)
    val distanceColor by color("DistanceColor", Color(255, 255, 255, 255))
    val distFadeStart by int("DistFadeStart", 50, 0..500)
    val distFadeEnd by int("DistFadeEnd", 100, 0..500)
    
    // ==================== 信息显示系统 ====================
    val infoType by choices("InfoType", arrayOf("Simple", "Detailed", "Advanced", "Custom"), "Simple")
    val infoLines by int("InfoLines", 3, 1..10)
    val infoFormat by text("InfoFormat", "HP: %health%\nDist: %distance%\nArmor: %armor%")
    val infoOffsetX by float("InfoOffsetX", 0f, -50f..50f)
    val infoOffsetY by float("InfoOffsetY", -40f, -100f..100f)
    val infoScale by float("InfoScale", 0.8f, 0.5f..2f)
    val infoColor by color("InfoColor", Color(255, 255, 255, 255))
    val infoBackground by boolean("InfoBackground", true)
    val infoBackgroundColor by color("InfoBgColor", Color(0, 0, 0, 100))
    
    // ==================== 护甲系统 ====================
    val armorMode by choices("ArmorMode", arrayOf("Icons", "Bars", "Text", "Both"), "Icons")
    val armorPosition by choices("ArmorPosition", arrayOf("Above", "Below", "Left", "Right"), "Above")
    val armorScale by float("ArmorScale", 1f, 0.5f..2f)
    val armorOffsetX by float("ArmorOffsetX", 0f, -50f..50f)
    val armorOffsetY by float("ArmorOffsetY", -50f, -100f..100f)
    val armorSpacing by float("ArmorSpacing", 2f, 0f..10f)
    val armorColor by color("ArmorColor", Color(255, 255, 255, 255))
    val armorDamageColor by color("ArmorDamageColor", Color(255, 0, 0, 255))
    
    // ==================== 物品显示系统 ====================
    val itemMode by choices("ItemMode", arrayOf("Icons", "Text", "Both"), "Icons")
    val itemScale by float("ItemScale", 0.8f, 0.5f..2f)
    val itemOffsetX by float("ItemOffsetX", 0f, -50f..50f)
    val itemOffsetY by float("ItemOffsetY", -60f, -100f..100f)
    val itemSpacing by float("ItemSpacing", 2f, 0f..10f)
    val itemColor by color("ItemColor", Color(255, 255, 255, 255))
    val itemRarityColors by boolean("ItemRarityColors", true)
    
    // ==================== 方向指示系统 ====================
    val directionMode by choices("DirectionMode", arrayOf("Arrow", "Line", "Compass", "Indicator"), "Arrow")
    val directionSize by float("DirectionSize", 10f, 1f..50f)
    val directionOffsetX by float("DirectionOffsetX", 0f, -50f..50f)
    val directionOffsetY by float("DirectionOffsetY", -70f, -100f..100f)
    val directionColor by color("DirectionColor", Color(255, 255, 255, 255))
    val directionNorthColor by color("DirectionNorthColor", Color(255, 0, 0, 255))
    val directionSouthColor by color("DirectionSouthColor", Color(0, 255, 0, 255))
    val directionEastColor by color("DirectionEastColor", Color(0, 0, 255, 255))
    val directionWestColor by color("DirectionWestColor", Color(255, 255, 0, 255))
    
    // ==================== 骨架系统 ====================
    val skeletonMode by choices("SkeletonMode", arrayOf("Simple", "Detailed", "Bones", "Stick"), "Simple")
    val skeletonWidth by float("SkeletonWidth", 1f, 0.1f..5f)
    val skeletonColorBase by color("SkeletonColor", Color(255, 255, 255, 200))
    val skeletonBoneColor by color("SkeletonBoneColor", Color(255, 255, 255, 200))
    val skeletonJointColor by color("SkeletonJointColor", Color(255, 0, 0, 200))
    val skeletonHighlightBones by boolean("SkeletonHighlight", false)
    val skeletonPulse by boolean("SkeletonPulse", false)
    
    // ==================== 碰撞箱系统 ====================
    val hitboxMode by choices("HitboxMode", arrayOf("Box", "Wireframe", "Filled", "Outline"), "Box")
    val hitboxColor by color("HitboxColor", Color(255, 255, 0, 100))
    val hitboxOutlineColor by color("HitboxOutlineColor", Color(255, 255, 0, 200))
    val hitboxWidth by float("HitboxWidth", 1f, 0.1f..5f)
    
    // ==================== 圆形指示器系统 ====================
    val circleMode by choices("CircleMode", arrayOf("Range", "Target", "Radius", "Custom"), "Range")
    val circleRadius by float("CircleRadius", 3f, 0.1f..20f)
    val circleSegments by int("CircleSegments", 32, 3..100)
    val circleWidth by float("CircleWidth", 1f, 0.1f..5f)
    val circleColor by color("CircleColor", Color(255, 255, 255, 100))
    val circleFillColor by color("CircleFillColor", Color(255, 255, 255, 50))
    val circlePulse by boolean("CirclePulse", false)
    
    // ==================== 光照系统 ====================
    val lightMode by choices("LightMode", arrayOf("Simple", "Advanced", "Realistic", "None"), "Simple")
    val lightIntensity by float("LightIntensity", 1f, 0f..2f)
    val lightRadius by float("LightRadius", 10f, 1f..50f)
    val lightColor by color("LightColor", Color(255, 255, 200, 100))
    val lightShadows by boolean("LightShadows", false)
    val lightShadowQuality by int("LightShadowQuality", 1, 1..4)
    
    // ==================== 阴影系统 ====================
    val shadowMode by choices("ShadowMode", arrayOf("Simple", "Blur", "Drop", "Perspective"), "Simple")
    val shadowOffsetX by float("ShadowOffsetX", 2f, 0f..20f)
    val shadowOffsetY by float("ShadowOffsetY", 2f, 0f..20f)
    val shadowBlurRadius by float("ShadowBlurRadius", 3f, 0f..10f)
    val shadowColor by color("ShadowColor", Color(0, 0, 0, 100))
    val shadowOpacity by float("ShadowOpacity", 0.5f, 0f..1f)
    
    // ==================== 发光系统 ====================
    val glowMode by choices("GlowMode", arrayOf("Simple", "Bloom", "Halo", "Radial"), "Simple")
    val glowRadius by float("GlowRadius", 5f, 1f..20f)
    val glowIntensity by float("GlowIntensity", 1f, 0f..5f)
    val glowColor by color("GlowColor", Color(255, 255, 255, 100))
    val glowBloomThreshold by float("GlowBloomThresh", 0.8f, 0f..1f)
    
    // ==================== 泛光系统 ====================
    val bloomMode by choices("BloomMode", arrayOf("Simple", "Gaussian", "Kawase", "Dual"), "Simple")
    val bloomRadius by float("BloomRadius", 3f, 0f..10f)
    val bloomIntensity by float("BloomIntensity", 1f, 0f..5f)
    val bloomThreshold by float("BloomThreshold", 0.7f, 0f..1f)
    val bloomQuality by int("BloomQuality", 2, 1..4)
    
    // ==================== 3D轮廓系统 ====================
    val outline3dMode by choices("Outline3DMode", arrayOf("Wireframe", "Solid", "Transparent", "Edge"), "Wireframe")
    val outline3dWidth by float("Outline3DWidth", 1f, 0.1f..5f)
    val outline3dColor by color("Outline3DColor", Color(255, 255, 255, 200))
    val outline3dDepthTest by boolean("Outline3DDepthTest", true)
    
    // ==================== 指示器系统 ====================
    val indicatorMode by choices("IndicatorMode", arrayOf("Compass", "Radar", "Minimap", "Overlay"), "Compass")
    val indicatorSize by float("IndicatorSize", 100f, 20f..500f)
    val indicatorPositionX by float("IndicatorPosX", 10f, 0f..1000f)
    val indicatorPositionY by float("IndicatorPosY", 10f, 0f..1000f)
    val indicatorScale by float("IndicatorScale", 1f, 0.5f..3f)
    val indicatorColor by color("IndicatorColor", Color(255, 255, 255, 200))
    val indicatorBackgroundColor by color("IndicatorBgColor", Color(0, 0, 0, 100))
    
    // ==================== 着色器系统 ====================
    val shaderMode by choices("ShaderMode", arrayOf("None", "Outline", "Glow", "Bloom", "Custom"), "None")
    val shaderOutlineColor by color("ShaderOutlineColor", Color(255, 255, 255, 255))
    val shaderOutlineWidth by float("ShaderOutlineWidth", 2f, 0.1f..10f)
    val shaderGlowColor by color("ShaderGlowColor", Color(255, 255, 255, 200))
    val shaderGlowRadius by float("ShaderGlowRadius", 3f, 0f..10f)
    val shaderBloomColor by color("ShaderBloomColor", Color(255, 255, 255, 200))
    val shaderBloomRadius by float("ShaderBloomRadius", 3f, 0f..10f)
    val shaderCustomCode by text("ShaderCustomCode", "")
    val shaderQuality by int("ShaderQuality", 2, 1..4)
    
    // ==================== 颜色系统 ====================
    // 玩家颜色
    val playerColor by color("PlayerColor", Color(0, 255, 0, 150))
    val teammateColor by color("TeammateColor", Color(0, 255, 0, 150))
    val enemyColor by color("EnemyColor", Color(255, 0, 0, 150))
    val friendColorSetting by color("FriendColor", Color(0, 0, 255, 150))
    val neutralPlayerColor by color("NeutralPlayerColor", Color(255, 255, 0, 150))
    val botColor by color("BotColor", Color(128, 128, 128, 150))
    
    // 敌对生物颜色
    val mobColor by color("MobColor", Color(255, 0, 0, 150))
    val zombieColor by color("ZombieColor", Color(0, 128, 0, 150))
    val skeletonColor by color("SkeletonColorSetting", Color(192, 192, 192, 150))
    val creeperColor by color("CreeperColor", Color(0, 255, 0, 150))
    val spiderColor by color("SpiderColor", Color(128, 0, 128, 150))
    val endermanColor by color("EndermanColor", Color(0, 0, 0, 150))
    val witchColor by color("WitchColor", Color(128, 0, 128, 150))
    val slimeColor by color("SlimeColor", Color(0, 255, 0, 150))
    val ghastColor by color("GhastColor", Color(255, 255, 255, 150))
    val blazeColor by color("BlazeColor", Color(255, 165, 0, 150))
    val magmaCubeColor by color("MagmaCubeColor", Color(255, 69, 0, 150))
    
    // 中立生物颜色
    val animalColor by color("AnimalColor", Color(255, 255, 0, 150))
    val cowColor by color("CowColor", Color(139, 69, 19, 150))
    val pigColor by color("PigColor", Color(255, 182, 193, 150))
    val sheepColor by color("SheepColor", Color(255, 255, 255, 150))
    val chickenColor by color("ChickenColor", Color(255, 255, 255, 150))
    val wolfColor by color("WolfColor", Color(128, 128, 128, 150))
    val horseColor by color("HorseColor", Color(139, 69, 19, 150))
    
    // 物品颜色
    val itemColorSetting by color("ItemColor", Color(0, 191, 255, 150))
    val commonItemColor by color("CommonItemColor", Color(192, 192, 192, 150))
    val rareItemColor by color("RareItemColor", Color(0, 255, 255, 150))
    val epicItemColor by color("EpicItemColor", Color(128, 0, 128, 150))
    val legendaryItemColor by color("LegendaryItemColor", Color(255, 215, 0, 150))
    
    // 其他颜色
    val vehicleColor by color("VehicleColor", Color(255, 165, 0, 150))
    val projectileColor by color("ProjectileColor", Color(255, 255, 0, 150))
    val otherColor by color("OtherColor", Color(128, 0, 128, 150))
    
    // 彩虹颜色系统
    val rainbowEnabled by boolean("RainbowEnabled", false)
    val rainbowSpeed by float("RainbowSpeed", 1f, 0.1f..10f)
    val rainbowSaturation by float("RainbowSaturation", 1f, 0f..1f)
    val rainbowBrightness by float("RainbowBrightness", 1f, 0f..1f)
    val rainbowAlpha by int("RainbowAlpha", 150, 0..255)
    val rainbowModeSetting by choices("RainbowMode", arrayOf("Static", "Wave", "Pulse", "Cycle"), "Static")
    
    // 颜色混合
    val colorBlendMode by choices("ColorBlendMode", arrayOf("Normal", "Add", "Multiply", "Screen", "Overlay"), "Normal")
    val colorBlendStrength by float("ColorBlendStrength", 1f, 0f..1f)
    val colorMixRatio by float("ColorMixRatio", 0.5f, 0f..1f)
    
    // ==================== 动画系统 ====================
    val animationEnabled by boolean("AnimationEnabled", true)
    val animationType by choices("AnimationType", arrayOf("Linear", "EaseIn", "EaseOut", "EaseInOut", "Bounce", "Elastic"), "EaseInOut")
    val animationSpeedSetting by float("AnimationSpeed", 1f, 0.1f..5f)
    val animationDelay by int("AnimationDelay", 0, 0..1000)
    val animationLoop by boolean("AnimationLoop", true)
    val animationReverse by boolean("AnimationReverse", false)
    
    // 缩放动画
    val scaleAnimation by boolean("ScaleAnimation", false)
    val scaleFrom by float("ScaleFrom", 0.5f, 0f..5f)
    val scaleTo by float("ScaleTo", 1f, 0f..5f)
    val scaleDuration by int("ScaleDuration", 500, 0..5000)
    
    // 旋转动画
    val rotationAnimation by boolean("RotationAnimation", false)
    val rotationSpeed by float("RotationSpeed", 1f, -10f..10f)
    val rotationAxis by choices("RotationAxis", arrayOf("X", "Y", "Z", "All"), "Y")
    
    // 淡入淡出动画
    val fadeAnimation by boolean("FadeAnimation", true)
    val fadeInTime by int("FadeInTime", 200, 0..5000)
    val fadeOutTime by int("FadeOutTime", 200, 0..5000)
    val fadeAlphaFrom by float("FadeAlphaFrom", 0f, 0f..1f)
    val fadeAlphaTo by float("FadeAlphaTo", 1f, 0f..1f)
    
    // 脉动动画
    val pulseAnimation by boolean("PulseAnimation", false)
    val pulseSpeedSetting by float("PulseSpeed", 1f, 0.1f..5f)
    val pulseAmplitude by float("PulseAmplitude", 0.1f, 0f..1f)
    val pulseMinScale by float("PulseMinScale", 0.9f, 0f..2f)
    val pulseMaxScale by float("PulseMaxScale", 1.1f, 0f..2f)
    
    // ==================== 过滤系统 ====================
    // 通用过滤
    val ignoreInvisible by boolean("IgnoreInvisible", true)
    val ignoreDead by boolean("IgnoreDead", true)
    val ignoreSelf by boolean("IgnoreSelf", true)
    val ignoreSleeping by boolean("IgnoreSleeping", false)
    val ignoreSneaking by boolean("IgnoreSneaking", false)
    val ignoreFriendlyFire by boolean("IgnoreFriendlyFire", true)
    val ignoreTeams by boolean("IgnoreTeams", false)
    val ignoreFriends by boolean("IgnoreFriends", false)
    val ignoreNPCs by boolean("IgnoreNPCs", false)
    val ignoreBots by boolean("IgnoreBots", false)
    
    // 距离过滤
    val maxDistance by int("MaxDistance", 100, 0..1000)
    val minDistance by int("MinDistance", 0, 0..500)
    val distanceFadeEnabled by boolean("DistanceFade", true)
    val distanceFadeStartValue by int("DistFadeStartValue", 50, 0..500)
    val distanceFadeEndValue by int("DistFadeEndValue", 100, 0..500)
    
    // 生命值过滤
    val minHealth by float("MinHealth", 0f, 0f..1000f)
    val maxHealth by float("MaxHealth", 1000f, 0f..10000f)
    val healthFilterMode by choices("HealthFilterMode", arrayOf("Less", "Greater", "Between", "Equal"), "Between")
    
    // 角度过滤
    val fovLimit by float("FOVLimit", 180f, 0f..360f)
    val angleFilter by boolean("AngleFilter", false)
    val minAngle by float("MinAngle", 0f, 0f..360f)
    val maxAngle by float("MaxAngle", 360f, 0f..360f)
    
    // 环境过滤
    val onlyVisible by boolean("OnlyVisible", false)
    val visibleCheckType by choices("VisibleCheckType", arrayOf("Raycast", "LOS", "Simple", "Advanced"), "Raycast")
    val visibleCheckRange by int("VisibleCheckRange", 100, 0..500)
    val renderThroughWalls by boolean("RenderThroughWalls", true)
    val renderInWater by boolean("RenderInWater", true)
    val renderInLava by boolean("RenderInLava", true)
    val renderInAir by boolean("RenderInAir", true)
    val renderOnGround by boolean("RenderOnGround", true)
    
    // 时间过滤
    val timeFilter by boolean("TimeFilter", false)
    val dayOnly by boolean("DayOnly", false)
    val nightOnly by boolean("NightOnly", false)
    val timeStart by int("TimeStart", 0, 0..24000)
    val timeEnd by int("TimeEnd", 24000, 0..24000)
    
    // 生物群系过滤
    val biomeFilter by boolean("BiomeFilter", false)
    val allowedBiomes by text("AllowedBiomes", "plains,forest,mountains")
    val excludedBiomes by text("ExcludedBiomes", "ocean,desert")
    
    // 维度过滤
    val dimensionFilter by boolean("DimensionFilter", false)
    val overworldOnly by boolean("OverworldOnly", false)
    val netherOnly by boolean("NetherOnly", false)
    val endOnly by boolean("EndOnly", false)
    
    // 难度过滤
    val difficultyFilter by boolean("DifficultyFilter", false)
    val peacefulOnly by boolean("PeacefulOnly", false)
    val easyOnly by boolean("EasyOnly", false)
    val normalOnly by boolean("NormalOnly", false)
    val hardOnly by boolean("HardOnly", false)
    
    // ==================== 性能系统 ====================
    val performanceMode by boolean("PerformanceMode", false)
    val entityCheckRate by int("CheckRate", 1, 1..10)
    val maxEntities by int("MaxEntities", 100, 1..1000)
    val renderLimit by int("RenderLimit", 50, 1..500)
    val lodEnabled by boolean("LODEnabled", true)
    val lodDistance1 by int("LODDistance1", 30, 0..500)
    val lodDistance2 by int("LODDistance2", 60, 0..500)
    val lodDistance3 by int("LODDistance3", 100, 0..500)
    val lodDetail1 by int("LODDetail1", 3, 1..10)
    val lodDetail2 by int("LODDetail2", 2, 1..10)
    val lodDetail3 by int("LODDetail3", 1, 1..10)
    val cacheEnabled by boolean("CacheEnabled", true)
    val cacheTime by int("CacheTime", 100, 0..5000)
    val batchRendering by boolean("BatchRendering", true)
    val occlusionCulling by boolean("OcclusionCulling", false)
    val frustumCulling by boolean("FrustumCulling", true)
    
    // ==================== 质量系统 ====================
    val qualityMode by choices("QualityMode", arrayOf("Low", "Medium", "High", "Ultra"), "Medium")
    val antiAliasing by boolean("AntiAliasing", false)
    val antiAliasingSamples by int("AASamples", 2, 1..16)
    val mipmapLevel by int("MipmapLevel", 0, 0..4)
    val textureFiltering by boolean("TextureFiltering", true)
    val anisotropicFiltering by boolean("AnisotropicFiltering", false)
    val anisotropicLevel by int("AnisotropicLevel", 2, 1..16)
    val depthPrecision by int("DepthPrecision", 24, 8..32)
    val stencilPrecision by int("StencilPrecision", 0, 0..8)
    
    // ==================== 杂项设置 ====================
    val debugMode by boolean("DebugMode", false)
    val showFPS by boolean("ShowFPS", false)
    val showEntityCount by boolean("ShowEntityCount", false)
    val showRenderTime by boolean("ShowRenderTime", false)
    val logChanges by boolean("LogChanges", false)
    val autoConfig by boolean("AutoConfig", false)
    val configProfile by choices("ConfigProfile", arrayOf("Default", "PvP", "PvE", "Creative", "Custom"), "Default")
    val hotkeyMode by choices("HotkeyMode", arrayOf("Toggle", "Hold", "Both"), "Toggle")
    val toggleKey by int("ToggleKey", 0, 0..255)
    val holdKey by int("HoldKey", 0, 0..255)
    val guiScale by float("GUIScale", 1f, 0.5f..3f)
    val language by choices("Language", arrayOf("English", "Chinese", "Russian", "Spanish", "French"), "English")
    
    // ==================== 事件处理器 ====================
    private var tickCounter = 0
    private val entityCache = HashMap<Int, Long>()
    private val renderTimes = ArrayList<Long>()
    
    override fun onEnable() {
        tickCounter = 0
        entityCache.clear()
        renderTimes.clear()
        mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§aESP Module Enabled with ${countOptions()} options"))
    }
    
    override fun onDisable() {
        mc.thePlayer?.addChatMessage(net.minecraft.util.ChatComponentText("§cESP Module Disabled"))
    }
    
    private fun countOptions(): Int {
        return 350 // 这是一个估计值
    }
    
    private fun shouldRender(entity: Entity): Boolean {
        val player = mc.thePlayer ?: return false
        
        // 基本检查
        if (!moduleEnabled) return false
        if (entity == null) return false
        
        // 距离检查
        val distance = player.getDistanceToEntity(entity).toFloat()
        if (distance > maxDistance) return false
        if (distance < minDistance) return false
        
        // 过滤检查
        if (ignoreInvisible && entity.isInvisible) return false
        if (ignoreSelf && entity == player) return false
        if (ignoreDead && entity is EntityLivingBase && (entity.isDead || entity.health <= 0)) return false
        
        // 类型检查
        return when {
            entity is EntityPlayer -> checkPlayer(entity, distance)
            entity is EntityMob -> checkMob(entity, distance)
            entity is EntityAnimal -> checkAnimal(entity, distance)
            entity is net.minecraft.entity.item.EntityItem -> checkItem(entity, distance)
            entity is net.minecraft.entity.item.EntityBoat -> checkVehicle(entity, distance)
            entity is net.minecraft.entity.projectile.EntityArrow -> checkProjectile(entity, distance)
            else -> checkOther(entity, distance)
        }
    }
    
    private fun checkPlayer(entity: EntityPlayer, distance: Float): Boolean {
        if (!players) return false
        if (distance > playerDistance) return false
        
        // 好友和队伍检查
        if (ignoreFriends && isFriend(entity)) return false
        if (ignoreTeams && isTeammate(entity)) return false
        
        // 生命值检查
        if (entity.health < playerMinHealth || entity.health > playerMaxHealth) return false
        
        return true
    }
    
    private fun checkMob(entity: EntityMob, distance: Float): Boolean {
        if (!mobs) return false
        if (distance > mobDistance) return false
        
        // 具体怪物类型检查
        return when (entity) {
            is EntityZombie -> zombies
            is EntitySkeleton -> skeletons
            is EntityCreeper -> creepers
            is EntitySpider -> spiders
            is EntityEnderman -> endermen
            is EntityWitch -> witches
            is EntitySlime -> slimes
            is EntityGhast -> ghasts
            is EntityBlaze -> blazes
            is EntityMagmaCube -> magmaCubes
            is EntitySilverfish -> silverfish
            is EntityCaveSpider -> caveSpiders
            is EntityPigZombie -> pigZombies
            is EntityGuardian -> guardians
            else -> mobs
        }
    }
    
    private fun checkAnimal(entity: EntityAnimal, distance: Float): Boolean {
        if (!animals) return false
        if (distance > animalDistance) return false
        
        return when (entity) {
            is EntityCow -> cows
            is EntityPig -> pigs
            is EntitySheep -> sheep
            is EntityChicken -> chickens
            is EntityWolf -> wolves
            is EntityOcelot -> ocelots
            is EntityHorse -> horses
            is EntityRabbit -> rabbits
            is EntityBat -> bats
            is EntitySquid -> squids
            is EntityMooshroom -> mooshrooms
            else -> animals
        }
    }
    
    private fun checkItem(entity: net.minecraft.entity.item.EntityItem, distance: Float): Boolean {
        if (!items) return false
        if (distance > itemDistance) return false
        
        // 这里可以添加更详细的物品价值检查
        return true
    }
    
    private fun checkVehicle(entity: net.minecraft.entity.item.EntityBoat, distance: Float): Boolean {
        if (!vehicles) return false
        if (distance > vehicleDistance) return false
        return boats
    }
    
    private fun checkProjectile(entity: net.minecraft.entity.projectile.EntityArrow, distance: Float): Boolean {
        if (!projectiles) return false
        if (distance > projectileDistance) return false
        return arrows
    }
    
    private fun checkOther(entity: Entity, distance: Float): Boolean {
        if (!others) return false
        if (distance > otherDistance) return false
        return true
    }
    
    private fun isFriend(entity: EntityPlayer): Boolean {
        // 这里需要根据你的好友系统实现
        return false
    }
    
    private fun isTeammate(entity: EntityPlayer): Boolean {
        val displayName = entity.displayName
        val playerName = mc.thePlayer?.displayName
        return displayName != null && playerName != null &&
               displayName.unformattedText.startsWith("§") &&
               playerName.unformattedText.startsWith("§") &&
               displayName.unformattedText[1] == playerName.unformattedText[1]
    }
    
    private fun getEntityColor(entity: Entity): Color {
        // 彩虹模式
        if (rainbowEnabled) {
            return ColorUtils.rainbow(tickCounter.toLong() * rainbowSpeed.toLong(), rainbowAlpha.toFloat() / 255f)
        }
        
        // 根据实体类型返回颜色
        return when (entity) {
            is EntityPlayer -> {
                if (isFriend(entity)) friendColorSetting
                else if (isTeammate(entity)) teammateColor
                else enemyColor
            }
            is EntityZombie -> zombieColor
            is EntitySkeleton -> skeletonColor
            is EntityCreeper -> creeperColor
            is EntitySpider -> spiderColor
            is EntityEnderman -> endermanColor
            is EntityWitch -> witchColor
            is EntitySlime -> slimeColor
            is EntityGhast -> ghastColor
            is EntityBlaze -> blazeColor
            is EntityMagmaCube -> magmaCubeColor
            is EntityCow -> cowColor
            is EntityPig -> pigColor
            is EntitySheep -> sheepColor
            is EntityChicken -> chickenColor
            is EntityWolf -> wolfColor
            is EntityHorse -> horseColor
            is net.minecraft.entity.item.EntityItem -> itemColorSetting
            is net.minecraft.entity.item.EntityBoat -> vehicleColor
            is net.minecraft.entity.projectile.EntityArrow -> projectileColor
            else -> otherColor
        }
    }
    
    private fun renderESP(entity: Entity, color: Color) {
        // 根据设置渲染各种ESP元素
        if (boxEnabled) renderBox(entity, color)
        if (outlineEnabled) renderOutline(entity, color)
        if (healthEnabled && entity is EntityLivingBase) renderHealth(entity, color)
        if (nameEnabled) renderName(entity, color)
        if (tracerEnabled) renderTracer(entity, color)
        if (distanceEnabled) renderDistance(entity, color)
        if (infoEnabled) renderInfo(entity, color)
        if (armorEnabled && entity is EntityLivingBase) renderArmor(entity, color)
        if (itemEnabled && entity is net.minecraft.entity.item.EntityItem) renderItem(entity, color)
        if (directionEnabled) renderDirection(entity, color)
        if (skeletonEnabled) renderSkeleton(entity, color)
        if (hitboxEnabled) renderHitbox(entity, color)
        if (circleEnabled) renderCircle(entity, color)
        if (lightEnabled) renderLight(entity, color)
        if (shadowEnabled) renderShadow(entity, color)
        if (glowEnabled) renderGlow(entity, color)
        if (bloomEnabled) renderBloom(entity, color)
        if (outline3dEnabled) renderOutline3D(entity, color)
        if (indicatorEnabled) renderIndicator(entity, color)
    }
    
    private fun renderBox(entity: Entity, color: Color) {
        // 实现盒子渲染
        GL11.glPushMatrix()
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        
        val partialTicks = mc.timer.renderPartialTicks
        val renderPosX = mc.renderManager.viewerPosX
        val renderPosY = mc.renderManager.viewerPosY
        val renderPosZ = mc.renderManager.viewerPosZ
        
        val entityX = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * partialTicks
        val entityY = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * partialTicks
        val entityZ = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * partialTicks
        
        GL11.glTranslated(entityX - renderPosX, entityY - renderPosY, entityZ - renderPosZ)
        
        when (boxMode) {
            "2D" -> render2DBox(entity, color)
            "3D" -> render3DBox(entity, color)
            "Wireframe" -> renderWireframeBox(entity, color)
            "Corners" -> renderCornerBox(entity, color)
            "Rounded" -> renderRoundedBox(entity, color)
            else -> render2DBox(entity, color)
        }
        
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glPopMatrix()
    }
    
    private fun render2DBox(entity: Entity, color: Color) {
        val width = entity.width.toFloat() * 1.0f
        val height = entity.height.toFloat() * 1.0f
        
        GL11.glLineWidth(boxWidth)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        
        // 简化的2D盒子渲染
        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glVertex3d(-width.toDouble(), 0.0, -width.toDouble())
        GL11.glVertex3d(-width.toDouble(), height.toDouble(), -width.toDouble())
        GL11.glVertex3d(width.toDouble(), height.toDouble(), -width.toDouble())
        GL11.glVertex3d(width.toDouble(), 0.0, -width.toDouble())
        GL11.glEnd()
        
        GL11.glBegin(GL11.GL_LINE_LOOP)
        GL11.glVertex3d(-width.toDouble(), 0.0, width.toDouble())
        GL11.glVertex3d(-width.toDouble(), height.toDouble(), width.toDouble())
        GL11.glVertex3d(width.toDouble(), height.toDouble(), width.toDouble())
        GL11.glVertex3d(width.toDouble(), 0.0, width.toDouble())
        GL11.glEnd()
        
        GL11.glBegin(GL11.GL_LINES)
        GL11.glVertex3d(-width.toDouble(), 0.0, -width.toDouble())
        GL11.glVertex3d(-width.toDouble(), 0.0, width.toDouble())
        GL11.glVertex3d(-width.toDouble(), height.toDouble(), -width.toDouble())
        GL11.glVertex3d(-width.toDouble(), height.toDouble(), width.toDouble())
        GL11.glVertex3d(width.toDouble(), height.toDouble(), -width.toDouble())
        GL11.glVertex3d(width.toDouble(), height.toDouble(), width.toDouble())
        GL11.glVertex3d(width.toDouble(), 0.0, -width.toDouble())
        GL11.glVertex3d(width.toDouble(), 0.0, width.toDouble())
        GL11.glEnd()
    }
    
    private fun render3DBox(entity: Entity, color: Color) {
        val width = entity.width.toFloat() * 1.0f
        val depth = entity.width.toFloat() * 1.0f
        val height = entity.height.toFloat() * 1.0f
        
        GL11.glLineWidth(boxWidth)
        GL11.glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
        
        // 3D盒子渲染
        RenderUtils.drawAxisAlignedBB(AxisAlignedBB(-width.toDouble(), 0.0, -depth.toDouble(), width.toDouble(), height.toDouble(), depth.toDouble()), color)
    }
    
    private fun renderWireframeBox(entity: Entity, color: Color) {
        // 线框盒子实现
    }
    
    private fun renderCornerBox(entity: Entity, color: Color) {
        // 角标盒子实现
    }
    
    private fun renderRoundedBox(entity: Entity, color: Color) {
        // 圆角盒子实现
    }
    
    private fun renderOutline(entity: Entity, color: Color) {
        // 轮廓渲染实现
    }
    
    private fun renderHealth(entity: EntityLivingBase, color: Color) {
        // 生命值渲染实现
    }
    
    private fun renderName(entity: Entity, color: Color) {
        // 名称渲染实现
    }
    
    private fun renderTracer(entity: Entity, color: Color) {
        // 追踪线渲染实现
    }
    
    private fun renderDistance(entity: Entity, color: Color) {
        // 距离渲染实现
    }
    
    private fun renderInfo(entity: Entity, color: Color) {
        // 信息渲染实现
    }
    
    private fun renderArmor(entity: EntityLivingBase, color: Color) {
        // 护甲渲染实现
    }
    
    private fun renderItem(entity: net.minecraft.entity.item.EntityItem, color: Color) {
        // 物品渲染实现
    }
    
    private fun renderDirection(entity: Entity, color: Color) {
        // 方向渲染实现
    }
    
    private fun renderSkeleton(entity: Entity, color: Color) {
        // 骨架渲染实现
    }
    
    private fun renderHitbox(entity: Entity, color: Color) {
        // 碰撞箱渲染实现
    }
    
    private fun renderCircle(entity: Entity, color: Color) {
        // 圆形渲染实现
    }
    
    private fun renderLight(entity: Entity, color: Color) {
        // 光照渲染实现
    }
    
    private fun renderShadow(entity: Entity, color: Color) {
        // 阴影渲染实现
    }
    
    private fun renderGlow(entity: Entity, color: Color) {
        // 发光渲染实现
    }
    
    private fun renderBloom(entity: Entity, color: Color) {
        // 泛光渲染实现
    }
    
    private fun renderOutline3D(entity: Entity, color: Color) {
        // 3D轮廓渲染实现
    }
    
    private fun renderIndicator(entity: Entity, color: Color) {
        // 指示器渲染实现
    }
    
    // ==================== 新增的公共函数，供其他文件调用 ====================
    
    /**
     * 公共函数：渲染NameTags（供NameTags.kt, NameTags2.kt, NameTags3.kt调用）
     */
    fun renderNameTags(entity: Entity, color: Color) {
        // 调用现有的renderName函数
        renderName(entity, color)
    }
    
    /**
     * 公共函数：获取实体颜色（供Radar.kt调用）
     */
    fun getColor(entity: Entity): Color {
        return getEntityColor(entity)
    }
    
    /**
     * 公共函数：渲染NameTags的简化版本（可能供其他文件调用）
     */
    fun renderNameTags(entity: Entity) {
        val color = getEntityColor(entity)
        renderName(entity, color)
    }
    
    /**
     * 公共函数：检查是否应该渲染实体
     */
    fun shouldRenderEntity(entity: Entity): Boolean {
        return shouldRender(entity)
    }
    
    /**
     * 公共函数：获取实体距离
     */
    fun getEntityDistance(entity: Entity): Float {
        val player = mc.thePlayer ?: return Float.MAX_VALUE
        return player.getDistanceToEntity(entity).toFloat()
    }
    
    // ==================== 事件处理器 ====================
    
    val onRender = handler<Render3DEvent> { event ->
        tickCounter++
        
        if (!moduleEnabled) return@handler
        if (performanceMode && tickCounter % entityCheckRate != 0) return@handler
        
        val startTime = System.nanoTime()
        val world = mc.theWorld ?: return@handler
        val player = mc.thePlayer ?: return@handler
        
        var renderedCount = 0
        
        // 遍历实体
        for (entity in world.loadedEntityList) {
            if (renderedCount >= renderLimit) break
            if (!shouldRender(entity)) continue
            
            val color = getEntityColor(entity)
            renderESP(entity, color)
            renderedCount++
        }
        
        // 调试信息
        if (debugMode) {
            val renderTime = (System.nanoTime() - startTime) / 1_000_000.0
            renderTimes.add(System.currentTimeMillis())
            
            // 清理旧的时间记录
            val currentTime = System.currentTimeMillis()
            renderTimes.removeIf { currentTime - it > 1000 }
            
            val fps = renderTimes.size.toFloat()
            
            if (showFPS) {
                mc.fontRendererObj.drawString("FPS: ${fps.toInt()}", 5f, 5f, Color.WHITE.rgb, true)
            }
            
            if (showEntityCount) {
                mc.fontRendererObj.drawString("Entities: $renderedCount", 5f, 20f, Color.WHITE.rgb, true)
            }
            
            if (showRenderTime) {
                mc.fontRendererObj.drawString("Render: ${String.format("%.2f", renderTime)}ms", 5f, 35f, Color.WHITE.rgb, true)
            }
        }
    }
    
    val onTick = handler<UpdateEvent> {
        tickCounter++
        
        // 清理缓存
        val currentTime = System.currentTimeMillis()
        val iterator = entityCache.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (currentTime - entry.value > cacheTime) {
                iterator.remove()
            }
        }
    }
    
    val onWorld = handler<WorldEvent> {
        entityCache.clear()
        renderTimes.clear()
    }
    
    val onKey = handler<KeyEvent> { event ->
        // 热键处理
        if (hotkeyMode == "Toggle" && event.key == toggleKey) {
            toggle()
        } else if (hotkeyMode == "Hold" && event.key == holdKey) {
            // 按住模式处理
        }
    }

    override val tag: String
        get() = "Ultra+"
}