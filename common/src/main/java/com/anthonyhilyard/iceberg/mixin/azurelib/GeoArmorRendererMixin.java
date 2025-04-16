package com.anthonyhilyard.iceberg.mixin.azurelib;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.Item;

@SuppressWarnings({"deprecation", "removal"})
@Mixin(GeoArmorRenderer.class)
public class GeoArmorRendererMixin<T extends Item & GeoItem>
{
	@Shadow
	protected T animatable;

	@Unique
	private static MultiBufferSource bufferSource;

	@ModifyArg(method = "renderToBuffer", require = 0, at = @At(value = "INVOKE",
			   target = "Lmod/azure/azurelib/common/api/client/renderer/GeoArmorRenderer;defaultRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lmod/azure/azurelib/core/animatable/GeoAnimatable;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFI)V"))
	private MultiBufferSource icebergCustomBufferSource(MultiBufferSource multiBufferSource)
	{
		if (bufferSource instanceof VertexCollector || bufferSource instanceof CheckedBufferSource)
		{
			return bufferSource;
		}
		else
		{
			return multiBufferSource;
		}
	}

	@SuppressWarnings("unchecked")
	@ModifyArg(method = "renderToBuffer", require = 0, at = @At(value = "INVOKE", remap = false,
			   target = "Lmod/azure/azurelib/common/api/client/renderer/GeoArmorRenderer;defaultRender(Lcom/mojang/blaze3d/vertex/PoseStack;Lmod/azure/azurelib/core/animatable/GeoAnimatable;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFI)V"))
	private VertexConsumer icebergCustomBuffer(VertexConsumer buffer)
	{
		if (bufferSource instanceof VertexCollector || bufferSource instanceof CheckedBufferSource)
		{
			GeoArmorRenderer<T> self = (GeoArmorRenderer<T>)(Object)this;
			Minecraft mc = Minecraft.getInstance();
			float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
			RenderType renderType = self.getRenderType(this.animatable, self.getTextureLocation(this.animatable), (MultiBufferSource)bufferSource, partialTick);
			return bufferSource.getBuffer(renderType);
		}
		else
		{
			return buffer;
		}
	}
}



