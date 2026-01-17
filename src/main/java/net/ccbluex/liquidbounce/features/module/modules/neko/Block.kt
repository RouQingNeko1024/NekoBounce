/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.minecraft.init.Blocks
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

object Block : Module("Block", Category.NEKO) {

    private val msTimer = MSTimer()
    private val delay by int("Delay", 100, 0..1000)

    override fun onEnable() {
        msTimer.reset()
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer == null || !msTimer.hasTimePassed(delay)) 
            return@handler
        
        // 获取石头物品（使用方块ID转换为物品）
        val stoneItem = Item.getItemFromBlock(Blocks.stone)
        val stoneStack = ItemStack(stoneItem, 64)
        
        // 强制设置第三格物品栏（索引2）为石头
        val slotIndex = 2  // 第三格索引是2（0,1,2）
        
        // 检查当前第三格物品栏是否需要更新
        val currentStack = mc.thePlayer.inventory.mainInventory[slotIndex]
        if (currentStack == null || 
            currentStack.item != stoneItem || 
            currentStack.stackSize != 64) {
            
            // 强制设置物品
            mc.thePlayer.inventory.mainInventory[slotIndex] = stoneStack
            
            // 强制更新客户端
            mc.thePlayer.inventoryContainer.detectAndSendChanges()
        }
        
        msTimer.reset()
    }

    override fun onDisable() {
        // 模块禁用时，恢复原样（可选）
        // 注释掉这行如果希望保持石头在禁用后仍然存在
        // mc.thePlayer?.inventory?.mainInventory?.set(2, null)
    }
}