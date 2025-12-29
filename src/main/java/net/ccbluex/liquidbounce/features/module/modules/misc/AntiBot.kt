/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.angleDifference
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.toRotation
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.server.*
import net.minecraft.potion.Potion
import java.lang.reflect.Field
import java.util.*
import kotlin.math.abs
import kotlin.math.sqrt

object AntiBot : Module("AntiBot", Category.MISC) {

    // 基本检测选项
    private val tab by boolean("Tab", true)
    private val tabMode by choices("TabMode", arrayOf("Equals", "Contains"), "Contains") { tab }

    private val entityID by boolean("EntityID", true)
    private val invalidUUID by boolean("InvalidUUID", true)
    private val color by boolean("Color", false)

    private val livingTime by boolean("LivingTime", false)
    private val livingTimeTicks by int("LivingTimeTicks", 40, 1..200) { livingTime }

    private val capabilities by boolean("Capabilities", true)
    private val ground by boolean("Ground", true)
    private val air by boolean("Air", false)
    private val invalidGround by boolean("InvalidGround", true)
    private val invalidSpeed by boolean("InvalidSpeed", false)
    private val swing by boolean("Swing", false)
    private val health by boolean("Health", false)
    private val derp by boolean("Derp", true)
    private val wasInvisible by boolean("WasInvisible", false)
    private val armor by boolean("Armor", false)
    private val ping by boolean("Ping", false)
    private val needHit by boolean("NeedHit", false)
    private val duplicateInWorld by boolean("DuplicateInWorld", false)
    private val duplicateInTab by boolean("DuplicateInTab", false)
    private val duplicateProfile by boolean("DuplicateProfile", false)
    private val properties by boolean("Properties", false)

    private val alwaysInRadius by boolean("AlwaysInRadius", false)
    private val alwaysRadius by float("AlwaysInRadiusBlocks", 20f, 3f..30f) { alwaysInRadius }
    private val alwaysRadiusTick by int("AlwaysInRadiusTick", 50, 1..100) { alwaysInRadius }

    private val alwaysBehind by boolean("AlwaysBehind", false)
    private val alwaysBehindRadius by float("AlwaysBehindInRadiusBlocks", 10f, 3f..30f) { alwaysBehind }
    private val behindRotDiffToIgnore by float("BehindRotationDiffToIgnore", 90f, 1f..180f) { alwaysBehind }

    // Matrix 模式特有选项
    private val matrixMode by boolean("MatrixMode", false)
    
    // 原始检测列表
    private val groundList = mutableSetOf<Int>()
    private val airList = mutableSetOf<Int>()
    private val invalidGroundList = mutableMapOf<Int, Int>()
    private val invalidSpeedList = mutableSetOf<Int>()
    private val swingList = mutableSetOf<Int>()
    private val invisibleList = mutableListOf<Int>()
    private val propertiesList = mutableSetOf<Int>()
    private val hitList = mutableSetOf<Int>()
    private val notAlwaysInRadiusList = mutableSetOf<Int>()
    private val alwaysBehindList = mutableSetOf<Int>()
    private val worldPlayerNames = mutableSetOf<String>()
    private val worldDuplicateNames = mutableSetOf<String>()
    private val tabPlayerNames = mutableSetOf<String>()
    private val tabDuplicateNames = mutableSetOf<String>()
    private val entityTickMap = mutableMapOf<Int, Int>()

    // Matrix 模式检测列表
    private val matrixSuspectList = hashSetOf<UUID>()
    private val matrixBotList = hashSetOf<UUID>()
    private val matrixWaitingArmor = hashMapOf<UUID, List<ItemStack>>()

    val botList = mutableSetOf<UUID>()

    fun isBot(entity: EntityLivingBase): Boolean {
        // Check if entity is a player
        if (entity !is EntityPlayer)
            return false

        // Check if anti bot is enabled
        if (!handleEvents())
            return false

        // Matrix 模式检测
        if (matrixMode && matrixBotList.contains(entity.uniqueID))
            return true

        // 原始检测逻辑
        if (color && "§" !in entity.displayName.unformattedText.replace("§r", ""))
            return true

        if (livingTime && entity.ticksExisted < livingTimeTicks)
            return true

        if (ground && entity.entityId !in groundList)
            return true

        if (air && entity.entityId !in airList)
            return true

        if (swing && entity.entityId !in swingList)
            return true

        if (health && (entity.health > 20F || entity.health < 0F))
            return true

        if (entityID && (entity.entityId >= 1000000000 || entity.entityId <= 0))
            return true

        if (derp && (entity.rotationPitch > 90F || entity.rotationPitch < -90F))
            return true

        if (wasInvisible && entity.entityId in invisibleList)
            return true

        if (properties && entity.entityId !in propertiesList)
            return true

        if (armor) {
            if (entity.inventory.armorInventory[0] == null && entity.inventory.armorInventory[1] == null &&
                entity.inventory.armorInventory[2] == null && entity.inventory.armorInventory[3] == null
            )
                return true
        }

        if (ping) {
            if (entity.getPing() == 0) return true
        }

        if (invalidUUID && mc.netHandler.getPlayerInfo(entity.uniqueID) == null) {
            return true
        }

        if (capabilities && (entity.isSpectator || entity.capabilities.isFlying || entity.capabilities.allowFlying
                    || entity.capabilities.disableDamage || entity.capabilities.isCreativeMode)
        )
            return true

        if (invalidSpeed && entity.entityId in invalidSpeedList)
            return true

        if (needHit && entity.entityId !in hitList)
            return true

        if (invalidGround && invalidGroundList.getOrDefault(entity.entityId, 0) >= 10)
            return true

        if (alwaysInRadius && entity.entityId !in notAlwaysInRadiusList)
            return true

        if (alwaysBehind && entity.entityId in alwaysBehindList)
            return true

        if (duplicateProfile) {
            return mc.netHandler.playerInfoMap.count {
                it.gameProfile.name == entity.gameProfile.name
                        && it.gameProfile.id != entity.gameProfile.id
            } == 1
        }

        if (duplicateInWorld) {
            for (player in mc.theWorld.playerEntities.filterNotNull()) {
                val playerName = player.name

                if (worldPlayerNames.contains(playerName)) {
                    worldDuplicateNames.add(playerName)
                } else {
                    worldPlayerNames.add(playerName)
                }
            }

            if (worldDuplicateNames.isNotEmpty()) {
                return mc.theWorld.playerEntities.count { it.name in worldDuplicateNames } > 1
            }
        }

        if (duplicateInTab) {
            for (networkPlayerInfo in mc.netHandler.playerInfoMap.filterNotNull()) {
                val playerName = stripColor(networkPlayerInfo.displayName.formattedText)

                if (tabPlayerNames.contains(playerName)) {
                    tabDuplicateNames.add(playerName)
                } else {
                    tabPlayerNames.add(playerName)
                }
            }

            if (tabDuplicateNames.isNotEmpty()) {
                return mc.netHandler.playerInfoMap.count { stripColor(it.displayName.formattedText) in tabDuplicateNames } > 1
            }
        }

        if (tab) {
            val equals = tabMode == "Equals"
            val targetName = stripColor(entity.displayName.formattedText)

            val shouldReturn = mc.netHandler.playerInfoMap.any { networkPlayerInfo ->
                val networkName = stripColor(networkPlayerInfo.displayName.formattedText)
                if (equals) {
                    targetName == networkName
                } else {
                    networkName in targetName
                }
            }
            return !shouldReturn
        }

        return entity.name.isEmpty() || entity.name == mc.thePlayer.name
    }

    val onUpdate = handler<UpdateEvent>(always = true) {
        val world = mc.theWorld ?: return@handler

        // Matrix 模式 tick 处理
        if (matrixMode) {
            handleMatrixTick()
        }

        world.loadedEntityList.forEach { entity ->
            if (entity !is EntityPlayer) return@forEach
            val profile = entity.gameProfile ?: return@forEach

            if (isBot(entity)) {
                if (profile.id !in botList) {
                    botList += profile.id
                }
            } else {
                if (profile.id in botList) {
                    botList -= profile.id
                }
            }
        }
    }

    val onPacket = handler<PacketEvent>(always = true) { event ->
        if (mc.thePlayer == null || mc.theWorld == null)
            return@handler

        val packet = event.packet

        // Matrix 模式数据包处理
        if (matrixMode) {
            handleMatrixPackets(packet)
        }

        // 原始数据包处理
        if (packet is S14PacketEntity) {
            val entity = packet.getEntity(mc.theWorld)

            if (entity is EntityPlayer) {
                if (entity.onGround && entity.entityId !in groundList)
                    groundList += entity.entityId

                if (!entity.onGround && entity.entityId !in airList)
                    airList += entity.entityId

                if (entity.onGround) {
                    if (entity.fallDistance > 0.0 || entity.posY == entity.prevPosY || !entity.isCollidedVertically) {
                        invalidGroundList.putIfAbsent(
                            entity.entityId,
                            invalidGroundList.getOrDefault(entity.entityId, 0) + 1
                        )
                    }
                } else {
                    val currentVL = invalidGroundList.getOrDefault(entity.entityId, 0)

                    if (currentVL > 0) {
                        invalidGroundList.putIfAbsent(entity.entityId, currentVL - 1)
                    } else {
                        invalidGroundList.remove(entity.entityId)
                    }
                }

                if ((entity.isInvisible || entity.isInvisibleToPlayer(mc.thePlayer)) && entity.entityId !in invisibleList)
                    invisibleList += entity.entityId

                if (alwaysInRadius) {
                    val distance = mc.thePlayer.getDistanceToEntity(entity)
                    val currentTicks = entityTickMap.getOrDefault(entity.entityId, 0)

                    if (distance < alwaysRadius) {
                        entityTickMap[entity.entityId] = currentTicks + 1
                    } else {
                        entityTickMap[entity.entityId] = 0
                    }

                    if (entityTickMap[entity.entityId]!! >= alwaysRadiusTick) {
                        notAlwaysInRadiusList -= entity.entityId
                    } else {
                        if (entity.entityId !in notAlwaysInRadiusList) {
                            notAlwaysInRadiusList += entity.entityId
                        }
                    }
                }

                if (alwaysBehind) {
                    val distance = mc.thePlayer.getDistanceToEntity(entity)
                    val rotation = toRotation(entity.getPositionEyes(1f), false)
                    val angleDifferenceToEntity = abs(angleDifference(rotation.yaw, serverRotation.yaw))

                    if (distance < alwaysBehindRadius && angleDifferenceToEntity > behindRotDiffToIgnore) {
                        alwaysBehindList += entity.entityId
                    } else {
                        if (entity.entityId in alwaysBehindList) {
                            alwaysBehindList -= entity.entityId
                        }
                    }
                }

                if (invalidSpeed) {
                    val deltaX = entity.posX - entity.prevPosX
                    val deltaZ = entity.posZ - entity.prevPosZ
                    val speed = sqrt(deltaX * deltaX + deltaZ * deltaZ)

                    if (speed in 0.45..0.46 && (!entity.isSprinting || !entity.isMoving ||
                                entity.getActivePotionEffect(Potion.moveSpeed) == null)
                    ) {
                        invalidSpeedList += entity.entityId
                    }
                }
            }
        }

        if (packet is S0BPacketAnimation) {
            val entity = mc.theWorld.getEntityByID(packet.entityID)

            if (entity != null && entity is EntityLivingBase && packet.animationType == 0
                && entity.entityId !in swingList
            )
                swingList += entity.entityId
        }

        if (packet is S20PacketEntityProperties) {
            propertiesList += packet.entityId
        }

        if (packet is S13PacketDestroyEntities) {
            for (entityID in packet.entityIDs) {
                // Remove [entityID] from every list upon deletion
                groundList -= entityID
                airList -= entityID
                invalidGroundList -= entityID
                swingList -= entityID
                invisibleList -= entityID
                notAlwaysInRadiusList -= entityID
                propertiesList -= entityID
            }
        }
    }

    private fun handleMatrixPackets(packet: Any) {
        when (packet) {
            is S38PacketPlayerListItem -> {
                try {
                    // 使用反射访问私有字段
                    val actionField: Field = S38PacketPlayerListItem::class.java.getDeclaredField("field_179970_a")
                    actionField.isAccessible = true
                    val action = actionField.get(packet) as S38PacketPlayerListItem.Action
                    
                    val entriesField: Field = S38PacketPlayerListItem::class.java.getDeclaredField("field_179969_b")
                    entriesField.isAccessible = true
                    val entries = entriesField.get(packet) as List<*>
                    
                    when (action) {
                        S38PacketPlayerListItem.Action.ADD_PLAYER -> {
                            for (entry in entries) {
                                // 获取每个entry的profile和ping
                                val entryClass = entry?.javaClass ?: continue
                                
                                val profileField = entryClass.getDeclaredField("field_179965_a")
                                profileField.isAccessible = true
                                val profile = profileField.get(entry) as? com.mojang.authlib.GameProfile ?: continue
                                
                                val pingField = entryClass.getDeclaredField("field_179964_b")
                                pingField.isAccessible = true
                                val ping = pingField.get(entry) as Int
                                
                                if (ping < 2 || profile.properties.isEmpty() == false || isGameProfileUnique(profile)) {
                                    continue
                                }
                                
                                if (isADuplicate(profile)) {
                                    matrixBotList.add(profile.id)
                                    continue
                                }
                                
                                matrixSuspectList.add(profile.id)
                            }
                        }
                        S38PacketPlayerListItem.Action.REMOVE_PLAYER -> {
                            for (entry in entries) {
                                val entryClass = entry?.javaClass ?: continue
                                
                                val profileField = entryClass.getDeclaredField("field_179965_a")
                                profileField.isAccessible = true
                                val profile = profileField.get(entry) as? com.mojang.authlib.GameProfile ?: continue
                                val uuid = profile.id
                                
                                if (matrixSuspectList.contains(uuid)) {
                                    matrixSuspectList.remove(uuid)
                                }
                                if (matrixBotList.contains(uuid)) {
                                    matrixBotList.remove(uuid)
                                }
                                if (matrixWaitingArmor.containsKey(uuid)) {
                                    matrixWaitingArmor.remove(uuid)
                                }
                            }
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    // 反射失败，静默处理
                    e.printStackTrace()
                }
            }
            is S3EPacketTeams -> {
                // 处理团队包（在某些服务器中可能用于反作弊检测）
                if (packet.action == 4 && packet.players != null) {
                    for (playerName in packet.players!!) {
                        val player = mc.theWorld.playerEntities.find { it.name == playerName } as? EntityPlayer ?: continue
                        val uuid = player.uniqueID

                        if (matrixSuspectList.contains(uuid)) {
                            matrixSuspectList.remove(uuid)
                        }
                        if (matrixBotList.contains(uuid)) {
                            matrixBotList.remove(uuid)
                        }
                        if (matrixWaitingArmor.containsKey(uuid)) {
                            matrixWaitingArmor.remove(uuid)
                        }
                    }
                }
            }
        }
    }

    private fun handleMatrixTick() {
        if (matrixSuspectList.isEmpty() && matrixWaitingArmor.isEmpty()) {
            return
        }

        // 处理等待中的实体
        val waitingToRemove = mutableListOf<UUID>()
        for ((uuid, prevArmor) in matrixWaitingArmor) {
            val entity = mc.theWorld.playerEntities.find { it.uniqueID == uuid } as? EntityPlayer ?: continue

            if (isFullyArmored(entity) || updatesArmor(entity, prevArmor) && entity.gameProfile.properties.isEmpty) {
                matrixBotList.add(uuid)
            }

            matrixSuspectList.remove(uuid)
            waitingToRemove.add(uuid)
        }

        // 清理已处理的等待实体
        for (uuid in waitingToRemove) {
            matrixWaitingArmor.remove(uuid)
        }

        // 处理新的嫌疑实体
        for (entity in mc.theWorld.playerEntities) {
            val player = entity as? EntityPlayer ?: continue

            if (!matrixSuspectList.contains(player.uniqueID)) {
                continue
            }

            if (!isFullyArmored(player)) {
                // 记录当前装甲并等待下一tick
                matrixWaitingArmor[player.uniqueID] = player.inventory.armorInventory.toList()
                continue
            }

            if (player.gameProfile.properties.isEmpty) {
                matrixBotList.add(player.uniqueID)
            }

            matrixSuspectList.remove(player.uniqueID)
        }
    }

    private fun isFullyArmored(entity: EntityPlayer): Boolean {
        return entity.inventory.armorInventory.all { stack ->
            stack != null && stack.item is ItemArmor && stack.isItemEnchanted
        }
    }

    /**
     * Matrix spawns its bot with a random set of armor but then instantly and silently gets a new set,
     * therefore somewhat tricking the client that the bot already had the new armor.
     *
     * With the help of at least 1 tick of waiting time, this function patches this "trick".
     */
    private fun updatesArmor(entity: EntityPlayer, prevArmor: List<ItemStack>?): Boolean {
        if (prevArmor == null) return false

        val currentArmor = entity.inventory.armorInventory.toList()
        if (prevArmor.size != currentArmor.size) return true

        for (i in prevArmor.indices) {
            if (prevArmor[i] != currentArmor[i]) {
                return true
            }
        }
        return false
    }

    // Helper functions for Matrix mode
    private fun isGameProfileUnique(profile: com.mojang.authlib.GameProfile): Boolean {
        // 检查游戏配置文件是否唯一
        return mc.netHandler.playerInfoMap.none { it.gameProfile.name == profile.name }
    }

    private fun isADuplicate(profile: com.mojang.authlib.GameProfile): Boolean {
        // 检查是否为重复的游戏配置文件
        return mc.netHandler.playerInfoMap.count { it.gameProfile.name == profile.name } > 1
    }

    val onAttack = handler<AttackEvent>(always = true) { e ->
        val entity = e.targetEntity

        if (entity != null && entity is EntityLivingBase && entity.entityId !in hitList)
            hitList += entity.entityId
    }

    val onWorld = handler<WorldEvent>(always = true) {
        clearAll()
    }

    private fun clearAll() {
        hitList.clear()
        swingList.clear()
        groundList.clear()
        invalidGroundList.clear()
        invalidSpeedList.clear()
        invisibleList.clear()
        notAlwaysInRadiusList.clear()
        worldPlayerNames.clear()
        worldDuplicateNames.clear()
        tabPlayerNames.clear()
        tabDuplicateNames.clear()
        alwaysBehindList.clear()
        entityTickMap.clear()
        botList.clear()
        
        // 清除 Matrix 模式列表
        matrixSuspectList.clear()
        matrixBotList.clear()
        matrixWaitingArmor.clear()
    }

    override fun onEnable() {
        clearAll()
    }

    override fun onDisable() {
        clearAll()
    }
}