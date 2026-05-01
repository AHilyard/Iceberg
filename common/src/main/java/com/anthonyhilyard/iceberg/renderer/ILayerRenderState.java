package com.anthonyhilyard.iceberg.renderer;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemDisplayContext;

public interface ILayerRenderState
{
	void renderWithoutTransform(PoseStack poseStack, ItemDisplayContext displayContext, SubmitNodeCollector submitNodeCollector, int packedLight, int overlay, int seed);
}