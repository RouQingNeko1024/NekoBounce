package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.util.ChatComponentText

object NekoEnhancementTool : Module("NekoEnhancementTool", Category.NEKO) {

    private var wasAlive = true

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE) return@handler
        
        val thePlayer = mc.thePlayer ?: return@handler
        
        // 检查玩家是否死亡
        if (thePlayer.isDead) {
            // 如果之前还活着，说明刚刚死亡
            if (wasAlive) {
                // 获取攻击者
                val attacker = thePlayer.getLastAttacker()
                val attackerName = attacker?.name ?: "未知"
                
                // 在本地聊天显示消息
                mc.thePlayer.addChatMessage(ChatComponentText("§8[§b§lNekoBounce]§r §f§l» §r§c你被${attackerName}打死了这个人一定开挂了"))
                
                // 更新状态
                wasAlive = false
            }
        } else {
            // 玩家还活着，重置状态
            wasAlive = true
        }
    }

    override fun onEnable() {
        wasAlive = true
    }

    override fun onDisable() {
        wasAlive = true
    }
}