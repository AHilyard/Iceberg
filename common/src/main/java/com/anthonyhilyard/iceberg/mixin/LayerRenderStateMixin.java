package com.anthonyhilyard.iceberg.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.anthonyhilyard.iceberg.renderer.ILayerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;

@Mixin(LayerRenderState.class)
public class LayerRenderStateMixin implements ILayerRenderState
{
	@Shadow
	BakedModel model;

	@Shadow
	private RenderType renderType;

	@Shadow
	private ItemStackRenderState.FoilType foilType;

	@Shadow
	private int[] tintLayers;

	@Shadow
	private SpecialModelRenderer<Object> specialRenderer;

	@Shadow
	private Object argumentForSpecialRendering;

	@Override
	public void renderWithoutTransform(PoseStack poseStack, ItemDisplayContext displayContext, MultiBufferSource multiBufferSource, int packedLight, int overlay)
	{

		if (specialRenderer != null)
		{
			specialRenderer.render(argumentForSpecialRendering, displayContext, poseStack, multiBufferSource, packedLight, overlay, foilType != FoilType.NONE);
		}
		else if (model != null)
		{
			ItemRenderer.renderItem(displayContext, poseStack, multiBufferSource, packedLight, overlay, tintLayers, model, renderType, foilType);
		}
	}
}
