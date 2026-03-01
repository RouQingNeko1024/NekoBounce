/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.WorldRenderer
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.atan2
import kotlin.math.sqrt

object CsgoAimbot : Module("CsgoAimbot", Category.COMBAT) {
    
    private val markedPlayers = mutableSetOf<EntityPlayer>()
    private var currentTarget: EntityPlayer? = null
    
    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler
        val theWorld = mc.theWorld ?: return@handler
        
        currentTarget = null
        
        // 检查鼠标中键是否被按下
        if (Keyboard.isKeyDown(Keyboard.KEY_M)) {
            val objectMouseOver = mc.objectMouseOver
            if (objectMouseOver != null && objectMouseOver.entityHit is EntityPlayer) {
                val player = objectMouseOver.entityHit as EntityPlayer
                if (player != thePlayer && !player.isDead && player.health > 0) {
                    if (markedPlayers.contains(player)) {
                        markedPlayers.remove(player)
                    } else {
                        markedPlayers.add(player)
                    }
                }
            }
        }
        
        // 如果没有任何标记玩家，则使用所有可见玩家
        val playersToCheck = if (markedPlayers.isNotEmpty()) {
            markedPlayers.filter { !it.isDead && it.health > 0 }
        } else {
            emptyList()
        }
        
        // 寻找所有符合条件的玩家
        val visiblePlayers = mutableListOf<EntityPlayer>()
        for (entity in playersToCheck) {
            if (entity == thePlayer) continue
            
            // 检查是否可见（不能穿墙）
            if (!isVisible(thePlayer, entity)) continue
            
            visiblePlayers.add(entity)
        }
        
        // 选择最近的目标
        if (visiblePlayers.isNotEmpty()) {
            currentTarget = visiblePlayers.minByOrNull { thePlayer.getDistanceToEntityBox(it) }
            
            // 瞄准目标
            if (currentTarget != null) {
                val rotations = getRotationsToEntity(currentTarget!!)
                thePlayer.rotationYaw = rotations.first
                thePlayer.rotationPitch = rotations.second
                
                // 模拟按住右键
                mc.gameSettings.keyBindUseItem.pressed = true
            }
        } else {
            // 没有目标时恢复右键
            mc.gameSettings.keyBindUseItem.pressed = false
        }
    }
    
    val onRender = handler<Render3DEvent> { event ->
        val thePlayer = mc.thePlayer ?: return@handler
        val partialTicks = event.partialTicks
        
        // 为所有标记玩家绘制点
        for (target in markedPlayers) {
            if (target.isDead || target.health <= 0) continue
            
            val posX = target.lastTickPosX + (target.posX - target.lastTickPosX) * partialTicks
            val posY = target.lastTickPosY + (target.posY - target.lastTickPosY) * partialTicks
            val posZ = target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * partialTicks
            
            // 检查是否可见
            val isVisible = isVisible(thePlayer, target)
            
            // 绘制一个点（在目标头部位置）
            drawPoint(posX, posY + target.eyeHeight, posZ, isVisible)
        }
    }
    
    override fun onDisable() {
        // 禁用模块时恢复右键
        mc.gameSettings.keyBindUseItem.pressed = false
        markedPlayers.clear()
    }
    
    /**
     * 获取到实体的旋转角度
     */
    private fun getRotationsToEntity(entity: EntityLivingBase): Pair<Float, Float> {
        val thePlayer = mc.thePlayer ?: return Pair(0f, 0f)
        
        val posX = entity.posX - thePlayer.posX
        val posY = entity.posY + entity.eyeHeight - (thePlayer.posY + thePlayer.getEyeHeight())
        val posZ = entity.posZ - thePlayer.posZ
        
        val distance = sqrt(posX * posX + posZ * posZ).toDouble()
        
        var yaw = (atan2(posZ, posX) * 180.0 / Math.PI).toFloat() - 90.0f
        var pitch = (-(atan2(posY, distance) * 180.0 / Math.PI)).toFloat()
        
        // 规范化角度
        yaw = yaw % 360f
        if (yaw < 0) yaw += 360f
        
        pitch = pitch.coerceIn(-90f, 90f)
        
        return Pair(yaw, pitch)
    }
    
    /**
     * 绘制一个点（使用小的十字）
     */
    private fun drawPoint(x: Double, y: Double, z: Double, isVisible: Boolean) {
        val thePlayer = mc.thePlayer ?: return
        val theWorld = mc.theWorld ?: return
        
        val playerX = thePlayer.lastTickPosX + (thePlayer.posX - thePlayer.lastTickPosX) * mc.timer.renderPartialTicks
        val playerY = thePlayer.lastTickPosY + (thePlayer.posY - thePlayer.lastTickPosY) * mc.timer.renderPartialTicks
        val playerZ = thePlayer.lastTickPosZ + (thePlayer.posZ - thePlayer.lastTickPosZ) * mc.timer.renderPartialTicks
        
        val dx = x - playerX
        val dy = y - playerY
        val dz = z - playerZ
        
        GL11.glPushMatrix()
        GL11.glTranslated(dx, dy, dz)
        GL11.glRotatef(-mc.renderManager.playerViewY, 0f, 1f, 0f)
        GL11.glRotatef(mc.renderManager.playerViewX, 1f, 0f, 0f)
        
        // 设置颜色：可见时为绿色，不可见时为红色
        if (isVisible) {
            GL11.glColor4f(0f, 1f, 0f, 1f) // 绿色
        } else {
            GL11.glColor4f(1f, 0f, 0f, 1f) // 红色
        }
        
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glDepthMask(false)
        GL11.glLineWidth(2f)
        
        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer
        
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION)
        
        // 绘制十字
        val size = 0.1
        worldRenderer.pos(-size, 0.0, 0.0).endVertex()
        worldRenderer.pos(size, 0.0, 0.0).endVertex()
        
        worldRenderer.pos(0.0, -size, 0.0).endVertex()
        worldRenderer.pos(0.0, size, 0.0).endVertex()
        
        worldRenderer.pos(0.0, 0.0, -size).endVertex()
        worldRenderer.pos(0.0, 0.0, size).endVertex()
        
        tessellator.draw()
        
        GL11.glDepthMask(true)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glPopMatrix()
    }
    
    /**
     * 检查目标是否可见（射线检测）
     */
    private fun isVisible(player: EntityPlayerSP, target: EntityPlayer): Boolean {
        val playerEyes = Vec3(
            player.posX,
            player.posY + player.getEyeHeight(),
            player.posZ
        )
        
        val targetEyes = Vec3(
            target.posX,
            target.posY + target.getEyeHeight(),
            target.posZ
        )
        
        // 进行视线检测
        return mc.theWorld.rayTraceBlocks(
            playerEyes,
            targetEyes,
            false,
            true,
            false
        ) == null
    }
}