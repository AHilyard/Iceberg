package com.anthonyhilyard.iceberg.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin
{
	@SuppressWarnings({ "resource", "unchecked" })
	@ModifyArg(method = "createLevels",
			at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"), index = 1)
	private Object levelLoadOnCreate(Object key, Object level)
	{
		MinecraftServer instance = (MinecraftServer)(Object)this;
		LevelEvents.LOAD.invoker().onLoad(instance.getLevel((ResourceKey<Level>)key));
		return level;
	}

	@Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;close()V", shift = Shift.BEFORE))
	private void levelUnloadOnStop(CallbackInfo info)
	{
		LevelEvents.UNLOAD.invoker().onUnload(null);
	}
}
