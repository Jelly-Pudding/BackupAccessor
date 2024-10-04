package com.jellypudding.backupAccessor.commands;

import com.jellypudding.backupAccessor.BackupAccessor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class TpbCommand {

    private final BackupAccessor plugin;

    public TpbCommand(BackupAccessor plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(Player player) {
        if (TpCommand.getPositionBeforeTeleport().containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Teleporting to original position...").color(NamedTextColor.GRAY));
            Location originalLocation = TpCommand.getPositionBeforeTeleport().get(player.getUniqueId());
            player.teleport(originalLocation);
            TpCommand.getPositionBeforeTeleport().remove(player.getUniqueId());
        } else {
            player.sendMessage(Component.text("It appears you did not use /backupaccessor tp. Teleporting you to the default world...").color(NamedTextColor.GRAY));
            Location playerLocation = player.getLocation();
            String defaultWorldName = Bukkit.getWorlds().get(0).getName(); // Get the default world name
            Location defaultLocation = new Location(Bukkit.getWorld(defaultWorldName), playerLocation.getX(), playerLocation.getY(), playerLocation.getZ(), playerLocation.getYaw(), playerLocation.getPitch());
            player.teleport(defaultLocation);
        }

        return true;
    }
}