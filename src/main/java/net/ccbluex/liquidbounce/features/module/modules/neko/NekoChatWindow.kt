package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.Minecraft
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatComponentText
import java.awt.Desktop
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import javax.swing.JFrame
import javax.swing.JTextPane
import javax.swing.JScrollPane
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.JOptionPane
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.Font
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.text.SimpleDateFormat
import java.util.Date
import javax.swing.UIManager
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit

object NekoChatWindow : Module("NekoChatWindow", Category.NEKO) {

    private val maxMessages by int("MaxMessages", 100, 10..500)
    private val showTimestamp by boolean("ShowTimestamp", false)
    private val hideOriginalChat by boolean("HideOriginalChat", true)
    private val autoSave by boolean("AutoSave", false)
    private val saveInterval by int("SaveInterval", 60, 10..300) { autoSave }
    private val debug by boolean("Debug", true)

    private val chatMessages = mutableListOf<String>()
    private val chatMessagesHtml = mutableListOf<String>() // 存储HTML格式的消息
    private var externalWindow: JFrame? = null
    private var textPane: JTextPane? = null // 改为JTextPane以支持HTML
    private var lastSaveTime = System.currentTimeMillis()

    override fun onEnable() {
        if (debug) {
            Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§a[NekoChatWindow] 模块已启用"))
        }
        
        // 清除之前的聊天记录
        chatMessages.clear()
        chatMessagesHtml.clear()
        lastSaveTime = System.currentTimeMillis()
        
        // 只在窗口不存在时创建窗口
        if (externalWindow == null) {
            createExternalWindow()
        } else {
            // 如果窗口已存在，只是显示它
            SwingUtilities.invokeLater {
                externalWindow?.isVisible = true
                updateTextPane()
            }
        }
    }

    override fun onDisable() {
        if (debug) {
            Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§c[NekoChatWindow] 模块已禁用"))
        }
        
        // 保存聊天记录
        if (autoSave) {
            saveChatToFile()
        }
        
        // 关闭并销毁窗口
        SwingUtilities.invokeLater {
            externalWindow?.dispose()
            externalWindow = null
            this@NekoChatWindow.textPane = null
        }
    }

    private fun createExternalWindow() {
        try {
            // 设置系统外观
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        SwingUtilities.invokeLater {
            try {
                // 检查窗口是否已存在
                if (externalWindow != null) {
                    externalWindow?.isVisible = true
                    updateTextPane()
                    return@invokeLater
                }
                
                val frame = JFrame("NekoBounce NekoChatWindow")
                frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                frame.size = Dimension(600, 400)
                
                // 设置窗口位置
                val screenSize = java.awt.Toolkit.getDefaultToolkit().screenSize
                frame.setLocation(screenSize.width - 650, 100)
                
                // 设置窗口图标
                try {
                    // 尝试从多个可能的路径加载图标
                    val iconPaths = listOf(
                        "liquidbounce/icon_64x64.png",
                        "assets/minecraft/liquidbounce/icon_64x64.png",
                        "/liquidbounce/icon_64x64.png",
                        "icon_64x64.png"
                    )
                    
                    var iconLoaded = false
                    for (iconPath in iconPaths) {
                        try {
                            val iconUrl = NekoChatWindow::class.java.classLoader.getResource(iconPath)
                            if (iconUrl != null) {
                                val iconImage = java.awt.Toolkit.getDefaultToolkit().getImage(iconUrl)
                                frame.iconImage = iconImage
                                iconLoaded = true
                                if (debug) {
                                    println("成功加载图标: $iconPath")
                                }
                                break
                            }
                        } catch (e: Exception) {
                            // 继续尝试下一个路径
                        }
                    }
                    
                    if (!iconLoaded) {
                        // 尝试从文件系统加载
                        try {
                            val mcDir = Minecraft.getMinecraft().mcDataDir
                            val iconFile = File(mcDir, "liquidbounce/icon_64x64.png")
                            if (iconFile.exists()) {
                                val iconImage = java.awt.Toolkit.getDefaultToolkit().getImage(iconFile.absolutePath)
                                frame.iconImage = iconImage
                                if (debug) {
                                    println("从文件系统加载图标: ${iconFile.absolutePath}")
                                }
                            }
                        } catch (e: Exception) {
                            // 忽略图标加载错误
                        }
                    }
                } catch (e: Exception) {
                    // 图标加载失败不影响主要功能
                    if (debug) {
                        println("加载窗口图标失败: ${e.message}")
                    }
                }
                
                // 创建文本区域（使用JTextPane以支持HTML）
                val textPane = JTextPane()
                this@NekoChatWindow.textPane = textPane
                textPane.isEditable = false
                textPane.contentType = "text/html"
                textPane.font = Font("Monospaced", Font.PLAIN, 12)
                
                // 添加滚动条
                val scrollPane = JScrollPane(textPane)
                scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
                
                // 创建按钮面板
                val buttonPanel = JPanel()
                buttonPanel.layout = FlowLayout(FlowLayout.RIGHT)
                
                val clearButton = JButton("Clear")
                clearButton.addActionListener {
                    chatMessages.clear()
                    chatMessagesHtml.clear()
                    updateTextPane()
                }
                
                val exportButton = JButton("Export")
                exportButton.addActionListener {
                    exportChatToFile()
                }
                
                val saveButton = JButton("Save")
                saveButton.addActionListener {
                    saveChatToFile()
                }
                
                buttonPanel.add(clearButton)
                buttonPanel.add(exportButton)
                buttonPanel.add(saveButton)
                
                // 设置布局
                frame.layout = BorderLayout()
                frame.add(scrollPane, BorderLayout.CENTER)
                frame.add(buttonPanel, BorderLayout.SOUTH)
                
                // 窗口关闭事件处理 - 窗口关闭时自动禁用模块
                frame.addWindowListener(object : WindowAdapter() {
                    override fun windowClosing(e: WindowEvent) {
                        // 窗口关闭时禁用模块
                        state = false
                    }
                    
                    override fun windowClosed(e: WindowEvent) {
                        // 清理资源
                        externalWindow = null
                        this@NekoChatWindow.textPane = null
                    }
                })
                
                frame.isVisible = true
                externalWindow = frame
                
                // 初始化显示
                updateTextPane()
                
                // 请求焦点
                frame.requestFocus()
                
                if (debug) {
                    Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§a[NekoChatWindow] 窗口已创建"))
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                if (debug) {
                    Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§c[NekoChatWindow] 创建窗口时出错: ${e.message}"))
                }
            }
        }
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        
        if (packet is S02PacketChat) {
            if (hideOriginalChat) {
                event.cancelEvent()
            }
            
            val chatComponent = packet.chatComponent
            val message = getUnformattedText(chatComponent)
            
            if (message.isNotEmpty()) {
                val formattedMessage = if (showTimestamp) {
                    val timestamp = SimpleDateFormat("HH:mm:ss").format(Date())
                    "[$timestamp] $message"
                } else {
                    message
                }
                
                // 添加原始消息到列表
                chatMessages.add(formattedMessage)
                
                // 转换格式代码为HTML并添加到HTML列表
                val htmlMessage = convertFormatCodesToHtml(formattedMessage)
                chatMessagesHtml.add(htmlMessage)
                
                // 限制消息数量
                while (chatMessages.size > maxMessages) {
                    chatMessages.removeAt(0)
                    chatMessagesHtml.removeAt(0)
                }
                
                // 更新外部窗口
                SwingUtilities.invokeLater {
                    updateTextPane()
                    
                    // 自动滚动到底部
                    textPane?.caretPosition = textPane?.document?.length ?: 0
                }
                
                // 自动保存
                if (autoSave && System.currentTimeMillis() - lastSaveTime > saveInterval * 1000L) {
                    saveChatToFile()
                    lastSaveTime = System.currentTimeMillis()
                }
            }
        }
    }

    private fun updateTextPane() {
        val currentHtml = buildHtmlDocument()
        textPane?.text = currentHtml
    }

    private fun buildHtmlDocument(): String {
        val htmlBuilder = StringBuilder()
        
        // HTML文档开始
        htmlBuilder.append("<html>")
        htmlBuilder.append("<head>")
        htmlBuilder.append("<style>")
        htmlBuilder.append("body { font-family: 'Monospaced', monospace; font-size: 12px; background-color: #2b2b2b; color: #ffffff; margin: 5px; }")
        htmlBuilder.append("</style>")
        htmlBuilder.append("</head>")
        htmlBuilder.append("<body>")
        
        // 添加所有HTML消息
        for (htmlMessage in chatMessagesHtml) {
            htmlBuilder.append(htmlMessage)
            htmlBuilder.append("<br>")
        }
        
        htmlBuilder.append("</body>")
        htmlBuilder.append("</html>")
        
        return htmlBuilder.toString()
    }

    private fun convertFormatCodesToHtml(message: String): String {
        // Minecraft格式代码转换表
        val colorMap = mapOf(
            '0' to "#000000", // 黑色
            '1' to "#0000AA", // 深蓝色
            '2' to "#00AA00", // 深绿色
            '3' to "#00AAAA", // 深青色
            '4' to "#AA0000", // 深红色
            '5' to "#AA00AA", // 紫色
            '6' to "#FFAA00", // 金色
            '7' to "#AAAAAA", // 灰色
            '8' to "#555555", // 深灰色
            '9' to "#5555FF", // 蓝色
            'a' to "#55FF55", // 绿色
            'b' to "#55FFFF", // 青色
            'c' to "#FF5555", // 红色
            'd' to "#FF55FF", // 粉红色
            'e' to "#FFFF55", // 黄色
            'f' to "#FFFFFF", // 白色
            'r' to "#FFFFFF"  // 重置为白色
        )
        
        // 格式化代码转换表
        val formatMap = mapOf(
            'l' to "font-weight:bold;", // 粗体
            'o' to "font-style:italic;", // 斜体
            'n' to "text-decoration:underline;", // 下划线
            'm' to "text-decoration:line-through;", // 删除线
            'k' to "" // 随机字符（忽略）
        )
        
        val result = StringBuilder()
        var i = 0
        var currentColor = "#FFFFFF" // 默认颜色
        var currentFormats = mutableSetOf<String>()
        
        while (i < message.length) {
            if (message[i] == '§' && i + 1 < message.length) {
                val code = message[i + 1].toLowerCase()
                
                if (colorMap.containsKey(code)) {
                    // 重置格式并设置新颜色
                    result.append("</span>")
                    currentFormats.clear()
                    currentColor = colorMap[code] ?: "#FFFFFF"
                    
                    // 构建新的span标签
                    val style = buildStyleString(currentColor, currentFormats)
                    result.append("<span style=\"$style\">")
                } else if (formatMap.containsKey(code)) {
                    // 添加格式化
                    val format = formatMap[code] ?: ""
                    if (format.isNotEmpty() && !currentFormats.contains(format)) {
                        currentFormats.add(format)
                        
                        // 更新当前span
                        result.append("</span>")
                        val style = buildStyleString(currentColor, currentFormats)
                        result.append("<span style=\"$style\">")
                    }
                } else if (code == 'r') {
                    // 重置所有格式和颜色
                    result.append("</span>")
                    currentColor = "#FFFFFF"
                    currentFormats.clear()
                    result.append("<span style=\"color:#FFFFFF;\">")
                }
                
                i += 2 // 跳过格式代码
            } else {
                // 转义HTML特殊字符
                when (message[i]) {
                    '<' -> result.append("&lt;")
                    '>' -> result.append("&gt;")
                    '&' -> result.append("&amp;")
                    '"' -> result.append("&quot;")
                    else -> result.append(message[i])
                }
                i++
            }
        }
        
        // 关闭最后的span标签
        if (result.isNotEmpty() && result.toString().contains("<span")) {
            result.append("</span>")
        }
        
        return result.toString()
    }

    private fun buildStyleString(color: String, formats: Set<String>): String {
        val styleBuilder = StringBuilder()
        styleBuilder.append("color:$color;")
        
        // 添加所有格式
        for (format in formats) {
            styleBuilder.append(format)
        }
        
        return styleBuilder.toString()
    }

    private fun getUnformattedText(chatComponent: net.minecraft.util.IChatComponent): String {
        return try {
            chatComponent.unformattedText
        } catch (e: Exception) {
            try {
                chatComponent.formattedText
            } catch (e2: Exception) {
                ""
            }
        }
    }

    private fun exportChatToFile() {
        try {
            val desktop = Desktop.getDesktop()
            val fileChooser = javax.swing.JFileChooser()
            fileChooser.dialogTitle = "Export Chat"
            fileChooser.selectedFile = File("chat_export_${System.currentTimeMillis()}.txt")
            
            if (fileChooser.showSaveDialog(externalWindow) == javax.swing.JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                FileWriter(file).use { writer ->
                    writer.write(chatMessages.joinToString("\n"))
                }
                JOptionPane.showMessageDialog(externalWindow, 
                    "Chat exported successfully to:\n${file.absolutePath}", 
                    "Export Complete", 
                    JOptionPane.INFORMATION_MESSAGE)
            }
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(externalWindow, 
                "Failed to export chat: ${e.message}", 
                "Export Error", 
                JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun saveChatToFile() {
        try {
            val liquidbounceDir = File(System.getProperty("user.home"), ".liquidbounce")
            if (!liquidbounceDir.exists()) {
                liquidbounceDir.mkdirs()
            }
            
            val chatDir = File(liquidbounceDir, "chatlogs")
            if (!chatDir.exists()) {
                chatDir.mkdirs()
            }
            
            val date = SimpleDateFormat("yyyy-MM-dd").format(Date())
            val time = SimpleDateFormat("HH-mm-ss").format(Date())
            val fileName = "chat_${date}_$time.txt"
            val file = File(chatDir, fileName)
            
            PrintWriter(file).use { writer ->
                writer.println("LiquidBounce Chat Log - $date $time")
                writer.println("=".repeat(50))
                chatMessages.forEach { writer.println(it) }
            }
            
            if (debug) {
                Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§a[NekoChatWindow] 聊天记录已保存到: ${file.absolutePath}"))
            }
        } catch (e: Exception) {
            if (debug) {
                Minecraft.getMinecraft().thePlayer?.addChatMessage(ChatComponentText("§c[NekoChatWindow] 保存聊天记录失败: ${e.message}"))
            }
        }
    }

    // 清空聊天功能
    fun clearChat() {
        chatMessages.clear()
        chatMessagesHtml.clear()
        SwingUtilities.invokeLater {
            updateTextPane()
        }
    }

    // 导出聊天功能
    fun exportChat(): String {
        return chatMessages.joinToString("\n")
    }
}