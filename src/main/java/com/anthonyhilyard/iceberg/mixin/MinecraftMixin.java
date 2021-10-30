package com.anthonyhilyard.iceberg.mixin;

import com.anthonyhilyard.iceberg.events.RenderTickEvents;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Timer;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@Shadow
	private boolean pause;

	@Shadow
	private float pausePartialTick;

	@Shadow
	private Timer timer;

	@Inject(method = "runTick", at = @At(value = "INVOKE_STRING", target = "Lnet/minecraft/util/profiling/ProfilerFiller;popPush(Ljava/lang/String;)V", args = { "ldc=gameRenderer" }))
	public void runTick(boolean tickWorld, CallbackInfo callbackInfo)
	{
		RenderTickEvents.START.invoker().onStart(pause ? pausePartialTick : timer.partialTick);
	}
}
