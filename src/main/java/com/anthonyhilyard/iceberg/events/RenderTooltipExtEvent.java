package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.event.RenderTooltipEvent;

@SuppressWarnings("null")
public class RenderTooltipExtEvent
{
	public static class Pre extends RenderTooltipEvent.Pre
	{
		private final boolean comparisonTooltip;
		private final int index;

		public Pre(ItemStack stack, PoseStack PoseStack, int x, int y, int screenWidth, int screenHeight, Font font, List<ClientTooltipComponent> components, boolean comparison, int index)
		{
			super(stack, PoseStack, x, y, screenWidth, screenHeight, font, components);
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public Pre(ItemStack stack, PoseStack PoseStack, int x, int y, int screenWidth, int screenHeight, Font font, List<ClientTooltipComponent> components, boolean comparison)
		{
			this(stack, PoseStack, x, y, screenWidth, screenHeight, font, components, comparison, 0);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
	}

	public static class Post extends RenderTooltipEvent
	{
		private final boolean comparisonTooltip;
		private final int index;
		private final int width;
		private final int height;

		public Post(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison, int index)
		{
			super(stack, PoseStack, x, y, font, components);
			this.width = width;
			this.height = height;
			this.comparisonTooltip = comparison;
			this.index = index;
		}

		public Post(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison)
		{
			this(stack, PoseStack, x, y, font, width, height, components, comparison, 0);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
		public int getWidth() { return width; }
		public int getHeight() { return height; }
	}

	public static class Color extends RenderTooltipEvent.Color
	{
		private final boolean comparisonTooltip;
		private final int index;

		public Color(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison, int index)
		{
			super(stack, PoseStack, x, y, font, background, borderStart, borderEnd, components);
			this.comparisonTooltip = comparison;
			this.index = index;
		}
		public Color(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison)
		{
			this(stack, PoseStack, x, y, font, background, borderStart, borderEnd, components, comparison, 0);
		}
		public Color(ItemStack stack, PoseStack PoseStack, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison, int index)
		{
			this(stack, PoseStack, x, y, font, backgroundStart, borderStart, borderEnd, components, comparison, index);
			setBackgroundStart(backgroundStart);
			setBackgroundEnd(backgroundEnd);
		}
		public boolean isComparison() { return comparisonTooltip; }
		public int getIndex() { return index; }
	}
}
