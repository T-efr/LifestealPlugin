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
        player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(
