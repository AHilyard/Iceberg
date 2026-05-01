package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.anthonyhilyard.iceberg.events.client.RenderTickEvents;
import com.anthonyhilyard.iceberg.renderer.CustomItemRenderer;
import com.mojang.blaze3d.pipeline.RenderTarget;

import net.minecraft.client.Minecraft;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V"))
	public void runTick(boolean tickWorld, CallbackInfo callbackInfo)
	{
		Minecraft instance = (Minecraft)(Object)this;
		RenderTickEvents.START.invoker().onStart(instance.getDeltaTracker());
	}

	@Inject(method = "getMainRenderTarget", at = @At(value = "HEAD"), cancellable = true)
	public void swapRenderTarget(CallbackInfoReturnable<RenderTarget> callbackInfo)
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
