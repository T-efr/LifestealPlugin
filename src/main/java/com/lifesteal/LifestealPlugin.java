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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class LifestealPlugin extends JavaPlugin implements Listener {

    private static final int MIN_HEARTS = 3;
    private static final int DEFAULT_MAX_HEARTS = 20;
    private static final double EGG_BONUS_HP = 10.0; // 5 Hearts

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerHeartRecipe();
        getLogger().info("Lifesteal Plugin Enabled!");
    }

    private void registerHeartRecipe() {
        ItemStack heart = createHeartItem();
        NamespacedKey key = new NamespacedKey(this, "lifesteal_heart");
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

    private void handleEggBuff(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        boolean hasEgg = player.getInventory().contains(Material.DRAGON_EGG);
        double currentMax = attr.getBaseValue();

        // If they have egg and are at/below normal cap, add 5 hearts
        if (hasEgg && currentMax <= (DEFAULT_MAX_HEARTS * 2)) {
            attr.setBaseValue(currentMax + EGG_BONUS_HP);
            player.sendMessage("§5§lThe Dragon Egg grants you 5 extra hearts!");
        } 
        // If they don't have egg and are above normal cap, remove 5 hearts
        else if (!hasEgg && currentMax > (DEFAULT_MAX_HEARTS * 2)) {
            double newMax = Math.max(MIN_HEARTS * 2, currentMax - EGG_BONUS_HP);
            attr.setBaseValue(newMax);
            if (player.getHealth() > newMax) player.setHealth(newMax);
            player.sendMessage("§cThe Dragon Egg was lost. You lost 5 hearts!");
        }
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) { handleEggBuff(e.getPlayer()); }
    @EventHandler public void onInvClose(InventoryCloseEvent e) { handleEggBuff((Player) e.getPlayer()); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { handleEggBuff(e.getPlayer()); }

    // --- WITHDRAW COMMAND (With Egg Protection) ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player player)) return true;

            if (args.length != 1) {
                player.sendMessage("§cUsage: /withdraw <amount>");
                return true;
            }

            try {
                int amount = Integer.parseInt(args[0]);
                if (amount <= 0) return true;

                AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double currentMax = attr.getBaseValue();
                
                // --- THE PROTECTION LOGIC ---
                // If they have the egg, their "floor" is 3 hearts + 5 egg hearts = 8 hearts (16 HP)
                double effectiveMinHealth = MIN_HEARTS * 2;
                if (player.getInventory().contains(Material.DRAGON_EGG)) {
                    effectiveMinHealth += EGG_BONUS_HP;
                }

                double resultHealth = currentMax - (amount * 2.0);

                if (resultHealth < effectiveMinHealth) {
                    player.sendMessage("§cYou cannot withdraw your minimum hearts or your Dragon Egg hearts!");
                    return true;
                }

                attr.setBaseValue(resultHealth);
                ItemStack heartItem = createHeartItem();
                heartItem.setAmount(amount);
                player.getInventory().addItem(heartItem);
                player.sendMessage("§aWithdrew " + amount + " heart(s)!");

            } catch (NumberFormatException e) {
                player.sendMessage("§cInvalid number.");
            }
            return true;
        }
        return false;
    }

    // --- DEATH & USE ---

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        AttributeInstance victimAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance killerAttr = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (victimAttr.getBaseValue() <= MIN_HEARTS * 2 + (playerHasEgg(victim) ? EGG_BONUS_HP : 0)) {
            killer.sendMessage("§e" + victim.getName() + " is at their minimum hearts. No heart gained.");
            return;
        }

        victimAttr.setBaseValue(victimAttr.getBaseValue() - 2.0);
        
        double limit = DEFAULT_MAX_HEARTS * 2 + (playerHasEgg(killer) ? EGG_BONUS_HP : 0);
        if (killerAttr.getBaseValue() < limit) {
            killerAttr.setBaseValue(killerAttr.getBaseValue() + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, killerAttr.getBaseValue()));
            killer.sendMessage("§a+1 ❤ §7Heart stolen!");
        }
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().getDisplayName().equals("§c❤ Heart")) return;

        Player player = event.getPlayer();
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        
        double limit = DEFAULT_MAX_HEARTS * 2 + (playerHasEgg(player) ? EGG_BONUS_HP : 0);

        if (attr.getBaseValue() < limit) {
            attr.setBaseValue(attr.getBaseValue() + 2.0);
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a+1 ❤ §7Heart consumed!");
        } else {
            player.sendMessage("§cYou have reached your heart limit!");
        }
        event.setCancelled(true);
    }

    private boolean playerHasEgg(Player player) {
        return player.getInventory().contains(Material.DRAGON_EGG);
    }
}
