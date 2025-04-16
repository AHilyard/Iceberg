package com.anthonyhilyard.iceberg.component;

import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public class TitleBreakComponent implements TooltipComponent, ClientTooltipComponent
{
	@Override
	public int getHeight() { return 0; }

	@Override
	public int getWidth(Font font) { return 0; }

	public static void registerFactory()
	{
		RegisterTooltipComponentFactoryEvent.EVENT.register(TitleBreakComponent.class, data -> {
			if (data instanceof TitleBreakComponent titleBreakComponent)
			{
				return titleBreakComponent;
			}
			return null;
		});
	}
}
