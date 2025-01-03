package org.Nysxl.BountySystem;

import org.Nysxl.BountySystem.Bounties.BountyManager;
import org.Nysxl.BountySystem.BountyKillListener.BountyKillListener;
import org.Nysxl.CommandManager.CommandBuilder;
import org.Nysxl.CommandManager.SubCommandBuilder;
import org.Nysxl.DynamicConfigManager.DynamicConfigManager;
import org.Nysxl.NysxlServerUtils;
import org.Nysxl.Utils.Economy.EconomyManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Collectors;

public class NysxlBountySystem extends JavaPlugin {

    private static DynamicConfigManager configManager;
    private static NysxlBountySystem instance;
    private static EconomyManager economy;
    private static BountyManager bountyManager;

    @Override
    public void onEnable() {
        instance = this;

        registerConfigManager();

        bountyManager = new BountyManager();
        bountyManager.loadConfigs();

        checkEconomy();

        economy = NysxlServerUtils.getEconomyManager();


        BountyKillListener bountyKillListener = new BountyKillListener();
        getServer().getPluginManager().registerEvents(bountyKillListener, this);

        registerCommands();
    }

    public void registerCommands() {
        CommandBuilder bountyCommand = new CommandBuilder(this)
                .setName("bounty")
                .setUsageMessage("Usage: /bounty open or /bounty set <player> <amount>");

        // Set bounty command
        SubCommandBuilder setBountyCommand = new SubCommandBuilder(bountyCommand)
                .setName("set")
                .addPermission("bounty.set")
                .usage(0, String.class, () -> Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .collect(Collectors.toList()))
                .usage(1, Integer.class, List.of("<amount>")) // Assuming amounts are integers
                .setPlayerExecutor((player, context) -> {
                    List<String> args = context.getArgs();
                    if (args.size() < 2) {
                        player.sendMessage("Usage: /bounty set <playername> <amount>");
                        return;
                    }
                    Player targetPlayer = Bukkit.getPlayer(args.get(0));

                    if(targetPlayer == null){
                        player.sendMessage("Player not found.");
                        return;
                    }

                    String amountStr = args.get(1);
                    try {
                        int amount = Integer.parseInt(amountStr);
                        bountyManager.setBounty(player, targetPlayer, amount);
                    } catch (NumberFormatException e) {
                        player.sendMessage("Amount must be a valid number.");
                    }
                })
                .setConsoleExecutor((sender, context) -> {
                    sender.sendMessage("This command can only be executed by a player.");
                });

        // Open bounty command
        SubCommandBuilder openBountyCommand = new SubCommandBuilder(bountyCommand)
                .setName("open")
                .addPermission("bounty.open")
                .setPlayerExecutor((player, context) -> {
                    bountyManager.openActiveBounties(player);
                    player.sendMessage("Opening bounty GUI...");
                });

        // Register the main command with the plugin
        bountyCommand
                .addSubCommand(openBountyCommand)
                .addSubCommand(setBountyCommand)
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

    public static NysxlBountySystem getInstance() {
        return instance;
    }

    public static BountyManager getBountyManager() {
        return bountyManager;
    }
}