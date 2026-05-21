package com.anthonyhilyard.iceberg.neoforge.common;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

@EventBusSubscriber(modid = Iceberg.MODID)
public class IcebergNeoForgeCommon
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void levelLoadEvent(LevelEvent.Load event)
	{
		LevelEvents.LOAD.invoker().onLoad(event.getLevel());
	}

	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void levelUnloadEvent(LevelEvent.Unload event)
	{
		LevelEvents.UNLOAD.invoker().onUnload(event.getLevel());
	}
}
