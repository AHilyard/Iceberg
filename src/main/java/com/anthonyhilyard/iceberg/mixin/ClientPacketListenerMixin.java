package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraftforge.common.MinecraftForge;

import com.anthonyhilyard.iceberg.events.NewItemPickupEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPacketListener.class)
public class ClientPacketListenerMixin
{
	@Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void handleTakeItemEntity(ClientboundTakeItemEntityPacket packet, CallbackInfo info, Entity entity, LivingEntity livingEntity, ItemEntity itemEntity, ItemStack itemStack)
	{
		if (livingEntity instanceof Player)
		{
			MinecraftForge.EVENT_BUS.post(new NewItemPickupEvent(livingEntity.getUUID(), itemStack));
		}
	}
}
