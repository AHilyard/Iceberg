package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Either;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;

import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.client.gui.GuiUtils;

public class Tooltips
{
	private static ItemRenderer itemRenderer = null;

	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private Font font;
		private List<ClientTooltipComponent> components = new ArrayList<>();

		public TooltipInfo(List<ClientTooltipComponent> components, Font font)
		{
			this.components = components;
			this.font = font;
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTitleLines() { return titleLines; }
		public Font getFont() { return font; }
		public List<ClientTooltipComponent> getComponents() { return components; }

		public void setFont(Font font) { this.font = font; }

		public int getMaxLineWidth()
		{
			int width = 0;
			for (ClientTooltipComponent component : components)
			{
				int componentWidth = component.getWidth(font);
				if (componentWidth > width)
				{
					width = componentWidth;
				}
			}
			return width;
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

	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColor, int borderColorStart, int borderColorEnd, boolean comparison, boolean constrain)
	{
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, backgroundColor, borderColorStart, borderColorEnd, comparison, constrain);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
									Rect2i rect, int screenWidth, int screenHeight,
									int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd, boolean comparison, boolean constrain)
	{
		if (info.getComponents().isEmpty())
		{
			return;
		}

		// Grab the itemRenderer now if needed.
		if (itemRenderer == null)
		{
			itemRenderer = Minecraft.getInstance().getItemRenderer();
		}

		int rectX = rect.getX() + 4;
		int rectY = rect.getY() + 4;
	
		RenderTooltipExtEvent.Pre preEvent = new RenderTooltipExtEvent.Pre(stack, poseStack, rectX, rectY, screenWidth, screenHeight, info.getFont(), info.getComponents(), comparison);
		if (MinecraftForge.EVENT_BUS.post(preEvent))
		{
			return;
		}

		rectX = preEvent.getX();
		rectY = preEvent.getY();
		screenWidth = preEvent.getScreenWidth();
		screenHeight = preEvent.getScreenHeight();
		info.setFont(preEvent.getFont());

		poseStack.pushPose();

		final int zLevel = 400;
		float f = itemRenderer.blitOffset;
		itemRenderer.blitOffset = zLevel;
		Matrix4f mat = poseStack.last().pose();

		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(stack, poseStack, rectX, rectY, info.getFont(), backgroundColorStart, backgroundColorEnd, borderColorStart, borderColorEnd, info.getComponents(), comparison);
		MinecraftForge.EVENT_BUS.post(colorEvent);

		backgroundColorStart = colorEvent.getBackgroundStart();
		backgroundColorEnd = colorEvent.getBackgroundEnd();
		borderColorStart = colorEvent.getBorderStart();
		borderColorEnd = colorEvent.getBorderEnd();

		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 4, rectX + rect.getWidth() + 3, rectY - 3, backgroundColorStart, backgroundColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 4, backgroundColorEnd, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 4, rectY - 3, rectX - 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 3, rectY - 3, rectX + rect.getWidth() + 4, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3 + 1, rectX - 3 + 1, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 2, rectY - 3 + 1, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY - 3 + 1, borderColorStart, borderColorStart);
		GuiUtils.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 2, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, borderColorEnd, borderColorEnd);

		BufferSource renderType = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		poseStack.translate(0.0D, 0.0D, zLevel);

		int tooltipTop = rectY;

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent textComponent = info.getComponents().get(componentNumber);
			textComponent.renderText(preEvent.getFont(), rectX, tooltipTop, mat, renderType);
			tooltipTop += textComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		renderType.endBatch();
		poseStack.popPose();
		tooltipTop = rectY;

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent imageComponent = info.getComponents().get(componentNumber);
			imageComponent.renderImage(preEvent.getFont(), rectX, tooltipTop, poseStack, itemRenderer, 400);
			tooltipTop += imageComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		itemRenderer.blitOffset = f;

		RenderTooltipExtEvent.Post postEvent = new RenderTooltipExtEvent.Post(stack, poseStack, rectX, rectY, info.getFont(), rect.getWidth(), rect.getHeight(), info.getComponents(), comparison);
		MinecraftForge.EVENT_BUS.post(postEvent);
	}

	public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent,
																	   int mouseX, int screenWidth, int screenHeight, @Nullable Font forcedFont, Font fallbackFont, int maxWidth)
	{
		Font font = ForgeHooksClient.getTooltipFont(forcedFont, stack, fallbackFont);
		List<Either<FormattedText, TooltipComponent>> elements = textElements.stream()
				.map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
				.collect(Collectors.toCollection(ArrayList::new));

		itemComponent.ifPresent(c -> elements.add(1, Either.right(c)));

		var event = new RenderTooltipEvent.GatherComponents(stack, screenWidth, screenHeight, elements, maxWidth);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return List.of();
		}

		int tooltipTextWidth = event.getTooltipElements().stream()
				.mapToInt(either -> either.map(font::width, component -> 0))
				.max()
				.orElse(0);

		boolean needsWrap = false;

		int tooltipX = mouseX + 12;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth)
		{
			tooltipX = mouseX - 16 - tooltipTextWidth;
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
			{
				if (mouseX > screenWidth / 2)
					tooltipTextWidth = mouseX - 12 - 8;
				else
					tooltipTextWidth = screenWidth - 16 - mouseX;
				needsWrap = true;
			}
		}

		if (event.getMaxWidth() > 0 && tooltipTextWidth > event.getMaxWidth())
		{
			tooltipTextWidth = event.getMaxWidth();
			needsWrap = true;
		}

		final int tooltipTextWidthFinal = tooltipTextWidth;
		if (needsWrap)
		{
			return event.getTooltipElements().stream()
					.flatMap(either -> either.map(
							text -> font.split(text, tooltipTextWidthFinal).stream().map(ClientTooltipComponent::create),
							component -> Stream.of(ClientTooltipComponent.create(component))
					))
					.toList();
		}
		return event.getTooltipElements().stream()
				.map(either -> either.map(
						text -> ClientTooltipComponent.create(text instanceof Component ? ((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
						ClientTooltipComponent::create
				))
				.toList();
	}

	public static Rect2i calculateRect(final ItemStack stack, PoseStack poseStack, List<ClientTooltipComponent> components,
									   int mouseX, int mouseY,int screenWidth, int screenHeight, int maxTextWidth, Font font)
	{
		Rect2i rect = new Rect2i(0, 0, 0, 0);
		if (components == null || components.isEmpty() || stack == null)
		{
			return rect;
		}

		// Generate a tooltip event even though we aren't rendering anything in case event handlers are modifying the input values.
		RenderTooltipEvent.Pre event = new RenderTooltipEvent.Pre(stack, poseStack, mouseX, mouseY, screenWidth, screenHeight, font, components);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return rect;
		}

		mouseX = event.getX();
		mouseY = event.getY();
		screenWidth = event.getScreenWidth();
		screenHeight = event.getScreenHeight();
		font = event.getFont();

		int tooltipTextWidth = 0;
		int tooltipHeight = components.size() == 1 ? -2 : 0;

		for (ClientTooltipComponent component : components)
		{
			int componentWidth = component.getWidth(event.getFont());
			if (componentWidth > tooltipTextWidth)
			{
				tooltipTextWidth = componentWidth;
			}

			tooltipHeight += component.getHeight();
		}

		int tooltipX = mouseX + 12;
		int tooltipY = mouseY - 12;
		if (tooltipX + tooltipTextWidth > screenWidth)
		{
			tooltipX -= 28 + tooltipTextWidth;
		}

		if (tooltipY + tooltipHeight + 6 > screenHeight)
		{
			tooltipY = screenHeight - tooltipHeight - 6;
		}

		rect = new Rect2i(tooltipX - 2, tooltipY - 4, tooltipTextWidth, tooltipHeight);
		return rect;
	}
}
