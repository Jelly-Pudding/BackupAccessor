package com.jellypudding.backupAccessor.commands;

import com.jellypudding.backupAccessor.BackupAccessor;
import com.jellypudding.backupAccessor.util.WorldFunctions;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.HashMap;
import java.util.UUID;

public class TpCommand {

    private final BackupAccessor plugin;
    private static final HashMap<UUID, Location> positionBeforeTeleport = new HashMap<>();

    public TpCommand(BackupAccessor plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(Player player) {
        if (player.getWorld().getName().equals(WorldFunctions.BACKUP_WORLD_NAME)) {
            player.sendMessage(Component.text("You are already in the BackupAccessor world!").color(NamedTextColor.RED));
            return true;
        }

        if (WorldFunctions.doesBackupAccessorWorldExist()) {
            Component importingPlayersWithCounts = ImportCommand.getImportingPlayersWithCounts();
            if (!importingPlayersWithCounts.equals(Component.empty())) {
                player.sendMessage(Component.text("Teleportation to BackupAccessor is currently unavailable due to ongoing region imports by: ")
                        .color(NamedTextColor.RED)
                        .append(importingPlayersWithCounts));
                return true;
            }

            if (WorldFunctions.isBackupAccessorWorldLoaded()) {
                player.sendMessage(Component.text("Teleporting to BackupAccessor world...").color(NamedTextColor.GRAY));
                Location playerLocation = player.getLocation();
                positionBeforeTeleport.put(player.getUniqueId(), playerLocation);

                World backupWorld = Bukkit.getWorld(WorldFunctions.BACKUP_WORLD_NAME);
                Location newLocation = new Location(backupWorld, playerLocation.getX(), playerLocation.getY(), playerLocation.getZ(), playerLocation.getYaw(), playerLocation.getPitch());
                player.teleport(newLocation);
            } else {
                player.sendMessage(Component.text("The BackupAccessor world is not loaded! Try /backupaccessor create.").color(NamedTextColor.RED));
            }
        } else {
            player.sendMessage(Component.text("The BackupAccessor world does not exist! Try /backupaccessor create.").color(NamedTextColor.RED));
        }

        return true;
    }

    public static HashMap<UUID, Location> getPositionBeforeTeleport() {
        return positionBeforeTeleport;
    }
}