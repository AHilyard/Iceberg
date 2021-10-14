package com.anthonyhilyard.iceberg.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Lighting;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;

import com.mojang.blaze3d.platform.GlStateManager.SourceFactor;
import com.mojang.blaze3d.platform.GlStateManager.DestFactor;
import com.mojang.math.Matrix4f;

import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

/**
 * An extended ItemRenderer that allows items to be rendered to RenderTarget before drawing to screen.  
 * This allows alpha values to be supported properly by all item types, even semi-transparent items.
 */
public class CustomItemRenderer extends ItemRenderer
{
	@SuppressWarnings("unused")
	private static final Logger LOGGER = LogManager.getLogger();

	private static RenderTarget iconFrameBuffer = null;
	private Minecraft mc;
	
	public CustomItemRenderer(TextureManager textureManagerIn, ModelManager modelManagerIn, ItemColors itemColorsIn, BlockEntityWithoutLevelRenderer blockEntityRendererIn, Minecraft mcIn)
	{
		super(textureManagerIn, modelManagerIn, itemColorsIn, blockEntityRendererIn);
		mc = mcIn;

		// Initialize the icon framebuffer if needed.
		if (iconFrameBuffer == null)
		{
			// Use 96 x 96 pixels for the icon frame buffer so at 1.5 scale we get 4x resolution (for smooth icons on larger gui scales).
			iconFrameBuffer = new MainTarget(96, 96);
			iconFrameBuffer.setClearColor(0.0f, 0.0f, 0.0f, 0.0f);
			iconFrameBuffer.clear(Minecraft.ON_OSX);
		}
	}

	protected void  renderItemModelIntoGUIWithAlpha(ItemStack stack, int x, int y, float alpha)
	{
		BakedModel bakedModel = mc.getItemRenderer().getModel(stack, null, null, 0);
		RenderTarget lastFrameBuffer = mc.getMainRenderTarget();

		// Bind the icon framebuffer so we can render to texture.
		iconFrameBuffer.clear(Minecraft.ON_OSX);
		iconFrameBuffer.bindWrite(true);

		RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
		RenderSystem.backupProjectionMatrix();
		RenderSystem.setProjectionMatrix(Matrix4f.orthographic(0.0f, iconFrameBuffer.width, iconFrameBuffer.height, 0.0f, 1000.0f, 3000.0f));

		Lighting.setupFor3DItems();

		mc.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).setFilter(false, false);
		RenderSystem.setShaderTexture(0, InventoryMenu.BLOCK_ATLAS);
		RenderSystem.enableBlend();
		RenderSystem.blendFuncSeparate(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA, SourceFactor.ONE, DestFactor.ONE_MINUS_SRC_ALPHA);
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

		PoseStack modelViewStack = RenderSystem.getModelViewStack();
		modelViewStack.pushPose();
		modelViewStack.setIdentity();
		modelViewStack.translate(48.0f, 48.0f, -2000.0f);
		modelViewStack.scale(96.0F, 96.0F, 96.0F);
		RenderSystem.applyModelViewMatrix();
		PoseStack poseStack = new PoseStack();
		MultiBufferSource.BufferSource multibuffersource$buffersource = Minecraft.getInstance().renderBuffers().bufferSource();
		boolean flag = !bakedModel.usesBlockLight();
		if (flag) { Lighting.setupForFlatItems(); }

		render(stack, ItemTransforms.TransformType.GUI, false, poseStack, multibuffersource$buffersource, 0xF000F0, OverlayTexture.NO_OVERLAY, bakedModel);
		multibuffersource$buffersource.endBatch();
		RenderSystem.enableDepthTest();
		if (flag) { Lighting.setupFor3DItems(); }

		modelViewStack.popPose();
		RenderSystem.applyModelViewMatrix();
		RenderSystem.restoreProjectionMatrix();

		// Rebind the previous framebuffer, if there was one.
		if (lastFrameBuffer != null)
		{
			lastFrameBuffer.bindWrite(true);

			// Blit from the texture we just rendered to, respecting the alpha value given.
			RenderSystem.enableBlend();
			RenderSystem.defaultBlendFunc();
			RenderSystem.disableCull();
			RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
			modelViewStack.pushPose();
			modelViewStack.scale(1.0f, -1.0f, 1.0f);
			modelViewStack.translate(0.0f, 0.0f, 50.0f + this.blitOffset);
			RenderSystem.applyModelViewMatrix();

			RenderSystem.setShaderTexture(0, iconFrameBuffer.getColorTextureId());

			GuiComponent.blit(new PoseStack(), x, y - 18, 16, 16, 0, 0, iconFrameBuffer.width, iconFrameBuffer.height, iconFrameBuffer.width, iconFrameBuffer.height);
			modelViewStack.popPose();
			RenderSystem.applyModelViewMatrix();
			iconFrameBuffer.unbindRead();
		}
		else
		{
			iconFrameBuffer.unbindWrite();
		}
	 }
}
