package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.function.Supplier;
import java.util.Map;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;
import com.google.common.collect.Maps;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;

@EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT)
public class NeoForgeReloadListenerRegistrar implements IReloadListenerRegistrar
{
	private static Map<Identifier, PreparableReloadListener> listeners = Maps.newHashMap();
	private static Map<Identifier, Supplier<PreparableReloadListener>> listenerSuppliers = Maps.newHashMap();

	@Override
	public void registerListener(PreparableReloadListener listener, Identifier listenerId)
	{
		listeners.put(listenerId, listener);
	}

	@Override
	public void registerListener(Supplier<PreparableReloadListener> listener, Identifier listenerId)
	{
		listenerSuppliers.put(listenerId, listener);
	}

	@SubscribeEvent
	public static void addListeners(AddClientReloadListenersEvent event)
	{
		for (Identifier listenerId : listeners.keySet())
		{
			event.addListener(listenerId, listeners.get(listenerId));
		}

		for (Identifier listenerId : listenerSuppliers.keySet())
		{
			event.addListener(listenerId, listenerSuppliers.get(listenerId).get());
		}
	}
}
