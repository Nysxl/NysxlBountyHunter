package org.Nysxl.BountySystem;

import org.Nysxl.BountySystem.Bounties.BountyManager;
import org.Nysxl.BountySystem.BountyKillListener.BountyKillListener;
import org.Nysxl.CommandManager.CommandRegistry;
import org.Nysxl.InventoryManager.DynamicConfigManager;
import org.Nysxl.NysxlServerUtils;
import org.Nysxl.Utils.Economy.EconomyManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NysxlBountySystem extends JavaPlugin {

    private CommandRegistry commandRegistry;
    private static DynamicConfigManager configManager;
    private static NysxlBountySystem instance;
    private static EconomyManager economy;
    private BountyManager bountyManager;

    @Override
    public void onEnable() {
        instance = this;

        registerConfigManager();
        checkEconomy();

        commandRegistry = new CommandRegistry(this);
        registerBountyKillEvent();

        economy = NysxlServerUtils.getEconomyManager();

        registerCommands();

        bountyManager = new BountyManager();
        bountyManager.loadConfigs();
    }

    private void registerCommands() {
        registerBountyCommand();
        registerBountyMenuCommand();
    }

    private void registerBountyCommand() {
        commandRegistry.createCommand("setBounty")
                .requirePlayer()
                .check(sender -> sender instanceof Player, "Only players can use this command.")
                .checkWithArgs((sender, args) -> {
                    if (args == null || args.length < 2) {
                        throw new IllegalArgumentException("Usage: /setBounty <player> <amount>");
                    }
                    Player player = (Player) sender;
                    Player target = player.getServer().getPlayer(args[0]);
                    if (target == null || target.equals(player)) {
                        throw new IllegalArgumentException("You cannot place a bounty on yourself or a non-existent player.");
                    }
                }, "Invalid arguments.")
                .onFallback((sender, partial) -> {
                    sender.sendMessage(ChatColor.RED + "Usage: /setBounty <player> <amount>");
                })
                .onExecute((sender, args) -> {
                    Player player = (Player) sender;
                    double bountyAmount;
                    try {
                        bountyAmount = Double.parseDouble(args[1]);
                    } catch (NumberFormatException e) {
                        player.sendMessage(ChatColor.RED + "Invalid bounty amount. Please enter a valid number.");
                        return;
                    }

                    Player target = player.getServer().getPlayer(args[0]);
                    bountyManager.setBounty(player, target, bountyAmount);
                })
                .register();
    }

    private void registerBountyMenuCommand() {
        commandRegistry.createCommand("activeBounties")
                .requirePlayer()
                .check(sender -> sender instanceof Player, "Only players can use this command.")
                .onFallback((sender, partial) -> {
                    sender.sendMessage(ChatColor.RED + "Usage: /activeBounties");
                })
                .onExecute((sender, args) -> {
                    Player player = (Player) sender;
                    bountyManager.openActiveBounties(player);
                })
                .register();
    }

    private void registerConfigManager() {
        configManager = new DynamicConfigManager(this);
        configManager.loadOrCreateDefaultConfig("config");
    }

    public static DynamicConfigManager getConfigManager() {
        return configManager;
    }

    private void checkEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getServer().getPluginManager().disablePlugin(this);
            getLogger().severe("Vault not found. Disabling plugin.");
        }
    }

    public static EconomyManager getEconomy() {
        return NysxlServerUtils.getEconomyManager();
    }

    private void registerBountyKillEvent() {
        new BountyKillListener(bountyManager).registerEvents(this);
    }

    public static NysxlBountySystem getInstance() {
        return instance;
    }
}