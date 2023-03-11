package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        Player player;

        // Usage: /createcompass <biome>
        if(args.length == 1){
            if(sender instanceof Player){
                player = (Player) sender;
            }else{
                sender.sendMessage(ChatColor.RED + "Error: This command cannot be sent from the console.");
                return true;
            }

        // Usage: /createcompass <biome> <player>
        }else if(args.length == 2){
            if(Bukkit.getOnlinePlayers().contains(Bukkit.getPlayer(args[1]))){
                player = Bukkit.getPlayer(args[1]);
            }else{
                sender.sendMessage(ChatColor.RED + "Error: That player is not online.");
                return true;
            }
        }else{
            sender.sendMessage(ChatColor.RED + "Error: Wrong number of arguments. Command usage is /createcompass <biome> OR /createcompass <biome> <player>");
            return true;
        }

        // Attempt to create a compass pointing to the biome named in args[0]
        try {
            ItemStack compass = new ItemStack(Material.COMPASS);

            CompassMeta compassMeta = searcher.createCompassMeta(player.getLocation(), Biome.valueOf(args[0]));

            compass.setItemMeta(compassMeta);

            // Add the compass to the player's inventory, or drop it at their position if it can't be added to their inventory.
            if(!player.getInventory().addItem(compass).isEmpty()){
                player.getWorld().dropItem(player.getLocation(), compass);
            }
        }catch(IllegalArgumentException e){
            sender.sendMessage(ChatColor.RED + "Error: Invalid Biome");
            return true;
        }

        return true;
    }

    // TabCompleter for the CreateCompass Command
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
            }else if(args.length == 2){
                ArrayList<String> playerNames = new ArrayList<>();
                for(Player player : Bukkit.getOnlinePlayers()){
                    playerNames.add(player.getName());
                }

                List<String> completions = new ArrayList<>();
                StringUtil.copyPartialMatches(args[1], playerNames, completions);
                return completions;
            }
            return Collections.EMPTY_LIST;
        }
    }
}
