package com.anthonyhilyard.iceberg.events.client;

import java.util.List;

import com.anthonyhilyard.iceberg.events.Event;
import com.anthonyhilyard.iceberg.events.EventFactory;

import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item.TooltipContext;
import net.minecraft.world.item.ItemStack;

/**
 * This event is fired right before an item tooltip is rendered.  This event fires on the logical client.
 */
public interface ItemTooltipEvent
{
	Event<ItemTooltipEvent> EVENT = EventFactory.create(ItemTooltipEvent.class,
		(listeners) -> (itemStack, context, flag, lines) -> {
			for (ItemTooltipEvent listener : listeners)
			{
				listener.onItemTooltip(itemStack, context, flag, lines);
			}
		}
	);

	public void onItemTooltip(ItemStack itemStack, TooltipContext context, TooltipFlag flag, List<Component> lines);
}
