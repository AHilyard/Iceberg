package com.anthonyhilyard.iceberg.events;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.world.entity.player.Player;

/**
 * This event is fired when a player is granted an advancement criterion.
 */
public interface CriterionCallback
{
	Event<CriterionCallback> EVENT = EventFactory.createArrayBacked(CriterionCallback.class,
		(listeners) -> (player, advancement, criterionKey) -> {
			for (CriterionCallback listener : listeners)
			{
				listener.awardCriterion(player, advancement, criterionKey);
			}
		}
	);

	public void awardCriterion(Player player, Advancement advancement, String criterionKey);
}
