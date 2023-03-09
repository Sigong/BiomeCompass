package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

public class BiomeCompass extends JavaPlugin {

    private static final String RESOURCE_WORLD_VERSION = "1.9.8";

    @Override
    public void onEnable(){
        String detectedVersion = Bukkit.getPluginManager().getPlugin("ResourceWorld").getDescription().getVersion();
        if(!(RESOURCE_WORLD_VERSION.equals(detectedVersion))){
            this.getLogger().warning("This version of BiomeCompass was programmed against ResourceWorld version " + RESOURCE_WORLD_VERSION + " but you are using ResourceWorld version " + detectedVersion + ". Errors may occur.");
        }

        // Create record of NamespacedKeys for use in other classes
        NamespacedKeyHolder keyHolder = new NamespacedKeyHolder(new NamespacedKey(this, "target-biome"), null, null);

        //register compass helper class if not static
        BiomeSearchManager searcher = new BiomeSearchManager(this, keyHolder);

        //Register createcompass command executor
        getCommand("createcompass").setExecutor(new CommandCreateCompass(this, searcher, keyHolder));

        //Register events to the class that controls when a compass is updated
        Bukkit.getPluginManager().registerEvents(new CompassUpdateController(this, searcher, keyHolder), this);

        getLogger().info("BiomeCompass has been enabled.");
    }

    @Override
    public void onDisable(){
        getLogger().info("BiomeCompass has been Disabled.");
    }
}
