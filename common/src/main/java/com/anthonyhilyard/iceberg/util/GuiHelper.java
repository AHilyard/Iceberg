package com.anthonyhilyard.iceberg.util;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

public class GuiHelper
{
	public static void drawGradientRect(GuiGraphics graphics, int left, int top, int right, int bottom, int startColor, int endColor)
	{
		graphics.fillGradient(left, top, right, bottom, startColor, endColor);
	}

	public static void drawGradientRect(Matrix4f mat, VertexConsumer vertexConsumer, int left, int top, int right, int bottom, int zLevel, int startColor, int endColor)
	{
		float startAlpha = (float)(startColor >> 24 & 255) / 255.0F;
		float startRed   = (float)(startColor >> 16 & 255) / 255.0F;
		float startGreen = (float)(startColor >>  8 & 255) / 255.0F;
		float startBlue  = (float)(startColor       & 255) / 255.0F;
		float endAlpha   = (float)(endColor   >> 24 & 255) / 255.0F;
		float endRed     = (float)(endColor   >> 16 & 255) / 255.0F;
		float endGreen   = (float)(endColor   >>  8 & 255) / 255.0F;
		float endBlue    = (float)(endColor         & 255) / 255.0F;

		vertexConsumer.addVertex(mat, right,    top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
		vertexConsumer.addVertex(mat,  left,    top, zLevel).setColor(startRed, startGreen, startBlue, startAlpha);
		vertexConsumer.addVertex(mat,  left, bottom, zLevel).setColor(  endRed,   endGreen,   endBlue,   endAlpha);
		vertexConsumer.addVertex(mat, right, bottom, zLevel).setColor(  endRed,   endGreen,   endBlue,   endAlpha);
	}

	public static void drawGradientRectHorizontal(GuiGraphics graphics, int left, int top, int right, int bottom, int startColor, int endColor)
	{
		int width = right - left;
		int height = bottom - top;

		graphics.pose().pushMatrix();
		graphics.pose().translate(left, top);
		graphics.pose().rotate((float) -Math.PI / 2f);

		graphics.fillGradient(-height, 0, 0, width, startColor, endColor);

		graphics.pose().popMatrix();
	}

	public static void blit(GuiGraphics graphics, Identifier texture, int x0, int x1, int y0, int y1, int z, int texWidth, int texHeight, float texX, float texY, int fullWidth, int fullHeight)
	{
		graphics.blit(texture, x0, y0, x1, y1, (texX + 0.0F) / (float)fullWidth, (texX + (float)texWidth) / (float)fullWidth, (texY + 0.0F) / (float)fullHeight, (texY + (float)texHeight) / (float)fullHeight);
	}

	public static void blit(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, float texX, float texY, int texWidth, int texHeight, int fullWidth, int fullHeight)
	{
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, texX, texY, width, height, fullWidth, fullHeight);
	}

	public static void blit(GuiGraphics graphics, Identifier texture, int x, int y, int width, int height, float texX, float texY, int texWidth, int texHeight, int fullWidth, int fullHeight, int color)
	{
		graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, texX, texY, width, height, fullWidth, fullHeight, color);
	}
}
