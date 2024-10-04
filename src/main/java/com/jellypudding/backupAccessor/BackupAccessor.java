package com.jellypudding.backupAccessor;

import com.jellypudding.backupAccessor.commands.ListCommand;
import com.jellypudding.backupAccessor.commands.SelectCommand;
import com.jellypudding.backupAccessor.commands.ImportCommand;
import com.jellypudding.backupAccessor.util.WorldFunctions;
import com.jellypudding.backupAccessor.util.BackupType;

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
    private SelectCommand selectCommand;
    private ImportCommand importCommand;
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
        this.selectCommand = new SelectCommand(this);
        this.importCommand = new ImportCommand(this);

        WorldFunctions.unloadThenDeleteBackupAccessor(Bukkit.getConsoleSender());

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
                    return handleSelectCommand(sender, command, label, args);
                case "invsee":
                    return handleInvseeCommand(sender, args);
                case "ecsee":
                case "endersee":
                    return handleEcseeCommand(sender, args);
                case "list":
                    return handleListCommand(sender, command, label, args);
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
                        .collect(java.util.stream.Collectors.toList());
            } else if (args.length >= 2) {
                switch (args[0].toLowerCase()) {
                    case "list":
                        return listCommand.onTabComplete(sender, command, alias, args);
                    case "select":
                        return selectCommand.onTabComplete(sender, command, alias, args);
                    // ... (other cases for future commands)
                }
            }
        }
        return new ArrayList<>();
    }

    private boolean handleSelectCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        return selectCommand.onCommand(sender, command, label, args);
    }

    private boolean handleInvseeCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Invsee command not implemented yet.");
        return true;
    }

    private boolean handleEcseeCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Ecsee command not implemented yet.");
        return true;
    }

    private boolean handleListCommand(CommandSender sender, Command command, String label, String[] args) {
        return listCommand.onCommand(sender, command, label, args);
    }

    private boolean handleImportCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        return importCommand.onCommand((Player) sender, args);
    }

    private boolean handleCreateCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        WorldFunctions.createBackupAccessorWorld((Player) sender);
        return true;
    }

    private boolean handleTpCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Tp command not implemented yet.");
        return true;
    }

    private boolean handleTpbCommand(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player to use this command!");
            return true;
        }
        sender.sendMessage("Tpb command not implemented yet.");
        return true;
    }

    private boolean handleDestroyCommand(CommandSender sender, String[] args) {
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