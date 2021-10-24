package com.anthonyhilyard.iceberg.events;

import java.util.UUID;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;

/**
 * This event is fired right before a player picks up a new item.  This event fires on the logical client.
 */
public interface NewItemPickupCallback
{
	Event<NewItemPickupCallback> EVENT = EventFactory.createArrayBacked(NewItemPickupCallback.class,
		(listeners) -> (playerUUID, itemStack) -> {
			for (NewItemPickupCallback listener : listeners)
			{
				listener.onItemPickup(playerUUID, itemStack);
			}
		}
	);

	public void onItemPickup(UUID playerUUID, ItemStack itemStack);
}