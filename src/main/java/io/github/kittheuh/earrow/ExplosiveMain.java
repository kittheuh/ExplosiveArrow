package io.github.kittheuh.earrow;

import com.destroystokyo.paper.brigadier.BukkitBrigadierCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.persistence.PersistentDataViewHolder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class ExplosiveMain extends JavaPlugin {
    private final NamespacedKey explosiveArrowKey = new NamespacedKey(this, "expl_arrow");
    private boolean isFolia = false;

    @Override
    public void onLoad() {
        saveDefaultConfig();
        reloadConfig();
        isFolia = isFolia();
    }

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ArrowListener(this), this);
        Objects.requireNonNull(getCommand("explosive")).setExecutor(new ExplosiveCommand(this));
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public NamespacedKey getExplosiveArrowKey() {
        return explosiveArrowKey;
    }

    public ItemStack createExplosiveArrow() {
        ItemStack stack = ItemStack.of(Material.TIPPED_ARROW, 1);

        ItemMeta meta = stack.getItemMeta();
        if (meta instanceof PotionMeta potionMeta) potionMeta.setBasePotionType(PotionType.FIRE_RESISTANCE);

        meta.displayName(Component.text("Explosive Arrow", ItemRarity.UNCOMMON.color()));
        meta.setRarity(ItemRarity.UNCOMMON);

        List<Component> lore = new ArrayList<>(2);
        lore.add(Component.text("Explodes on impact", NamedTextColor.GOLD));

        if (!getConfig().getBoolean("firing.allow-multishot", false) && getConfig().getBoolean("show-multishot-warn", true))
            lore.add(Component.text("Cannot be loaded onto items enchanted w/Multishot", NamedTextColor.GRAY));

        meta.lore(lore);
        meta.setEnchantmentGlintOverride(true);
        applyExplosiveTrait(meta, 1);
        stack.setItemMeta(meta);

        return stack;
    }

    public void applyExplosiveTrait(PersistentDataHolder holder, float force) {
        holder.getPersistentDataContainer().set(getExplosiveArrowKey(), PersistentDataType.FLOAT, force);
    }

    public float getExplosiveTrait(PersistentDataViewHolder holder) {
        return holder.getPersistentDataContainer().getOrDefault(getExplosiveArrowKey(), PersistentDataType.FLOAT, 1f);
    }

    public boolean notExplosiveArrow(PersistentDataViewHolder holder) {
        return !holder.getPersistentDataContainer().has(getExplosiveArrowKey(), PersistentDataType.FLOAT);
    }

    public boolean checkFolia() {
        return isFolia;
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
