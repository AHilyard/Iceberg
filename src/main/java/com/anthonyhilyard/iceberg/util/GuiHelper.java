package com.anthonyhilyard.iceberg.util;

import net.minecraft.client.renderer.GameRenderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.joml.Matrix4f;

public class GuiHelper
{
	public static void drawGradientRect(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor)
	{
		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		drawGradientRect(mat, bufferBuilder, left, top, right, bottom, zLevel, startColor, endColor);
		BufferUploader.drawWithShader(bufferBuilder.end());

		RenderSystem.disableBlend();
	}

	public static void drawGradientRect(Matrix4f mat, BufferBuilder bufferBuilder, int left, int top, int right, int bottom, int zLevel, int startColor, int endColor)
	{
		float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
		float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
		float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
		float startBlue  = (float)(startColor       & 255) / 255.0F;
		float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
		float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
		float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
		float endBlue    = (float)(endColor         & 255) / 255.0F;

		bufferBuilder.vertex(mat, right,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		bufferBuilder.vertex(mat,  left,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		bufferBuilder.vertex(mat,  left, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
		bufferBuilder.vertex(mat, right, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
	}

	public static void drawGradientRectHorizontal(Matrix4f mat, int zLevel, int left, int top, int right, int bottom, int startColor, int endColor)
	{
		float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
		float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
		float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
		float startBlue  = (float)(startColor       & 255) / 255.0F;
		float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
		float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
		float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
		float endBlue    = (float)(endColor         & 255) / 255.0F;

		RenderSystem.enableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);

		Tesselator tessellator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tessellator.getBuilder();
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		bufferBuilder.vertex(mat, right,    top, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
		bufferBuilder.vertex(mat,  left,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		bufferBuilder.vertex(mat,  left, bottom, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		bufferBuilder.vertex(mat, right, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
		tessellator.end();

		RenderSystem.disableBlend();
	}

	public static void blit(PoseStack poseStack, int x, int y, int width, int height, float texX, float texY, int texWidth, int texHeight, int fullWidth, int fullHeight) {
		blit(poseStack, x, x + width, y, y + height, 0, texWidth, texHeight, texX, texY, fullWidth, fullHeight);
	}
	public static void blit(PoseStack poseStack, int x0, int x1, int y0, int y1, int z, int texWidth, int texHeight, float texX, float texY, int fullWidth, int fullHeight) {
		innerBlit(poseStack, x0, x1, y0, y1, z, (texX + 0.0F) / (float)fullWidth, (texX + (float)texWidth) / (float)fullWidth, (texY + 0.0F) / (float)fullHeight, (texY + (float)texHeight) / (float)fullHeight);
	}

	private static void innerBlit(PoseStack poseStack, int x0, int x1, int y0, int y1, int z, float u0, float u1, float v0, float v1)
	{
		RenderSystem.setShader(GameRenderer::getPositionTexShader);
		Matrix4f matrix4f = poseStack.last().pose();
		BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(matrix4f, (float)x0, (float)y0, (float)z).uv(u0, v0).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x0, (float)y1, (float)z).uv(u0, v1).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x1, (float)y1, (float)z).uv(u1, v1).endVertex();
		bufferbuilder.vertex(matrix4f, (float)x1, (float)y0, (float)z).uv(u1, v0).endVertex();
		BufferUploader.drawWithShader(bufferbuilder.end());
	}
}
