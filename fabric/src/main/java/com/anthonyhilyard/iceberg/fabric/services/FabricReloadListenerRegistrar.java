package com.anthonyhilyard.iceberg.fabric.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class FabricReloadListenerRegistrar implements IReloadListenerRegistrar
{
	@Override
	public void registerListener(PreparableReloadListener listener, Identifier listenerId)
	{
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener()
		{
			@Override
			public CompletableFuture<Void> reload(SharedState sharedState,
				  Executor executor, PreparationBarrier preparationBarrier, Executor executor2)
			{
				return listener.reload(sharedState, executor, preparationBarrier, executor2);
			}

			@Override
			public Identifier getFabricId() { return listenerId; }

			@Override
			public String getName() { return listener.getName(); }
		});
	}

	@Override
	public void registerListener(Supplier<PreparableReloadListener> listener, Identifier listenerId)
	{
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener()
		{
			@Override
			public CompletableFuture<Void> reload(SharedState sharedState,
				  Executor executor, PreparationBarrier preparationBarrier, Executor executor2)
			{
				return listener.get().reload(sharedState, executor, preparationBarrier, executor2);
			}

			@Override
			public Identifier getFabricId() { return listenerId; }

			@Override
			public String getName() { return listener.get().getName(); }
		});
	}
}
