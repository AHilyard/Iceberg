package com.anthonyhilyard.iceberg.events.common;

import com.anthonyhilyard.iceberg.events.ToggleableEvent;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.world.entity.player.Player;

/**
 * This event is fired when a player is granted an advancement criterion.
 */
public interface CriterionEvent
{
	ToggleableEvent<CriterionEvent> EVENT = ToggleableEvent.create(CriterionEvent.class,
		(listeners) -> (player, advancementHolder, criterionKey) -> {
			for (CriterionEvent listener : listeners)
			{
				listener.awardCriterion(player, advancementHolder, criterionKey);
			}
		}
	);

	public void awardCriterion(Player player, AdvancementHolder advancementHolder, String criterionKey);
}
