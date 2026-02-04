/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code By GoldBounce,Lizz,NightSky,FDP
 * https://github.com/SkidderMC/FDPClient
 * https://github.com/qm123pz/NightSky-Client
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.ScreenEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.inventory.Slot
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.client.shader.Framebuffer
import net.minecraft.client.network.NetworkPlayerInfo
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiPlayerTabOverlay
import net.minecraft.util.IChatComponent
import net.minecraft.world.WorldSettings
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20
import org.lwjgl.opengl.EXTFramebufferObject
import org.lwjgl.opengl.EXTPackedDepthStencil
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.ceil
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.cos
import kotlin.math.sin

object Island : Module("Island", Category.RENDER) {
    private val ClientName by text("ClientName", "Neko")
    private val animTension by float("BounceTension", 0.05f, 0.01f..1.0f)
    private val animFriction by float("BounceFriction", 0.3f, 0.01f..1.0f)
    private val styles = "New Opai"
    private val customip by boolean("customIP", false)
    private val ip by text("IP", "hidden.ip") { customip }
    private val ColorA_ by int("Red", 255, 0..255)
    private val ColorB_ by int("Green", 255, 0..255)
    private val ColorC_ by int("Blue", 255, 0..255)

    private val BackgroundAlpha by int("BackGroundAlpha", 160, 0..255)

    // Shadow Setting
    private val ShadowCheck by boolean("Shadow", false)
    private val shadowRadiusValue by float("Shadow-Radius", 15F, 1F..50F)

    // Blur Setting
    private val blurCheck by boolean("Blur", true)
    private val blurRadius by float("BlurStrength", 10F, 1F..50F)

    private val notifyDuration by int("NotifyTime(ms)", 3000, 1000..10000)
    private val versionNameUp by text("VersionName", "development") { styles == "Opal" }
    private val ButtonColor by color("Button-Color", Color(20, 150, 180, 255))
    private val ModuleNotify by boolean("Notification", true)
    private val isScaffold by boolean("Scaffold", true)
    private val ScaffoldTheme by color("ScaffoldTheme", Color(65, 130, 225))
    private val maxBlocks by int("maxBlocks", 576, 64..576)

    // Break Progress Settings
    private val breakProgressCheck by boolean("BreakProgress", true)
    private val breakProgressTheme by color("BreakProgressTheme", Color(225, 150, 65))
    
    // Gapple Progress Settings
    private val showGappleProgress by boolean("GappleProgress", true)
    private val gappleProgressTheme by color("GappleProgressTheme", Color(255, 215, 0))
    
    // Chest Settings
    private val ChestTheme by boolean("Chest", true)
    private val ChestRounded by float("ChestRoundRadius", 4F, 0.0F..8.0F)

    // TabList Settings
    private val tabListCheck by boolean("TabList", true)
    private val tabListMaxRows by int("TabList-MaxRows", 20, 5..100)

    private val bpsUpdateInterval by int("BPS-Update-Interval(ms)", 100, 50..500)
    private val versionNameDown = clientVersionText

    // 方块挖掘进度相关变量
    private var breakProgressTarget = 0F
    private var animatedBreakProgress = 0F
    private var animatedBreakProgressVelocity = 0F
    private var lastBreakProgressUpdateTime: Long = 0L

    // Gapple2进度相关变量
    private var gappleProgressTarget = 0F
    private var animatedGappleProgress = 0F
    private var animatedGappleProgressVelocity = 0F
    private var lastGappleProgressUpdateTime: Long = 0L

    // Globals (X, Y, W, H 全套动画变量)
    private var AnimGlobalX = 0F
    private var AnimGlobalY = 0F
    private var AnimGlobalWidth = 100F
    private var AnimGlobalHeight = 28F

    private var VelGlobalX = 0f
    private var VelGlobalY = 0f
    private var VelGlobalWidth = 0f
    private var VelGlobalHeight = 0f

    // --- 新增：涟漪动画相关变量 ---
    private data class SlotRipple(val x: Float, val y: Float, val startTime: Long)
    private val slotRipples = CopyOnWriteArrayList<SlotRipple>()
    private val prevSlotItems = HashMap<Int, ItemStack?>()
    private var lastChestContainerHash: Int = 0

    private const val ITEM_NOTIFY_HEIGHT = 38F
    private const val NORMAL_WATERMARK_HEIGHT = 28F

    private var prevX: Double = 0.0
    private var prevZ: Double = 0.0
    private var AnimatedBps = 0.0
    private var lastBPSUpdateTime: Long = 0L
    private var displayedBPS: Double = 0.0
    private var ProgressBarAnimationWidth = 0F
    private var VelProgressBar = 0F

    private val prevModuleStates = HashMap<Module, Boolean>()
    private val notifications = CopyOnWriteArrayList<ToggleNotification>()
    private var scaledScreen = ScaledResolution(mc)
    private var width = scaledScreen.scaledWidth
    private var height = scaledScreen.scaledHeight
    private var start_y = (height / 20).toFloat()

    // --- TabList Reflection Cache ---
    private var headerFooterCacheTime = 0L
    private var cachedHeader: List<String>? = null
    private var cachedFooter: List<String>? = null

    private fun getSafePing(): Int {
        val player = mc.thePlayer ?: return 0
        return mc.netHandler?.getPlayerInfo(player.uniqueID)?.responseTime ?: 0
    }

    private fun spring(current: Float, target: Float, velocity: Float): Pair<Float, Float> {
        val displacement = target - current
        val force = displacement * animTension
        val drag = velocity * animFriction
        val acceleration = force - drag
        val newVelocity = velocity + acceleration
        val newPosition = current + newVelocity
        return newPosition to newVelocity
    }

    // 反射获取 TabList 的 Header 和 Footer
    private fun getTabListHeaderFooter(): Pair<IChatComponent?, IChatComponent?> {
        try {
            val tabOverlay = mc.ingameGUI.tabList
            val cls = GuiPlayerTabOverlay::class.java

            // 尝试获取 header (field_175256_a)
            var headerField = try { cls.getDeclaredField("header") } catch (e: Exception) { cls.getDeclaredField("field_175256_a") }
            headerField.isAccessible = true
            val header = headerField.get(tabOverlay) as? IChatComponent

            // 尝试获取 footer (field_175255_b)
            var footerField = try { cls.getDeclaredField("footer") } catch (e: Exception) { cls.getDeclaredField("field_175255_b") }
            footerField.isAccessible = true
            val footer = footerField.get(tabOverlay) as? IChatComponent

            return Pair(header, footer)
        } catch (e: Exception) {
            return Pair(null, null)
        }
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer == null || mc.theWorld == null) return@handler

        if (tabListCheck) {
            mc.gameSettings.keyBindPlayerList.pressed = false
        }

        val distanceX = mc.thePlayer.posX - prevX
        val distanceZ = mc.thePlayer.posZ - prevZ
        val currentCalculatedBPS = sqrt(distanceX.pow(2) + distanceZ.pow(2)) * 20.0
        if (System.currentTimeMillis() - lastBPSUpdateTime >= bpsUpdateInterval) {
            displayedBPS = currentCalculatedBPS
            lastBPSUpdateTime = System.currentTimeMillis()
        }
        prevX = mc.thePlayer.posX
        prevZ = mc.thePlayer.posZ

        // 更新方块挖掘进度
        if (mc.playerController != null && mc.thePlayer != null) {
            val currentBreakProgress = mc.playerController.curBlockDamageMP
            if (System.currentTimeMillis() - lastBreakProgressUpdateTime >= 50) {
                breakProgressTarget = currentBreakProgress
                lastBreakProgressUpdateTime = System.currentTimeMillis()
            }
        } else {
            breakProgressTarget = 0F
        }

        // 更新Gapple2进度 - 使用修复后的函数
        val gappleModule = ModuleManager.getModule("Gapple2")
        if (gappleModule != null && gappleModule.state) {
            val currentProgress = getGappleEatingProgress()
            if (System.currentTimeMillis() - lastGappleProgressUpdateTime >= 50) {
                gappleProgressTarget = currentProgress
                lastGappleProgressUpdateTime = System.currentTimeMillis()
            }
        } else {
            gappleProgressTarget = 0F
        }

        if (ModuleNotify) {
            for (module in ModuleManager) {
                if (!prevModuleStates.containsKey(module)) {
                    prevModuleStates[module] = module.state
                    continue
                }
                val prevState = prevModuleStates[module]!!
                val currentState = module.state
                if (prevState != currentState) {
                    prevModuleStates[module] = currentState

                    val titleText = "Module Toggled"
                    val modName = "${module.name}"
                    val stateText = if (currentState) "§l§aEnabled" else "§l§cDisabled"
                    val message = "§l$modName§r §fhas been $stateText§r §f!"

                    showToggleNotification(titleText, message, currentState, module.name)
                }
            }
        }
    }

    // 修复：Gapple2进度获取函数 - 不再使用反射访问私有字段
    private fun getGappleEatingProgress(): Float {
        val gappleModule = ModuleManager.getModule("Gapple2") ?: return 0f
        if (!gappleModule.state) return 0f
        
        // 尝试直接调用Gapple2模块的公共方法
        return try {
            // 方法1：使用类型安全转换和公共方法
            val gapple2Class = Class.forName("net.ccbluex.liquidbounce.features.module.modules.player.Gapple2")
            val getProgressMethod = gapple2Class.getDeclaredMethod("getEatingProgress")
            getProgressMethod.isAccessible = true
            val progress = getProgressMethod.invoke(gappleModule)
            progress as? Float ?: 0f
        } catch (e: ClassNotFoundException) {
            // 方法2：尝试直接调用（如果Gapple2类在同一个包中）
            try {
                val getProgressMethod = gappleModule::class.java.getDeclaredMethod("getEatingProgress")
                getProgressMethod.isAccessible = true
                (getProgressMethod.invoke(gappleModule) as? Float) ?: 0f
            } catch (e2: Exception) {
                // 方法3：使用反射访问字段（备用方案）
                try {
                    val isEatingField = gappleModule::class.java.getDeclaredField("isEating")
                    isEatingField.isAccessible = true
                    val isEating = isEatingField.getBoolean(gappleModule)
                    
                    if (!isEating) return 0f
                    
                    val ticksField = gappleModule::class.java.getDeclaredField("ticks")
                    ticksField.isAccessible = true
                    val ticks = ticksField.getInt(gappleModule)
                    
                    val cField = gappleModule::class.java.getDeclaredField("c")
                    cField.isAccessible = true
                    val c = cField.get(gappleModule) as Int
                    
                    (ticks.toFloat() / c.toFloat()).coerceIn(0f, 1f)
                } catch (e3: Exception) {
                    0f
                }
            }
        } catch (e: Exception) {
            0f
        }
    }

    val onScreen = handler<ScreenEvent>(always = true) { event ->
        if (mc.theWorld == null || mc.thePlayer == null) return@handler
    }

    val onRender2D = handler<Render2DEvent> {
        glPushMatrix()
        glEnable(GL_BLEND)
        glDisable(GL_TEXTURE_2D)
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glEnable(GL_LINE_SMOOTH)
        glHint(GL_LINE_SMOOTH_HINT, GL_NICEST)

        updateNotifications()
        scaledScreen = ScaledResolution(mc)
        width = scaledScreen.scaledWidth
        height = scaledScreen.scaledHeight
        start_y = (height / 20).toFloat()

        val scaffoldModule = ModuleManager.getModule("Scaffold")
        val scaffoldModule2 = ModuleManager.getModule("Scaffold2")

        val isChestOpen = mc.currentScreen is GuiChest && ChestTheme
        val chestSlots = if (isChestOpen) {
            (mc.currentScreen as GuiChest).inventorySlots?.inventorySlots?.filter { it.inventory != mc.thePlayer?.inventory } ?: emptyList()
        } else emptyList()

        // --- 核心修复：箱子状态与涟漪数据重置 ---
        if (!isChestOpen) {
            // 箱子关闭时清空数据
            prevSlotItems.clear()
            slotRipples.clear()
            lastChestContainerHash = 0
        } else {
            // 获取当前容器 ID，检测是否切换了箱子
            val currentContainerId = (mc.currentScreen as GuiChest).inventorySlots.windowId
            if (currentContainerId != lastChestContainerHash) {
                prevSlotItems.clear()
                slotRipples.clear()
                lastChestContainerHash = currentContainerId
            }
        }
        // ------------------------------------

        val tabKey = mc.gameSettings.keyBindPlayerList.keyCode
        val isTabKeyDown = if (tabKey > 0) Keyboard.isKeyDown(tabKey) else false
        val showTabList = tabListCheck && isTabKeyDown && mc.netHandler != null

        val playerList = if (showTabList) {
            mc.netHandler.playerInfoMap
                .filter { it.gameProfile.name != null }
                .sortedWith(compareBy({ it.gameProfile.name }))
        } else emptyList()

        // 更新方块挖掘进度动画
        val (nextBreakProgress, vBreakProgress) = spring(animatedBreakProgress, breakProgressTarget, animatedBreakProgressVelocity)
        animatedBreakProgress = nextBreakProgress.coerceIn(0F, 1F)
        animatedBreakProgressVelocity = vBreakProgress

        // 更新Gapple2进度动画
        val (nextGappleProgress, vGappleProgress) = spring(animatedGappleProgress, gappleProgressTarget, animatedGappleProgressVelocity)
        animatedGappleProgress = nextGappleProgress.coerceIn(0F, 1F)
        animatedGappleProgressVelocity = vGappleProgress

        var targetWidth = 0F
        var targetHeight = 0F
        var targetX = 0F
        var targetY = start_y
        var renderMode = "NONE"

        var headerLines: List<String> = emptyList()
        var footerLines: List<String> = emptyList()

        if (chestSlots.isNotEmpty()) {
            renderMode = "CHEST"
            val columns = 9
            val rows = (chestSlots.size + 8) / 9
            val padding = 8F
            val slotSize = 16F
            targetWidth = columns * slotSize + padding * 2
            targetHeight = rows * slotSize + padding * 2
            targetX = (width - targetWidth) / 2
            targetY = start_y.coerceIn(5f, height - targetHeight - 5f)

        } else if (showTabList && playerList.isNotEmpty()) {
            renderMode = "TABLIST"
            if (System.currentTimeMillis() - headerFooterCacheTime > 500) {
                val (h, f) = getTabListHeaderFooter()
                cachedHeader = h?.formattedText?.split("\n")
                cachedFooter = f?.formattedText?.split("\n")
                headerFooterCacheTime = System.currentTimeMillis()
            }
            headerLines = cachedHeader ?: emptyList()
            footerLines = cachedFooter ?: emptyList()

            val playerCount = playerList.size
            val maxRows = tabListMaxRows
            val columns = ceil(playerCount.toDouble() / maxRows.toDouble()).toInt()
            val headSize = 10F
            val outerPadding = 8F
            val padding = 6F
            val spacing = 4F
            var maxNameWidth = 50F

            playerList.forEach { it ->
                val fullName = mc.ingameGUI.tabList.getPlayerName(it)
                val w = Fonts.fontRegular35.getStringWidth(fullName).toFloat()
                if (w > maxNameWidth) maxNameWidth = w
            }
            val columnWidth = padding + headSize + spacing + maxNameWidth + spacing + 25F + padding
            val playersWidth = columns * columnWidth

            var maxHeaderW = 0f
            headerLines.forEach { maxHeaderW = max(maxHeaderW, Fonts.fontRegular35.getStringWidth(it).toFloat()) }
            var maxFooterW = 0f
            footerLines.forEach { maxFooterW = max(maxFooterW, Fonts.fontRegular35.getStringWidth(it).toFloat()) }

            targetWidth = max(playersWidth, max(maxHeaderW, maxFooterW) + padding * 2)

            val lineH = Fonts.fontRegular35.FONT_HEIGHT + 2
            val headerHeight = if(headerLines.isNotEmpty()) headerLines.size * lineH + 2 else 0
            val footerHeight = if(footerLines.isNotEmpty()) footerLines.size * lineH + 2 else 0

            val actualRows = if (columns == 1) playerCount else maxRows
            val playersBlockHeight = actualRows * (headSize + spacing) - spacing

            targetHeight = outerPadding +
                    headerHeight.toFloat() +
                    (if(headerHeight > 0) 2F else 0F) +
                    playersBlockHeight +
                    (if(footerHeight > 0) 2F else 0F) +
                    footerHeight.toFloat() +
                    outerPadding

            targetX = (width - targetWidth) / 2
            targetY = start_y

        } else if (scaffoldModule?.state == true ||  scaffoldModule2?.state == true && isScaffold) {
            renderMode = "SCAFFOLD"
            targetWidth = 190F
            targetHeight = 58F
            targetX = (width - targetWidth) / 2
        } else if (notifications.isNotEmpty() && ModuleNotify && styles == "New Opai") {
            renderMode = "NOTIFY_STACK"
            val borderInfo = calcMaxNotificationWidth()
            targetWidth = borderInfo.coerceAtLeast(180F)
            targetHeight = (notifications.size * ITEM_NOTIFY_HEIGHT).toFloat()
            targetX = (width - targetWidth) / 2
        } else if (breakProgressCheck && animatedBreakProgress > 0.01f) {
            renderMode = "BREAK_PROGRESS"
            targetWidth = 190F
            targetHeight = 58F
            targetX = (width - targetWidth) / 2
            targetY = start_y
        } else if (showGappleProgress && animatedGappleProgress > 0.01f && ModuleManager.getModule("Gapple2")?.state == true) {
            renderMode = "GAPPLE_PROGRESS"
            targetWidth = 190F
            targetHeight = 58F
            targetX = (width - targetWidth) / 2
            targetY = start_y
        } else {
            when (styles) {
                "New Opai" -> {
                    renderMode = "NORMAL_OPAI"
                    val info = calcNormal3Info()
                    targetWidth = info.width
                    targetHeight = NORMAL_WATERMARK_HEIGHT
                    targetX = (width - targetWidth) / 2
                }
                "Normal" -> {
                    drawNormal()
                    if (notifications.isNotEmpty() && ModuleNotify) drawOldStyleNotifications()
                }
                "Opal" -> {
                    drawNormal2()
                    if (notifications.isNotEmpty() && ModuleNotify) drawOldStyleNotifications(startYOffset = 32F)
                }
            }
        }

        if (renderMode != "NONE") {
            val (nextW, vW) = spring(AnimGlobalWidth, targetWidth, VelGlobalWidth)
            AnimGlobalWidth = nextW.coerceAtLeast(0F)
            VelGlobalWidth = vW

            val (nextH, vH) = spring(AnimGlobalHeight, targetHeight, VelGlobalHeight)
            AnimGlobalHeight = nextH.coerceAtLeast(0F)
            VelGlobalHeight = vH

            val (nextX, vX) = spring(AnimGlobalX, targetX, VelGlobalX)
            AnimGlobalX = nextX
            VelGlobalX = vX

            val (nextY, vY) = spring(AnimGlobalY, targetY, VelGlobalY)
            AnimGlobalY = nextY
            VelGlobalY = vY

            val currentRadius = if (AnimGlobalHeight > 30F) {
                if (renderMode == "CHEST" || renderMode == "TABLIST") ChestRounded else 8F
            } else AnimGlobalHeight / 2F

            val drawX = AnimGlobalX
            val drawY = AnimGlobalY
            val drawW = AnimGlobalWidth
            val drawH = AnimGlobalHeight

            try {
                EmbeddedStencil.checkSetupFBO(mc.framebuffer)
                EmbeddedStencil.write(false)
                drawRoundedRect(drawX, drawY, drawX + drawW, drawY + drawH, Color.WHITE.rgb, currentRadius)

                EmbeddedStencil.erase(false)
                ShowShadow(drawX, drawY, drawW, drawH)

                EmbeddedStencil.erase(true)
                if (blurCheck) {
                    GlStateManager.pushMatrix()
                    InternalBlurShader.blurArea(drawX, drawY, drawW, drawH, blurRadius)
                    GlStateManager.popMatrix()
                }

                drawRoundedBorderRect(
                    drawX, drawY,
                    drawX + drawW, drawY + drawH,
                    0.1F,
                    Color(0, 0, 0, BackgroundAlpha).rgb,
                    Color(0, 0, 0, BackgroundAlpha).rgb,
                    currentRadius
                )

                EmbeddedStencil.dispose()

            } catch (e: Exception) {
                ShowShadow(drawX, drawY, drawW, drawH)
                drawRoundedBorderRect(drawX, drawY, drawX + drawW, drawY + drawH, 0.1F, Color(0,0,0,BackgroundAlpha).rgb, Color(0,0,0,BackgroundAlpha).rgb, currentRadius)
            }

            when (renderMode) {
                "SCAFFOLD" -> renderScaffoldContent(drawX, drawY, drawW, drawH)
                "NOTIFY_STACK" -> renderNotificationStack(drawX, drawY, drawW, drawH)
                "NORMAL_OPAI" -> renderNormal3Content(drawX, drawY, drawW, drawH)
                "CHEST" -> renderChestContent(drawX, drawY, drawW, drawH, chestSlots)
                "TABLIST" -> renderTabListContent(drawX, drawY, drawW, drawH, playerList, headerLines, footerLines)
                "BREAK_PROGRESS" -> renderBreakProgressContent(drawX, drawY, drawW, drawH)
                "GAPPLE_PROGRESS" -> renderGappleProgressContent(drawX, drawY, drawW, drawH)
            }
        }

        glHint(GL_LINE_SMOOTH_HINT, GL_DONT_CARE)
        glDisable(GL_LINE_SMOOTH)
        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glPopMatrix()
    }

    // --- 新增：Gapple2进度绘制函数 ---
    private fun renderGappleProgressContent(x: Float, y: Float, w: Float, h: Float) {
        val percentage = animatedGappleProgress.coerceIn(0f, 1f)
        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(gappleProgressTheme.red, gappleProgressTheme.green, gappleProgressTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        
        // 尝试绘制苹果图标
        try {
            val appleImgSize = 24
            drawImage(ResourceLocation("liquidbounce/watermark_images/apple.png"), 
                    (iconBgX + (iconSize - appleImgSize) / 2).toInt(), 
                    (iconBgY + (iconSize - appleImgSize) / 2 + 1).toInt(), 
                    appleImgSize, appleImgSize, Color.WHITE)
        } catch (e: Exception) {
            // 如果图标不存在，绘制文字"EAT"
            Fonts.fontGoogleSans40.drawCenteredString("EAT", iconBgX + iconSize / 2, iconBgY + iconSize / 2 - 8, Color.WHITE.rgb)
        }

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontGoogleSans40.drawString("Eating Gapple", textX, titleY, Color.WHITE.rgb)
        val percentText = String.format("%.1f", percentage * 100) + "%"
        Fonts.fontRegular40.drawString(percentText, textX, titleY + Fonts.fontGoogleSans45.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val currentBarWidth = maxBarWidth * percentage
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color(gappleProgressTheme.red, gappleProgressTheme.green, gappleProgressTheme.blue, 255)
        drawRoundedRect(x + padding, barY, x + padding + currentBarWidth, barY + barHeight, lighter.rgb, 3F)
    }

    // --- 方块挖掘进度绘制函数 ---
    private fun renderBreakProgressContent(x: Float, y: Float, w: Float, h: Float) {
        val percentage = animatedBreakProgress.coerceIn(0f, 1f)
        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(breakProgressTheme.red, breakProgressTheme.green, breakProgressTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        val bedImgSize = 24
        drawImage(ResourceLocation("liquidbounce/watermark_images/bed.png"), (iconBgX + (iconSize - bedImgSize) / 2).toInt(), (iconBgY + (iconSize - bedImgSize) / 2 + 1).toInt(), bedImgSize, bedImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontGoogleSans40.drawString("Break Progress", textX, titleY, Color.WHITE.rgb)
        val percentText = String.format("%.1f", percentage * 100) + "%"
        Fonts.fontRegular40.drawString(percentText, textX, titleY + Fonts.fontGoogleSans45.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val currentBarWidth = maxBarWidth * percentage
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color(breakProgressTheme.red, breakProgressTheme.green, breakProgressTheme.blue, 255)
        drawRoundedRect(x + padding, barY, x + padding + currentBarWidth, barY + barHeight, lighter.rgb, 3F)
    }

    // 绘制圆形工具函数
    private fun drawCircle(x: Float, y: Float, radius: Float, color: Color) {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.disableTexture2D()
        GlStateManager.tryBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ZERO)
        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)

        glBegin(GL_POLYGON)
        for (i in 0..360 step 10) {
            val theta = i * Math.PI / 180
            glVertex2d(x + radius * cos(theta), y + radius * sin(theta))
        }
        glEnd()

        GlStateManager.enableTexture2D()
        GlStateManager.disableBlend()
        GlStateManager.popMatrix()
    }

    private fun renderChestContent(x: Float, y: Float, w: Float, h: Float, slots: List<Slot>) {
        val padding = 8F
        val slotSize = 16

        // 涟漪参数
        val rippleDuration = 600L
        val maxRadius = 18F

        enableGUIStandardItemLighting()
        try {
            slots.forEachIndexed { index, slot ->
                val stack = slot.stack
                val col = index % 9
                val row = index / 9
                val itemX = (x + padding + col * slotSize).toInt()
                val itemY = (y + padding + row * slotSize).toInt()

                val prevStack = prevSlotItems[index]

                // --- 检测变更逻辑 ---
                // 只有当 map 包含 index 时（说明至少是第二帧），才开始检测变化，防止打开箱子第一帧全屏涟漪
                if (prevSlotItems.containsKey(index)) {
                    val isChanged = when {
                        stack == null && prevStack == null -> false
                        stack == null || prevStack == null -> true
                        else -> !ItemStack.areItemStacksEqual(stack, prevStack) || stack.stackSize != prevStack.stackSize
                    }

                    if (isChanged) {
                        // 物品变了 -> 添加涟漪 (中心点)
                        slotRipples.add(SlotRipple((itemX + 8).toFloat(), (itemY + 8).toFloat(), System.currentTimeMillis()))
                    }
                }

                // 记录当前帧状态
                prevSlotItems[index] = stack?.copy()

                // 绘制物品
                if (stack != null) {
                    if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)
                    mc.renderItem.renderItemAndEffectIntoGUI(stack, itemX, itemY)
                    mc.renderItem.renderItemOverlays(mc.fontRendererObj, stack, itemX, itemY)
                    if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)
                }
            }

            // --- 绘制涟漪层 ---
            disableStandardItemLighting()
            GlStateManager.disableDepth()

            val currentTime = System.currentTimeMillis()
            val iterator = slotRipples.iterator()

            while (iterator.hasNext()) {
                val ripple = iterator.next()
                val timeAlive = currentTime - ripple.startTime

                if (timeAlive > rippleDuration) {
                    slotRipples.remove(ripple)
                } else {
                    // 动画进度 0 -> 1
                    val progress = timeAlive.toFloat() / rippleDuration.toFloat()
                    // 缓动
                    val ease = 1f - (1f - progress).pow(3)
                    val radius = maxRadius * ease
                    // 淡出 (Alpha 180 -> 0)
                    val alpha = (180 * (1f - progress)).toInt().coerceIn(0, 255)

                    if (alpha > 0) {
                        drawCircle(ripple.x, ripple.y, radius, Color(255, 255, 255, alpha))
                    }
                }
            }
            GlStateManager.enableDepth()

        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            disableStandardItemLighting()
            GlStateManager.enableAlpha()
            GlStateManager.disableBlend()
            GlStateManager.disableLighting()
        }
    }

    private fun renderTabListContent(x: Float, y: Float, w: Float, h: Float, players: List<NetworkPlayerInfo>, header: List<String>, footer: List<String>) {
        val maxRows = tabListMaxRows
        val outerPadding = 8F

        var currentY = y + outerPadding
        val centerX = x + w / 2F

        if (header.isNotEmpty()) {
            for (line in header) {
                Fonts.fontRegular35.drawCenteredString(line, centerX, currentY, Color.WHITE.rgb)
                currentY += Fonts.fontRegular35.FONT_HEIGHT + 2
            }
            currentY += 2
        }

        if (players.isNotEmpty()) {
            var maxNameWidth = 50F
            players.forEach { it ->
                val fullName = mc.ingameGUI.tabList.getPlayerName(it)
                val wName = Fonts.fontRegular35.getStringWidth(fullName).toFloat()
                if (wName > maxNameWidth) maxNameWidth = wName
            }
            val headSize = 10F
            val padding = 6F
            val spacing = 4F
            val itemHeight = headSize + spacing
            val colWidth = padding + headSize + spacing + maxNameWidth + spacing + 25F + padding
            val totalCols = ceil(players.size.toDouble() / maxRows.toDouble()).toInt()

            val playersTotalWidth = totalCols * colWidth
            var startX = centerX - playersTotalWidth / 2F

            val columnY = currentY

            for (i in players.indices) {
                val player = players[i]

                if (i > 0 && i % maxRows == 0) {
                    startX += colWidth
                    currentY = columnY
                }

                val rowX = startX + padding

                mc.textureManager.bindTexture(player.locationSkin)
                glColor4f(1f, 1f, 1f, 1f)
                Gui.drawScaledCustomSizeModalRect(rowX.toInt(), currentY.toInt(), 8f, 8f, 8, 8, headSize.toInt(), headSize.toInt(), 64f, 64f)

                val fullName = mc.ingameGUI.tabList.getPlayerName(player)
                Fonts.fontRegular35.drawString(fullName, rowX + headSize + spacing, currentY + 1.5F, Color.WHITE.rgb)

                val ping = player.responseTime
                val pingColor = when {
                    ping < 0 -> Color(50, 50, 50)
                    ping < 100 -> Color(100, 255, 100)
                    ping < 200 -> Color(255, 200, 50)
                    else -> Color(255, 80, 80)
                }
                val pingText = "${ping}ms"
                val pingW = Fonts.fontRegular35.getStringWidth(pingText).toFloat()
                Fonts.fontRegular35.drawString(pingText, startX + colWidth - padding - pingW, currentY + 1.5F, pingColor.rgb)

                currentY += itemHeight
            }

            val actualRows = if (totalCols == 1) players.size else maxRows
            currentY = columnY + actualRows * itemHeight + 2
        }

        if (footer.isNotEmpty()) {
            for (line in footer) {
                Fonts.fontRegular35.drawCenteredString(line, centerX, currentY, Color.WHITE.rgb)
                currentY += Fonts.fontRegular35.FONT_HEIGHT + 2
            }
        }
    }

    private fun renderScaffoldContent(x: Float, y: Float, w: Float, h: Float) {
        val totalBlockCount = mc.thePlayer.inventory.mainInventory.filterNotNull().filter { it.item is ItemBlock }.sumOf { it.stackSize }
        val percentage = (totalBlockCount.toFloat() / maxBlocks.toFloat()).coerceIn(0f, 1f)
        val targetBPS = displayedBPS
        AnimatedBps += (targetBPS - AnimatedBps) * 0.15 * (Minecraft.getDebugFPS() / 20.0).coerceIn(0.1, 2.0)

        val padding = 8F
        val cornerRadius = 6F
        val iconSize = 32F
        val iconBgX = x + padding
        val iconBgY = y + padding
        val themeColor = Color(ScaffoldTheme.red, ScaffoldTheme.green, ScaffoldTheme.blue, 200)
        drawRoundedRect(iconBgX, iconBgY, iconBgX + iconSize, iconBgY + iconSize, themeColor.rgb, cornerRadius - 1)
        val blockImgSize = 24
        drawImage(ResourceLocation("liquidbounce/watermark_images/block.png"), (iconBgX + (iconSize - blockImgSize) / 2).toInt(), (iconBgY + (iconSize - blockImgSize) / 2 + 1).toInt(), blockImgSize, blockImgSize, Color.WHITE)

        val textX = iconBgX + iconSize + 8F
        val titleY = y + padding + 2F
        Fonts.fontGoogleSans40.drawString("Scaffold Toggled", textX, titleY, Color.WHITE.rgb)
        val bpsText = String.format("%.2f", if(AnimatedBps < 0.01) 0.0 else AnimatedBps)
        Fonts.fontRegular40.drawString("$totalBlockCount blocks - $bpsText block/s", textX, titleY + Fonts.fontGoogleSans45.FONT_HEIGHT + 2F, Color(200, 200, 200).rgb)

        val barHeight = 8F
        val barY = y + h - barHeight - padding
        val maxBarWidth = w - (padding * 2)
        val targetBarWidth = maxBarWidth * percentage
        val (nextBarW, vBar) = spring(ProgressBarAnimationWidth, targetBarWidth, VelProgressBar)
        ProgressBarAnimationWidth = nextBarW.coerceIn(0F, maxBarWidth)
        VelProgressBar = vBar
        drawRoundedRect(x + padding, barY, x + padding + maxBarWidth, barY + barHeight, Color(60, 60, 70, 180).rgb, 3F)
        val lighter = Color((ScaffoldTheme.red + 50).coerceAtMost(255), (ScaffoldTheme.green + 50).coerceAtMost(255), (ScaffoldTheme.blue + 50).coerceAtMost(255), 255)
        drawRoundedRect(x + padding, barY, x + padding + ProgressBarAnimationWidth, barY + barHeight, lighter.rgb, 3F)
    }

    private fun renderNotificationStack(x: Float, y: Float, w: Float, h: Float) {
        var currentYOffset = 0F
        val centerXOffset = 10F
        for (notify in notifications) {
            val rowY = y + currentYOffset
            notify.draw(x + centerXOffset, rowY)
            currentYOffset += ITEM_NOTIFY_HEIGHT
        }
    }

    private fun renderNormal3Content(x: Float, y: Float, w: Float, h: Float) {
        val info = calcNormal3Info()
        val textBaseY = y + (h - Fonts.fontGoogleSans40.FONT_HEIGHT) / 2 + Fonts.fontGoogleSans40.FONT_HEIGHT - 8
        val iconSize = 15F
        val iconY = y + (h - iconSize) / 2
        var cx = x + info.padding

        val colorRGB = Color(ColorA_, ColorB_, ColorC_, 255)
        drawImage(ResourceLocation("liquidbounce/watermark_images/logo_icon.png"), cx, iconY, 15, 15, colorRGB)
        cx += 15F + info.elementSpacing
        Fonts.fontGoogleSans40.drawString(ClientName, cx, textBaseY, colorRGB.rgb)
        cx += info.clientNameWidth + info.dotSpacing
        drawCenteredDot(cx, textBaseY)
        cx += 4F
        drawImage(ResourceLocation("liquidbounce/watermark_images/user.png"), cx, iconY, 15, 15, Color.WHITE)
        cx += 15F + info.elementSpacing
        Fonts.fontGoogleSans40.drawString(info.username, cx - 1F, textBaseY, Color.WHITE.rgb)
        cx += info.usernameWidth + info.dotSpacing
        drawCenteredDot(cx, textBaseY)
        cx += 4F
        drawImage(ResourceLocation("liquidbounce/watermark_images/ms.png"), cx, iconY, 15, 15, Color.GREEN)
        cx += 15F + info.elementSpacing
        Fonts.fontGoogleSans40.drawString(info.pingStr, cx, textBaseY, Color.GREEN.rgb)
        cx += info.pingTextWidth
        Fonts.fontGoogleSans40.drawString("  to  ", cx, textBaseY, Color.WHITE.rgb)
        cx += info.toTextWidth
        Fonts.fontGoogleSans40.drawString(info.ipStr, cx, textBaseY, Color.WHITE.rgb)
        cx += info.serverIpWidth + info.dotSpacing
        drawCenteredDot(cx - 1, textBaseY)
        cx += 3F
        drawImage(ResourceLocation("liquidbounce/watermark_images/fps.png"), cx, iconY, 15, 15, Color.WHITE)
        cx += 15F + info.elementSpacing
        Fonts.fontGoogleSans40.drawString(info.fpsStr, cx, textBaseY, Color.WHITE.rgb)
    }

    data class Normal3Info(
        val width: Float,
        val padding: Float = 13F,
        val elementSpacing: Float = 8F,
        val dotSpacing: Float = 13F,
        val username: String,
        val clientNameWidth: Float,
        val usernameWidth: Float,
        val pingStr: String,
        val pingTextWidth: Float,
        val toTextWidth: Float,
        val ipStr: String,
        val serverIpWidth: Float,
        val fpsStr: String,
        val fpsTextWidth: Float
    )

    private fun calcNormal3Info(): Normal3Info {
        val username = mc.session?.username ?: "Unknown"
        val fps = Minecraft.getDebugFPS()
        val pings = getSafePing()
        val ipStr = if (customip) ip else ServerUtils.remoteIp ?: "SinglePlayer"
        val clientNameWidth = Fonts.fontGoogleSans40.getStringWidth(ClientName).toFloat()
        val usernameWidth = Fonts.fontGoogleSans40.getStringWidth(username).toFloat() - 1f
        val pingStr = "${pings}ms"
        val pingTextWidth = Fonts.fontGoogleSans40.getStringWidth(pingStr).toFloat()
        val toTextWidth = Fonts.fontGoogleSans40.getStringWidth("  to  ").toFloat()
        val serverIpWidth = Fonts.fontGoogleSans40.getStringWidth(ipStr).toFloat()
        val fpsStr = "${fps}fps"
        val fpsTextWidth = Fonts.fontGoogleSans40.getStringWidth(fpsStr).toFloat()
        val padding = 13F
        val icon = 15F
        val space = 8F
        val dot = 13F
        val dotW = 4F
        val w = padding + icon + space + clientNameWidth + dot + dotW +
                icon + space + usernameWidth + dot + dotW +
                icon + space + pingTextWidth + toTextWidth + serverIpWidth + dot + 3F +
                icon + space + fpsTextWidth + padding
        return Normal3Info(w, username=username, clientNameWidth=clientNameWidth, usernameWidth=usernameWidth,
            pingStr=pingStr, pingTextWidth=pingTextWidth, toTextWidth=toTextWidth, ipStr=ipStr, serverIpWidth=serverIpWidth,
            fpsStr=fpsStr, fpsTextWidth=fpsTextWidth)
    }

    private fun calcMaxNotificationWidth(): Float {
        if (notifications.isEmpty()) return 0F
        var maxWidth = 0f
        val fixedElementWidth = 30F + 15F + 30F
        for (notif in notifications) {
            val titleWidth = Fonts.fontGoogleSans40.getStringWidth(notif.title).toFloat()
            val descWidth = Fonts.fontRegular35.getStringWidth(notif.message).toFloat()
            val textWidth = max(titleWidth, descWidth)
            val totalWidth = fixedElementWidth + textWidth
            maxWidth = max(maxWidth, totalWidth)
        }
        return maxWidth
    }

    private fun drawOldStyleNotifications(startYOffset: Float = 0F) {
        var currentY = start_y + startYOffset
        val padding = 3F
        for(notify in notifications) {
            val tW = Fonts.fontGoogleSans40.getStringWidth(notify.title)
            val dW = Fonts.fontRegular35.getStringWidth(notify.message)
            val w = 35F + max(tW, dW) + 20F
            val x = (width - w)/2
            drawRoundedBorderRect(x, currentY, x+w, currentY+ITEM_NOTIFY_HEIGHT, 0.2F, Color(0,0,0,BackgroundAlpha).rgb, Color(0,0,0,BackgroundAlpha).rgb, 10F)
            notify.draw(x + padding, currentY + 5F)
            currentY += ITEM_NOTIFY_HEIGHT + 2F
        }
    }

    private fun drawNormal() {
        val username = mc.session.username
        val fps = Minecraft.getDebugFPS()
        val pings = getSafePing()
        val colorRGB = Color(ColorA_, ColorB_, ColorC_, 255)
        val text = " | $username | ${fps}fps | ${pings}ms"
        val mainText = ClientName
        val h = 38F
        val wCalc = 20F + 18F + 5F + Fonts.fontGoogleSans40.getStringWidth(mainText + text) + 10F
        val x = (width - wCalc)/2
        val y = start_y
        val (nX, _) = spring(AnimGlobalX, x, VelGlobalX)
        AnimGlobalX = nX
        ShowShadow(x, y, wCalc, h)
        drawRoundedBorderRect(x, y, x+wCalc, y+h, 0.5F, Color(10,10,10,BackgroundAlpha).rgb, Color(30,30,30,BackgroundAlpha).rgb, h/2)
        drawImage(ResourceLocation("liquidbounce/logo_icon.png"), (x+5).toInt(), (y + (h-18)/2).toInt(), 18, 18, colorRGB)
        Fonts.fontGoogleSans40.drawString(mainText, x + 28, y + (h-9)/2+1, colorRGB.rgb)
        Fonts.fontGoogleSans40.drawString(text, x + 28 + Fonts.fontGoogleSans40.getStringWidth(mainText), y + (h-9)/2+1, -1)
    }

    private fun drawNormal2() {}
    
    private fun drawCenteredDot(x: Float, textBaseY: Float) {
        val dotY = textBaseY - Fonts.fontGoogleSans40.FONT_HEIGHT / 2 + 3F
        Fonts.fontGoogleSans40.drawString("·", x - 3f, dotY, Color(180, 180, 180, 255).rgb)
    }

    private fun ShowShadow(x: Float, y: Float, w: Float, h: Float) {
        if (ShadowCheck) GlowUtils.drawGlow(x, y, w, h, shadowRadiusValue.toInt(), Color(0, 0, 0, 120))
    }

    fun drawToggleButton(StartX: Float, StartY: Float, ContainerH: Float, ModuleState: Boolean, animationState: SwitchAnimationState) {
        val btnH = 19F
        val btnW = 30F
        val margin = 3F
        val radius = btnH / 2
        val btnStartY = StartY + (ITEM_NOTIFY_HEIGHT - btnH) / 2
        animationState.updateState(ModuleState)
        val anim = animationState.getOutput()
        val trackColor = if (ModuleState) Color(ButtonColor.red, ButtonColor.green, ButtonColor.blue, 255) else Color(45, 45, 45, 255)
        drawRoundedBorderRect(StartX, btnStartY, StartX + btnW, btnStartY + btnH, 0.1f, trackColor.rgb, trackColor.rgb, radius)
        val knobSize = btnH - margin * 2
        val knobX = StartX + margin + (btnW - margin*2 - knobSize) * anim.toFloat()
        val knobColor = if (ModuleState) Color.WHITE.rgb else Color(100, 100, 100, 255).rgb
        drawRoundedBorderRect(knobX, btnStartY + margin, knobX + knobSize, btnStartY + margin + knobSize, 0.1f, knobColor, knobColor, knobSize / 2)
    }

    fun drawToggleText(StartX: Float, StartY: Float, TextBar: Pair<String, String>, ContainerH: Float) {
        val titleH = 9F
        val textStartX = StartX + 30F + 8F
        val center = StartY + ITEM_NOTIFY_HEIGHT / 2
        Fonts.fontGoogleSans40.drawString(TextBar.first, textStartX, center - titleH + 1F, Color.WHITE.rgb)
        Fonts.fontRegular35.drawString(TextBar.second, textStartX, center + 3F, Color.WHITE.rgb)
    }

    enum class Direction { FORWARDS, BACKWARDS }
    class EaseOutExpo(private val duration: Long, private val end: Double) {
        private var start = 0.0
        private var startTime = System.currentTimeMillis()
        private var direction = Direction.FORWARDS
        fun setDirection(dir: Direction) { if (this.direction != dir) { this.direction = dir; startTime = System.currentTimeMillis(); start = getOutput() } }
        fun getOutput(): Double {
            val progress = (System.currentTimeMillis() - startTime).toDouble() / duration
            val result = when (direction) {
                Direction.FORWARDS -> if (progress >= 1.0) end else (-2.0.pow(-10 * progress) + 1) * end
                Direction.BACKWARDS -> if (progress >= 1.0) 0.0 else (2.0.pow(-10 * progress) * end)
            }
            return result.coerceIn(0.0, end)
        }
    }
    class SwitchAnimationState {
        private val animation = EaseOutExpo(300, 1.0)
        fun updateState(state: Boolean) = animation.setDirection(if (state) Direction.FORWARDS else Direction.BACKWARDS)
        fun getOutput() = animation.getOutput()
    }

    private abstract class Notification(val id: String = UUID.randomUUID().toString(), var title: String, var message: String, var createTime: Long = System.currentTimeMillis(), var duration: Long = 3000L) {
        var isMarkedForDelete = false
        abstract fun draw(x: Float, y: Float)
        open fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) { this.message = newMsg; this.createTime = System.currentTimeMillis(); this.duration = newDuration }
        fun getHeight() = ITEM_NOTIFY_HEIGHT
        fun update() { if (System.currentTimeMillis() > createTime + duration) isMarkedForDelete = true }
    }
    private class ToggleNotification(t: String, m: String, d: Long, var enabled: Boolean, val moduleName: String) : Notification(title=t, message=m, duration=d) {
        val anim = SwitchAnimationState()
        init { anim.updateState(enabled) }
        override fun updateState(newMsg: String, newEnable: Boolean, newDuration: Long) { super.updateState(newMsg, newEnable, newDuration); this.enabled = newEnable; anim.updateState(newEnable) }
        override fun draw(x: Float, y: Float) { drawToggleButton(x, y, 0F, enabled, anim); drawToggleText(x, y, Pair(title, message), 0F) }
    }

    fun showToggleNotification(title: String, message: String, enabled: Boolean, moduleName: String) {
        val existing = notifications.find { it.moduleName == moduleName }
        val duration = notifyDuration.toLong()
        if (existing != null) existing.updateState(message, enabled, duration)
        else notifications.add(ToggleNotification(title, message, duration, enabled, moduleName))
    }
    private fun updateNotifications() { notifications.forEach { it.update() }; notifications.removeAll { it.isMarkedForDelete } }

    object EmbeddedStencil {
        fun checkSetupFBO(framebuffer: Framebuffer?) {
            if (framebuffer != null && framebuffer.depthBuffer > -1) {
                setupFBO(framebuffer)
                framebuffer.depthBuffer = -1
            }
        }
        fun setupFBO(framebuffer: Framebuffer) {
            EXTFramebufferObject.glDeleteRenderbuffersEXT(framebuffer.depthBuffer)
            val stencilDepthBufferID = EXTFramebufferObject.glGenRenderbuffersEXT()
            EXTFramebufferObject.glBindRenderbufferEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
            EXTFramebufferObject.glRenderbufferStorageEXT(EXTFramebufferObject.GL_RENDERBUFFER_EXT, EXTPackedDepthStencil.GL_DEPTH_STENCIL_EXT, Minecraft.getMinecraft().displayWidth, Minecraft.getMinecraft().displayHeight)
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_DEPTH_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
            EXTFramebufferObject.glFramebufferRenderbufferEXT(EXTFramebufferObject.GL_FRAMEBUFFER_EXT, EXTFramebufferObject.GL_STENCIL_ATTACHMENT_EXT, EXTFramebufferObject.GL_RENDERBUFFER_EXT, stencilDepthBufferID)
        }
        fun write(invert: Boolean) {
            checkSetupFBO(Minecraft.getMinecraft().framebuffer)
            glClearStencil(0)
            glClear(GL_STENCIL_BUFFER_BIT)
            glEnable(GL_STENCIL_TEST)
            glStencilFunc(GL_ALWAYS, 1, 65535)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            if (!invert) {
                glColorMask(false, false, false, false)
                glDepthMask(false)
                glStencilFunc(GL_ALWAYS, 1, 65535)
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            }
        }
        fun erase(invert: Boolean) {
            glStencilFunc(if (invert) GL_EQUAL else GL_NOTEQUAL, 1, 65535)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            if (invert) {
                glColorMask(true, true, true, true)
                glDepthMask(true)
                glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            } else {
                glColorMask(true, true, true, true)
                glDepthMask(true)
                glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
            }
        }
        fun dispose() {
            glDisable(GL_STENCIL_TEST)
        }
    }

    object InternalBlurShader {
        private val mc = Minecraft.getMinecraft()
        private var blurOutputFramebuffer: Framebuffer? = null
        private var shaderProgramID: Int = -1
        private var uniformTextureLocation = -1
        private var uniformTexelSizeLocation = -1
        private var uniformDirectionLocation = -1
        private var uniformRadiusLocation = -1

        fun blurArea(x: Float, y: Float, width: Float, height: Float, radius: Float) {
            val sr = ScaledResolution(mc)
            val factor = sr.scaleFactor
            ensureShaderInitialized()
            ensureFramebuffer(mc.displayWidth, mc.displayHeight)

            val sX = (x * factor).toInt()
            val sY = (mc.displayHeight - (y * factor).toInt() - (height * factor).toInt())
            val sW = (width * factor).toInt()
            val sH = (height * factor).toInt()

            glEnable(GL_SCISSOR_TEST)
            val pad = (radius * factor).toInt()
            glScissor(sX - pad, sY - pad, sW + pad * 2, sH + pad * 2)

            val buffer = blurOutputFramebuffer ?: return
            val mainBuffer = mc.framebuffer

            buffer.framebufferClear()
            buffer.bindFramebuffer(true)
            mainBuffer.bindFramebufferTexture()
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

            GL20.glUseProgram(shaderProgramID)
            GL20.glUniform2f(uniformTexelSizeLocation, 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
            GL20.glUniform1i(uniformTextureLocation, 0)
            GL20.glUniform1f(uniformRadiusLocation, radius)
            GL20.glUniform2f(uniformDirectionLocation, 1.0f, 0.0f)
            drawQuads()

            mainBuffer.bindFramebuffer(true)
            buffer.bindFramebufferTexture()
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071)

            GL20.glUniform2f(uniformDirectionLocation, 0.0f, 1.0f)
            drawQuads()

            GL20.glUseProgram(0)
            glDisable(GL_SCISSOR_TEST)
        }

        private fun ensureShaderInitialized() {
            if (shaderProgramID != -1) return
            val vertexShaderSrc = "#version 120\nvoid main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }"
            val fragmentShaderSrc = "#version 120\nuniform sampler2D textureIn; uniform vec2 texelSize; uniform vec2 direction; uniform float radius;\nfloat gaussian(float x, float sigma) { return exp(-(x*x) / (2.0 * sigma * sigma)); }\nvoid main() { vec2 coord = gl_TexCoord[0].xy; vec4 sum = vec4(0.0); float totalWeight = 0.0; int range = int(min(radius, 50.0)); float sigma = radius / 2.0; for (int i = -range; i <= range; i++) { float weight = gaussian(float(i), sigma); vec2 offset = float(i) * texelSize * direction; sum += texture2D(textureIn, coord + offset) * weight; totalWeight += weight; } gl_FragColor = sum / totalWeight; }"
            val vID = createShader(vertexShaderSrc, GL20.GL_VERTEX_SHADER)
            val fID = createShader(fragmentShaderSrc, GL20.GL_FRAGMENT_SHADER)
            shaderProgramID = GL20.glCreateProgram()
            GL20.glAttachShader(shaderProgramID, vID)
            GL20.glAttachShader(shaderProgramID, fID)
            GL20.glLinkProgram(shaderProgramID)
            GL20.glUseProgram(shaderProgramID)
            uniformTextureLocation = GL20.glGetUniformLocation(shaderProgramID, "textureIn")
            uniformTexelSizeLocation = GL20.glGetUniformLocation(shaderProgramID, "texelSize")
            uniformDirectionLocation = GL20.glGetUniformLocation(shaderProgramID, "direction")
            uniformRadiusLocation = GL20.glGetUniformLocation(shaderProgramID, "radius")
            GL20.glUseProgram(0)
        }
        private fun ensureFramebuffer(w: Int, h: Int) {
            if (blurOutputFramebuffer == null || blurOutputFramebuffer!!.framebufferWidth != w || blurOutputFramebuffer!!.framebufferHeight != h) {
                blurOutputFramebuffer?.deleteFramebuffer()
                blurOutputFramebuffer = Framebuffer(w, h, true)
                blurOutputFramebuffer!!.setFramebufferFilter(9729)
            }
        }
        private fun createShader(src: String, type: Int): Int {
            val id = GL20.glCreateShader(type)
            GL20.glShaderSource(id, src)
            GL20.glCompileShader(id)
            return id
        }
        private fun drawQuads() {
            val sr = ScaledResolution(mc)
            val w = sr.scaledWidth_double
            val h = sr.scaledHeight_double
            glBegin(GL_QUADS)
            glTexCoord2f(0f, 1f); glVertex2d(0.0, 0.0)
            glTexCoord2f(0f, 0f); glVertex2d(0.0, h)
            glTexCoord2f(1f, 0f); glVertex2d(w, h)
            glTexCoord2f(1f, 1f); glVertex2d(w, 0.0)
            glEnd()
        }
    }
}