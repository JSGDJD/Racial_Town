package org.HUD.hotelRoom.race;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

/**
 * 种族语音HTTP服务器
 * 提供WebRTC语音聊天网页服务
 */
public class RaceVoiceServer {
    
    private final int port;
    private final RaceVoiceManager voiceManager;
    private HttpServer server;
    
    // 存储信令消息队列 <PlayerUUID, List<SignalMessage>>
    private final Map<String, List<String>> signalQueue = new ConcurrentHashMap<>();
    
    public RaceVoiceServer(int port, RaceVoiceManager voiceManager) {
        this.port = port;
        this.voiceManager = voiceManager;
    }
    
    /**
     * 启动HTTP服务器
     */
    public void start() throws IOException {
        // 绑定到 0.0.0.0 以支持局域网访问
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        
        // 注册路由
        server.createContext("/", new IndexHandler());
        server.createContext("/api/player-info", new PlayerInfoHandler());
        server.createContext("/api/channels", new ChannelsHandler());
        server.createContext("/api/join-channel", new JoinChannelHandler());
        server.createContext("/api/leave-channel", new LeaveChannelHandler());
        server.createContext("/api/signal", new SignalHandler());
        server.createContext("/api/poll-signal", new PollSignalHandler());
        server.createContext("/api/heartbeat", new HeartbeatHandler());
        
        server.start();
    }
    
    /**
     * 停止HTTP服务器
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            
            // 等待端口完全释放
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 主页处理器 - 返回语音聊天网页
     */
    private class IndexHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = generateVoiceChatHtml();
            byte[] response = html.getBytes(StandardCharsets.UTF_8);
            
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, response.length);
            
            OutputStream os = exchange.getResponseBody();
            os.write(response);
            os.close();
        }
    }
    
    /**
     * 玩家信息API处理器
     */
    private class PlayerInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // 从URL参数获取玩家UUID
            String query = exchange.getRequestURI().getQuery();
            String playerUUID = getParameter(query, "uuid");
            
            if (playerUUID == null || playerUUID.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Missing UUID parameter\"}");
                return;
            }
            
            try {
                UUID uuid = UUID.fromString(playerUUID);
                String race = voiceManager.getPlayerRace(uuid);
                String channel = voiceManager.getPlayerChannel(uuid);
                
                String json = String.format(
                    "{\"uuid\": \"%s\", \"race\": \"%s\", \"channel\": \"%s\"}",
                    playerUUID, race, channel != null ? channel : ""
                );
                
                sendJsonResponse(exchange, 200, json);
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Invalid UUID format\"}");
            }
        }
    }
    
    /**
     * 频道列表API处理器
     */
    private class ChannelsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, Integer> channelInfo = voiceManager.getChannelInfo();
            
            StringBuilder json = new StringBuilder("{\"channels\": [");
            boolean first = true;
            for (Map.Entry<String, Integer> entry : channelInfo.entrySet()) {
                if (!first) json.append(",");
                json.append(String.format(
                    "{\"name\": \"%s\", \"playerCount\": %d}",
                    entry.getKey(), entry.getValue()
                ));
                first = false;
            }
            json.append("]}");
            
            sendJsonResponse(exchange, 200, json.toString());
        }
    }
    
    /**
     * 加入频道API处理器
     */
    private class JoinChannelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            
            String body = readRequestBody(exchange);
            String playerUUID = extractJsonValue(body, "uuid");
            String raceName = extractJsonValue(body, "race");
            
            if (playerUUID == null || raceName == null) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Missing parameters\"}");
                return;
            }
            
            try {
                UUID uuid = UUID.fromString(playerUUID);
                boolean success = voiceManager.joinChannel(uuid, raceName);
                
                if (success) {
                    sendJsonResponse(exchange, 200, "{\"success\": true}");
                } else {
                    sendJsonResponse(exchange, 403, "{\"error\": \"Race mismatch\"}");
                }
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Invalid UUID format\"}");
            }
        }
    }
    
    /**
     * 离开频道API处理器
     */
    private class LeaveChannelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            
            String body = readRequestBody(exchange);
            String playerUUID = extractJsonValue(body, "uuid");
            
            if (playerUUID == null) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Missing UUID parameter\"}");
                return;
            }
            
            try {
                UUID uuid = UUID.fromString(playerUUID);
                voiceManager.leaveChannel(uuid);
                sendJsonResponse(exchange, 200, "{\"success\": true}");
            } catch (IllegalArgumentException e) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Invalid UUID format\"}");
            }
        }
    }
    
    /**
     * WebRTC信令处理器
     */
    private class SignalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // WebRTC信令交换处理
            if (!"POST".equals(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\": \"Method not allowed\"}");
                return;
            }
            
            String body = readRequestBody(exchange);
            String fromUUID = extractJsonValue(body, "from");
            String toUUID = extractJsonValue(body, "to");
            String type = extractJsonValue(body, "type");
            String data = extractJsonField(body, "data");
            
            if (fromUUID == null || type == null) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Missing parameters\"}");
                return;
            }
            
            // 如果指定了目标玩家，发送给特定玩家
            if (toUUID != null && !toUUID.isEmpty()) {
                String message = String.format(
                    "{\"from\":\"%s\",\"type\":\"%s\",\"data\":%s}",
                    fromUUID, type, data != null ? data : "null"
                );
                signalQueue.computeIfAbsent(toUUID, k -> new ArrayList<>()).add(message);
                sendJsonResponse(exchange, 200, "{\"success\": true}");
            } else {
                // 广播给同频道的所有玩家
                try {
                    UUID uuid = UUID.fromString(fromUUID);
                    String channel = voiceManager.getPlayerChannel(uuid);
                    if (channel != null) {
                        Set<UUID> channelPlayers = voiceManager.getChannelPlayers(channel);
                        String message = String.format(
                            "{\"from\":\"%s\",\"type\":\"%s\",\"data\":%s}",
                            fromUUID, type, data != null ? data : "null"
                        );
                        
                        for (UUID targetUUID : channelPlayers) {
                            if (!targetUUID.equals(uuid)) {
                                signalQueue.computeIfAbsent(targetUUID.toString(), k -> new ArrayList<>()).add(message);
                            }
                        }
                        sendJsonResponse(exchange, 200, "{\"success\": true}");
                    } else {
                        sendJsonResponse(exchange, 400, "{\"error\": \"Not in channel\"}");
                    }
                } catch (IllegalArgumentException e) {
                    sendJsonResponse(exchange, 400, "{\"error\": \"Invalid UUID\"}");
                }
            }
        }
    }
    
    /**
     * 轮询信令消息处理器
     */
    private class PollSignalHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String uuid = getParameter(query, "uuid");
            
            if (uuid == null || uuid.isEmpty()) {
                sendJsonResponse(exchange, 400, "{\"error\": \"Missing UUID\"}");
                return;
            }
            
            List<String> messages = signalQueue.remove(uuid);
            if (messages != null && !messages.isEmpty()) {
                StringBuilder json = new StringBuilder("{\"messages\": [");
                for (int i = 0; i < messages.size(); i++) {
                    if (i > 0) json.append(",");
                    json.append(messages.get(i));
                }
                json.append("]}");
                sendJsonResponse(exchange, 200, json.toString());
            } else {
                sendJsonResponse(exchange, 200, "{\"messages\": []}");
            }
        }
    }
    
    /**
     * 心跳检测处理器 - 检查服务器是否在运行
     */
    private class HeartbeatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, 200, "{\"status\": \"online\", \"timestamp\": " + System.currentTimeMillis() + "}");
        }
    }
    
    // ===== 工具方法 =====
    
    private void sendJsonResponse(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] response = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.length);
        
        OutputStream os = exchange.getResponseBody();
        os.write(response);
        os.close();
    }
    
    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }
    
    private String getParameter(String query, String key) {
        if (query == null) return null;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=");
            if (keyValue.length == 2 && keyValue[0].equals(key)) {
                return keyValue[1];
            }
        }
        return null;
    }
    
    private String extractJsonValue(String json, String key) {
        String pattern = "\"" + key + "\"\\s*:\\s*\"([^\"]+)\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    private String extractJsonField(String json, String key) {
        // 提取JSON对象或数组字段
        String pattern = "\"" + key + "\"\\s*:\\s*(\\{[^}]*\\}|\\[[^\\]]*\\])";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    
    /**
     * 生成语音聊天网页HTML
     */
    private String generateVoiceChatHtml() {
        return "<!DOCTYPE html>\n" +
            "<html lang=\"zh-CN\">\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
            "    <title>种族语音聊天</title>\n" +
            "    <style>\n" +
            "        * { margin: 0; padding: 0; box-sizing: border-box; }\n" +
            "        body {\n" +
            "            font-family: 'Microsoft YaHei', Arial, sans-serif;\n" +
            "            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);\n" +
            "            height: 100vh;\n" +
            "            display: flex;\n" +
            "        }\n" +
            "        .sidebar {\n" +
            "            width: 280px;\n" +
            "            background: rgba(255, 255, 255, 0.95);\n" +
            "            padding: 20px;\n" +
            "            box-shadow: 2px 0 10px rgba(0,0,0,0.1);\n" +
            "            overflow-y: auto;\n" +
            "        }\n" +
            "        .sidebar h2 {\n" +
            "            color: #667eea;\n" +
            "            margin-bottom: 20px;\n" +
            "            font-size: 24px;\n" +
            "        }\n" +
            "        .channel {\n" +
            "            background: white;\n" +
            "            border: 2px solid #667eea;\n" +
            "            border-radius: 8px;\n" +
            "            padding: 15px;\n" +
            "            margin-bottom: 12px;\n" +
            "            cursor: pointer;\n" +
            "            transition: all 0.3s;\n" +
            "        }\n" +
            "        .channel:hover {\n" +
            "            background: #667eea;\n" +
            "            color: white;\n" +
            "            transform: translateX(5px);\n" +
            "        }\n" +
            "        .channel.active {\n" +
            "            background: #667eea;\n" +
            "            color: white;\n" +
            "        }\n" +
            "        .channel.locked {\n" +
            "            opacity: 0.5;\n" +
            "            cursor: not-allowed;\n" +
            "        }\n" +
            "        .channel-name {\n" +
            "            font-size: 18px;\n" +
            "            font-weight: bold;\n" +
            "            margin-bottom: 5px;\n" +
            "        }\n" +
            "        .channel-info {\n" +
            "            font-size: 14px;\n" +
            "            opacity: 0.8;\n" +
            "        }\n" +
            "        .main-content {\n" +
            "            flex: 1;\n" +
            "            padding: 40px;\n" +
            "            display: flex;\n" +
            "            flex-direction: column;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "        }\n" +
            "        .status-card {\n" +
            "            background: rgba(255, 255, 255, 0.95);\n" +
            "            border-radius: 16px;\n" +
            "            padding: 40px;\n" +
            "            box-shadow: 0 10px 30px rgba(0,0,0,0.2);\n" +
            "            text-align: center;\n" +
            "            max-width: 500px;\n" +
            "        }\n" +
            "        .status-icon {\n" +
            "            width: 100px;\n" +
            "            height: 100px;\n" +
            "            margin: 0 auto 20px;\n" +
            "            border-radius: 50%;\n" +
            "            background: #667eea;\n" +
            "            display: flex;\n" +
            "            align-items: center;\n" +
            "            justify-content: center;\n" +
            "            font-size: 48px;\n" +
            "        }\n" +
            "        .mic-button {\n" +
            "            width: 80px;\n" +
            "            height: 80px;\n" +
            "            border-radius: 50%;\n" +
            "            border: none;\n" +
            "            background: #667eea;\n" +
            "            color: white;\n" +
            "            font-size: 32px;\n" +
            "            cursor: pointer;\n" +
            "            transition: all 0.3s;\n" +
            "            margin: 20px auto;\n" +
            "        }\n" +
            "        .mic-button:hover {\n" +
            "            background: #5568d3;\n" +
            "            transform: scale(1.1);\n" +
            "        }\n" +
            "        .mic-button.active {\n" +
            "            background: #e74c3c;\n" +
            "            animation: pulse 1.5s infinite;\n" +
            "        }\n" +
            "        @keyframes pulse {\n" +
            "            0%, 100% { transform: scale(1); }\n" +
            "            50% { transform: scale(1.1); }\n" +
            "        }\n" +
            "        .player-race {\n" +
            "            font-size: 18px;\n" +
            "            color: #667eea;\n" +
            "            margin: 10px 0;\n" +
            "        }\n" +
            "        .status-text {\n" +
            "            font-size: 16px;\n" +
            "            color: #666;\n" +
            "            margin-top: 10px;\n" +
            "        }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div class=\"sidebar\">\n" +
            "        <h2>🎙️ 种族频道</h2>\n" +
            "        <div id=\"channels-list\"></div>\n" +
            "    </div>\n" +
            "    <div class=\"main-content\">\n" +
            "        <div class=\"status-card\">\n" +
            "            <div class=\"status-icon\" id=\"status-icon\">🎤</div>\n" +
            "            <h1 id=\"status-title\">种族语音聊天</h1>\n" +
            "            <div class=\"player-race\" id=\"player-race\">检测中...</div>\n" +
            "            <button class=\"mic-button\" id=\"mic-button\" onclick=\"toggleMic()\">🎤</button>\n" +
            "            <div class=\"status-text\" id=\"status-text\">请选择你的种族频道</div>\n" +
            "        </div>\n" +
            "    </div>\n" +
            "    <script>\n" +
            generateVoiceChatScript() +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }
    
    /**
     * 生成语音聊天JavaScript代码
     */
    private String generateVoiceChatScript() {
        return "        let playerUUID = null;\n" +
            "        let playerRace = null;\n" +
            "        let currentChannel = null;\n" +
            "        let localStream = null;\n" +
            "        let isMicActive = false;\n" +
            "        let peerConnections = {};\n" +
            "        let pollingInterval = null;\n" +
            "        let heartbeatInterval = null;\n" +
            "        let serverOnline = true;\n" +
            "        \n" +
            "        const configuration = {\n" +
            "            iceServers: [\n" +
            "                { urls: 'stun:stun.l.google.com:19302' },\n" +
            "                { urls: 'stun:stun1.l.google.com:19302' }\n" +
            "            ]\n" +
            "        };\n" +
            "        \n" +
            "        // 检查浏览器支持\n" +
            "        function checkBrowserSupport() {\n" +
            "            // 检查是否为安全上下文\n" +
            "            const isSecureContext = window.isSecureContext || location.protocol === 'https:' || \n" +
            "                                     location.hostname === 'localhost' || location.hostname === '127.0.0.1';\n" +
            "            \n" +
            "            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {\n" +
            "                let errorMsg = '您的浏览器不支持麦克风访问！\\n\\n';\n" +
            "                \n" +
            "                // 判断是安全上下文问题还是浏览器版本问题\n" +
            "                if (!isSecureContext) {\n" +
            "                    errorMsg += '原因：当前使用HTTP协议访问局域网IP\\n';\n" +
            "                    errorMsg += '浏览器安全限制禁止了麦克风访问！\\n\\n';\n" +
            "                    errorMsg += '解决方案：\\n';\n" +
            "                    errorMsg += '1. 在服务器本机使用 localhost 访问\\n';\n" +
            "                    errorMsg += '2. 配置HTTPS证书（推荐用于生产环境）\\n\\n';\n" +
            "                    errorMsg += '当前URL: ' + location.href;\n" +
            "                } else {\n" +
            "                    errorMsg += '请使用以下浏览器：\\n';\n" +
            "                    errorMsg += '- Chrome 53+\\n';\n" +
            "                    errorMsg += '- Firefox 36+\\n';\n" +
            "                    errorMsg += '- Edge 79+\\n';\n" +
            "                    errorMsg += '- Safari 11+';\n" +
            "                }\n" +
            "                \n" +
            "                alert(errorMsg);\n" +
            "                return false;\n" +
            "            }\n" +
            "            if (!window.RTCPeerConnection) {\n" +
            "                alert('您的浏览器不支持WebRTC连接功能！');\n" +
            "                return false;\n" +
            "            }\n" +
            "            return true;\n" +
            "        }\n" +
            "        \n" +
            "        function getPlayerUUID() {\n" +
            "            const params = new URLSearchParams(window.location.search);\n" +
            "            return params.get('uuid');\n" +
            "        }\n" +
            "        \n" +
            "        async function initialize() {\n" +
            "            if (!checkBrowserSupport()) {\n" +
            "                document.getElementById('status-text').textContent = '浏览器不支持语音功能';\n" +
            "                return;\n" +
            "            }\n" +
            "            playerUUID = getPlayerUUID();\n" +
            "            if (!playerUUID) {\n" +
            "                alert('未检测到玩家信息');\n" +
            "                return;\n" +
            "            }\n" +
            "            await fetchPlayerInfo();\n" +
            "            await loadChannels();\n" +
            "            setInterval(loadChannels, 5000);\n" +
            "            // 启动心跳检测\n" +
            "            startHeartbeat();\n" +
            "        }\n" +
            "        \n" +
            "        // 心跳检测 - 每10秒检查一次服务器状态\n" +
            "        function startHeartbeat() {\n" +
            "            heartbeatInterval = setInterval(async () => {\n" +
            "                try {\n" +
            "                    const response = await fetch('/api/heartbeat', { \n" +
            "                        method: 'GET',\n" +
            "                        signal: AbortSignal.timeout(5000) // 5秒超时\n" +
            "                    });\n" +
            "                    if (response.ok) {\n" +
            "                        if (!serverOnline) {\n" +
            "                            serverOnline = true;\n" +
            "                            document.getElementById('status-text').textContent = '服务器已恢复连接';\n" +
            "                        }\n" +
            "                    } else {\n" +
            "                        handleServerOffline();\n" +
            "                    }\n" +
            "                } catch (error) {\n" +
            "                    handleServerOffline();\n" +
            "                }\n" +
            "            }, 10000); // 每10秒检查一次\n" +
            "        }\n" +
            "        \n" +
            "        function handleServerOffline() {\n" +
            "            if (serverOnline) {\n" +
            "                serverOnline = false;\n" +
            "                document.getElementById('status-text').textContent = '⚠️ 服务器已关闭';\n" +
            "                alert('游戏服务器已关闭！\\n\\n语音聊天功能已不可用。\\n请关闭此页面。');\n" +
            "                // 清理资源\n" +
            "                if (currentChannel) {\n" +
            "                    leaveChannel();\n" +
            "                }\n" +
            "                if (heartbeatInterval) {\n" +
            "                    clearInterval(heartbeatInterval);\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function fetchPlayerInfo() {\n" +
            "            try {\n" +
            "                const response = await fetch(`/api/player-info?uuid=${playerUUID}`);\n" +
            "                if (!response.ok) {\n" +
            "                    throw new Error('服务器响应错误');\n" +
            "                }\n" +
            "                const data = await response.json();\n" +
            "                playerRace = data.race;\n" +
            "                document.getElementById('player-race').textContent = `你的种族: ${playerRace}`;\n" +
            "            } catch (error) {\n" +
            "                console.error('获取玩家信息失败:', error);\n" +
            "                document.getElementById('status-text').textContent = '服务器已关闭，语音功能不可用';\n" +
            "                alert('无法连接到游戏服务器！\\n\\n语音聊天需要游戏服务器开启才能使用。\\n请确保 Minecraft 服务器正在运行。');\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function loadChannels() {\n" +
            "            try {\n" +
            "                const response = await fetch('/api/channels');\n" +
            "                if (!response.ok) {\n" +
            "                    throw new Error('服务器不可用');\n" +
            "                }\n" +
            "                const data = await response.json();\n" +
            "                renderChannels(data.channels);\n" +
            "            } catch (error) {\n" +
            "                console.error('加载频道列表失败:', error);\n" +
            "                document.getElementById('status-text').textContent = '服务器连接失败';\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function renderChannels(channels) {\n" +
            "            const container = document.getElementById('channels-list');\n" +
            "            container.innerHTML = '';\n" +
            "            channels.forEach(channel => {\n" +
            "                const div = document.createElement('div');\n" +
            "                div.className = 'channel';\n" +
            "                const isPlayerRace = channel.name === playerRace;\n" +
            "                if (!isPlayerRace) div.classList.add('locked');\n" +
            "                if (channel.name === currentChannel) div.classList.add('active');\n" +
            "                div.innerHTML = `<div class=\\\"channel-name\\\">${isPlayerRace ? '✓ ' : '🔒 '}${channel.name}</div>" +
            "                    <div class=\\\"channel-info\\\">${channel.playerCount} 人在线</div>`;\n" +
            "                if (isPlayerRace) div.onclick = () => joinChannel(channel.name);\n" +
            "                container.appendChild(div);\n" +
            "            });\n" +
            "        }\n" +
            "        \n" +
            "        async function joinChannel(raceName) {\n" +
            "            try {\n" +
            "                const response = await fetch('/api/join-channel', {\n" +
            "                    method: 'POST',\n" +
            "                    headers: { 'Content-Type': 'application/json' },\n" +
            "                    body: JSON.stringify({ uuid: playerUUID, race: raceName })\n" +
            "                });\n" +
            "                if (response.ok) {\n" +
            "                    currentChannel = raceName;\n" +
            "                    document.getElementById('status-text').textContent = `已加入 ${raceName} 频道`;\n" +
            "                    await requestMicrophone();\n" +
            "                    startSignaling();\n" +
            "                    await notifyJoin();\n" +
            "                } else {\n" +
            "                    alert('加入频道失败：种族不匹配');\n" +
            "                }\n" +
            "            } catch (error) {\n" +
            "                console.error('加入频道失败:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function leaveChannel() {\n" +
            "            if (localStream) {\n" +
            "                localStream.getTracks().forEach(track => track.stop());\n" +
            "                localStream = null;\n" +
            "            }\n" +
            "            Object.values(peerConnections).forEach(pc => pc.close());\n" +
            "            peerConnections = {};\n" +
            "            if (pollingInterval) {\n" +
            "                clearInterval(pollingInterval);\n" +
            "                pollingInterval = null;\n" +
            "            }\n" +
            "            try {\n" +
            "                await fetch('/api/leave-channel', {\n" +
            "                    method: 'POST',\n" +
            "                    headers: { 'Content-Type': 'application/json' },\n" +
            "                    body: JSON.stringify({ uuid: playerUUID })\n" +
            "                });\n" +
            "                currentChannel = null;\n" +
            "                isMicActive = false;\n" +
            "                updateMicButton();\n" +
            "                document.getElementById('status-text').textContent = '已离开频道';\n" +
            "            } catch (error) {\n" +
            "                console.error('离开频道失败:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function requestMicrophone() {\n" +
            "            // 检查是否为安全上下文\n" +
            "            const isSecureContext = window.isSecureContext || location.protocol === 'https:' || \n" +
            "                                     location.hostname === 'localhost' || location.hostname === '127.0.0.1';\n" +
            "            \n" +
            "            // 检查API是否可用\n" +
            "            if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {\n" +
            "                let errorMsg = '无法访问麦克风！\\n\\n';\n" +
            "                \n" +
            "                if (!isSecureContext) {\n" +
            "                    errorMsg += '❌ 原因：使用HTTP访问局域网地址\\n';\n" +
            "                    errorMsg += '浏览器安全策略阻止了麦克风访问\\n\\n';\n" +
            "                    errorMsg += '✅ 解决方案：\\n';\n" +
            "                    errorMsg += '1. 在服务器本机访问（使用localhost）\\n';\n" +
            "                    errorMsg += '2. 管理员配置HTTPS证书\\n\\n';\n" +
            "                    errorMsg += '当前地址：' + location.hostname + '\\n';\n" +
            "                    errorMsg += '协议：' + location.protocol;\n" +
            "                } else {\n" +
            "                    errorMsg += '请确保：\\n';\n" +
            "                    errorMsg += '1. 使用现代浏览器（Chrome/Firefox/Edge）\\n';\n" +
            "                    errorMsg += '2. 已允许麦克风权限';\n" +
            "                }\n" +
            "                \n" +
            "                alert(errorMsg);\n" +
            "                document.getElementById('status-text').textContent = '麦克风 API 不可用';\n" +
            "                return;\n" +
            "            }\n" +
            "            \n" +
            "            try {\n" +
            "                localStream = await navigator.mediaDevices.getUserMedia({ \n" +
            "                    audio: {\n" +
            "                        echoCancellation: true,\n" +
            "                        noiseSuppression: true,\n" +
            "                        autoGainControl: true\n" +
            "                    }\n" +
            "                });\n" +
            "                document.getElementById('status-text').textContent = '麦克风已就绪，点击按钮开始说话';\n" +
            "            } catch (error) {\n" +
            "                console.error('麦克风权限获取失败:', error);\n" +
            "                let errorMsg = '无法访问麦克风！\\n\\n';\n" +
            "                if (error.name === 'NotAllowedError') {\n" +
            "                    errorMsg += '错误：权限被拒绝\\n请在浏览器设置中允许麦克风访问';\n" +
            "                } else if (error.name === 'NotFoundError') {\n" +
            "                    errorMsg += '错误：未找到麦克风设备\\n请检查麦克风是否正确连接';\n" +
            "                } else if (error.name === 'NotReadableError') {\n" +
            "                    errorMsg += '错误：麦克风被占用\\n请关闭其他使用麦克风的程序';\n" +
            "                } else {\n" +
            "                    errorMsg += '错误：' + error.message;\n" +
            "                }\n" +
            "                alert(errorMsg);\n" +
            "                document.getElementById('status-text').textContent = '麦克风访问失败';\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function toggleMic() {\n" +
            "            if (!currentChannel) {\n" +
            "                alert('请先选择一个频道');\n" +
            "                return;\n" +
            "            }\n" +
            "            if (!localStream) {\n" +
            "                alert('麦克风未就绪');\n" +
            "                return;\n" +
            "            }\n" +
            "            isMicActive = !isMicActive;\n" +
            "            localStream.getAudioTracks()[0].enabled = isMicActive;\n" +
            "            updateMicButton();\n" +
            "        }\n" +
            "        \n" +
            "        function updateMicButton() {\n" +
            "            const button = document.getElementById('mic-button');\n" +
            "            if (isMicActive) {\n" +
            "                button.classList.add('active');\n" +
            "                document.getElementById('status-text').textContent = '🎤 正在说话...';\n" +
            "            } else {\n" +
            "                button.classList.remove('active');\n" +
            "                document.getElementById('status-text').textContent = '🔇 麦克风已静音';\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        function startSignaling() {\n" +
            "            pollingInterval = setInterval(pollSignals, 1000);\n" +
            "        }\n" +
            "        \n" +
            "        async function pollSignals() {\n" +
            "            try {\n" +
            "                const response = await fetch(`/api/poll-signal?uuid=${playerUUID}`);\n" +
            "                const data = await response.json();\n" +
            "                if (data.messages && data.messages.length > 0) {\n" +
            "                    for (const message of data.messages) {\n" +
            "                        await handleSignal(message);\n" +
            "                    }\n" +
            "                }\n" +
            "            } catch (error) {\n" +
            "                console.error('轮询信令失败:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function handleSignal(message) {\n" +
            "            const { from, type, data } = message;\n" +
            "            \n" +
            "            if (type === 'offer') {\n" +
            "                await handleOffer(from, data);\n" +
            "            } else if (type === 'answer') {\n" +
            "                await handleAnswer(from, data);\n" +
            "            } else if (type === 'ice-candidate') {\n" +
            "                await handleIceCandidate(from, data);\n" +
            "            } else if (type === 'join') {\n" +
            "                await createOffer(from);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function createPeerConnection(remoteUUID) {\n" +
            "            const pc = new RTCPeerConnection(configuration);\n" +
            "            \n" +
            "            pc.onicecandidate = (event) => {\n" +
            "                if (event.candidate) {\n" +
            "                    sendSignal(remoteUUID, 'ice-candidate', event.candidate);\n" +
            "                }\n" +
            "            };\n" +
            "            \n" +
            "            pc.ontrack = (event) => {\n" +
            "                const audio = new Audio();\n" +
            "                audio.srcObject = event.streams[0];\n" +
            "                audio.play();\n" +
            "            };\n" +
            "            \n" +
            "            if (localStream) {\n" +
            "                localStream.getTracks().forEach(track => {\n" +
            "                    pc.addTrack(track, localStream);\n" +
            "                });\n" +
            "            }\n" +
            "            \n" +
            "            peerConnections[remoteUUID] = pc;\n" +
            "            return pc;\n" +
            "        }\n" +
            "        \n" +
            "        async function createOffer(remoteUUID) {\n" +
            "            const pc = await createPeerConnection(remoteUUID);\n" +
            "            const offer = await pc.createOffer();\n" +
            "            await pc.setLocalDescription(offer);\n" +
            "            sendSignal(remoteUUID, 'offer', offer);\n" +
            "        }\n" +
            "        \n" +
            "        async function handleOffer(remoteUUID, offer) {\n" +
            "            const pc = await createPeerConnection(remoteUUID);\n" +
            "            await pc.setRemoteDescription(new RTCSessionDescription(offer));\n" +
            "            const answer = await pc.createAnswer();\n" +
            "            await pc.setLocalDescription(answer);\n" +
            "            sendSignal(remoteUUID, 'answer', answer);\n" +
            "        }\n" +
            "        \n" +
            "        async function handleAnswer(remoteUUID, answer) {\n" +
            "            const pc = peerConnections[remoteUUID];\n" +
            "            if (pc) {\n" +
            "                await pc.setRemoteDescription(new RTCSessionDescription(answer));\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function handleIceCandidate(remoteUUID, candidate) {\n" +
            "            const pc = peerConnections[remoteUUID];\n" +
            "            if (pc) {\n" +
            "                await pc.addIceCandidate(new RTCIceCandidate(candidate));\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function sendSignal(to, type, data) {\n" +
            "            try {\n" +
            "                await fetch('/api/signal', {\n" +
            "                    method: 'POST',\n" +
            "                    headers: { 'Content-Type': 'application/json' },\n" +
            "                    body: JSON.stringify({\n" +
            "                        from: playerUUID,\n" +
            "                        to: to,\n" +
            "                        type: type,\n" +
            "                        data: data\n" +
            "                    })\n" +
            "                });\n" +
            "            } catch (error) {\n" +
            "                console.error('发送信令失败:', error);\n" +
            "            }\n" +
            "        }\n" +
            "        \n" +
            "        async function notifyJoin() {\n" +
            "            await fetch('/api/signal', {\n" +
            "                method: 'POST',\n" +
            "                headers: { 'Content-Type': 'application/json' },\n" +
            "                body: JSON.stringify({\n" +
            "                    from: playerUUID,\n" +
            "                    type: 'join',\n" +
            "                    data: null\n" +
            "                })\n" +
            "            });\n" +
            "        }\n" +
            "        \n" +
            "        window.addEventListener('beforeunload', () => {\n" +
            "            if (currentChannel) leaveChannel();\n" +
            "            if (heartbeatInterval) clearInterval(heartbeatInterval);\n" +
            "        });\n" +
            "        \n" +
            "        initialize();\n";
    }
}
