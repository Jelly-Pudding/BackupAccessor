package com.jellypudding.backupAccessor.util;

import com.jellypudding.backupAccessor.BackupAccessor;

import com.jellypudding.backupAccessor.commands.ImportCommand;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.utils.IOUtils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.bukkit.command.CommandSender;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

public class FileFunctions {

    private static final int ITEMS_PER_PAGE = 10;

    private static String selectedWorldBackup = "";
    private static String selectedNetherBackup = "";
    private static String selectedEndBackup = "";
    private static String selectedPlayerBackup = "";

    private static String tarDirectoryWorld = "";
    private static String tarDirectoryNether = "";
    private static String tarDirectoryEnd = "";
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

    public static boolean checkWorldPath(Player player) {
        File worldPathFile = new File(BackupAccessor.getInstance().getWorldPath());
        if (worldPathFile.exists()) {
            File[] listOfFiles = worldPathFile.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isDirectory() && (file.getName().equals("world") || file.getName().equals("world_nether") || file.getName().equals("world_the_end"))) {
                        // This worldPath is most likely correct.
                        return true;
                    }
                }
            }
            player.sendMessage(Component.text("The worldPath provided in config.yml does not appear to be correct.", NamedTextColor.DARK_RED));
        } else {
            player.sendMessage(Component.text("The worldPath provided in config.yml does not exist.", NamedTextColor.DARK_RED));
        }
        return false;
    }

    public static boolean isBackupAccessorEmpty() {
        File worldFolder = new File(BackupAccessor.getInstance().getWorldPath(), WorldFunctions.BACKUP_WORLD_NAME);
        File regionFolder = new File(worldFolder, "region");
        if (!regionFolder.exists() || !regionFolder.isDirectory()) {
            return true;
        }
        File[] regionFiles = regionFolder.listFiles((dir, name) -> name.endsWith(".mca"));
        return regionFiles == null || regionFiles.length == 0;
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
                    selectedPlayerBackup = file.getAbsolutePath() + File.separator;
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
        // Use a stack to process directories iteratively
        Stack<File> stack = new Stack<>();
        stack.push(folder);

        // Local variables to track which dimensions were found
        boolean foundOverworld = false;
        boolean foundNether = false;
        boolean foundEnd = false;

        // Keep track if any dimension was found
        boolean anyDimensionFound = false;

        while (!stack.isEmpty()) {
            File currentFolder = stack.pop();
            File[] listOfFiles = currentFolder.listFiles();

            if (listOfFiles == null) {
                continue;
            }

            for (File file : listOfFiles) {
                if (file.isDirectory()) {
                    String fileName = file.getName();
                    if (fileName.equals("region")) {
                        selectedWorldBackup = file.getAbsolutePath() + File.separator;
                        foundOverworld = true;
                        anyDimensionFound = true;
                    } else if (fileName.equals("DIM-1")) {
                        File netherRegion = new File(file, "region");
                        if (netherRegion.exists() && netherRegion.isDirectory()) {
                            selectedNetherBackup = netherRegion.getAbsolutePath() + File.separator;
                            foundNether = true;
                            anyDimensionFound = true;
                        }
                    } else if (fileName.equals("DIM1")) {
                        File endRegion = new File(file, "region");
                        if (endRegion.exists() && endRegion.isDirectory()) {
                            selectedEndBackup = endRegion.getAbsolutePath() + File.separator;
                            foundEnd = true;
                            anyDimensionFound = true;
                        }
                    } else if (!fileName.equals("playerdata")) {
                        stack.push(file);
                    }
                }
            }
        }

        // After the search, if at least one dimension was found, set missing ones to empty strings
        if (anyDimensionFound) {
            if (!foundOverworld) {
                selectedWorldBackup = "";
            }
            if (!foundNether) {
                selectedNetherBackup = "";
            }
            if (!foundEnd) {
                selectedEndBackup = "";
            }
        }

        // Return true if any dimension was found
        return anyDimensionFound;
    }

    private static boolean isValidDirectory(File file, String[] acceptedFolders) {
        return file.isDirectory() && Arrays.asList(acceptedFolders).contains(file.getName());
    }

    private static boolean validateTarBackup(CommandSender sender, BackupType backupType, String directory, String[] acceptedFolders) {
        try (TarArchiveInputStream tarInput = createTarInputStream(sender, backupType, directory)) {
            if (tarInput == null) {
                return false;
            }

            boolean worldFound = false;
            boolean netherFound = false;
            boolean endFound = false;
            boolean anyFound = false;

            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null) {
                if (entry.isDirectory() && isValidTarEntry(entry, backupType, acceptedFolders)) {
                    if (backupType == BackupType.WORLD) {
                        String dimension = setTarDirectory(entry, backupType);
                        switch (dimension) {
                            case "world":
                                worldFound = true;
                                break;
                            case "nether":
                                netherFound = true;
                                break;
                            case "end":
                                endFound = true;
                                break;
                        }
                        anyFound = true;
                    } else {
                        setTarDirectory(entry, backupType);
                        return true; // For player backups, we only need to find one valid entry
                    }
                }
            }

            // If at least one dimension was found, set the others to empty strings if not found
            if (anyFound) {
                if (!worldFound) tarDirectoryWorld = "";
                if (!netherFound) tarDirectoryNether = "";
                if (!endFound) tarDirectoryEnd = "";
            }

            return anyFound; // For world backups, return true if any dimension was found
        } catch (IOException e) {
            sender.sendMessage("Error while reading Tar file: " + e.getMessage());
        }
        return false;
    }

    private static TarArchiveInputStream createTarInputStream(CommandSender sender, BackupType backupType, String directory) throws IOException {
        InputStream fileStream = new FileInputStream(directory);
        BufferedInputStream bufferedStream = new BufferedInputStream(fileStream);

        if ((backupType == BackupType.WORLD && tarFileWorld) || (backupType == BackupType.PLAYER && tarFilePlayer)) {
            return new TarArchiveInputStream(bufferedStream);
        } else {
            InputStream compressorStream;
            if ((backupType == BackupType.WORLD && tarCompressedFileWorld) || (backupType == BackupType.PLAYER && tarCompressedFilePlayer)) {
                compressorStream = new GzipCompressorInputStream(bufferedStream);
            } else if ((backupType == BackupType.WORLD && tarBz2FileWorld) || (backupType == BackupType.PLAYER && tarBz2FilePlayer)) {
                compressorStream = new BZip2CompressorInputStream(bufferedStream);
            } else {
                sender.sendMessage("Unsupported compression format");
                return null;
            }
            return new TarArchiveInputStream(compressorStream);
        }
    }

    private static boolean isValidTarEntry(TarArchiveEntry entry, BackupType backupType, String[] acceptedFolders) {
        String[] processedEntry = processTarEntryName(entry);
        String folderName = processedEntry[0];
        String parentFolder = processedEntry[2];

        if (backupType == BackupType.PLAYER && (parentFolder.equals("DIM1") || parentFolder.equals("DIM-1"))) {
            return false;
        }

        return Arrays.asList(acceptedFolders).contains(folderName);
    }

    private static String setTarDirectory(TarArchiveEntry entry, BackupType backupType) {
        String[] processedEntry = processTarEntryName(entry);
        String parentPath = processedEntry[1];

        switch (backupType) {
            case WORLD:
                if (entry.getName().contains("DIM-1")) {
                    tarDirectoryNether = parentPath + "region" + File.separator;
                    return "nether";
                } else if (entry.getName().contains("DIM1")) {
                    tarDirectoryEnd = parentPath + "region" + File.separator;
                    return "end";
                } else {
                    tarDirectoryWorld = parentPath + "region" + File.separator;
                    return "world";
                }
            case PLAYER:
                tarDirectoryPlayer = parentPath + "playerdata" + File.separator;
                return "player";
            default:
                throw new IllegalArgumentException("Unexpected backup type: " + backupType);
        }
    }

    private static String[] processTarEntryName(TarArchiveEntry entry) {
        String entryName = entry.getName();
        String entryMinusSlash = entryName.endsWith("/") ? entryName.substring(0, entryName.length() - 1) : entryName;

        String folderName = entryMinusSlash.substring(entryMinusSlash.lastIndexOf("/") + 1);
        String parentPath = entryMinusSlash.substring(0, entryMinusSlash.length() - folderName.length());

        String parentMinusSlash = parentPath.endsWith("/") ? parentPath.substring(0, parentPath.length() - 1) : parentPath;
        String parentFolder = parentMinusSlash.substring(parentMinusSlash.lastIndexOf('/') + 1);

        return new String[]{folderName, parentPath, parentFolder};
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

    public static boolean isTarBackup(BackupType backupType) {
        if (backupType == BackupType.WORLD) {
            return tarFileWorld || tarCompressedFileWorld || tarBz2FileWorld;
        } else {
            return tarFilePlayer || tarCompressedFilePlayer || tarBz2FilePlayer;
        }
    }

    public static String getSelectedWorldBackup() { return selectedWorldBackup; }
    public static String getSelectedNetherBackup() { return selectedNetherBackup; }
    public static String getSelectedEndBackup() { return selectedEndBackup; }
    public static String getSelectedPlayerBackup() { return selectedPlayerBackup; }
    public static String getTarDirectoryWorld() { return tarDirectoryWorld; }
    public static String getTarDirectoryNether() { return tarDirectoryNether; }
    public static String getTarDirectoryEnd() { return tarDirectoryEnd; }
    public static String getTarDirectoryPlayer() { return tarDirectoryPlayer; }


    public static boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }



    public static boolean transferFiles(Player player, BackupType backupType, String selectedBackup, List<String> files, String type, String dummyUUID) {
        if (backupType == BackupType.WORLD) {
            // Creates parent directories if they do not already exist.
            new File(BackupAccessor.getInstance().getWorldPath() + "/BackupAccessor/region").mkdirs();
        }

        boolean allSuccessful = true;
        try {
            for (String file : files) {
                File source = new File(selectedBackup + file);
                File destination = new File(BackupAccessor.getInstance().getWorldPath() +
                        (backupType == BackupType.WORLD ? "/BackupAccessor/region/" + file : "/playerdata/" + dummyUUID + ".dat"));
                if (source.exists()) {
                    Files.copy(source.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } else {
                    player.sendMessage(Component.text("Error: The " + type + " file (" + source.getAbsolutePath() + ") does not exist.").color(NamedTextColor.RED));
                    allSuccessful = false;
                    break;
                }
            }

            if (allSuccessful) {
                if (backupType == BackupType.WORLD) {
                    ImportCommand.updateExtentsForImportedRegions(files);
                }
                player.sendMessage(Component.text("Done!").color(NamedTextColor.GREEN));
            }
        } catch (IOException e) {
            player.sendMessage(Component.text("Copy file error: " + e.getMessage()).color(NamedTextColor.RED));
            allSuccessful = false;
        }
        return allSuccessful;
    }

    public static void transferFromTar(Player player, BackupType backupType, String desiredDirectory, List<String> desiredFiles, String targetName, String dummyUUID, boolean enderChest) {
        String selectedDirectory = backupType == BackupType.WORLD ? selectedWorldBackup : selectedPlayerBackup;

        Bukkit.getScheduler().runTaskAsynchronously(BackupAccessor.getInstance(), () -> {
            boolean done = processTarFile(player, backupType, selectedDirectory, desiredDirectory, desiredFiles, dummyUUID);
            postProcessTransfer(player, done, backupType, targetName, dummyUUID, enderChest);
        });
    }

    private static boolean processTarFile(Player player, BackupType backupType, String tarFilePath, String desiredDirectory, List<String> desiredFiles, String dummyUUID) {
        try (TarArchiveInputStream tarInput = createTarInputStream(player, backupType, tarFilePath)) {
            if (tarInput == null) {
                return false;
            }

            File tempDir = new File(BackupAccessor.getInstance().getDataFolder(), "temp_" + System.currentTimeMillis());
            tempDir.mkdirs();

            Set<String> filesToTransfer = new HashSet<>(desiredFiles);
            TarArchiveEntry entry;
            while ((entry = tarInput.getNextTarEntry()) != null && !filesToTransfer.isEmpty()) {
                String entryName = entry.getName();
                for (String desiredFile : filesToTransfer) {
                    if (entryName.equals(desiredDirectory + desiredFile)) {
                        String destination = backupType == BackupType.WORLD ?
                                "/BackupAccessor/region/" + desiredFile :
                                "/playerdata/" + dummyUUID + ".dat";
                        if (!extractAndCopyFile(player, backupType, tempDir, tarInput, entry, destination)) {
                            return false;
                        }
                        filesToTransfer.remove(desiredFile);
                        break;
                    }
                }
            }

            if (!filesToTransfer.isEmpty()) {
                player.sendMessage(Component.text("Error: Some files were not found in the tar archive.").color(NamedTextColor.RED));
                return false;
            }

            if (backupType == BackupType.WORLD) {
                ImportCommand.updateExtentsForImportedRegions(desiredFiles);
            }

            return true;
        } catch (IOException e) {
            player.sendMessage(Component.text("Error processing tar file: " + e.getMessage()).color(NamedTextColor.RED));
            return false;
        }
    }

    private static boolean extractAndCopyFile(Player player, BackupType backupType, File tempDir, TarArchiveInputStream tarInput, TarArchiveEntry entry, String destination) throws IOException {
        File outputFile = new File(tempDir, entry.getName());
        outputFile.getParentFile().mkdirs();

        try (OutputStream outputFileStream = new FileOutputStream(outputFile)) {
            IOUtils.copy(tarInput, outputFileStream);
        }

        File destinationFile = new File(BackupAccessor.getInstance().getWorldPath() + destination);
        destinationFile.getParentFile().mkdirs();
        Files.copy(outputFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        player.sendMessage(Component.text("Imported the " + backupType.name().toLowerCase() + " file " + entry.getName()).color(NamedTextColor.GREEN));
        return true;
    }

    private static void postProcessTransfer(Player player, boolean done, BackupType backupType, String targetName, String dummyUUID, boolean enderChest) {
        if (!done) {
            ImportCommand.completeImport(player);
            return;
        }

        Bukkit.getScheduler().runTask(BackupAccessor.getInstance(), () -> {
            if (backupType == BackupType.WORLD) {
                player.sendMessage(Component.text("Finished importing regions!").color(NamedTextColor.GREEN));
                if (WorldFunctions.doesBackupAccessorWorldExist()) {
                    WorldFunctions.loadBackupAccessorWorldAndAdjustBorder(player, true);
                }
            } else {
                // Handle player inventory viewing here
            }
            ImportCommand.completeImport(player);
        });
    }


}