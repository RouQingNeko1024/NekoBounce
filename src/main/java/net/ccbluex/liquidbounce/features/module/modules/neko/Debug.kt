package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientUtils.displayChatMessage
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3

object Debug : Module("Debug", Category.NEKO) {

val onAttack = handler<AttackEvent> { event ->
    val thePlayer = mc.thePlayer ?: return@handler
    val target = event.targetEntity
    
    if (target !is EntityLivingBase) return@handler
    
    // 玩家眼睛位置
    val playerEyePos = thePlayer.getPositionEyes(1.0f)
    
    // 目标实体的边界框
    val targetBoundingBox = target.entityBoundingBox
    
    // 计算视线与目标边界框的交点
    val rayTraceResult = targetBoundingBox.calculateIntercept(
        playerEyePos,
        Vec3(
            playerEyePos.xCoord + thePlayer.lookVec.xCoord * 6.0,
            playerEyePos.yCoord + thePlayer.lookVec.yCoord * 6.0,
            playerEyePos.zCoord + thePlayer.lookVec.zCoord * 6.0
        )
    )
    
    // 玩家到实体的实际攻击距离（视线与边界框的交点距离）
    val actualDistance = if (rayTraceResult != null) {
        playerEyePos.distanceTo(rayTraceResult.hitVec)
    } else {
        // 如果没有交点，计算到实体中心的距离
        playerEyePos.distanceTo(Vec3(target.posX, target.posY + target.eyeHeight, target.posZ))
    }
    
    // 玩家到实体中心点的距离
    val centerDistance = playerEyePos.distanceTo(
        Vec3(target.posX, target.posY + target.eyeHeight, target.posZ)
    )
    
    // 显示在聊天框
    displayChatMessage("Distance:${"%.4f".format(centerDistance)} , Real Distance:${"%.4f".format(actualDistance)}")
}
}