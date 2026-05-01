package com.anthonyhilyard.iceberg.neoforge.server;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.server.PlayerLoginEvent;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;

@EventBusSubscriber(modid = Iceberg.MODID)
public class IcebergNeoForgeServer
{
	@SubscribeEvent(priority = EventPriority.HIGH)
	public static void event(PlayerLoggedInEvent event)
	{
		ServerPlayer serverPlayer = (ServerPlayer)event.getEntity();
		PlayerLoginEvent.EVENT.invoker().playerLogin(serverPlayer, serverPlayer.level().getServer());
	}
}
