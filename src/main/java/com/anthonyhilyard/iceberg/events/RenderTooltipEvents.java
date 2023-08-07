package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.datafixers.util.Either;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipPositioner;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public final class RenderTooltipEvents
{
	public RenderTooltipEvents() { }

	public static final Event<RenderTooltipEvents.Gather> GATHER = EventFactory.createArrayBacked(RenderTooltipEvents.Gather.class,
		callbacks ->  (itemStack, screenWidth, screenHeight, tooltipElements, maxWidth, index) -> {
			GatherResult result = new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
			for (RenderTooltipEvents.Gather callback : callbacks)
			{
				result = callback.onGather(itemStack, screenWidth, screenHeight, result.tooltipElements, result.maxWidth, index);

				if (result.result != InteractionResult.PASS)
				{
					return result;
				}
			}
			return result;
		});

	public static final Event<RenderTooltipEvents.PreExt> PREEXT = EventFactory.createArrayBacked(RenderTooltipEvents.PreExt.class,
		callbacks ->  (stack, graphics, x, y, screenWidth, screenHeight, font, components, positioner, comparison, index) -> {
			PreExtResult result = new PreExtResult(InteractionResult.PASS, x, y, screenWidth, screenHeight, font);
			for (RenderTooltipEvents.PreExt callback : callbacks)
			{
				result = callback.onPre(stack, graphics, result.x, result.y, result.screenWidth, result.screenHeight, result.font, components, positioner, comparison, index);

				if (result.result != InteractionResult.PASS)
				{
					return result;
				}
			}
			return result;
		});

	public static final Event<RenderTooltipEvents.ColorExt> COLOREXT = EventFactory.createArrayBacked(RenderTooltipEvents.ColorExt.class,
		callbacks -> (stack, graphics, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, components, comparison, index) -> {
			ColorExtResult result = new ColorExtResult(backgroundStart, backgroundEnd, borderStart, borderEnd);
			for (RenderTooltipEvents.ColorExt callback : callbacks)
			{
				result = callback.onColor(stack, graphics, x, y, font, result.backgroundStart, result.backgroundEnd, result.borderStart, result.borderEnd, components, comparison, index);
			}
			return result;
	});

	public static final Event<RenderTooltipEvents.PostExt> POSTEXT = EventFactory.createArrayBacked(RenderTooltipEvents.PostExt.class,
		callbacks -> (stack, graphics, x, y, font, width, height, components, comparison, index) -> {
			for (RenderTooltipEvents.PostExt callback : callbacks)
			{
				callback.onPost(stack, graphics, x, y, font, width, height, components, comparison, index);
			}
	});

	@FunctionalInterface
	public interface Gather
	{
		GatherResult onGather(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index);
	}

	@FunctionalInterface
	public interface PreExt
	{
		PreExtResult onPre(ItemStack stack, GuiGraphics graphics, int x, int y, int screenWidth, int screenHeight, Font font, List<ClientTooltipComponent> components, ClientTooltipPositioner positioner, boolean comparison, int index);
	}

	@FunctionalInterface
	public interface ColorExt
	{
		ColorExtResult onColor(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, List<ClientTooltipComponent> components, boolean comparison, int index);
	}

	@FunctionalInterface
	public interface PostExt
	{
		void onPost(ItemStack stack, GuiGraphics graphics, int x, int y, Font font, int width, int height, List<ClientTooltipComponent> components, boolean comparison, int index);
	}

	public record GatherResult(InteractionResult result, int maxWidth, List<Either<FormattedText, TooltipComponent>> tooltipElements) {}
	public record PreExtResult(InteractionResult result, int x, int y, int screenWidth, int screenHeight, Font font) {}
	public record ColorExtResult(int backgroundStart, int backgroundEnd, int borderStart, int borderEnd) {}
}
