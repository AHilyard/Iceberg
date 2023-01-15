package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Either;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
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
		callbacks ->  (stack, components, poseStack, x, y, screenWidth, screenHeight, font, comparison, index) -> {
			PreExtResult result = new PreExtResult(InteractionResult.PASS, x, y, screenWidth, screenHeight, font);
			for (RenderTooltipEvents.PreExt callback : callbacks)
			{
				result = callback.onPre(stack, components, poseStack, result.x, result.y, screenWidth, screenHeight, result.font, comparison, index);

				if (result.result != InteractionResult.PASS)
				{
					return result;
				}
			}
			return result;
		});

	@Deprecated
	public static final Event<RenderTooltipEvents.Pre> PRE = EventFactory.createArrayBacked(RenderTooltipEvents.Pre.class,
		callbacks -> (stack, components, poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison) -> {
		for (RenderTooltipEvents.Pre callback : callbacks)
		{
			InteractionResult result = callback.onPre(stack, components, poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison);

			if (result != InteractionResult.PASS)
			{
				return result;
			}
		}
		return InteractionResult.PASS;
	});

	public static final Event<RenderTooltipEvents.ColorExt> COLOREXT = EventFactory.createArrayBacked(RenderTooltipEvents.ColorExt.class,
		callbacks -> (stack, components, poseStack, x, y, font, backgroundStart, backgroundEnd, borderStart, borderEnd, comparison, index) -> {
			ColorExtResult result = new ColorExtResult(backgroundStart, backgroundEnd, borderStart, borderEnd);
			for (RenderTooltipEvents.ColorExt callback : callbacks)
			{
				result = callback.onColor(stack, components, poseStack, x, y, font, result.backgroundStart, result.backgroundEnd, result.borderStart, result.borderEnd, comparison, index);
			}
			return result;
	});

	@Deprecated
	public static final Event<RenderTooltipEvents.Color> COLOR = EventFactory.createArrayBacked(RenderTooltipEvents.Color.class,
		callbacks -> (stack, components, poseStack, x, y, font, background, borderStart, borderEnd, comparison) -> {
		ColorResult result = new ColorResult(background, borderStart, borderEnd);
		for (RenderTooltipEvents.Color callback : callbacks)
		{
			result = callback.onColor(stack, components, poseStack, x, y, font, result.background, result.borderStart, result.borderEnd, comparison);
		}
		return result;
	});

	public static final Event<RenderTooltipEvents.PostExt> POSTEXT = EventFactory.createArrayBacked(RenderTooltipEvents.PostExt.class,
		callbacks -> (stack, components, poseStack, x, y, font, width, height, comparison, index) -> {
			for (RenderTooltipEvents.PostExt callback : callbacks)
			{
				callback.onPost(stack, components, poseStack, x, y, font, width, height, comparison, index);
			}
	});

	public static final Event<RenderTooltipEvents.Post> POST = EventFactory.createArrayBacked(RenderTooltipEvents.Post.class,
		callbacks -> (stack, components, poseStack, x, y, font, width, height, comparison) -> {
			for (RenderTooltipEvents.Post callback : callbacks)
			{
				callback.onPost(stack, components, poseStack, x, y, font, width, height, comparison);
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
		PreExtResult onPre(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, int screenWidth, int screenHeight, Font font, boolean comparison, int index);
	}

	@Deprecated
	@FunctionalInterface
	public interface Pre
	{
		InteractionResult onPre(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, Font font, boolean comparison);
	}

	@FunctionalInterface
	public interface ColorExt
	{
		ColorExtResult onColor(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int backgroundStart, int backgroundEnd, int borderStart, int borderEnd, boolean comparison, int index);
	}

	@Deprecated
	@FunctionalInterface
	public interface Color
	{
		ColorResult onColor(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, boolean comparison);
	}

	@FunctionalInterface
	public interface PostExt
	{
		void onPost(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int width, int height, boolean comparison, int index);
	}

	@Deprecated
	@FunctionalInterface
	public interface Post
	{
		void onPost(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int width, int height, boolean comparison);
	}

	public record GatherResult(InteractionResult result, int maxWidth, List<Either<FormattedText, TooltipComponent>> tooltipElements) {}
	public record PreExtResult(InteractionResult result, int x, int y, int screenWidth, int screenHeight, Font font) {}
	public record ColorExtResult(int backgroundStart, int backgroundEnd, int borderStart, int borderEnd) {}
	public record ColorResult(int background, int borderStart, int borderEnd) {}
}
