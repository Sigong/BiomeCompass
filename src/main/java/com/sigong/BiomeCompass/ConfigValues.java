package com.sigong.BiomeCompass;

import org.bukkit.Material;

import java.util.ArrayList;

public record ConfigValues(
    int configVersion,

    ArrayList<String> enabledWorldNames,

    int chunkSearchRadius,
    int uncheckedChunkLimit,

    boolean manualUpdateEnabled,
    int manualUpdateInterval,
    boolean manualUpdateConsumesItem,
    Material manualUpdateFuel,
    int manualUpdateFuelAmount,

    boolean automaticUpdateEnabled,
    int automaticUpdateInterval,
    boolean automaticUpdateConsumesItem,
    Material automaticUpdateFuel,
    int automaticUpdateFuelAmount
){}
