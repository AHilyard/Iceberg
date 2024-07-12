package com.anthonyhilyard.iceberg.events.server;

import com.anthonyhilyard.iceberg.events.ToggleableEvent;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * This event is fired when a player logs into the server.
 */
public interface PlayerLoginEvent
{
	ToggleableEvent<PlayerLoginEvent> EVENT = ToggleableEvent.create(PlayerLoginEvent.class,
		(listeners) -> (serverPlayer, server) -> {
			for (PlayerLoginEvent listener : listeners)
			{
				listener.playerLogin(serverPlayer, server);
			}
		}
	);

	public void playerLogin(ServerPlayer serverPlayer, MinecraftServer server);
}
