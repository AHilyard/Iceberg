package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.network.PacketDistributor;

import com.anthonyhilyard.iceberg.network.IcebergNetworkProtocol;
import com.anthonyhilyard.iceberg.network.NewItemPickupEventPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ForgeEventFactory.class)
public class ForgeEventFactoryMixin
{
	@Inject(method = { "onItemPickup(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/entity/player/Player;)I" },
			at = { @At("HEAD") }, remap = false)
	private static void onItemPickup(ItemEntity entityItem, Player player, CallbackInfoReturnable<Integer> info)
	{
		if (player instanceof ServerPlayer && FMLEnvironment.dist.isDedicatedServer())
		{
			IcebergNetworkProtocol.CHANNEL.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer)player), new NewItemPickupEventPacket(player.getUUID(), entityItem.getItem()));
		}
	}
}
