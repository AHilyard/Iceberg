package com.anthonyhilyard.iceberg.fabric.server;

import com.anthonyhilyard.iceberg.events.server.PlayerLoginEvent;
import com.anthonyhilyard.iceberg.fabric.config.ConfigTracker;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;

public final class IcebergFabricServer implements DedicatedServerModInitializer
{
	@Override
	public void onInitializeServer()
	{
		// Load configs.
		ConfigTracker.INSTANCE.loadConfigs(FabricLoader.getInstance().getConfigDir());
		
		// Register all server events.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { PlayerLoginEvent.EVENT.invoker().playerLogin(handler.player, server); });
	}
}
