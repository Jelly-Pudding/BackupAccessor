package com.jellypudding.backupAccessor.commands;

import com.jellypudding.backupAccessor.BackupAccessor;
import com.jellypudding.backupAccessor.util.BackupType;
import com.jellypudding.backupAccessor.util.FileFunctions;
import com.jellypudding.backupAccessor.util.WorldFunctions;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ImportCommand {

    private final BackupAccessor plugin;
    private static final ConcurrentHashMap<String, AtomicInteger> ongoingImports = new ConcurrentHashMap<>();

    public ImportCommand(BackupAccessor plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(Player player, String[] args) {
        importRegions(player);
        return true;
    }

    private void importRegions(Player player) {
        if (!FileFunctions.checkWorldPath(player)) {
            player.sendMessage(Component.text("Please change the worldPath in config.yml to the path that has 'world/world_nether/world_the_end' in it.").color(NamedTextColor.RED));
            return;
        }

        if (FileFunctions.getSelectedWorldBackup().isEmpty()) {
            player.sendMessage(Component.text("Please select a world backup with /backupaccessor select world <backup>. Choose one from /backupaccessor list world.").color(NamedTextColor.RED));
            return;
        }

        World playerWorld = player.getWorld();

        if (playerWorld.getName().equalsIgnoreCase("BackupAccessor")) {
            player.sendMessage(Component.text("You cannot import a region when you are in the BackupAccessor world!").color(NamedTextColor.RED));
            return;
        }

        if (WorldFunctions.doesBackupAccessorWorldExist() && WorldFunctions.isBackupAccessorWorldLoaded()) {
            World backupAccessorWorld = Bukkit.getWorld(WorldFunctions.BACKUP_WORLD_NAME);
            if (backupAccessorWorld != null) {
                if (Bukkit.isTickingWorlds()) {
                    // Schedule the world unloading for the next tick
                    Bukkit.getScheduler().runTask(plugin, () -> unloadBackupAccessorWorld(player, backupAccessorWorld));
                } else {
                    // It's safe to unload the world immediately
                    unloadBackupAccessorWorld(player, backupAccessorWorld);
                }
                return; // Return here to wait for the world to unload before proceeding
            }
        }

        continueImportProcess(player);
    }

    private void unloadBackupAccessorWorld(Player player, World world) {
        if (!Bukkit.unloadWorld(world, false)) {
            player.sendMessage(Component.text("Cannot import region file as someone else is inside the BackupAccessor world!").color(NamedTextColor.RED));
        } else {
            // World unloaded successfully, continue with the import process
            continueImportProcess(player);
        }
    }

    private void continueImportProcess(Player player) {
        World playerWorld = player.getWorld();
        String selectedBackup;
        String selectedTarBackup;

        switch (playerWorld.getEnvironment()) {
            case NORMAL:
                selectedBackup = FileFunctions.getSelectedWorldBackup();
                selectedTarBackup = FileFunctions.getTarDirectoryWorld();
                break;
            case NETHER:
                selectedBackup = FileFunctions.getSelectedNetherBackup();
                selectedTarBackup = FileFunctions.getTarDirectoryNether();
                break;
            case THE_END:
                selectedBackup = FileFunctions.getSelectedEndBackup();
                selectedTarBackup = FileFunctions.getTarDirectoryEnd();
                break;
            default:
                player.sendMessage(Component.text("Failed to identify the dimension: " + playerWorld.getEnvironment()).color(NamedTextColor.RED));
                return;
        }

        boolean isDirectoryBackup = FileFunctions.isDirectoryBackup(BackupType.WORLD);

        if ((isDirectoryBackup && selectedBackup.isEmpty()) || (!isDirectoryBackup && selectedTarBackup.isEmpty())) {
            player.sendMessage(Component.text("No backup selected for this dimension.").color(NamedTextColor.RED));
            return;
        }

        Chunk playerChunk = player.getLocation().getChunk();
        List<String> regionsToImport = getNearbyRegions(player, playerChunk, 5);

        // Increment ongoing import count for the player
        ongoingImports.compute(player.getName(), (key, count) -> {
            if (count == null) {
                return new AtomicInteger(1);
            } else {
                count.incrementAndGet();
                return count;
            }
        });

        // Determine whether to use transferFiles or transferFromTar
        if (!isDirectoryBackup) {
            FileFunctions.transferFromTar(player, BackupType.WORLD, selectedTarBackup, regionsToImport, "", "", false);
        } else {
            CompletableFuture.runAsync(() -> {
                boolean success = FileFunctions.transferFiles(player, BackupType.WORLD, selectedBackup,
                        regionsToImport, "region", "");

                if (success && WorldFunctions.doesBackupAccessorWorldExist()) {
                    player.sendMessage(Component.text("Reloading BackupAccessor world...").color(NamedTextColor.GREEN));
                    Bukkit.getScheduler().runTask(plugin, () -> WorldFunctions.loadBackupAccessorWorldAndAdjustBorder(player, true));
                }

                completeImport(player);
            }).exceptionally(e -> {
                player.sendMessage(Component.text("An error occurred during import: " + e.getMessage()).color(NamedTextColor.RED));
                completeImport(player);
                return null;
            });
        }
    }

    private List<String> getNearbyRegions(Player player, Chunk chunk, int nearChunkThreshold) {
        int regionX = chunk.getX() >> 5;
        int regionZ = chunk.getZ() >> 5;

        int chunkXInRegion = chunk.getX() & 31;
        int chunkZInRegion = chunk.getZ() & 31;

        List<String> regions = new ArrayList<>();
        String currentRegion = "r." + regionX + "." + regionZ + ".mca";
        regions.add(currentRegion);

        Component message = Component.text("You are currently in the region: ", NamedTextColor.GRAY)
                .append(Component.text(currentRegion, NamedTextColor.DARK_GRAY));

        boolean nearNorth = chunkZInRegion < nearChunkThreshold;
        boolean nearSouth = chunkZInRegion > 31 - nearChunkThreshold;
        boolean nearWest = chunkXInRegion < nearChunkThreshold;
        boolean nearEast = chunkXInRegion > 31 - nearChunkThreshold;

        if (nearWest) message = addRegionToMessage("West", regionX - 1, regionZ, regions, message);
        if (nearEast) message = addRegionToMessage("East", regionX + 1, regionZ, regions, message);
        if (nearNorth) message = addRegionToMessage("North", regionX, regionZ - 1, regions, message);
        if (nearSouth) message = addRegionToMessage("South", regionX, regionZ + 1, regions, message);
        if (nearNorth && nearWest) message = addRegionToMessage("North-West", regionX - 1, regionZ - 1, regions, message);
        if (nearNorth && nearEast) message = addRegionToMessage("North-East", regionX + 1, regionZ - 1, regions, message);
        if (nearSouth && nearWest) message = addRegionToMessage("South-West", regionX - 1, regionZ + 1, regions, message);
        if (nearSouth && nearEast) message = addRegionToMessage("South-East", regionX + 1, regionZ + 1, regions, message);

        message = message.append(Component.newline())
                .append(Component.text("Importing ", NamedTextColor.GRAY))
                .append(Component.text(regions.size(), NamedTextColor.DARK_GRAY))
                .append(Component.text(" region" + (regions.size() > 1 ? " files" : " file") + " into BackupAccessor: ", NamedTextColor.GRAY));

        for (int i = 0; i < regions.size(); i++) {
            if (i > 0) message = message.append(Component.text(", ", NamedTextColor.GRAY));
            message = message.append(Component.text(regions.get(i), NamedTextColor.DARK_GRAY));
        }

        message = message.append(Component.newline())
                .append(Component.text("This should take a minute or two, but it may take longer than that. Please have patience.", NamedTextColor.LIGHT_PURPLE));

        player.sendMessage(message);
        return regions;
    }

    private Component addRegionToMessage(String direction, int regionX, int regionZ, List<String> regions, Component message) {
        String regionName = "r." + regionX + "." + regionZ + ".mca";
        regions.add(regionName);
        return message.append(Component.newline())
                .append(Component.text("Nearby region to the " + direction + ": ", NamedTextColor.GRAY))
                .append(Component.text(regionName, NamedTextColor.DARK_GRAY));
    }

    public static void updateExtentsForImportedRegions(List<String> regionsToImport) {
        for (String regionName : regionsToImport) {
            String[] parts = regionName.split("\\.");
            int regionX = Integer.parseInt(parts[1]);
            int regionZ = Integer.parseInt(parts[2]);
            WorldFunctions.updateExtents(regionX, regionZ);
        }
    }

    public static void completeImport(Player player) {
        ongoingImports.computeIfPresent(player.getName(), (key, count) -> {
            if (count.decrementAndGet() <= 0) {
                return null;
            } else {
                return count;
            }
        });
    }

    public static Component getImportingPlayersWithCounts() {
        Component message = Component.empty();
        int totalImportingPlayers = (int) ongoingImports.values().stream().filter(count -> count.get() > 0).count();
        boolean first = true;

        for (var entry : ongoingImports.entrySet()) {
            if (entry.getValue().get() <= 0) continue;
            if (!first) message = message.append(Component.text(", "));
            first = false;

            int importCount = entry.getValue().get();
            String importText = importCount > 1 ? importCount + " imports" : "1 import";

            message = message.append(Component.text(entry.getKey()).color(NamedTextColor.YELLOW))
                    .append(Component.text(" (").color(NamedTextColor.YELLOW))
                    .append(Component.text(importText).color(NamedTextColor.RED))
                    .append(Component.text(")").color(NamedTextColor.YELLOW));

            if (message.children().size() >= 3) break;
        }

        if (totalImportingPlayers > 3) {
            int additionalPlayers = totalImportingPlayers - 3;
            message = message.append(Component.text(" (and " + additionalPlayers + " more)").color(NamedTextColor.GRAY));
        }

        return message;
    }
}