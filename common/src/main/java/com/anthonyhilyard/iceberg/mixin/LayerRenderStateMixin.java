package com.anthonyhilyard.iceberg.mixin;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.resources.model.geometry.ItemQuads;
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
	private ItemQuads quads;

	@Shadow
	private ItemStackRenderState.FoilType foilType;

	@Shadow
	private IntList tintLayers;

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
			submitNodeCollector.submitItem(poseStack, displayContext, packedLight, overlay, seed, tintLayers.toIntArray(), quads, foilType);
		}
	}
}