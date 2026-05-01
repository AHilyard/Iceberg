package com.anthonyhilyard.iceberg.services;

import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

public interface IReloadListenerRegistrar
{
	void registerListener(PreparableReloadListener listener, Identifier listenerId);
	void registerListener(Supplier<PreparableReloadListener> listener, Identifier listenerId);
}
