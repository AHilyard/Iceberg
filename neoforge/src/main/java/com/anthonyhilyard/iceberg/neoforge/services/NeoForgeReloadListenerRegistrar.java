package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.function.Supplier;
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
	private static Map<ResourceLocation, Supplier<PreparableReloadListener>> listenerSuppliers = Maps.newHashMap();

	@Override
	public void registerListener(PreparableReloadListener listener, ResourceLocation listenerId)
	{
		listeners.put(listenerId, listener);
	}

	@Override
	public void registerListener(Supplier<PreparableReloadListener> listener, ResourceLocation listenerId)
	{
		listenerSuppliers.put(listenerId, listener);
	}

	@SubscribeEvent
	public static void addListeners(AddClientReloadListenersEvent event)
	{
		for (ResourceLocation listenerId : listeners.keySet())
		{
			event.addListener(listenerId, listeners.get(listenerId));
		}

		for (ResourceLocation listenerId : listenerSuppliers.keySet())
		{
			event.addListener(listenerId, listenerSuppliers.get(listenerId).get());
		}
	}
}
