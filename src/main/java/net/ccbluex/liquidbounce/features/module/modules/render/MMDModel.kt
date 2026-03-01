/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.ColorSettingsInteger
import net.minecraft.client.renderer.GlStateManager
import org.lwjgl.opengl.GL11
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException

object MMDModel : Module("MMDModel", Category.RENDER) {
    private val modelVertices = mutableListOf<FloatArray>()
    private val modelNormals = mutableListOf<FloatArray>()
    private val modelFaces = mutableListOf<FaceData>()
    private var displayList = -1

    // 添加配置选项
    private val modelScale by float("ModelScale", 0.15f, 0.01f..1f)
    private val modelOffsetY by float("ModelOffsetY", -12f, -20f..20f)
    private val enableOutline by boolean("EnableOutline", true)
    private val outlineWidth by float("OutlineWidth", 2f, 1f..5f)

    // 使用ColorSettingsInteger处理颜色
    private val outlineColor = ColorSettingsInteger(this, "OutlineColor").with(255, 255, 255, 100)
    private val modelColor = ColorSettingsInteger(this, "ModelColor").with(255, 255, 255, 255)

    init {
        loadObjModel("C:/Users/Administrator/Documents/NekoBounce/mmd.obj")
        compileDisplayList()
    }

    val onRender3D = handler<Render3DEvent> { event ->
        if (!state) return@handler

        val player = mc.thePlayer ?: return@handler

        // 保存当前状态
        GL11.glPushMatrix()
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS)

        // 设置渲染状态
        GlStateManager.disableLighting()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GlStateManager.disableTexture2D()
        GlStateManager.disableCull()

        // 计算玩家位置
        val x = player.lastTickPosX + (player.posX - player.lastTickPosX) * event.partialTicks - mc.renderManager.viewerPosX
        val y = player.lastTickPosY + (player.posY - player.lastTickPosY) * event.partialTicks - mc.renderManager.viewerPosY
        val z = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * event.partialTicks - mc.renderManager.viewerPosZ

        // 渲染外轮廓（如果需要）
        if (enableOutline && displayList != -1) {
            GL11.glPushMatrix()
            GL11.glTranslatef(x.toFloat(), y.toFloat() + 1.6f, z.toFloat())

            // 修正旋转：首先绕X轴旋转180度，然后绕Y轴旋转180度，最后根据玩家视角旋转
            GL11.glRotatef(180f, 1f, 0f, 0f)  // 绕X轴旋转180度，解决上下颠倒
            GL11.glRotatef(180f, 0f, 1f, 0f)  // 绕Y轴旋转180度，解决前后颠倒
            GL11.glRotatef(
                player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * event.partialTicks,
                0f, 1f, 0f
            )

            GL11.glScalef(modelScale, modelScale, modelScale)
            GL11.glTranslatef(0f, modelOffsetY, 0f)

            // 使用默认白色颜色，避免复杂的颜色处理
            GlStateManager.color(1f, 1f, 1f, 0.4f)

            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE)
            GL11.glLineWidth(outlineWidth)
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_LINE)
            GL11.glPolygonOffset(1f, 1f)

            GL11.glCallList(displayList)

            GL11.glDisable(GL11.GL_POLYGON_OFFSET_LINE)
            GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL)
            GL11.glPopMatrix()
        }

        // 渲染主模型
        if (displayList != -1) {
            GL11.glPushMatrix()
            GL11.glTranslatef(x.toFloat(), y.toFloat() + 1.6f, z.toFloat())

            // 修正旋转：首先绕X轴旋转180度，然后绕Y轴旋转180度，最后根据玩家视角旋转
            GL11.glRotatef(180f, 1f, 0f, 0f)  // 绕X轴旋转180度，解决上下颠倒
            GL11.glRotatef(180f, 0f, 1f, 0f)  // 绕Y轴旋转180度，解决前后颠倒
            GL11.glRotatef(
                player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * event.partialTicks,
                0f, 1f, 0f
            )

            GL11.glScalef(modelScale, modelScale, modelScale)
            GL11.glTranslatef(0f, modelOffsetY, 0f)

            // 使用默认白色颜色
            GlStateManager.color(1f, 1f, 1f, 1f)

            GL11.glCallList(displayList)
            GL11.glPopMatrix()
        }

        // 恢复状态
        GlStateManager.enableCull()
        GlStateManager.enableTexture2D()
        GlStateManager.enableLighting()
        GlStateManager.disableBlend()

        GL11.glPopAttrib()
        GL11.glPopMatrix()
    }

    private fun loadObjModel(filePath: String) {
        try {
            BufferedReader(FileReader(filePath)).use { reader ->
                reader.lineSequence().forEach { line ->
                    when {
                        line.startsWith("v ") -> {
                            val parts = line.split(" ").mapNotNull { it.toFloatOrNull() }
                            if (parts.size >= 3) {
                                modelVertices.add(floatArrayOf(parts[0], parts[1], parts[2]))
                            }
                        }
                        line.startsWith("vn ") -> {
                            val parts = line.split(" ").mapNotNull { it.toFloatOrNull() }
                            if (parts.size >= 3) {
                                modelNormals.add(floatArrayOf(parts[0], parts[1], parts[2]))
                            }
                        }
                        line.startsWith("f ") -> {
                            val vertexIndices = mutableListOf<Int>()
                            val normalIndices = mutableListOf<Int>()

                            val parts = line.substring(2).split(" ")
                            for (part in parts) {
                                val indices = part.split("/")
                                if (indices.isNotEmpty()) {
                                    vertexIndices.add(indices[0].toInt() - 1)
                                    if (indices.size >= 3 && indices[2].isNotEmpty()) {
                                        normalIndices.add(indices[2].toInt() - 1)
                                    }
                                }
                            }

                            if (vertexIndices.size >= 3) {
                                modelFaces.add(FaceData(vertexIndices, normalIndices))
                            }
                        }
                    }
                }
            }
            println("[MMDModel] Loaded OBJ model: ${modelVertices.size} vertices, ${modelFaces.size} faces")
        } catch (e: IOException) {
            println("[MMDModel] Failed to load OBJ model: ${e.message}")
        } catch (e: Exception) {
            println("[MMDModel] Error loading OBJ model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun compileDisplayList() {
        if (modelFaces.isEmpty() || modelVertices.isEmpty()) {
            println("[MMDModel] No model data to compile")
            return
        }

        displayList = GL11.glGenLists(1)
        GL11.glNewList(displayList, GL11.GL_COMPILE)

        // 计算模型中心用于偏移
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var minZ = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var maxY = Float.MIN_VALUE
        var maxZ = Float.MIN_VALUE

        for (vertex in modelVertices) {
            minX = minOf(minX, vertex[0])
            minY = minOf(minY, vertex[1])
            minZ = minOf(minZ, vertex[2])
            maxX = maxOf(maxX, vertex[0])
            maxY = maxOf(maxY, vertex[1])
            maxZ = maxOf(maxZ, vertex[2])
        }

        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val centerZ = (minZ + maxZ) / 2f

        GL11.glBegin(GL11.GL_TRIANGLES)
        for (face in modelFaces) {
            for (i in face.vertexIndices.indices) {
                val vertexIdx = face.vertexIndices[i]
                if (vertexIdx < modelVertices.size) {
                    val vertex = modelVertices[vertexIdx]

                    // 中心偏移使模型围绕中心点
                    val vx = vertex[0] - centerX
                    val vy = vertex[1] - centerY
                    val vz = vertex[2] - centerZ

                    // 应用法线（如果有）
                    if (i < face.normalIndices.size) {
                        val normalIdx = face.normalIndices[i]
                        if (normalIdx < modelNormals.size) {
                            val normal = modelNormals[normalIdx]
                            GL11.glNormal3f(normal[0], normal[1], normal[2])
                        }
                    }

                    GL11.glVertex3f(vx, vy, vz)
                }
            }
        }
        GL11.glEnd()

        GL11.glEndList()
        println("[MMDModel] Compiled display list: $displayList")
        println("[MMDModel] Model bounds: X($minX-$maxX), Y($minY-$maxY), Z($minZ-$maxZ)")
        println("[MMDModel] Model center: ($centerX, $centerY, $centerZ)")
    }

    override fun onEnable() {
        if (displayList == -1 && modelFaces.isNotEmpty()) {
            compileDisplayList()
        }
        println("[MMDModel] Enabled")
    }

    override fun onDisable() {
        if (displayList != -1) {
            GL11.glDeleteLists(displayList, 1)
            displayList = -1
        }
        println("[MMDModel] Disabled")
    }
}

private data class FaceData(
    val vertexIndices: List<Int>,
    val normalIndices: List<Int>
)