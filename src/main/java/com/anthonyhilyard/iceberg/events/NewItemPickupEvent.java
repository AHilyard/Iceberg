package com.anthonyhilyard.iceberg.events;

import net.minecraftforge.event.entity.player.PlayerEvent;

import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;

/**
 * This event is fired right before a player picks up a new item.  Unlike EntityItemPickupEvent, this event fires on the logical client.
 * <br>
 * This event is not {@link net.minecraftforge.eventbus.api.Cancelable}.<br>
 * <br>
 * This event does not have a result. {@link HasResult}<br>
 * <br>
 * This event is fired on the {@link MinecraftForge#EVENT_BUS}.
 */
public class NewItemPickupEvent extends PlayerEvent
{
	private final ItemStack itemStack;

	public NewItemPickupEvent(PlayerEntity player, ItemStack itemStack)
	{
		super(player);
		this.itemStack = itemStack;
	}

	@SuppressWarnings("resource")
	public NewItemPickupEvent(UUID playerUUID, ItemStack itemStack)
	{
		this(Minecraft.getInstance().level.getPlayerByUUID(playerUUID), itemStack);
	}

	public ItemStack getItemStack()
	{
		return itemStack;
	}
}
