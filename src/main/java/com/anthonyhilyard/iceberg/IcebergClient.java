package com.anthonyhilyard.iceberg;

import net.minecraftforge.api.distmarker.Dist;
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
