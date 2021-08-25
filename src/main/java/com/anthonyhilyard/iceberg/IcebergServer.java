package com.anthonyhilyard.iceberg;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD, value = Dist.DEDICATED_SERVER)
public class IcebergServer
{
	public IcebergServer()
	{
	}

	public void onServerStarting(FMLServerStartingEvent event)
	{

	}
}
