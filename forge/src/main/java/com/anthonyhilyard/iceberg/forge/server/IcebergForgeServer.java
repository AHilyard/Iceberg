package com.anthonyhilyard.iceberg.forge.server;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.events.server.PlayerLoginEvent;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.eventbus.api.listener.Priority;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Iceberg.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class IcebergForgeServer
{
	@SubscribeEvent(priority = Priority.HIGH)
	public static void event(PlayerLoggedInEvent event)
	{
		ServerPlayer serverPlayer = (ServerPlayer)event.getEntity();
		PlayerLoginEvent.EVENT.invoker().playerLogin(serverPlayer, serverPlayer.level().getServer());
	}
}
