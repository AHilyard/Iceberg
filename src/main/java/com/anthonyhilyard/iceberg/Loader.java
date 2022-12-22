package com.anthonyhilyard.iceberg;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
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
			TitleBreakComponent.registerFactory();
			IcebergClient mod = new IcebergClient();
			MinecraftForge.EVENT_BUS.register(IcebergClient.class);
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
}
