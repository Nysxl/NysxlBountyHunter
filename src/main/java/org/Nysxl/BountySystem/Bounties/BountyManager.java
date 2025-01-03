package org.Nysxl.BountySystem.Bounties;

import org.Nysxl.BountySystem.NysxlBountySystem;
import org.Nysxl.InventoryManager.DynamicButton;
import org.Nysxl.InventoryManager.DynamicPagedInventoryHandler;
import org.Nysxl.NysxlServerUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyManager {

    private static final long SAVE_DELAY_TICKS = 200L; // 60 seconds in ticks
    private final List<Bounty> activeBounties = new ArrayList<>();
    private double minBounty;
    private boolean claimAllBountiesOnKill;

    private BukkitTask saveTask = null;

    public BountyManager() {
        loadConfigs();
        loadClaimAllBountiesOnKillConfig();
    }

    private void loadClaimAllBountiesOnKillConfig() {
        this.claimAllBountiesOnKill = NysxlBountySystem.getConfigManager().getConfig("config").getBoolean("bounty.claimAllBountiesOnKill", false);
    }

    public void openActiveBounties(Player player) {
        DynamicPagedInventoryHandler inventoryHandler = new DynamicPagedInventoryHandler(
                "Active Bounties",
                54,
                null,
                NysxlBountySystem.getInstance()
        );

        for (Bounty bounty : activeBounties) {
            UUID targetUUID = bounty.target();
            Player targetPlayer = Bukkit.getPlayer(targetUUID);
            String targetName = (targetPlayer != null) ? targetPlayer.getName() : "Unknown";
            double bountyAmount = bounty.Amount();

            DynamicButton bountyButton = DynamicButton.builder()
                    .material(Material.PLAYER_HEAD)
                    .displayName(ChatColor.YELLOW + targetName + ": " + ChatColor.GOLD + bountyAmount + "$")
                    .headOwner(targetUUID)
                    .build();

            inventoryHandler.addButton(bountyButton, player);
        }

        inventoryHandler.open(player);
    }

    public void setBounty(Player player, Player target, double bounty) {
        if(player.equals(target)){
            player.sendMessage(ChatColor.RED + "You can't place a bounty on yourself.");
            return;
        }

        if (bounty <= 0) {
            player.sendMessage(ChatColor.RED + "Bounty amount must be greater than 0.");
            return;
        }

        if (bounty < minBounty) {
            player.sendMessage(ChatColor.RED + "Bounty needs to be " + minBounty + "$ or higher.");
            return;
        }

        if (!NysxlServerUtils.getEconomyManager().withdrawBalance(player, bounty)) {
            player.sendMessage(ChatColor.RED + "Insufficient balance.");
            return;
        }

        double taxed = bounty * NysxlServerUtils.getEconomyManager().getTaxRate();
        double bountyAfterTax = bounty - taxed;

        NysxlServerUtils.getEconomyManager().addToAvailableTaxes(taxed);

        Bounty newBounty = new Bounty(player.getUniqueId(), target.getUniqueId(), bountyAfterTax);
        activeBounties.add(newBounty);

        addToSaveQueue(newBounty);

        player.sendMessage(ChatColor.GREEN + "Bounty of " + bountyAfterTax + " placed on " + target.getName() + "!");
        target.sendMessage(ChatColor.RED + "A bounty has been placed on you for " + bountyAfterTax + "!");
    }

    private void addToSaveQueue(Bounty bounty) {
        if (saveTask != null) {
            saveTask.cancel();
        }

        saveTask = Bukkit.getScheduler().runTaskLater(NysxlBountySystem.getInstance(), this::saveBountiesToConfig, SAVE_DELAY_TICKS);
    }

    public void saveBountiesToConfig() {
        NysxlBountySystem.getConfigManager().saveObjects(
                "config",
                "bounty.activeBounties",
                activeBounties,
                bounty -> {
                    var map = new java.util.HashMap<String, Object>();
                    map.put("setter", bounty.bountySetter().toString());
                    map.put("target", bounty.target().toString());
                    map.put("amount", bounty.Amount());
                    return map;
                }
        );
    }

    private List<Bounty> loadBountiesFromConfig() {
        return NysxlBountySystem.getConfigManager().loadObjects(
                "config",
                "bounty.activeBounties",
                map -> new Bounty(
                        UUID.fromString((String) map.get("setter")),
                        UUID.fromString((String) map.get("target")),
                        (double) map.get("amount")
                )
        );
    }

    public void loadConfigs() {
        activeBounties.clear();
        activeBounties.addAll(loadBountiesFromConfig());
    }

    public List<Bounty> getActiveBounties() {
        return activeBounties;
    }

    public boolean isClaimAllBountiesOnKill() {
        return claimAllBountiesOnKill;
    }

    public void setClaimAllBountiesOnKill(boolean claimAllBountiesOnKill) {
        this.claimAllBountiesOnKill = claimAllBountiesOnKill;
    }

    public double getMinBounty() {
        return minBounty;
    }

    public void setMinBounty(double minBounty) {
        this.minBounty = minBounty;
    }
}