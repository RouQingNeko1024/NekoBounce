/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.ui.client.hud.element.elements

import io.qzz.nekobounce.ui.client.hud.HUD.addNotification
import io.qzz.nekobounce.ui.client.hud.HUD.notifications
import io.qzz.nekobounce.ui.client.hud.designer.GuiHudDesigner
import io.qzz.nekobounce.ui.client.hud.element.Border
import io.qzz.nekobounce.ui.client.hud.element.Element
import io.qzz.nekobounce.ui.client.hud.element.ElementInfo
import io.qzz.nekobounce.ui.client.hud.element.Side
import io.qzz.nekobounce.ui.client.hud.element.elements.Notification.Companion.maxTextLength
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.GlowUtils
import io.qzz.nekobounce.utils.client.ClientUtils
import io.qzz.nekobounce.utils.extensions.lerpWith
import io.qzz.nekobounce.utils.render.ColorUtils.withAlpha
import io.qzz.nekobounce.utils.render.RenderUtils
import io.qzz.nekobounce.utils.render.RenderUtils.deltaTime
import io.qzz.nekobounce.utils.render.RenderUtils.drawRoundedBorder
import io.qzz.nekobounce.utils.render.RenderUtils.drawRoundedRect
import java.awt.Color

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications2", single = true, priority = -1)
class Notifications2(
    x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element("Notifications2", x, y, scale, side) {

    val styles by choices("Styles", arrayOf("Liquidbounce","Classic"), "Liquidbounce")
    val shadowCheck by boolean("ShadowCheck", true) {styles == "Classic"}
    val shadowStrength by int("ShadowStrength", 1, 1..2) {styles == "Classic"}
    val horizontalFade by choices("HorizontalFade", arrayOf("InOnly", "OutOnly", "Both", "None"), "OutOnly")
    val padding by int("Padding", 5, -1..20)
    val roundRadius by float("RoundRadius", 3f, 0f..10f)
    val color by color("BackgroundColor", Color.BLACK.withAlpha(128))
    val renderBorder by boolean("RenderBorder", false)
    val borderColor by color("BorderColor", Color.BLUE.withAlpha(255)) { renderBorder }
    val borderWidth by float("BorderWidth", 2f, 0.5F..5F) { renderBorder }

    private val exampleNotification = Notification("Example Title", "Example Description")

    private var index = 0

    override fun updateElement() {
        if (mc.currentScreen is GuiHudDesigner && ClientUtils.runTimeTicks % 60 == 0) {
            // 使用映射将 Notifications2 的枚举转换为 Notifications 的枚举
            exampleNotification.severityType = when (index % 5) {
                0 -> Notifications.SeverityType.SUCCESS
                1 -> Notifications.SeverityType.INFO
                2 -> Notifications.SeverityType.WARNING
                3 -> Notifications.SeverityType.ERROR
                4 -> Notifications.SeverityType.RED_SUCCESS
                else -> Notifications.SeverityType.INFO
            }
            index = (index + 1) % 5
        }
    }

    override fun drawElement(): Border? {
        var verticalOffset = 0f

        maxTextLength = maxOf(100, notifications.maxOfOrNull { it.textLength } ?: 0)

        // 使用类型过滤和转换来处理通知
        notifications.removeIf { notification ->
            if (notification != exampleNotification) {
                notification.y = (notification.y..verticalOffset).lerpWith(RenderUtils.deltaTimeNormalized())
            }

            // 调用自定义的绘制方法
            drawCustomNotification(notification, this).also { 
                if (!it) verticalOffset += Notification.MAX_HEIGHT + padding 
            }
        }

        if (mc.currentScreen is GuiHudDesigner) {
            if (exampleNotification !in notifications) {
                index = 0
                addNotification(exampleNotification)
            }

            exampleNotification.fadeState = Notification.FadeState.STAY
            exampleNotification.textLength = Fonts.fontSemibold40.getStringWidth(exampleNotification.longestString)

            val notificationHeight = Notification.MAX_HEIGHT

            exampleNotification.y = 0F

            return Border(
                -(maxTextLength.toFloat() + 24 + 20), -notificationHeight.toFloat(), 0F, 0F
            )
        }

        return null
    }

    /**
     * 自定义通知绘制方法，处理不同类型的通知
     */
    private fun drawCustomNotification(notification: Notification, element: Notifications2): Boolean {
        return when (element.styles) {
            "Liquidbounce" -> drawLiquidBounceStyle(notification, element)
            "Classic" -> drawClassicStyle(notification, element)
            else -> drawLiquidBounceStyle(notification, element)
        }
    }

    private fun drawLiquidBounceStyle(notification: Notification, element: Notifications2): Boolean {
        val notificationWidth = notification.textLength + Notification.ICON_SIZE + 16F
        val extraSpace = 4F

        val currentX = when (notification.fadeState) {
            Notification.FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) notification.x else notificationWidth
            Notification.FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) notification.x else notificationWidth
            else -> notification.x
        }

        drawRoundedRect(0F, -notification.y - Notification.MAX_HEIGHT, -currentX - extraSpace, -notification.y, element.color.rgb, element.roundRadius)

        if (element.renderBorder) {
            drawRoundedBorder(
                0F,
                -notification.y - Notification.MAX_HEIGHT,
                -currentX - extraSpace,
                -notification.y,
                element.borderWidth,
                element.borderColor.rgb,
                element.roundRadius
            )
        }

        val nearTopSpot = -notification.y - Notification.MAX_HEIGHT + 10

        Fonts.fontSemibold40.drawString(notification.title, Notification.ICON_SIZE + 8F - currentX, nearTopSpot - 5, Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString(
            notification.description, Notification.ICON_SIZE + 8F - currentX, nearTopSpot + Fonts.fontSemibold40.fontHeight - 2, Int.MAX_VALUE
        )

        RenderUtils.drawImage(
            notification.severityType.path, -currentX + 2, -notification.y - Notification.MAX_HEIGHT + 4, Notification.ICON_SIZE, Notification.ICON_SIZE, radius = element.roundRadius
        )

        return updateNotificationAnimation(notification, notificationWidth)
    }

    private fun drawClassicStyle(notification: Notification, element: Notifications2): Boolean {
        val notificationWidth = notification.textLength + Notification.ICON_SIZE + 16F
        val extraSpace = 4F
        val currentX = when (notification.fadeState) {
            Notification.FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) notification.x else notificationWidth
            Notification.FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) notification.x else notificationWidth
            else -> notification.x
        }
        val ofst = 145f

        // 使用字符串比较而不是枚举比较
        val severityTypeName = notification.severityType.name

        val (backgroundColor, borderColor, textColor) = when (severityTypeName) {
            "SUCCESS" -> Triple(
                Color(28, 148, 97).withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color(46, 170, 80).withAlpha(230),
                Color.WHITE
            )
            "RED_SUCCESS" -> Triple(
                Color(137, 39, 39).withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color(229, 57, 53).withAlpha(230),
                Color.WHITE
            )
            "INFO" -> Triple(
                Color(52, 152, 219).withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color(41, 128, 185).withAlpha(230),
                Color.WHITE
            )
            "WARNING" -> Triple(
                Color(255, 193, 7).withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color(245, 166, 35).withAlpha(230),
                Color(33, 33, 33)
            )
            "ERROR" -> Triple(
                Color(239, 83, 80).withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color(222, 50, 50).withAlpha(230),
                Color.WHITE
            )
            else -> Triple(
                Color.BLACK.withAlpha(element.color.alpha.coerceAtLeast(180)),
                Color.BLUE.withAlpha(230),
                Color.WHITE
            )
        }

        val txWd = Fonts.fontGoogleSans45.getStringWidth(notification.title + ' ' + notification.description)

        if (element.shadowCheck) {
            val glowX = -currentX - extraSpace - txWd - 15f + ofst
            val glowY = -notification.y - Notification.MAX_HEIGHT + 2f
            val glowWidth = txWd + 17f
            val glowHeight = Notification.MAX_HEIGHT - 9f

            GlowUtils.drawGlow(
                glowX, glowY,
                glowWidth, glowHeight,
                (element.shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }

        drawRoundedRect(-currentX - extraSpace - txWd - 15f + ofst, -notification.y - Notification.MAX_HEIGHT + 2f, -currentX - extraSpace - 1F + ofst, -notification.y - 7f, backgroundColor.rgb, element.roundRadius)

        if (element.renderBorder) {
            drawRoundedBorder(
                -currentX - extraSpace - txWd - 15f,
                -notification.y - Notification.MAX_HEIGHT,
                -currentX - extraSpace,
                -notification.y,
                element.borderWidth,
                borderColor.rgb,
                element.roundRadius
            )
        }

        val nearTopSpot = -notification.y - Notification.MAX_HEIGHT + 10
        Fonts.fontGoogleSans45.drawString(notification.title + ' ' + notification.description + '!', -currentX - extraSpace - txWd - 8f + ofst, nearTopSpot + 5 - 6F, textColor.rgb)
        
        return updateNotificationAnimation(notification, notificationWidth)
    }

    private fun updateNotificationAnimation(notification: Notification, notificationWidth: Float): Boolean {
        val delta = deltaTime

        when (notification.fadeState) {
            Notification.FadeState.IN -> {
                if (notification.x < notificationWidth) {
                    notification.x += delta
                }
                if (notification.x >= notificationWidth) {
                    notification.fadeState = Notification.FadeState.STAY
                    notification.x = notificationWidth
                }
                return false
            }

            Notification.FadeState.STAY -> {
                if (notification.textLength != maxTextLength) {
                    maxTextLength = maxOf(notification.textLength, maxTextLength)
                    notification.x = maxTextLength + Notification.ICON_SIZE + 16F
                }
                return false
            }

            Notification.FadeState.OUT -> {
                if (notification.x > 0) {
                    notification.x -= delta
                    notification.y -= delta / 4F
                } else {
                    notification.fadeState = Notification.FadeState.END
                }
                return false
            }

            Notification.FadeState.END -> return true
        }

        return false
    }
}