package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.ITextProperties;
import net.minecraft.util.text.LanguageMap;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.gui.GuiUtils;

public class Tooltips
{
	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private FontRenderer font;
		private List<? extends ITextProperties> lines = new ArrayList<>();

		public TooltipInfo(List<? extends ITextProperties> lines, FontRenderer font)
		{
			this.lines = lines;
			this.font = font;
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTitleLines() { return titleLines; }
		public FontRenderer getFont() { return font; }
		public List<? extends ITextProperties> getLines() { return lines; }

		public void setFont(FontRenderer font) { this.font = font; }

		public int getMaxLineWidth()
		{
			int textWidth = 0;
			for (ITextProperties textLine : lines)
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
			List<ITextProperties> wrappedLines = new ArrayList<>();
			for (int i = 0; i < lines.size(); i++)
			{
				ITextProperties textLine = lines.get(i);
				List<ITextProperties> wrappedLine = font.getSplitter().splitLines(textLine, maxWidth, Style.EMPTY);
				if (i == 0)
				{
					titleLines = wrappedLine.size();
				}

				for (ITextProperties line : wrappedLine)
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

	public static void renderItemTooltip(@Nonnull final ItemStack stack, MatrixStack matrixStack, TooltipInfo info,
										Rectangle2d rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd)
	{
		renderItemTooltip(stack, matrixStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, false);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, MatrixStack matrixStack, TooltipInfo info,
										Rectangle2d rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison)
	{
		renderItemTooltip(stack, matrixStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, comparison, false);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, MatrixStack matrixStack, TooltipInfo info,
										Rectangle2d rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison, boolean constrain)
	{
		renderItemTooltip(stack, matrixStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, comparison, constrain, false, 0);
	}

	@SuppressWarnings("deprecation")
	public static void renderItemTooltip(@Nonnull final ItemStack stack, MatrixStack matrixStack, TooltipInfo info,
										Rectangle2d rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd,
										boolean comparison, boolean constrain, boolean centeredTitle, int index)
	{
		if (info.getLines().isEmpty())
		{
			return;
		}

		// Center the title now if needed.
		if (centeredTitle)
		{
			info = new TooltipInfo(centerTitle(info.getLines(), info.getFont(), rect.getWidth()), info.getFont());
		}

		int rectX = rect.getX() - 8;
		int rectY = rect.getY() + 18;
		int maxTextWidth = rect.getWidth() - 8;

		RenderTooltipExtEvent.Pre event = new RenderTooltipExtEvent.Pre(stack, info.getLines(), matrixStack, rectX, rectY, screenWidth, screenHeight, maxTextWidth, info.getFont(), comparison, index);
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

		RenderSystem.disableRescaleNormal();
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
		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(stack, info.getLines(), matrixStack, tooltipX, tooltipY, info.getFont(), backgroundColor, borderColorStart, borderColorEnd, comparison, index);
		MinecraftForge.EVENT_BUS.post(colorEvent);
		backgroundColor = colorEvent.getBackground();
		borderColorStart = colorEvent.getBorderStart();
		borderColorEnd = colorEvent.getBorderEnd();

		matrixStack.pushPose();
		Matrix4f mat = matrixStack.last().pose();

		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, backgroundColor, backgroundColor);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, borderColorStart, borderColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, borderColorEnd, borderColorEnd);

		MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.PostBackground(stack, info.getLines(), matrixStack, tooltipX, tooltipY, info.getFont(), tooltipTextWidth, tooltipHeight, comparison, index));

		IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
		matrixStack.translate(0.0D, 0.0D, zLevel);

		int tooltipTop = tooltipY;

		for (int lineNumber = 0; lineNumber < info.getLines().size(); ++lineNumber)
		{
			ITextProperties line = info.getLines().get(lineNumber);
			if (line != null)
			{
				info.getFont().drawInBatch(LanguageMap.getInstance().getVisualOrder(line), (float)tooltipX, (float)tooltipY, -1, true, mat, renderType, false, 0, 15728880);
			}

			if (lineNumber + 1 == info.getTitleLines())
			{
				tooltipY += 2;
			}

			tooltipY += 10;
		}

		renderType.endBatch();
		matrixStack.popPose();

		MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.PostText(stack, info.getLines(), matrixStack, tooltipX, tooltipTop, info.getFont(), tooltipTextWidth, tooltipHeight, comparison, index));

		RenderSystem.enableDepthTest();
		RenderSystem.enableRescaleNormal();
	}

	public static Rectangle2d calculateRect(final ItemStack stack, MatrixStack matrixStack, List<? extends ITextProperties> textLines, int mouseX, int mouseY,
												int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font)
	{
		return calculateRect(stack, matrixStack, textLines, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font, 0, false);
	}

	public static Rectangle2d calculateRect(final ItemStack stack, MatrixStack matrixStack, List<? extends ITextProperties> textLines, int mouseX, int mouseY,
												int screenWidth, int screenHeight, int maxTextWidth, FontRenderer font, int minWidth, boolean centeredTitle)
	{
		Rectangle2d rect = new Rectangle2d(0, 0, 0, 0);
		if (textLines == null || textLines.isEmpty() || stack == null)
		{
			return rect;
		}

		// Generate a tooltip event even though we aren't rendering anything in case the event handlers are modifying the input values.
		RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(stack, textLines, matrixStack, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font);
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

		int tooltipTextWidth = minWidth;

		if (centeredTitle)
		{
			textLines = centerTitle(textLines, font, minWidth);
		}

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

	public static List<? extends ITextProperties> centerTitle(List<? extends ITextProperties> textLines, FontRenderer font, int minWidth)
	{
		// Calculate tooltip width first.
		int tooltipWidth = minWidth;

		for (ITextProperties textLine : textLines)
		{
			if (textLine == null)
			{
				return textLines;
			}
			int textLineWidth = font.width(textLine);
			if (textLineWidth > tooltipWidth)
			{
				tooltipWidth = textLineWidth;
			}
		}

		// TODO: If the title is multiple lines, we need to extend this for each one.

		List<ITextProperties> result = new ArrayList<>(textLines);

		ITextComponent title = (ITextComponent)textLines.get(0);

		if (title != null)
		{
			while (font.width(result.get(0)) < tooltipWidth)
			{
				title = new StringTextComponent(" ").append(title).append(" ");
				if (title == null)
				{
					break;
				}
				
				result.set(0, new StringTextComponent("").append(title));
			}
		}
		return result;
	}
}
