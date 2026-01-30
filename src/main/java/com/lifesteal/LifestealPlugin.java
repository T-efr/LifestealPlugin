package com.lifesteal;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class LifestealPlugin extends JavaPlugin implements Listener {

    private static final int MIN_HEARTS = 3; 
    private static final int DEFAULT_HEARTS = 10; // 20 HP (Standard Minecraft max)
    private static final int HARD_CAP_HEARTS = 20; // 40 HP (Max possible via hearts/kills)
    private static final int DRAGON_EGG_CAP_HEARTS = 25; // 50 HP

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerHeartRecipe();
        getLogger().info("Lifesteal Plugin enabled!");
    }

    private void registerHeartRecipe() {
        ItemStack heart = createHeartItem();
        NamespacedKey key = new NamespacedKey(this, "lifesteal_heart");
        ShapedRecipe recipe = new ShapedRecipe(key, heart);

        recipe.shape("ONO", "GSG", "ONO");
        // Ensure your server is 1.21+ for OMINOUS_TRIAL_KEY
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
            // Updated lore to reflect the mechanic
            meta.setLore(Arrays.asList(
                "§7Right-click to gain a heart!", 
                "§7Crafting Limit: You can only craft this",
                "§7if you are below 10 hearts."
            ));
            heart.setItemMeta(meta);
        }
        return heart;
    } // FIXED: Added missing closing brace

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        // Basic checks
        if (item == null || item.getType() != Material.RED_DYE) return;
        if (!item.hasItemMeta()) return;
        if (!item.getItemMeta().hasDisplayName()) return;
        if (!item.getItemMeta().getDisplayName().equals("§c❤ Heart")) return;

        double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        
        // DETERMINE MAX CAP:
        // If they have dragon egg, cap is higher. Otherwise, standard Hard Cap.
        double maxLimit = getMaxHearts(player) * 2;

        if (currentMaxHealth >= maxLimit) {
            player.sendMessage("§cYou have reached the maximum heart limit for your tier!");
            return;
        }

        // Apply the heart
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentMaxHealth + 2.0);
        player.setHealth(Math.min(player.getHealth() + 2.0, currentMaxHealth + 2.0));

        item.setAmount(item.getAmount() - 1);
        player.sendMessage("§a+1 ❤ Heart consumed!");
    }

    @EventHandler
    public void onCraftHeart(CraftItemEvent event) {
        if (!event.getRecipe().getResult().isSimilar(createHeartItem())) return;

        Player player = (Player) event.getWhoClicked();
        double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        // LOGIC FIX:
        // We block crafting ONLY if the player is at (or above) the default 10 hearts (20 HP).
        // This forces them to use PVP or Withdrawals to get "extra" hearts.
        if (currentMaxHealth >= DEFAULT_HEARTS * 2) {
            player.sendMessage("§cYou cannot craft hearts when you are at full health!");
            player.sendMessage("§7(Crafting is only for recovering lost hearts)");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        // Self-kill or Environment checks
        if (killer == null || killer.equals(victim)) return;

        double victimMax = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double killerMax = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        // Victim loses heart (down to min)
        if (victimMax > MIN_HEARTS * 2) {
            victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(victimMax - 2.0);
        }

        // Killer gains heart (up to max cap)
        if (killerMax < getMaxHearts(killer) * 2) {
            killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(killerMax + 2.0);
            killer.sendMessage("§aYou stole a heart from " + victim.getName() + "!");
        }
    }

    private int getMaxHearts(Player player) {
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            return DRAGON_EGG_CAP_HEARTS;
        }
        return HARD_CAP_HEARTS;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }
}
