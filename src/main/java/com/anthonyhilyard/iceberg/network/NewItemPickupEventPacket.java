package com.anthonyhilyard.iceberg.network;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

import com.anthonyhilyard.iceberg.events.NewItemPickupEvent;


public final class NewItemPickupEventPacket
{
	private final UUID playerUUID;
	private final ItemStack item;

	public NewItemPickupEventPacket(final UUID playerUUID, final ItemStack item)
	{
		this.playerUUID = playerUUID;
		this.item = item;
	}

	public static void encode(final NewItemPickupEventPacket msg, final PacketBuffer packetBuffer)
	{
		packetBuffer.writeUUID(msg.playerUUID);
		packetBuffer.writeItem(msg.item);
	}

	public static NewItemPickupEventPacket decode(final PacketBuffer packetBuffer)
	{
		return new NewItemPickupEventPacket(packetBuffer.readUUID(), packetBuffer.readItem());
	}

	public static void handle(final NewItemPickupEventPacket msg, final Supplier<NetworkEvent.Context> contextSupplier)
	{
		final NetworkEvent.Context context = contextSupplier.get();
		context.enqueueWork(() -> {
			MinecraftForge.EVENT_BUS.post(new NewItemPickupEvent(msg.playerUUID, msg.item));
		});
		context.setPacketHandled(true);
	}

}