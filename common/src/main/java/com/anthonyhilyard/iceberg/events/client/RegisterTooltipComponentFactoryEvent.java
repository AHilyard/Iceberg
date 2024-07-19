package com.anthonyhilyard.iceberg.events.client;

import com.anthonyhilyard.iceberg.events.EventFactory;
import com.anthonyhilyard.iceberg.events.TypeTrackedEvent;

import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public interface RegisterTooltipComponentFactoryEvent
{
	TypeTrackedEvent<TooltipComponent, RegisterTooltipComponentFactoryEvent> EVENT = EventFactory.createTypeTracked(RegisterTooltipComponentFactoryEvent.class,
		callbacks -> (data) -> {
		for (RegisterTooltipComponentFactoryEvent callback : callbacks)
		{
			ClientTooltipComponent component = callback.getComponent(data);

			if (component != null)
			{
				return component;
			}
		}

		return null;
	});

	/**
	 * Return the tooltip component for the passed data, or null if none is available.
	 */
	public ClientTooltipComponent getComponent(TooltipComponent data);
}
