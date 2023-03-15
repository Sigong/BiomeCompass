package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class BiomeCompass extends JavaPlugin {
    public static NamespacedKey TARGET_BIOME_KEY;
    public static NamespacedKey LAST_SEARCH_LOCATION_CHUNK_X_KEY;
    public static NamespacedKey LAST_SEARCH_LOCATION_CHUNK_Z_KEY;

    @Override
    public void onEnable(){
        // Save the default config file if no file is present, then retrieve all values from the config file
        saveDefaultConfig();
        ConfigValues configValues = new ConfigValues(this);

        // TODO: Add message customization here (save the default file, retrieve what the server's version)

        // Create record of NamespacedKeys for use in other classes
        TARGET_BIOME_KEY = new NamespacedKey(this, "target-biome");
        LAST_SEARCH_LOCATION_CHUNK_X_KEY = new NamespacedKey(this, "last-search-position-x");
        LAST_SEARCH_LOCATION_CHUNK_Z_KEY = new NamespacedKey(this, "last-search-position-z");

        //register compass helper class if not static
        BiomeSearchManager searcher = new BiomeSearchManager(this, configValues);

        getCommand("createcompass").setExecutor(new CommandCreateCompass(this, searcher));
        getCommand("biomecompass").setExecutor(new CommandBiomeCompass(this, configValues));

        //Register events to the class that controls when a compass is updated
        Bukkit.getPluginManager().registerEvents(new CompassUpdateController(this, searcher, configValues), this);

        getLogger().info("BiomeCompass has been enabled.");
    }

    @Override
    public void onDisable(){
        getLogger().info("BiomeCompass has been Disabled.");
    }
}
