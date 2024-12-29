package org.Nysxl.BountySystem.Bounties;

import java.util.UUID;

public record Bounty(UUID bountySetter, UUID target, double Amount) {
}
