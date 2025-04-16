package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.anthonyhilyard.iceberg.Iceberg;
import com.anthonyhilyard.iceberg.component.IExtendedText;
import com.anthonyhilyard.iceberg.component.TitleBreakComponent;
import com.anthonyhilyard.iceberg.component.IExtendedText.TextAlignment;
import com.anthonyhilyard.iceberg.events.client.RegisterTooltipComponentFactoryEvent;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.client.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.mixin.GuiGraphicsInvoker;
import com.mojang.datafixers.util.Either;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector2i;
import org.joml.Vector2ic;

public class Tooltips
{
	public record TooltipColors(int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd) {}
	public static final TooltipColors DEFAULT_COLORS = new TooltipColors(0xF0100010, 0xF0100010, 0x505000FF, 0x5028007F);

	private static final FormattedCharSequence SPACE = FormattedCharSequence.forward(" ", Style.EMPTY);
	private static boolean tooltipWidthWarningShown = false;

	public static TooltipColors currentColors = DEFAULT_COLORS;

	@Deprecated(forRemoval = true, since = "1.3.0")
	public static class TooltipInfo
	{
		private int tooltipWidth;
		private int titleLines;
		private int titleStart;
		private Font font;
		private List<ClientTooltipComponent> components;

		public TooltipInfo(List<ClientTooltipComponent> components, Font font)
		{
			this(components, font, calculateTitleLines(components));
		}

		public TooltipInfo(List<ClientTooltipComponent> components, Font font, int titleLines)
		{
			this(components, font, titleLines, calculateTitleStart(components));
		}

		public TooltipInfo(List<ClientTooltipComponent> components, Font font, int titleLines, int titleStart)
		{
			this.components = components;
			this.font = font;
			this.titleLines = titleLines;
			this.titleStart = titleStart;
			this.tooltipWidth = getMaxLineWidth();
		}

		public int getTooltipWidth() { return tooltipWidth; }
		public int getTooltipHeight() { return components.size() > titleLines ? components.size() * 10 + 2 : 8; }
		public int getTitleLines() { return titleLines; }
		public int getTitleStart() { return titleStart; }
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

	public record TooltipRenderContext(int maxWidth, int maxHeight, boolean comparison, int index) {}
	public static final TooltipRenderContext EMPTY_CONTEXT = new TooltipRenderContext(0, 0, false, 0);
	public static final TooltipRenderContext CALCULATE_RECT_CONTEXT = new TooltipRenderContext(0, 0, false, 0);

	private static TooltipRenderContext currentRenderContext = EMPTY_CONTEXT;
	public static TooltipRenderContext getCurrentRenderContext()
	{
		return currentRenderContext;
	}

	private static Rect2i currentRect = new Rect2i(0, 0, 0, 0);
	public static void setCurrentRect(int x, int y, int width, int height)
	{
		currentRect.setX(x);
		currentRect.setY(y);
		currentRect.setWidth(width);
		currentRect.setHeight(height);
	}

	public static Rect2i getCurrentRect() { return currentRect; }

	private static boolean tooltipsOnscreen = false;
	public static boolean anyTooltipsVisible() { return tooltipsOnscreen; }
	public static void setAnyTooltipsVisible(boolean visible) { tooltipsOnscreen = visible; }

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
			else
			{
				titleLines = 0;
			}
		}

		// We didn't find a title break (shouldn't happen normally), so default to 1.
		if (!foundTitleBreak)
		{
			titleLines = 1;
		}

		return titleLines;
	}

	public static int calculateTitleStart(List<ClientTooltipComponent> components)
	{
		if (components == null || components.isEmpty())
		{
			return 0;
		}

		// Determine the index of the first "title line".
		// If there is a title break component, it will be the first text component in a contiguous
		// collection of text components before that.  Otherwise, it's just the first text component.
		int firstTextIndex = -1;
		int currentTextRunStart = -1;

		for (int i = 0; i < components.size(); i++)
		{
			ClientTooltipComponent component = components.get(i);

			if (component instanceof TitleBreakComponent)
			{
				// If we're in a run of text, return its start; otherwise 0.
				return currentTextRunStart == -1 ? 0 : currentTextRunStart;
			}
			else if (component instanceof ClientTextTooltip)
			{
				// Start a run if we aren't in one.
				if (currentTextRunStart == -1)
				{
					currentTextRunStart = i;
				}
				// Track the first text index if we haven't set it yet.
				if (firstTextIndex == -1)
				{
					firstTextIndex = i;
				}
			}
			else
			{
				// Non-text (not a break), so end the current run.
				currentTextRunStart = -1;
			}
		}

		// If we finished without finding a TitleBreakComponent,
		// return the first text index or 0 if there was no text at all.
		return firstTextIndex == -1 ? 0 : firstTextIndex;
	}

	public static int getTitleOffset(int tooltipWidth, int textWidth, int leftPadding, int rightPadding, TextAlignment textAlignment)
	{
		int offset = leftPadding;

		switch (textAlignment)
		{
			case CENTER:
				offset += (tooltipWidth - textWidth - leftPadding - rightPadding) / 2;
				break;
			case RIGHT:
				offset += tooltipWidth - textWidth - rightPadding;
				break;
			default:
				break;
		}

		return Math.max(leftPadding, offset);
	}

	public static int getTitleWidth(ClientTextTooltip title, Font font)
	{
		int textWidth = font.width(title.text);

		if (title instanceof IExtendedText extendedTitle)
		{
			int leftPadding = extendedTitle.getLeftPadding();
			int rightPadding = extendedTitle.getRightPadding();

			int tooltipWidth = Tooltips.getCurrentRect().getWidth();
			if (tooltipWidth == 0)
			{
				textWidth += leftPadding + rightPadding;
			}
			else
			{
				textWidth += getTitleOffset(tooltipWidth, textWidth, leftPadding, rightPadding, extendedTitle.getAlignment()) + rightPadding;
			}
		}
		return textWidth;
	}

	private static class TooltipRectPositioner implements ClientTooltipPositioner
	{
		private final Rect2i tooltipRect;
		public TooltipRectPositioner(Rect2i tooltipRect) { this.tooltipRect = tooltipRect; }

		@Override
		public Vector2ic positionTooltip(int screenWidth, int screenHeight, int mouseX, int mouseY, int tooltipWidth, int tooltipHeight)
		{
			return new Vector2i(tooltipRect.getX() + 2, tooltipRect.getY());
		}
	}

	@Deprecated(forRemoval = true, since = "1.3.0")
	public static void renderItemTooltip(@NotNull final ItemStack stack, TooltipInfo info,
										Rect2i rect, int screenWidth, int screenHeight,
										int backgroundColorStart, int backgroundColorEnd, int borderColorStart, int borderColorEnd,
										GuiGraphics graphics, ClientTooltipPositioner positioner,
										boolean comparison, boolean constrain, boolean centeredTitle, int index)
	{
		renderItemTooltip(stack, info.getFont(), info.getComponents(), rect, graphics, positioner, comparison, index);
	}

	@Deprecated(forRemoval = true, since = "1.3.0")
	public static void renderItemTooltip(@NotNull final ItemStack stack, TooltipInfo info,
										Rect2i rect, GuiGraphics graphics, ClientTooltipPositioner positioner,
										boolean comparison, boolean constrain, int index)
	{
		renderItemTooltip(stack, info.getFont(), info.getComponents(), rect, graphics, positioner, comparison, index);
	}

	public static void renderItemTooltip(@NotNull final ItemStack stack, Font font, List<ClientTooltipComponent> components,
										Rect2i rect, GuiGraphics graphics, ClientTooltipPositioner positioner,
										boolean comparison, int index)
	{
		// Set the current render context.
		currentRenderContext = new TooltipRenderContext(rect.getWidth(), rect.getHeight(), comparison, index);

		if (graphics instanceof GuiGraphicsInvoker graphicsInvoker && graphics instanceof ITooltipAccess tooltipAccess)
		{
			tooltipAccess.setIcebergTooltipStack(stack);
			graphicsInvoker.invokeRenderTooltipInternal(font, components, rect.getX() + 2, rect.getY(), new TooltipRectPositioner(rect));
			tooltipAccess.setIcebergTooltipStack(ItemStack.EMPTY);
		}

		// Reset the current render context.
		currentRenderContext = EMPTY_CONTEXT;
	}

	private static ClientTooltipComponent getClientComponent(TooltipComponent componentData)
	{
		ClientTooltipComponent result = null;

		// First try using the register event.
		result = RegisterTooltipComponentFactoryEvent.EVENT.invoker().getComponent(componentData);

		// If that fails, try using the create method for vanilla and mixed-in tooltip components.
		if (result == null)
		{
			try { result = ClientTooltipComponent.create(componentData); }
			catch (IllegalArgumentException e) { }
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
							Iceberg.LOGGER.error("Error rendering tooltip component: \n" + ExceptionUtils.getStackTrace(e));
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

		// If the first component has a negative width, apply that to the wrap width.
		if (eventResult.tooltipElements().size() > 0 &&
			eventResult.tooltipElements().get(0).right().orElse(null) instanceof ClientTooltipComponent clientComponent &&
			clientComponent.getWidth(font) < 0)
		{
			tooltipTextWidth += clientComponent.getWidth(font);
		}

		final int tooltipTextWidthFinal = tooltipTextWidth;
		if (needsWrap)
		{
			return eventResult.tooltipElements().stream().flatMap(either -> either.map(text ->
								splitLine(text, font, tooltipTextWidthFinal),
								component -> Stream.of(getClientComponent(component)))).toList();
		}

		return eventResult.tooltipElements().stream().map(either -> either.map(text ->
							ClientTooltipComponent.create(text instanceof Component ?
								((Component) text).getVisualOrderText() : Language.getInstance().getVisualOrder(text)),
							component -> getClientComponent(component))).toList();
	}

	private static Stream<ClientTooltipComponent> splitLine(FormattedText text, Font font, int maxWidth)
	{
		// Don't discard empty lines.
		if (text instanceof Component component && component.getString().isEmpty())
		{
			return Stream.of(component.getVisualOrderText()).map(ClientTooltipComponent::create);
		}

		return font.split(text, maxWidth).stream().map(ClientTooltipComponent::create);
	}

	private static final Rect2i emptyRect = new Rect2i(0, 0, 0, 0);

	@Deprecated(forRemoval = true, since = "1.3.0")
	public static Rect2i calculateRect(final ItemStack stack, GuiGraphics graphics, ClientTooltipPositioner positioner, List<ClientTooltipComponent> components,
									   int mouseX, int mouseY, int screenWidth, int screenHeight, int maxTextWidth, Font font, int minWidth, boolean centeredTitle)
	{
		return calculateRect(stack, graphics, positioner, components, mouseX, mouseY, font);
	}

	public static Rect2i calculateRect(final ItemStack stack, GuiGraphics graphics, ClientTooltipPositioner positioner, List<ClientTooltipComponent> components,
									   int mouseX, int mouseY, Font font)
	{
		if (components == null || components.isEmpty() || stack == null)
		{
			return emptyRect;
		}

		// Set the current render context.
		TooltipRenderContext prevContext = currentRenderContext;
		currentRenderContext = CALCULATE_RECT_CONTEXT;

		if (graphics instanceof GuiGraphicsInvoker graphicsInvoker && graphics instanceof ITooltipAccess tooltipAccess)
		{
			ItemStack prevStack = tooltipAccess.getIcebergTooltipStack();
			tooltipAccess.setIcebergTooltipStack(stack);
			graphicsInvoker.invokeRenderTooltipInternal(font, components, mouseX, mouseY, positioner);
			tooltipAccess.setIcebergTooltipStack(prevStack);
		}

		// Restore the previous render context.
		currentRenderContext = prevContext;

		return currentRect;
	}

	@Deprecated(since = "1.3.0", forRemoval = true)
	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int width)
	{
		return centerTitle(components, font, width, calculateTitleLines(components));
	}

	@Deprecated(since = "1.3.0", forRemoval = true)
	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int width, int titleLines)
	{
		List<ClientTooltipComponent> result = new ArrayList<>(components);

		if (components.isEmpty() || titleLines <= 0 || titleLines >= components.size())
		{
			return result;
		}

		// Find the first title component, which is the first text component.
		int titleStart = calculateTitleStart(components);

		// Verify that there actually is at least one text component.
		if (titleStart >= components.size())
		{
			return result;
		}

		for (int i = 0; i < titleLines; i++)
		{
			ClientTooltipComponent titleComponent = components.get(titleStart + i);

			if (titleComponent != null)
			{
				List<FormattedText> recomposedLines = StringRecomposer.recompose(List.of(titleComponent));
				if (!recomposedLines.isEmpty())
				{
					FormattedCharSequence title = Language.getInstance().getVisualOrder(recomposedLines.get(0));

					while (ClientTooltipComponent.create(title).getWidth(font) < width)
					{
						title = FormattedCharSequence.fromList(List.of(SPACE, title, SPACE));
						if (title == null)
						{
							break;
						}
					}
					result.set(titleStart + i, ClientTooltipComponent.create(title));
				}
			}
		}
		return result;
	}
}