package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextProperties;
import net.minecraftforge.client.event.RenderTooltipEvent;

public class RenderTooltipExtEvent
{
	public static class Pre extends RenderTooltipEvent.Pre
	{
		private boolean comparisonTooltip = false;
		public Pre(ItemStack stack, List<? extends ITextProperties> lines, MatrixStack matrixStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, FontRenderer font, boolean comparison)
		{
			super(stack, lines, matrixStack, x, y, screenWidth, screenHeight, maxWidth, font);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	public static class PostBackground extends RenderTooltipEvent.PostBackground
	{
		private boolean comparisonTooltip = false;
		public PostBackground(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison)
		{
			super(stack, textLines, matrixStack, x, y, font, width, height);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	public static class PostText extends RenderTooltipEvent.PostText
	{
		private boolean comparisonTooltip = false;
		public PostText(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison)
		{
			super(stack, textLines, matrixStack, x, y, font, width, height);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	public static class Color extends RenderTooltipEvent.Color
	{
		private boolean comparisonTooltip = false;
		public Color(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int background, int borderStart, int borderEnd, boolean comparison)
		{
			super(stack, textLines, matrixStack, x, y, font, background, borderStart, borderEnd);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}
}
