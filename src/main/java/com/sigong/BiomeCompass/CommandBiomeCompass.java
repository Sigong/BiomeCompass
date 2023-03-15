package com.sigong.BiomeCompass;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;

public class CommandBiomeCompass implements CommandExecutor {
    public CommandBiomeCompass(BiomeCompass instance, ConfigValues configValues) {
        this.instance = instance;
        this.configValues = configValues;

        instance.getCommand("biomecompass").setTabCompleter(new BiomeCompassTabCompleter());
    }

    private final BiomeCompass instance;
    private final ConfigValues configValues;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if(args.length == 1 && args[0].equalsIgnoreCase("reload")){
            configValues.loadValuesFromConfig();
            sender.sendMessage(ChatColor.GREEN + "BiomeCompass config has been reloaded.");
        }else{
            sender.sendMessage(ChatColor.RED + "Proper usage is " + instance.getCommand("biomecompass").getUsage());
        }
        return true;
    }

    private class BiomeCompassTabCompleter implements TabCompleter {
        @Override
        public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] args) {
            if(args.length == 1) {
                ArrayList<String> list = new ArrayList<>();
                list.add("reload");
                return list;
            }
            return null;
        }
    }
}
