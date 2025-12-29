/*
 * NekoBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/RouQingNeko1024/NekoBounce/
 */
package io.qzz.nekobounce.features.module.modules.combat

import io.qzz.nekobounce.event.EventState
import io.qzz.nekobounce.event.MotionEvent
import io.qzz.nekobounce.event.handler
import io.qzz.nekobounce.features.module.Category
import io.qzz.nekobounce.features.module.Module
import io.qzz.nekobounce.utils.attack.EntityUtils.isSelected
import io.qzz.nekobounce.utils.client.PacketUtils.sendPacket
import io.qzz.nekobounce.utils.pathfinding.PathUtils.findPath
import io.qzz.nekobounce.utils.rotation.RaycastUtils
import io.qzz.nekobounce.utils.rotation.RotationUtils
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3

object TeleportHit : Module("TeleportHit", Category.COMBAT) {

    private var targetEntity: EntityLivingBase? = null
    private var shouldHit = false

    val onMotion = handler<MotionEvent> { event ->
        if (event.eventState != EventState.PRE)
            return@handler

        val facedEntity = RaycastUtils.raycastEntity(100.0) { raycastedEntity -> raycastedEntity is EntityLivingBase }

        val thePlayer: EntityPlayerSP = mc.thePlayer ?: return@handler

        if (mc.gameSettings.keyBindAttack.isKeyDown && isSelected(facedEntity, true)) {
            if (facedEntity?.getDistanceSqToEntity(mc.thePlayer)!! >= 1) targetEntity = facedEntity as EntityLivingBase
        }

        targetEntity?.let {
            if (!shouldHit) {
                shouldHit = true
                return@handler
            }

            if (thePlayer.fallDistance > 0F) {
                val rotationVector: Vec3 = RotationUtils.getVectorForRotation(mc.thePlayer.rotationYaw, 0f)
                val x = mc.thePlayer.posX + rotationVector.xCoord * (mc.thePlayer.getDistanceToEntity(it) - 1f)
                val z = mc.thePlayer.posZ + rotationVector.zCoord * (mc.thePlayer.getDistanceToEntity(it) - 1f)
                val y = it.posY + 0.25

                findPath(x, y + 1, z, 4.0).forEach { pos ->
                    sendPacket(
                        C04PacketPlayerPosition(
                            pos.x,
                            pos.y,
                            pos.z,
                            false
                        )
                    )
                }

                thePlayer.swingItem()
                sendPacket(C02PacketUseEntity(it, C02PacketUseEntity.Action.ATTACK))
                thePlayer.onCriticalHit(it)
                shouldHit = false
                targetEntity = null
            } else if (thePlayer.onGround) {
                thePlayer.jump()
            }
        } ?: run { shouldHit = false }
    }
}
