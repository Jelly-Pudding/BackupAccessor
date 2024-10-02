package com.jellypudding.backupAccessor.util;

import com.jellypudding.backupAccessor.BackupAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

public class FileFunctions {

    private static final int ITEMS_PER_PAGE = 10;

    private static String selectedWorldBackup = "";
    private static String selectedNetherBackup = "";
    private static String selectedEndBackup = "";
    private static String selectedPlayerBackup = "";

    private static String tarDirectoryWorld = "";
    private static String tarDirectoryPlayer = "";

    private static boolean tarFileWorld = false;
    private static boolean tarCompressedFileWorld = false;
    private static boolean tarBz2FileWorld = false;
    private static boolean tarFilePlayer = false;
    private static boolean tarCompressedFilePlayer = false;
    private static boolean tarBz2FilePlayer = false;

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

    private static void sendPaginatedContents(CommandSender sender, List<Component> contents, String title, int page) {
        int totalPages = (contents.size() + ITEMS_PER_PAGE - 1) / ITEMS_PER_PAGE;
        page = Math.max(1, Math.min(page, totalPages));

        int startIndex = (page - 1) * ITEMS_PER_PAGE;
        int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, contents.size());

        Component header = Component.text("--- " + title + " (Page " + page + "/" + totalPages + ") ---", NamedTextColor.GREEN);
        sender.sendMessage(header);

        for (int i = startIndex; i < endIndex; i++) {
            sender.sendMessage(contents.get(i));
        }

        Component footer = Component.text("---------------------------", NamedTextColor.GREEN);
        sender.sendMessage(footer);

        if (page < totalPages) {
            sender.sendMessage(Component.text("Use '/backupaccessor list " +
                    (title.startsWith("world") ? "world" : "player") +
                    " " + (page + 1) + "' to see the next page", NamedTextColor.YELLOW));
        }
    }

    public static boolean verifyAndSetBackupType(String playerGivenName, BackupType backupType) {
        List<String> backupPaths = backupType == BackupType.WORLD ?
                BackupAccessor.getInstance().getWorldBackupPaths() :
                BackupAccessor.getInstance().getPlayerBackupPaths();

        for (String backupPath : backupPaths) {
            File folder = new File(backupPath, playerGivenName);
            if (!folder.exists()) {
                continue;
            }

            String absolutePath = folder.getAbsolutePath();
            if (folder.isDirectory()) {
                updateBackupPath(absolutePath, backupType, false, false, false);
            } else if (playerGivenName.endsWith(".tar")) {
                updateBackupPath(absolutePath, backupType, true, false, false);
            } else if (playerGivenName.endsWith(".tar.gz") || playerGivenName.endsWith(".tgz")) {
                updateBackupPath(absolutePath, backupType, false, true, false);
            } else if (playerGivenName.endsWith(".tar.bz2")) {
                updateBackupPath(absolutePath, backupType, false, false, true);
            } else {
                continue;
            }
            return true;
        }
        return false;
    }

    private static void updateBackupPath(String path, BackupType backupType, boolean tar, boolean compressedTar, boolean bz2) {
        if (backupType == BackupType.WORLD) {
            selectedWorldBackup = path;
            tarFileWorld = tar;
            tarCompressedFileWorld = compressedTar;
            tarBz2FileWorld = bz2;
        } else {
            selectedPlayerBackup = path;
            tarFilePlayer = tar;
            tarCompressedFilePlayer = compressedTar;
            tarBz2FilePlayer = bz2;
        }
    }

    public static boolean validateBackupContentAndSetPath(CommandSender sender, BackupType backupType, String directory, String[] acceptedFolders) {
        resetSelectedBackups();
        if (isDirectoryBackup(backupType)) {
            return validateDirectoryBackup(sender, backupType, directory, acceptedFolders);
        } else {
            return validateTarBackup(sender, backupType, directory, acceptedFolders);
        }
    }

    private static boolean isDirectoryBackup(BackupType backupType) {
        switch (backupType) {
            case WORLD:
                return !tarFileWorld && !tarCompressedFileWorld && !tarBz2FileWorld;
            case PLAYER:
                return !tarFilePlayer && !tarCompressedFilePlayer && !tarBz2FilePlayer;
            default:
                throw new AssertionError("Missing switch case for " + backupType);
        }
    }

    private static boolean validateDirectoryBackup(CommandSender sender, BackupType backupType, String directory, String[] acceptedFolders) {
        File folder = new File(directory);
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return false;
        }

        if (backupType == BackupType.WORLD) {
            return searchWorldBackup(folder);
        } else {
            // Player backup validation
            for (File file : listOfFiles) {
                if (isValidDirectory(file, acceptedFolders)) {
                    selectedPlayerBackup = file.getAbsolutePath();
                    return true;
                }
                if (file.isDirectory() && validateDirectoryBackup(sender, backupType, file.getAbsolutePath(), acceptedFolders)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean searchWorldBackup(File folder) {
        File[] listOfFiles = folder.listFiles();
        if (listOfFiles == null) {
            return false;
        }

        boolean found = false;

        for (File file : listOfFiles) {
            if (file.isDirectory()) {
                if (file.getName().equals("region") && selectedWorldBackup.isEmpty()) {
                    selectedWorldBackup = file.getAbsolutePath();
                    found = true;
                } else if (file.getName().equals("DIM-1") && selectedNetherBackup.isEmpty()) {
                    File netherRegion = new File(file, "region");
                    if (netherRegion.exists() && netherRegion.isDirectory()) {
                        selectedNetherBackup = netherRegion.getAbsolutePath();
                        found = true;
                    }
                } else if (file.getName().equals("DIM1") && selectedEndBackup.isEmpty()) {
                    File endRegion = new File(file, "region");
                    if (endRegion.exists() && endRegion.isDirectory()) {
                        selectedEndBackup = endRegion.getAbsolutePath();
                        found = true;
                    }
                } else if (!selectedWorldBackup.isEmpty() && !selectedNetherBackup.isEmpty() && !selectedEndBackup.isEmpty()) {
                    return true; // All three dimensions found, no need to continue searching
                } else if (!file.getName().equals("playerdata")) {
                    boolean subDirFound = searchWorldBackup(file);
                    found = found || subDirFound;
                }
            }
        }

        return found;
    }

    private static boolean isValidDirectory(File file, String[] acceptedFolders) {
        return file.isDirectory() && Arrays.asList(acceptedFolders).contains(file.getName());
    }

    private static boolean validateTarBackup(CommandSender sender, BackupType backupType, String directory, String[] acceptedFolders) {
        // TODO: Implement tar file validation
        // This would require additional libraries and more complex logic to handle tar files
        // For now, we'll return false and log a message
        sender.sendMessage(Component.text("Tar file validation is not implemented yet.", NamedTextColor.RED));
        return false;
    }

    public static List<Pair<String, List<File>>> getBackupListForType(BackupType backupType) {
        List<String> backupPaths = backupType == BackupType.WORLD ?
                BackupAccessor.getInstance().getWorldBackupPaths() :
                BackupAccessor.getInstance().getPlayerBackupPaths();

        return getBackupList(backupPaths);
    }

    private static List<Pair<String, List<File>>> getBackupList(List<String> backupPaths) {
        List<Pair<String, List<File>>> backupFiles = new ArrayList<>();
        for (String backupPath : backupPaths) {
            File folder = new File(backupPath);
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                Arrays.sort(listOfFiles, Comparator.comparing(File::getName));
                backupFiles.add(Pair.of(backupPath, Arrays.asList(listOfFiles)));
            } else {
                backupFiles.add(Pair.of(backupPath, null));
            }
        }
        return backupFiles;
    }

    public static String getFileType(BackupType backupType) {
        if (backupType == BackupType.WORLD) {
            if (tarFileWorld) return "tar";
            if (tarCompressedFileWorld) return "compressed tar";
            if (tarBz2FileWorld) return "tar.bz2";
        } else {
            if (tarFilePlayer) return "tar";
            if (tarCompressedFilePlayer) return "compressed tar";
            if (tarBz2FilePlayer) return "tar.bz2";
        }
        return "directory";
    }

    private static void resetSelectedBackups() {
        selectedWorldBackup = "";
        selectedNetherBackup = "";
        selectedEndBackup = "";
        selectedPlayerBackup = "";
    }

    public static String getSelectedWorldBackup() {
        return selectedWorldBackup;
    }

    public static String getSelectedNetherBackup() {
        return selectedNetherBackup;
    }

    public static String getSelectedEndBackup() {
        return selectedEndBackup;
    }

    public static String getSelectedPlayerBackup() {
        return selectedPlayerBackup;
    }

    public static String getTarDirectoryWorld() {
        return tarDirectoryWorld;
    }

    public static String getTarDirectoryPlayer() {
        return tarDirectoryPlayer;
    }
}