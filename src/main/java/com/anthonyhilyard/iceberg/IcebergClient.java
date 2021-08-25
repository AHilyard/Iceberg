package com.anthonyhilyard.iceberg;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Loader.MODID, bus = Bus.MOD, value = Dist.CLIENT)
public class IcebergClient
{
	public IcebergClient()
	{
	}

	public void onClientSetup(FMLClientSetupEvent event)
	{
		event.enqueueWork(new Runnable()
		{
			@Override
			public void run()
			{

			}
		});
	}
}
