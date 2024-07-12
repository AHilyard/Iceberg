package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.events.client.NewItemPickupEvent;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public class LivingEntityMixin
{
	@Inject(method = { "onItemPickup" }, at = @At(value = "HEAD"))
	private void onItemPickup(ItemEntity itemEntity, CallbackInfo info)
	{
		if ((LivingEntity)(Object)this instanceof Player)
		{
			Player player = (Player)(Object)this;
			NewItemPickupEvent.EVENT.invoker().onItemPickup(player.getUUID(), itemEntity.getItem());
		}
	}
}
