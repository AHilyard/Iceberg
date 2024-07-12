package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import com.anthonyhilyard.iceberg.events.common.CriterionEvent;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.level.ServerPlayer;

@Mixin(PlayerAdvancements.class)
public class PlayerAdvancementsMixin
{
	@Shadow
	private ServerPlayer player;

	@Inject(method = "award", at = @At(value = "TAIL"), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
	public void onAward(AdvancementHolder advancementHolder, String criterionKey, CallbackInfoReturnable<Boolean> callbackInfo, boolean success)
	{
		if (success)
		{
			CriterionEvent.EVENT.invoker().awardCriterion(player, advancementHolder, criterionKey);
		}
	}
}
