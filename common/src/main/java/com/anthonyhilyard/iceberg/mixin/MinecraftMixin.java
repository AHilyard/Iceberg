package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.events.client.RenderTickEvents;

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
}
