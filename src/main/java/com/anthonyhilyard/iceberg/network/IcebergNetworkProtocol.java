package com.anthonyhilyard.iceberg.network;

import com.anthonyhilyard.iceberg.Loader;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.simple.SimpleChannel;

public class IcebergNetworkProtocol
{
	private static final String NETWORK_PROTOCOL_VERSION = "1";

	public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(Loader.MODID, "main"), () -> NETWORK_PROTOCOL_VERSION,
			NETWORK_PROTOCOL_VERSION::equals, NETWORK_PROTOCOL_VERSION::equals
	);

	public static final void register()
	{
		int messageID = 0;

		CHANNEL.registerMessage(
			messageID++,
			NewItemPickupEventPacket.class,
			NewItemPickupEventPacket::encode,
			NewItemPickupEventPacket::decode,
			NewItemPickupEventPacket::handle
		);
	}
}
