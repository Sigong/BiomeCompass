package com.sigong.BiomeCompass;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CompassMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class CompassUpdateController implements Listener {
    public CompassUpdateController(BiomeCompass instance, BiomeSearchManager searcher, ConfigValues configValues) {
        this.instance = instance;
        this.searcher = searcher;
        this.configValues = configValues;
    }

    private final BiomeCompass instance;
    private final BiomeSearchManager searcher;
    private final ConfigValues configValues;

    //TODO: When the plugin is reloaded, players lose their repeating tasks
    //TODO: Existing tasks also won't have their interval updated when the config values are reloaded
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event){
        (new CompassUpdateTask(event.getPlayer())).runTaskTimer(instance, 0L, 20L * configValues.getAutomaticUpdateInterval());
    }

    // Updates any compasses in the hotbar and offhand when a player moves between worlds.
    @EventHandler
    public void onPlayerChangeWorld(PlayerChangedWorldEvent event){
        //Check if player has a compass in the hotbar or offhand
        updateHotbarOffhandCompasses(event.getPlayer(), CompassUpdateReason.AUTOMATIC);
    }

    // Returns true if a given item is a BiomeCompass
    private boolean isBiomeCompass(ItemStack item){
        if(item == null){
            //Bukkit.getLogger().info("Item is null");
            return false;
        }

        if(item.getType() != Material.COMPASS){
            //Bukkit.getLogger().info("Item is not compass");
            return false;
        }

        if(!item.hasItemMeta()){
            //Bukkit.getLogger().info("Compass has no meta");
            return false;
        }

        CompassMeta meta = (CompassMeta) item.getItemMeta();

        if(!meta.getPersistentDataContainer().has(BiomeCompass.TARGET_BIOME_KEY, PersistentDataType.INTEGER)){
            //Bukkit.getLogger().info("CompassMeta does not have the right key");
            return false;
        }

        //Add checks for other keys if necessary

        return true;
    }

    // If the player right clicks while holding a biome compass, update it.
    @EventHandler
    public void onCompassRightClick(PlayerInteractEvent event){
        if(event.getAction() == Action.RIGHT_CLICK_AIR || (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getHand() == EquipmentSlot.HAND )){
            Player player = event.getPlayer();
            ItemStack item = player.getInventory().getItemInMainHand();
            if(isBiomeCompass(item)){
                CompassMeta newMeta = searcher.updateCompassMeta((CompassMeta) item.getItemMeta(), player, CompassUpdateReason.MANUAL);
                item.setItemMeta(newMeta);
            }
        }
    }

    // Checks hotbar and offhand of a given for BiomeCompasses, and updates each
    public void updateHotbarOffhandCompasses(Player player, CompassUpdateReason reason){
        ItemStack item;

        //Check each item in hotbar
        for(int i = 0; i < 9; i++){
            item = player.getInventory().getItem(i);
            if(isBiomeCompass(item)){
                CompassMeta compassMeta = (CompassMeta) item.getItemMeta();
                CompassMeta newMeta = searcher.updateCompassMeta(compassMeta, player, reason);
                item.setItemMeta(newMeta);
            }
        }

        //Check offhand
        if(isBiomeCompass(player.getInventory().getItemInOffHand())){
            CompassMeta compassMeta = (CompassMeta) player.getInventory().getItemInOffHand().getItemMeta();
            CompassMeta newMeta = searcher.updateCompassMeta(compassMeta, player, reason);
            player.getInventory().getItemInOffHand().setItemMeta(newMeta);
        }
    }

    //Repeating task that checks each player and updates any hotbar of offhand compasses periodically
    private class CompassUpdateTask extends BukkitRunnable {
        public CompassUpdateTask(Player player){
            this.player = player;
        }

        private final Player player;

        @Override
        public void run() {
            if(player.isOnline()){
                // Update the player's compasses if they are online.
                updateHotbarOffhandCompasses(player, CompassUpdateReason.AUTOMATIC);
            }else{
                // Cancel the task if the player is offline.
                this.cancel();
            }
        }
    }

}
