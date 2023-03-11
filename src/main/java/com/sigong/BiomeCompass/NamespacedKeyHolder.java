package com.sigong.BiomeCompass;

import org.bukkit.NamespacedKey;

public record NamespacedKeyHolder(
        NamespacedKey targetBiomeKey,
        NamespacedKey lastSearchLocationX,
        NamespacedKey lastSearchLocationZ
) {
}
