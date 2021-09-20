package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.PacketDistributor;

import com.anthonyhilyard.iceberg.network.IcebergNetworkProtocol;
import com.anthonyhilyard.iceberg.network.NewItemPickupEventPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ForgeEventFactory.class)
public class ForgeEventFactoryMixin
{
	@Inject(method = { "onItemPickup(Lnet/minecraft/entity/item/ItemEntity;Lnet/minecraft/entity/player/PlayerEntity;)I" },
			at = { @At("HEAD") }, remap = false)
	private static void onItemPickup(ItemEntity entityItem, PlayerEntity player, CallbackInfoReturnable<Integer> info)
	{
		if (player instanceof ServerPlayerEntity && FMLEnvironment.dist.isDedicatedServer())
		{
			IcebergNetworkProtocol.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity)player), new NewItemPickupEventPacket(player.getUUID(), entityItem.getItem()));
		}
	}
}
