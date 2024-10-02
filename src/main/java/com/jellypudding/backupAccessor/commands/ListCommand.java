package com.jellypudding.backupAccessor.commands;

import com.jellypudding.backupAccessor.util.BackupType;
import com.jellypudding.backupAccessor.util.FileFunctions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ListCommand implements CommandExecutor, TabCompleter {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length < 2 || args.length > 3) {
            sender.sendMessage(Component.text("Usage: /backupaccessor list <world|player> [page]", NamedTextColor.RED));
            return true;
        }

        String backupTypeArg = args[1].toLowerCase();
        BackupType backupType;

        try {
            backupType = BackupType.valueOf(backupTypeArg.toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid backup type. Use 'world' or 'player'.", NamedTextColor.RED));
            return true;
        }

        int page = 1;
        if (args.length == 3) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(Component.text("Invalid page number. Please use a number.", NamedTextColor.RED));
                return true;
            }
        }

        FileFunctions.listContents(sender, backupType, page);
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 2) {
            return List.of("world", "player");
        }
        return new ArrayList<>();
    }
}