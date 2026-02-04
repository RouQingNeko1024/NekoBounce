package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.util.ChatComponentText
import net.minecraft.util.EnumChatFormatting
import java.io.*
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue

object IRC : Module("IRC", Category.MISC) {

    // ========== 配置部分 ==========
    private val server = "irc.libera.chat"
    private val port = 6667
    private val channel = "#liquidbounce"
    
    // 用户名文件路径
    private val usernameFilePath = "C:\\Users\\Administrator\\Documents\\NekoBounce\\username.txt"
    
    // 用户名读取
    private val ircUsername: String
        get() {
            val file = File(usernameFilePath)
            return if (file.exists()) {
                val content = file.readText().trim()
                if (content.isNotEmpty()) content else "NekoPlayer"
            } else {
                // 创建文件
                file.parentFile.mkdirs()
                file.writeText("NekoPlayer")
                "NekoPlayer"
            }
        }
    
    // 动态昵称（使用用户名+时间戳）
    private val nickname: String
        get() = "${ircUsername.take(8)}_${System.currentTimeMillis() % 10000}"

    // ========== 连接状态 ==========
    private var socket: Socket? = null
    private var writer: BufferedWriter? = null
    private var reader: BufferedReader? = null
    private var connected = false
    private var reconnectThread: Thread? = null
    private var readerThread: Thread? = null
    private var pingThread: Thread? = null
    
    // ========== 数据存储 ==========
    private val ircNameMap = mutableMapOf<String, String>() // IRC昵称 -> 用户名映射
    private val messageQueue = ConcurrentLinkedQueue<String>()

    // ========== 模块生命周期 ==========
    override fun onEnable() {
        // 确保用户名文件存在
        ensureUsernameFile()
        
        addMessage("${EnumChatFormatting.YELLOW}正在启用IRC模块...")
        addMessage("${EnumChatFormatting.YELLOW}用户名: ${ircUsername}")
        addMessage("${EnumChatFormatting.YELLOW}昵称: $nickname")
        
        // 启动连接线程
        Thread {
            connectToIRC()
        }.start()
    }

    override fun onDisable() {
        disconnectFromIRC()
        addMessage("${EnumChatFormatting.RED}IRC模块已禁用")
    }

    // ========== 事件处理 ==========
    val onPacket = handler { event: net.ccbluex.liquidbounce.event.PacketEvent ->
        if (!state || !connected) return@handler
        
        val packet = event.packet
        
        if (packet is S02PacketChat) {
            val chatText = packet.chatComponent.unformattedText
            
            // 处理IRC消息
            if (chatText.startsWith("[IRC] ")) {
                handleIRCChatMessage(chatText, event)
            }
        }
    }

    // ========== IRC连接核心 ==========
    private fun connectToIRC() {
        try {
            addMessage("${EnumChatFormatting.YELLOW}正在连接 $server:$port...")
            
            // 创建Socket连接
            socket = Socket()
            socket!!.soTimeout = 30000 // 30秒读取超时
            
            // 设置连接超时
            socket!!.connect(java.net.InetSocketAddress(server, port), 15000)
            
            // 创建读写流
            writer = BufferedWriter(OutputStreamWriter(socket!!.getOutputStream(), "UTF-8"))
            reader = BufferedReader(InputStreamReader(socket!!.getInputStream(), "UTF-8"))
            
            // 发送认证信息
            sendIRCRaw("NICK $nickname")
            sendIRCRaw("USER ${ircUsername} 0 * :${ircUsername} from LiquidBounce")
            
            connected = true
            
            addMessage("${EnumChatFormatting.GREEN}✓ 已连接到IRC服务器")
            
            // 启动消息读取线程
            startReaderThread()
            
            // 启动PING保持连接线程
            startPingThread()
            
            // 等待服务器响应后加入频道
            Thread.sleep(3000)
            sendIRCRaw("JOIN $channel")
            addMessage("${EnumChatFormatting.GREEN}✓ 已加入频道: $channel")
            
        } catch (e: java.net.UnknownHostException) {
            addMessage("${EnumChatFormatting.RED}✗ 无法解析服务器地址")
            scheduleReconnect()
        } catch (e: java.net.ConnectException) {
            addMessage("${EnumChatFormatting.RED}✗ 连接被拒绝，请检查网络")
            scheduleReconnect()
        } catch (e: java.net.SocketTimeoutException) {
            addMessage("${EnumChatFormatting.RED}✗ 连接超时")
            scheduleReconnect()
        } catch (e: Exception) {
            addMessage("${EnumChatFormatting.RED}✗ 连接失败: ${e.message}")
            scheduleReconnect()
        }
    }

    private fun disconnectFromIRC() {
        connected = false
        
        // 停止线程
        readerThread?.interrupt()
        pingThread?.interrupt()
        
        try {
            if (socket?.isConnected == true) {
                sendIRCRaw("QUIT :LiquidBounce关闭")
            }
            writer?.close()
            reader?.close()
            socket?.close()
        } catch (e: Exception) {
            // 忽略关闭错误
        }
        
        // 清理资源
        writer = null
        reader = null
        socket = null
        ircNameMap.clear()
    }

    // ========== 消息处理 ==========
    private fun handleIRCChatMessage(chatText: String, event: net.ccbluex.liquidbounce.event.PacketEvent) {
        val colonIndex = chatText.indexOf(": ", 6)
        if (colonIndex > 0) {
            val ircNick = chatText.substring(6, colonIndex).trim()
            val message = chatText.substring(colonIndex + 2)
            
            // 确保用户映射存在
            if (!ircNameMap.containsKey(ircNick)) {
                ircNameMap[ircNick] = ircUsername
            }
            
            // 应用名字替换：[IRC+用户名]实际昵称
            val displayName = ircNameMap[ircNick] ?: ircUsername
            val newDisplay = "${EnumChatFormatting.GOLD}[IRC+$displayName]${EnumChatFormatting.RESET}$ircNick"
            
            val newChat = chatText.replace(
                "[IRC] $ircNick:",
                "$newDisplay:"
            )
            
            // 显示修改后的消息
            mc.thePlayer?.addChatMessage(ChatComponentText(newChat))
            
            // 取消原始事件（防止显示两条消息）
            cancelEvent(event)
        }
    }

    private fun cancelEvent(event: net.ccbluex.liquidbounce.event.PacketEvent) {
        try {
            // 尝试不同的取消方法
            event.javaClass.getMethod("cancelEvent").invoke(event)
        } catch (e: Exception) {
            try {
                event.javaClass.getMethod("cancel").invoke(event)
            } catch (e: Exception) {
                // 如果无法取消，让事件继续（会显示两条消息）
            }
        }
    }

    private fun startReaderThread() {
        readerThread = Thread({
            try {
                while (connected && reader != null) {
                    val line = reader?.readLine()
                    if (line != null) {
                        processIRCLine(line)
                    } else {
                        break
                    }
                }
            } catch (e: Exception) {
                if (connected) {
                    addMessage("${EnumChatFormatting.RED}读取错误: ${e.message}")
                    connected = false
                    scheduleReconnect()
                }
            }
        }, "IRC-Reader")
        readerThread?.start()
    }

    private fun startPingThread() {
        pingThread = Thread({
            while (connected) {
                try {
                    Thread.sleep(60000) // 每分钟发送一次PING
                    sendIRCRaw("PING $server")
                } catch (e: InterruptedException) {
                    break
                }
            }
        }, "IRC-Ping")
        pingThread?.start()
    }

    private fun processIRCLine(line: String) {
        // 处理PING
        if (line.startsWith("PING")) {
            val response = line.replace("PING", "PONG")
            sendIRCRaw(response)
            return
        }
        
        // 处理PRIVMSG（聊天消息）
        if (line.contains("PRIVMSG $channel :")) {
            // 格式: :Nick!user@host PRIVMSG #channel :message
            val parts = line.split(" :", limit = 3)
            if (parts.size >= 3) {
                val prefix = parts[1]
                val message = parts[2]
                
                // 提取昵称
                val nickStart = prefix.indexOf(':') + 1
                val nickEnd = prefix.indexOf('!')
                if (nickStart > 0 && nickEnd > nickStart) {
                    val nick = prefix.substring(nickStart, nickEnd)
                    
                    // 存储用户映射
                    if (!ircNameMap.containsKey(nick)) {
                        ircNameMap[nick] = ircUsername
                    }
                    
                    // 发送到游戏聊天（会自动被onPacket处理）
                    sendToGameChat("[IRC] $nick: $message")
                }
            }
            return
        }
        
        // 处理JOIN（用户加入）
        if (line.contains("JOIN $channel")) {
            // 格式: :Nick!user@host JOIN #channel
            val nickStart = line.indexOf(':') + 1
            val nickEnd = line.indexOf('!')
            if (nickStart > 0 && nickEnd > nickStart) {
                val nick = line.substring(nickStart, nickEnd)
                ircNameMap[nick] = ircUsername
                sendToGameChat("${EnumChatFormatting.GREEN}>> $nick 加入了频道")
            }
            return
        }
        
        // 处理PART（用户离开）
        if (line.contains("PART $channel")) {
            // 格式: :Nick!user@host PART #channel :reason
            val nickStart = line.indexOf(':') + 1
            val nickEnd = line.indexOf('!')
            if (nickStart > 0 && nickEnd > nickStart) {
                val nick = line.substring(nickStart, nickEnd)
                sendToGameChat("${EnumChatFormatting.RED}<< $nick 离开了频道")
            }
            return
        }
        
        // 处理服务器消息
        when {
            line.contains("001") -> { // 欢迎消息
                addMessage("${EnumChatFormatting.GREEN}✓ IRC登录成功")
            }
            line.contains("433") -> { // 昵称已在使用
                addMessage("${EnumChatFormatting.RED}昵称已被使用，自动更换...")
            }
            line.contains("332") -> { // 频道主题
                // :server 332 nick #channel :topic
                val topic = line.substring(line.lastIndexOf(':') + 1)
                addMessage("${EnumChatFormatting.YELLOW}频道主题: $topic")
            }
            line.contains("353") -> { // 用户列表
                // :server 353 nick = #channel :user1 user2 user3
                val usersPart = line.substring(line.lastIndexOf(':') + 1)
                val users = usersPart.split(" ")
                users.forEach { user ->
                    val cleanUser = user.replace("@", "").replace("+", "")
                    if (cleanUser.isNotEmpty()) {
                        ircNameMap[cleanUser] = ircUsername
                    }
                }
            }
        }
    }

    // ========== 工具函数 ==========
    private fun ensureUsernameFile() {
        val file = File(usernameFilePath)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.writeText("NekoPlayer")
            addMessage("${EnumChatFormatting.GREEN}✓ 已创建用户名文件: ${file.absolutePath}")
        }
    }

    private fun sendIRCRaw(command: String) {
        try {
            if (writer != null && socket?.isConnected == true) {
                writer?.write("$command\r\n")
                writer?.flush()
            }
        } catch (e: Exception) {
            if (connected) {
                addMessage("${EnumChatFormatting.RED}发送失败: ${e.message}")
                connected = false
                scheduleReconnect()
            }
        }
    }

    fun sendMessage(message: String) {
        if (!connected) {
            addMessage("${EnumChatFormatting.RED}IRC未连接")
            return
        }
        
        try {
            // 发送到IRC服务器
            sendIRCRaw("PRIVMSG $channel :$message")
            
            // 在本地显示自己发送的消息
            // 格式: [IRC+用户名]昵称: 消息
            val displayName = if (ircUsername.isNotEmpty()) "+$ircUsername" else ""
            val formattedMessage = "${EnumChatFormatting.GOLD}[IRC$displayName]${EnumChatFormatting.YELLOW}$nickname${EnumChatFormatting.WHITE}: $message"
            
            // 直接显示，不会被onPacket处理（因为不是以"[IRC] "开头）
            mc.thePlayer?.addChatMessage(ChatComponentText(formattedMessage))
            
        } catch (e: Exception) {
            addMessage("${EnumChatFormatting.RED}发送失败: ${e.message}")
        }
    }

    private fun sendToGameChat(message: String) {
        mc.thePlayer?.addChatMessage(ChatComponentText(message))
    }

    private fun addMessage(message: String) {
        mc.thePlayer?.addChatMessage(ChatComponentText("${EnumChatFormatting.GOLD}[IRC] $message"))
    }

    private fun scheduleReconnect() {
        if (reconnectThread?.isAlive == true) return
        
        reconnectThread = Thread({
            Thread.sleep(10000) // 10秒后重试
            if (state) {
                addMessage("${EnumChatFormatting.YELLOW}尝试重新连接IRC...")
                connectToIRC()
            }
        }, "IRC-Reconnect")
        reconnectThread?.start()
    }

    // ========== 命令处理 ==========
    fun handleCommand(args: Array<String>) {
        when {
            args.isEmpty() -> {
                showHelp()
            }
            args[0].equals("connect", true) -> {
                if (!state) {
                    state = true
                    addMessage("${EnumChatFormatting.GREEN}正在启用IRC...")
                } else {
                    addMessage("${EnumChatFormatting.YELLOW}IRC已在运行中")
                }
            }
            args[0].equals("disconnect", true) -> {
                if (state) {
                    state = false
                } else {
                    addMessage("${EnumChatFormatting.YELLOW}IRC已禁用")
                }
            }
            args[0].equals("msg", true) && args.size > 1 -> {
                val msg = args.drop(1).joinToString(" ")
                sendMessage(msg)
            }
            args[0].equals("status", true) -> {
                showStatus()
            }
            args[0].equals("users", true) -> {
                showUsers()
            }
            args[0].equals("reconnect", true) -> {
                addMessage("${EnumChatFormatting.YELLOW}重新连接中...")
                disconnectFromIRC()
                Thread.sleep(1000)
                connectToIRC()
            }
            args[0].equals("raw", true) && args.size > 1 -> {
                val rawCmd = args.drop(1).joinToString(" ")
                sendIRCRaw(rawCmd)
                addMessage("${EnumChatFormatting.YELLOW}发送原始命令: $rawCmd")
            }
            args[0].equals("nick", true) && args.size > 1 -> {
                // 昵称命令需要IRC协议支持，这里只显示消息
                addMessage("${EnumChatFormatting.YELLOW}当前昵称: $nickname")
                addMessage("${EnumChatFormatting.YELLOW}用户名: $ircUsername")
            }
            else -> {
                addMessage("${EnumChatFormatting.RED}未知命令: ${args[0]}")
                showHelp()
            }
        }
    }

    private fun showHelp() {
        addMessage("${EnumChatFormatting.GOLD}=== IRC 命令帮助 ===")
        addMessage("${EnumChatFormatting.YELLOW}.irc connect ${EnumChatFormatting.WHITE}- 启用IRC连接")
        addMessage("${EnumChatFormatting.YELLOW}.irc disconnect ${EnumChatFormatting.WHITE}- 禁用IRC连接")
        addMessage("${EnumChatFormatting.YELLOW}.irc msg <消息> ${EnumChatFormatting.WHITE}- 发送聊天消息")
        addMessage("${EnumChatFormatting.YELLOW}.irc status ${EnumChatFormatting.WHITE}- 查看连接状态")
        addMessage("${EnumChatFormatting.YELLOW}.irc users ${EnumChatFormatting.WHITE}- 查看在线用户")
        addMessage("${EnumChatFormatting.YELLOW}.irc reconnect ${EnumChatFormatting.WHITE}- 重新连接")
        addMessage("${EnumChatFormatting.YELLOW}.irc raw <命令> ${EnumChatFormatting.WHITE}- 发送原始IRC命令")
        addMessage("${EnumChatFormatting.YELLOW}.irc nick ${EnumChatFormatting.WHITE}- 查看当前昵称")
    }

    private fun showStatus() {
        val status = if (connected) "${EnumChatFormatting.GREEN}已连接" else "${EnumChatFormatting.RED}未连接"
        
        addMessage("${EnumChatFormatting.GOLD}=== IRC 状态 ===")
        addMessage("连接状态: $status")
        addMessage("服务器: $server:$port")
        addMessage("频道: $channel")
        addMessage("用户名: ${ircUsername}")
        addMessage("昵称: $nickname")
        addMessage("在线用户数: ${ircNameMap.size}")
        
        if (connected) {
            addMessage("${EnumChatFormatting.GREEN}✓ 连接正常")
        } else {
            addMessage("${EnumChatFormatting.RED}✗ 连接异常，尝试 .irc reconnect")
        }
    }

    private fun showUsers() {
        if (ircNameMap.isEmpty()) {
            addMessage("暂无用户在线或未获取到用户列表")
        } else {
            addMessage("${EnumChatFormatting.GOLD}=== 在线用户 (${ircNameMap.size}) ===")
            ircNameMap.keys.sorted().forEachIndexed { index, nick ->
                val displayName = ircNameMap[nick] ?: ircUsername
                val formattedNick = "${EnumChatFormatting.YELLOW}[IRC+$displayName]${EnumChatFormatting.RESET}$nick"
                addMessage("${index + 1}. $formattedNick")
            }
        }
    }

    override val tag: String
        get() = if (connected) "在线" else "离线"
}