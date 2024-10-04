package com.jellypudding.backupAccessor.util;

import com.jellypudding.backupAccessor.BackupAccessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.entity.Player;
import net.kyori.adventure.util.TriState;

import org.bukkit.command.CommandSender;

import java.io.File;

public class WorldFunctions {

    public static final String BACKUP_WORLD_NAME = "BackupAccessor";

    // Used for calculating the size of the world border.
    private static int minX = Integer.MAX_VALUE;
    private static int minZ = Integer.MAX_VALUE;
    private static int maxX = Integer.MIN_VALUE;
    private static int maxZ = Integer.MIN_VALUE;

    public static boolean doesBackupAccessorWorldExist() {
        return Bukkit.getWorld(BACKUP_WORLD_NAME) != null;
    }

    public static boolean isBackupAccessorWorldLoaded() {
        return Bukkit.getWorld(BACKUP_WORLD_NAME) != null;
    }

    public static void createBackupAccessorWorld(Player player) {
        if (doesBackupAccessorWorldExist()) {
            player.sendMessage(Component.text("BackupAccessor world already exists.").color(NamedTextColor.RED));
            loadBackupAccessorWorldAndAdjustBorder(player, true);
            return;
        }

        if (FileFunctions.isBackupAccessorEmpty()) {
            player.sendMessage(Component.text("BackupAccessor has no region files. Make sure you use /backupaccessor import first.").color(NamedTextColor.RED));
            return;
        }

        WorldCreator creator = new WorldCreator(BACKUP_WORLD_NAME);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(false);
        creator.generator(new ChunkGenerator() {
            // This creates a completely empty world
        });
        creator.generatorSettings("{\"structures\": {\"structures\": {}}, \"layers\": [{\"block\": \"air\", \"height\": 1}], \"biome\":\"the_void\"}");
        creator.keepSpawnLoaded(TriState.TRUE);
        creator.hardcore(false);

        World world = creator.createWorld();

        if (world != null) {
            world.setAutoSave(false);
            adjustWorldBorderForBackupAccessor(world);
            player.sendMessage(Component.text("Successfully created and loaded BackupAccessor world.").color(NamedTextColor.GREEN));
        } else {
            player.sendMessage(Component.text("Failed to create BackupAccessor world.").color(NamedTextColor.RED));
        }
    }

    public static void loadBackupAccessorWorldAndAdjustBorder(Player player, boolean verbose) {
        if (!doesBackupAccessorWorldExist()) {
            if (verbose) {
                player.sendMessage(Component.text("BackupAccessor world does not exist. Creating it now...").color(NamedTextColor.YELLOW));
            }
            createBackupAccessorWorld(player);
            return;
        }

        World world = Bukkit.getWorld(BACKUP_WORLD_NAME);
        if (world == null) {
            world = Bukkit.createWorld(new WorldCreator(BACKUP_WORLD_NAME));
        }

        if (world != null) {
            adjustWorldBorderForBackupAccessor(world);
            if (verbose) {
                player.sendMessage(Component.text("Successfully loaded BackupAccessor world.").color(NamedTextColor.GREEN));
            }
        } else {
            player.sendMessage(Component.text("Failed to load BackupAccessor world.").color(NamedTextColor.RED));
        }
    }


    public static void unloadThenDeleteBackupAccessor(CommandSender sender) {
        World world = Bukkit.getWorld(BACKUP_WORLD_NAME);
        if (world != null) {
            for (Player p : world.getPlayers()) {
                p.teleport(Bukkit.getWorlds().getFirst().getSpawnLocation());
            }
            if (Bukkit.unloadWorld(world, false)) {
                // World is unloaded, now we need to delete its files
                File worldFolder = world.getWorldFolder();
                if (FileFunctions.deleteDirectory(worldFolder)) {
                    sender.sendMessage(Component.text("Successfully deleted BackupAccessor world.").color(NamedTextColor.GREEN));
                } else {
                    sender.sendMessage(Component.text("Failed to delete BackupAccessor world files.").color(NamedTextColor.RED));
                }
            } else {
                sender.sendMessage(Component.text("Failed to unload BackupAccessor world.").color(NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("BackupAccessor world does not exist.").color(NamedTextColor.YELLOW));
        }
    }

    public static void updateExtents(int regionX, int regionZ) {
        int regionMinX = regionX * 512;
        int regionMaxX = regionMinX + 511;
        int regionMinZ = regionZ * 512;
        int regionMaxZ = regionMinZ + 511;

        minX = Math.min(minX, regionMinX);
        maxX = Math.max(maxX, regionMaxX);
        minZ = Math.min(minZ, regionMinZ);
        maxZ = Math.max(maxZ, regionMaxZ);
    }

    private static void adjustWorldBorderForBackupAccessor(World world) {
        // Add 1 to centre the border on the block.
        int centerX = (minX + maxX + 1) / 2;
        int centerZ = (minZ + maxZ + 1) / 2;

        // Add 1 to ensure the border encloses the last block.
        int diameterX = maxX - minX + 1;
        int diameterZ = maxZ - minZ + 1;
        int diameter = Math.max(diameterX, diameterZ);

        world.getWorldBorder().setCenter(centerX, centerZ);
        world.getWorldBorder().setSize(diameter);
    }
}