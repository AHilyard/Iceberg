package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.Map;

import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;
import com.google.common.collect.Maps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

public class NeoForgeReloadListenerRegistrar implements IReloadListenerRegistrar
{
	private static Map<ResourceLocation, PreparableReloadListener> listeners = Maps.newHashMap();

	@Override
	public void registerListener(PreparableReloadListener listener, ResourceLocation listenerId)
	{
		listeners.put(listenerId, listener);
	}

	@SubscribeEvent
	public static void addListeners(AddClientReloadListenersEvent event)
	{
		for (ResourceLocation listenerId : listeners.keySet())
		{
			event.addListener(listenerId, listeners.get(listenerId));
		}
	}
}
