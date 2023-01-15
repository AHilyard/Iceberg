package com.anthonyhilyard.iceberg;

import java.util.List;

import com.anthonyhilyard.iceberg.events.RenderTooltipEvents;
import com.anthonyhilyard.iceberg.events.RenderTooltipEvents.GatherResult;
import com.anthonyhilyard.iceberg.util.Tooltips.TitleBreakComponent;
import com.mojang.datafixers.util.Either;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;

public class IcebergClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		TitleBreakComponent.registerFactory();
		RenderTooltipEvents.GATHER.register(IcebergClient::onGatherComponentsEventEnd);

		// Event testing.
		// CriterionCallback.EVENT.register((player, advancement, criterion) -> { Loader.LOGGER.info("CriterionCallback: {}, {}, {}", player.getName().getString(), advancement.getId().toString(), criterion); });
		// NewItemPickupCallback.EVENT.register((uuid, itemStack) -> { Loader.LOGGER.info("NewItemPickupCallback: {}, {}", uuid.toString(), itemStack.getDisplayName().getString()); });
		// RenderTickEvents.START.register((timer) -> { Loader.LOGGER.info("RenderTickEvents.START: {}", timer); });
		// RenderTooltipEvents.PRE.register((stack, components, poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.PRE: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, screenWidth, screenHeight, maxWidth, font, comparison);
		// 	return InteractionResult.SUCCESS;
		// });
		// RenderTooltipEvents.COLOR.register((stack, components, poseStack, x, y, font, background, borderStart, borderEnd, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.COLOR: {}, {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, font, borderStart, borderEnd, comparison);
		// 	return null;
		// });
		// RenderTooltipEvents.POST.register((stack, components, poseStack, x, y, font, width, height, comparison) -> {
		// 	Loader.LOGGER.info("RenderTooltipEvents.POST: {}, {}, {}, {}, {}, {}, {}, {}, {}", stack.getDisplayName().getString(), components.stream().map(Object::toString).collect(Collectors.joining()), poseStack, x, y, font, width, height, comparison);
		// });
	}

	public static GatherResult onGatherComponentsEventEnd(ItemStack itemStack, int screenWidth, int screenHeight, List<Either<FormattedText, TooltipComponent>> tooltipElements, int maxWidth, int index)
	{
		if (tooltipElements.size() > 1)
		{
			// Insert a title break component after the first formattedText component.
			for (int i = 0; i < tooltipElements.size(); i++)
			{
				if (tooltipElements.get(i).left().isPresent())
				{
					tooltipElements.add(i + 1, Either.<FormattedText, TooltipComponent>right(new TitleBreakComponent()));
					break;
				}
			}
		}

		return new GatherResult(InteractionResult.PASS, maxWidth, tooltipElements);
	}
}
