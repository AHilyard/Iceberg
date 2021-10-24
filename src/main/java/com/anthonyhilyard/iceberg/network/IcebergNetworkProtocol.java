package com.anthonyhilyard.iceberg.network;

import com.anthonyhilyard.iceberg.Loader;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
// import net.minecraftforge.fmllegacy.network.NetworkRegistry;
// import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

public final class IcebergNetworkProtocol
{
	//private static final String NETWORK_PROTOCOL_VERSION = "1";
	private static final ResourceLocation IDENTIFIER = new ResourceLocation(Loader.MODID, "main");
	
	// public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
	// 		new ResourceLocation(Loader.MODID, "main"), () -> NETWORK_PROTOCOL_VERSION,
	// 		NETWORK_PROTOCOL_VERSION::equals, NETWORK_PROTOCOL_VERSION::equals
	// );

	// public static final void register()
	// {
	// 	int messageID = 0;

	// 	CHANNEL.registerMessage(
	// 		messageID++,
	// 		NewItemPickupEventPacket.class,
	// 		NewItemPickupEventPacket::encode,
	// 		NewItemPickupEventPacket::decode,
	// 		NewItemPickupEventPacket::handle
	// 	);
	// }

	public static void sendItemPickupEvent(ServerPlayer player, ItemStack item)
	{
		if (!player.level.isClientSide)
		{
			// Build buffer.
			FriendlyByteBuf buffer = PacketByteBufs.create();
			buffer.writeUUID(player.getUUID());
			buffer.writeItem(item);

			// Send packet.
			ServerPlayNetworking.send(player, IDENTIFIER, buffer);
		}
	}
}
