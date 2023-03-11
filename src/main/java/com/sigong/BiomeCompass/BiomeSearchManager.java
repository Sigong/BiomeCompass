package com.sigong.BiomeCompass;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.Locale;

public class BiomeSearchManager {
    public BiomeSearchManager(BiomeCompass instance, NamespacedKeyHolder keyHolder, ConfigValues configValues){
        this.instance = instance;
        this.keyHolder = keyHolder;
        this.configValues = configValues;
    }

    // Plugin main class instance
    private final BiomeCompass instance;

    // NamespacedKey Holder
    private final NamespacedKeyHolder keyHolder;

    // Holder for values retrieved from the plugin's config file
    private final ConfigValues configValues;

    // Creates a CompassMeta for a biome compass that points to the nearest biome of the desired type
    public CompassMeta createCompassMeta(Location startLocation, Biome targetBiome){
        CompassMeta compassMeta = (CompassMeta) new ItemStack(Material.COMPASS).getItemMeta();

        //The location of the nearest biome of the desired type
        Location biomeLocation = null;
        if(compassEnabledForWorld(startLocation.getWorld())) {
            biomeLocation = searchWorldForBiome(startLocation, targetBiome, configValues.chunkSearchRadius());
        }

        //If no biome was found (or search timed out), point to a location that will make the compass spin
        //TODO: figure out how to point to nonexistent world instead of the end
        if(biomeLocation == null) {
            biomeLocation = new Location(Bukkit.getWorld("world_the_end"), 0, 0, 0);
        }

        compassMeta.setLodestoneTracked(false);

        compassMeta.setLodestone(biomeLocation);

        // Store ordinal of target biome in PDC
        compassMeta.getPersistentDataContainer().set(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER, targetBiome.ordinal());

        // Store distance to target biome in PDC (or 0.0 if biome not found in this world)
        double distance = 0.0;
        if(startLocation.getWorld().equals(biomeLocation.getWorld())){
            distance = startLocation.distance(biomeLocation);
        }
        compassMeta.getPersistentDataContainer().set(keyHolder.lastSearchLocationX(), PersistentDataType.INTEGER, startLocation.getBlockX());
        compassMeta.getPersistentDataContainer().set(keyHolder.lastSearchLocationZ(), PersistentDataType.INTEGER, startLocation.getBlockZ());

        compassMeta.setDisplayName(ChatColor.RESET + "Biome Compass: " + WordUtils.capitalize(targetBiome.toString().toLowerCase(Locale.ROOT).replace('_', ' ')));

        //TODO: set compass lore to keep track of what world it is for, or what worlds it works in, or something else (if biome was found?)

        return compassMeta;
    }

    // Updates the target location of a biomecompass only if it needs to be updated
    // The compass needs to be updated only if there is a high enough number of unchecked chunks (as defined in config)
    // Unchecked chunks are chunks that haven't been searched, but are closer to the player than the compass target
    public CompassMeta updateCompassMeta(CompassMeta currentMeta, Player player, CompassUpdateReason reason) {
        Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "UPDATING COMPASS: " + ChatColor.RESET + reason.toString());

        if(!compassEnabledForWorld(player.getWorld())){
            if(reason == CompassUpdateReason.MANUAL){
                player.sendMessage(ChatColor.RED + "BiomeCompass is not enabled in this world.");
            }
            return currentMeta;
        }

        if(reason == CompassUpdateReason.MANUAL){
            if(false){ //TODO: figure out condition to check if the player updated it too recently to update again
                // TODO: Message about too soon to manually update again
                return currentMeta;
            }
            if(configValues.manualUpdateConsumesItem()){
                //TODO: consume item or return existing meta if not enough item to consume
            }
        }else if(reason == CompassUpdateReason.AUTOMATIC && configValues.automaticUpdateConsumesItem()){
            //TODO: consume item or return existing meta if not enough item to consume
        }

        Biome biome = Biome.values()[currentMeta.getPersistentDataContainer().get(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER)];

        int uncheckedChunks = 0; // TODO: use the formula to check for how many new blocks have been exposed, then perform a search if it exceeds the threshhold

        if(uncheckedChunks >= configValues.uncheckedChunkLimit()){
            Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "UNCHECKED CHUNKS WERE HIGH ENOUGH");
            return createCompassMeta(player.getLocation(), biome);
        }else{
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "UNCHECKED CHUNKS WERE NOT HIGH ENOUGH");
            return currentMeta;
        }
    }

    // Returns true if the compass is enabled in the given world
    private boolean compassEnabledForWorld(World world){
        return configValues.enabledWorldNames().contains(world.getName());
    }

    //Search the world for the target biome starting at a given location
    @Nullable
    private Location searchWorldForBiome(Location startLocation, Biome biome, int searchRadiusInChunks){
        //int worldSizeInChunks; //the distance from one world border to the other
        World world = startLocation.getWorld();

        double worldSizeInChunks = world.getWorldBorder().getSize()/16;

        int chunkX = startLocation.getChunk().getX();
        int chunkZ = startLocation.getChunk().getZ();

        //If the starting coordinates are inside the matrix
        if(checkCoordinatesWithinBorder(chunkX, chunkZ, worldSizeInChunks)){
            int count = 0;
            
            // The radius of chunks that are currently being searched
            int radius = 1;

            //TIMING INFORMATION
            long startTime = System.currentTimeMillis();

            //Check the starting Chunk
            int testX = chunkX;
            int testZ = chunkZ;
            Bukkit.getLogger().info("Checking Starting Chunk (" + chunkX + ", " + chunkZ + ")");
            //If test coordinates are in range and the biome matches
            if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                if(biomeMatchesTarget(world, testX, testZ, biome)) {
                    long endTime = System.currentTimeMillis();
                    Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                    return new Location(world, testX * 16, 255, testZ * 16);
                }
                count = count + 1;
            }

            while(radius <= searchRadiusInChunks){
                // 1. Step in the next step of the spiral
                //      - spiral approximated by doing each side separately
                //      - if a side's unchanging index is outside the spiral, it doesn't need to be checked at all
                // 2. If the current position is in the matrix, check the biome and increment the count
                //      - If the biome matches the target, create a location and return it
                
                // Iterate through each side simultaneously
                // Return location if biome is found
                for(int i = 0; i < radius*2; i++){
                    //Up Side (defined as -X direction from start position)
                    // -X -Z for start direction, +Z for progression
                    testX = chunkX - radius;
                    testZ = chunkZ - radius + i;
                    //Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                        count = count + 1;
                    }

                    //Right Side (defined as +Z direction from start position)
                    // -X +Z for start direction, +X for progression
                    testX = chunkX - radius + i;
                    testZ = chunkZ + radius;
                    //Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                        count = count + 1;
                    }

                    //Down Side (defined as +X direction from start position)
                    // +X +Z for start direction, -Z for progression
                    testX = chunkX + radius;
                    testZ = chunkZ + radius - i;
                    //Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                        count = count + 1;
                    }


                    //Left Side (defined as -Z direction from start position)
                    // +X -Z for start direction, -X for progression
                    testX = chunkX + radius - i;
                    testZ = chunkZ - radius;
                    //Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                        count = count + 1;
                    }
                }

                long endTime = System.currentTimeMillis();
                //Bukkit.getLogger().info("Searched " + count + " total chunks (radius " + radius + ") for " + biome.toString() + ". It took " + (endTime - startTime) + " milliseconds.");

                radius = radius + 1;
            }

            //If it doesn't return in the loop, no biome was found, so return null
            Bukkit.getLogger().info("Biome " + biome.toString() + " was not found.");
        }
        return null; //Starting location was outside world border, or no biome was found
    }

    // Checks if a given X,Z position is within a radius of the origin
    private static boolean checkCoordinatesWithinBorder(int X, int Z, double worldDiameter){
        return ((X >= (-worldDiameter/2) && X < (worldDiameter/2)) && (Z >= (-worldDiameter/2) && Z < (worldDiameter/2)));
    }
    
    // Checks if the biome at a given X,Z position matches the target biome
    private static boolean biomeMatchesTarget(World world, int X, int Z, Biome targetBiome){
        return ((RegionAccessor) world).getBiome(X*16, 255, Z*16) == targetBiome;
    }
}

