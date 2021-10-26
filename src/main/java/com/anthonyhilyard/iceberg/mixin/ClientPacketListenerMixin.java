package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;

import com.anthonyhilyard.iceberg.events.NewItemPickupCallback;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin
{
	@Inject(method = { "handleTakeItemEntity" },
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void handleTakeItemEntity(ClientboundTakeItemEntityPacket clientboundTakeItemEntityPacket, CallbackInfo info, Entity entity, LivingEntity livingEntity)
	{
		if (livingEntity instanceof Player)
		{
			Player player = (Player)livingEntity;
			ItemEntity itemEntity = (ItemEntity)entity;
			NewItemPickupCallback.EVENT.invoker().onItemPickup(player.getUUID(), itemEntity.getItem());
		}
	}
}
