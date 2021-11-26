package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;

import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Style;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fmlclient.gui.GuiUtils;

public class Tooltips
{
	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private Font font;
		private List<? extends FormattedText> lines = new ArrayList<>();

		public TooltipInfo(List<? extends FormattedText> lines, Font font)
		{
			this.lines = lines;
			this.font = font;
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTitleLines() { return titleLines; }
		public Font getFont() { return font; }
		public List<? extends FormattedText> getLines() { return lines; }

		public void setFont(Font font) { this.font = font; }

		public int getMaxLineWidth()
		{
			int textWidth = 0;
			for (FormattedText textLine : lines)
			{
				int textLineWidth = font.width(textLine);
				if (textLineWidth > textWidth)
				{
					textWidth = textLineWidth;
				}
			}
			return textWidth;
		}

		public void wrap(int maxWidth)
		{
			tooltipWidth = 0;
			List<FormattedText> wrappedLines = new ArrayList<>();
			for (int i = 0; i < lines.size(); i++)
			{
				FormattedText textLine = lines.get(i);
				List<FormattedText> wrappedLine = font.getSplitter().splitLines(textLine, maxWidth, Style.EMPTY);
				if (i == 0)
				{
					titleLines = wrappedLine.size();
				}

				for (FormattedText line : wrappedLine)
				{
					int lineWidth = font.width(line);
					if (lineWidth > tooltipWidth)
					{
						tooltipWidth = lineWidth;
					}
					wrappedLines.add(line);
				}
			}

			lines = wrappedLines;
		}
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd)
	{
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, false);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison)
	{
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, comparison, false);
	}

	@SuppressWarnings("removal")
	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison, boolean constrain)
	{
		if (info.getLines().isEmpty())
		{
			return;
		}

		int rectX = rect.getX() - 8;
		int rectY = rect.getY() + 18;
		int maxTextWidth = rect.getWidth() - 8;

		RenderTooltipExtEvent.Pre event = new RenderTooltipExtEvent.Pre(stack, info.getLines(), poseStack, rectX, rectY, screenWidth, screenHeight, maxTextWidth, info.getFont(), comparison);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return;
		}

		rectX = event.getX();
		rectY = event.getY();
		screenWidth = event.getScreenWidth();
		screenHeight = event.getScreenHeight();
		maxTextWidth = event.getMaxWidth();
		info.setFont(event.getFontRenderer());

		RenderSystem.disableDepthTest();
		int tooltipTextWidth = info.getMaxLineWidth();

		// Constrain the minimum width to the rect.
		if (constrain)
		{
			tooltipTextWidth = Math.max(info.getMaxLineWidth(), rect.getWidth() - 8);
		}

		boolean needsWrap = false;

		int tooltipX = rectX + 12;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth)
		{
			tooltipX = rectX - 16 - tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (rectX > screenWidth / 2)
				{
					tooltipTextWidth = rectX - 12 - 8;
				}
				else
				{
					tooltipTextWidth = screenWidth - 16 - rectX;
				}
				needsWrap = true;
			}
		}

		if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth)
		{
			tooltipTextWidth = maxTextWidth;
			needsWrap = true;
		}

		if (needsWrap)
		{
			info.wrap(tooltipTextWidth);
			tooltipTextWidth = info.getTooltipWidth();
			tooltipX = rectX + 12;
		}

		int tooltipY = rectY - 12;
		int tooltipHeight = 8;

		if (info.getLines().size() > 1)
		{
			tooltipHeight += (info.getLines().size() - 1) * 10;
			if (info.getLines().size() > info.getTitleLines())
			{
				tooltipHeight += 2; // gap between title lines and next lines
			}
		}

		if (tooltipY < 4)
		{
			tooltipY = 4;
		}
		else if (tooltipY + tooltipHeight + 4 > screenHeight)
		{
			tooltipY = screenHeight - tooltipHeight - 4;
		}

		final int zLevel = 400;
		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(stack, info.getLines(), poseStack, tooltipX, tooltipY, info.getFont(), backgroundColor, borderColorStart, borderColorEnd, comparison);
		MinecraftForge.EVENT_BUS.post(colorEvent);
		backgroundColor = colorEvent.getBackground();
		borderColorStart = colorEvent.getBorderStart();
		borderColorEnd = colorEvent.getBorderEnd();

		poseStack.pushPose();
		Matrix4f mat = poseStack.last().pose();

		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

		MinecraftForge.EVENT_BUS.post(new RenderTooltipEvent.PostBackground(stack, info.getLines(), poseStack, tooltipX, tooltipY, info.getFont(), tooltipTextWidth, tooltipHeight));

		BufferSource renderType = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		poseStack.translate(0.0D, 0.0D, zLevel);

		int tooltipTop = tooltipY;

		for (int lineNumber = 0; lineNumber < info.getLines().size(); ++lineNumber)
		{
			FormattedText line = info.getLines().get(lineNumber);
			if (line != null)
			{
				info.getFont().drawInBatch(Language.getInstance().getVisualOrder(line), (float)tooltipX, (float)tooltipY, -1, true, mat, renderType, false, 0, 15728880);
			}

			if (lineNumber + 1 == info.getTitleLines())
			{
				tooltipY += 2;
			}

			tooltipY += 10;
		}

		renderType.endBatch();
		poseStack.popPose();

		MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.PostText(stack, info.getLines(), poseStack, tooltipX, tooltipTop, info.getFont(), tooltipTextWidth, tooltipHeight, comparison));

		RenderSystem.enableDepthTest();
	}

	@SuppressWarnings("removal")
	public static Rect2i calculateRect(final ItemStack stack, PoseStack poseStack, List<? extends FormattedText> textLines, int mouseX, int mouseY,
												int screenWidth, int screenHeight, int maxTextWidth, Font font)
	{
		Rect2i rect = new Rect2i(0, 0, 0, 0);
		if (textLines == null || textLines.isEmpty() || stack == null)
		{
			return rect;
		}

		// Generate a tooltip event even though we aren't rendering anything in case the event handlers are modifying the input values.
		RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(stack, textLines, poseStack, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return rect;
		}

		mouseX = event.getX();
		mouseY = event.getY();
		screenWidth = event.getScreenWidth();
		screenHeight = event.getScreenHeight();
		maxTextWidth = event.getMaxWidth();
		font = event.getFontRenderer();

		int tooltipTextWidth = 0;

		for (FormattedText textLine : textLines)
		{
			int textLineWidth = font.width(textLine);
			if (textLineWidth > tooltipTextWidth)
			{
				tooltipTextWidth = textLineWidth;
			}
		}

		boolean needsWrap = false;

		int titleLinesCount = 1;
		int tooltipX = mouseX + 14;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth)
		{
			tooltipX = mouseX - 16 - tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (mouseX > screenWidth / 2)
				{
					tooltipTextWidth = mouseX - 14 - 8;
				}
				else
				{
					tooltipTextWidth = screenWidth - 16 - mouseX;
				}
				needsWrap = true;
			}
		}

		if (maxTextWidth > 0 && tooltipTextWidth > maxTextWidth)
		{
			tooltipTextWidth = maxTextWidth;
			needsWrap = true;
		}

		if (needsWrap)
		{
			int wrappedTooltipWidth = 0;
			List<FormattedText> wrappedTextLines = new ArrayList<>();
			for (int i = 0; i < textLines.size(); i++)
			{
				FormattedText textLine = textLines.get(i);
				List<FormattedText> wrappedLine = font.getSplitter().splitLines(textLine, tooltipTextWidth, Style.EMPTY);
				if (i == 0)
				{
					titleLinesCount = wrappedLine.size();
				}

				for (FormattedText line : wrappedLine)
				{
					int lineWidth = font.width(line);
					if (lineWidth > wrappedTooltipWidth)
					{
						wrappedTooltipWidth = lineWidth;
					}
					wrappedTextLines.add(line);
				}
			}
			tooltipTextWidth = wrappedTooltipWidth;
			textLines = wrappedTextLines;

			if (mouseX > screenWidth / 2)
			{
				tooltipX = mouseX - 16 - tooltipTextWidth;
			}
			else
			{
				tooltipX = mouseX + 14;
			}
		}

		int tooltipY = mouseY - 14;
		int tooltipHeight = 8;

		if (textLines.size() > 1)
		{
			tooltipHeight += (textLines.size() - 1) * 10;
			if (textLines.size() > titleLinesCount)
			{
				tooltipHeight += 2; // gap between title lines and next lines
			}
		}

		if (tooltipY < 4)
		{
			tooltipY = 4;
		}
		else if (tooltipY + tooltipHeight + 4 > screenHeight)
		{
			tooltipY = screenHeight - tooltipHeight - 4;
		}

		rect = new Rect2i(tooltipX - 4, tooltipY - 4, tooltipTextWidth + 8, tooltipHeight + 8);
		return rect;
	}
}
