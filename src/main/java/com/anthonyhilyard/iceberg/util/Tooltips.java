package com.anthonyhilyard.iceberg.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.anthonyhilyard.iceberg.Loader;
import com.anthonyhilyard.iceberg.events.GatherComponentsExtEvent;
import com.anthonyhilyard.iceberg.events.RenderTooltipExtEvent;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Either;
import com.mojang.math.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTextTooltip;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.MultiBufferSource.BufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.commons.lang3.exception.ExceptionUtils;

public class Tooltips
{
	private static final FormattedCharSequence SPACE = FormattedCharSequence.forward(" ", Style.EMPTY);
	private static ItemRenderer itemRenderer = null;
	private static boolean tooltipWidthWarningShown = false;

	public static class TitleBreakComponent implements TooltipComponent, ClientTooltipComponent
	{
		@Override
		public int getHeight() { return 0; }

		@Override
		public int getWidth(Font font) { return 0; }
		
		public static void registerFactory()
		{
			FMLJavaModLoadingContext.get().getModEventBus().addListener(TitleBreakComponent::onRegisterTooltipEvent);
		}

		private static void onRegisterTooltipEvent(RegisterClientTooltipComponentFactoriesEvent event)
		{
			event.register(TitleBreakComponent.class, x -> x);
		}
	}

	public static interface InlineComponent { }

	public static class TooltipInfo
	{
		private int tooltipWidth = 0;
		private int titleLines = 1;
		private Font font;
		private List<ClientTooltipComponent> components = new ArrayList<>();

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
	
		RenderTooltipExtEvent.Pre preEvent = new RenderTooltipExtEvent.Pre(stack, poseStack, rectX, rectY, screenWidth, screenHeight, info.getFont(), info.getComponents(), comparison, index);
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

		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
		Matrix4f matrix4f = poseStack.last().pose();

		RenderTooltipExtEvent.Color colorEvent = new RenderTooltipExtEvent.Color(stack, poseStack, rectX, rectY, info.getFont(), backgroundColorStart, backgroundColorEnd, borderColorStart, borderColorEnd, info.getComponents(), comparison, index);
		MinecraftForge.EVENT_BUS.post(colorEvent);

		backgroundColorStart = colorEvent.getBackgroundStart();
		backgroundColorEnd = colorEvent.getBackgroundEnd();
		borderColorStart = colorEvent.getBorderStart();
		borderColorEnd = colorEvent.getBorderEnd();

		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY - 4, rectX + rect.getWidth() + 3, rectY - 3, backgroundColorStart, backgroundColorStart);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY + rect.getHeight() + 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 4, backgroundColorEnd, backgroundColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 4, rectY - 3, rectX - 3, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX + rect.getWidth() + 3, rectY - 3, rectX + rect.getWidth() + 4, rectY + rect.getHeight() + 3, backgroundColorStart, backgroundColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY - 3 + 1, rectX - 3 + 1, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX + rect.getWidth() + 2, rectY - 3 + 1, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3 - 1, borderColorStart, borderColorEnd);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY - 3, rectX + rect.getWidth() + 3, rectY - 3 + 1, borderColorStart, borderColorStart);
		GuiHelper.drawGradientRect(matrix4f, zLevel, rectX - 3, rectY + rect.getHeight() + 2, rectX + rect.getWidth() + 3, rectY + rect.getHeight() + 3, borderColorEnd, borderColorEnd);

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
			textComponent.renderText(preEvent.getFont(), rectX, tooltipTop, matrix4f, bufferSource);
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
			ClientTooltipComponent imageComponent = info.getComponents().get(componentNumber);
			imageComponent.renderImage(preEvent.getFont(), rectX, tooltipTop, poseStack, itemRenderer, zLevel);
			tooltipTop += imageComponent.getHeight() + (componentNumber == 0 ? 2 : 0);
		}

		itemRenderer.blitOffset = f;

		RenderTooltipExtEvent.Post postEvent = new RenderTooltipExtEvent.Post(stack, poseStack, rectX, rectY, info.getFont(), rect.getWidth(), rect.getHeight(), info.getComponents(), comparison, index);
		MinecraftForge.EVENT_BUS.post(postEvent);
	}

	public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent,
	int mouseX, int screenWidth, int screenHeight, @Nullable Font forcedFont, Font fallbackFont, int maxWidth)
	{
		return gatherTooltipComponents(stack, textElements, itemComponent, mouseX, screenWidth, screenHeight, forcedFont, fallbackFont, maxWidth, 0);
	}

	public static List<ClientTooltipComponent> gatherTooltipComponents(ItemStack stack, List<? extends FormattedText> textElements, Optional<TooltipComponent> itemComponent,
																	   int mouseX, int screenWidth, int screenHeight, @Nullable Font forcedFont, Font fallbackFont, int maxWidth, int index)
	{
		Font font = ForgeHooksClient.getTooltipFont(forcedFont, stack, fallbackFont);
		List<Either<FormattedText, TooltipComponent>> elements = textElements.stream()
				.map((Function<FormattedText, Either<FormattedText, TooltipComponent>>) Either::left)
				.collect(Collectors.toCollection(ArrayList::new));

		itemComponent.ifPresent(c -> elements.add(1, Either.right(c)));

		var event = new GatherComponentsExtEvent(stack, screenWidth, screenHeight, elements, maxWidth, index);
		if (MinecraftForge.EVENT_BUS.post(event))
		{
			return List.of();
		}

		int tooltipTextWidth = event.getTooltipElements().stream()
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
			if (tooltipX < 4) // if the tooltip doesn't fit on the screen
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

		int tooltipTextWidth = minWidth;
		int tooltipHeight = components.size() == 1 ? -2 : 0;
		int titleLines = calculateTitleLines(components);

		if (centeredTitle)
		{
			// Calculate the current tooltip width prior to centering.
			for (ClientTooltipComponent component : components)
			{
				int componentWidth = component.getWidth(event.getFont());
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

	public static List<ClientTooltipComponent> centerTitle(List<ClientTooltipComponent> components, Font font, int width)
	{
		return centerTitle(components, font, width, 1);
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
