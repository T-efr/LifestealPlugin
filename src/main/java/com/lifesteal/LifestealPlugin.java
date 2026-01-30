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
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
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
    private static final int DRAGON_EGG_MAX_HEARTS = 25;
    private static final int CRAFTED_HEART_MAX = 10;

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
            heart.setItemMeta(meta);
        }
        return heart;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
    }

    @EventHandler
    public void onUseHeart(PlayerInteractEvent event) {
        if (!event.getAction().isRightClick()) return;

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.RED_DYE) return;
        if (!item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        if (!meta.hasDisplayName()) return;
        if (!meta.getDisplayName().equals("§c❤ Heart")) return;

        double currentMaxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        if (currentMaxHealth >= CRAFTED_HEART_MAX * 2) {
            player.sendMessage("§cCrafted heart limit reached!");
            return;
        }

        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(currentMaxHealth + 2.0);
        player.setHealth(Math.min(player.getHealth() + 2.0, currentMaxHealth + 2.0));

        item.setAmount(item.getAmount() - 1);

        player.sendMessage("§a+1 ❤ Heart consumed!");
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) return;

        double victimMax = victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
        double killerMax = killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        if (victimMax <= MIN_HEARTS * 2) return;

        victim.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(victimMax - 2.0);

        if (killerMax < getMaxHearts(killer) * 2) {
            killer.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(killerMax + 2.0);
        }
    }

    @EventHandler
    public void onCraftHeart(CraftItemEvent event) {
        if (!event.getRecipe().getResult().isSimilar(createHeartItem())) return;

        Player player = (Player) event.getWhoClicked();
        double max = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();

        if (max >= CRAFTED_HEART_MAX * 2) {
            player.sendMessage("§cCrafted heart limit reached!");
            event.setCancelled(true);
        }
    }

    private int getMaxHearts(Player player) {
        if (player.getInventory().contains(Material.DRAGON_EGG)) {
            return DRAGON_EGG_MAX_HEARTS;
        }
        return DEFAULT_MAX_HEARTS;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }
}
}
