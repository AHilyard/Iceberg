package com.anthonyhilyard.iceberg.network;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.events.NewItemPickupCallback;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class IcebergNetworkProtocol
{
	private static final String VERSION = "v1";

	private static final ResourceLocation SEND_ITEM_ID = new ResourceLocation(Loader.MODID, VERSION + '/' + String.valueOf(0));
	
	public static void sendItemPickupEvent(ServerPlayer player, ItemStack item)
	{
		Loader.LOGGER.info("Sending item pickup event message!");
		if (!player.level.isClientSide)
		{
			// Build buffer.
			FriendlyByteBuf buffer = PacketByteBufs.create();
			buffer.writeUUID(player.getUUID());
			buffer.writeItem(item);

			// Send packet.
			ServerPlayNetworking.send(player, SEND_ITEM_ID, buffer);
		}
	}

	public static void handleItemPickupEvent(Minecraft client, ClientPacketListener handler, FriendlyByteBuf buf, PacketSender responseSender)
	{
		Loader.LOGGER.info("receiving item pickup event message!");
		client.execute(() -> {
			NewItemPickupCallback.EVENT.invoker().onItemPickup(buf.readUUID(), buf.readItem());
		});
	}

	public static void registerHandlers()
	{
		ClientPlayNetworking.registerGlobalReceiver(SEND_ITEM_ID, IcebergNetworkProtocol::handleItemPickupEvent);
	}
}
