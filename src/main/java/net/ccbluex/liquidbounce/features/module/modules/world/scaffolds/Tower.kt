/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce
 * Code By GoldBounce,Lizz,NightSky,FDP
 * https://github.com/SkidderMC/FDPClient
 * https://github.com/qm123pz/NightSky-Client
 * https://github.com/bzym2/GoldBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold.searchMode
import net.ccbluex.liquidbounce.features.module.modules.world.scaffolds.Scaffold.shouldGoDown
import net.ccbluex.liquidbounce.utils.ReflectionUtil
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.blocksAmount
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.potion.Potion
import net.minecraft.stats.StatList
import net.minecraft.util.BlockPos
import kotlin.math.truncate

object Tower : Configurable("Tower"), MinecraftInstance, Listenable {

    val towerModeValues = choices(
        "TowerMode",
        arrayOf(
            "None",
            "Jump",
            "MotionJump",
            "Motion",
            "ConstantMotion",
            "MotionTP",
            "Packet",
            "Teleport",
            "AAC3.3.9",
            "AAC3.6.4",
            "Vulcan2.9.0",
            "Pulldown",
            "Prediction",
            "Matrix",  // 添加Matrix模式
            "BlocksMC",
            "NCP"
        ),
        "None"
    )

    private var matrixState = 0  // 添加Matrix状态变量
    private val motionBlocksMC by float("BlocksMC-Motion", 1F, 0.1F..1F) { towerModeValues.equals("BlocksMC")}
    private val motionSpeedEffectBlocksMC by float("BlocksMC-SpeedEffect-Motion", 1F, 0.1F..1F) { towerModeValues.equals("BlocksMC")}
    val stopWhenBlockAboveValues = boolean("StopWhenBlockAbove", false) { towerModeValues.get() != "None" }

    val onJumpValues = boolean("TowerOnJump", true) { towerModeValues.get() != "None" }
    val notOnMoveValues = boolean("TowerNotOnMove", false) { towerModeValues.get() != "None" }

    // Jump mode
    val jumpMotionValues = float("JumpMotion", 0.42f, 0.3681289f..0.79f) { towerModeValues.get() == "MotionJump" }
    val jumpDelayValues = int(
        "JumpDelay",
        0,
        0..20
    ) { towerModeValues.get() == "MotionJump" || towerModeValues.get() == "Jump" }

    // Constant Motion values
    val constantMotionValues = float(
        "ConstantMotion",
        0.42f,
        0.1f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpGroundValues = float(
        "ConstantMotionJumpGround",
        0.79f,
        0.76f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpPacketValues = boolean("JumpPacket", true) { towerModeValues.get() == "ConstantMotion" }

    // Pull-down
    val triggerMotionValues = float("TriggerMotion", 0.1f, 0.0f..0.2f) { towerModeValues.get() == "Pulldown" }
    val dragMotionValues = float("DragMotion", 1.0f, 0.1f..1.0f) { towerModeValues.get() == "Pulldown" }

    // Teleport
    val teleportHeightValues = float("TeleportHeight", 1.15f, 0.1f..5f) { towerModeValues.get() == "Teleport" }
    val teleportDelayValues = int("TeleportDelay", 0, 0..20) { towerModeValues.get() == "Teleport" }
    val teleportGroundValues = boolean("TeleportGround", true) { towerModeValues.get() == "Teleport" }
    val teleportNoMotionValues = boolean("TeleportNoMotion", false) { towerModeValues.get() == "Teleport" }

    // Prediction
    val predictionRangeValues = float("PredictionRange", 0.5f, 0.1f..2f) { towerModeValues.get() == "Prediction" }
    val predictionDelayValues = int("PredictionDelay", 0, 0..20) { towerModeValues.get() == "Prediction" }
    val predictionSpeedValues = float("PredictionSpeed", 0.42f, 0.1f..1f) { towerModeValues.get() == "Prediction" }
    val predictionExtraBlock = boolean("PredictionExtraBlock", false) { towerModeValues.get() == "Prediction" }
    val predictionExtraDelay = int("PredictionExtraDelay", 0, 0..10) { towerModeValues.get() == "Prediction" && predictionExtraBlock.get() }

    var isTowering = false
    private var predictionTicks = 0
    private var placeExtraBlock = false
    private var extraBlockDelay = 0

    // Mode stuff
    private val tickTimer = TickTimer()
    private var jumpGround = 0.0

    // Handle motion events
    val onMove = handler<MoveEvent> { event ->
        if (isTowering && towerModeValues.get() == "BlocksMC"){
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                event.x *= motionSpeedEffectBlocksMC
                event.z *= motionSpeedEffectBlocksMC
            } else {
                event.x *= motionBlocksMC
                event.z *= motionBlocksMC
            }
        }
    }
    val onMotion = handler<MotionEvent> { event ->
        val eventState = event.eventState

        val player = mc.thePlayer ?: return@handler

        isTowering = false

        if (towerModeValues.get() == "None" || notOnMoveValues.get() && player.isMoving ||
            onJumpValues.get() && !mc.gameSettings.keyBindJump.isKeyDown
        ) {
            return@handler
        }

        isTowering = true

        // 添加Matrix模式处理
        if (towerModeValues.get().equals("Matrix", ignoreCase = true)) {
            when (matrixState) {
                0 -> {
                    if (!player.onGround) {
                        matrixState = 1
                    }
                }
                1 -> {
                    if (player.onGround) {
                        matrixState = 2
                    }
                }
                2 -> {
                    if (player.onGround) {
                        // 与Loftily保持一致：触发fake jump并设置motionY
                        fakeJump()
                        player.motionY = 0.42
                    } else if (player.motionY < 0.19) {
                        // 在下落较慢时强制回报为"着地"，并再次给出上升动量
                        event.onGround = true
                        player.motionY = 0.42
                    }
                }
            }

            // Matrix模式独立控制，不继续执行后续通用逻辑
            return@handler
        }

        if (eventState == EventState.POST) {
            tickTimer.update()

            if (!stopWhenBlockAboveValues.get() || BlockPos(player).up(2).block == air) {
                move()
            }

            val blockPos = BlockPos(player).down()

            if (blockPos.block == air) {
                Scaffold.search(blockPos, !shouldGoDown, searchMode == "Area")
            }
        }
    }

    // Handle jump events
    val onJump = handler<JumpEvent> { event ->
        if (onJumpValues.get()) {
            if (Scaffold.scaffoldMode == "GodBridge" && (Scaffold.jumpAutomatically) || !Scaffold.shouldJumpOnInput)
                return@handler
            if (towerModeValues.get() == "None" || towerModeValues.get() == "Jump")
                return@handler
            if (notOnMoveValues.get() && mc.thePlayer.isMoving)
                return@handler
            if (Speed.state || Fly.state)
                return@handler

            event.cancelEvent()
        }
    }

    // Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        mc.thePlayer?.isAirBorne = true
        mc.thePlayer?.triggerAchievement(StatList.jumpStat)
    }

    /**
     * Move player
     */
    private fun move() {
        val player = mc.thePlayer ?: return

        if (blocksAmount() <= 0)
            return

        when (towerModeValues.get().lowercase()) {
            "blocksmc" -> {
                MovementUtils.strafe()

                if (player.onGround) {
                    player.motionY = 0.42
                    fakeJump()

                    // 可选：根据药水加速增强跳跃高度
                    if (player.isPotionActive(Potion.moveSpeed)) {
                        val amplifier = player.getActivePotionEffect(Potion.moveSpeed).amplifier
                        player.motionY += 0.08 * amplifier
                    }

                } else if (player.motionY < 0.1 && player.motionY > -0.1) {
                    player.motionY = -0.0784000015258789
                }

                // 检查脚下是否有方块支撑
                val blockBelow = BlockPos(player.posX, player.posY - 1.4, player.posZ)
            }

            "jump" -> if (player.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                player.tryJump()
            } else if (!player.onGround) {
                player.isAirBorne = false
                tickTimer.reset()
            }

            "motion" -> if (player.onGround) {
                fakeJump()
                player.motionY = 0.42
            } else if (player.motionY < 0.1) {
                player.motionY = -0.3
            }

            // Old Name (Jump)
            "motionjump" -> if (player.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                player.motionY = jumpMotionValues.get().toDouble()
                tickTimer.reset()
            }

            "motiontp" -> if (player.onGround) {
                fakeJump()
                player.motionY = 0.42
            } else if (player.motionY < 0.23) {
                player.setPosition(player.posX, truncate(player.posY), player.posZ)
            }

            "packet" -> if (player.onGround && tickTimer.hasTimePassed(2)) {
                fakeJump()
                sendPackets(
                    C04PacketPlayerPosition(
                        player.posX,
                        player.posY + 0.42,
                        player.posZ,
                        false
                    ),
                    C04PacketPlayerPosition(
                        player.posX,
                        player.posY + 0.753,
                        player.posZ,
                        false
                    )
                )
                player.setPosition(player.posX, player.posY + 1.0, player.posZ)
                tickTimer.reset()
            }

            "teleport" -> {
                if (teleportNoMotionValues.get()) {
                    player.motionY = 0.0
                }
                if ((player.onGround || !teleportGroundValues.get()) && tickTimer.hasTimePassed(
                        teleportDelayValues.get()
                    )
                ) {
                    fakeJump()
                    player.setPositionAndUpdate(
                        player.posX, player.posY + teleportHeightValues.get(), player.posZ
                    )
                    tickTimer.reset()
                }
            }

            "constantmotion" -> {
                if (player.onGround) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    jumpGround = player.posY
                    player.motionY = constantMotionValues.get().toDouble()
                }
                if (player.posY > jumpGround + constantMotionJumpGroundValues.get()) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    player.setPosition(
                        player.posX, truncate(player.posY), player.posZ
                    ) // TODO: toInt() required?
                    player.motionY = constantMotionValues.get().toDouble()
                    jumpGround = player.posY
                }
            }

            "pulldown" -> {
                if (!player.onGround && player.motionY < triggerMotionValues.get()) {
                    player.motionY = -dragMotionValues.get().toDouble()
                } else {
                    fakeJump()
                }
            }

            // Credit: @localpthebest / Nextgen
            "vulcan2.9.0" -> {
                if (player.ticksExisted % 10 == 0) {
                    // Prevent Flight Flag
                    player.motionY = -0.1
                    return
                }

                fakeJump()

                if (player.ticksExisted % 2 == 0) {
                    player.motionY = 0.7
                } else {
                    player.motionY = if (player.isMoving) 0.42 else 0.6
                }
            }

            "aac3.3.9" -> {
                if (player.onGround) {
                    fakeJump()
                    player.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (player.motionY < 0) {
                    player.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> if (player.ticksExisted % 4 == 1) {
                player.motionY = 0.4195464
                player.setPosition(player.posX - 0.035, player.posY, player.posZ)
            } else if (player.ticksExisted % 4 == 0) {
                player.motionY = -0.5
                player.setPosition(player.posX + 0.035, player.posY, player.posZ)
            }

            "prediction" -> {
                if (player.onGround && tickTimer.hasTimePassed(predictionDelayValues.get())) {
                    fakeJump()

                    // 更新预测tick计数
                    predictionTicks++

                    // 计算玩家预测位置
                    val predictX = player.posX + (player.motionX * predictionRangeValues.get())
                    val predictZ = player.posZ + (player.motionZ * predictionRangeValues.get())

                    // 在预测位置放置方块
                    Scaffold.search(BlockPos(predictX, player.posY - 1, predictZ), !shouldGoDown, searchMode == "Area")

                    // 根据predictionTicks调整运动
                    when (predictionTicks) {
                        1 -> {
                            player.motionY = predictionSpeedValues.get().toDouble()
                            if (predictionExtraBlock.get()) {
                                placeExtraBlock = true
                                extraBlockDelay = predictionExtraDelay.get()
                            }
                        }
                        2 -> {
                            // 给予额外的速度以保持上升
                            if (player.motionY < 0.3) {
                                player.motionY = 0.3
                            }
                        }
                        3 -> {
                            // 准备下落
                            if (player.motionY > 0) {
                                player.motionY = 1.0 - player.posY % 1.0
                            }
                            predictionTicks = 0
                        }
                    }

                    // 处理额外方块放置
                    if (placeExtraBlock && extraBlockDelay <= 0) {
                        // 在当前位置下方再放置一个方块
                        Scaffold.search(BlockPos(player), !shouldGoDown, searchMode == "Area")
                        placeExtraBlock = false
                    } else if (placeExtraBlock) {
                        extraBlockDelay--
                    }

                    tickTimer.reset()
                }
            }
            "ncp" -> {
                if (player.onGround) {
                    jumpGround = player.posY
                    player.motionY = 0.42
                    fakeJump()
                }
                if (player.posY > jumpGround + 0.79) {
                    player.setPosition(
                        player.posX,
                        truncate(player.posY),
                        player.posZ
                    )
                    player.motionY = 0.42
                    jumpGround = player.posY
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val packet = event.packet

        if (packet is C08PacketPlayerBlockPlacement) {
            // c08 item override to solve issues in scaffold and some other modules, maybe bypass some anticheat in future\
            ReflectionUtil.setFieldValue(packet, "stack", mc.thePlayer.inventory.mainInventory[mc.thePlayer.inventory.currentItem])
            // illegal facing checks
            ReflectionUtil.setFieldValue(
                packet,
                "facingX",
                (ReflectionUtil.getFieldValue<Float>(packet, "facingX")).coerceIn(-1.0f..1.0f)
            )
            ReflectionUtil.setFieldValue(
                packet,
                "facingY",
                (ReflectionUtil.getFieldValue<Float>(packet, "facingY")).coerceIn(-1.0f..1.0f)
            )
            ReflectionUtil.setFieldValue(
                packet,
                "facingZ",
                (ReflectionUtil.getFieldValue<Float>(packet, "facingZ")).coerceIn(-1.0f..1.0f)
            )
            if (towerModeValues.equals("BlocksMC") && isTowering) {
                if (mc.thePlayer.motionY > -0.0784000015258789) {
                    if (packet.position.equals(
                            BlockPos(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY - 1.4,
                                mc.thePlayer.posZ
                            )
                        )
                    ) {
                        mc.thePlayer.motionY = -0.0784000015258789
                    }
                }
            }
        }
        if (towerModeValues.get() == "Vulcan2.9.0" && packet is C04PacketPlayerPosition &&
            !player.isMoving && player.ticksExisted % 2 == 0
        ) {
            packet.x += 0.1
            packet.z += 0.1
        }
    }

    override fun handleEvents() = Scaffold.handleEvents()
}