package com.anthonyhilyard.iceberg.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.Inject;

import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

@Mixin(Minecraft.class)
public class MinecraftMixin
{
	@Inject(method = "setLevel", at = @At(value = "HEAD"))
	private void levelUnloadOnSet(ClientLevel clientLevel, CallbackInfo ci)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null)
		{
			LevelEvents.UNLOAD.invoker().onUnload(minecraft.level);
		}
	}

	@Inject(method = "disconnect(Lnet/minecraft/client/gui/screens/Screen;ZZ)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreenAndShow(Lnet/minecraft/client/gui/screens/Screen;)V", shift = Shift.AFTER))
	private void levelUnloadOnDisconnect(CallbackInfo info)
	{
		Minecraft minecraft = Minecraft.getInstance();
		if (minecraft.level != null)
		{
			LevelEvents.UNLOAD.invoker().onUnload(minecraft.level);
		}
	}
}
