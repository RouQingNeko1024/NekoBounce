/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.utils.client.HWID
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import java.awt.Color

class GuiHWIDCheck : GuiScreen() {

    private var hwidText = "正在获取主板序列号..."
    private var statusText = "正在连接验证服务器..."
    private var statusColor = Color.YELLOW.rgb
    private var dots = 0
    private var lastUpdate = 0L
    private var verificationAttempts = 0
    private var verified = false
    private var hwidError = false
    private var verificationStarted = false

    init {
        println("[GuiHWIDCheck] 初始化开始")
        
        Thread {
            try {
                println("[GuiHWIDCheck] 获取HWID线程启动")
                hwidText = "主板HWID: ${HWID.getHWID()}"
                println("[GuiHWIDCheck] HWID获取成功: $hwidText")
            } catch (e: Exception) {
                hwidText = "错误: ${e.message}"
                statusText = "§4✗ 无法获取主板信息"
                statusColor = Color.RED.rgb
                hwidError = true
                println("[GuiHWIDCheck] HWID获取失败: ${e.message}")
                e.printStackTrace()
            }
        }.start()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        
        val sr = ScaledResolution(mc)
        val width = sr.scaledWidth
        val height = sr.scaledHeight
        
        // 标题
        GlStateManager.pushMatrix()
        GlStateManager.scale(2.0f, 2.0f, 2.0f)
        drawCenteredString(fontRendererObj, "§l§6NekoBounce - HWID验证", width / 4, 30, Color.WHITE.rgb)
        GlStateManager.popMatrix()
        
        // 显示HWID信息
        drawCenteredString(fontRendererObj, hwidText, width / 2, height / 2 - 40, 
            if (hwidError) Color.RED.rgb else Color.CYAN.rgb)
        
        // 更新加载动画
        if (System.currentTimeMillis() - lastUpdate > 500) {
            lastUpdate = System.currentTimeMillis()
            dots = (dots + 1) % 4
        }
        
        val loadingDots = ".".repeat(dots)
        
        // 状态信息
        drawCenteredString(fontRendererObj, "$statusText$loadingDots", width / 2, height / 2, statusColor)
        
        // 尝试次数
        if (verificationAttempts > 0) {
            drawCenteredString(fontRendererObj, "§7尝试: $verificationAttempts/3", width / 2, height / 2 + 20, Color.GRAY.rgb)
        }
        
        // 服务器信息
        drawCenteredString(fontRendererObj, "§7服务器: nekobounce.qzz.io/hwid/", width / 2, height / 2 + 40, Color.GRAY.rgb)
        drawCenteredString(fontRendererObj, "§7请查看控制台输出调试信息", width / 2, height / 2 + 55, Color.GRAY.rgb)
        
        // 启动验证（只启动一次）
        if (!verificationStarted && !hwidError && System.currentTimeMillis() > 1000) {
            verificationStarted = true
            startVerification()
        }
        
        // 超过3次尝试，显示错误并停止
        if (!verified && verificationAttempts >= 3) {
            drawCenteredString(fontRendererObj, "§c验证失败，已达最大尝试次数", width / 2, height / 2 + 80, Color.RED.rgb)
            drawCenteredString(fontRendererObj, "§c请检查网络连接或联系管理员", width / 2, height / 2 + 95, Color.RED.rgb)
            drawCenteredString(fontRendererObj, "§c你的HWID: ${hwidText.substring(8)}", width / 2, height / 2 + 110, Color.RED.rgb)
        }
        
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * 启动验证过程 - 最多3次，重试间隔1秒
     */
    private fun startVerification() {
        println("[GuiHWIDCheck] 启动验证过程，最多尝试3次，重试间隔1秒")
        
        Thread {
            while (!verified && verificationAttempts < 3) {
                try {
                    verificationAttempts++
                    println("\n[GuiHWIDCheck] === 第 $verificationAttempts/3 次验证 ===")
                    
                    mc.addScheduledTask {
                        statusText = "正在验证HWID ($verificationAttempts/3)..."
                        statusColor = Color.YELLOW.rgb
                    }
                    
                    println("[GuiHWIDCheck] 调用 HWID.verify()")
                    val startTime = System.currentTimeMillis()
                    val result = HWID.verify()
                    val elapsedTime = System.currentTimeMillis() - startTime
                    
                    println("[GuiHWIDCheck] HWID.verify() 返回: $result (耗时: ${elapsedTime}ms)")
                    
                    if (result) {
                        verified = true
                        println("[GuiHWIDCheck] ✓ 验证成功!")
                        
                        mc.addScheduledTask {
                            statusText = "§a✓ 验证成功"
                            statusColor = Color.GREEN.rgb
                        }
                        
                        Thread.sleep(1000)
                        
                        mc.addScheduledTask {
                            println("[GuiHWIDCheck] 关闭验证界面")
                            mc.displayGuiScreen(null)
                        }
                        
                        break
                    } else {
                        println("[GuiHWIDCheck] ✗ 验证失败")
                        
                        mc.addScheduledTask {
                            statusText = "§c验证失败 ($verificationAttempts/3)"
                            statusColor = Color.RED.rgb
                        }
                        
                        // 如果不是最后一次尝试，等待1秒后重试
                        if (verificationAttempts < 3) {
                            println("[GuiHWIDCheck] 等待1秒后重试...")
                            Thread.sleep(1000)
                        } else {
                            println("[GuiHWIDCheck] 已达最大尝试次数，停止验证")
                            mc.addScheduledTask {
                                statusText = "§4验证失败，已达最大尝试次数"
                                statusColor = Color.RED.rgb
                            }
                        }
                    }
                    
                } catch (e: Exception) {
                    println("[GuiHWIDCheck] ⚠ 验证异常: ${e.message}")
                    e.printStackTrace()
                    
                    mc.addScheduledTask {
                        statusText = "§4验证异常 ($verificationAttempts/3)"
                        statusColor = Color.RED.rgb
                    }
                    
                    // 如果不是最后一次尝试，等待1秒后重试
                    if (verificationAttempts < 3) {
                        Thread.sleep(1000)
                    }
                }
            }
            
            if (!verified) {
                println("[GuiHWIDCheck] 验证彻底失败，已达最大尝试次数（3次）")
            }
            
        }.start()
    }

    override fun doesGuiPauseGame(): Boolean = false
    
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        // 禁用所有键盘输入
    }
    
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // 禁用所有鼠标点击
    }
    
    override fun onGuiClosed() {
        println("[GuiHWIDCheck] 界面关闭，验证状态: $verified")
        super.onGuiClosed()
    }
}