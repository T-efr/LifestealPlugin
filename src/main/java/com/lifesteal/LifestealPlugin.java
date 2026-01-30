package com.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.NamespacedKey;

import java.util.Arrays;

public class LifestealPlugin extends JavaPlugin implements Listener {

    private static final int MIN_HEARTS = 3;
    private static final int DEFAULT_MAX_HEARTS = 20;
    private static final int DRAGON_EGG_MAX_HEARTS = 25;
    private static final int CRAFTED_HEART_MAX = 10;

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerHeartRecipe();
        getLogger().info("Lifesteal Plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Lifesteal Plugin has been disabled!");
    }

    private void registerHeartRecipe() {
        ItemStack heart = createHeartItem();
        NamespacedKey key = new NamespacedKey(this, "lifesteal_heart");
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
            meta.setLore(Arrays.asList("§7Right-click to gain a heart!", "§7Maximum: 10 hearts from crafting"));
            meta.setCustomModelData(1); // Optional: for custom textures
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.getAttribute(Attribute.GENERIC_MAX_HEALTH) == null) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) {
            return; // No killer or suicide
        }

        double victimMaxHealth = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double killerMaxHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        // Check if victim is at minimum hearts
        if (victimMaxHealth <= MIN_HEARTS * 2) {
            victim.sendMessage("§cYou are at minimum hearts and cannot lose more!");
            killer.sendMessage("§e" + victim.getName() + " §7is at minimum hearts. No heart gained.");
            return;
        }

        // Remove heart from victim
        victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(victimMaxHealth - 2.0);
        victim.sendMessage("§c-1 ❤ §7You lost a heart! Current max: §c" + (victimMaxHealth - 2.0) / 2 + " hearts");

        // Add heart to killer (respecting max)
        int killerMaxHearts = getMaxHearts(killer);
        if (killerMaxHealth < killerMaxHearts * 2) {
            killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(killerMaxHealth + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, killerMaxHealth + 2.0));
            killer.sendMessage("§a+1 ❤ §7You gained a heart! Current max: §a" + (killerMaxHealth + 2.0) / 2 + " hearts");
        } else {
            killer.sendMessage("§eYou are at maximum hearts!");
        }
    }

    @EventHandler
    public void onCraftHeart(CraftItemEvent event) {
        if (event.getRecipe().getResult().isSimilar(createHeartItem())) {
            Player player = (Player) event.getWhoClicked();
            
            // Check if player is already at crafted heart limit (10 hearts = 20 health)
            double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            
            if (currentMaxHealth >= CRAFTED_HEART_MAX * 2) {
                player.sendMessage("§cYou cannot craft hearts beyond 10 hearts! Kill players to gain more.");
                event.setCancelled(true);
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("§cOnly players can use this command!");
                return true;
            }

            Player player = (Player) sender;

            if (args.length != 1) {
                player.sendMessage("§cUsage: /withdraw <amount>");
                return true;
            }

            int amount;
            try {
                amount = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cPlease enter a valid number!");
                return true;
            }

            if (amount <= 0) {
                player.sendMessage("§cAmount must be positive!");
                return true;
            }

            double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            double newMaxHealth = currentMaxHealth - (amount * 2.0);

            if (newMaxHealth < MIN_HEARTS * 2) {
                player.sendMessage("§cYou cannot withdraw that many hearts! You would go below " + MIN_HEARTS + " hearts.");
                return true;
            }

            // Withdraw hearts
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(newMaxHealth);
            if (player.getHealth() > newMaxHealth) {
                player.setHealth(newMaxHealth);
            }

            // Give heart items
            ItemStack hearts = createHeartItem();
            hearts.setAmount(amount);
            player.getInventory().addItem(hearts);

            player.sendMessage("§aWithdrew §c" + amount + " ❤ §ahearts! Current max: §c" + newMaxHealth / 2 + " hearts");
            return true;
        }
        return false;
    }

    private int getMaxHearts(Player player) {
        // Check if player has dragon egg in inventory
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            return DRAGON_EGG_MAX_HEARTS;
        }
        return DEFAULT_MAX_HEARTS;
    }
}
