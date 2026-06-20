package com.anthonyhilyard.iceberg.mixin;

import java.util.List;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import com.anthonyhilyard.iceberg.renderer.ILayerRenderState;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.ItemStackRenderState.FoilType;
import net.minecraft.client.renderer.item.ItemStackRenderState.LayerRenderState;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemDisplayContext;

@Mixin(LayerRenderState.class)
public class LayerRenderStateMixin implements ILayerRenderState
{
	@Shadow
	private List<BakedQuad> quads;

	@Shadow
	private ItemStackRenderState.FoilType foilType;

	@Shadow
	private int[] tintLayers;

	@Shadow
	private SpecialModelRenderer<Object> specialRenderer;

	@Shadow
	private Object argumentForSpecialRendering;

	@Override
	public void renderWithoutTransform(PoseStack poseStack, ItemDisplayContext displayContext, SubmitNodeCollector submitNodeCollector, int packedLight, int overlay, int seed)
	{

		if (specialRenderer != null)
		{
			specialRenderer.submit(argumentForSpecialRendering, poseStack, submitNodeCollector, packedLight, overlay, foilType != FoilType.NONE, seed);
		}
		else
		{
			submitNodeCollector.submitItem(poseStack, displayContext, packedLight, overlay, seed, tintLayers, quads, foilType);
		}
	}
}