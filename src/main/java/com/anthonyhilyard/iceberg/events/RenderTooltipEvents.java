package com.anthonyhilyard.iceberg.events;

import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;

public final class RenderTooltipEvents
{
	public RenderTooltipEvents() { }

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

	public static final Event<RenderTooltipEvents.Color> COLOR = EventFactory.createArrayBacked(RenderTooltipEvents.Color.class,
		callbacks -> (stack, components, poseStack, x, y, font, background, borderStart, borderEnd, comparison) -> {
		ColorResult result = new ColorResult(background, borderStart, borderEnd);
		for (RenderTooltipEvents.Color callback : callbacks)
		{
			result = callback.onColor(stack, components, poseStack, x, y, font, result.background, result.borderStart, result.borderEnd, comparison);
		}
		return result;
	});

	public static final Event<RenderTooltipEvents.Post> POST = EventFactory.createArrayBacked(RenderTooltipEvents.Post.class,
		callbacks -> (stack, components, poseStack, x, y, font, width, height, comparison) -> {
		for (RenderTooltipEvents.Post callback : callbacks)
		{
			callback.onPost(stack, components, poseStack, x, y, font, width, height, comparison);
		}
	});

	@FunctionalInterface
	public interface Pre
	{
		InteractionResult onPre(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, int screenWidth, int screenHeight, int maxWidth, Font font, boolean comparison);
	}

	@FunctionalInterface
	public interface Color
	{
		ColorResult onColor(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int background, int borderStart, int borderEnd, boolean comparison);
	}

	@FunctionalInterface
	public interface Post
	{
		void onPost(ItemStack stack, List<ClientTooltipComponent> components, PoseStack poseStack, int x, int y, Font font, int width, int height, boolean comparison);
	}

	public record ColorResult(int background, int borderStart, int borderEnd) {}
}
