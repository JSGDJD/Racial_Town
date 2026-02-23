package org.HUD.hotelRoom.house;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.HUD.hotelRoom.HotelRoom;

import java.io.File;

public final class HouseConfig {

    private final HotelRoom plugin;
    private YamlConfiguration cfg;

    /* 默认值 */
    private Material navMaterial   = Material.COMPASS;
    private String navDisplay      = "§a§l导航中 → {house}";
    private String navModelData    = "0";   // int 也可，解析时转 int
    private java.util.List<String> navLore = java.util.List.of(
            "§7跟随我即可到达目的地",
            "§7距离：§e{distance} m"
    );

    public HouseConfig(HotelRoom plugin){
        this.plugin = plugin;
    }

    /* 第一次加载 /reload 都走这里 */
    public void load(){
        File file = new File(plugin.getDataFolder(), "house.yml");
        if(!file.exists()){
            plugin.saveResource("house.yml", false); // 把 jar 里默认的 house.yml 释放出来
        }
        cfg = YamlConfiguration.loadConfiguration(file);

        navMaterial   = Material.matchMaterial(cfg.getString("navigation-item.type", "COMPASS"));
        navDisplay    = cfg.getString("navigation-item.name", navDisplay);
        navModelData  = cfg.getString("navigation-item.model-data", "0");
        navLore       = cfg.getStringList("navigation-item.lore");
        if(navMaterial == null) navMaterial = Material.COMPASS;
    }

    /* ===== Getters ===== */
    public Material getNavMaterial() { return navMaterial; }
    public String getNavDisplay()    { return navDisplay; }
    public int getNavModelData(){
        try{ return Integer.parseInt(navModelData); }
        catch(NumberFormatException e){ return 0; }
    }
    public java.util.List<String> getNavLore(){ return navLore; }
}
