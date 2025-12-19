package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.util.ResourceLocation
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs

object TargetHUD : Module("TargetHUD", Category.RENDER) {

    private var healthBarWidth = 0.0
    private var healthBarWidth2 = 0.0
    private var hudHeight = 0.0
    private var hudAlpha = 0.0
    private var prevTarget: EntityLivingBase? = null

    private val anim = AnimationUtils()

    // 动画速度设置
    private val animationSpeed by float("AnimationSpeed", 0.2f, 0.05f..1f)
    private val fadeAnimation by boolean("FadeAnimation", true)

    // 用于缓存玩家皮肤纹理
    private val skinCache = mutableMapOf<String, ResourceLocation>()

    val onRender2D = handler<Render2DEvent> { event ->
        // 添加空值检查
        if (mc.thePlayer == null || mc.theWorld == null) return@handler

        val target = KillAura.target ?: return@handler
        if (target !is EntityPlayer) return@handler

        val sr = ScaledResolution(mc)
        val scaledWidth = sr.scaledWidth.toFloat()
        val scaledHeight = sr.scaledHeight.toFloat()

        val x = scaledWidth / 2.0f - 50
        val y = scaledHeight / 2.0f + 32

        val health: Float
        var hpPercentage: Double
        val hurt: Color
        val healthColor: Int
        val healthStr: String

        health = target.health
        hpPercentage = (health / target.maxHealth).toDouble()
        hurt = Color.getHSBColor(310f / 360f, target.hurtTime / 10f, 1f)
        healthStr = (target.health.toInt().toFloat() / 2.0f).toString()
        healthColor = ColorUtils.interpolateHealthColor(target, 255, 0, 0, 255, true, true).rgb

        hpPercentage = hpPercentage.coerceIn(0.0, 1.0)
        val hpWidth = 140.0 * hpPercentage

        healthBarWidth2 = anim.animate(healthBarWidth2, hpWidth, animationSpeed.toDouble(), false)
        healthBarWidth = getAnimationStateSmooth(hpWidth, healthBarWidth, animationSpeed.toDouble() * 2)
        hudHeight = getAnimationStateSmooth(40.0, hudHeight, animationSpeed.toDouble())

        // 如果目标改变，重置透明度
        if (prevTarget != target) {
            hudAlpha = 0.0
            prevTarget = target
        }

        // 更新透明度动画
        if (fadeAnimation) {
            hudAlpha = getAnimationStateSmooth(1.0, hudAlpha, animationSpeed.toDouble())
        } else {
            hudAlpha = 1.0
        }


        if (hudHeight == 0.0) {
            healthBarWidth2 = 140.0
            healthBarWidth = 140.0
        }

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        RenderUtils.makeScissorBox(x, y + 40f - hudHeight.toFloat(), x + 140f, y + 40f)

        // 背景 - 应用透明度动画
        val bgAlpha = (180 * hudAlpha).toInt()
        val borderAlpha = (49 * hudAlpha).toInt()
        RenderUtils.drawRect(x, y, x + 140f, y + 40f, Color(0, 0, 0, bgAlpha).rgb)
        RenderUtils.drawRect(x, y + 37f, x + 140, y + 40f, Color(0, 0, 0, borderAlpha).rgb)

        // 健康条 - 应用透明度动画
        val healthBarAlpha = (220 * hudAlpha).toInt()
        RenderUtils.drawRect(x, y + 37f, (x + healthBarWidth2).toFloat(), y + 40f, Color(255, 0, 213, healthBarAlpha).rgb)
        RenderUtils.drawGradientRect(
            x, y + 37.0, x + healthBarWidth, y + 40.0,
            Color(0, 81, 179, healthBarAlpha).rgb,
            Color(healthColor shr 16 and 0xFF, healthColor shr 8 and 0xFF, healthColor and 0xFF, healthBarAlpha).rgb, 0f
        )

        // 健康文字
        Fonts.fontsiyuanback25.drawStringWithShadow(
            "\u2764",
            x + 40f + 85f - Fonts.fontsiyuanback25.getStringWidth(healthStr) / 2f - Fonts.fontsiyuanback25.getStringWidth("\u2764") / 1.9f,
            y + 26.5f,
            Color(hurt.red, hurt.green, hurt.blue, (255 * hudAlpha).toInt()).rgb
        )

        // 目标信息 - 应用透明度动画
        val textColor = Color(255, 255, 255, (255 * hudAlpha).toInt()).rgb

        Fonts.fontsiyuanback25.drawStringWithShadow(
            "XYZ: ${target.posX.toInt()} ${target.posY.toInt()} ${target.posZ.toInt()} | Hurt: ${target.hurtTime > 0}",
            x + 37f,
            y + 15f,
            textColor
        )

        if (target is EntityPlayer) {
            Fonts.fontsiyuanback25.drawStringWithShadow(
                "Block: ${if (target.isBlocking) "True" else "False"}",
                x + 37f,
                y + 25f,
                textColor
            )
        }

        Fonts.fontsiyuanback35.drawStringWithShadow(
            target.name,
            x + 36f,
            y + 4f,
            textColor
        )

        // 获取并绘制头像
        try {
            // 方法1：使用玩家皮肤的ResourceLocation
            val skinLocation = getPlayerSkin(target)
            if (skinLocation != null) {
                RenderUtils.drawHead(
                    skinLocation,
                    x.toInt() + 3,
                    y.toInt() + 3,
                    8f,
                    8f,
                    8,
                    8,
                    32,
                    32,
                    64f,
                    64f,
                    Color(255, 255, 255, (255 * hudAlpha).toInt())
                )
            }
        } catch (e: Exception) {
            // 如果无法获取皮肤，可以绘制默认头像或跳过
            e.printStackTrace()
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST)
    }

    /**
     * 获取玩家皮肤纹理
     */
    private fun getPlayerSkin(player: EntityPlayer): ResourceLocation? {
        try {
            // 方法1：直接从玩家实体获取皮肤
            val renderManager = mc.renderManager
            //    val renderer = renderManager.getEntityRenderObject(player)

            //  if (renderer != null) {
            // 尝试获取皮肤纹理
            //        val texture = renderer.getEntityTexture(player)
            //         if (texture != null) {
            //            return texture
            //           }
            //       }

            // 方法2：通过玩家的游戏配置获取皮肤
            val gameProfile = player.gameProfile
            if (gameProfile != null) {
                // 使用玩家的UUID来获取或生成皮肤资源位置
                val skinIdentifier = "skin_${gameProfile.id}"

                // 检查缓存
                if (skinCache.containsKey(skinIdentifier)) {
                    return skinCache[skinIdentifier]
                }

                // 如果没有缓存，创建一个默认的皮肤资源位置
                // 注意：这是简化的实现，实际中应该从Minecraft的皮肤系统获取
                val defaultSkin = ResourceLocation("textures/entity/steve.png")
                skinCache[skinIdentifier] = defaultSkin
                return defaultSkin
            }

            // 方法3：返回默认的Steve皮肤
            return ResourceLocation("textures/entity/steve.png")

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 清理缓存（可选）
     */
    fun clearCache() {
        skinCache.clear()
    }

    private fun getAnimationStateSmooth(newState: Double, prevState: Double, speed: Double): Double {
        return abs(prevState - newState).coerceAtMost(speed).let { diff ->
            if (prevState < newState) prevState + diff else prevState - diff
        }
    }

    class AnimationUtils {
        fun animate(current: Double, target: Double, speed: Double, inverse: Boolean = false): Double {
            val diff = kotlin.math.abs(current - target)
            val factor = (diff * speed).coerceAtMost(1.0)
            return if (inverse) current - factor else current + factor
        }
    }
}
