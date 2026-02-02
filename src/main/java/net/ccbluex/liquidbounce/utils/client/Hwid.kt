/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.client

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HWID {

    private var hwid: String? = null
    private var verified = false

    /**
     * 获取主板HWID
     */
    fun getHWID(): String {
        if (hwid == null) {
            hwid = getMotherboardSerial()
        }
        return hwid!!
    }

    /**
     * 获取主板序列号
     */
    private fun getMotherboardSerial(): String {
        println("[HWID] 获取主板序列号")
        
        return try {
            val process = Runtime.getRuntime().exec("wmic baseboard get serialnumber")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            
            // 读取输出
            val lines = mutableListOf<String>()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                lines.add(line ?: "")
            }
            reader.close()
            process.waitFor()
            
            // 查找序列号
            var serial = ""
            for (i in 1 until lines.size) { // 跳过第一行标题
                val trimmed = lines[i].trim()
                if (trimmed.isNotBlank() && 
                    !trimmed.equals("SerialNumber", ignoreCase = true) &&
                    !trimmed.contains(" ") &&
                    trimmed.length >= 4) {
                    serial = trimmed
                    break
                }
            }
            
            if (serial.isBlank()) {
                throw Exception("未找到有效序列号")
            }
            
            serial.uppercase()
            
        } catch (e: Exception) {
            throw Exception("获取HWID失败: ${e.message}")
        }
    }

    /**
     * 验证HWID - 简化测试版
     */
    fun verify(): Boolean {
        println("\n[HWID] ===== 开始HWID验证 =====")
        
        if (verified) {
            println("[HWID] 已验证过，返回true")
            return true
        }
        
        try {
            // 1. 获取本地HWID
            val localHWID = getHWID()
            println("[HWID] 本地HWID: '$localHWID'")
            
            // 2. 直接测试网络连接
            println("[HWID] 测试网络连接...")
            val canConnect = testNetwork()
            
            if (!canConnect) {
                println("[HWID] ✗ 网络连接失败!")
                return false
            }
            
            println("[HWID] ✓ 网络连接正常")
            
            // 3. 获取网站内容
            println("[HWID] 获取网站内容...")
            val websiteContent = getWebsiteContent()
            
            if (websiteContent.isBlank()) {
                println("[HWID] ✗ 网站内容为空!")
                return false
            }
            
            println("[HWID] 网站内容 (${websiteContent.length} 字符):")
            println(websiteContent)
            
            // 4. 简单检查是否包含HWID
            val contains = websiteContent.contains(localHWID, ignoreCase = true)
            println("[HWID] 网站是否包含HWID '$localHWID': $contains")
            
            verified = contains
            
            if (verified) {
                println("[HWID] ✓✓✓ 验证成功! ✓✓✓")
            } else {
                println("[HWID] ✗✗✗ 验证失败 ✗✗✗")
            }
            
        } catch (e: Exception) {
            println("[HWID] ⚠ 验证异常: ${e.message}")
            e.printStackTrace()
            return false
        }
        
        println("[HWID] ===== 验证结束 =====")
        return verified
    }

    /**
     * 测试网络连接
     */
    private fun testNetwork(): Boolean {
        println("[HWID-NET] 测试网络连接...")
        
        return try {
            val url = URL("https://nekobounce.qzz.io/hwid/")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            
            println("[HWID-NET] 连接中...")
            connection.connect()
            
            val responseCode = connection.responseCode
            println("[HWID-NET] 响应码: $responseCode")
            
            connection.disconnect()
            true
            
        } catch (e: Exception) {
            println("[HWID-NET] ✗ 连接失败: ${e.message}")
            false
        }
    }

    /**
     * 获取网站内容
     */
    private fun getWebsiteContent(): String {
        println("[HWID-NET] 获取网站内容...")
        
        return try {
            val url = URL("https://nekobounce.qzz.io/hwid/")
            val connection = url.openConnection() as HttpURLConnection
            
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000
            
            println("[HWID-NET] 发送请求...")
            connection.connect()
            
            val responseCode = connection.responseCode
            println("[HWID-NET] HTTP状态: $responseCode")
            
            if (responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val content = reader.readText()
                reader.close()
                
                println("[HWID-NET] ✓ 获取成功")
                connection.disconnect()
                content
            } else {
                println("[HWID-NET] ✗ HTTP错误: $responseCode")
                connection.disconnect()
                ""
            }
            
        } catch (e: Exception) {
            println("[HWID-NET] ✗ 获取失败: ${e.message}")
            ""
        }
    }

    /**
     * 检查是否已验证
     */
    fun isVerified(): Boolean = verified
}