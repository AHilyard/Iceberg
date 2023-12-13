package com.anthonyhilyard.iceberg.events;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.entity.player.Player;

/**
 * This event is fired when a player is granted an advancement criterion.
 */
public interface CriterionCallback
{
	ToggleableEvent<CriterionCallback> EVENT = ToggleableEvent.create(CriterionCallback.class,
		(listeners) -> (player, advancementHolder, criterionKey) -> {
			for (CriterionCallback listener : listeners)
			{
				listener.awardCriterion(player, advancementHolder, criterionKey);
			}
		}
	);

	public void awardCriterion(Player player, AdvancementHolder advancementHolder, String criterionKey);
}
