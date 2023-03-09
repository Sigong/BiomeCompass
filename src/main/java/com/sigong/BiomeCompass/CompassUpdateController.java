package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;

public class CompassUpdateController implements Listener {
    public CompassUpdateController(BiomeCompass instance, BiomeSearchManager searcher, NamespacedKeyHolder keyHolder) {
        this.instance = instance;
        this.searcher = searcher;
        this.keyHolder = keyHolder;
    }

    private final BiomeCompass instance;
    private final BiomeSearchManager searcher;
    private final NamespacedKeyHolder keyHolder;

    //TODO: A queue for biome searches (not necessarily in this class) might help limit the amount of lag caused by searching (have a delay between each search)

    //TODO: Eventhandler for when the world is reset (might not be necessary without a BiomeMap)

    //TODO: EventHandler for when a player moves between worlds (compass should be updated)
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event){
        event.getPlayer().sendMessage("You are currently in " + event.getPlayer().getWorld().getName());
        event.getPlayer().sendMessage("You were previously in " + event.getFrom().getName());

        //Check if player has a compass in the hotbar or offhand
        Player player = event.getPlayer();
        ItemStack item;

        //Check each item in hotbar
        for(int i = 0; i < 9; i++){
            item = player.getInventory().getItem(i);
            Bukkit.getLogger().info("testing item "+ i);
            if(isBiomeCompass(item)){
                CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                //Location currentTarget = compassMeta.getLodestone(); //Todo: re-add this after you figure out how to use it
                int ordinal = compassMeta.getPersistentDataContainer().get(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER);
                Biome targetBiome = Biome.values()[ordinal];

                CompassMeta newMeta = searcher.updateCompassMeta(compassMeta, event.getPlayer().getLocation());

                player.getInventory().getItem(i).setItemMeta(newMeta);

                player.sendMessage("UPDATED HOTBAR COMPASS META ON WORLD CHANGE EVENT");
            }
        }

        //Check offhand
        if(isBiomeCompass(player.getInventory().getItemInOffHand())){
            CompassMeta compassMeta = (CompassMeta) player.getInventory().getItemInOffHand().getItemMeta();

            int ordinal = player.getInventory().getItemInOffHand().getItemMeta().getPersistentDataContainer().get(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER);
            Biome targetBiome = Biome.values()[ordinal];

            CompassMeta newMeta = searcher.updateCompassMeta(compassMeta, event.getPlayer().getLocation());

            player.getInventory().getItemInOffHand().setItemMeta(newMeta);

            player.sendMessage("UPDATED OFFHAND COMPASS META ON WORLD CHANGE EVENT");
        }

        //update the compass
    }

    // Returns true if a given item is a BiomeCompass
    private boolean isBiomeCompass(ItemStack item){
        if(item == null){
            Bukkit.getLogger().info("Item is null");
            return false;
        }

        if(item.getType() != Material.COMPASS){
            Bukkit.getLogger().info("Item is not compass");
            return false;
        }

        if(!item.hasItemMeta()){
            Bukkit.getLogger().info("Compass has no meta");
            return false;
        }

        CompassMeta meta = (CompassMeta) item.getItemMeta();

        if(!meta.getPersistentDataContainer().has(keyHolder.targetBiomeKey(), PersistentDataType.INTEGER)){
            Bukkit.getLogger().info("CompassMeta does not have the right key");
            return false;
        }

        //TODO: add checks for other keys if necessary

        return true;
    }

    //TODO: EventHandler for when a player updates the compass (right click, currently)

    //TODO: Repeating task that checks each player and updates any hotbar of offhand compasses periodically

}
