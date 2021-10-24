package com.anthonyhilyard.iceberg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class Loader implements ModInitializer
{
	public static final String MODID = "iceberg";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	@Override
	public void onInitialize()
	{
	}

	// Event testing.
	//
	// @Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
	// public static class AdvancementEvents
	// {
	// 	@SubscribeEvent
	// 	public static void onCriterion(final CriterionEvent event)
	// 	{
	// 		LOGGER.info("{} gained {} for {}!", event.getPlayer().getName().getString(), event.getCriterionKey(), event.getAdvancement().getId().toString());
	// 	}

	// 	@SubscribeEvent
	// 	public static void onFluidEntered(final EntityFluidEvent.Entered event)
	// 	{
	// 		LOGGER.info("{} entered {}!", event.getEntity().getName().getString(), event.getFluid().getRegistryName().toString());
	// 	}

	// 	@SubscribeEvent
	// 	public static void onFluidExited(final EntityFluidEvent.Exited event)
	// 	{
	// 		LOGGER.info("{} exited {}!", event.getEntity().getName().getString(), event.getFluid().getRegistryName().toString());
	// 	}
	// }

}