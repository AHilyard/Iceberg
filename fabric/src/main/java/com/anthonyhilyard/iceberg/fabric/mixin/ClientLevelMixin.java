package com.anthonyhilyard.iceberg.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

@Mixin(ClientLevel.class)
public class ClientLevelMixin
{
	@Inject(method = "<init>(Lnet/minecraft/client/multiplayer/ClientPacketListener;Lnet/minecraft/client/multiplayer/ClientLevel$ClientLevelData;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/Holder;IILnet/minecraft/client/renderer/LevelRenderer;ZJI)V",
			at = @At("TAIL"))
	private void levelLoadOnInit(ClientPacketListener clientPacketListener, ClientLevelData clientLevelData, ResourceKey<Level> resourceKey, Holder<DimensionType> holder, int i, int j, LevelRenderer levelRenderer, boolean bl, long l, int k, CallbackInfo info)
	{
		LevelEvents.LOAD.invoker().onLoad((ClientLevel)(Object)this);
	}
}
