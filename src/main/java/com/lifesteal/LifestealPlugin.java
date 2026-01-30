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
    private static final int CRAFTED_HEART_MAX = 10; // This is actually 20HP

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerHeartRecipe();
        getLogger().info("Lifesteal Plugin has been enabled!");
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
            meta.setCustomModelData(1);
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        // Only set to 20 if they've never joined before, 
        // otherwise they lose their progress every login!
        if (!player.hasPlayedBefore()) {
            player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
        }
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        double victimMaxHealth = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double killerMaxHealth = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        if (victimMaxHealth > MIN_HEARTS * 2) {
            victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(victimMaxHealth - 2.0);
            victim.sendMessage("§c-1 ❤ §7You lost a heart!");
        }

        int killerMaxHeartsAllowed = getMaxHearts(killer);
        if (killerMaxHealth < killerMaxHeartsAllowed * 2) {
            killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(killerMaxHealth + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, killerMaxHealth + 2.0));
            killer.sendMessage("§a+1 ❤ §7You stole a heart!");
        }
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        if (event.getItem() == null || !event.getAction().name().contains("RIGHT_CLICK")) return;
        
        ItemStack item = event.getItem();
        if (item.getType() == Material.RED_DYE && item.hasItemMeta() && 
            item.getItemMeta().getDisplayName().equals("§c❤ Heart")) {
            
            Player player = event.getPlayer();
            double currentMax = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
            
            // Logic: Allow item use if they are below the Dragon Egg cap (25 hearts/50 HP)
            if (currentMax < getMaxHearts(player) * 2) {
                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentMax + 2.0);
                item.setAmount(item.getAmount() - 1);
                player.sendMessage("§a+1 ❤ §7Heart consumed!");
            } else {
                player.sendMessage("§cYou have reached your maximum heart capacity!");
            }
            event.setCancelled(true);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player)) return true;
            Player player = (Player) sender;

            if (args.length != 1) {
                player.sendMessage("§c/withdraw <amount>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[0]);
                double currentHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
                
                if (currentHealth - (amount * 2) < MIN_HEARTS * 2) {
                    player.sendMessage("§cToo many hearts to withdraw!");
                    return true;
                }

                player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentHealth - (amount * 2));
                ItemStack hearts = createHeartItem();
                hearts.setAmount(amount);
                player.getInventory().addItem(hearts);
                player.sendMessage("§aWithdrew " + amount + " hearts.");
                
            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number.");
            }
            return true;
        }
        return false;
    }

    private int getMaxHearts(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG) ? DRAGON_EGG_MAX_HEARTS : DEFAULT_MAX_HEARTS;
    }
}
