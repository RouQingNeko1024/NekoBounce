/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.firefly.general

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.combat.KeepSprint
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.utils.attack.CPSCounter
import net.ccbluex.liquidbounce.utils.client.PacketUtils
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.extensions.attackEntityWithModifiedSprint
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.inventory.attackDamage
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.potion.Potion
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

@Suppress("UnusedReceiverParameter")
object SomeUtil {
    val mc = Minecraft.getMinecraft()!!

    /**
     * 更强的Round函数
     */
    fun roundToPlacesIfNeeded(value: Double, places: Int? = 5): Double {
        val scale = places ?: 5
        fun isAlreadyRounded(value: Double, scale: Int): Boolean {
            if (value.isNaN() || value.isInfinite()) return true

            val factor = 10.0.pow(scale.coerceIn(0, 15))
            val scaled = value * factor

            if (!scaled.isFinite()) return true

            val rounded = round(scaled)
            val difference = abs(scaled - rounded)

            return difference < 1e-8
        }
        return when {
            abs(value) < 1e-14 -> value
            abs(value - 1.0) < 1e-14 -> value
            value.isNaN() || value.isInfinite() -> value
            isAlreadyRounded(value, scale) -> value
            else -> {
                val factor = 10.0.pow(scale.coerceIn(0, 15))
                val scaled = value * factor

                if (scaled.isFinite()) {
                    round(scaled) / factor
                } else {
                    value
                }
            }
        }

    }

    /**
     * 效果类似reduceY函数,区别在于这个函数针对于MotionXZ,
     * factor为衰减乘数,允许>1来达到增加XZMotion的效果,
     * hurtTime形参用于控制在特定时机内触发代码,
     * setScale形参用于控制你希望这个可能带小数的factor的小数点后最大位数为多少.
     */
    fun reduceXZ(factor: Double, hurtTime: IntRange? = null, setScale: Int? = null) {
        val player = mc.thePlayer ?: return
        if (hurtTime == null || player.hurtTime in hurtTime) {
            val adjustedFactor = roundToPlacesIfNeeded(factor, setScale)
            player.motionX *= adjustedFactor
            player.motionZ *= adjustedFactor
        }
    }

    /**
     * 效果类似reduceXZ函数,区别在于这个函数针对于MotionY,
     * factor为衰减乘数,允许>1来达到增加YMotion的效果,
     * hurtTime形参用于控制在特定时机内触发代码,
     * setScale形参用于控制你希望这个可能带小数的factor的小数点后最大位数为多少.
     */
    fun reduceY(factor: Double, hurtTime: IntRange? = null, setScale: Int? = null) {
        val player = mc.thePlayer ?: return
        if (hurtTime == null || player.hurtTime in hurtTime) {
            val adjustedFactor = roundToPlacesIfNeeded(factor, setScale)
            player.motionY *= adjustedFactor
        }
    }

    /**
     * 直接覆盖设置玩家目前的MotionXYZ
     */
    fun setMotion(
        xMotion: Double? = null,
        yMotion: Double? = null,
        zMotion: Double? = null
    ) {
        xMotion?.let { mc.thePlayer.motionX = it }
        yMotion?.let { mc.thePlayer.motionY = it }
        zMotion?.let { mc.thePlayer.motionZ = it }
    }

    /**
     * 此函数用于快捷修改玩家的疾跑状态
     * setState顾名思义就是控制你想让此函数达到的目标,
     * sendPacketToServer就是控制你想让这次修改是否额外发送Packet至服务器,
     * forceChange就是是否强制改变至<setState>的状态
     */
    fun changeSprint(setState: Boolean, sendPacketToServer: Boolean = true, forceChange: Boolean = false) {
        val player = mc.thePlayer ?: return
        
        if (forceChange) {
            player.isSprinting = setState
            if (sendPacketToServer) {
                PacketUtils.sendPacket(
                    C0BPacketEntityAction(
                        player,
                        if (setState) C0BPacketEntityAction.Action.START_SPRINTING
                        else C0BPacketEntityAction.Action.STOP_SPRINTING
                    )
                )
            }
            return
        }
        
        player.isSprinting = setState
        
        if (!sendPacketToServer) return

        PacketUtils.sendPacket(
            C0BPacketEntityAction(
                player,
                if (setState) C0BPacketEntityAction.Action.START_SPRINTING
                else C0BPacketEntityAction.Action.STOP_SPRINTING
            )
        )
    }

    fun changeTimer(speed: Float) {
        mc.timer.timerSpeed = speed
    }

    fun keepingSprint(): Boolean {
        val player = mc.thePlayer ?: return false
        
        // 检查KillAura中是否有保持疾跑的设置
        val shouldKeepSprint = try {
            val field = KillAura::class.java.getDeclaredField("shouldKeepSprint")
            field.isAccessible = true
            field.getBoolean(KillAura)
        } catch (e: Exception) {
            false
        }
        
        if (shouldKeepSprint) return true
        
        // 检查KeepSprint模块是否启用
        val keepSprintModule = try {
            val managerClass = Class.forName("net.ccbluex.liquidbounce.features.module.ModuleManager")
            val getModuleMethod = managerClass.getMethod("getModule", Class::class.java)
            getModuleMethod.invoke(null, KeepSprint::class.java)
        } catch (e: Exception) {
            null
        }
        
        if (keepSprintModule == null) {
            return false
        }
        
        // 检查模块状态 - 修复这里的问题
        val isModuleEnabled = try {
            val stateMethod = keepSprintModule.javaClass.getMethod("state")
            stateMethod.invoke(keepSprintModule) as Boolean
        } catch (e: Exception) {
            false
        }
        
        if (!isModuleEnabled) {
            return false
        }
        
        // 检查hurtTime范围
        if (player.hurtTime <= 0 || player.hurtTime > 10) return false
        
        if (player.isBurning) return false
        if (player.isInWater) return false
        if (player.isInLava) return false
        if (player.isInWeb) return false
        if (player.isPotionActive(Potion.moveSlowdown)) return false
        if (player.isPotionActive(Potion.moveSpeed)) return false
        
        return player.isSprinting
    }

    /**
     * 这是一个通用的攻击函数,大多数参数保持默认即可合法攻击,可以在外部直接调用runAttack()
     * @param keepSprint 用于控制是否在执行攻击的时候保持疾跑并且cancel掉HitSlow, true为保持疾跑
     * @param maxDistance 控制你与目标之间的最大距离, 默认3.0F
     * @param attackCount 控制此函数这次执行的攻击次数, 默认1
     * @param attackTarget 是否选定一个已在外部定义的目标,若想要不被检测需要注意检测玩家准心是否瞄准了外部所传入的Target, null时使用鼠标悬停实体
     * @param ignoreBlocking 是否允许在自身格挡中时执行攻击, true为允许攻击(无视格挡), false为不允许攻击(尊重格挡), 默认为true
     * @param swingMode 有"Packet","Normal","Off"三个参数可供传入, Packet就是在客户端层面不显示攻击,Normal就是都显示,Off都不显示, 默认"Packet"
     * @param fakeSwing 是否在目标与玩家超出最大攻击距离的时候仅仅执行挥手而不攻击, true为执行虚假挥动
     * @param debugMessage 是否在攻击执行以后直接在聊天栏打印消息, true为显示调试消息
     * @param debugMessageString 调整这个消息的内容,可供自定义, 默认"Attacked"
     * @param silentAttack 是否不触发AttackEvent攻击, true为静默攻击
     * @param extraReduceXZ 是否在点击同时进行一次XZReduce, null为不减少
     * @param extraReduceY 是否在点击同时进行一次YReduce, null为不减少
     * @return Boolean 是否成功执行了至少一次实际攻击(命中目标), true为攻击成功, false为攻击失败或未攻击
     **/
    fun runAttack(
        keepSprint: Boolean = false,
        maxDistance: Float = 3.0F,
        attackCount: Int = 1,
        attackTarget: Entity? = null,
        ignoreBlocking: Boolean? = true,
        swingMode: String? = "Packet",
        fakeSwing: Boolean? = false,
        debugMessage: Boolean = false,
        debugMessageString: String = "Attacked",
        silentAttack: Boolean = false,
        extraReduceXZ: Double? = null,
        extraReduceY: Double? = null,
        attackChance: Float = 1.0f,
    ): Boolean {
        var trulyAttack = 0
        val shouldSwingNormal = swingMode == "Normal"
        val shouldSwingPacket = swingMode == "Packet" || swingMode == null
        val shouldSwingOff = swingMode == "Off"

        val swingAction = {
            when {
                shouldSwingNormal -> mc.thePlayer.swingItem()
                shouldSwingPacket -> PacketUtils.sendPacket(C0APacketAnimation())
                shouldSwingOff -> null
            }
        }

        val target = attackTarget ?: mc.objectMouseOver?.entityHit

        if (target == null) {
            if (fakeSwing == true) swingAction()
            return false
        }

        val distance = mc.thePlayer.getDistanceToEntityBox(target)
        val withinRange = distance < maxDistance

        val playerIsBlocking = mc.thePlayer.isBlocking
        val shouldIgnoreBlock = ignoreBlocking ?: true

        if (playerIsBlocking && !shouldIgnoreBlock) {
            return false
        }

        var attackPerformed = false

        if (withinRange) {
            repeat(attackCount) {
                if (RandomUtils.nextFloat() <= attackChance) return@repeat
                
                // 直接攻击，不使用静默攻击管理器
                mc.thePlayer.attackEntityWithModifiedSprint(target, !keepSprint) {
                    swingAction()
                }
                
                if (extraReduceXZ != null) reduceXZ(extraReduceXZ)
                if (extraReduceY != null) reduceY(extraReduceY)
                
                CPSCounter.registerClick(CPSCounter.MouseButton.LEFT)
                attackPerformed = true
                trulyAttack++
            }

            if (debugMessage) chat("$debugMessageString x$trulyAttack")
            return attackPerformed
        } else if (fakeSwing == true) swingAction()
        return false
    }
    
    fun EntityPlayerSP.isHurting(checkPacket: Boolean? = null, event: PacketEvent? = null): Boolean {
        val player = mc.thePlayer ?: return false

        return if (checkPacket == true && event != null) {
            val packet = event.packet
            packet is S12PacketEntityVelocity && packet.entityID == player.entityId && player.hurtTime > 0
        } else {
            player.hurtTime > 0
        }
    }

    fun EntityPlayerSP.isFalling(): Boolean {
        return !mc.thePlayer.onGround && mc.thePlayer.motionY < 0.0
    }

    fun EntityPlayerSP.isInBadEnvironment(): Boolean {
        return mc.thePlayer.isInWeb ||
                mc.thePlayer.isInLava ||
                mc.thePlayer.isBurning ||
                mc.thePlayer.isInWater ||
                mc.thePlayer.isRiding ||
                mc.thePlayer.isRidingHorse
    }

    val EntityPlayerSP.bps: Double
        get() {
            val player = mc.thePlayer ?: return 0.0
            return sqrt((player.motionX * player.motionX + player.motionZ * player.motionZ)) * 20.0
        }

    val EntityPlayerSP.bpt: Double
        get() {
            val player = mc.thePlayer ?: return 0.0
            return sqrt((player.motionX * player.motionX + player.motionZ * player.motionZ))
        }
        
    val EntityPlayerSP.velocityX: Double
        get() = mc.thePlayer?.motionX ?: 0.0
        
    val EntityPlayerSP.velocityY: Double
        get() = mc.thePlayer?.motionY ?: 0.0
        
    val EntityPlayerSP.velocityZ: Double
        get() = mc.thePlayer?.motionZ ?: 0.0
        
    fun EntityPlayerSP.setBPSTo(targetBPS: Double) {
        val player = mc.thePlayer ?: return
        val currentBPS = player.bps
        if (currentBPS != 0.0) {
            reduceXZ((targetBPS / currentBPS))
        }
    }

    /**
     * 计算玩家当前武器的理论伤害
     * 包括基础伤害、武器伤害和锋利附魔伤害
     * @return 理论伤害值
     */
    fun getCurrentWeaponDamage(isCritical: Boolean): Double {
        val player = mc.thePlayer ?: return 1.0
        val heldItem = player.heldItem ?: return 1.0
        var damage = 1.0 + heldItem.attackDamage
        if (isCritical) {
            damage = damage * 1.5
        }

        val strengthEffect = player.getActivePotionEffect(Potion.damageBoost)
        if (strengthEffect != null) {
            val amplifier = strengthEffect.amplifier
            val strengthMultiplier = 1.0 + (amplifier + 1) * 1.3
            damage *= strengthMultiplier
        }

        val weaknessEffect = player.getActivePotionEffect(Potion.weakness)
        if (weaknessEffect != null) {
            damage -= (weaknessEffect.amplifier + 1) * 0.5
        }

        return damage.coerceAtLeast(0.5)
    }

    fun EntityPlayerSP.safeJump(jumpStrength: Double = 0.2) {
        if (!mc.thePlayer.onGround || mc.gameSettings.keyBindJump.isKeyDown) return
        mc.thePlayer.jumpMovementFactor = jumpStrength.toFloat()
        mc.thePlayer.jump()
        mc.thePlayer.jumpMovementFactor = 0.2f
    }
    
    /**
     * 计算实体运动方向与实体面向方向之间的角度差（以度为单位）
     */
    fun calculateAngleDifference(entity: EntityLivingBase = mc.thePlayer): Double {
        val motionX = entity.motionX
        val motionZ = entity.motionZ
        val playerYaw = entity.rotationYaw
        
        val movementAngle = Math.toDegrees(atan2(motionZ, motionX))
        val normalizedPlayerYaw = (playerYaw % 360 + 360) % 360
        
        var angleDifference = abs(movementAngle - normalizedPlayerYaw)
        
        if (angleDifference > 180) {
            angleDifference = 360 - angleDifference
        }
        
        return angleDifference
    }
}