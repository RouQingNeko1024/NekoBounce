/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import net.ccbluex.liquidbounce.utils.client.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.extensions.lerpWith
import net.ccbluex.liquidbounce.utils.kotlin.removeEach
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRectInt
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.minecraft.scoreboard.ScoreObjective
import net.minecraft.scoreboard.ScorePlayerTeam
import net.minecraft.util.EnumChatFormatting
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs

/**
 * CustomHUD scoreboard
 *
 * Allows to move and customize minecraft scoreboard
 */
@ElementInfo(name = "Scoreboard")
class ScoreboardElement(
    x: Double = 11.0, y: Double = 100.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.LEFT, Side.Vertical.MIDDLE)
) : Element("Scoreboard", x, y, scale, side) {
    private val textColor by color("TextColor", Color.WHITE)
    private val backgroundColor by color("BackgroundColor", Color.BLACK.withAlpha(128))
    private val roundedRectRadius by float("Rounded-Radius", 3F, 0F..5F)

    // 渐变背景设置
    private val gradientBackground by boolean("GradientBackground", false)
    private val leftColor by color("LeftColor", Color.BLACK.withAlpha(50)) { gradientBackground }
    private val rightColor by color("RightColor", Color.BLACK.withAlpha(200)) { gradientBackground }
    private val gradientDirection by choices("GradientDirection", arrayOf("LeftToRight", "RightToLeft", "TopToBottom", "BottomToTop"), "LeftToRight") { gradientBackground }

    // 添加阴影设置
    private val shadow by boolean("Shadow", false)
    private val shadowStrength by int("ShadowStrength", 1, 1..2) { shadow }

    private val rect by boolean("Rect", true)
    private val rectColor = color("RectangleColor", Color(0, 111, 255)) { rect }

    private val drawRectOnTitle by boolean("DrawRectOnTitle", true)
    private val titleRectColor by color("TitleRectColor", Color.BLACK.withAlpha(128)) { drawRectOnTitle }
    private val titleRectExtraHeight by int("TitleRectExtraHeight", 5, 0..20) { drawRectOnTitle }
    private val rectHeightPadding by int("TitleRectHeightPadding", 0, 0..10) { drawRectOnTitle }

    private val serverIp by choices("ServerIP", arrayOf("Normal", "None", "Client", "Website","qwq" ,"风格岛" ,"Cat","Styles","Flux","花雨庭","柔情","xinxin","Fdp"), "Client")
    private val number by boolean("Number", false)
    private val textShadow by boolean("TextShadow", false)
    private val font by font("Font", Fonts.fontSemibold35)

    // 绘制渐变背景 - 使用LiquidBounce的渲染系统
    private fun drawGradientBackground(minX: Int, minY: Int, maxX: Int, maxY: Int, corners: RenderUtils.RoundedCorners) {
        if (!gradientBackground) return

        val width = maxX - minX
        val height = maxY - minY
        
        // 保存GL状态
        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glPushMatrix()
        
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glShadeModel(GL_SMOOTH)
        
        glBegin(GL_QUADS)
        
        when (gradientDirection) {
            "LeftToRight" -> {
                // 从左到右渐变
                glColor4f(
                    leftColor.red / 255f,
                    leftColor.green / 255f,
                    leftColor.blue / 255f,
                    leftColor.alpha / 255f
                )
                glVertex2i(minX, minY)
                glVertex2i(minX, maxY)
                glColor4f(
                    rightColor.red / 255f,
                    rightColor.green / 255f,
                    rightColor.blue / 255f,
                    rightColor.alpha / 255f
                )
                glVertex2i(maxX, maxY)
                glVertex2i(maxX, minY)
            }
            "RightToLeft" -> {
                // 从右到左渐变
                glColor4f(
                    rightColor.red / 255f,
                    rightColor.green / 255f,
                    rightColor.blue / 255f,
                    rightColor.alpha / 255f
                )
                glVertex2i(minX, minY)
                glVertex2i(minX, maxY)
                glColor4f(
                    leftColor.red / 255f,
                    leftColor.green / 255f,
                    leftColor.blue / 255f,
                    leftColor.alpha / 255f
                )
                glVertex2i(maxX, maxY)
                glVertex2i(maxX, minY)
            }
            "TopToBottom" -> {
                // 从上到下渐变
                glColor4f(
                    leftColor.red / 255f,
                    leftColor.green / 255f,
                    leftColor.blue / 255f,
                    leftColor.alpha / 255f
                )
                glVertex2i(minX, minY)
                glVertex2i(maxX, minY)
                glColor4f(
                    rightColor.red / 255f,
                    rightColor.green / 255f,
                    rightColor.blue / 255f,
                    rightColor.alpha / 255f
                )
                glVertex2i(maxX, maxY)
                glVertex2i(minX, maxY)
            }
            "BottomToTop" -> {
                // 从下到上渐变
                glColor4f(
                    rightColor.red / 255f,
                    rightColor.green / 255f,
                    rightColor.blue / 255f,
                    rightColor.alpha / 255f
                )
                glVertex2i(minX, minY)
                glVertex2i(maxX, minY)
                glColor4f(
                    leftColor.red / 255f,
                    leftColor.green / 255f,
                    leftColor.blue / 255f,
                    leftColor.alpha / 255f
                )
                glVertex2i(maxX, maxY)
                glVertex2i(minX, maxY)
            }
        }
        
        glEnd()
        
        // 恢复GL状态
        glShadeModel(GL_FLAT)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        
        glPopMatrix()
        glPopAttrib()
    }

    // 阴影绘制函数
    private fun showShadow(startX: Float, startY: Float, width: Float, height: Float) {
        if (shadow) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrength * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }

    /**
     * Draw element
     */
    override fun drawElement(): Border? {
        assumeNonVolatile {
            val (fontRenderer, fontHeight) = font to ((font as? GameFontRenderer)?.height ?: font.FONT_HEIGHT)
            val textColor = textColor.rgb
            val backColor = backgroundColor.rgb

            val worldScoreboard = mc.theWorld.scoreboard ?: return null
            var currObjective: ScoreObjective? = null
            val playerTeam = worldScoreboard.getPlayersTeam(mc.thePlayer.name)

            if (playerTeam != null) {
                val colorIndex = playerTeam.chatFormat.colorIndex

                if (colorIndex >= 0) currObjective = worldScoreboard.getObjectiveInDisplaySlot(3 + colorIndex)
            }

            val objective = currObjective ?: worldScoreboard.getObjectiveInDisplaySlot(1) ?: return null

            val scoreboard = objective.scoreboard ?: return null
            var scoreCollection = scoreboard.getSortedScores(objective) ?: return null
            val scores = scoreCollection.filter { it.playerName?.startsWith("#") == false }

            scoreCollection = if (scores.size > 15) {
                scores.drop(scoreCollection.size - 15)
            } else scores

            var maxWidth = fontRenderer.getStringWidth(objective.displayName)

            for (score in scoreCollection) {
                val scorePlayerTeam = scoreboard.getPlayersTeam(score.playerName)
                val width = if (number) {
                    "${ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)}: ${EnumChatFormatting.RED}${score.scorePoints}"
                } else {
                    ScorePlayerTeam.formatPlayerName(scorePlayerTeam, score.playerName)
                }
                maxWidth = maxWidth.coerceAtLeast(fontRenderer.getStringWidth(width))
            }

            val maxHeight = scoreCollection.size * fontHeight
            val l1 = -maxWidth  - 3

            val inc = if (drawRectOnTitle) titleRectExtraHeight else 0

            val (minX, maxX) = if (side.horizontal != Side.Horizontal.LEFT) {
                l1 - 4 to 7
            } else {
                -7 to (abs(l1 - 4))
            }

            val numberX = maxX - 7

            val indexRects = mutableListOf<() -> Unit>()

            // 绘制阴影
            if (shadow) {
                val shadowExtension = 5F
                val shadowX = minX - shadowExtension
                val shadowY = -(4 + inc) - shadowExtension
                val shadowWidth = (maxX - minX) + shadowExtension * 2
                val shadowHeight = (maxHeight + fontHeight + 2 + (4 + inc)) + shadowExtension * 2

                showShadow(shadowX.toFloat(), shadowY.toFloat(), shadowWidth.toFloat(), shadowHeight.toFloat())
            }

            // 修复withClipping调用，使用正确的参数格式
            withClipping(
                {
                    val corners = if (rect) {
                        if (side.horizontal != Side.Horizontal.LEFT) {
                            RenderUtils.RoundedCorners.LEFT_ONLY
                        } else {
                            RenderUtils.RoundedCorners.RIGHT_ONLY
                        }
                    } else {
                        RenderUtils.RoundedCorners.ALL
                    }

                    if (gradientBackground) {
                        // 绘制渐变背景
                        drawGradientBackground(
                            minX,
                            -(4 + inc),
                            maxX,
                            maxHeight + fontHeight + 2,
                            corners
                        )
                    } else {
                        // 绘制普通背景
                        drawRoundedRectInt(
                            minX,
                            -(4 + inc),
                            maxX,
                            maxHeight + fontHeight + 2,
                            backColor,
                            roundedRectRadius,
                            corners
                        )
                    }
                },
                {
                    scoreCollection.filterNotNull().forEachIndexed { index, score ->
                        val team = scoreboard.getPlayersTeam(score.playerName)

                        var name = ScorePlayerTeam.formatPlayerName(team, score.playerName)
                        val scorePoints = if (number) "${EnumChatFormatting.RED}${score.scorePoints}" else ""

                        val height = maxHeight - index * fontHeight.toFloat()

                        glColor4f(1f, 1f, 1f, 1f)

                        if (serverIp != "Normal") {
                            try {
                                val nameWithoutFormatting = name?.replace(EnumChatFormatting.RESET.toString(), "")
                                    ?.replace(Regex("[\u00a7&][0-9a-fk-or]"), "")?.trim()
                                val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase() ?: ""

                                val domainRegex =
Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b|欧|OMG|onmygod|@|工作室|KKC|Domcer|network|kkc|weipu|布吉岛|像素未来|国际版|庭", RegexOption.IGNORE_CASE)
                                val containsDomain = nameWithoutFormatting?.let { domainRegex.containsMatchIn(it) } ?: false

                                runCatching {
                                    if (nameWithoutFormatting?.lowercase() == trimmedServerIP || containsDomain) {
                                        val colorCode = name?.substring(0, 2) ?: "§9"
                                        name = when (serverIp.lowercase()) {
                                            "none" -> ""
                                            "client" -> "§b§lNekoBounce"
                                            "website" -> "§b§lNekoBounce.qzz.io"
                                            "cat" -> "§b§l柔情猫娘"
                                            "qwq" -> "§b§l猫娘客户端"
                                            "风格岛" -> "§b§l风格岛"
                                            "styles" -> "§bstyles.wtf"
                                            "flux" -> "§bflux.today"
                                            "花雨庭" -> "§c❀§b 花雨庭 §c❀§r"
                                            "柔情" -> "§c❀§b 柔情猫娘 §c❀§r"
                                            "xinxin" -> "§b免费heshuyou.xyz§r"
                                            "Fdp" -> "§7[§b§lFDPClient§r§7]"
                                            else -> name
                                        }
                                    }
                                }.onFailure { e ->
                                    LOGGER.error("Error while changing Scoreboard Server IP: ${e.message}")
                                }
                            } catch (e: Exception) {
                                LOGGER.error("Error while drawing ScoreboardElement", e)
                            }
                        }

                        val textX = if (side.horizontal != Side.Horizontal.LEFT) {
                            l1
                        } else {
                            minX + 4
                        }.toFloat()

                        fontRenderer.drawString(name, textX, height, textColor, textShadow)
                        if (number) {
                            fontRenderer.drawString(
                                scorePoints,
                                (numberX - font.getStringWidth(scorePoints)).toFloat(),
                                height,
                                textColor,
                                textShadow
                            )
                        }

                        if (index == scoreCollection.size - 1) {
                            val title = objective.displayName
                            val displayName = if (serverIp != "Normal") {
                                try {
                                    val nameWithoutFormatting = title.replace(EnumChatFormatting.RESET.toString(), "")
                                        .replace(Regex("[\u00a7&][0-9a-fk-or]"), "").trim()
                                    val trimmedServerIP = mc.currentServerData?.serverIP?.trim()?.lowercase() ?: ""

                                    val domainRegex =
                                        Regex("\\b(?:[a-zA-Z0-9](?:[a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,63}\\b")
                                    val containsDomain =
                                        nameWithoutFormatting.let { domainRegex.containsMatchIn(it) } == true

                                    if (nameWithoutFormatting.lowercase() == trimmedServerIP || containsDomain) {
                                        val colorCode = title.substring(0, 2)
                                        when (serverIp.lowercase()) {
                                            "none" -> ""
                                            "client" -> "$colorCode$CLIENT_NAME"
                                            "website" -> "$colorCode$CLIENT_WEBSITE"
                                            else -> title
                                        }
                                    } else title
                                } catch (e: Exception) {
                                    LOGGER.error("Error while drawing ScoreboardElement", e)
                                    title
                                }
                            } else title

                            if (drawRectOnTitle) {
                                drawRect(minX, -(4 + inc), maxX, fontHeight - inc + rectHeightPadding, titleRectColor.rgb)
                            }

                            glColor4f(1f, 1f, 1f, 1f)

                            fontRenderer.drawString(
                                displayName,
                                (minX..maxX).lerpWith(0.5F) - fontRenderer.getStringWidth(displayName) / 2,
                                height - fontHeight - inc,
                                textColor,
                                textShadow
                            )
                        }

                        indexRects += {
                            if (rect) {
                                val rectColor = if (this.rectColor.rainbow) {
                                    ColorUtils.rainbow(400000000L * index).rgb
                                } else {
                                    this.rectColor.selectedColor().rgb
                                }

                                drawRoundedRect(
                                    (if (side.horizontal != Side.Horizontal.LEFT) maxX + 4 else minX - 4).toFloat(),
                                    (if (index == scoreCollection.size - 1) -2F else height) - inc - 2F,
                                    (if (side.horizontal != Side.Horizontal.LEFT) maxX else minX).toFloat(),
                                    (if (index == 0) fontHeight.toFloat() else height + fontHeight * 2F) + 2F,
                                    rectColor,
                                    roundedRectRadius,
                                    if (side.horizontal != Side.Horizontal.LEFT) {
                                        RenderUtils.RoundedCorners.RIGHT_ONLY
                                    } else {
                                        RenderUtils.RoundedCorners.LEFT_ONLY
                                    }
                                )
                            }
                        }
                    }
                }
            )

            indexRects.removeEach { it(); true }

            return Border(minX.toFloat() - if (rect && side.horizontal == Side.Horizontal.LEFT) 5 else 0, -4F - inc, maxX.toFloat() + if (rect && side.horizontal != Side.Horizontal.LEFT) 5 else 0, maxHeight + fontHeight + 2F)
        }

        return null
    }
}