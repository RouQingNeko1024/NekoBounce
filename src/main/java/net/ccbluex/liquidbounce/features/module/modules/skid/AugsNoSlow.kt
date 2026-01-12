package net.ccbluex.liquidbounce.features.module.modules.skid

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.item.ItemBow
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.*
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object AugsNoSlow : Module("AugsNoSlow", Category.SKID) {

    // Settings
    private val startSlow by boolean("StartSlow", false)
    
    // Sword settings
    private val swordSlowdown by boolean("Sword-Slowdown", false)
    private val swordSprint by boolean("Sword-Sprint", false)
    private val swordSwitch by boolean("Sword-Switch", false)
    private val swordToggle by boolean("Sword-Toggle", false)
    private val swordBug by boolean("Sword-Bug", false)
    private val swordTimer by boolean("Sword-Timer", false)
    private val swordForward by float("Sword-Forward", 0.2f, 0f..1f)
    private val swordStrafe by float("Sword-Strafe", 0.2f, 0f..1f)
    private val timerSword by float("Sword-TimerSpeed", 0.2f, 0.1f..2f)

    // Bow settings
    private val bowSlowdown by boolean("Bow-Slowdown", false)
    private val bowSprint by boolean("Bow-Sprint", false)
    private val bowSwitch by boolean("Bow-Switch", false)
    private val bowToggle by boolean("Bow-Toggle", false)
    private val bowTimer by boolean("Bow-Timer", false)
    private val bowForward by float("Bow-Forward", 0.2f, 0f..1f)
    private val bowStrafe by float("Bow-Strafe", 0.2f, 0f..1f)
    private val timerBow by float("Bow-TimerSpeed", 0.2f, 0.1f..2f)

    // Rest items settings
    private val restSlowdown by boolean("Rest-Slowdown", false)
    private val restSprint by boolean("Rest-Sprint", false)
    private val restSwitch by boolean("Rest-Switch", false)
    private val restToggle by boolean("Rest-Toggle", false)
    private val restBug by boolean("Rest-Bug", false)
    private val restTimer by boolean("Rest-Timer", false)
    private val restForward by float("Rest-Forward", 0.2f, 0f..1f)
    private val restStrafe by float("Rest-Strafe", 0.2f, 0f..1f)
    private val timerRest by float("Rest-TimerSpeed", 0.2f, 0.1f..2f)

    private var counter = 0
    private var lastItemStack: net.minecraft.item.ItemStack? = null
    private val timeHelper = MSTimer()

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val currentItem = player.currentEquippedItem ?: return@handler

        if (!player.isUsingItem || (player.moveForward == 0f && player.moveStrafing == 0f)) {
            return@handler
        }

        when {
            currentItem.item is ItemSword -> {
                if (swordTimer) {
                    mc.timer.timerSpeed = timerSword
                } else {
                    mc.timer.timerSpeed = 1f
                }
            }
            currentItem.item is ItemBow -> {
                if (bowTimer) {
                    mc.timer.timerSpeed = timerBow
                } else {
                    mc.timer.timerSpeed = 1f
                }
                if (bowSwitch) {
                    val slotIDtoSwitch = if (player.inventory.currentItem == 7) {
                        player.inventory.currentItem - 2
                    } else {
                        player.inventory.currentItem + 2
                    }
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(slotIDtoSwitch))
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
                }
            }
            else -> {
                if (restTimer) {
                    mc.timer.timerSpeed = timerRest
                } else {
                    mc.timer.timerSpeed = 1f
                }
                if (restSwitch) {
                    val slotIDtoSwitch = if (player.inventory.currentItem == 7) {
                        player.inventory.currentItem - 2
                    } else {
                        player.inventory.currentItem + 2
                    }
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(slotIDtoSwitch))
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
                }
            }
        }
    }

    val onMotion = handler<MotionEvent> {
        val player = mc.thePlayer ?: return@handler
        val currentItem = player.currentEquippedItem ?: return@handler

        if (!player.isUsingItem || (player.moveForward == 0f && player.moveStrafing == 0f)) {
            return@handler
        }

        when {
            currentItem.item is ItemSword -> {
                if (swordToggle) {
                    player.sendQueue.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                }
                if (swordSwitch) {
                    val slotIDtoSwitch = if (player.inventory.currentItem == 7) {
                        player.inventory.currentItem - 2
                    } else {
                        player.inventory.currentItem + 2
                    }
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(slotIDtoSwitch))
                    player.sendQueue.addToSendQueue(C09PacketHeldItemChange(player.inventory.currentItem))
                }
            }
            currentItem.item is ItemBow -> {
                if (bowToggle) {
                    player.sendQueue.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                }
            }
            else -> {
                if (restToggle) {
                    player.sendQueue.addToSendQueue(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                }
            }
        }
    }

    val onPostMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.POST) return@handler
        
        val player = mc.thePlayer ?: return@handler
        val currentItem = player.currentEquippedItem ?: return@handler

        if (!player.isUsingItem || (player.moveForward == 0f && player.moveStrafing == 0f)) {
            return@handler
        }

        when {
            currentItem.item is ItemSword -> {
                if (swordToggle) {
                    player.sendQueue.addToSendQueue(
                        C08PacketPlayerBlockPlacement(player.inventory.getCurrentItem())
                    )
                }
            }
            currentItem.item is ItemBow -> {
                if (bowToggle) {
                    player.sendQueue.addToSendQueue(
                        C08PacketPlayerBlockPlacement(player.inventory.getCurrentItem())
                    )
                }
            }
            else -> {
                if (restToggle) {
                    player.sendQueue.addToSendQueue(
                        C08PacketPlayerBlockPlacement(player.inventory.getCurrentItem())
                    )
                }
            }
        }
    }

    val onClick = handler<ClickWindowEvent> {
        val player = mc.thePlayer ?: return@handler
        val currentItem = player.currentEquippedItem ?: return@handler

        if (!player.isUsingItem || (player.moveForward == 0f && player.moveStrafing == 0f)) {
            counter = 0
            return@handler
        }

        if (lastItemStack != null && !lastItemStack!!.equals(currentItem)) {
            counter = 0
        }

        when {
            currentItem.item is ItemSword -> {
                if (swordBug) {
                    if (counter != 1) {
                        player.sendQueue.addToSendQueue(
                            C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                        )
                        player.stopUsingItem()
                        player.closeScreen()
                        it.cancelEvent()
                        counter = 1
                    }
                }
            }
            currentItem.item !is ItemBow -> {
                if (restBug) {
                    if (counter != 3) {
                        player.sendQueue.addToSendQueue(
                            C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT)
                        )
                        player.stopUsingItem()
                        player.closeScreen()
                        it.cancelEvent()
                        counter = 3
                    }
                }
            }
        }

        if (it.isCancelled) {
            mc.sendClickBlockToController(
                mc.currentScreen == null && mc.gameSettings.keyBindAttack.isKeyDown && mc.inGameHasFocus
            )
        }

        lastItemStack = currentItem
    }

    // 根据LiquidBounce的实际事件类调整
    // 如果SlowDownEvent没有这些字段，需要创建自定义事件处理器
    val onSlowDown = handler<SlowDownEvent> {
        val player = mc.thePlayer ?: return@handler
        val currentItem = player.currentEquippedItem ?: return@handler

        if (!player.isUsingItem || (player.moveForward == 0f && player.moveStrafing == 0f)) {
            timeHelper.reset()
            return@handler
        }

        if (!timeHelper.hasTimePassed(400) && startSlow) {
            return@handler
        }

        // 根据LiquidBounce的SlowDownEvent实际结构调整
        // 如果事件没有sprint字段，直接设置玩家的sprint状态
        when {
            currentItem.item is ItemSword -> {
                if (swordSprint) {
                    player.isSprinting = true
                }
                if (swordSlowdown) {
                    // 通过修改玩家的移动因子来实现减速控制
                    player.motionX *= swordForward.toDouble()
                    player.motionZ *= swordForward.toDouble()
                }
            }
            currentItem.item is ItemBow -> {
                if (bowSprint) {
                    player.isSprinting = true
                }
                if (bowSlowdown) {
                    player.motionX *= bowForward.toDouble()
                    player.motionZ *= bowForward.toDouble()
                }
            }
            else -> {
                if (restSprint) {
                    player.isSprinting = true
                }
                if (restSlowdown) {
                    player.motionX *= restForward.toDouble()
                    player.motionZ *= restForward.toDouble()
                }
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}