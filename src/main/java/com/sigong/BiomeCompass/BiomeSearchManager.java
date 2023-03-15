package com.sigong.BiomeCompass;

import net.minecraft.world.level.block.entity.TileEntityStructure;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.RegionAccessor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

public class BiomeSearchManager {
    public BiomeSearchManager(BiomeCompass instance, ConfigValues configValues){
        this.instance = instance;
        this.configValues = configValues;
        this.lastManualUpdate = new HashMap<UUID, Long>();
    }

    // Plugin main class instance
    private final BiomeCompass instance;

    // Holder for values retrieved from the plugin's config file
    private final ConfigValues configValues;

    // Hashmap tracking how long since each player manually updated a BiomeCompass
    private final HashMap<UUID, Long> lastManualUpdate;

    // Creates a CompassMeta for a biome compass that points to the nearest biome of the desired type
    public CompassMeta createCompassMeta(Location startLocation, Biome targetBiome){
        CompassMeta compassMeta = (CompassMeta) new ItemStack(Material.COMPASS).getItemMeta();

        //The location of the nearest biome of the desired type
        Location biomeLocation = null;
        if(compassEnabledForWorld(startLocation.getWorld())) {
            biomeLocation = searchWorldForBiome(startLocation, targetBiome, configValues.getChunkSearchRadius());
        }

        //If no biome was found (or search timed out), point to a location that will make the compass spin
        //TODO: figure out how to point to nonexistent world instead of the end
        if(biomeLocation == null) {
            biomeLocation = new Location(Bukkit.getWorld("world_the_end"), 0, 0, 0);
        }

        compassMeta.setLodestoneTracked(false);

        compassMeta.setLodestone(biomeLocation);

        // Store ordinal of target biome in PDC
        compassMeta.getPersistentDataContainer().set(BiomeCompass.TARGET_BIOME_KEY, PersistentDataType.INTEGER, targetBiome.ordinal());

        // Store distance to target biome in PDC (or 0.0 if biome not found in this world)
        double distance = 0.0;
        if(startLocation.getWorld().equals(biomeLocation.getWorld())){
            distance = startLocation.distance(biomeLocation);
        }
        compassMeta.getPersistentDataContainer().set(BiomeCompass.LAST_SEARCH_LOCATION_CHUNK_X_KEY, PersistentDataType.INTEGER, startLocation.getChunk().getX());
        compassMeta.getPersistentDataContainer().set(BiomeCompass.LAST_SEARCH_LOCATION_CHUNK_Z_KEY, PersistentDataType.INTEGER, startLocation.getChunk().getZ());

        compassMeta.setDisplayName(ChatColor.RESET + "Biome Compass: " + WordUtils.capitalize(targetBiome.toString().toLowerCase(Locale.ROOT).replace('_', ' ')));

        //TODO: set compass lore to keep track of what world it is for, or what worlds it works in, or something else (if biome was found?)

        return compassMeta;
    }

    // Updates the target location of a biomecompass only if it needs to be updated
    // The compass needs to be updated only if there is a high enough number of unchecked chunks (as defined in config)
    // Unchecked chunks are chunks that haven't been searched, but are closer to the player than the compass target
    public CompassMeta updateCompassMeta(CompassMeta currentMeta, Player player, CompassUpdateReason reason) {
        //Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "UPDATING COMPASS: " + ChatColor.RESET + reason.toString());

        // If the manual is disabled and this is a manual update, or automatic disabled an automatic update
        if((reason == CompassUpdateReason.MANUAL && !configValues.isManualUpdateEnabled()) || (reason == CompassUpdateReason.AUTOMATIC && !configValues.isAutomaticUpdateEnabled())){
            return currentMeta;
        }

        // Compass doesn't update in a world it isn't enabled for.
        if(!compassEnabledForWorld(player.getWorld())){
            if(reason == CompassUpdateReason.MANUAL){
                player.sendMessage(ChatColor.RED + "BiomeCompass is not enabled in this world.");
            }
            return currentMeta;
        }

        // Compass doesn't update if Manually updated too soon, or if player doesn't have enough fuel to manually update
        if(reason == CompassUpdateReason.MANUAL){
            // Manually updated too soon (map filled in if player has never updated before)
            lastManualUpdate.computeIfAbsent(player.getUniqueId(), k -> {return 0L;}); //TODO: I don't understand this lambda notation
            int secondsSinceLastUpdate = Math.toIntExact((System.currentTimeMillis() - lastManualUpdate.get(player.getUniqueId()))/1000L);
            if(secondsSinceLastUpdate < configValues.getManualUpdateInterval()){
                player.sendMessage(ChatColor.RED + "Error: Can't manually update the BiomeCompass so soon after the last update. You must wait " + (configValues.getManualUpdateInterval() - secondsSinceLastUpdate) + " more seconds.");
                return currentMeta;
            }

            // Manual update consumes it, but player couldn't pay the fuel cost
            if(configValues.getManualUpdateConsumesItem() && !playerPaidFuelCost(player, configValues.getManualUpdateFuel(), configValues.getManualUpdateFuelAmount())){
                player.sendMessage(ChatColor.RED + "You don't have enough " + configValues.getManualUpdateFuel().toString() + " to manually update the compass. You need " + configValues.getManualUpdateFuelAmount() + ".");
                return currentMeta;
            }
        }

        // Player doesn't have enough fuel to automatically update the compass
        if(reason == CompassUpdateReason.AUTOMATIC && configValues.getAutomaticUpdateConsumesItem() && !playerPaidFuelCost(player, configValues.getAutomaticUpdateFuel(), configValues.getAutomaticUpdateFuelAmount())){
            player.sendMessage(ChatColor.RED + "You don't have enough " + configValues.getManualUpdateFuel().toString() + " to automatically update the compass. You need " + configValues.getManualUpdateFuelAmount() + ".");
            return currentMeta;
        }

        // There aren't enough unchecked chunks to merit a compass update
        int uncheckedChunks = calculateUncheckedChunks(currentMeta, player.getLocation());

        //Bukkit.getConsoleSender().sendMessage(ChatColor.LIGHT_PURPLE + "Unchecked Chunks: " + uncheckedChunks);

        if(uncheckedChunks < configValues.getUncheckedChunkLimit()){
            //Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "UNCHECKED CHUNKS WERE NOT HIGH ENOUGH");
            return currentMeta;
        }

        // Update the compass by performing a biome search, update lastManualUpdate if this update was manual
        //Bukkit.getConsoleSender().sendMessage(ChatColor.GREEN + "UNCHECKED CHUNKS WERE HIGH ENOUGH");

        if(reason == CompassUpdateReason.MANUAL){lastManualUpdate.put(player.getUniqueId(), System.currentTimeMillis());}

        Biome biome = Biome.values()[currentMeta.getPersistentDataContainer().get(BiomeCompass.TARGET_BIOME_KEY, PersistentDataType.INTEGER)];
        return createCompassMeta(player.getLocation(), biome);
    }

    // Returns true if the player had enough fuel, and the fuel was removed from their inventory
    public boolean playerPaidFuelCost(Player player, Material fuel, int fuelAmount){
        if(player.getInventory().contains(fuel, fuelAmount)){
            int removalsLeft = fuelAmount; //how many items still need to be consumed
            // Remove items until enough have been removed
            for(ItemStack item : player.getInventory().getContents()){
                if(removalsLeft == 0) break;
                if(item != null && item.getType() == fuel){
                    if(item.getAmount() <= removalsLeft){
                        removalsLeft = removalsLeft - item.getAmount();
                        item.setAmount(0);
                    }else{ //amount must be greater
                        item.setAmount(item.getAmount() - removalsLeft);
                        removalsLeft = 0;
                    }
                }
            }
            return true;
        }else{
            return false;
        }
    }

    public int calculateUncheckedChunks(CompassMeta compassMeta, Location playerLocation){
        // Biome Location
        int xb = compassMeta.getLodestone().getChunk().getX();
        int zb = compassMeta.getLodestone().getChunk().getZ();

        // Player Location as of last search
        int x1= compassMeta.getPersistentDataContainer().get(BiomeCompass.LAST_SEARCH_LOCATION_CHUNK_X_KEY, PersistentDataType.INTEGER);
        int z1 = compassMeta.getPersistentDataContainer().get(BiomeCompass.LAST_SEARCH_LOCATION_CHUNK_Z_KEY, PersistentDataType.INTEGER);

        // Current Player Location
        int x2 = playerLocation.getChunk().getX();
        int z2 = playerLocation.getChunk().getZ();

        // Radius of the square that was searched during the last search
        int r1 = Math.max(Math.abs(xb - x1), Math.abs(zb - z1));

        // Radius of the square that will be searched during the current search
        int r2 = Math.max(Math.abs(xb - x2), Math.abs(zb - z2));

        // https://www.desmos.com/calculator/xerdv0vl0k
        int overlapLengthX = (Math.min(x1 + r1, x2 + r2) - Math.max(x1 - r1, x2 - r2));
        int overlapLengthZ = (Math.min(z1 + r1, z2 + r2) - Math.max(z1 - r1, z2 - r2));
        //int overlapArea = 4*r2*r2 - overlapLengthX * overlapLengthZ;

        int result = (2*r2 + 1)*(2*r2 + 1) - (overlapLengthX + 1)*(overlapLengthZ + 1);

        //Bukkit.getConsoleSender().sendMessage(xb + ", " + zb + ", " + x1 + ", " + z1 + ", " + x2 + ", " + z2 + ", " + r1 + ", " + r2 + ", " + result);

        return result;
    }

    // Returns true if the compass is enabled in the given world
    private boolean compassEnabledForWorld(World world){
        return configValues.getEnabledWorldNames().contains(world.getName());
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
            //If test coordinates are in range and the biome matches
            if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                count = count + 1;
                if(biomeMatchesTarget(world, testX, testZ, biome)) {
                    long endTime = System.currentTimeMillis();
                    //Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                    return new Location(world, testX * 16, 255, testZ * 16);
                }
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
                    ////Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        count = count + 1;
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            //Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                    }

                    //Right Side (defined as +Z direction from start position)
                    // -X +Z for start direction, +X for progression
                    testX = chunkX - radius + i;
                    testZ = chunkZ + radius;
                    ////Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        count = count + 1;
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            //Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                    }

                    //Down Side (defined as +X direction from start position)
                    // +X +Z for start direction, -Z for progression
                    testX = chunkX + radius;
                    testZ = chunkZ + radius - i;
                    ////Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        count = count + 1;
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            //Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                    }


                    //Left Side (defined as -Z direction from start position)
                    // +X -Z for start direction, -X for progression
                    testX = chunkX + radius - i;
                    testZ = chunkZ - radius;
                    ////Bukkit.getLogger().info("Tested Chunk (" + testX + ", " + testZ + ")");
                    //If test coordinates are in range and the biome matches
                    if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                        count = count + 1;
                        if(biomeMatchesTarget(world, testX, testZ, biome)) {
                            long endTime = System.currentTimeMillis();
                            //Bukkit.getLogger().info("Located " + biome.toString() + " after searching " + count + " chunks. It took " + (endTime - startTime) + " milliseconds.");
                            return new Location(world, testX * 16, 255, testZ * 16);
                        }
                    }
                }

                long endTime = System.currentTimeMillis();
                ////Bukkit.getLogger().info("Searched " + count + " total chunks (radius " + radius + ") for " + biome.toString() + ". It took " + (endTime - startTime) + " milliseconds.");

                radius = radius + 1;
            }

            //If it doesn't return in the loop, no biome was found, so return null
            long endTime = System.currentTimeMillis();
            //Bukkit.getLogger().info("Biome " + biome.toString() + " was not found within " + searchRadiusInChunks + " chunks. (searched " + count + " chunks in " + (endTime - startTime) + " milliseconds)");
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

