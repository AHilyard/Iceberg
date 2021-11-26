package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorResult;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureManager;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Style;

public class Tooltips
{
	private static boolean initialized = false;
	private static ItemRenderer itemRenderer = null;
	private static TextureManager textureManager = null;

	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private Font font;
		private List<ClientTooltipComponent> lines = new ArrayList<>();

		public TooltipInfo(List<ClientTooltipComponent> lines, Font font)
		{
			this.lines = lines;
			this.font = font;
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTitleLines() { return titleLines; }
		public Font getFont() { return font; }
		public List<ClientTooltipComponent> getLines() { return lines; }

		public void setFont(Font font) { this.font = font; }

		public int getMaxLineWidth()
		{
			int textWidth = 0;
			for (ClientTooltipComponent component : lines)
			{
				int textLineWidth = component.getWidth(font);
				if (textLineWidth > textWidth)
				{
					textWidth = textLineWidth;
				}
			}
			return textWidth;
		}

		private List<ClientTextTooltip> splitComponent(ClientTextTooltip component, int maxWidth, Font font)
		{
			FormattedText text = FormattedText.composite(StringRecomposer.recompose(Arrays.asList(component)));
			List<FormattedText> wrappedLines = font.getSplitter().splitLines(text, maxWidth, Style.EMPTY);
			List<ClientTextTooltip> result = new ArrayList<>();

			for (FormattedText wrappedLine : wrappedLines)
			{
				result.add(new ClientTextTooltip(Language.getInstance().getVisualOrder(wrappedLine)));
			}

			return result;
		}

		public void wrap(int maxWidth)
		{
			tooltipWidth = 0;
			List<ClientTooltipComponent> wrappedLines = new ArrayList<>();
			for (int i = 0; i < lines.size(); i++)
			{
				ClientTooltipComponent textLine = lines.get(i);

				// Only wrap text lines.
				// TODO: What to do with images that are too big?
				if (!(textLine instanceof ClientTextTooltip))
				{
					continue;
				}

				List<ClientTextTooltip> wrappedLine = splitComponent((ClientTextTooltip)textLine, maxWidth, font);
				if (i == 0)
				{
					titleLines = wrappedLine.size();
				}

				for (ClientTooltipComponent line : wrappedLine)
				{
					int lineWidth = line.getWidth(font);
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

	private static void init(Minecraft minecraft)
	{
		itemRenderer = minecraft.getItemRenderer();
		textureManager = minecraft.getTextureManager();
		initialized = true;
	}

	public static void renderItemTooltip(final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd)
	{
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, false);
	}

	public static void renderItemTooltip(final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison)
	{
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, borderColorStart, borderColorEnd, comparison, false);
	}

	public static void renderItemTooltip(final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison, boolean constrain)
	{
		// Initialize if needed.
		if (!initialized)
		{
			init(Minecraft.getInstance());
		}

		if (info.lines.isEmpty())
		{
			return;
		}

		int rectX = rect.getX() - 8;
		int rectY = rect.getY() + 18;
		int maxTextWidth = rect.getWidth() - 8;

		InteractionResult result = RenderTooltipEvents.PRE.invoker().onPre(stack, info.getLines(), poseStack, rectX, rectY, screenWidth, screenHeight, maxTextWidth, info.getFont(), comparison);
		if (result != InteractionResult.PASS)
		{
			return;
		}

		boolean needsWrap = false;

		int tooltipX = rectX + 12;
		int tooltipTextWidth = info.getMaxLineWidth();

		// Constrain the minimum width to the rect.
		if (constrain)
		{
			tooltipTextWidth = Math.max(info.getMaxLineWidth(), rect.getWidth() - 8);
		}

		if (tooltipX + tooltipTextWidth + 4 > screenWidth)
		{
			tooltipX = rectX - 16 - tooltipTextWidth;
			if (tooltipX < 4)
			{
				if (rectX > screenWidth / 2)
				{
					tooltipTextWidth = rectX - 20;
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

		poseStack.pushPose();
		int bgColor = 0xF0100010;
		int borderStart = 0x505000FF;
		int borderEnd = 0x5028007F;

		ColorResult colors = RenderTooltipEvents.COLOR.invoker().onColor(stack, info.lines, poseStack, tooltipX, tooltipY, info.getFont(), bgColor, borderStart, borderEnd, comparison);

		bgColor = colors.background();
		borderStart = colors.borderStart();
		borderEnd = colors.borderEnd();

		float f = itemRenderer.blitOffset;
		itemRenderer.blitOffset = 400.0F;
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferBuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f mat = poseStack.last().pose();
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY - 4, tooltipX + tooltipTextWidth + 3, tooltipY - 3, 400, bgColor, bgColor);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY + tooltipHeight + 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 4, 400, bgColor, bgColor);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, 400, bgColor, bgColor);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 4, tooltipY - 3, tooltipX - 3, tooltipY + tooltipHeight + 3, 400, bgColor, bgColor);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX + tooltipTextWidth + 3, tooltipY - 3, tooltipX + tooltipTextWidth + 4, tooltipY + tooltipHeight + 3, 400, bgColor, bgColor);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY - 3 + 1, tooltipX - 3 + 1, tooltipY + tooltipHeight + 3 - 1, 400, borderStart, borderEnd);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX + tooltipTextWidth + 2, tooltipY - 3 + 1, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3 - 1, 400, borderStart, borderEnd);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY - 3, tooltipX + tooltipTextWidth + 3, tooltipY - 3 + 1, 400, borderStart, borderStart);
		GuiHelper.drawGradientRect(mat, bufferBuilder, tooltipX - 3, tooltipY + tooltipHeight + 2, tooltipX + tooltipTextWidth + 3, tooltipY + tooltipHeight + 3, 400, borderEnd, borderEnd);
		RenderSystem.enableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		bufferBuilder.end();
		BufferUploader.end(bufferBuilder);
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();

		MultiBufferSource.BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		poseStack.translate(0.0D, 0.0D, 400.0D);
		int v = tooltipY;

		ClientTooltipComponent clientTooltipComponent3;
		for (int i = 0; i < info.getLines().size(); ++i)
		{
			clientTooltipComponent3 = (ClientTooltipComponent)info.getLines().get(i);
			clientTooltipComponent3.renderText(info.getFont(), tooltipX, v, mat, bufferSource);
			v += clientTooltipComponent3.getHeight() + (i == 0 ? 2 : 0);
		}

		bufferSource.endBatch();
		poseStack.popPose();
		v = tooltipY;

		for (int i = 0; i < info.getLines().size(); ++i)
		{
			clientTooltipComponent3 = (ClientTooltipComponent)info.getLines().get(i);
			clientTooltipComponent3.renderImage(info.getFont(), tooltipX, v, poseStack, itemRenderer, 400, textureManager);
			v += clientTooltipComponent3.getHeight() + (i == 0 ? 2 : 0);
		}

		itemRenderer.blitOffset = f;

		RenderTooltipEvents.POST.invoker().onPost(stack, info.getLines(), poseStack, tooltipX, tooltipY, info.getFont(), tooltipTextWidth, tooltipHeight, comparison);
	}

	public static Rect2i calculateRect(final ItemStack stack, PoseStack mStack, List<ClientTooltipComponent> textLines, int mouseX, int mouseY,
												int screenWidth, int screenHeight, int maxTextWidth, Font font)
	{
		Rect2i rect = new Rect2i(0, 0, 0, 0);
		if (textLines == null || textLines.isEmpty() || stack == null)
		{
			return rect;
		}

		int tooltipTextWidth = 0;

		for (ClientTooltipComponent textLine : textLines)
		{
			int textLineWidth = textLine.getWidth(font);
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
			TooltipInfo info = new TooltipInfo(textLines, font);
			info.wrap(tooltipTextWidth);
			
			tooltipTextWidth = info.tooltipWidth;
			textLines = info.lines;

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
