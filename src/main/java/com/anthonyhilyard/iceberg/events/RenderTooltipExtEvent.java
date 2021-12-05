package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;

public class RenderTooltipExtEvent
{
	public static class Pre extends RenderTooltipEvent.Pre
	{
		private final boolean comparisonTooltip;

		public Pre(ItemStack stack, PoseStack PoseStack, int x, int y, int screenWidth, int screenHeight, Font font, List<ClientTooltipComponent> components, boolean comparison)
		{
			super(stack, PoseStack, x, y, screenWidth, screenHeight, font, components);
			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
	}

	public static class Post extends RenderTooltipEvent
	{
		private final boolean comparisonTooltip;
		private final int width;
		private final int height;

		public Post(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison)
		{
			super(stack, PoseStack, x, y, font, components);
			this.width = width;
			this.height = height;

			comparisonTooltip = comparison;
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getWidth() { return width; }
		public int getHeight() { return height; }
	}

	public static class Color extends RenderTooltipEvent.Color
	{
		private final boolean comparisonTooltip;

		public Color(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison)
		{
			super(stack, PoseStack, x, y, font, background, borderStart, borderEnd, components);
			comparisonTooltip = comparison;
		}
		public Color(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison)
		{
			this(stack, PoseStack, x, y, font, backgroundStart, borderStart, borderEnd, components, comparison);
			setBackgroundStart(backgroundStart);
			setBackgroundEnd(backgroundEnd);
		}
		public boolean isComparison() { return comparisonTooltip; }
	}
}
