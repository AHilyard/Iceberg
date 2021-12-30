package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;

public class GatherComponentsExtEvent extends GatherComponents
{
	private final int index;
	public GatherComponentsExtEvent(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		super(itemStack, screenWidth, screenHeight, tooltipElements, maxWidth);
		this.index = index;
	}
	public int getIndex() { return index; }
}
