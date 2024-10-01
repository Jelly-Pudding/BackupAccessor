package com.jellypudding.backupAccessor;

import com.jellypudding.backupAccessor.commands.ListCommand;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public final class BackupAccessor extends JavaPlugin {

    private static BackupAccessor instance;
    private ListCommand listCommand;
    private String worldPath;
    private List<String> worldBackupPaths;
    private List<String> playerBackupPaths;
    private boolean getUUIDFromMojangAPI;
    private String dbUsername;
    private String dbPassword;
    private String dbName;
    private String dbAddress;
    private String dbQuery;
    private String[] dummyUUIDs = new String[8];

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfigValues();
        this.listCommand = new ListCommand();

        // Unload and delete BackupAccessor world if it exists
        // Note: You'll need to implement this WorldFunctions class
        // WorldFunctions.unloadThenDeleteBackupAccessor(Bukkit.getConsoleSender());

        getLogger().info("BackupAccessor has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("BackupAccessor has been disabled.");
    }

    public static BackupAccessor getInstance() {
        return instance;
    }

    private void loadConfigValues() {
        FileConfiguration config = this.getConfig();
        worldPath = config.getString("worldPath", "/home");
        worldBackupPaths = config.getStringList("worldBackupPaths");
        playerBackupPaths = config.getStringList("playerBackupPaths");
        getUUIDFromMojangAPI = config.getBoolean("getUUIDFromMojangAPI", true);
        dbUsername = config.getString("dbUsername", "user");
        dbPassword = config.getString("dbPassword", "pass");
        dbName = config.getString("dbName", "luckperms");
        dbAddress = config.getString("dbAddress", "localhost");
        dbQuery = config.getString("dbQuery", "SELECT uuid FROM players WHERE username = ? AND uuid IS NOT NULL ORDER BY uuid DESC LIMIT 1");
        for (int i = 0; i < 8; i++) {
            dummyUUIDs[i] = config.getString("dummyUUID" + i, "00000000-0000-0000-0000-000000000000");
        }
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (command.getName().equalsIgnoreCase("backupaccessor")) {
            if (args.length == 0) {
                sender.sendMessage("Usage: /backupaccessor <subcommand> [args]");
                return true;
            }

            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "select":
                    return handleSelectCommand(sender, args);
                case "invsee":
                    return handleInvseeCommand(sender, args);
                case "ecsee":
                case "endersee":
                    return handleEcseeCommand(sender, args);
                case "list":
                    return handleListCommand(sender, args);
                case "import":
                    return handleImportCommand(sender, args);
                case "create":
                    return handleCreateCommand(sender, args);
                case "tp":
                    return handleTpCommand(sender, args);
                case "tpb":
                    return handleTpbCommand(sender, args);
                case "destroy":
                    return handleDestroyCommand(sender, args);
                case "reload":
                    return handleReloadCommand(sender, args);
                default:
                    sender.sendMessage("Unknown subcommand. Use /backupaccessor for help.");
                    return true;
            }
        }
        return false;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (command.getName().equalsIgnoreCase("backupaccessor")) {
            if (args.length == 1) {
                List<String> subCommands = List.of("select", "invsee", "ecsee", "list", "import", "create", "tp", "tpb", "destroy", "reload");
                return subCommands.stream()
                        .filter(sc -> sc.startsWith(args[0].toLowerCase()))
                        .toList();
            } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
                return listCommand.onTabComplete(sender, command, alias, args);
            }
        }
        return new ArrayList<>();
    }

    private boolean handleSelectCommand(CommandSender sender, String[] args) {
        // Implement select command logic
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Select command not implemented yet.");
        return true;
    }

    private boolean handleInvseeCommand(CommandSender sender, String[] args) {
        // Implement invsee command logic
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Invsee command not implemented yet.");
        return true;
    }

    private boolean handleEcseeCommand(CommandSender sender, String[] args) {
        // Implement ecsee command logic
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Ecsee command not implemented yet.");
        return true;
    }

    private boolean handleListCommand(CommandSender sender, String[] args) {
        return listCommand.onCommand(sender, null, "backupaccessor", args);
    }

    private boolean handleImportCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        // Implement import command logic
        sender.sendMessage("Import command not implemented yet.");
        return true;
    }

    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        // Implement create command logic
        sender.sendMessage("Create command not implemented yet.");
        return true;
    }

    private boolean handleTpCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        // Implement tp command logic
        sender.sendMessage("Tp command not implemented yet.");
        return true;
    }

    private boolean handleTpbCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        // Implement tpb command logic
        sender.sendMessage("Tpb command not implemented yet.");
        return true;
    }

    private boolean handleDestroyCommand(CommandSender sender, String[] args) {
        // Implement destroy command logic
        sender.sendMessage("Destroy command not implemented yet.");
        return true;
    }

    private boolean handleReloadCommand(CommandSender sender, String[] args) {
        reloadConfig();
        loadConfigValues();
        sender.sendMessage("BackupAccessor configuration reloaded.");
        return true;
    }

    // Getter methods for configuration values
    public String getWorldPath() { return worldPath; }
    public List<String> getWorldBackupPaths() { return worldBackupPaths; }
    public List<String> getPlayerBackupPaths() { return playerBackupPaths; }
    public boolean isGetUUIDFromMojangAPI() { return getUUIDFromMojangAPI; }
    public String getDbUsername() { return dbUsername; }
    public String getDbPassword() { return dbPassword; }
    public String getDbName() { return dbName; }
    public String getDbAddress() { return dbAddress; }
    public String getDbQuery() { return dbQuery; }
    public String getDummyUUID(int index) {
        if (index < 0 || index >= 8) throw new IllegalArgumentException("Index must be between 0 and 7");
        return dummyUUIDs[index];
    }
}