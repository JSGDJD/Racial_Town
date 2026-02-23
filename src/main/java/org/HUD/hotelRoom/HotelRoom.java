package org.HUD.hotelRoom;

import org.HUD.hotelRoom.attribute.AttributeManager;
import org.HUD.hotelRoom.attribute.CustomAttributeManager;
import org.HUD.hotelRoom.attribute.MythicMobsAttributeManager;
import org.HUD.hotelRoom.attribute.MythicMobsIntegration;
import org.HUD.hotelRoom.attribute.command.AttributeCommand;
import org.HUD.hotelRoom.attribute.listener.AttributeListener;
import org.HUD.hotelRoom.attribute.listener.CombatAttributeListener;
import org.HUD.hotelRoom.attribute.listener.ItemAttributeListener;
import org.HUD.hotelRoom.command.*;
import org.HUD.hotelRoom.family.FamilyManager;
import org.HUD.hotelRoom.family.gui.FamilyApplicationGUI;
import org.HUD.hotelRoom.family.command.FamilyCommand;
import org.HUD.hotelRoom.family.gui.FamilyMainGUI;

import org.HUD.hotelRoom.gui.*;
import org.HUD.hotelRoom.honor.HonorDecayManager;
import org.HUD.hotelRoom.house.HouseConfig;
import org.HUD.hotelRoom.house.HouseListCmd;
import org.HUD.hotelRoom.house.HouseListGUI;
import org.HUD.hotelRoom.papi.HotelRoomPapi;
import org.HUD.hotelRoom.protection.FacadeSnapshotQueue;
import org.HUD.hotelRoom.protection.ProtectionListener;
import org.HUD.hotelRoom.race.RaceAttributeManager;
import org.HUD.hotelRoom.race.RaceDataStorage;
import org.HUD.hotelRoom.race.RaceEvolutionManager;
import org.HUD.hotelRoom.race.RaceExpManager;
import org.HUD.hotelRoom.race.RaceVoiceManager;
import org.HUD.hotelRoom.race.command.RaceExpCommand;
import org.HUD.hotelRoom.race.command.RaceVoiceCommand;
import org.HUD.hotelRoom.race.listener.RaceExpListener;
import org.HUD.hotelRoom.race.listener.RaceVoiceQuitListener;

import org.HUD.hotelRoom.util.DailyHonorManager;
import org.HUD.hotelRoom.util.SQLiteStorage;
import org.HUD.hotelRoom.util.SelectionMgr;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;

public final class HotelRoom extends JavaPlugin {

    private static HotelRoom inst;
    private SelectionMgr selectionMgr;
    private HouseConfig houseConfig;
    private ProtectionListener protectionListener;
    @Override
    public void onEnable() {
        inst = this;
        
        saveDefaultConfig();
        
        SQLiteStorage.open();
        
        RaceDataStorage.initialize();
        AttributeManager.initialize(this);
        CustomAttributeManager.initialize(this);
        MythicMobsAttributeManager.initialize(this);
        RaceExpManager.initialize(this);
        RaceEvolutionManager.initialize(this);
        RaceAttributeManager.initialize(this);
        
        selectionMgr = new SelectionMgr(this);
        
        Bukkit.getScheduler().runTaskLater(this, () -> {
            SelectionMgr.HOTELS.putAll(SQLiteStorage.loadAll());
            SelectionMgr.HOTELS.values().forEach(h -> selectionMgr.addIndex(h));
            getLogger().info("HotelRoom：已加载 " + SelectionMgr.HOTELS.size() + " 个领地并建立空间索引");
        }, 20L);
        
        FacadeSnapshotQueue.startWorker(this);
        
        CooldownManager.reload(getConfig());
        houseConfig = new HouseConfig(this);
        houseConfig.load();
        
        DailyHonorManager.loadConfig(getConfig());
        
        getServer().getPluginManager().registerEvents(new AttributeListener(), this);
        getServer().getPluginManager().registerEvents(new ItemAttributeListener(), this);
        getServer().getPluginManager().registerEvents(new CombatAttributeListener(), this);
        
        try {
            Class.forName("io.lumine.mythic.bukkit.MythicBukkit");
            new MythicMobsIntegration(this);
            new org.HUD.hotelRoom.attribute.MythicMobsVariableProvider(this);
            if (AttributeManager.isDamageLogEnabled()) {
                this.getLogger().info("[MythicMobs] 已初始化集成和变量提供者");
            }
        } catch (ClassNotFoundException e) {
            if (AttributeManager.isDamageLogEnabled()) {
                this.getLogger().info("[MythicMobs] 未安装，跳过集成");
            }
        }
        
        getServer().getPluginManager().registerEvents(selectionMgr, this);
        protectionListener = new ProtectionListener(this);
        getServer().getPluginManager().registerEvents(protectionListener, this);

        if(Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null){
            new HotelRoomPapi().register();
            getLogger().info("HotelRoom PAPI 扩展已载入");
        }

        getCommand("givehotel").setExecutor(new GiveHotelCmd());
        getCommand("removehotel").setExecutor(new RemoveHotelCmd());
        getCommand("createhotel").setExecutor(new CreateCmd(selectionMgr));
        getCommand("createpublichotel").setExecutor(new CreatePublicHotelCmd(selectionMgr));
        getCommand("createofficialhotel").setExecutor(new CreateOfficialHotelCmd(selectionMgr));
        getCommand("addhotelplayer").setExecutor(new AddHotelPlayerCmd());
        getCommand("removehotelplayer").setExecutor(new RemoveHotelPlayerCmd());
        getCommand("hotelroom").setExecutor(new HotelRoomCmd());
        
        getCommand("debugattr").setExecutor(new org.HUD.hotelRoom.attribute.command.DebugAttributeCommand());
        getCommand("testarmor").setExecutor(new org.HUD.hotelRoom.attribute.command.TestArmorDefenseCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerRightClickPlayer(), this);
        getServer().getPluginManager().registerEvents(new HonorGUI(), this);
        getServer().getPluginManager().registerEvents(new AbandonConfirmGUI(), this);
        getServer().getPluginManager().registerEvents(new org.HUD.hotelRoom.listener.PlayerJoinListener(), this);
        getServer().getPluginManager().registerEvents(new org.HUD.hotelRoom.listener.GoldenAppleListener(), this);

        HonorDecayManager.start(this);
        getCommand("hrlist").setExecutor(new HouseListCmd());
        getServer().getPluginManager().registerEvents(new HouseListGUI(), this);
        getServer().getPluginManager().registerEvents(new HonorReqSetGUI(), this);
        
        RaceVoiceManager.initialize(this);
        try {
            RaceVoiceManager.getInstance().startServer();
        } catch (IOException e) {
            getLogger().severe("种族语音服务器启动失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        getCommand("racevoice").setExecutor(new RaceVoiceCommand());
        
        RaceExpCommand raceExpCmd = new RaceExpCommand();
        getCommand("raceexp").setExecutor(raceExpCmd);
        getCommand("raceexp").setTabCompleter(raceExpCmd);
        
        AttributeCommand attrCmd = new AttributeCommand();
        getCommand("attr").setExecutor(attrCmd);
        getCommand("attr").setTabCompleter(attrCmd);
        
        getServer().getPluginManager().registerEvents(new RaceVoiceQuitListener(), this);
        getServer().getPluginManager().registerEvents(new RaceExpListener(), this);
        getCommand("racegui").setExecutor(new org.HUD.hotelRoom.command.RaceGUICommand());
        getCommand("changerace").setExecutor(new ChangeRaceCmd());
        getServer().getPluginManager().registerEvents(new org.HUD.hotelRoom.gui.RaceEvolutionGUI(), this);
        
        try {
            RaceDataStorage.loadRaceConfigs();
        } catch (Exception e) {
            getLogger().severe("加载种族配置失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        try {
            FamilyManager.initialize(this);
            getCommand("family").setExecutor(new FamilyCommand(this));
            FamilyMainGUI.getInstance();
            FamilyApplicationGUI.initialize(this);
            getLogger().info("家族系统已初始化");
        } catch (Exception e) {
            getLogger().severe("初始化家族系统失败: " + e.getMessage());
            e.printStackTrace();
        }

    }

    @Override
    public void onDisable() {
        if (RaceVoiceManager.getInstance() != null) {
            RaceVoiceManager.getInstance().stopServer();
            getLogger().info("种族语音服务器已关闭");
        }
        
        if (AttributeManager.getInstance() != null) {
            AttributeManager.getInstance().shutdown();
        }
        
        RaceDataStorage.close();
        
        SQLiteStorage.close();
        HouseListGUI.onDisable();
        selectionMgr.shutdown();
        
        if (FamilyManager.getInstance() != null) {
            FamilyManager.getInstance().saveData();
        }
    }
    public HouseConfig getHouseConfig(){ return houseConfig; }
    public ProtectionListener getProtectionListener() { return protectionListener; }
    public static HotelRoom get() { return inst; }
}