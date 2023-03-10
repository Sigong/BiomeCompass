package com.sigong.BiomeCompass;

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
        if(sender instanceof Player){
            Player player = (Player) sender;

            if(args.length == 1){
                try {
                    ItemStack compass = new ItemStack(Material.COMPASS);

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
