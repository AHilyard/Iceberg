package com.anthonyhilyard.iceberg.events.client;

import java.util.UUID;

import com.anthonyhilyard.iceberg.events.Event;
import com.anthonyhilyard.iceberg.events.EventFactory;

import net.minecraft.world.item.ItemStack;

/**
 * This event is fired right before a player picks up a new item.  This event fires on the logical client.
 */
public interface NewItemPickupEvent
{
	Event<NewItemPickupEvent> EVENT = EventFactory.create(NewItemPickupEvent.class,
		(listeners) -> (playerUUID, itemStack) -> {
			for (NewItemPickupEvent listener : listeners)
			{
				listener.onItemPickup(playerUUID, itemStack);
			}
		}
	);

	public void onItemPickup(UUID playerUUID, ItemStack itemStack);
}