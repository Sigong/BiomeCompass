package com.sigong.BiomeCompass;

import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class CommandCreateCompass implements CommandExecutor {

    public CommandCreateCompass(BiomeCompass instance, BiomeSearchManager searcher, NamespacedKeyHolder keyHolder){
        this.instance = instance;
        instance.getCommand("createcompass").setTabCompleter(new CreateCompassTabCompleter());

        this.searcher = searcher;

        this.keyHolder = keyHolder;
    }

    private final BiomeCompass instance;
    private final BiomeSearchManager searcher;
    private final NamespacedKeyHolder keyHolder;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if(sender instanceof Player){
            Player player = (Player) sender;

            if(args.length == 1){
                try {
                    ItemStack compass = new ItemStack(Material.COMPASS);
                    /*CompassMeta compassMeta = (CompassMeta) compass.getItemMeta();

                    //The location of the nearest biome of the desired type
                    Location loc = searcher.searchWorldForBiome(player.getLocation(), Biome.valueOf(args[0]), null);

                    //If no biome was found (or search timed out), point to a location that will make the compass spin
                    //TODO: figure out how to point to nonexistent world instead of the end
                    if(loc == null) {
                        player.sendMessage(ChatColor.RED + "[BiomeCompass] Requested biome was not found within the search range. Compass will still detect a biome that is in range.");
                        loc = new Location(Bukkit.getWorld("world_the_end"), 0, 0, 0);
                    }else{
                        player.sendMessage(ChatColor.GREEN + "[BiomeCompass] Biome was located; Follow the compass!");
                        player.sendMessage(loc.toString()); //TODO: remove
                    }

                    compassMeta.setLodestoneTracked(false);

                    compassMeta.setLodestone(loc);

                    compassMeta.getPersistentDataContainer().set(new NamespacedKey(instance,"target-biome"), PersistentDataType.INTEGER, Biome.valueOf(args[0]).ordinal());

                    compassMeta.setDisplayName(ChatColor.RESET + "Biome Compass: " + WordUtils.capitalize(Biome.valueOf(args[0]).toString().toLowerCase(Locale.ROOT).replace('_', ' ')));

                    //todo: set compass lore to keep track of what world it is for

                    compass.setItemMeta(compassMeta);

                     */

                    CompassMeta compassMeta = searcher.createCompassMeta(player.getLocation(), Biome.valueOf(args[0]));

                    compass.setItemMeta(compassMeta);

                    player.getInventory().setItemInMainHand(compass); //TODO: change to additem
                }catch(IllegalArgumentException e){
                    sender.sendMessage(ChatColor.RED + "Error: Invalid Biome");
                }
            }else{
                //improper number of arguments
            }

        }else{
            sender.sendMessage(ChatColor.RED + "This command cannot be sent from the console.");
        }

        return true;
    }

    private class CreateCompassTabCompleter implements TabCompleter {

        public CreateCompassTabCompleter(){
            biomeNames = new ArrayList<String>(Biome.values().length);
            for(Biome biome : Biome.values()){
                biomeNames.add(biome.toString());
            }
        }

        private final ArrayList<String> biomeNames;
        //private final ArrayList<String> worldNames;

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[0], biomeNames, completions);
                return completions;
            }
            //TODO: add a second block if I decide to add world option to command
            return Collections.EMPTY_LIST;
        }
    }
}
