package com.anthonyhilyard.iceberg.mixin.azurelib;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.renderer.CheckedBufferSource;
import com.anthonyhilyard.iceberg.renderer.VertexCollector;
import com.mojang.blaze3d.vertex.VertexConsumer;

import mod.azure.azurelib.rewrite.render.armor.AzArmorModel;
import mod.azure.azurelib.rewrite.render.armor.AzArmorRendererConfig;
import mod.azure.azurelib.rewrite.render.armor.AzArmorRendererPipeline;
import mod.azure.azurelib.rewrite.render.armor.AzArmorRendererPipelineContext;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;


@Mixin(AzArmorModel.class)
public class AzArmorModelMixin<E extends LivingEntity>
{
	@Shadow
	@Final
	private AzArmorRendererPipeline rendererPipeline;

	@ModifyArg(method = "renderToBuffer", require = 0, at = @At(value = "INVOKE",
			   target = "Lmod/azure/azurelib/rewrite/render/armor/AzArmorRendererPipeline;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lmod/azure/azurelib/rewrite/model/AzBakedModel;Ljava/lang/Object;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFI)V"))
	private MultiBufferSource icebergCustomBufferSource(MultiBufferSource multiBufferSource)
	{
		MultiBufferSource bufferSource = rendererPipeline.context().multiBufferSource();
		Iceberg.LOGGER.info("buffersource Class: " + (bufferSource == null ? "<null>" : bufferSource.getClass().getName()));
		if (bufferSource instanceof VertexCollector || bufferSource instanceof CheckedBufferSource)
		{
			return bufferSource;
		}
		else
		{
			return multiBufferSource;
		}
	}

	@ModifyArg(method = "renderToBuffer", require = 0, at = @At(value = "INVOKE",
			   target = "Lmod/azure/azurelib/rewrite/render/armor/AzArmorRendererPipeline;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lmod/azure/azurelib/rewrite/model/AzBakedModel;Ljava/lang/Object;Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/VertexConsumer;FFI)V"))
	private VertexConsumer icebergCustomBuffer(VertexConsumer buffer)
	{
		AzArmorRendererPipelineContext context = rendererPipeline.context();
		AzArmorRendererConfig config = rendererPipeline.config();
		MultiBufferSource bufferSource = context.multiBufferSource();

		if (bufferSource instanceof VertexCollector || bufferSource instanceof CheckedBufferSource)
		{
			Minecraft mc = Minecraft.getInstance();
			float partialTick = mc.getTimer().getGameTimeDeltaPartialTick(true);
			RenderType renderType = context.getDefaultRenderType(context.animatable(), config.textureLocation(context.animatable()), bufferSource, partialTick);
			return bufferSource.getBuffer(renderType);
		}
		else
		{
			return buffer;
		}
	}
}



