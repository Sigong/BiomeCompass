package com.sigong.BiomeCompass;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

//TODO: Use configurate library to streamline this class
public class ConfigValues{
    public ConfigValues(BiomeCompass instance){
        this.instance = instance;
        loadValuesFromConfig();
    }

    // The version of the config file this was programmed against. (Unused until config version changes.)
    private static final int CONFIG_VERSION = 1;

    private final BiomeCompass instance;

    private int configVersion;

    private ArrayList<String> enabledWorldNames;

    private int chunkSearchRadius;

    private int uncheckedChunkLimit;

    private boolean manualUpdateEnabled;
    private int manualUpdateInterval;
    private boolean manualUpdateConsumesItem;
    private Material manualUpdateFuel;
    private int manualUpdateFuelAmount;

    private boolean automaticUpdateEnabled;
    private int automaticUpdateInterval;
    private boolean automaticUpdateConsumesItem;
    private  Material automaticUpdateFuel;
    private  int automaticUpdateFuelAmount;

    public void loadValuesFromConfig(){
        FileConfiguration config = instance.getConfig();

        configVersion = config.getInt("config-version");

        enabledWorldNames = new ArrayList<>((List<String>)config.getList("enabled-worlds"));

        chunkSearchRadius = config.getInt("chunk-search-radius");
        uncheckedChunkLimit = config.getInt("unchecked-chunk-limit");

        manualUpdateEnabled = config.getBoolean("manual-update-enabled");
        manualUpdateInterval = config.getInt("manual-update-interval");
        manualUpdateConsumesItem = config.getBoolean("manual-update-consumes-item");
        manualUpdateFuel = Material.valueOf(config.getString("manual-update-fuel"));
        manualUpdateFuelAmount = config.getInt("manual-update-fuel-amount");

        automaticUpdateEnabled = config.getBoolean("automatic-update-enabled");
        automaticUpdateInterval = config.getInt("automatic-update-interval");
        automaticUpdateConsumesItem = config.getBoolean("automatic-update-consumes-item");
        automaticUpdateFuel = Material.valueOf(config.getString("automatic-update-fuel"));
        automaticUpdateFuelAmount = config.getInt("automatic-update-fuel-amount");

    }

    public ArrayList<String> getEnabledWorldNames() {
        return enabledWorldNames;
    }

    public int getChunkSearchRadius() {
        return chunkSearchRadius;
    }

    public int getUncheckedChunkLimit() {
        return uncheckedChunkLimit;
    }

    public boolean isManualUpdateEnabled() {
        return manualUpdateEnabled;
    }

    public int getManualUpdateInterval() {
        return manualUpdateInterval;
    }

    public boolean getManualUpdateConsumesItem() {
        return manualUpdateConsumesItem;
    }

    public Material getManualUpdateFuel() {
        return manualUpdateFuel;
    }

    public int getManualUpdateFuelAmount() {
        return manualUpdateFuelAmount;
    }

    public boolean isAutomaticUpdateEnabled() {
        return automaticUpdateEnabled;
    }

    public int getAutomaticUpdateInterval() {
        return automaticUpdateInterval;
    }

    public boolean getAutomaticUpdateConsumesItem() {
        return automaticUpdateConsumesItem;
    }

    public Material getAutomaticUpdateFuel() {
        return automaticUpdateFuel;
    }

    public int getAutomaticUpdateFuelAmount() {
        return automaticUpdateFuelAmount;
    }
}
