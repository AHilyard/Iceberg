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
		private int index = 0;

		public Pre(ItemStack stack, List<? extends ITextProperties> lines, MatrixStack matrixStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, FontRenderer font, boolean comparison, int index)
		{
			super(stack, lines, matrixStack, x, y, screenWidth, screenHeight, maxWidth, font);
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public Pre(ItemStack stack, List<? extends ITextProperties> lines, MatrixStack matrixStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, FontRenderer font, boolean comparison)
		{
			this(stack, lines, matrixStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison, 0);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
	}

	public static class PostBackground extends RenderTooltipEvent.PostBackground
	{
		private boolean comparisonTooltip = false;
		private int index = 0;

		public PostBackground(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison, int index)
		{
			super(stack, textLines, matrixStack, x, y, font, width, height);
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public PostBackground(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison)
		{
			this(stack, textLines, matrixStack, x, y, font, width, height, comparison, 0);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
	}

	public static class PostText extends RenderTooltipEvent.PostText
	{
		private boolean comparisonTooltip = false;
		private int index = 0;

		public PostText(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison, int index)
		{
			super(stack, textLines, matrixStack, x, y, font, width, height);
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public PostText(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int width, int height, boolean comparison)
		{
			this(stack, textLines, matrixStack, x, y, font, width, height, comparison, 0);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
	}

	public static class Color extends RenderTooltipEvent.Color
	{
		private boolean comparisonTooltip = false;
		private int index = 0;
		private int backgroundEnd = 0;

		public Color(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int background, int borderStart, int borderEnd, boolean comparison, int index)
		{
			super(stack, textLines, matrixStack, x, y, font, background, borderStart, borderEnd);
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public Color(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int background, int borderStart, int borderEnd, boolean comparison)
		{
			this(stack, textLines, matrixStack, x, y, font, background, borderStart, borderEnd, comparison, 0);
		}

		public Color(ItemStack stack, List<? extends ITextProperties> textLines, MatrixStack matrixStack, int x, int y, FontRenderer font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, boolean comparison, int index)
		{
			this(stack, textLines, matrixStack, x, y, font, backgroundStart, borderStart, borderEnd, comparison, index);
			setBackgroundStart(backgroundStart);
			setBackgroundEnd(backgroundEnd);
		}

		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
		public void setBackgroundStart(int backgroundStart) { setBackground(backgroundStart); }
		public void setBackgroundEnd(int backgroundEnd) { this.backgroundEnd = backgroundEnd; }
		public int getBackgroundStart() { return getBackground(); }
		public int getBackgroundEnd() { return backgroundEnd; }
	}
}
