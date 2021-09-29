package com.anthonyhilyard.iceberg.util;

import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.math.vector.Matrix4f;

public class GuiHelper
{
	@SuppressWarnings("deprecation")
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
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.shadeModel(GL11.GL_SMOOTH);

		Tessellator tessellator = Tessellator.getInstance();
		BufferBuilder buffer = tessellator.getBuilder();
		buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
		buffer.vertex(mat, right,    top, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
		buffer.vertex(mat,  left,    top, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		buffer.vertex(mat,  left, bottom, zLevel).color(startRed, startGreen, startBlue, startAlpha).endVertex();
		buffer.vertex(mat, right, bottom, zLevel).color(  endRed,   endGreen,   endBlue,   endAlpha).endVertex();
		tessellator.end();

		RenderSystem.shadeModel(GL11.GL_FLAT);
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();
	}
}
