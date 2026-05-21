package com.anthonyhilyard.iceberg.forge.common;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Iceberg.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IcebergForgeCommon
{
	@SubscribeEvent(priority = Priority.HIGH)
	public static void levelLoadEvent(LevelEvent.Load event)
	{
		LevelEvents.LOAD.invoker().onLoad(event.getLevel());
	}

	@SubscribeEvent(priority = Priority.HIGH)
	public static void levelUnloadEvent(LevelEvent.Unload event)
	{
		LevelEvents.UNLOAD.invoker().onUnload(event.getLevel());
	}
}
