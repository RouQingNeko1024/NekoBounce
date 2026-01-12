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

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.ui.client.hud.designer.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Text.Companion
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.GlowUtils
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawImage
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.animation.AnimationUtil
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.client.renderer.RenderHelper.disableStandardItemLighting
import net.minecraft.client.renderer.RenderHelper.enableGUIStandardItemLighting
import net.minecraft.item.ItemBlock
import net.minecraft.util.ResourceLocation
import net.minecraft.client.Minecraft
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.max
import kotlin.math.pow

object WaterMark : Module("WaterMark", Category.RENDER) {
    private val ClientName by text("ClientName", "NekoBounce")
    private val animationSpeed by float("AnimationSpeed", 0.2F, 0.05F..1F)
    //private val Opal by boolean("Opal",false)
    private val styles by choices("Styles", arrayOf("None","Normal","Opal"),"Normal")
    private val ColorA_ by int("Red",255,0..255)
    private val ColorB_ by int("Green",255,0..255)
    private val ColorC_ by int("Blue",255,0..255)
    private val ShadowCheck by boolean("Shadow",false)
    private val shadowStrengh by int("ShadowStrength", 1, 1..2)
    private val BackgroundAlpha by int("BackGroundAlpha",120,0..255)
    private val versionNameUp by text("VersionName","Beta")
    private val ModuleNotify by boolean("Notification",true)
    private val ScaffoldTheme by color("ScaffoldTheme",Color(255,255,255))
    private val maxBlocks by int("maxBlocks",576,64..576)
    private val versionNameDown = clientVersionText
    enum class State {
        Normal,
        Normal2,
        Scaffold,
        Notify
    }
    private val progressLen = 120F
    private var ProgressBarAnimationWidth = progressLen
    val DECIMAL_FORMAT = DecimalFormat("0.00")
    private var scaledScreen = ScaledResolution(mc)
    private var width = scaledScreen.scaledWidth
    private var height = scaledScreen.scaledHeight
    private var island_State = State.Normal
    private var start_y = (height/20).toFloat()
    private var AnimStartX = (width/2).toFloat()
    private var AnimEndX = AnimStartX+100F
    private val NOTIFICATION_HEIGHT = 35f
    private var AnimModuleEndY = NOTIFICATION_HEIGHT
    private val notifications = CopyOnWriteArrayList<Notification>()

    val onRender2D = handler<Render2DEvent>{
        updateNotifications()
        scaledScreen = ScaledResolution(mc)
        width = scaledScreen.scaledWidth
        height = scaledScreen.scaledHeight
        island_State = State.Normal
        start_y = (height/20).toFloat()
        if (moduleManager.getModule("Scaffold")?.state == true) {
            island_State = State.Scaffold
        }else{
            when (styles){
                "Normal" -> {island_State = State.Normal}
                "Opal" -> {island_State = State.Normal2}
                else -> {}
            }
            if (notifications.isNotEmpty() && ModuleNotify) {
                island_State = State.Notify
            }
        }
        when (island_State) {
            State.Normal -> drawNormal()
            State.Normal2 -> drawNormal2()
            State.Scaffold -> drawScaffold()
            State.Notify -> drawNotificationsUI(scaledScreen,start_y)
            else -> {}
        }
    }
    private fun drawNormal(){
        val username = mc.session.username
        val fps = Minecraft.getDebugFPS()
        val pings = mc.thePlayer.getPing()
        val colorRGB = Color(ColorA_, ColorB_, ColorC_,255)
        val drawTextWidth = " | ${username} | ${fps}fps | ${pings}ms"
        val drawMainText = ClientName
        val onlyMainTextWidth = Fonts.fontGoogleSans40.getStringWidth(drawMainText)
        val allTextWidth = Fonts.fontGoogleSans40.getStringWidth(drawTextWidth)+onlyMainTextWidth
        val textUIHeight = 9F
        val imageWidthHeights = 18F
        val containerToUiDistance = 5F
        val borderHeight = containerToUiDistance*2+imageWidthHeights
        val roundedNumber = borderHeight/2
        val allUILen = containerToUiDistance+imageWidthHeights+containerToUiDistance+allTextWidth+containerToUiDistance
        val startUIX = (width-allUILen)/2
        val startYCalc = start_y+borderHeight/2
        val borderStartY =startYCalc-borderHeight/2
        val startTextYCalc = startYCalc-textUIHeight/2+1F // offset
        val startImageYCalc = startYCalc-imageWidthHeights/2
        AnimStartX = AnimationUtil.base(AnimStartX.toDouble(),startUIX.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)
        AnimEndX = AnimationUtil.base(AnimEndX.toDouble(),allUILen+startUIX.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)
        ShowShadow(AnimStartX,borderStartY, AnimEndX-startUIX, borderHeight)
        drawRoundedBorderRect(AnimStartX,borderStartY, AnimEndX,borderStartY+borderHeight,0.5F,Color(10,10,10, BackgroundAlpha).rgb,Color(30,30,30, BackgroundAlpha).rgb,roundedNumber)
        drawImage(ResourceLocation("liquidbounce/logo_icon.png"),AnimStartX+containerToUiDistance,startImageYCalc,imageWidthHeights.toInt(),imageWidthHeights.toInt(),colorRGB)
        Fonts.fontGoogleSans40.drawString(drawMainText,AnimStartX+containerToUiDistance+imageWidthHeights+containerToUiDistance,startTextYCalc,colorRGB.rgb)
        Fonts.fontGoogleSans40.drawString(drawTextWidth,AnimStartX+containerToUiDistance+imageWidthHeights+containerToUiDistance+onlyMainTextWidth,startTextYCalc,Color(255,255,255,255).rgb)
    }
    private fun drawNormal2() {
        val serverip = ServerUtils.remoteIp
        val playerPing = "${mc.thePlayer.getPing()}ms"
        val textWidth = Fonts.fontSemibold40.getStringWidth(ClientName)
        val ColorAL = Color(ColorA_, ColorB_, ColorC_,255)
        val imageLen = 21F
        val containerToUiDistance = 2F
        val uiToUIDistance = 4F
        val textBar2 = max(Fonts.fontSemibold40.getStringWidth(versionNameUp),Fonts.fontSemibold35.getStringWidth(
            versionNameDown
        ))
        val textBar3 = max(Fonts.fontSemibold40.getStringWidth(serverip),Fonts.fontSemibold35.getStringWidth(playerPing))
        val LineWidth = 2F
        val fastLen1 = containerToUiDistance+imageLen+uiToUIDistance
        val allLen = fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance+textBar2+uiToUIDistance+LineWidth+uiToUIDistance+textBar3+containerToUiDistance+3F
        val startX = (width-allLen)/2
        AnimStartX = AnimationUtil.base(AnimStartX.toDouble(),startX.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)
        AnimEndX = AnimationUtil.base(AnimEndX.toDouble(),allLen+startX.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)
        drawRoundedRect(
            AnimStartX,
            start_y, AnimEndX , start_y +27F,Color(0,0,0,
                BackgroundAlpha
            ).rgb,13F)
        ShowShadow(AnimStartX, start_y, AnimEndX-startX, 27F)
        drawImage(ResourceLocation("liquidbounce/logo_icon.png"), startX+containerToUiDistance+2F, start_y +4F, 19, 19,ColorAL)//23F, 23F
        Fonts.fontSemibold40.drawString(ClientName,startX+fastLen1, start_y +9F,ColorAL.rgb,false)
        Fonts.fontSemibold40.drawString("|",startX+fastLen1+textWidth+uiToUIDistance-1F,
            start_y +9F,Color(120,120,120,250).rgb,false)
        Fonts.fontSemibold40.drawString(
            versionNameUp,startX+fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance,
            start_y +4.5F,Color(255,255,255,255).rgb,false)
        Fonts.fontSemibold35.drawString(
            versionNameDown,startX+fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance,
            start_y +14F,Color(255,255,255,110).rgb,false)
        Fonts.fontSemibold40.drawString("|",startX+fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance+textBar2+uiToUIDistance-1F,
            start_y +9F,Color(120,120,120,250).rgb,false)
        Fonts.fontSemibold40.drawString(serverip,startX+fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance+textBar2+uiToUIDistance+LineWidth+uiToUIDistance,
            start_y +4.5F,Color(255,255,255,255).rgb,false)
        Fonts.fontSemibold35.drawString(playerPing,startX+fastLen1+textWidth+uiToUIDistance+LineWidth+uiToUIDistance+textBar2+uiToUIDistance+LineWidth+uiToUIDistance,
            start_y +14F,Color(255,255,255,110).rgb,false)
    }
    private fun drawScaffold() {
        val stack = mc.thePlayer?.inventory?.getStackInSlot(SilentHotbar.currentSlot)
        val shouldRender = stack?.item is ItemBlock
        val progressLen_height = 3F
        val imageLen = 23F
        val offsetLen = 2F
        val blockAmount = InventoryUtils.blocksAmount()
        val Pitch = Companion.DECIMAL_FORMAT.format(mc.thePlayer.rotationPitch)
        val countWidth = Fonts.fontSemibold40.getStringWidth("$blockAmount blocks")
        val percentProLen = progressLen/maxBlocks
        val fastCalc = offsetLen+imageLen+offsetLen
        val allLen = fastCalc+progressLen+offsetLen+4F+countWidth+offsetLen
        val startXScaffold = (width-allLen)/2 // ((width/2)-(allLen/2))

        AnimStartX = AnimationUtil.base(AnimStartX.toDouble(),startXScaffold.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)
        AnimEndX = AnimationUtil.base(AnimEndX.toDouble(),allLen+startXScaffold+1.0, animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)

        var progressLenReal2 = fastCalc+percentProLen*blockAmount
        if (blockAmount>maxBlocks){
            progressLenReal2 = fastCalc+progressLen
        }

        ProgressBarAnimationWidth = AnimationUtil.base(ProgressBarAnimationWidth.toDouble(),progressLenReal2.toDouble(), animationSpeed.toDouble()).toFloat().coerceAtLeast(0f)

        drawRoundedRect(AnimStartX-1F,start_y, AnimEndX, start_y+27F,Color(0,0,0, BackgroundAlpha).rgb,13F)
        ShowShadow(AnimStartX,start_y, AnimEndX-startXScaffold, 27F)

        drawRoundedRect(startXScaffold+fastCalc, start_y+13.5F-progressLen_height/2,startXScaffold+fastCalc+progressLen,start_y+27F/2+progressLen_height/2,
            Color(safeColor(ScaffoldTheme.red-170),safeColor(ScaffoldTheme.green-170),safeColor(ScaffoldTheme.blue-170),255).rgb,1.5F)
        drawRoundedRect(startXScaffold+fastCalc, start_y+13.5F-progressLen_height/2,startXScaffold+ProgressBarAnimationWidth,start_y+27F/2+progressLen_height/2,
            Color(ScaffoldTheme.red,ScaffoldTheme.green,ScaffoldTheme.blue,255).rgb,1.5F)

        Fonts.fontSemibold40.drawString("$blockAmount blocks",startXScaffold+fastCalc+progressLen+offsetLen+3F,start_y+4.5F,Color.WHITE.rgb)
        Fonts.fontSemibold35.drawString("${Pitch} a",startXScaffold+fastCalc+progressLen+offsetLen+3F,start_y+14F,Color(140,140,140,255).rgb)

        glPushMatrix()
        enableGUIStandardItemLighting()
        if (mc.currentScreen is GuiHudDesigner) glDisable(GL_DEPTH_TEST)
        if (shouldRender) {
            mc.renderItem.renderItemAndEffectIntoGUI(stack, (startXScaffold+offsetLen+4).toInt(), (offsetLen+start_y+4).toInt())
        }
        disableStandardItemLighting()
        enableAlpha()
        disableBlend()
        disableLighting()
        if (mc.currentScreen is GuiHudDesigner) glEnable(GL_DEPTH_TEST)
        glPopMatrix()
    }
    private fun ShowShadow(startX: Float,startY: Float,width: Float,height:Float){
        if (ShadowCheck) {
            GlowUtils.drawGlow(
                startX, startY,
                width, height,
                (shadowStrengh * 13F).toInt(),
                Color(0, 0, 0, 120)
            )
        }
    }

    private fun drawToggleButton(StartX: Float, StartY: Float, BigBoardHeight: Float, ModuleState: Boolean, animationState: SwitchAnimationState) {
        val buttonHeight = 19F
        val buttonWidth = 32F
        val buttonToButtonDistance = 4F
        val buttonRounded = buttonHeight / 2
        val smallButtonHeight = buttonHeight - buttonToButtonDistance * 2
        val smallButtonWidth = smallButtonHeight
        val toBigBorderLen = 6F
        val ButtonStartX = (BigBoardHeight - buttonHeight) / 2
        animationState.updateState(ModuleState)
        val animation = animationState.getOutput()
        drawRoundedBorderRect(
            StartX + toBigBorderLen,
            StartY + ButtonStartX,
            StartX + toBigBorderLen + buttonWidth,
            StartY + ButtonStartX + buttonHeight,0.1f,
            Color.DARK_GRAY.rgb,
            Color.DARK_GRAY.rgb,
            buttonRounded
        )
        val color = if (ModuleState) {
            Color(
                (128 - 80 * animation).toInt(),
                (128 - 11 * animation).toInt(),
                (128 + 19 * animation).toInt(),
                255
            )
        } else {
            Color(128, 128, 128, 255)
        }
        drawRoundedRect(
            StartX + toBigBorderLen + 1,
            StartY + ButtonStartX + 1,
            StartX + toBigBorderLen + buttonWidth - 1,
            StartY + ButtonStartX + buttonHeight - 1,
            color.rgb,
            buttonRounded - 1
        )
        val smallButtonX = StartX + toBigBorderLen + buttonToButtonDistance +
                (buttonWidth - buttonToButtonDistance * 2 - smallButtonWidth) * animation.toFloat()

        drawRoundedBorderRect(
            smallButtonX,
            StartY + ButtonStartX + buttonToButtonDistance,
            smallButtonX + smallButtonWidth,
            StartY + ButtonStartX + buttonToButtonDistance + smallButtonHeight,0.1f,
            Color.DARK_GRAY.rgb,
            Color.DARK_GRAY.rgb,
            smallButtonHeight / 2
        )
    }


    private fun drawToggleText(StartX:Float,StartY: Float, TextBar: Pair<String,String>, BigBoardHeight: Float) {
        val TextHeight = 9F
        val title = TextBar.first
        val description = TextBar.second
        val buttonToTextLen = 5F
        val TextStartX = StartX+39F+buttonToTextLen
        Fonts.fontRegular40.drawString(title,TextStartX,StartY+BigBoardHeight/2-TextHeight,Color(255,255,255,255).rgb)
        Fonts.fontRegular35.drawString(description,TextStartX,StartY+BigBoardHeight/2+2F,Color(255,255,255,255).rgb)
    }

    // 添加动画方向枚举
    enum class Direction {
        FORWARDS,
        BACKWARDS
    }
    class EaseOutExpo(private val duration: Long, private val end: Double) {
        private var start = 0.0
        private var startTime = 0L
        private var direction = Direction.FORWARDS

        init {
            startTime = System.currentTimeMillis()
        }
        fun setDirection(direction: Direction) {
            if (this.direction != direction) {
                this.direction = direction
                startTime = System.currentTimeMillis()
                start = getOutput()
            }
        }
        fun getOutput(): Double {
            val elapsedTime = (System.currentTimeMillis() - startTime).coerceAtMost(duration)
            val progress = elapsedTime.toDouble() / duration
            val result = when (direction) {
                Direction.FORWARDS -> if (progress == 1.0) end else (-2.0.pow(-10 * progress) + 1) * end
                Direction.BACKWARDS -> if (progress == 1.0) 0.0 else (2.0.pow(-10 * progress) * end)
            }

            return result.coerceIn(0.0, end)
        }
    }
    class SwitchAnimationState {
        private val animation: EaseOutExpo = EaseOutExpo(300, 1.0)

        fun updateState(state: Boolean) {
            animation.setDirection(if (state) Direction.FORWARDS else Direction.BACKWARDS)
        }

        fun getOutput(): Double {
            return animation.getOutput()
        }
    }

    private abstract class Notification (
        val id:String = UUID.randomUUID().toString(),
        var title: String,
        var message: String,
        var createTime: Long = System.currentTimeMillis(),
        val duration: Long = 3000L,
    ) {
        var isMarkedForDelete = false
        abstract fun draw(x: Float, y: Float)
        open var enabled: Boolean = false
        fun getHeight(): Float = NOTIFICATION_HEIGHT
        fun update() {
            if (isFading()){
                isMarkedForDelete = true
            }
        }
        fun isFading(): Boolean = System.currentTimeMillis() > createTime + duration || isMarkedForDelete
    }

    private class ToggleNotification(
        title: String,
        message: String,
        duration: Long,
        enabled: Boolean,
        val moduleName: String
    ) : Notification(duration = duration, title = title, message = message) {
        private val animationState = SwitchAnimationState()  // 添加动画状态

        init {
            this.enabled = enabled
        }

        override fun draw(x: Float, y: Float){
            drawToggleButton(x, y, 35F, enabled, animationState)  // 传递动画状态
            drawToggleText(x, y, Pair(title, message), 35F)
        }
    }

    fun showToggleNotification(title: String, message: String, enabled: Boolean, duration: Long = 3000L, moduleName: String? = null) {
        if (moduleName != null) {
            val existingNotification = notifications.find {
                it is ToggleNotification && it.moduleName == moduleName
            }
            if (existingNotification != null) {
                existingNotification.createTime = System.currentTimeMillis()
                existingNotification.title = title
                existingNotification.message = message
                existingNotification.enabled = enabled
                return
            }
        }

        // 添加新通知
        notifications.add(ToggleNotification(title, message, duration, enabled, moduleName ?: ""))
    }
    private fun updateNotifications() {
        notifications.forEach { it.update() }
        notifications.removeAll { it.isMarkedForDelete }
    }
    private fun calcNotification(): Pair<Float,Float> {
        if (notifications.isEmpty()) return Pair(0F,0F)
        var resultHeight = 0f
        var maxWidth = 0f
        for (notif in notifications) {
            val height = notif.getHeight()
            val width = Fonts.fontSemibold35.getStringWidth(notif.message).toFloat()+6F+32F+5F+6F
            resultHeight += height
            maxWidth = max(maxWidth, width)
        }
        return Pair(maxWidth, resultHeight)
    }
    private fun drawNotificationsUI(sr: ScaledResolution, StartY: Float) {
        val screenWidth = sr.scaledWidth.toFloat()
        val myBordersA: Pair<Float, Float> = calcNotification()
        val startX_a = (screenWidth-myBordersA.first)/2//screenWidth / 2 - myBordersA.first / 2
        AnimModuleEndY = AnimationUtil.base(AnimModuleEndY.toDouble(),(StartY + myBordersA.second).toDouble(),0.6).toFloat().coerceAtLeast(0f)

        AnimStartX = AnimationUtil.base(AnimStartX.toDouble(),startX_a.toDouble(),0.8).toFloat().coerceAtLeast(0f)
        AnimEndX = AnimationUtil.base(AnimEndX.toDouble(),3.0+startX_a+myBordersA.first.toDouble(),0.8).toFloat().coerceAtLeast(0f)

        drawRoundedBorderRect(AnimStartX, StartY, AnimEndX , AnimModuleEndY,1F,Color(0, 0, 0, BackgroundAlpha).rgb,Color(0, 0, 0, BackgroundAlpha).rgb, 10F)
        ShowShadow(AnimStartX, StartY, AnimEndX-startX_a, AnimModuleEndY-StartY)
        //glEnable(GL_SCISSOR_TEST)

        var currentY = StartY
        for (notify in notifications) {
            if (myBordersA.second > 0) {
                notify.draw(startX_a, currentY)
                currentY += notify.getHeight()
            }

        }
        //glDisable(GL_SCISSOR_TEST)
    }
    private fun safeColor(ColorA: Int) : Int{
        if (ColorA>255) return 255
        else if (ColorA<0) return 0
        else return ColorA
    }
    private fun easeOutQuad(t: Float, b: Float, c: Float): Float {
        return -c * t * (t - 2) + b
    }

    // 或者使用线性插值
    private fun lerp(start: Float, end: Float, progress: Float): Float {
        return start + (end - start) * progress
    }
}
