/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C0APacketAnimation
import kotlin.random.Random

object EvoCriticals : Module("EvoCriticals", Category.SKID) {

    val mode by choices(
        "Mode",
        arrayOf(
            "Packet",
            "NCPPacket",
            "BlocksMC",
            "BlocksMC2",
            "NoGround",
            "Hop",
            "TPHop",
            "Jump",
            "LowJump",
            "CustomMotion",
            "Visual",
            "MatrixV1",
            "MatrixV2",
            "MatrixDamage",
            "MatrixSmart",
            "MatrixSemi",
            "AutoFreeze",
            "AutoSpeed"
        ),
        "Packet"
    )

    val delay by int("Delay", 0, 0..500)
    private val hurtTime by int("HurtTime", 10, 0..10)
    private val customMotionY by float("Custom-Y", 0.2f, 0.01f..0.42f) { mode == "CustomMotion" }

    val msTimer = MSTimer()
    private var attacks = 0
    private var attacking = false
    private var stuckEnabled = false
    
    // AutoFreeze 模式相关变量
    private var freezeX = 0.0
    private var freezeY = 0.0
    private var freezeZ = 0.0
    private var freezeMotionX = 0.0
    private var freezeMotionY = 0.0
    private var freezeMotionZ = 0.0
    private var isJumping = false

    override fun onEnable() {
        if (mode == "NoGround")
            mc.thePlayer.tryJump()
        if (mode == "MatrixSmart" || mode == "MatrixSemi")
            attacks = 0
    }

    val onAttack = handler<AttackEvent> { event ->
        if (event.targetEntity is EntityLivingBase) {
            val thePlayer = mc.thePlayer ?: return@handler
            val entity = event.targetEntity

            if (mode != "MatrixV1" && mode != "MatrixV2" && mode != "MatrixDamage" && mode != "MatrixSmart" && mode != "MatrixSemi" && (!thePlayer.onGround || thePlayer.isOnLadder || thePlayer.isInWeb || thePlayer.isInLiquid ||
                thePlayer.ridingEntity != null || entity.hurtTime > hurtTime ||
                Fly.handleEvents() || !msTimer.hasTimePassed(delay)))
                return@handler

            // MatrixV1, MatrixV2, MatrixDamage, MatrixSmart and MatrixSemi have different conditions - only check hurtTime and delay
            if ((mode == "MatrixV1" || mode == "MatrixV2" || mode == "MatrixDamage" || mode == "MatrixSmart" || mode == "MatrixSemi") && (entity.hurtTime > hurtTime || !msTimer.hasTimePassed(delay)))
                return@handler

            val (x, y, z) = thePlayer

            when (mode.lowercase()) {
                "packet" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.0625, z, true),
                        C04PacketPlayerPosition(x, y, z, false)
                    )
                    thePlayer.onCriticalHit(entity)
                }

                "ncppacket" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.11, z, false),
                        C04PacketPlayerPosition(x, y + 0.1100013579, z, false),
                        C04PacketPlayerPosition(x, y + 0.0000013579, z, false)
                    )
                    mc.thePlayer.onCriticalHit(entity)
                }

                "blocksmc" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.001091981, z, true),
                        C04PacketPlayerPosition(x, y, z, false)
                    )
                }

                "blocksmc2" -> {
                    if (thePlayer.ticksExisted % 4 == 0) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.0011, z, true),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }
                }

                "hop" -> {
                    thePlayer.motionY = 0.1
                    thePlayer.fallDistance = 0.1f
                    thePlayer.onGround = false
                }

                "tphop" -> {
                    sendPackets(
                        C04PacketPlayerPosition(x, y + 0.02, z, false),
                        C04PacketPlayerPosition(x, y + 0.01, z, false)
                    )
                    thePlayer.setPosition(x, y + 0.01, z)
                }

                "jump" -> thePlayer.motionY = 0.42
                "lowjump" -> thePlayer.motionY = 0.3425
                "custommotion" -> thePlayer.motionY = customMotionY.toDouble()
                "visual" -> thePlayer.onCriticalHit(entity)
                "matrixv1" -> {
                    // MatrixV1 思路：在攻击包前快速下发 3 个“小跳”位移动作包（-1E-4）
                    val x = thePlayer.posX
                    val y = thePlayer.posY
                    val z = thePlayer.posZ

                    // 三连小跳
                    sendPackets(
                        C04PacketPlayerPosition(x, y - 1E-4, z, false),
                        C04PacketPlayerPosition(x, y - 1E-4, z, false),
                        C04PacketPlayerPosition(x, y - 1E-4, z, false)
                    )

                    // 拉回真坐标
                    sendPackets(
                        C04PacketPlayerPosition(x, y, z, true)
                    )
                }
                "matrixv2" -> {
                    // MatrixV2 思路：
                    // 1. 若在地面上，先挥一次手 发一个 -0.001 的假坐标 再挥一次手
                    // 2. 若在空中且 fallDistance < 0.3，走“假 TP”流程
                    val x = thePlayer.posX
                    val y = thePlayer.posY
                    val z = thePlayer.posZ
                    val onGround = thePlayer.onGround

                    if (onGround) {
                        // 地面快速“微跳” - 先挥手，发假坐标，再挥手
                        sendPackets(
                            C0APacketAnimation(),
                            C04PacketPlayerPosition(x, y - 0.001, z, false),
                            C0APacketAnimation()
                        )
                    } else if (thePlayer.fallDistance < 0.3f) {
                        // 空中假 TP
                        val fakeX = x + 1000 + Random.nextDouble() * 10000
                        val fakeZ = z + 1000 + Random.nextDouble() * 10000

                        // 发送假TP包
                        sendPackets(
                            C04PacketPlayerPosition(fakeX, y, fakeZ, false),
                            C04PacketPlayerPosition(x, y - 0.06, z, false)
                        )

                        // 本地设置motionY并限制速度
                        thePlayer.motionY = -0.078
                    }
                }
                "matrixdamage" -> {
                    // MatrixDamage 思路：
                    // 1. 若在地面上：+0.023  +0.011（两段小跳）
                    // 2. 若在空气中：-0.011 并给本地 -0.08 向下速度
                    // 3. 调用 MoveUtil.stop2() 瞬间刹停，保证“落地”瞬间打出暴击
                    val x = thePlayer.posX
                    val y = thePlayer.posY
                    val z = thePlayer.posZ
                    val onGround = thePlayer.onGround

                    if (onGround) {
                        // 地面两段小跳
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.023, z, false),
                            C04PacketPlayerPosition(x, y + 0.011, z, false)
                        )
                    } else {
                        // 空中 -0.011 并设置本地向下速度
                        sendPackets(
                            C04PacketPlayerPosition(x, y - 0.011, z, onGround)
                        )
                        thePlayer.motionY = -0.08
                    }
                    // 瞬间刹停，保证落地瞬间暴击
                    // 注意：这里可能需要调用停止移动的方法，但由于模块间依赖关系，
                    // 我们将motionX和motionZ设为0来模拟刹停效果
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                }
                "matrixsmart" -> {
                    // MatrixSmart 模式：根据攻击次数来决定是否发送暴击包
                    attacks++
                    if (attacks > 3) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.110314, z, false),
                            C04PacketPlayerPosition(x, y + 0.0200081, z, false),
                            C04PacketPlayerPosition(x, y + 0.00000001300009, z, false),
                            C04PacketPlayerPosition(x, y + 0.000000000022, z, false),
                            C04PacketPlayerPosition(x, y, z, true)
                        )
                        attacks = 0
                    }
                }
                "matrixsemi" -> {
                    // MatrixSemi 模式：根据攻击次数来决定是否发送暴击包
                    attacks++
                    if (attacks > 3) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.0825080378093, z, false),
                            C04PacketPlayerPosition(x, y + 0.023243243674, z, false),
                            C04PacketPlayerPosition(x, y + 0.0215634532004, z, false),
                            C04PacketPlayerPosition(x, y + 0.00150000001304, z, false)
                        )
                        attacks = 0
                    }
                }
                "autofreeze" -> {
                    attacking = true
                    // 在地面上时跳跃
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                        // 记录跳跃开始状态
                        freezeX = mc.thePlayer.posX
                        freezeY = mc.thePlayer.posY
                        freezeZ = mc.thePlayer.posZ
                        freezeMotionX = mc.thePlayer.motionX
                        freezeMotionY = mc.thePlayer.motionY
                        freezeMotionZ = mc.thePlayer.motionZ
                        isJumping = true
                    }
                }
                "autospeed" -> {
                    attacking = true
                    if (mc.thePlayer.onGround && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava) {
                        mc.thePlayer.jump()
                    }
                }
            }

            msTimer.reset()
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is C03PacketPlayer && mode == "NoGround")
            packet.onGround = false
    }

    val onUpdate = handler<UpdateEvent> {
        when (mode.lowercase()) {
            "autofreeze" -> {
                val killAura = ModuleManager["KillAura"] as? net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
                if (killAura?.target != null && mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
                if (mc.thePlayer.fallDistance > 0f) {
                    stuckEnabled = true
                }
                if (killAura?.target == null && stuckEnabled) {
                    stuckEnabled = false
                }
            }
            "autospeed" -> {
                val killAura = ModuleManager["KillAura"] as? net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
                if (killAura?.target != null) {
                    val speed = ModuleManager["Speed"] as? net.ccbluex.liquidbounce.features.module.modules.movement.Speed
                    if (speed?.state == false) {
                        speed?.state = true
                    }
                    if (mc.thePlayer.onGround && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava) {
                        mc.thePlayer.jump()
                    }
                } else {
                    val speed = ModuleManager["Speed"] as? net.ccbluex.liquidbounce.features.module.modules.movement.Speed
                    if (speed?.state == true) {
                        speed?.state = false
                    }
                }
            }
        }
    }

    override val tag
        get() = mode
}