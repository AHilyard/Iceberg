package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import com.anthonyhilyard.iceberg.network.IcebergNetworkProtocol;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntity.class)
public class ItemEntityMixin
{
	@Inject(method = { "playerTouch" },
			at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;take(Lnet/minecraft/world/entity/Entity;I)V", ordinal = 0, shift = Shift.AFTER))
	private void onPlayerTouch(Player player, CallbackInfo info)
	{
		if (player instanceof ServerPlayer)
		{
			IcebergNetworkProtocol.sendItemPickupEvent((ServerPlayer)player, ((ItemEntity)(Object)this).getItem());
		}
	}
}
