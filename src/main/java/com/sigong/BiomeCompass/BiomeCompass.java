package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import javax.naming.Name;
import java.util.ArrayList;
import java.util.List;

public class BiomeCompass extends JavaPlugin {
    // The version of the config file this was programmed against. (Unused until config version changes.)
    private static final int CONFIG_VERSION = 1;

    @Override
    public void onEnable(){
        // Save the default config file if no file is present, then retrieve all values from the config file
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        ConfigValues configValues = new ConfigValues(config.getInt("config-version"),
                                                    new ArrayList<>((List<String>)config.getList("enabled-worlds")),
                                                    config.getInt("chunk-search-radius"),
                                                    config.getInt("unchecked-chunk-limit"),
                                                    config.getBoolean("manual-update-enabled"),
                                                    config.getInt("manual-update-interval"),
                                                    config.getBoolean("manual-update-consumes-item"),
                                                    Material.valueOf(config.getString("manual-update-fuel")),
                                                    config.getInt("manual-update-fuel-amount"),
                                                    config.getBoolean("automatic-update-enabled"),
                                                    config.getInt("automatic-update-interval"),
                                                    config.getBoolean("automatic-update-consumes-item"),
                                                    Material.valueOf(config.getString("automatic-update-fuel")),
                                                    config.getInt("automatic-update-fuel-amount"));

        // TODO: Add message customization here (save the default file, retrieve what the server's version)

        // Create record of NamespacedKeys for use in other classes
        NamespacedKeyHolder keyHolder = new NamespacedKeyHolder(new NamespacedKey(this, "target-biome"),
                                                                new NamespacedKey(this, "last-search-position-x"),
                                                                new NamespacedKey(this, "last-search-position-z"));

        //register compass helper class if not static
        BiomeSearchManager searcher = new BiomeSearchManager(this, keyHolder, configValues);

        //Register createcompass command executor
        getCommand("createcompass").setExecutor(new CommandCreateCompass(this, searcher, keyHolder));

        // TODO: add CommandExecutor for "/biomecompass reload"
        getCommand("biomecompass").setExecutor(new CommandBiomeCompass(this, configValues));

        //Register events to the class that controls when a compass is updated
        Bukkit.getPluginManager().registerEvents(new CompassUpdateController(this, searcher, keyHolder, configValues), this);

        getLogger().info("BiomeCompass has been enabled.");
    }

    @Override
    public void onDisable(){
        getLogger().info("BiomeCompass has been Disabled.");
    }
}
