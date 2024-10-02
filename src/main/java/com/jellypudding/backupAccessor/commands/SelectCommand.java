package com.jellypudding.backupAccessor.commands;

import com.jellypudding.backupAccessor.BackupAccessor;
import com.jellypudding.backupAccessor.util.BackupType;
import com.jellypudding.backupAccessor.util.FileFunctions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class SelectCommand implements CommandExecutor, TabCompleter {

    private final BackupAccessor plugin;

    public SelectCommand(BackupAccessor plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length != 3) {
            sender.sendMessage(Component.text("Usage: /backupaccessor select <world|player> <backup_name>", NamedTextColor.RED));
            return true;
        }

        BackupType backupType;
        try {
            backupType = BackupType.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text("Invalid backup type. Use 'world' or 'player'.", NamedTextColor.RED));
            return true;
        }

        String backupName = args[2];

        if (!FileFunctions.verifyAndSetBackupType(backupName, backupType)) {
            sender.sendMessage(Component.text("Invalid " + backupType.name().toLowerCase() + " backup. Choose one from /backupaccessor list " + backupType.name().toLowerCase() + " and check your spelling.", NamedTextColor.RED));
            return true;
        }

        String[] searchFolders = getBackupSearchPathsForType(backupType);
        String selectedBackup = backupType == BackupType.WORLD ? FileFunctions.getSelectedWorldBackup() : FileFunctions.getSelectedPlayerBackup();

        if (FileFunctions.validateBackupContentAndSetPath(sender, backupType, selectedBackup, searchFolders)) {
            reportSuccess(sender, selectedBackup, backupType);
        } else {
            reportFailure(sender, selectedBackup, backupType, searchFolders);
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 2) {
            return List.of("world", "player");
        } else if (args.length == 3) {
            BackupType backupType;
            try {
                backupType = BackupType.valueOf(args[1].toUpperCase());
            } catch (IllegalArgumentException e) {
                return new ArrayList<>();
            }

            String prefix = args[2].toLowerCase();
            return FileFunctions.getBackupListForType(backupType).stream()
                    .filter(p -> p.getValue() != null)
                    .flatMap(p -> p.getValue().stream())
                    .map(File::getName)
                    .filter(s -> s.toLowerCase().startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    private String[] getBackupSearchPathsForType(BackupType backupType) {
        return backupType == BackupType.WORLD ?
                new String[]{"region", "DIM1", "DIM-1"} :
                new String[]{"playerdata"};
    }

    private void reportSuccess(CommandSender sender, String backupName, BackupType backupType) {
        String fileType = FileFunctions.getFileType(backupType);
        String tarDirectory = backupType == BackupType.WORLD ? FileFunctions.getTarDirectoryWorld() : FileFunctions.getTarDirectoryPlayer();

        Component successMessage;
        if ("directory".equals(fileType)) {
            if (backupType == BackupType.WORLD) {
                successMessage = Component.text("Successfully found world backup:\n", NamedTextColor.GREEN)
                        .append(Component.text("Overworld: " + FileFunctions.getSelectedWorldBackup() + "\n", NamedTextColor.GOLD))
                        .append(Component.text("Nether: " + FileFunctions.getSelectedNetherBackup() + "\n", NamedTextColor.GOLD))
                        .append(Component.text("End: " + FileFunctions.getSelectedEndBackup(), NamedTextColor.GOLD));
            } else {
                successMessage = Component.text("Successfully found player backup:\n", NamedTextColor.GREEN)
                        .append(Component.text(FileFunctions.getSelectedPlayerBackup(), NamedTextColor.GOLD));
            }
        } else {
            successMessage = Component.text("Successfully found " + backupType.name().toLowerCase() + " backup inside the " + fileType + " file\n", NamedTextColor.GREEN)
                    .append(Component.text(backupName + ": " + tarDirectory, NamedTextColor.GOLD));
        }

        sender.sendMessage(successMessage);
    }

    private void reportFailure(CommandSender sender, String backupName, BackupType backupType, String[] searchFolders) {
        String fileType = FileFunctions.getFileType(backupType);
        String folderType = searchFolders[0];

        String folderDescription = "any " + folderType + " folders";
        if (backupType == BackupType.PLAYER && "playerdata".equals(folderType)) {
            folderDescription = "a playerdata folder";
        }

        String locationDescription = "directory".equals(fileType) ? " within the directory " : " within the " + fileType + " file ";
        sender.sendMessage(Component.text("Failed to find " + folderDescription + locationDescription + backupName, NamedTextColor.RED));
    }
}