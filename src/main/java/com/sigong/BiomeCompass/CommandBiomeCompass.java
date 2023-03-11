package com.sigong.BiomeCompass;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class CommandBiomeCompass implements CommandExecutor {
    public CommandBiomeCompass(BiomeCompass biomeCompass, ConfigValues configValues) {
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //TODO: reload configvalues from config (might need to switch from record to custom class)
        return true;
    }
}
