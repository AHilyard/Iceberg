package com.anthonyhilyard.iceberg.client;

import java.util.List;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.anthonyhilyard.iceberg.services.Services;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class IcebergClient
{
	public static void init()
	{
		TitleBreakComponent.registerFactory();
		RenderTooltipEvents.GATHER.register(IcebergClient::onGatherComponentsEventEnd);

		Services.getReloadListenerRegistrar().registerListener(() -> CustomItemRenderer.getInstance(), Identifier.fromNamespaceAndPath(Iceberg.MODID, "custom_item_renderer"));
	}

	public static GatherResult onGatherComponentsEventEnd(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		if (tooltipElements.size() > 1)
		{
			// Insert a title break component after the first formattedText component.
			for (int i = 0; i < tooltipElements.size(); i++)
			{
				if (tooltipElements.get(i).left().isPresent())
				{
					tooltipElements.add(i + 1, Either.<FormattedText, TooltipComponent>right(new TitleBreakComponent()));
					break;
				}
			}
		}

		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}
}
