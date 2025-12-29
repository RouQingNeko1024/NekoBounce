package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.potion.Potion
import kotlin.math.round

object TeleportAura : Module("TeleportAura", Category.COMBAT) {

    private val mode by choices("Mode", arrayOf("Single", "Multiple"), "Single")
    private val range by float("Range", 32f, 3f..100f)
    private val minCPS by int("MinCPS", 10, 1..20)
    private val maxCPS by int("MaxCPS", 15, 1..20)
    private val render by boolean("Render", true)
    
    // 目标筛选选项
    private val players by boolean("Players", true)
    private val animals by boolean("Animals", false)
    private val mobs by boolean("Mobs", false)
    private val invisible by boolean("Invisible", false)
    private val throughWalls by boolean("ThroughWalls", false)

    private val clickTimer = MSTimer()
    private var nextSwing = 0L
    private var target: EntityLivingBase? = null
    private var path: List<Vec3>? = null

    override fun onDisable() {
        target = null
        path = null
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        
        // 获取范围内的目标
        val targets = getTargetsInRange(range)
        
        if (targets.isEmpty()) {
            target = null
            return@handler
        }

        // 选择最近的目标
        target = targets.minByOrNull { player.getDistanceToEntity(it) }

        if (target == null || player.isDead) {
            return@handler
        }

        // 执行攻击
        doAttack(targets)
    }

    val onRender3D = handler<Render3DEvent> {
        if (!render || path == null || target == null) {
            return@handler
        }

        // 这里可以添加路径渲染逻辑
        // 使用 LiquidBounce 的渲染工具绘制路径线
    }

    /**
     * 获取范围内的有效目标
     */
    private fun getTargetsInRange(range: Float): List<EntityLivingBase> {
        val player = mc.thePlayer ?: return emptyList()
        val world = mc.theWorld ?: return emptyList()
        
        val targets = mutableListOf<EntityLivingBase>()

        for (entity in world.loadedEntityList) {
            if (entity !is EntityLivingBase) continue
            if (entity == player) continue
            if (entity.isDead) continue
            if (entity.health <= 0) continue
            
            // 距离检查
            val distance = player.getDistanceToEntity(entity)
            if (distance > range) continue
            
            // 视线检查（如果启用）
            if (!throughWalls && !player.canEntityBeSeen(entity)) continue
            
            // 目标类型筛选
            when (entity) {
                is EntityPlayer -> {
                    if (!players) continue
                    if (entity.isInvisible && !invisible) continue
                    // 好友检查 - 使用 LiquidBounce 的 FriendManager
                    // if (FriendManager.isFriend(entity.name)) continue
                }
                is EntityAnimal -> if (!animals) continue
                is EntityMob -> if (!mobs) continue
                else -> continue // 跳过其他实体类型
            }
            
            targets.add(entity)
        }
        
        return targets
    }

    private fun doAttack(targets: List<EntityLivingBase>) {
        val player = mc.thePlayer ?: return
        
        if (!clickTimer.hasTimePassed(nextSwing) || target == null || 
            mc.gameSettings.keyBindAttack.isKeyDown || mc.gameSettings.keyBindUseItem.isKeyDown) {
            return
        }

        val clicks = round(random(minCPS.toDouble(), maxCPS.toDouble())).toLong()
        nextSwing = 1000 / clicks

        when (mode) {
            "Single" -> {
                if (player.getDistanceToEntity(target!!) <= range) {
                    attack(target!!)
                }
            }
            "Multiple" -> {
                val validTargets = targets.filter { player.getDistanceToEntity(it) <= range }
                if (validTargets.isNotEmpty()) {
                    validTargets.forEach { attack(it) }
                }
            }
        }

        clickTimer.reset()
    }

    private fun attack(target: EntityLivingBase) {
        val player = mc.thePlayer ?: return
        
        // 同步当前物品
        mc.playerController.syncCurrentPlayItem()

        // 创建攻击事件
        val attackEvent = AttackEvent(target)
        // 如果需要事件系统，可以在这里调用 EventManager.call(attackEvent)

        // 如果事件被取消则返回
        // if (attackEvent.isCancelled) return

        // 计算路径（简化版本，使用直线路径）
        val startPos = Vec3(player.posX, player.posY, player.posZ)
        val targetPos = Vec3(target.posX, target.posY, target.posZ)
        
        // 创建路径点（可以添加中间点来模拟真实路径）
        path = listOf(startPos, targetPos)

        if (path == null) {
            return
        }

        // 传送至目标位置
        for (vector in path!!) {
            sendPacket(C03PacketPlayer.C04PacketPlayerPosition(vector.x, vector.y, vector.z, true))
        }

        // 触发挥动动画
        player.swingItem()

        // 发送攻击包
        sendPacket(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))

        // 反转路径并传送回来
        val reversedPath = path!!.reversed()
        for (vector in reversedPath) {
            sendPacket(C03PacketPlayer.C04PacketPlayerPosition(vector.x, vector.y, vector.z, true))
        }

        // 暴击检测
        if (player.fallDistance > 0 && !player.onGround && 
            !player.isOnLadder && !player.isInWater && 
            !player.isPotionActive(Potion.blindness) && 
            player.ridingEntity == null) {
            player.onCriticalHit(target)
        }
    }

    // 辅助数据类
    private data class Vec3(val x: Double, val y: Double, val z: Double)

    // 随机数生成
    private fun random(min: Double, max: Double): Double {
        return min + (Math.random() * (max - min))
    }
}