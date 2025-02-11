package com.anthonyhilyard.iceberg.forge.common;

import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class IcebergForgeCommon
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
