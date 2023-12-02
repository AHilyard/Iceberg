package com.anthonyhilyard.iceberg;

import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.mojang.datafixers.util.Either;

import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent.GatherComponents;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.FORGE, value = Dist.CLIENT)
public class IcebergClient
{
	public IcebergClient()
	{
	}

	public void onClientSetup(FMLClientSetupEvent event)
	{
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public static void onGatherComponentsEventEnd(GatherComponents event)
	{
		if (event.getTooltipElements().size() > 1)
		{
			// Insert a title break component after the first formattedText component.
			for (int i = 0; i < event.getTooltipElements().size(); i++)
			{
				if (event.getTooltipElements().get(i).left().isPresent())
				{
					event.getTooltipElements().add(i + 1, Either.<FormattedText, TooltipComponent>right(new TitleBreakComponent()));
					break;
				}
			}
		}
	}

	// @SubscribeEvent
	// public static void onTooltipPre(RenderTooltipEvent.Pre event)
	// {
	// 	Loader.LOGGER.info("tooltip pre");
	// }

	// @SubscribeEvent
	// public static void onTooltipColor(RenderTooltipEvent.Color event)
	// {
	// 	Loader.LOGGER.info("tooltip color");
	// }

	// @SubscribeEvent
	// public static void onTooltipPost(RenderTooltipExtEvent.Post event)
	// {
	// 	Loader.LOGGER.info("tooltip post");
	// }
}
