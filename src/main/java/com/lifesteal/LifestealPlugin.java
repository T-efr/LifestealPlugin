package com.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class LifestealPlugin extends JavaPlugin implements Listener {

    private static final int MIN_HEARTS = 3;
    private static final int DEFAULT_MAX_HEARTS = 20;
    private static final int DRAGON_EGG_MAX_HEARTS = 25;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerHeartRecipe();
        getLogger().info("Lifesteal Plugin Enabled!");
    }

    private void registerHeartRecipe() {
        ItemStack heart = createHeartItem();
        NamespacedKey key = new NamespacedKey(this, "lifesteal_heart");
        // Remove old recipe to avoid errors on reload
        Bukkit.removeRecipe(key);
        ShapedRecipe recipe = new ShapedRecipe(key, heart);
        recipe.shape("ONO", "GSG", "ONO");
        recipe.setIngredient('O', Material.OMINOUS_TRIAL_KEY);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('G', Material.GOLD_BLOCK);
        recipe.setIngredient('S', Material.NETHER_STAR);
        Bukkit.addRecipe(recipe);
    }

    private ItemStack createHeartItem() {
        ItemStack heart = new ItemStack(Material.RED_DYE);
        ItemMeta meta = heart.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c❤ Heart");
            meta.setLore(Arrays.asList("§7Right-click to gain a heart!"));
            heart.setItemMeta(meta);
        }
        return heart;
    }

    // --- DRAGON EGG LOGIC ---
    
    private void updatePlayerMaxHealth(Player player) {
        AttributeInstance maxHealthAttr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealthAttr == null) return;

        double currentMax = maxHealthAttr.getBaseValue();
        int allowedMaxHearts = getMaxHeartsAllowed(player);
        double allowedMaxHP = allowedMaxHearts * 2.0;

        // If player dropped the egg and is now OVER their allowed max
        if (currentMax > allowedMaxHP) {
            maxHealthAttr.setBaseValue(allowedMaxHP);
            if (player.getHealth() > allowedMaxHP) {
                player.setHealth(allowedMaxHP);
            }
            player.sendMessage("§cYou lost your extra hearts because the Dragon Egg is gone!");
        }
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) { updatePlayerMaxHealth(e.getPlayer()); }
    @EventHandler public void onInvClose(InventoryCloseEvent e) { updatePlayerMaxHealth((Player) e.getPlayer()); }

    // --- DEATH & STEALING ---

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        AttributeInstance victimAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance killerAttr = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (victimAttr == null || killerAttr == null) return;

        if (victimAttr.getBaseValue() <= MIN_HEARTS * 2) {
            killer.sendMessage("§e" + victim.getName() + " is at minimum hearts. No heart gained.");
            return;
        }

        victimAttr.setBaseValue(victimAttr.getBaseValue() - 2.0);
        
        int killerCap = getMaxHeartsAllowed(killer);
        if (killerAttr.getBaseValue() < killerCap * 2) {
            killerAttr.setBaseValue(killerAttr.getBaseValue() + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, killerAttr.getBaseValue()));
            killer.sendMessage("§a+1 ❤ §7Heart stolen!");
        }
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta()) return;
        if (!item.getItemMeta().getDisplayName().equals("§c❤ Heart")) return;

        Player player = event.getPlayer();
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        if (attr.getBaseValue() < getMaxHeartsAllowed(player) * 2) {
            attr.setBaseValue(attr.getBaseValue() + 2.0);
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a+1 ❤ §7Heart consumed!");
        } else {
            player.sendMessage("§cYou are at your maximum hearts!");
        }
        event.setCancelled(true);
    }

    private int getMaxHeartsAllowed(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG) ? DRAGON_EGG_MAX_HEARTS : DEFAULT_MAX_HEARTS;
    }
}
