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
	@SuppressWarnings("resource")
	@Inject(method = "runTick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = { "ldc=gameRenderer" }))
	public void runTick(boolean tickWorld, CallbackInfo callbackInfo)
	{
		Minecraft instance = (Minecraft)(Object)this;
		RenderTickEvents.START.invoker().onStart(instance.getTimer());
	}
}
