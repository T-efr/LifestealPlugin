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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;

public class LifestealPlugin extends JavaPlugin implements Listener {

    private static final int MIN_HEARTS = 3;
    private static final int DEFAULT_MAX_HEARTS = 20;
    private static final double EGG_BONUS_HP = 10.0; // 5 Hearts
    private NamespacedKey eggTag;

    @Override
    public void onEnable() {
        this.eggTag = new NamespacedKey(this, "has_egg_buff");
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

    // --- IMPROVED DRAGON EGG LOGIC ---

    private void handleEggBuff(Player player) {
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (attr == null) return;

        boolean hasEggInInv = player.getInventory().contains(Material.DRAGON_EGG);
        boolean hasEggBuffApplied = player.getPersistentDataContainer().has(eggTag, PersistentDataType.BYTE);

        // CASE 1: Has egg but NO buff applied yet -> Add 5 hearts
        if (hasEggInInv && !hasEggBuffApplied) {
            attr.setBaseValue(attr.getBaseValue() + EGG_BONUS_HP);
            player.getPersistentDataContainer().set(eggTag, PersistentDataType.BYTE, (byte) 1);
            player.sendMessage("§5§lThe Dragon Egg grants you 5 extra hearts!");
        } 
        // CASE 2: No egg in inv but buff IS applied -> Remove 5 hearts
        else if (!hasEggInInv && hasEggBuffApplied) {
            double newMax = Math.max(MIN_HEARTS * 2, attr.getBaseValue() - EGG_BONUS_HP);
            attr.setBaseValue(newMax);
            if (player.getHealth() > newMax) player.setHealth(newMax);
            player.getPersistentDataContainer().remove(eggTag);
            player.sendMessage("§cThe Dragon Egg was lost. You lost 5 hearts!");
        }
    }

    @EventHandler public void onDrop(PlayerDropItemEvent e) { handleEggBuff(e.getPlayer()); }
    @EventHandler public void onInvClose(InventoryCloseEvent e) { handleEggBuff((Player) e.getPlayer()); }
    @EventHandler public void onJoin(PlayerJoinEvent e) { handleEggBuff(e.getPlayer()); }

    // --- WITHDRAW COMMAND ---

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("withdraw")) {
            if (!(sender instanceof Player player)) return true;
            if (args.length != 1) { player.sendMessage("§c/withdraw <amount>"); return true; }

            try {
                int amount = Integer.parseInt(args[0]);
                if (amount <= 0) return true;

                AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                double currentMax = attr.getBaseValue();
                
                // Effective floor: 3 hearts normally, 8 hearts if holding egg
                double minLimit = (MIN_HEARTS * 2) + (player.getPersistentDataContainer().has(eggTag, PersistentDataType.BYTE) ? EGG_BONUS_HP : 0);

                if (currentMax - (amount * 2.0) < minLimit) {
                    player.sendMessage("§cYou cannot withdraw your minimum hearts or Dragon Egg bonus!");
                    return true;
                }

                attr.setBaseValue(currentMax - (amount * 2.0));
                ItemStack heartItem = createHeartItem();
                heartItem.setAmount(amount);
                player.getInventory().addItem(heartItem);
                player.sendMessage("§aWithdrew " + amount + " heart(s)!");

            } catch (NumberFormatException e) { player.sendMessage("§cInvalid number."); }
            return true;
        }
        return false;
    }

    // --- DEATH & CONSUME ---

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();
        if (killer == null || killer.equals(victim)) return;

        AttributeInstance vAttr = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        AttributeInstance kAttr = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        double vMin = (MIN_HEARTS * 2) + (victim.getPersistentDataContainer().has(eggTag, PersistentDataType.BYTE) ? EGG_BONUS_HP : 0);
        if (vAttr.getBaseValue() <= vMin) {
            killer.sendMessage("§eTarget is at minimum hearts!");
            return;
        }

        vAttr.setBaseValue(vAttr.getBaseValue() - 2.0);
        
        double kLimit = (DEFAULT_MAX_HEARTS * 2) + (killer.getPersistentDataContainer().has(eggTag, PersistentDataType.BYTE) ? EGG_BONUS_HP : 0);
        if (kAttr.getBaseValue() < kLimit) {
            kAttr.setBaseValue(kAttr.getBaseValue() + 2.0);
            killer.setHealth(Math.min(killer.getHealth() + 2.0, kAttr.getBaseValue()));
            killer.sendMessage("§a+1 ❤ Heart stolen!");
        }
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        if (!event.getAction().name().contains("RIGHT_CLICK")) return;
        ItemStack item = event.getItem();
        if (item == null || !item.hasItemMeta() || !item.getItemMeta().getDisplayName().equals("§c❤ Heart")) return;

        Player player = event.getPlayer();
        AttributeInstance attr = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        double limit = (DEFAULT_MAX_HEARTS * 2) + (player.getPersistentDataContainer().has(eggTag, PersistentDataType.BYTE) ? EGG_BONUS_HP : 0);

        if (attr.getBaseValue() < limit) {
            attr.setBaseValue(attr.getBaseValue() + 2.0);
            item.setAmount(item.getAmount() - 1);
            player.sendMessage("§a+1 ❤ Heart consumed!");
        } else {
            player.sendMessage("§cYou are at your heart limit!");
        }
        event.setCancelled(true);
    }
}
