package com.anthonyhilyard.iceberg.services;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public interface IReloadListenerRegistrar
{
	void registerListener(PreparableReloadListener listener, ResourceLocation listenerId);
}
