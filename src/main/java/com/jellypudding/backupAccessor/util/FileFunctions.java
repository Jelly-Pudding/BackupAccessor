package com.jellypudding.backupAccessor.util;

import com.jellypudding.backupAccessor.BackupAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileFunctions {

    public enum BackupType {
        WORLD,
        PLAYER
    }

    private static final int ITEMS_PER_PAGE = 10;

    public static void listContents(CommandSender sender, BackupType backupType, int page) {
        List<String> backupPaths = backupType == BackupType.WORLD ?
                BackupAccessor.getInstance().getWorldBackupPaths() :
                BackupAccessor.getInstance().getPlayerBackupPaths();

        List<Component> contents = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        for (String backupPath : backupPaths) {
            File folder = new File(backupPath);
            File[] files = folder.listFiles();

            if (files == null) {
                sender.sendMessage(Component.text("The directory '" + backupPath + "' does not exist.", NamedTextColor.RED));
                sender.sendMessage(Component.text("This can be changed in config.yml. Make sure to reload the plugin afterwards.", NamedTextColor.RED));
                continue;
            }

            Arrays.sort(files, Comparator.comparing(File::getName));

            for (File file : files) {
                if (file.isDirectory() || file.getName().endsWith(".tar") || file.getName().endsWith(".tar.bz2")
                        || file.getName().endsWith(".tar.gz") || file.getName().endsWith(".tgz")) {
                    try {
                        BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
                        String creationTime = dateFormat.format(new Date(attrs.creationTime().toMillis()));
                        contents.add(Component.text(file.getName(), NamedTextColor.RED)
                                .append(Component.text(" " + creationTime, NamedTextColor.GOLD)));
                    } catch (Exception e) {
                        contents.add(Component.text(file.getName(), NamedTextColor.RED));
                    }
                }
            }
        }

        if (contents.isEmpty()) {
            sender.sendMessage(Component.text("No backups found in the specified directories.", NamedTextColor.RED));
        } else {
            sendPaginatedContents(sender, contents, backupType.name().toLowerCase() + " backups", page);
        }
    }

    public static void sendPaginatedContents(CommandSender sender, List<Component> contents, String title, int page) {
        int totalPages = (contents.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        page = Math.max(1, Math.min(page, totalPages));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, contents.size());

        Component header = Component.text("--- " + title + " (Page " + page + "/" + totalPages + ") ---", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
        sender.sendMessage(header);

        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(contents.get(i));
        }

        Component footer = Component.text("---------------------------", NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
        sender.sendMessage(footer);

        if (page < totalPages) {
            sender.sendMessage(Component.text("Use '/backupaccessor list " +
                    (title.startsWith("world") ? "world" : "player") +
                    " " + (page + 1) + "' to see the next page", NamedTextColor.YELLOW));
        }
    }
}