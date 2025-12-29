/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.ui.client.hud.element.elements

import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.ui.client.hud.HUD.addNotification
import io.qzz.nekobounce.ui.client.hud.HUD.notifications
import io.qzz.nekobounce.ui.client.hud.designer.GuiHudDesigner
import io.qzz.nekobounce.ui.client.hud.element.Border
import io.qzz.nekobounce.ui.client.hud.element.Element
import io.qzz.nekobounce.ui.client.hud.element.ElementInfo
import io.qzz.nekobounce.ui.client.hud.element.Side
import io.qzz.nekobounce.ui.client.hud.element.elements.Notification.Companion.maxTextLength
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.client.ClientUtils
import io.qzz.nekobounce.utils.extensions.lerpWith
import io.qzz.nekobounce.utils.render.ColorUtils.withAlpha
import io.qzz.nekobounce.utils.render.RenderUtils
import io.qzz.nekobounce.utils.render.RenderUtils.deltaTime
import io.qzz.nekobounce.utils.render.RenderUtils.drawRoundedBorder
import io.qzz.nekobounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.util.ResourceLocation
import java.awt.Color

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true, priority = -1)
class Notifications(
    x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element("Notifications", x, y, scale, side) {

    val style by choices("Style", arrayOf("Classic", "Modern", "Tenacity", "Intellij", "Skid", "FDP"), "Modern")
    val horizontalFade by choices("HorizontalFade", arrayOf("InOnly", "OutOnly", "Both", "None"), "OutOnly")
    val padding by int("Padding", 5, 1..20)
    val roundRadius by float("RoundRadius", 3f, 0f..10f)
    val color by color("BackgroundColor", Color.BLACK.withAlpha(128))
    val renderBorder by boolean("RenderBorder", false)
    val borderColor by color("BorderColor", Color.BLUE.withAlpha(255)) { renderBorder }
    val borderWidth by float("BorderWidth", 2f, 0.5F..5F) { renderBorder }
    val backGroundAlpha by int("BackGroundAlpha", 170, 0..255)
    val titleShadow by boolean("TitleShadow", false)
    val contentShadow by boolean("ContentShadow", true)
    val whiteText by boolean("WhiteTextColor", true)
    val modeColored by boolean("CustomModeColored", true)
    
    // Text offset settings for all styles
    val textOffsetX by int("TextOffsetX", 0, -50..50)
    val textOffsetY by int("TextOffsetY", 0, -50..50)

    private val exampleNotification = Notification("Example Title", "Example Description")

    private var index = 0

    override fun updateElement() {
        if (mc.currentScreen is GuiHudDesigner && ClientUtils.runTimeTicks % 60 == 0) {
            exampleNotification.severityType = SeverityType.entries[++index % SeverityType.entries.size]
        }
    }

    override fun drawElement(): Border? {
        var verticalOffset = 0f

        maxTextLength = maxOf(100, notifications.maxOfOrNull { it.textLength } ?: 0)

        notifications.removeIf { notification ->
            if (notification != exampleNotification) {
                notification.y = (notification.y..verticalOffset).lerpWith(RenderUtils.deltaTimeNormalized())
            }

            notification.drawNotification(this).also { if (!it) verticalOffset += Notification.MAX_HEIGHT + padding }
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

    enum class SeverityType(val path: ResourceLocation) {
        SUCCESS(ResourceLocation("liquidbounce/notifications/success.png")),
        RED_SUCCESS(ResourceLocation("liquidbounce/notifications/redsuccess.png")),
        INFO(ResourceLocation("liquidbounce/notifications/info.png")),
        WARNING(ResourceLocation("liquidbounce/notifications/warning.png")),
        ERROR(ResourceLocation("liquidbounce/notifications/error.png"))
    }
}

class Notification(
    var title: String,
    var description: String,
    private val delay: Long = 2000L,
    var severityType: Notifications.SeverityType = Notifications.SeverityType.INFO
) {
    var x = 0F
    var y: Float = (notifications.lastOrNull()?.y ?: 0F) + MAX_HEIGHT * 2
    var textLength = 0

    val longestString: String
        get() = arrayOf(title, description).maxByOrNull { Fonts.fontSemibold40.getStringWidth(it) } ?: title

    private var stay = delay
    private var fadeStep = 0F
    var fadeState = FadeState.IN

    fun replaceModuleNotification(title: String, description: String, severityType: Notifications.SeverityType) {
        if (fadeState.ordinal > 1) {
            return
        }

        stay = delay
        this.severityType = severityType
        this.title = title
        this.description = description

        textLength = Fonts.fontSemibold40.getStringWidth(longestString)
        maxTextLength = maxOf(textLength, maxTextLength)

        notifications.sortBy { it.stay }
    }

    companion object {
        fun informative(title: String, message: String, delay: Long = 2000L) =
            Notification(title, message, delay, Notifications.SeverityType.INFO)

        fun informative(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.INFO)

        fun error(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.ERROR)

        fun warning(title: Module, message: String, delay: Long = 2000L) =
            Notification(title.spacedName, message, delay, Notifications.SeverityType.WARNING)

        var maxTextLength = 0
        const val MAX_HEIGHT = 32
        const val ICON_SIZE = 24
    }

    enum class FadeState {
        IN, STAY, OUT, END
    }

    init {
        textLength = Fonts.fontSemibold40.getStringWidth(longestString)
        maxTextLength = maxOf(maxTextLength, textLength)
    }

    fun drawNotification(element: Notifications): Boolean {
        val style = element.style
        
        return when (style) {
            "Modern" -> drawModernStyle(element)
            "FDP" -> drawFDPStyle(element)
            "Tenacity" -> drawTenacityStyle(element)
            "Intellij" -> drawIntellijStyle(element)
            "Skid" -> drawSkidStyle(element)
            else -> drawClassicStyle(element)
        }
    }

    private fun drawModernStyle(element: Notifications): Boolean {
        val notificationWidth = maxTextLength + ICON_SIZE + 16F
        val extraSpace = 4F

        val currentX = when (fadeState) {
            FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) x else notificationWidth
            FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) x else notificationWidth
            else -> x
        }

        // Modern style background with progress bar
        val bgColor = Color(0, 0, 0, element.backGroundAlpha)
        drawRoundedRect(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, bgColor.rgb, element.roundRadius)

        // Progress bar
        val progressColor = getModernProgressColor(element)
        val progressWidth = maxOf(notificationWidth - notificationWidth * ((System.currentTimeMillis() - stay) / (delay * 2F)), 0F)
        drawRoundedRect(0F, -y - 2F, -progressWidth - extraSpace, -y, progressColor.rgb, element.roundRadius)

        val nearTopSpot = -y - MAX_HEIGHT + 10

        // Text with offset
        val textColor = if (element.whiteText) Color.WHITE.rgb else Color(10, 10, 10).rgb
        val titleX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val titleY = nearTopSpot - 5 + element.textOffsetY
        val descX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val descY = nearTopSpot + Fonts.fontSemibold40.fontHeight - 2 + element.textOffsetY
        
        if (element.titleShadow) {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor, true)
        } else {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        }
        
        if (element.contentShadow) {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor, true)
        } else {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor)
        }

        RenderUtils.drawImage(
            severityType.path, -currentX + 2, -y - MAX_HEIGHT + 4, ICON_SIZE, ICON_SIZE, radius = element.roundRadius
        )

        return updateAnimation(notificationWidth)
    }

    private fun drawFDPStyle(element: Notifications): Boolean {
        val notificationWidth = maxTextLength + ICON_SIZE + 16F
        val extraSpace = 4F

        val currentX = when (fadeState) {
            FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) x else notificationWidth
            FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) x else notificationWidth
            else -> x
        }

        // FDP style background
        val bgColor = Color(0, 0, 0, element.backGroundAlpha / 4)
        drawRoundedRect(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, bgColor.rgb, element.roundRadius)

        // Progress bar
        val progressWidth = maxOf(notificationWidth - notificationWidth * ((System.currentTimeMillis() - stay) / (delay * 2F)), 0F)
        drawRoundedRect(0F, -y - 2F, -progressWidth - extraSpace, -y, Color(0, 0, 0, 40).rgb, element.roundRadius)

        val nearTopSpot = -y - MAX_HEIGHT + 10

        // Text with offset
        val textColor = if (element.whiteText) Color.WHITE.rgb else Color(10, 10, 10).rgb
        val titleX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val titleY = nearTopSpot - 5 + element.textOffsetY
        val descX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val descY = nearTopSpot + Fonts.fontSemibold40.fontHeight - 2 + element.textOffsetY
        
        if (element.titleShadow) {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor, true)
        } else {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        }
        
        if (element.contentShadow) {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor, true)
        } else {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor)
        }

        RenderUtils.drawImage(
            severityType.path, -currentX + 2, -y - MAX_HEIGHT + 4, ICON_SIZE, ICON_SIZE, radius = element.roundRadius
        )

        return updateAnimation(notificationWidth)
    }

    private fun drawTenacityStyle(element: Notifications): Boolean {
        val titleWidth = Fonts.fontSemibold40.getStringWidth(title)
        val descWidth = Fonts.fontSemibold35.getStringWidth(description)
        val notificationWidth = maxOf(100, maxOf(titleWidth, descWidth) + 40)
        val currentX = if (fadeState == FadeState.IN || fadeState == FadeState.OUT) x else notificationWidth.toFloat()
        
        val bgColor = when (severityType) {
            Notifications.SeverityType.ERROR -> Color(180, 0, 0, element.backGroundAlpha)
            Notifications.SeverityType.SUCCESS -> Color(0, 180, 0, element.backGroundAlpha)
            Notifications.SeverityType.WARNING -> Color(0, 0, 0, element.backGroundAlpha)
            else -> Color(0, 0, 0, element.backGroundAlpha)
        }
        
        drawRoundedRect(-18F - currentX, -y - MAX_HEIGHT + 1, notificationWidth.toFloat() - currentX, -y - 2, bgColor.rgb, 5f)
        
        // Draw icon based on type
        val iconPath = when (severityType) {
            Notifications.SeverityType.ERROR -> ResourceLocation("liquidbounce/notifications/tenacity/cross.png")
            Notifications.SeverityType.SUCCESS -> ResourceLocation("liquidbounce/notifications/tenacity/tick.png")
            Notifications.SeverityType.WARNING -> ResourceLocation("liquidbounce/notifications/warning.png")
            else -> ResourceLocation("liquidbounce/notifications/tenacity/info.png")
        }
        
        RenderUtils.drawImage(iconPath, -13F - currentX, -y - MAX_HEIGHT + 5, 18, 18)
        
        // Text with offset
        val textColor = if (element.whiteText) Color.WHITE.rgb else Color(10, 10, 10).rgb
        val titleX = 9F - currentX + element.textOffsetX
        val titleY = -y - 16F + element.textOffsetY
        val descX = 9F - currentX + element.textOffsetX
        val descY = -y - 6F + element.textOffsetY
        
        Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        Fonts.fontSemibold35.drawString(description, descX, descY, textColor)

        return updateAnimation(notificationWidth.toFloat())
    }

    private fun drawIntellijStyle(element: Notifications): Boolean {
        val titleWidth = Fonts.fontSemibold40.getStringWidth(title)
        val descWidth = Fonts.fontSemibold35.getStringWidth(description)
        val textLength = maxOf(titleWidth, descWidth)
        val notificationWidth = textLength + 30
        val currentX = if (fadeState == FadeState.IN || fadeState == FadeState.OUT) x else notificationWidth.toFloat()
        
        val bgColor = when (severityType) {
            Notifications.SeverityType.ERROR -> Color(89, 61, 65, element.backGroundAlpha)
            Notifications.SeverityType.INFO -> Color(61, 72, 87, element.backGroundAlpha)
            Notifications.SeverityType.SUCCESS -> Color(55, 78, 55, element.backGroundAlpha)
            Notifications.SeverityType.WARNING -> Color(80, 80, 57, element.backGroundAlpha)
            else -> Color(61, 72, 87, element.backGroundAlpha)
        }
        
        // Main background
        drawRoundedRect(0F - currentX, -y - MAX_HEIGHT, notificationWidth.toFloat() - currentX, -y, bgColor.rgb, 0f)
        
        // Accent background
        val accentColor = when (severityType) {
            Notifications.SeverityType.ERROR -> Color(115, 69, 75, element.backGroundAlpha)
            Notifications.SeverityType.INFO -> Color(70, 94, 115, element.backGroundAlpha)
            Notifications.SeverityType.SUCCESS -> Color(67, 104, 67, element.backGroundAlpha)
            Notifications.SeverityType.WARNING -> Color(103, 103, 63, element.backGroundAlpha)
            else -> Color(70, 94, 115, element.backGroundAlpha)
        }
        drawRoundedRect(1F - currentX, -y - MAX_HEIGHT + 1, notificationWidth.toFloat() - 1 - currentX, -y - 1, accentColor.rgb, 0f)
        
        // Draw icon
        val iconPath = when (severityType) {
            Notifications.SeverityType.ERROR -> ResourceLocation("liquidbounce/notifications/error.png")
            Notifications.SeverityType.SUCCESS -> ResourceLocation("liquidbounce/notifications/success.png")
            Notifications.SeverityType.WARNING -> ResourceLocation("liquidbounce/notifications/warning.png")
            else -> ResourceLocation("liquidbounce/notifications/info.png")
        }
        
        RenderUtils.drawImage(iconPath, notificationWidth.toFloat() - 25 - currentX, -y - MAX_HEIGHT + 4, 7, 7)
        
        // Text with offset
        val textColor = when (severityType) {
            Notifications.SeverityType.ERROR -> Color(249, 130, 108).rgb
            Notifications.SeverityType.INFO -> Color(119, 145, 147).rgb
            Notifications.SeverityType.SUCCESS -> Color(10, 142, 2).rgb
            Notifications.SeverityType.WARNING -> Color(175, 163, 0).rgb
            else -> if (element.whiteText) Color.WHITE.rgb else Color(10, 10, 10).rgb
        }
        
        val titleX = 6F - currentX + element.textOffsetX
        val titleY = -y - 25F + element.textOffsetY
        val descX = 6F - currentX + element.textOffsetX
        val descY = -y - 13F + element.textOffsetY
        
        Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        Fonts.fontSemibold35.drawString(description, descX, descY, textColor)

        return updateAnimation(notificationWidth.toFloat())
    }

    private fun drawSkidStyle(element: Notifications): Boolean {
        val notificationWidth = maxTextLength + ICON_SIZE + 16F
        val currentX = if (fadeState == FadeState.IN || fadeState == FadeState.OUT) x else notificationWidth
        
        // Side bar
        val sideColor = getModernProgressColor(element)
        drawRoundedRect(2F - currentX, -y - MAX_HEIGHT, 4F - currentX, -y, sideColor.rgb, 0f)
        
        // Main background
        drawRoundedRect(3F - currentX, -y - MAX_HEIGHT, notificationWidth + 5F - currentX, -y, Color(0, 0, 0, 150).rgb, 0f)
        
        // Gradient effect (simplified)
        drawRoundedRect(3F - currentX, -y - MAX_HEIGHT, 20F - currentX, -y, sideColor.rgb, 0f)
        
        // Progress bar
        val progressWidth = maxOf(notificationWidth - notificationWidth * ((System.currentTimeMillis() - stay) / (delay * 2F)), 0F)
        drawRoundedRect(2F - currentX, -y - 1F, progressWidth + 5F - currentX, -y, Color(52, 97, 237).rgb, 0f)

        // Text with offset
        val textColor = if (element.whiteText) Color.WHITE.rgb else Color(10, 10, 10).rgb
        val titleX = 4F - currentX + element.textOffsetX
        val titleY = -y - 29F + element.textOffsetY
        val descX = 4F - currentX + element.textOffsetX
        val descY = -y - 22F + element.textOffsetY
        
        if (element.titleShadow) {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor, true)
        } else {
            Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        }
        
        if (element.contentShadow) {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor, true)
        } else {
            Fonts.fontSemibold35.drawString(description, descX, descY, textColor)
        }

        return updateAnimation(notificationWidth)
    }

    private fun drawClassicStyle(element: Notifications): Boolean {
        val notificationWidth = maxTextLength + ICON_SIZE + 16F
        val extraSpace = 4F

        val currentX = when (fadeState) {
            FadeState.IN -> if (element.horizontalFade in arrayOf("InOnly", "Both")) x else notificationWidth
            FadeState.OUT -> if (element.horizontalFade in arrayOf("OutOnly", "Both")) x else notificationWidth
            else -> x
        }

        // Classic background
        drawRoundedRect(0F, -y - MAX_HEIGHT, -currentX - extraSpace, -y, element.color.rgb, element.roundRadius)

        // Border if enabled
        if (element.renderBorder) {
            drawRoundedBorder(
                0F,
                -y - MAX_HEIGHT,
                -currentX - extraSpace,
                -y,
                element.borderWidth,
                element.borderColor.rgb,
                element.roundRadius
            )
        }

        val nearTopSpot = -y - MAX_HEIGHT + 10

        // Text with offset
        val textColor = if (element.whiteText) Color.WHITE.rgb else Color.WHITE.rgb
        val titleX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val titleY = nearTopSpot - 5 + element.textOffsetY
        val descX = ICON_SIZE + 8F - currentX + element.textOffsetX
        val descY = nearTopSpot + Fonts.fontSemibold40.fontHeight - 2 + element.textOffsetY
        
        Fonts.fontSemibold40.drawString(title, titleX, titleY, textColor)
        Fonts.fontSemibold35.drawString(description, descX, descY, Int.MAX_VALUE)

        // Icon
        RenderUtils.drawImage(
            severityType.path, -currentX + 2, -y - MAX_HEIGHT + 4, ICON_SIZE, ICON_SIZE, radius = element.roundRadius
        )

        return updateAnimation(notificationWidth)
    }

    private fun getModernProgressColor(element: Notifications): Color {
        if (!element.modeColored) {
            return when (severityType) {
                Notifications.SeverityType.SUCCESS -> Color(0x60E092)
                Notifications.SeverityType.ERROR -> Color(0xFF2F2F)
                Notifications.SeverityType.WARNING -> Color(0xF5FD00)
                Notifications.SeverityType.INFO -> Color(0x6490A7)
                else -> Color(0x6490A7)
            }
        }

        return when (severityType) {
            Notifications.SeverityType.SUCCESS -> Color(0x36D399)
            Notifications.SeverityType.ERROR -> Color(0xF87272)
            Notifications.SeverityType.WARNING -> Color(0xFBBD23)
            Notifications.SeverityType.INFO -> Color(0xF2F2F2)
            else -> Color(0xF2F2F2)
        }
    }

    private fun updateAnimation(notificationWidth: Float): Boolean {
        val delta = deltaTime

        when (fadeState) {
            FadeState.IN -> {
                if (x < notificationWidth) {
                    x += delta
                }
                if (x >= notificationWidth) {
                    fadeState = FadeState.STAY
                    x = notificationWidth
                    fadeStep = notificationWidth
                }
                stay = delay
            }

            FadeState.STAY -> {
                if (textLength != maxTextLength) {
                    maxTextLength = maxOf(textLength, maxTextLength)
                    x = maxTextLength + ICON_SIZE + 16F
                    fadeStep = x
                }
                stay -= delta
                if (stay <= 0) {
                    fadeState = FadeState.OUT
                }
            }

            FadeState.OUT -> if (x > 0) {
                x -= delta
                y -= delta / 4F
            } else {
                fadeState = FadeState.END
            }

            FadeState.END -> return true
        }

        return false
    }
}