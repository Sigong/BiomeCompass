package com.sigong.BiomeCompass;

import org.bukkit.NamespacedKey;

public record NamespacedKeyHolder(
        NamespacedKey targetBiomeKey,
        NamespacedKey distanceAtLastSearchKey,
        NamespacedKey lastSearchLocationX,
        NamespacedKey lastSearchLocationZ
) {
}
