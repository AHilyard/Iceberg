package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.FormattedText;
import net.minecraftforge.client.event.RenderTooltipEvent;

public class RenderTooltipExtEvent
{
	public static class Pre extends RenderTooltipEvent.Pre
	{
		private boolean comparisonTooltip = false;

		@SuppressWarnings("removal")
		public Pre(ItemStack stack, List<? extends FormattedText> lines, PoseStack PoseStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, Font font, boolean comparison)
		{
			super(stack, lines, PoseStack, x, y, screenWidth, screenHeight, maxWidth, font);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	@SuppressWarnings("removal")
	public static class PostBackground extends RenderTooltipEvent.PostBackground
	{
		private boolean comparisonTooltip = false;

		public PostBackground(ItemStack stack, List<? extends FormattedText> textLines, PoseStack PoseStack, int x, int y, Font font, int width, int height, boolean comparison)
		{
			super(stack, textLines, PoseStack, x, y, font, width, height);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	@SuppressWarnings("removal")
	public static class PostText extends RenderTooltipEvent.PostText
	{
		private boolean comparisonTooltip = false;

		public PostText(ItemStack stack, List<? extends FormattedText> textLines, PoseStack PoseStack, int x, int y, Font font, int width, int height, boolean comparison)
		{
			super(stack, textLines, PoseStack, x, y, font, width, height);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	public static class Color extends RenderTooltipEvent.Color
	{
		private boolean comparisonTooltip = false;

		@SuppressWarnings("removal")
		public Color(ItemStack stack, List<? extends FormattedText> textLines, PoseStack PoseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, boolean comparison)
		{
			super(stack, textLines, PoseStack, x, y, font, background, borderStart, borderEnd);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}
}
