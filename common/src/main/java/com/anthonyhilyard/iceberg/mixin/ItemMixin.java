package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.util.EntityCollector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

@Mixin(value = Item.class, priority = 100)
public class ItemMixin
{
	@Inject(method = "getPlayerPOVHitResult", at = @At(value = "HEAD"), cancellable = true)
	private static void icebergGetPlayerPOVHitResult(Level level, Player player, ClipContext.Fluid clipContext, CallbackInfoReturnable<HitResult> info)
	{
		// If the level is an entity collector, always return a valid hit result.
		if (level instanceof EntityCollector)
		{
			info.setReturnValue(new BlockHitResult(Vec3.ZERO, Direction.DOWN, BlockPos.ZERO, false));
		}
	}
}
