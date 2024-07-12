package com.anthonyhilyard.iceberg.neoforge.services;

import java.util.Set;

import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;
import com.google.common.collect.Sets;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;


public class NeoForgeReloadListenerRegistrar implements IReloadListenerRegistrar
{
	private static Set<PreparableReloadListener> listeners = Sets.newHashSet();

	@Override
	public void registerListener(PreparableReloadListener listener, ResourceLocation listenerId)
	{
		listeners.add(listener);
	}

	@SubscribeEvent
	public static void addListeners(RegisterClientReloadListenersEvent event)
	{
		for (PreparableReloadListener listener : listeners)
		{
			event.registerReloadListener(listener);
		}
	}
}
