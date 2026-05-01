package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.renderer.LevelRenderer;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	@Inject(method = "getItemEntityTarget", at = @At(value = "HEAD"), cancellable = true)
	public void swapItemEntityTarget(CallbackInfoReturnable<RenderTarget> callbackInfo)
	{
		//TODO
		/*
		if (CustomItemRenderer.swapFrameBuffer)
		{
			callbackInfo.setReturnValue(CustomItemRenderer.iconFrameBuffer);
		}
		 */
	}
}
