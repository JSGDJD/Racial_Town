                                                                          package org.HUD.hotelRoom.race;

import org.HUD.hotelRoom.HotelRoom;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 种族语音聊天管理器
 * 管理玩家的种族语音连接状态
 */
public class RaceVoiceManager {
    
    private static RaceVoiceManager instance;
    private final HotelRoom plugin;
    
    // 存储玩家当前所在的语音频道 <PlayerUUID, RaceName>
    private final Map<UUID, String> playerChannels = new ConcurrentHashMap<>();
    
    // 存储每个频道中的玩家列表 <RaceName, Set<PlayerUUID>>
    private final Map<String, Set<UUID>> channelPlayers = new ConcurrentHashMap<>();
    
    // 存储玩家的WebRTC连接状态
    private final Map<UUID, Boolean> playerConnected = new ConcurrentHashMap<>();
    
    // HTTP服务器引用
    private RaceVoiceServer voiceServer;
    
    // race.yml配置
    private YamlConfiguration raceConfig;
    
    private RaceVoiceManager(HotelRoom plugin) {
        this.plugin = plugin;
        loadConfig();
    }
    
    public static void initialize(HotelRoom plugin) {
        if (instance == null) {
            instance = new RaceVoiceManager(plugin);
        }
    }
    
    public static RaceVoiceManager getInstance() {
        return instance;
    }
    
    /**
     * 加载race.yml配置
     */
    private void loadConfig() {
        File raceConfigFile = new File(plugin.getDataFolder(), "race.yml");
        if (!raceConfigFile.exists()) {
            plugin.saveResource("race.yml", false);
        }
        raceConfig = YamlConfiguration.loadConfiguration(raceConfigFile);
    }
    
    /**
     * 重载配置
     */
    public void reload() {
        loadConfig();
        plugin.getLogger().info("种族语音配置已重载");
    }
    
    /**
     * 重启语音服务器（用于端口更改等）
     */
    public void restartServer() {
        stopServer();
        
        // 异步延迟重启，确保端口完全释放
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            try {
                startServer();
                plugin.getLogger().info("种族语音服务器已重启");
            } catch (IOException e) {
                plugin.getLogger().severe("种族语音服务器重启失败: " + e.getMessage());
                e.printStackTrace();
            }
        }, 10L); // 延迟 0.5 秒（10 ticks）
    }
    
    /**
     * 启动语音服务器
     */
    public void startServer() throws IOException {
        if (voiceServer != null) {
            plugin.getLogger().warning("语音服务器已经在运行中，跳过启动");
            return;
        }
        
        int port = raceConfig.getInt("race-voice.port", 8888);
        voiceServer = new RaceVoiceServer(port, this);
        voiceServer.start();
        
        String serverUrl = getVoiceServerUrl();
        
        plugin.getLogger().info("种族语音服务器已启动");
        plugin.getLogger().info("访问地址: " + serverUrl);
        plugin.getLogger().info("局域网内所有玩家均可访问此地址");
    }
    
    /**
     * 停止语音服务器
     */
    public void stopServer() {
        if (voiceServer != null) {
            voiceServer.stop();
            voiceServer = null;
            plugin.getLogger().info("种族语音服务器已停止");
        }
    }
    
    /**
     * 获取玩家的当前种族
     */
    public String getPlayerRace(UUID playerUUID) {
        // 从数据库或配置中获取玩家种族
        // 这里需要根据你的种族系统实现来调整
        Player player = plugin.getServer().getPlayer(playerUUID);
        if (player != null) {
            // 临时实现：从玩家的持久化数据或权限组获取种族
            // 你需要根据实际的种族系统来实现这个方法
            return RaceDataStorage.getPlayerRace(playerUUID);
        }
        return "human"; // 默认种族
    }
    
    /**
     * 玩家加入种族频道
     */
    public boolean joinChannel(UUID playerUUID, String raceName) {
        // 验证玩家种族是否匹配
        String playerRace = getPlayerRace(playerUUID);
        if (!playerRace.equalsIgnoreCase(raceName)) {
            return false; // 种族不匹配，不允许加入
        }
        
        // 如果玩家已在其他频道，先离开
        leaveChannel(playerUUID);
        
        // 加入新频道
        playerChannels.put(playerUUID, raceName);
        channelPlayers.computeIfAbsent(raceName, k -> ConcurrentHashMap.newKeySet()).add(playerUUID);
        
        plugin.getLogger().info("玩家 " + playerUUID + " 加入了 " + raceName + " 语音频道");
        return true;
    }
    
    /**
     * 玩家离开当前频道
     */
    public void leaveChannel(UUID playerUUID) {
        String currentChannel = playerChannels.remove(playerUUID);
        if (currentChannel != null) {
            Set<UUID> players = channelPlayers.get(currentChannel);
            if (players != null) {
                players.remove(playerUUID);
                if (players.isEmpty()) {
                    channelPlayers.remove(currentChannel);
                }
            }
            plugin.getLogger().info("玩家 " + playerUUID + " 离开了 " + currentChannel + " 语音频道");
        }
        playerConnected.remove(playerUUID);
    }
    
    /**
     * 获取玩家当前所在的频道
     */
    public String getPlayerChannel(UUID playerUUID) {
        return playerChannels.get(playerUUID);
    }
    
    /**
     * 获取频道中的所有玩家
     */
    public Set<UUID> getChannelPlayers(String raceName) {
        return new HashSet<>(channelPlayers.getOrDefault(raceName, Collections.emptySet()));
    }
    
    /**
     * 设置玩家连接状态
     */
    public void setPlayerConnected(UUID playerUUID, boolean connected) {
        playerConnected.put(playerUUID, connected);
    }
    
    /**
     * 检查玩家是否已连接
     */
    public boolean isPlayerConnected(UUID playerUUID) {
        return playerConnected.getOrDefault(playerUUID, false);
    }
    
    /**
     * 获取所有可用的种族列表
     */
    public List<String> getAvailableRaces() {
        // 从race.yml配置文件中读取种族列表
        return raceConfig.getStringList("race-voice.available-races");
    }
    
    /**
     * 获取语音服务器URL
     */
    public String getVoiceServerUrl() {
        int port = raceConfig.getInt("race-voice.port", 8888);
        String host = raceConfig.getString("race-voice.host", "auto");
        
        // 如果配置为auto，自动获取局域网IP
        if ("auto".equalsIgnoreCase(host)) {
            host = getServerIP();
        }
        
        return "http://" + host + ":" + port;
    }
    
    /**
     * 自动获取服务器IP地址
     */
    private String getServerIP() {
        try {
            // 尝试获取局域网IP
            java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
            String ip = localHost.getHostAddress();
            
            // 如果是127.0.0.1，尝试获取真实局域网IP
            if ("127.0.0.1".equals(ip) || "localhost".equals(ip)) {
                java.util.Enumeration<java.net.NetworkInterface> interfaces = 
                    java.net.NetworkInterface.getNetworkInterfaces();
                
                while (interfaces.hasMoreElements()) {
                    java.net.NetworkInterface iface = interfaces.nextElement();
                    
                    // 跳过回环和非活动的接口
                    if (iface.isLoopback() || !iface.isUp()) {
                        continue;
                    }
                    
                    java.util.Enumeration<java.net.InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        java.net.InetAddress addr = addresses.nextElement();
                        
                        // 只要IPv4地址
                        if (addr instanceof java.net.Inet4Address && !addr.isLoopbackAddress()) {
                            return addr.getHostAddress();
                        }
                    }
                }
            }
            
            return ip;
        } catch (Exception e) {
            plugin.getLogger().warning("无法获取服务器IP，使用localhost: " + e.getMessage());
            return "localhost";
        }
    }
    
    /**
     * 清理断线玩家
     */
    public void cleanupDisconnectedPlayers() {
        playerChannels.keySet().removeIf(uuid -> {
            Player player = plugin.getServer().getPlayer(uuid);
            return player == null || !player.isOnline();
        });
        
        channelPlayers.values().forEach(players -> 
            players.removeIf(uuid -> {
                Player player = plugin.getServer().getPlayer(uuid);
                return player == null || !player.isOnline();
            })
        );
    }
    
    /**
     * 获取频道信息（用于网页显示）
     */
    public Map<String, Integer> getChannelInfo() {
        Map<String, Integer> info = new HashMap<>();
        for (String race : getAvailableRaces()) {
            int playerCount = channelPlayers.getOrDefault(race, Collections.emptySet()).size();
            info.put(race, playerCount);
        }
        return info;
    }
}
