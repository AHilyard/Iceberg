package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.PreExtResult;
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
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.datafixers.util.Either;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import com.mojang.math.Matrix4f;

public class Tooltips
{
	private static boolean initialized = false;
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
				int textLineWidth = component.getWidth(font);
				if (textLineWidth > width)
				{
					width = textLineWidth;
				}
			}
			return width;
		}
	}

	private static void init(Minecraft minecraft)
	{
		itemRenderer = minecraft.getItemRenderer();
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
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, backgroundColor, borderColorStart, borderColorEnd, comparison, constrain, false, 0);
	}

	public static void renderItemTooltip(final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd,
										boolean comparison, boolean constrain, boolean centeredTitle, int index)
	{
		if (info.components.isEmpty())
		{
			return;
		}

		// Initialize if needed.
		if (!initialized)
		{
			init(Minecraft.getInstance());
		}

		// Center the title now if needed.
		if (centeredTitle)
		{
			info = new TooltipInfo(centerTitle(info.getComponents(), info.getFont(), rect.getWidth()), info.getFont());
		}

		int rectX = rect.getX() + 4;
		int rectY = rect.getY() + 4;

		PreExtResult preResult = RenderTooltipEvents.PREEXT.invoker().onPre(stack, info.getComponents(), poseStack, rectX, rectY, screenWidth, screenHeight, info.getFont(), comparison, index);
		if (preResult.result() != InteractionResult.PASS)
		{
			return;
		}

		rectX = preResult.x();
		rectY = preResult.y();
		screenWidth = preResult.screenWidth();
		screenHeight = preResult.screenHeight();
		info.setFont(preResult.font());

		poseStack.pushPose();

		final int zLevel = 400;

		float f = itemRenderer.blitOffset;
		itemRenderer.blitOffset = zLevel;
		Matrix4f mat = poseStack.last().pose();

		ColorExtResult colors = RenderTooltipEvents.COLOREXT.invoker().onColor(stack, info.components, poseStack, rectX, rectY, info.getFont(), backgroundColorStart, backgroundColorEnd, borderColorStart, borderColorEnd, comparison, index);

		backgroundColorStart = colors.backgroundStart();
		backgroundColorEnd = colors.backgroundEnd();
		borderColorStart = colors.borderStart();
		borderColorEnd = colors.borderEnd();

		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY - 4, rectX + rect.getWidth() + 3, rectY - 3, backgroundColorStart, backgroundColorStart);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 4, backgroundColorEnd, backgroundColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 4, rectY - 3, rectX - 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 3, rectY - 3, rectX + rect.getWidth() + 4, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3 + 1, rectX - 3 + 1, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX + rect.getWidth() + 2, rectY - 3 + 1, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY - 3 + 1, borderColorStart, borderColorStart);
		GuiHelper.drawGradientRect(mat, zLevel, rectX - 3, rectY + rect.getHeight() + 2, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, borderColorEnd, borderColorEnd);

		BufferSource renderType = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		poseStack.translate(0.0D, 0.0D, zLevel);

		int tooltipTop = rectY;

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent textComponent = (ClientTooltipComponent)info.getComponents().get(componentNumber);
			textComponent.renderText(info.getFont(), rectX, tooltipTop, mat, renderType);
			tooltipTop += textComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		renderType.endBatch();
		poseStack.popPose();
		tooltipTop = rectY;

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent imageComponent = (ClientTooltipComponent)info.getComponents().get(componentNumber);
			imageComponent.renderImage(info.getFont(), rectX, tooltipTop, poseStack, itemRenderer, 400);
			tooltipTop += imageComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		itemRenderer.blitOffset = f;

		RenderTooltipEvents.POSTEXT.invoker().onPost(stack, info.getComponents(), poseStack, rectX, rectY, info.getFont(), rect.getWidth(), rect.getHeight(), comparison, index);
	}

	public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent,
	int mouseX, int screenWidth, int screenHeight, Font forcedFont, Font fallbackFont, int maxWidth)
	{
		return gatherTooltipComponents(stack, textElements, itemComponent, mouseX, screenWidth, screenHeight, forcedFont, fallbackFont, maxWidth, 0);
	}

	public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent,
																	   int mouseX, int screenWidth, int screenHeight, Font forcedFont, Font fallbackFont, int maxWidth, int index)
	{
		final Font font = forcedFont == null ? fallbackFont : forcedFont;

		List<Either<FormattedText, TooltipComponent>> elements = textElements.stream()
				.map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
				.collect(Collectors.toCollection(ArrayList::new));

		itemComponent.ifPresent(c -> elements.add(1, Either.right(c)));

		GatherResult eventResult = RenderTooltipEvents.GATHER.invoker().onGather(stack, screenWidth, screenHeight, elements, maxWidth, index);
		if (eventResult.result() != InteractionResult.PASS)
		{
			return List.of();
		}

		// Wrap text as needed.  First get the maximum width of all components.
		int tooltipTextWidth = eventResult.tooltipElements().stream()
				.mapToInt(either -> either.map(font::width, component -> 0))
				.max().orElse(0);

		boolean needsWrap = false;

		int tooltipX = mouseX + 12;
		if (tooltipX + tooltipTextWidth + 4 > screenWidth)
		{
			tooltipX = mouseX - 16 - tooltipTextWidth;
			if (tooltipX < 4)
			{
				if (mouseX > screenWidth / 2)
				{
					tooltipTextWidth = mouseX - 12 - 8;
				}
				else
				{
					tooltipTextWidth = screenWidth - 16 - mouseX;
				}
				needsWrap = true;
			}
		}

		if (eventResult.maxWidth() > 0 && tooltipTextWidth > eventResult.maxWidth())
		{
			tooltipTextWidth = eventResult.maxWidth();
			needsWrap = true;
		}

		final int tooltipTextWidthFinal = tooltipTextWidth;
		if (needsWrap)
		{
			return eventResult.tooltipElements().stream().flatMap(either -> either.map(text ->
								font.split(text, tooltipTextWidthFinal).stream().map(ClientTooltipComponent::create),
								component -> Stream.of(ClientTooltipComponent.create(component)))).toList();
		}

		return eventResult.tooltipElements().stream().map(either -> either.map(text ->
							ClientTooltipComponent.create(text instanceof Component ? ((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
							x -> {
								// First try using the create method, for vanilla and properly-implemented tooltip components.
								try {
									return ClientTooltipComponent.create(x);
								}
								// If that fails, attempt just casting it.
								catch (IllegalArgumentException e)
								{
									return (ClientTooltipComponent)x;
								}
							})).toList();
	}

	public static Rect2i calculateRect(final ItemStack stack, PoseStack poseStack, List<ClientTooltipComponent> components,
									   int mouseX, int mouseY,int screenWidth, int screenHeight, int maxTextWidth, Font font)
	{
		return calculateRect(stack, poseStack, components, mouseX, mouseY, screenWidth, screenHeight, maxTextWidth, font, 0, false);
	}

	public static Rect2i calculateRect(final ItemStack stack, PoseStack poseStack, List<ClientTooltipComponent> components,
									   int mouseX, int mouseY,int screenWidth, int screenHeight, int maxTextWidth, Font font, int minWidth, boolean centeredTitle)
	{
		Rect2i rect = new Rect2i(0, 0, 0, 0);
		if (components == null || components.isEmpty() || stack == null)
		{
			return rect;
		}

		// Generate a tooltip event even though we aren't rendering anything in case event handlers are modifying the input values.
		PreExtResult preResult = RenderTooltipEvents.PREEXT.invoker().onPre(stack, components, poseStack, mouseX, mouseY, screenWidth, screenHeight, font, false, 0);
		if (preResult.result() != InteractionResult.PASS)
		{
			return rect;
		}

		mouseX = preResult.x();
		mouseY = preResult.y();
		screenWidth = preResult.screenWidth();
		screenHeight = preResult.screenHeight();
		font = preResult.font();

		int tooltipTextWidth = minWidth;
		int tooltipHeight = components.size() == 1 ? -2 : 0;

		if (centeredTitle)
		{
			components = centerTitle(components, font, minWidth);
		}

		for (ClientTooltipComponent component : components)
		{
			int componentWidth = component.getWidth(font);
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

	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int minWidth)
	{
		// Calculate tooltip width first.
		int tooltipWidth = minWidth;

		for (ClientTooltipComponent clienttooltipcomponent : components)
		{
			if (clienttooltipcomponent == null)
			{
				return components;
			}
			int componentWidth = clienttooltipcomponent.getWidth(font);
			if (componentWidth > tooltipWidth)
			{
				tooltipWidth = componentWidth;
			}
		}

		// TODO: If the title is multiple lines, we need to extend this for each one.

		List<FormattedText> recomposedLines = StringRecomposer.recompose(List.of(components.get(0)));
		if (recomposedLines.isEmpty())
		{
			return components;
		}

		List<ClientTooltipComponent> result = new ArrayList<>(components);

		FormattedCharSequence title = Language.getInstance().getVisualOrder(recomposedLines.get(0));
		FormattedCharSequence space = FormattedCharSequence.forward(" ", Style.EMPTY);
		while (result.get(0).getWidth(font) < tooltipWidth)
		{
			title = FormattedCharSequence.fromList(List.of(space, title, space));
			if (title == null)
			{
				break;
			}
			result.set(0, ClientTooltipComponent.create(title));
		}
		return result;
	}
}