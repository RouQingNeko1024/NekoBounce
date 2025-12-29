/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
@file:Suppress("unused")

package io.qzz.nekobounce.features.module.modules.world

import kotlinx.coroutines.delay
import io.qzz.nekobounce.NekoBounce.hud
import io.qzz.nekobounce.event.PacketEvent
import io.qzz.nekobounce.event.Render2DEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.features.module.modules.combat.AutoArmor
import io.qzz.nekobounce.features.module.modules.player.InventoryCleaner
import io.qzz.nekobounce.features.module.modules.player.InventoryCleaner.canBeSortedTo
import io.qzz.nekobounce.features.module.modules.player.InventoryCleaner.isStackUseful
import io.qzz.nekobounce.ui.client.hud.element.elements.Notification
import io.qzz.nekobounce.ui.font.Fonts
import io.qzz.nekobounce.utils.client.chat
import io.qzz.nekobounce.utils.inventory.InventoryManager
import io.qzz.nekobounce.utils.inventory.InventoryManager.canClickInventory
import io.qzz.nekobounce.utils.inventory.InventoryManager.chestStealerCurrentSlot
import io.qzz.nekobounce.utils.inventory.InventoryManager.chestStealerLastSlot
import io.qzz.nekobounce.utils.inventory.InventoryUtils.countSpaceInInventory
import io.qzz.nekobounce.utils.inventory.InventoryUtils.hasSpaceInInventory
import io.qzz.nekobounce.utils.inventory.SilentHotbar
import io.qzz.nekobounce.utils.render.RenderUtils
import io.qzz.nekobounce.utils.timing.TickedActions.awaitTicked
import io.qzz.nekobounce.utils.timing.TickedActions.clickNextTick
import io.qzz.nekobounce.utils.timing.TickedActions.isTicked
import io.qzz.nekobounce.utils.timing.TickedActions.nextTick
import io.qzz.nekobounce.utils.timing.TimeUtils.randomDelay
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.entity.EntityLiving.getArmorPosition
import net.minecraft.init.Blocks
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S30PacketWindowItems
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL20
import java.awt.Color
import kotlin.math.sqrt

object ChestStealer : Module("ChestStealer", Category.WORLD) {

    // --- Mode Selection ---
    private val modeValue by choices("Mode", arrayOf("Normal", "Matrix"), "Normal")

    // --- Matrix Mode Settings ---
    private val matrixStartDelay by int("MatrixStartDelay", 200, 0..2000) { modeValue == "Matrix" }
    private val matrixWaitDelay by int("MatrixWaitDelay", 100, 0..1000) { modeValue == "Matrix" }
    private val matrixCloseDelay by int("MatrixCloseDelay", 100, 0..1000) { modeValue == "Matrix" }

    // --- Normal Mode Settings ---
    private val smartDelay by boolean("SmartDelay", false) { modeValue == "Normal" }
    private val multiplier by int("DelayMultiplier", 120, 0..500) { smartDelay && modeValue == "Normal" }
    private val smartOrder by boolean("SmartOrder", true) { smartDelay && modeValue == "Normal" }

    private val simulateShortStop by boolean("SimulateShortStop", false) { modeValue == "Normal" }

    private val delay by intRange("Delay", 50..50, 0..500) { !smartDelay && modeValue == "Normal" }
    private val startDelay by intRange("StartDelay", 50..100, 0..500) { modeValue == "Normal" }
    private val closeDelay by intRange("CloseDelay", 50..100, 0..500) { modeValue == "Normal" }

    private val instantActions by boolean("InstantActions", false) { modeValue == "Normal" }

    // --- General Settings ---
    private val noMove by +InventoryManager.noMoveValue
    private val noMoveAir by +InventoryManager.noMoveAirValue
    private val noMoveGround by +InventoryManager.noMoveGroundValue

    private val chestTitle by boolean("ChestTitle", true)
    private val randomSlot by boolean("RandomSlot", true)

    // --- Render Settings ---
    private val progressBar by boolean("ProgressBar", true).subjective()
    private val progressBarY by float("ProgressBar-Y", -140f, -400f..0f) { progressBar }.subjective()

    // Blur defaults to false as requested
    private val progressBarBlur by boolean("ProgressBar-Blur", false) { progressBar }.subjective()
    private val progressBarBlurStrength by int("ProgressBar-Blur-Strength", 8, 0..20) { progressBar && progressBarBlur }.subjective()
    private val progressBarOverlayColor by color("ProgressBar-OverlayColor", Color(20, 20, 20, 180)) { progressBar }.subjective()
    private val progressBarTitle by text("ProgressBar-Title", "Steal Progress") { progressBar }.subjective()
    private val progressBarColor by color("ProgressBar-Color", Color(7, 153, 244)) { progressBar }.subjective()
    private val progressPercentageColor by color("ProgressPercentage-Color", Color.WHITE) { progressBar }.subjective()
    private val progressBarRadius by float("ProgressBar-Radius", 8f, 0f..15f) { progressBar }.subjective()

    val silentGUI by boolean("SilentGUI", false).subjective()
    val highlightSlot by boolean("Highlight-Slot", false) { !silentGUI }.subjective()
    val backgroundColor = color("BackgroundColor", Color(128, 128, 128)) { highlightSlot && !silentGUI }.subjective()
    val borderStrength by int("Border-Strength", 3, 1..5) { highlightSlot && !silentGUI }.subjective()
    val borderColor = color("BorderColor", Color(128, 128, 128)) { highlightSlot && !silentGUI }.subjective()

    private val chestDebug by choices("Chest-Debug", arrayOf("Off", "Text", "Notification"), "Off").subjective()
    private val itemStolenDebug by boolean("ItemStolen-Debug", false) { chestDebug != "Off" }.subjective()

    private var progress: Float? = null
        set(value) {
            field = value?.coerceIn(0f, 1f)
            if (field == null) {
                // 当偷窃结束时，重置所有动画状态
                easingProgress = 0f
            }
        }

    private var easingProgress = 0f
    private var animationAlpha = 0f
    private var receivedId: Int? = null
    private var stacks = emptyList<ItemStack?>()

    private suspend fun shouldOperate(): Boolean {
        while (true) {
            if (!handleEvents() || mc.playerController?.currentGameType?.isSurvivalOrAdventure != true ||
                mc.currentScreen !is GuiChest || mc.thePlayer?.openContainer?.windowId != receivedId)
                return false

            if (canClickInventory()) return true

            if (!instantActions) delay(50)
        }
    }

    suspend fun stealFromChest() {
        if (!handleEvents()) return

        val thePlayer = mc.thePlayer ?: return
        val screen = mc.currentScreen ?: return

        if (screen !is GuiChest || !shouldOperate()) return

        if (chestTitle && Blocks.chest.localizedName !in (screen.lowerChestInventory ?: return).name) return

        // Branch logic based on mode
        if (modeValue.equals("Matrix", ignoreCase = true)) {
            stealMatrix()
        } else {
            stealNormal()
        }
    }

    // New Matrix Logic
    private suspend fun stealMatrix() {
        val thePlayer = mc.thePlayer ?: return

        progress = 0f

        // 用于跟踪本次打开箱子会话中累积拿取的物品总数
        var totalStolenSoFar = 0

        // Loop logic: Start Delay -> Take -> Check/Wait -> Close or Loop
        while (shouldOperate() && hasSpaceInInventory()) {

            // 1. Wait StartDelay
            delay(matrixStartDelay.toLong())

            if (!shouldOperate()) break

            val itemsToSteal = getItemsToSteal()

            if (itemsToSteal.isNotEmpty()) {
                // 2. Take all items at once
                if (chestDebug != "Off") debug("Taking ${itemsToSteal.size} items (Matrix)")

                // 计算当前的“感知总数” = 已经拿走的 + 这一批要拿走的
                // 这样当新物品刷新时，总数增加，进度条会正确反映（例如从 100% 变回 60%）
                val currentBatchSize = itemsToSteal.size
                val currentTotalScope = totalStolenSoFar + currentBatchSize

                itemsToSteal.forEachIndexed { index, (slot, stack, sortableTo) ->
                    chestStealerCurrentSlot = slot
                    mc.playerController.windowClick(thePlayer.openContainer.windowId, slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1, thePlayer)

                    // 进度计算：(之前的总数 + 当前批次的索引 + 1) / (之前的总数 + 当前批次大小)
                    val currentProgressIndex = totalStolenSoFar + index + 1
                    updateProgressAndArmor(currentProgressIndex, currentTotalScope, stack)
                }

                chestStealerCurrentSlot = -1
                // 更新累积计数器
                totalStolenSoFar += currentBatchSize

                // Items found and taken, loop continues (restarts StartDelay logic)
                continue
            }

            // 3. If no items, wait WaitDelay
            delay(matrixWaitDelay.toLong())

            // Check again after wait
            if (!shouldOperate()) break
            val itemsAfterWait = getItemsToSteal()

            if (itemsAfterWait.isNotEmpty()) {
                // Items appeared during wait, loop back to start.
                // totalStolenSoFar is preserved, so the denominator will grow in the next loop.
                continue
            } else {
                // 4. Still no items, wait CloseDelay then close
                progress = 1f
                delay(matrixCloseDelay.toLong())

                if (shouldOperate()) {
                    thePlayer.closeScreen()
                    debug("Chest closed (Matrix)")
                }
                break // Exit loop
            }
        }

        progress = null
        chestStealerCurrentSlot = -1
    }

    // Original Logic (Renamed to stealNormal)
    private suspend fun stealNormal() {
        val thePlayer = mc.thePlayer ?: return

        progress = 0f
        delay(startDelay.random().toLong())
        debug("Stealing items..")

        while (true) {
            if (!shouldOperate() || !hasSpaceInInventory()) break

            var hasTaken = false
            val itemsToSteal = getItemsToSteal()

            run scheduler@{
                itemsToSteal.forEachIndexed { index, (slot, stack, sortableTo) ->
                    if (!shouldOperate() || !hasSpaceInInventory()) {
                        if (!instantActions) nextTick { SilentHotbar.resetSlot() }
                        chestStealerCurrentSlot = -1
                        chestStealerLastSlot = -1
                        return@scheduler
                    }

                    hasTaken = true
                    chestStealerCurrentSlot = slot

                    val stealingDelay = if (smartDelay) {
                        val nextSlot = itemsToSteal.getOrNull(index + 1)?.index ?: slot
                        val dist = squaredDistanceOfSlots(slot, nextSlot)
                        randomDelay((sqrt(dist.toDouble()) * multiplier).toInt(), (sqrt(dist.toDouble()) * multiplier).toInt() + 20)
                    } else {
                        delay.random()
                    }

                    if (itemStolenDebug) debug("item: ${stack.displayName.lowercase()} | slot: $slot | delay: ${stealingDelay}ms")

                    if (instantActions) {
                        mc.playerController.windowClick(thePlayer.openContainer.windowId, slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1, thePlayer)
                        updateProgressAndArmor(index + 1, itemsToSteal.size, stack)
                    } else {
                        clickNextTick(slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1) {
                            updateProgressAndArmor(index + 1, itemsToSteal.size, stack)
                        }
                    }

                    delay(stealingDelay.toLong())

                    if (simulateShortStop && Math.random() > 0.75) {
                        delay(randomDelay(150, 500).toLong())
                    }
                }
            }

            if (!hasTaken) break
            if (!instantActions) awaitTicked()
            stacks = thePlayer.openContainer.inventory
        }

        progress = 1f
        delay(closeDelay.random().toLong())
        if (!instantActions) nextTick { SilentHotbar.resetSlot() }

        chestStealerCurrentSlot = -1
        chestStealerLastSlot = -1
        if (instantActions) thePlayer.closeScreen() else nextTick { thePlayer.closeScreen() }
        progress = null
        debug("Chest closed")

        if (!instantActions) awaitTicked()
    }

    private fun updateProgressAndArmor(count: Int, total: Int, stack: ItemStack) {
        progress = count.toFloat() / total.toFloat()
        if (AutoArmor.canEquipFromChest()) {
            val item = stack.item
            val thePlayer = mc.thePlayer ?: return
            if (item is ItemArmor && thePlayer.inventory.armorInventory[getArmorPosition(stack) - 1] == null) {
                nextTick {
                    val newIndex = thePlayer.inventory.mainInventory.take(9).indexOfFirst { it?.getIsItemStackEqual(stack) == true }
                    if (newIndex != -1) AutoArmor.equipFromHotbarInChest(newIndex, stack)
                }
            }
        }
    }

    private fun squaredDistanceOfSlots(from: Int, to: Int): Int {
        val (x1, y1) = from % 9 to from / 9
        val (x2, y2) = to % 9 to to / 9
        return (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)
    }

    private data class ItemTakeRecord(val index: Int, val stack: ItemStack, val sortableToSlot: Int?)

    private fun getItemsToSteal(): List<ItemTakeRecord> {
        val sortBlacklist = BooleanArray(9)
        var spaceInInventory = countSpaceInInventory()
        return stacks.dropLast(36)
            .mapIndexedNotNull { index, stack ->
                stack ?: return@mapIndexedNotNull null
                if (modeValue == "Normal" && !instantActions && isTicked(index)) return@mapIndexedNotNull null

                val mergeableCount = mc.thePlayer.inventory.mainInventory.sumOf { otherStack ->
                    if (otherStack != null && otherStack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(stack, otherStack)) otherStack.maxStackSize - otherStack.stackSize else 0
                }
                val canFullyMerge = mergeableCount >= stack.stackSize
                if (mergeableCount <= 0 && spaceInInventory <= 0) return@mapIndexedNotNull null
                if (InventoryCleaner.handleEvents() && !isStackUseful(stack, stacks, noLimits = canFullyMerge)) return@mapIndexedNotNull null

                var sortableTo: Int? = null
                if (mergeableCount <= 0 && InventoryCleaner.handleEvents() && InventoryCleaner.sort) {
                    for (hotbarIndex in 0..8) {
                        if (!sortBlacklist[hotbarIndex] && canBeSortedTo(hotbarIndex, stack.item)) {
                            val hotbarStack = stacks.getOrNull(stacks.size - 9 + hotbarIndex)
                            if (!canBeSortedTo(hotbarIndex, hotbarStack?.item) || !isStackUseful(hotbarStack, stacks, strictlyBest = true)) {
                                sortableTo = hotbarIndex
                                sortBlacklist[hotbarIndex] = true
                                break
                            }
                        }
                    }
                }
                if (!canFullyMerge) spaceInInventory--
                ItemTakeRecord(index, stack, sortableTo)
            }.toMutableList().also {
                if (randomSlot && modeValue == "Normal") it.shuffle()

                it.sortByDescending { it.stack.item is ItemArmor }
                it.sortByDescending { it.sortableToSlot != null }
                if (AutoArmor.canEquipFromChest()) it.sortByDescending { it.stack.item is ItemArmor }

                if (smartOrder && modeValue == "Normal") sortBasedOnOptimumPath(it)
            }
    }

    private fun sortBasedOnOptimumPath(itemsToSteal: MutableList<ItemTakeRecord>) {
        for (i in 0 until itemsToSteal.size - 1) {
            var bestChoiceIndex = i + 1
            var minDistance = Int.MAX_VALUE
            for (j in i + 1 until itemsToSteal.size) {
                val distance = squaredDistanceOfSlots(itemsToSteal[i].index, itemsToSteal[j].index)
                if (distance < minDistance) {
                    minDistance = distance
                    bestChoiceIndex = j
                }
            }
            if (bestChoiceIndex != i + 1) {
                val temp = itemsToSteal[i + 1]
                itemsToSteal[i + 1] = itemsToSteal[bestChoiceIndex]
                itemsToSteal[bestChoiceIndex] = temp
            }
        }
    }

    val onRender2D = handler<Render2DEvent> {
        if (progressBar) {
            val targetAlpha = if (mc.currentScreen is GuiChest && progress != null) 1f else 0f
            animationAlpha += (targetAlpha - animationAlpha) * 0.15f * it.partialTicks
            animationAlpha = animationAlpha.coerceIn(0f, 1f)

            val currentProgress = this.progress
            if (currentProgress != null) {
                easingProgress += (currentProgress - easingProgress) / 4f * it.partialTicks
            }

            if (animationAlpha <= 0.01f) {
                return@handler
            }

            val sr = ScaledResolution(mc)
            val windowHeight = 55f
            val windowWidth = windowHeight * (16f / 9f)
            val windowX = (sr.scaledWidth - windowWidth) / 2f
            val windowY = sr.scaledHeight - windowHeight + progressBarY
            val windowRadius = this.progressBarRadius

            fun Color.withAlpha(alpha: Float): Int {
                return Color(red, green, blue, (this.alpha * alpha).toInt()).rgb
            }

            glEnable(GL_STENCIL_TEST)
            glColorMask(false, false, false, false)
            glStencilFunc(GL_ALWAYS, 1, 0xFF)
            glStencilOp(GL_KEEP, GL_KEEP, GL_REPLACE)
            RenderUtils.drawRoundedRect(windowX, windowY, windowX + windowWidth, windowY + windowHeight, 0, windowRadius)

            glColorMask(true, true, true, true)
            glStencilFunc(GL_EQUAL, 1, 0xFF)
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)

            if (progressBarBlur) {
                InternalBlurShader.renderBlur(progressBarBlurStrength.toFloat())
            }
            RenderUtils.drawRoundedRect(windowX, windowY, windowX + windowWidth, windowY + windowHeight, progressBarOverlayColor.withAlpha(animationAlpha), windowRadius)

            glDisable(GL_STENCIL_TEST)

            val barWidth = windowWidth * 0.85f
            val barHeight = 6f
            val barX = windowX + (windowWidth - barWidth) / 2f
            val barY = windowY + 30f
            val barRadius = 3f

            val titleFont = Fonts.fontGoogleSans35
            val title = progressBarTitle
            titleFont.drawString(title, windowX + (windowWidth - titleFont.getStringWidth(title)) / 2f, windowY + 10f, Color.WHITE.withAlpha(animationAlpha))

            RenderUtils.drawRoundedRect(barX, barY, barX + barWidth, barY + barHeight, Color(10, 10, 10, 180).withAlpha(animationAlpha), barRadius)
            if (easingProgress > 0) {
                RenderUtils.drawRoundedRect(barX, barY, barX + barWidth * easingProgress, barY + barHeight, progressBarColor.withAlpha(animationAlpha), barRadius)
            }

            val percentageFont = Fonts.fontGoogleSans35
            val percentageText = "${(easingProgress * 100).toInt()}%"
            val textX = barX + barWidth - percentageFont.getStringWidth(percentageText) - 5f
            val textY = barY + (barHeight - percentageFont.height) / 2f + 1f
            percentageFont.drawString(percentageText, textX, textY, progressPercentageColor.withAlpha(animationAlpha))
        }
    }

    val onPacket = handler<PacketEvent> {
        when (it.packet) {
            is S2DPacketOpenWindow -> {
                receivedId = null; progress = null
            }
            is S2EPacketCloseWindow -> {
                receivedId = null; progress = null
            }
            is S30PacketWindowItems -> {
                val packet = it.packet
                if (packet.func_148911_c() != 0) {
                    if (receivedId != packet.func_148911_c()) debug("Chest opened with ${packet.itemStacks.size} items")
                    receivedId = packet.func_148911_c()
                    stacks = packet.itemStacks.toList()
                }
            }
        }
    }

    private fun debug(message: String) {
        if (chestDebug == "Off") return
        when (chestDebug.lowercase()) {
            "text" -> chat(message)
            "notification" -> hud.addNotification(Notification.informative(this, message, 500L))
        }
    }

    private object InternalBlurShader {
        private val mc = Minecraft.getMinecraft()
        private var blurOutputFramebuffer: Framebuffer? = null
        private var shaderProgramID: Int = -1
        private var uniformTextureLocation = -1
        private var uniformTexelSizeLocation = -1
        private var uniformDirectionLocation = -1
        private var uniformRadiusLocation = -1

        fun renderBlur(radius: Float) {
            ensureShaderInitialized()
            ensureFramebuffer(mc.displayWidth, mc.displayHeight)
            val buffer = blurOutputFramebuffer ?: return

            buffer.framebufferClear()
            buffer.bindFramebuffer(true)
            GL20.glUseProgram(shaderProgramID)
            GL20.glUniform1i(uniformTextureLocation, 0)
            GL20.glUniform1f(uniformRadiusLocation, radius)
            GL20.glUniform2f(uniformTexelSizeLocation, 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
            GL20.glUniform2f(uniformDirectionLocation, 1.0f, 0.0f)
            mc.framebuffer.bindFramebufferTexture()
            drawScreenQuad()

            mc.framebuffer.bindFramebuffer(true)
            GL20.glUniform2f(uniformDirectionLocation, 0.0f, 1.0f)
            buffer.bindFramebufferTexture()
            drawScreenQuad()

            GL20.glUseProgram(0)
            GlStateManager.enableBlend()
        }

        private fun ensureShaderInitialized() {
            if (shaderProgramID != -1) return
            val vSrc = "#version 120\nvoid main() { gl_TexCoord[0] = gl_MultiTexCoord0; gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex; }"
            val fSrc = "#version 120\nuniform sampler2D textureIn; uniform vec2 texelSize; uniform vec2 direction; uniform float radius;\nfloat gaussian(float x, float sigma) { return exp(-(x*x) / (2.0 * sigma * sigma)); }\nvoid main() { vec2 coord = gl_TexCoord[0].xy; vec4 sum = vec4(0.0); float totalWeight = 0.0; int range = int(min(radius, 50.0)); float sigma = radius / 2.0; for (int i = -range; i <= range; i++) { float weight = gaussian(float(i), sigma); vec2 offset = float(i) * texelSize * direction; sum += texture2D(textureIn, coord + offset) * weight; totalWeight += weight; } gl_FragColor = sum / totalWeight; }"
            val vID = createShader(vSrc, GL20.GL_VERTEX_SHADER); val fID = createShader(fSrc, GL20.GL_FRAGMENT_SHADER)
            shaderProgramID = GL20.glCreateProgram(); GL20.glAttachShader(shaderProgramID, vID); GL20.glAttachShader(shaderProgramID, fID)
            GL20.glLinkProgram(shaderProgramID); GL20.glUseProgram(shaderProgramID)
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
            }
        }

        private fun createShader(src: String, type: Int): Int {
            val id = GL20.glCreateShader(type); GL20.glShaderSource(id, src); GL20.glCompileShader(id); return id
        }

        private fun drawScreenQuad() {
            val sr = ScaledResolution(mc)
            glBegin(GL_QUADS)
            glTexCoord2f(0f, 1f); glVertex2f(0f, 0f)
            glTexCoord2f(0f, 0f); glVertex2f(0f, sr.scaledHeight.toFloat())
            glTexCoord2f(1f, 0f); glVertex2f(sr.scaledWidth.toFloat(), sr.scaledHeight.toFloat())
            glTexCoord2f(1f, 1f); glVertex2f(sr.scaledWidth.toFloat(), 0f)
            glEnd()
        }
    }
}