package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SCollectItemPacket;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.common.MinecraftForge;

import com.anthonyhilyard.iceberg.events.NewItemPickupEvent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientPlayNetHandler.class)
public class ClientPlayNetHandlerMixin
{
	@Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;shrink(I)V"),
			locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	private void handleTakeItemEntity(SCollectItemPacket packet, CallbackInfo info, Entity entity, LivingEntity livingEntity, ItemEntity itemEntity, ItemStack itemStack)
	{
		if (livingEntity instanceof PlayerEntity)
		{
			MinecraftForge.EVENT_BUS.post(new NewItemPickupEvent(livingEntity.getUUID(), itemStack));
		}
	}
}