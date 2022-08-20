package com.anthonyhilyard.iceberg.util;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nonnull;

import org.lwjgl.BufferUtils;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.Rectangle2d;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Matrix4f;
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
			this.tooltipWidth = getMaxLineWidth();
		}

		public TooltipInfo(List<? extends ITextProperties> lines, FontRenderer font, int titleLines)
		{
			this.lines = lines;
			this.font = font;
			this.titleLines = titleLines;
			this.tooltipWidth = getMaxLineWidth();
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTooltipHeight() { return lines.size() > titleLines ? lines.size() * 10 + 2 : 8; }
		public int getTitleLines() { return titleLines; }
		public FontRenderer getFont() { return font; }
		public List<? extends ITextProperties> getLines() { return lines; }

		public void setFont(FontRenderer font) { this.font = font; }

		public int getMaxLineWidth()
		{
			return getMaxLineWidth(0);
		}

		public int getMaxLineWidth(int minWidth)
		{
			int textWidth = minWidth;
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
			wrap(maxWidth, 0);
		}

		public void wrap(int maxWidth, int minWidth)
		{
			tooltipWidth = minWidth;
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

			if (tooltipWidth > maxWidth)
			{
				tooltipWidth = maxWidth;
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
		renderItemTooltip(stack, matrixStack, info, rect, screenWidth, screenHeight, backgroundColor, backgroundColor, borderColorStart, borderColorEnd, comparison, constrain, false, 0);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, MatrixStack matrixStack, TooltipInfo info,
										Rectangle2d rect, int screenWidth, int screenHeight,
										int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd,
										boolean comparison, boolean constrain, boolean centeredTitle, int index)
	{
		if (info.getLines().isEmpty())
		{
			return;
		}

		int rectX = rect.getX() + 4;
		int rectY = rect.getY() + 4;
		int maxTextWidth = rect.getWidth();

		RenderTooltipExtEvent.Pre preEvent = new RenderTooltipExtEvent.Pre(stack, info.getLines(), matrixStack, rectX, rectY, screenWidth, screenHeight, maxTextWidth, info.getFont(), comparison, index);
		if (MinecraftForge.EVENT_BUS.post(preEvent))
		{
			return;
		}

		rectX = preEvent.getX();
		rectY = preEvent.getY();
		screenWidth = preEvent.getScreenWidth();
		screenHeight = preEvent.getScreenHeight();
		maxTextWidth = preEvent.getMaxWidth();
		info.setFont(preEvent.getFontRenderer());

		RenderSystem.disableDepthTest();
		int tooltipTextWidth = info.getMaxLineWidth();

		// Constrain the minimum width to the rect.
		if (constrain)
		{
			//tooltipTextWidth = Math.max(tooltipTextWidth, rect.getWidth());
		}

		if (tooltipTextWidth > maxTextWidth)
		{
			info.wrap(maxTextWidth);
		}

		// Center the title now if needed.
		if (centeredTitle)
		{
			info = new TooltipInfo(centerTitle(info.getLines(), info.getFont(), rect.getWidth(), info.getTitleLines()), info.getFont(), info.getTitleLines());
		}

		//rect = new Rectangle2d(rect.getX(), rect.getY(), info.getTooltipWidth(), info.getTooltipHeight());

		final int zLevel = 400;
		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(stack, info.getLines(), matrixStack, rectX, rectY, info.getFont(), backgroundColorStart, backgroundColorEnd, borderColorStart, borderColorEnd, comparison, index);
		MinecraftForge.EVENT_BUS.post(colorEvent);

		backgroundColorStart = colorEvent.getBackgroundStart();
		backgroundColorEnd = colorEvent.getBackgroundEnd();
		borderColorStart = colorEvent.getBorderStart();
		borderColorEnd = colorEvent.getBorderEnd();

		matrixStack.pushPose();
		Matrix4f mat = matrixStack.last().pose();

		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 4, rectX + rect.getWidth() + 3, rectY - 3, backgroundColorStart, backgroundColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 4, backgroundColorEnd, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 4, rectY - 3, rectX - 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 3, rectY - 3, rectX + rect.getWidth() + 4, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3 + 1, rectX - 3 + 1, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 2, rectY - 3 + 1, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY - 3 + 1, borderColorStart, borderColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 2, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, borderColorEnd, borderColorEnd);

		MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.PostBackground(stack, info.getLines(), matrixStack, rectX, rectY, info.getFont(), rect.getWidth(), rect.getHeight(), comparison, index));

		IRenderTypeBuffer.Impl renderType = IRenderTypeBuffer.immediate(Tessellator.getInstance().getBuilder());
		matrixStack.translate(0.0D, 0.0D, zLevel);

		int tooltipTop = rectY;

		for (int lineNumber = 0; lineNumber < info.getLines().size(); ++lineNumber)
		{
			ITextProperties line = info.getLines().get(lineNumber);
			if (line != null)
			{
				info.getFont().drawInBatch(LanguageMap.getInstance().getVisualOrder(line), (float)rectX, (float)rectY, -1, true, mat, renderType, false, 0, 0xF000F0);
			}

			if (lineNumber + 1 == info.getTitleLines())
			{
				rectY += 2;
			}

			rectY += 10;
		}

		renderType.endBatch();
		matrixStack.popPose();

		MinecraftForge.EVENT_BUS.post(new RenderTooltipExtEvent.PostText(stack, info.getLines(), matrixStack, rectX, tooltipTop, info.getFont(), rect.getWidth(), rect.getHeight(), comparison, index));

		RenderSystem.enableDepthTest();

		// Since many mods improperly implement tooltip icons, this helps to fix them by forcing the Z translation to 400.
		FloatBuffer matrixBuffer = BufferUtils.createFloatBuffer(16);
		Matrix4f lastMatrix = matrixStack.last().pose();
		lastMatrix.store(matrixBuffer);
		float matrixX = matrixBuffer.get(3), matrixY = matrixBuffer.get(7);
		lastMatrix.setTranslation(matrixX, matrixY, 400);
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

		TooltipInfo info = new TooltipInfo(textLines, font);
		int tooltipTextWidth = info.getMaxLineWidth(minWidth);

		boolean needsWrap = false;

		int tooltipX = mouseX + 12;
		int tooltipY = mouseY - 12;
		if (tooltipX + tooltipTextWidth > screenWidth)
		{
			tooltipX -= 28 + tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (mouseX > screenWidth / 2)
				{
					tooltipTextWidth = mouseX - 20;
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
			info.wrap(tooltipTextWidth, minWidth);
			tooltipTextWidth = info.getTooltipWidth();
		}

		if (centeredTitle)
		{
			info = new TooltipInfo(centerTitle(info.getLines(), info.getFont(), tooltipTextWidth, info.getTitleLines()), info.getFont(), info.getTitleLines());
			tooltipTextWidth = info.getTooltipWidth();
		}

		int tooltipHeight = info.getTooltipHeight();

		if (tooltipX < 6)
		{
			tooltipX = 6;
		}

		if (tooltipY < 6)
		{
			tooltipY = 6;
		}

		if (tooltipY + tooltipHeight + 6 > screenHeight)
		{
			tooltipY = screenHeight - tooltipHeight - 6;
		}

		rect = new Rectangle2d(tooltipX - 2, tooltipY - 4, tooltipTextWidth, tooltipHeight);
		return rect;
	}

	public static List<? extends ITextProperties> centerTitle(List<? extends ITextProperties> textLines, FontRenderer font, int width, int titleLines)
	{
		List<ITextProperties> result = new ArrayList<>(textLines);

		for (int i = 0; i < titleLines; i++)
		{
			ITextProperties title = textLines.get(i);

			if (title != null)
			{
				while (font.width(result.get(i)) < width)
				{
					title = ITextProperties.composite(new StringTextComponent(" "), title, new StringTextComponent(" "));
					if (title == null)
					{
						break;
					}
					
					result.set(i, title);
				}
			}
		}
		return result;
	}
}
