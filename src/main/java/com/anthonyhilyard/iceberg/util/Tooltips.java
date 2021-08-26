package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.matrix.MatrixStack;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;

public class Tooltips
{
	public static Rectangle2d calculateRect(final ItemStack stack, MatrixStack mStack, List<? extends ITextProperties> textLines, int mouseX, int mouseY,
												int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font)
	{
		Rectangle2d rect = new Rectangle2d(0, 0, 0, 0);
		if (textLines.isEmpty())
		{
			return rect;
		}

		// Generate a tooltip event even though we aren't rendering anything in case the event handlers are modifying the input values.
		RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(stack, textLines, mStack, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font);
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

		for (ITextProperties textLine : textLines)
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
			List<ITextProperties> wrappedTextLines = new ArrayList<>();
			for (int i = 0; i < textLines.size(); i++)
			{
				ITextProperties textLine = textLines.get(i);
				List<ITextProperties> wrappedLine = font.getSplitter().splitLines(textLine, tooltipTextWidth, Style.EMPTY);
				if (i == 0)
				{
					titleLinesCount = wrappedLine.size();
				}

				for (ITextProperties line : wrappedLine)
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

		rect = new Rectangle2d(tooltipX - 4, tooltipY - 4, tooltipTextWidth + 8, tooltipHeight + 8);
		return rect;
	}
}
