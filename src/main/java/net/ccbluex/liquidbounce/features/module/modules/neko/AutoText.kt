package net.ccbluex.liquidbounce.features.module.modules.neko

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object AutoText : Module("AutoText", Category.NEKO) {

    private val mode by choices("Mode", arrayOf("File1", "File2"), "File1")
    private val delay by int("Delay", 1000, 500..10000)

    private val msTimer = MSTimer()
    private var lines = mutableListOf<String>()
    private var currentIndex = 0

    override fun onEnable() {
        msTimer.reset()
        
        // 创建目录并复制文件
        setupFiles()
        
        loadText()
        currentIndex = 0
    }

    private fun setupFiles() {
        try {
            // 创建目标目录
            val targetDir = File("C:\\Users\\Administrator\\Documents\\NekoBounce\\gal")
            if (!targetDir.exists()) {
                targetDir.mkdirs()
            }

            // 从JAR中复制文件到目标目录
            val jarFiles = arrayOf("1.txt", "2.txt")
            for (fileName in jarFiles) {
                val targetFile = File(targetDir, fileName)
                
                // 如果目标文件不存在，从JAR资源复制
                if (!targetFile.exists()) {
                    // 尝试从JAR中读取资源
                    val resourcePath = "/assets/minecraft/liquidbounce/gal/$fileName"
                    val inputStream = AutoText::class.java.getResourceAsStream(resourcePath)
                    
                    if (inputStream != null) {
                        Files.copy(inputStream, Paths.get(targetFile.absolutePath), StandardCopyOption.REPLACE_EXISTING)
                        inputStream.close()
                    } else {
                        // 如果JAR中没有资源，创建空文件
                        targetFile.createNewFile()
                    }
                }
            }
        } catch (e: Exception) {
            // 处理异常，但不要崩溃
            lines.clear()
            lines.add("File setup error: ${e.message}")
        }
    }

    private fun loadText() {
        val fileName = if (mode == "File1") "1.txt" else "2.txt"
        val targetDir = "C:\\Users\\Administrator\\Documents\\NekoBounce\\gal"
        val file = File("$targetDir\\$fileName")
        
        lines.clear()
        if (file.exists() && file.isFile) {
            try {
                lines.addAll(file.readLines().filter { it.isNotEmpty() })
            } catch (e: Exception) {
                lines.add("Error reading text: ${e.message}")
            }
        } else {
            lines.add("File not found: ${file.absolutePath}")
        }
        
        currentIndex = 0
    }

    val onUpdate = handler<UpdateEvent> {
        if (mc.thePlayer == null || lines.isEmpty() || !msTimer.hasTimePassed(delay)) 
            return@handler

        if (currentIndex >= lines.size) {
            currentIndex = 0
        }
        
        mc.thePlayer.sendChatMessage(lines[currentIndex])
        currentIndex++
        
        msTimer.reset()
    }

    override fun onDisable() {
        lines.clear()
        currentIndex = 0
    }

    override val tag: String
        get() = mode
}