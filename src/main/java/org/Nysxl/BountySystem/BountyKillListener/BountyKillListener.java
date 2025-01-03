package org.Nysxl.BountySystem.BountyKillListener;

import org.Nysxl.BountySystem.Bounties.Bounty;
import org.Nysxl.BountySystem.Bounties.BountyManager;
import org.Nysxl.BountySystem.NysxlBountySystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Optional;

public class BountyKillListener implements Listener {


    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity(); // Explicit cast

        Player killer = victim.getKiller();
        if (killer == null || !(killer instanceof Player)) return;

        List<Bounty> activeBounties = NysxlBountySystem.getBountyManager().getActiveBounties();

        if (NysxlBountySystem.getBountyManager().isClaimAllBountiesOnKill()) {
            // Filter and collect all bounties for the victim
            List<Bounty> claimedBounties = activeBounties.stream()
                    .filter(b -> b.target().equals(victim.getUniqueId()))
                    .toList();

            if (!claimedBounties.isEmpty()) {
                // Calculate the total bounty amount
                double totalBounty = claimedBounties.stream()
                        .mapToDouble(Bounty::Amount)
                        .sum();

                // Remove all claimed bounties from active bounties
                activeBounties.removeAll(claimedBounties);

                // Deposit the total bounty amount to the killer
                boolean success = NysxlBountySystem.getEconomy().depositBalance(killer, totalBounty);

                if (success) {
                    killer.sendMessage(ChatColor.GREEN + "You have collected a total of " + totalBounty + " from " +
                            claimedBounties.size() + " bounties for killing " + victim.getName() + "!");
                } else {
                    killer.sendMessage(ChatColor.RED + "Failed to deposit the total bounty reward.");
                }

                // Save updated bounties
                NysxlBountySystem.getBountyManager().saveBountiesToConfig();
            }
        } else {
            // Claim only the first bounty for the victim
            Optional<Bounty> firstBounty = activeBounties.stream()
                    .filter(b -> b.target().equals(victim.getUniqueId()))
                    .findFirst();

            firstBounty.ifPresent(bounty -> {
                activeBounties.remove(bounty);

                // Deposit the single bounty amount
                boolean success = NysxlBountySystem.getEconomy().depositBalance(killer, bounty.Amount());

                if (success) {
                    killer.sendMessage(ChatColor.GREEN + "You have collected a bounty of " + bounty.Amount() +
                            " for killing " + victim.getName() + "!");
                } else {
                    killer.sendMessage(ChatColor.RED + "Failed to deposit the bounty reward.");
                }

                // Save updated bounties
                NysxlBountySystem.getBountyManager().saveBountiesToConfig();
            });
        }
    }

    public void registerEvents(JavaPlugin plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }
}