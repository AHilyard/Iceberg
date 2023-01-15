package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.ColorExtResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.PreExtResult;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.fabric.api.client.rendering.v1.TooltipComponentCallback;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.joml.Matrix4f;

public class Tooltips
{
	public record TooltipColors(TextColor backgroundColorStart, TextColor backgroundColorEnd, TextColor borderColorStart, TextColor borderColorEnd) {}
	private static final TooltipColors DEFAULT_COLORS = new TooltipColors(TextColor.fromRgb(0xF0100010), TextColor.fromRgb(0xF0100010), TextColor.fromRgb(0x505000FF), TextColor.fromRgb(0x5028007F));

	private static final FormattedCharSequence SPACE = FormattedCharSequence.forward(" ", Style.EMPTY);
	private static ItemRenderer itemRenderer = null;
	private static boolean tooltipWidthWarningShown = false;

	public static TooltipColors currentColors = DEFAULT_COLORS;

	public static class TitleBreakComponent implements TooltipComponent, ClientTooltipComponent
	{
		@Override
		public int getHeight() { return 0; }

		@Override
		public int getWidth(Font font) { return 0; }

		public static void registerFactory()
		{
			TooltipComponentCallback.EVENT.register(data -> {
				if (data instanceof TitleBreakComponent titleBreakComponent)
				{
					return titleBreakComponent;
				}
				return null;
			});
		}
	}

	public static interface InlineComponent { }

	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private Font font;
		private List<ClientTooltipComponent> components = new ArrayList<>();

		public TooltipInfo(List<ClientTooltipComponent> components, Font font)
		{
			this(components, font, calculateTitleLines(components));
		}

		public TooltipInfo(List<ClientTooltipComponent> components, Font font, int titleLines)
		{
			this.components = components;
			this.font = font;
			this.titleLines = titleLines;
			this.tooltipWidth = getMaxLineWidth();
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTooltipHeight() { return components.size() > titleLines ? components.size() * 10 + 2 : 8; }
		public int getTitleLines() { return titleLines; }
		public Font getFont() { return font; }
		public List<ClientTooltipComponent> getComponents() { return components; }

		public void setFont(Font font) { this.font = font; }

		public int getMaxLineWidth()
		{
			return getMaxLineWidth(0);
		}

		public int getMaxLineWidth(int minWidth)
		{
			int textWidth = minWidth;
			for (ClientTooltipComponent component : components)
			{
				int componentWidth = component.getWidth(font);
				if (componentWidth > textWidth)
				{
					textWidth = componentWidth;
				}
			}
			return textWidth;
		}
	}

	public static int calculateTitleLines(List<ClientTooltipComponent> components)
	{
		if (components == null || components.isEmpty())
		{
			return 0;
		}

		// Determine the number of "title lines".  This will be the number of text components before the first TitleBreakComponent.
		// If for some reason there is no TitleBreakComponent, we'll default to 1.
		int titleLines = 0;
		boolean foundTitleBreak = false;
		for (ClientTooltipComponent component : components)
		{
			if (component instanceof ClientTextTooltip)
			{
				titleLines++;
			}
			else if (component instanceof TitleBreakComponent)
			{
				foundTitleBreak = true;
				break;
			}
		}

		// We didn't find a title break (shouldn't happen normally), so default to 1.
		if (!foundTitleBreak)
		{
			titleLines = 1;
		}

		return titleLines;
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
		renderItemTooltip(stack, poseStack, info, rect, screenWidth, screenHeight, backgroundColor, backgroundColor, borderColorStart, borderColorEnd, comparison, constrain, false, 0);
	}

	public static void renderItemTooltip(@Nonnull final ItemStack stack, PoseStack poseStack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd,
										boolean comparison, boolean constrain, boolean centeredTitle, int index)
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

		// Center the title now if needed.
		if (centeredTitle)
		{
			info = new TooltipInfo(centerTitle(info.getComponents(), info.getFont(), info.getMaxLineWidth(), info.getTitleLines()), info.getFont(), info.getTitleLines());
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

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix4f = poseStack.last().pose();

		ColorExtResult colors = RenderTooltipEvents.COLOREXT.invoker().onColor(stack, info.components, poseStack, rectX, rectY, info.getFont(), backgroundColorStart, backgroundColorEnd, borderColorStart, borderColorEnd, comparison, index);

		backgroundColorStart = colors.backgroundStart();
		backgroundColorEnd = colors.backgroundEnd();
		borderColorStart = colors.borderStart();
		borderColorEnd = colors.borderEnd();

		currentColors = new TooltipColors(TextColor.fromRgb(backgroundColorStart), TextColor.fromRgb(backgroundColorEnd), TextColor.fromRgb(borderColorStart), TextColor.fromRgb(borderColorEnd));

		TooltipRenderUtil.renderTooltipBackground((matrix, bufferBuilder, left, top, right, bottom, z, startColor, endColor) -> {
			GuiHelper.drawGradientRect(matrix, bufferBuilder, left, top, right, bottom, z, startColor, endColor);
		}, matrix4f, bufferbuilder, rectX, rectY, rect.getWidth(), rect.getHeight(), zLevel);

		RenderSystem.enableDepthTest();
		RenderSystem.disableTexture();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		BufferUploader.drawWithShader(bufferbuilder.end());
		RenderSystem.disableBlend();
		RenderSystem.enableTexture();
		BufferSource bufferSource = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
		poseStack.translate(0.0f, 0.0f, zLevel);

		int tooltipTop = rectY;
		int titleLines = info.getTitleLines();

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent textComponent = info.getComponents().get(componentNumber);
			textComponent.renderText(preResult.font(), rectX, tooltipTop, matrix4f, bufferSource);
			tooltipTop += textComponent.getHeight();
			if ((textComponent instanceof ClientTextTooltip || textComponent instanceof InlineComponent) && titleLines > 0)
			{
				titleLines -= (textComponent instanceof InlineComponent) ? 2 : 1;
				if (titleLines <= 0)
				{
					tooltipTop += 2;
				}
			}
		}

		bufferSource.endBatch();
		poseStack.popPose();
		tooltipTop = rectY;

		for (int componentNumber = 0; componentNumber < info.getComponents().size(); ++componentNumber)
		{
			ClientTooltipComponent imageComponent = (ClientTooltipComponent)info.getComponents().get(componentNumber);
			imageComponent.renderImage(info.getFont(), rectX, tooltipTop, poseStack, itemRenderer, zLevel);
			tooltipTop += imageComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		itemRenderer.blitOffset = f;

		RenderTooltipEvents.POSTEXT.invoker().onPost(stack, info.getComponents(), poseStack, rectX, rectY, info.getFont(), rect.getWidth(), rect.getHeight(), comparison, index);
	}

	private static ClientTooltipComponent getClientComponent(TooltipComponent componentData)
	{
		ClientTooltipComponent result = null;

		// First try using the create method, for vanilla and mixed-in tooltip components.
		try { result = ClientTooltipComponent.create(componentData); }
		catch (IllegalArgumentException e) { }

		// If that fails, try using the Fabric API event.
		if (result == null)
		{
			result = TooltipComponentCallback.EVENT.invoker().getComponent(componentData);
		}

		// Finally, if all else fails, try casting (some mods implement it this way).
		if (result == null)
		{
			try { result = (ClientTooltipComponent)componentData; }
			catch (ClassCastException e) { }
		}

		if (result == null)
		{
			throw new IllegalArgumentException("Unknown TooltipComponent");
		}
		return result;
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
				.mapToInt(either -> either.map(component -> {
					try
					{
						return font.width(component);
					}
					catch (Exception e)
					{
						// Log this exception, but only once.
						if (!tooltipWidthWarningShown)
						{
							Loader.LOGGER.error("Error rendering tooltip component: \n" + ExceptionUtils.getStackTrace(e));
							tooltipWidthWarningShown = true;
						}
						return 0;
					}
				}, component -> 0))
				.max()
				.orElse(0);

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
								component -> Stream.of(getClientComponent(component)))).toList();
		}

		return eventResult.tooltipElements().stream().map(either -> either.map(text ->
							ClientTooltipComponent.create(text instanceof Component ? ((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
							Tooltips::getClientComponent)).toList();
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
		int titleLines = calculateTitleLines(components);

		if (centeredTitle)
		{
			// Calculate the current tooltip width prior to centering.
			for (ClientTooltipComponent component : components)
			{
				int componentWidth = component.getWidth(font);
				if (componentWidth > tooltipTextWidth)
				{
					tooltipTextWidth = componentWidth;
				}
			}
			components = centerTitle(components, font, tooltipTextWidth, titleLines);
		}

		tooltipTextWidth = minWidth;

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

	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int width)
	{
		return centerTitle(components, font, width, calculateTitleLines(components));
	}

	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int width, int titleLines)
	{
		List<ClientTooltipComponent> result = new ArrayList<>(components);

		if (components.isEmpty() || titleLines <= 0 || titleLines >= components.size())
		{
			return result;
		}

		// Find the title component, which is the first text component.
		int titleIndex = 0;
		for (ClientTooltipComponent clienttooltipcomponent : components)
		{
			if (clienttooltipcomponent instanceof ClientTextTooltip)
			{
				break;
			}
			titleIndex++;
		}

		for (int i = 0; i < titleLines; i++)
		{
			ClientTooltipComponent titleComponent = components.get(titleIndex + i);

			if (titleComponent != null)
			{
				List<FormattedText> recomposedLines = StringRecomposer.recompose(List.of(titleComponent));
				if (recomposedLines.isEmpty())
				{
					return components;
				}

				FormattedCharSequence title = Language.getInstance().getVisualOrder(recomposedLines.get(0));

				while (ClientTooltipComponent.create(title).getWidth(font) < width)
				{
					title = FormattedCharSequence.fromList(List.of(SPACE, title, SPACE));
					if (title == null)
					{
						break;
					}
				}
				result.set(titleIndex + i, ClientTooltipComponent.create(title));
			}
		}
		return result;
	}
}