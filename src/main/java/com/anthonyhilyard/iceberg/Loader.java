package com.anthonyhilyard.iceberg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Loader.MODID)
public class Loader
{
	public static final String MODID = "iceberg";
	public static final Logger LOGGER = LogManager.getLogger(MODID);

	public Loader()
	{
		if (FMLEnvironment.dist == Dist.CLIENT)
		{
			IcebergClient mod = new IcebergClient();
			FMLJavaModLoadingContext.get().getModEventBus().addListener(mod::onClientSetup);
		}
		else
		{
			new IcebergServer();
		}

		ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "ANY", (remote, isServer) -> true));
	}

	@SubscribeEvent
	public void onCommonSetup(FMLCommonSetupEvent event)
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
