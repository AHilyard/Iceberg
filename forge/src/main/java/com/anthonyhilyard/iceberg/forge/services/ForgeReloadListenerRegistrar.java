package com.anthonyhilyard.iceberg.forge.services;

import java.util.function.Supplier;
import java.util.Set;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;
import com.google.common.collect.Sets;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Iceberg.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeReloadListenerRegistrar implements IReloadListenerRegistrar
{
	private static Set<PreparableReloadListener> listeners = Sets.newHashSet();
	private static Set<Supplier<PreparableReloadListener>> listenerSuppliers = Sets.newHashSet();

	@Override
	public void registerListener(PreparableReloadListener listener, Identifier listenerId)
	{
		listeners.add(listener);
	}

	@Override
	public void registerListener(Supplier<PreparableReloadListener> listener, Identifier listenerId)
	{
		listenerSuppliers.add(listener);
	}

	@SubscribeEvent
	public static void addListeners(RegisterClientReloadListenersEvent event)
	{
		for (PreparableReloadListener listener : listeners)
		{
			event.registerReloadListener(listener);
		}

		for (Supplier<PreparableReloadListener> listener : listenerSuppliers)
		{
			event.registerReloadListener(listener.get());
		}
	}
}
