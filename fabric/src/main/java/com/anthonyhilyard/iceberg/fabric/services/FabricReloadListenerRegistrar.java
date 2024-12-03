package com.anthonyhilyard.iceberg.fabric.services;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import com.anthonyhilyard.iceberg.services.IReloadListenerRegistrar;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;

public class FabricReloadListenerRegistrar implements IReloadListenerRegistrar
{
	@Override
	public void registerListener(PreparableReloadListener listener, ResourceLocation listenerId)
	{
		ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new IdentifiableResourceReloadListener()
		{
			@Override
			public CompletableFuture<Void> reload(PreparationBarrier preparationBarrier,
					ResourceManager resourceManager, Executor executor, Executor executor2)
			{
				return listener.reload(preparationBarrier, resourceManager, executor, executor2);
			}

			@Override
			public ResourceLocation getFabricId() { return listenerId; }

			@Override
			public String getName() { return listener.getName(); }
		});
	}
}
