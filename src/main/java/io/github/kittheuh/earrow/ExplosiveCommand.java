package io.github.kittheuh.earrow;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public class ExplosiveCommand implements TabExecutor {
    private final ExplosiveMain plugin;

    public ExplosiveCommand(ExplosiveMain plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sender.sendRichMessage("<gold>Explosive Arrow plugin by kittheuh");
            return true;
        }

        if (args[0].equals("reload")) {
            if (!sender.hasPermission("explosive.reload")) return false;

            plugin.reloadConfig();
            sender.sendRichMessage("<green>Config reloaded.");
            return true;
        }

        if (!sender.hasPermission("explosive.give-arrows")) return false;
        handleGiveArrow(sender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 1) {
            if (sender.hasPermission("explosive.reload")) {
                if (args[0].isEmpty() || args[0].startsWith("r")) return List.of("reload");
            } else if (sender.hasPermission("explosive.give-arrows")) return Collections.emptyList();
        } else if (args.length > 1) {
            if (args[0].equals("reload") && sender.hasPermission("explosive.reload")) return Collections.emptyList();
            if (!sender.hasPermission("explosive.give-arrows")) return Collections.emptyList();

            try {
                Integer.parseInt(args[0]);
                return null;
            } catch (NumberFormatException e) {
                return Collections.emptyList();
            }
        }

        return null;
    }

    private void handleGiveArrow(CommandSender sender, String[] args) {
        int i;
        try {
            i = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendRichMessage("<red>Please provide a valid number.");
            return;
        }

        if (i < 0) {
            sender.sendRichMessage("<red>Please provide a number greater than 0.");
            return;
        } else if (i > 64) {
            sender.sendRichMessage("<gold>Stack count capped at 64.");
            i = 64;
        }

        Player player;
        if (args.length == 1 && !(sender instanceof Player)) {
            sender.sendRichMessage("<red>Please provide a player to give some arrows to.");
            return;
        }

        if (args.length > 1) { // player provided
            player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendRichMessage("<red>Player not found.");
                return;
            }
        } else player = (Player) sender;

        ItemStack explosiveArrow = plugin.createExplosiveArrow();
        explosiveArrow.add(i);

        player.getInventory().addItem(explosiveArrow);
        sender.sendRichMessage("<yellow>Explosive Arrow</yellow> <gray>x<count></gray> given to <gray><player></gray>.",
                Placeholder.unparsed("count", String.valueOf(i)),
                Placeholder.unparsed("player", player.getName())
        );
    }
}
