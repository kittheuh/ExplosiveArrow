package io.github.kittheuh.earrow;

import com.destroystokyo.paper.event.player.PlayerReadyArrowEvent;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Dispenser;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.recipe.CraftingBookCategory;
import org.bukkit.projectiles.BlockProjectileSource;

import java.util.*;

public class ArrowListener implements Listener {
    private final List<Location> recentFires;
    private final NamespacedKey arrowRecipeKey;
    private final ExplosiveMain plugin;

    public ArrowListener(ExplosiveMain plugin) {
        this.plugin = plugin;
        arrowRecipeKey = new NamespacedKey(plugin, "expl_arrow_recipe");
        recentFires = plugin.checkFolia() ? Collections.synchronizedList(new ArrayList<>()) : new ArrayList<>();

        ShapedRecipe recipe = new ShapedRecipe(arrowRecipeKey, plugin.createExplosiveArrow())
                .shape("IGI",
                        "IAI",
                        "IBI"
                ).setIngredient('I', Material.IRON_NUGGET)
                .setIngredient('G', Material.GUNPOWDER)
                .setIngredient('A', Material.ARROW)
                .setIngredient('B', Material.BLAZE_POWDER);

        recipe.setCategory(CraftingBookCategory.EQUIPMENT);
        plugin.getServer().addRecipe(recipe);
    }


    @EventHandler(ignoreCancelled = true)
    public void onBlockPreDispense(BlockPreDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) return;
        if (plugin.notExplosiveArrow(event.getItemStack())) return;
        if (plugin.getConfig().getBoolean("firing.allow-dispenser", false)) return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDispense(BlockDispenseEvent event) {
        if (event.getBlock().getType() != Material.DISPENSER) return;
        if (plugin.notExplosiveArrow(event.getItem())) return;

        BlockData data = event.getBlock().getBlockData();
        if (!(data instanceof Dispenser dispenser)) return;

        // Disallow dispensing if the block in front cannot be passed to prevent wasting arrows
        Block blockInFront = event.getBlock().getLocation().add(dispenser.getFacing().getDirection()).getBlock();
        if (!blockInFront.isPassable()) {
            event.setCancelled(true);
            return;
        }

        recentFires.add(event.getBlock().getLocation());
    }

    @EventHandler(ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        checkRecentFire(event.getEntity());
    }

    @EventHandler
    public void onEntityTryShootArrow(PlayerReadyArrowEvent event) {
        ItemStack consumable = event.getArrow();
        if (consumable.getType() != Material.TIPPED_ARROW) return;
        if (plugin.notExplosiveArrow(consumable)) return;

        if (plugin.getConfig().getBoolean("firing.allow-multishot", false)) return; // don't bother checking if allowed

        if (event.getBow().containsEnchantment(Enchantment.MULTISHOT))
            event.setCancelled(true);
    }

    @EventHandler
    public void onEntityShootArrow(EntityShootBowEvent event) {
        ItemStack consumable = event.getConsumable();
        if (consumable == null || plugin.notExplosiveArrow(consumable)) return;

        plugin.applyExplosiveTrait(event.getProjectile(), event.getForce());

        // handle firing durability penalty
        if (event.getBow() == null) return;

        int penalty = plugin.getConfig().getInt("firing.penalty.amount", 2);
        if (penalty == 0) return;

        if (event.getEntity() instanceof Player player && player.getGameMode() == GameMode.CREATIVE) {
            if (plugin.getConfig().getBoolean("firing.penalty.ignore-creative", true)) return;
        }

        event.getBow().damage(penalty, event.getEntity());
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        if (!(projectile instanceof Arrow arrow)) return;

        // fallback in case the arrow was never marked properly when shot from dispenser?
        if (plugin.notExplosiveArrow(arrow) && !checkRecentFire(arrow)) return;

        Location explLoc = arrow.getLocation();
        if (plugin.getConfig().getBoolean("explosion.create-at-target", true)) {
            if (event.getHitEntity() != null) explLoc = event.getHitEntity().getLocation();
            else if (event.getHitBlock() != null) explLoc = event.getHitBlock().getLocation();
        }

        float power = (float) plugin.getConfig().getDouble("explosion.power", 2);
        if (plugin.getConfig().getBoolean("explosion.scale-with-force", true)) {
            power *= plugin.getExplosiveTrait(arrow);
        }

        boolean setFire = switch (plugin.getConfig().getString("explosion.set-fire", "flame")) {
            case "always" -> true;
            case "never" -> false;
            default -> arrow.getFireTicks() > 0;
        };

        boolean explode = plugin.getConfig().getBoolean("explosion.destroy-blocks", false);

        // only create explosion if 0 or above, negative disables it
        if (power >= 0) arrow.getWorld().createExplosion(explLoc, power, setFire, explode, projectile);

        // prevent the fire res effect from applying
        if (event.getHitEntity() != null) arrow.setBasePotionType(null);

        // delete if not persistent
        if (!plugin.getConfig().getBoolean("persist-after-explosion", false)) arrow.remove();
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) return;
        if (plugin.notExplosiveArrow(arrow)) return;

        if (plugin.getConfig().getBoolean("allow-damage-stacking", true)) return;
        event.setDamage(0);
    }

    private boolean checkRecentFire(Projectile projectile) {
        if (!(projectile.getShooter() instanceof BlockProjectileSource source)) return false; // designed for only blocks

        boolean removed = recentFires.remove(source.getBlock().getLocation());
        if (removed) plugin.applyExplosiveTrait(projectile, 1f);
        return removed;
    }

}
