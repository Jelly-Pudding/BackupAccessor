# BackupAccessor: Access Backups In-game

### Features

* Select which backups you wish to use while in-game.
* Access backup regions.
* Access backup player inventories. This includes their armor and their off-hand slot.

### Instructions

Once you run the server with the `BackupAccessor.jar` file, a configuration file will be created in the `/config` folder. Edit the configuration file to include the absolute paths to your backup directories as well as your world directory, and ensure you only include forward slashes (/). While in-game, you can then do `/BackupAccessor reload` and run the commands found below.

### Commands

#### `/BackupAccessor list <type>`
Lists available backups. Type can be either 'World' or 'Player'.

#### `/BackupAccessor select <type> <backup>`
Select which backup to use. Type can be either 'World' or 'Player'.

#### `/BackupAccessor invsee <player>`
Opens up the player's inventory from the selected player backup. This includes their armor and their off-hand slot.

#### `/BackupAccessor ecsee <player>`
Opens up the player's ender chest from the selected player backup.

#### `/BackupAccessor import`
The backup region file corresponding to the one you are standing in gets imported into BackupAccessor.

#### `/BackupAccessor create`
Creates and loads the BackupAccessor world.

#### `/BackupAccessor tp`
Teleports you to BackupAccessor.

#### `/BackupAccessor tpb`
Teleports you back to your original position.

#### `/BackupAccessor reload`
Reloads the BackupAccessor configuration file.

#### `/BackupAccessor destroy`
The BackupAccessor world is unloaded and its files are deleted.

### Support Me
Donations to my [patreon](https://www.patreon.com/lolwhatyesme) will help with the development of this plugin.
