package com.anthonyhilyard.iceberg.forge.server;

import com.anthonyhilyard.iceberg.events.server.PlayerLoginEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class IcebergForgeServer
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void event(PlayerLoggedInEvent event)
	{
		ServerPlayer serverPlayer = (ServerPlayer)event.getEntity();
		PlayerLoginEvent.EVENT.invoker().playerLogin(serverPlayer, serverPlayer.getServer());
	}
}
