package com.sigong.BiomeCompass;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.RegionAccessor;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nullable;
import java.util.Locale;

public class BiomeSearchManager {
    public BiomeSearchManager(BiomeCompass instance, NamespacedKeyHolder keyHolder){
        this.instance = instance;
        this.keyHolder = keyHolder;

        //Load information from ResourceWorld's config file
        Plugin resourceWorld = Bukkit.getServer().getPluginManager().getPlugin("ResourceWorld");
        FileConfiguration resourceWorldConfig = resourceWorld.getConfig();

        //resource_world
        resourceWorldEnabled = resourceWorldConfig.getBoolean("world.enabled");
        resourceWorldName = resourceWorldConfig.getString("world.world_name");
        resourceWorldSize = resourceWorldConfig.getInt("world.world_border.size");

        //resource_nether
        resourceNetherEnabled = resourceWorldConfig.getBoolean("nether.enabled");
        resourceNetherName = resourceWorldConfig.getString("nether.world_name");
        resourceNetherSize = resourceWorldConfig.getInt("nether.world_border.size");

        //resource_end
        resourceEndEnabled = resourceWorldConfig.getBoolean("end.enabled");
        resourceEndName = resourceWorldConfig.getString("end.world_name");
        resourceEndSize = resourceWorldConfig.getInt("end.world_border.size");
    }

    // Plugin main class instance
    private final BiomeCompass instance;

    // NamespacedKey Holder
    private final NamespacedKeyHolder keyHolder;

    // The maximum distance in chunks from the player that will be searched for the biome
    private final int CHUNK_SEARCH_RADIUS = 50;
    
    // Information from ResourceWorld config file
    private final boolean resourceWorldEnabled, resourceNetherEnabled, resourceEndEnabled;
    private final String resourceWorldName, resourceNetherName, resourceEndName;
    private final int resourceWorldSize, resourceNetherSize, resourceEndSize;

    // Creates a CompassMeta for a biome compass that points to the nearest biome of the desired type
    public CompassMeta createCompassMeta(Location startLocation, Biome targetBiome){
        CompassMeta compassMeta = (CompassMeta) new ItemStack(Material.COMPASS).getItemMeta();

        //The location of the nearest biome of the desired type
        Location biomeLocation = null;
        if(compassEnabledForWorld(startLocation.getWorld())) {
            //TODO: add a check involving the currentLocation and the targt location to see if a search is even needed (two checks, one for distance and one for angle?)
            biomeLocation = searchWorldForBiome(startLocation, targetBiome, CHUNK_SEARCH_RADIUS);
        }

        //If no biome was found (or search timed out), point to a location that will make the compass spin
        //TODO: figure out how to point to nonexistent world instead of the end
        //TODO: change compass name or lore based on whether or not a biome was found
        if(biomeLocation == null) {
            biomeLocation = new Location(Bukkit.getWorld("world_the_end"), 0, 0, 0);
        }

        compassMeta.setLodestoneTracked(false);

        compassMeta.setLodestone(biomeLocation);

        compassMeta.getPersistentDataContainer().set(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER, targetBiome.ordinal());

        compassMeta.setDisplayName(ChatColor.RESET + "Biome Compass: " + WordUtils.capitalize(targetBiome.toString().toLowerCase(Locale.ROOT).replace('_', ' ')));

        //TODO: set compass lore to keep track of what world it is for, or what worlds it works in, or something else

        return compassMeta;
    }

    // Updates the target location of a biomecompass only if it needs to be updated
    //TODO: figure out how to detect this, maybe compare previous and current angles between player and target
    //TODO: the distance to the target as of the last search could be stored in the CompassMeta, and definitely search if it is greater now (update the distance though)
    public CompassMeta updateCompassMeta(CompassMeta currentMeta, Location currentLocation) {
        Biome biome = Biome.values()[currentMeta.getPersistentDataContainer().get(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER)];

        //TODO: add a check involving the currentLocation and the targt location to see if a search is even needed (two checks, one for distance and one for angle?)
        if(true){
            return createCompassMeta(currentLocation, biome);
        }else{
            return currentMeta;
        }
    }

    // Returns true if the compass is enabled in the given world
    private boolean compassEnabledForWorld(World world){
        String worldName = world.getName();
        if((resourceWorldEnabled && worldName.equals(resourceWorldName)) || ((resourceNetherEnabled && worldName.equals(resourceNetherName))) || ((resourceEndEnabled && worldName.equals(resourceEndName)))) {
            return true;
        }
        return false;
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
            double maxCount = worldSizeInChunks*worldSizeInChunks;
            
            // The radius of chunks that are currently being searched
            int radius = 1;

            //Check the starting Chunk
            int testX = chunkX;
            int testZ = chunkZ;
            Bukkit.getLogger().info("Checking Starting Chunk (" + chunkX + ", " + chunkZ + ")");
            //If test coordinates are in range and the biome matches
            if(checkCoordinatesWithinBorder(testX, testZ, worldSizeInChunks)){
                if(biomeMatchesTarget(world, testX, testZ, biome)) {
                    return new Location(world, testX * 16, 255, testZ * 16);
                }
                count = count + 1;
            }

            //TIMING INFORMATION
            long startTime = System.currentTimeMillis();

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
                Bukkit.getLogger().info("Searched " + count + " total chunks (radius " + radius + ") for " + biome.toString() + ". It took " + (endTime - startTime) + " milliseconds.");

                radius = radius + 1;
            }

            //If it doesn't return in the loop, no biome was found, so return null
            Bukkit.getLogger().info("Biome " + biome.toString() + " was not found.");
            return null;
        }else{
            //Starting location was outside the map (this should never happen)
            return null;
        }
    }

    // Checks if a given X,Z position is within a radius of the origin
    private static boolean checkCoordinatesWithinBorder(int X, int Z, double worldDiameter){
        //is the X,Z coordinate inside of the world border
        boolean insideWorldBorder = ((X >= (-worldDiameter/2) && X < (worldDiameter/2)) && (Z >= (-worldDiameter/2) && Z < (worldDiameter/2)));

        return insideWorldBorder;
    }
    
    // Checks if the biome at a given X,Z position matches the target biome
    private static boolean biomeMatchesTarget(World world, int X, int Z, Biome targetBiome){
        return ((RegionAccessor) world).getBiome(X*16, 255, Z*16) == targetBiome;
    }
    
    @Nullable
    private static Location checkChunk(World world, int chunkX, int chunkZ){
        return null;
    }
}
