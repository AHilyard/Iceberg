package com.anthonyhilyard.iceberg.fabric.mixin;

import net.minecraft.client.renderer.extract.LevelExtractor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.anthonyhilyard.iceberg.events.common.LevelEvents;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientLevel.ClientLevelData;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;

@Mixin(ClientLevel.class)
public class ClientLevelMixin
{
	@Inject(method = "<init>",
			at = @At("TAIL"))
	private void levelLoadOnInit(ClientPacketListener connection, ClientLevelData levelData, ResourceKey dimension, Holder dimensionType, int serverChunkRadius, int serverSimulationDistance, LevelExtractor levelExtractor, boolean isDebug, long biomeZoomSeed, int seaLevel, CallbackInfo ci)
	{
		LevelEvents.LOAD.invoker().onLoad((ClientLevel)(Object)this);
	}
}
